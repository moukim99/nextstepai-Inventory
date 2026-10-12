package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.data.StockTrackingType
import com.nextstepai.inventory.data.db.PurchaseOrderDao
import com.nextstepai.inventory.data.db.PurchaseOrderEntity
import com.nextstepai.inventory.data.db.PurchaseOrderLineEntity
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.data.db.SqliteNumericIdAllocator
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.data.db.StockItemTrackingEntity
import com.nextstepai.inventory.util.AppUuid
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.data.db.StockLocationEntity
import com.nextstepai.inventory.data.db.getRoomDatabase

/**
 * المستودع (Repository) المسؤول عن إدارة أوامر الشراء (Purchase Order) وبنودها والمزامنة الدفعية.
 */
class PurchaseOrderRepository(
    private val orderDao: PurchaseOrderDao = PurchaseOrderDao(),
    private val stockDao: StockItemDao = StockItemDao(),
    private val partDao: PartDao = PartDao(),
    private val trackingDao: StockItemTrackingDao = StockItemTrackingDao(),
    private val locationDao: StockLocationDao = StockLocationDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService()
) {
    /**
     * جلب قائمة أوامر الشراء المتاحة مجزأة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getOrdersPaged(
        supplierId: Long? = null,
        status: POStatus? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<PurchaseOrderEntity> {
        return orderDao.getOrdersPaged(supplierId = supplierId, statusCode = status?.code, limit = limit, offset = offset)
    }

    /**
     * البحث والفلترة في قائمة أوامر الشراء من قاعدة البيانات الدائمة (SQLite).
     */
    fun searchOrders(
        query: String = "",
        supplierId: Long? = null,
        status: POStatus? = null
    ): List<PurchaseOrder> {
        val entities = orderDao.getOrdersPaged(
            supplierId = supplierId,
            statusCode = status?.code,
            limit = 500,
            offset = 0
        )
        if (entities.isNotEmpty()) {
            val allParts = runCatching { partDao.getPartsPaged(limit = 1000) }.getOrDefault(emptyList())
            val partNameMap = allParts.associate { (it.uuid.removePrefix("part-").toLongOrNull() ?: 0L) to it.name }

            var result = entities.mapIndexed { index, entity ->
                val numericId = entity.uuid.removePrefix("po-").toLongOrNull() ?: (index + 1L)

                val dbLines = orderDao.getLinesForOrder(entity.uuid)
                val lineItems = dbLines.mapIndexed { lIndex, lineEntity ->
                    val lineNumericId = lineEntity.uuid.removePrefix("po-line-").toLongOrNull() ?: (lIndex + 1L)
                    val partName = partNameMap[lineEntity.supplierPartId] ?: "قطعة #${lineEntity.supplierPartId}"

                    PurchaseOrderLineItem(
                        id = lineNumericId,
                        orderId = numericId,
                        supplierPartId = lineEntity.supplierPartId,
                        partName = partName,
                        quantity = lineEntity.quantity,
                        receivedQuantity = lineEntity.receivedQuantity,
                        purchasePrice = lineEntity.purchasePrice,
                        targetDate = entity.targetDate,
                        destinationLocationId = null,
                        notes = ""
                    )
                }

                PurchaseOrder(
                    id = numericId,
                    reference = entity.reference,
                    supplierId = entity.supplierId,
                    supplierName = entity.supplierName,
                    status = POStatus.fromCode(entity.statusCode),
                    description = entity.description,
                    orderCurrency = entity.orderCurrency,
                    targetDate = entity.targetDate,
                    sourceType = entity.sourceType,
                    sourceReferenceUuid = entity.sourceReferenceUuid,
                    destinationLocationUuid = entity.destinationLocationUuid,
                    lineItems = lineItems
                )
            }
            if (query.isNotBlank()) {
                result = result.filter {
                    it.reference.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.supplierName.contains(query, ignoreCase = true)
                }
            }
            return result
        }
        return emptyList()
    }

    /**
     * إضافة أمر شراء جديد مع حفظ الكيان القابل للمزامنة.
     */
    fun addOrder(order: PurchaseOrder): PurchaseOrder {
        require(order.reference.isNotBlank()) { "الرمز المرجعي لأمر الشراء إلزامي" }
        val existingOrder = orderDao.getOrderByReference(order.reference)
        require(existingOrder == null) { "الرمز المرجعي '${order.reference}' مستخدم بالفعل لأمر شراء آخر." }

        val numericId = if (order.id > 0L) order.id else SqliteNumericIdAllocator.nextId("purchase_orders", "po-")
        val orderUuid = "po-$numericId"
        val finalOrder = order.copy(id = numericId)

        orderDao.insertOrUpdateOrder(
            PurchaseOrderEntity(
                uuid = orderUuid,
                reference = finalOrder.reference,
                supplierId = finalOrder.supplierId,
                supplierName = finalOrder.supplierName,
                statusCode = finalOrder.status.code,
                description = finalOrder.description,
                orderCurrency = finalOrder.orderCurrency,
                targetDate = finalOrder.targetDate,
                totalCost = finalOrder.totalCost,
                sourceType = finalOrder.sourceType,
                sourceReferenceUuid = finalOrder.sourceReferenceUuid,
                destinationLocationUuid = finalOrder.destinationLocationUuid,
                syncStatus = SyncStatus.PENDING
            )
        )
        val createdLines = finalOrder.lineItems.map { line ->
            val lineNumericId = if (line.id > 0L) line.id else SqliteNumericIdAllocator.nextId("purchase_order_lines", "po-line-")
            val lineUuid = "po-line-$lineNumericId"
            orderDao.insertOrUpdateLine(
                PurchaseOrderLineEntity(
                    uuid = lineUuid,
                    orderUuid = orderUuid,
                    supplierPartId = line.supplierPartId,
                    quantity = line.quantity,
                    receivedQuantity = line.receivedQuantity,
                    purchasePrice = line.purchasePrice,
                    syncStatus = SyncStatus.PENDING
                )
            )
            line.copy(id = lineNumericId, orderId = numericId)
        }
        return finalOrder.copy(lineItems = createdLines)
    }

    /**
     * إضافة بند داخل أمر شراء.
     */
    fun addLineItem(item: PurchaseOrderLineItem): PurchaseOrderLineItem {
        val parentOrder = orderDao.getOrderById(item.orderId)
        require(parentOrder != null) { "أمر الشراء المرتبط غير موجود" }
        require(parentOrder.statusCode != POStatus.PLACED.code && parentOrder.statusCode != POStatus.COMPLETE.code) {
            "لا يمكن إضافة بنود لأمر شراء معتمد أو مكتمل ومقفل."
        }

        val lineNumericId = if (item.id > 0L) item.id else SqliteNumericIdAllocator.nextId("purchase_order_lines", "po-line-")
        val lineUuid = "po-line-$lineNumericId"
        orderDao.insertOrUpdateLine(
            PurchaseOrderLineEntity(
                uuid = lineUuid,
                orderUuid = parentOrder.uuid,
                supplierPartId = item.supplierPartId,
                quantity = item.quantity,
                receivedQuantity = item.receivedQuantity,
                purchasePrice = item.purchasePrice,
                syncStatus = SyncStatus.PENDING
            )
        )
        return item.copy(id = lineNumericId)
    }

    /**
     * استلام كمية من بند أمر شراء وتحديث رصيد المخزون وسجل التتبع ذرياً (Closed-Loop Atomic Transaction).
     * 1. التحقق من صحة وصلاحية أمر الشراء (عدم الإلغاء أو الاكتمال المسبق).
     * 2. التحقق من الكمية المستلمة وعدم تجاوز المتبقي (منع الاستلام المكرر).
     * 3. تحديث receivedQuantity في purchase_order_lines.
     * 4. إدراج/تحديث رصيد المخزون الفعلي في stock_items ومجموع القطعة في parts.
     * 5. توثيق حركة الاستلام في stock_item_tracking (CREATED / INFLOW).
     * 6. فحص اكتمال كافة بنود الأمر:
     *    - إذا استلمت جميع البنود بالكامل -> إغلاق الأمر وتحديث حالته إلى COMPLETE (30).
     *    - إذا كان الاستلام جزئياً -> بقاء حالة الأمر في PLACED (20).
     */
    fun receiveLineItem(
        lineItemId: Long,
        qty: Double,
        destinationLocationUuid: String? = null,
        batchName: String? = null
    ): Boolean {
        require(qty > 0.0) { "الكمية المستلمة يجب أن تكون أكبر من الصفر" }

        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare("BEGIN IMMEDIATE;").use { it.step() }
        try {
            val now = Clock.System.now().toEpochMilliseconds()

            // 1. قراءة بيانات البند والأمر داخل المعاملة الذرية
            val lineEntity = orderDao.getLineById(lineItemId)
                ?: throw IllegalArgumentException("بند أمر الشراء برقم $lineItemId غير موجود في قاعدة البيانات")
            val orderEntity = orderDao.getOrderByUuid(lineEntity.orderUuid)
                ?: throw IllegalArgumentException("أمر الشراء المرتبط بالبند '${lineEntity.orderUuid}' غير موجود")

            val orderRef = orderEntity.reference
            val orderStatus = orderEntity.statusCode

            // 2. التحقق من صلاحية أمر الشراء وقابليته للاستلام
            if (orderStatus == POStatus.CANCELLED.code) {
                throw IllegalStateException("لا يمكن استلام بضاعة لأمر شراء ملغي ('$orderRef').")
            }
            if (orderStatus == POStatus.COMPLETE.code) {
                throw IllegalStateException("أمر الشراء '$orderRef' مكتمل ومستلم بالكامل بالفعل ولا يمكن استلام كميات إضافية منه.")
            }

            val currentReceived = lineEntity.receivedQuantity
            val totalQty = lineEntity.quantity
            val remainingNeeded = totalQty - currentReceived

            if (remainingNeeded <= 0.0001) {
                throw IllegalStateException("تم استلام هذا البند بالكامل بالفعل ولا يمكن تكرار استلامه.")
            }
            require(qty <= remainingNeeded + 0.0001) {
                "الكمية المستلمة ($qty) تتجاوز الكمية المتبقية المطلوبة ($remainingNeeded) للبند في أمر الشراء '$orderRef'."
            }

            // 3. التحقق الصارم من موقع الاستلام داخل المعاملة (Review Point: Location Validation & Fallback Policy)
            val requestedLocationUuid = destinationLocationUuid ?: orderEntity.destinationLocationUuid?.takeIf { it.isNotBlank() }
            val targetLocation: StockLocationEntity = if (requestedLocationUuid != null) {
                val loc = locationDao.getLocationByUuid(requestedLocationUuid)
                    ?: locationDao.getLocationById(requestedLocationUuid.removePrefix("location-").removePrefix("loc-").toLongOrNull() ?: -1L)
                    ?: throw IllegalArgumentException("موقع الاستلام المحدد ('$requestedLocationUuid') غير موجود في النظام")
                if (loc.structural) {
                    throw IllegalArgumentException("لا يمكن استلام المواد في موقع هيكلي ('${loc.name}')")
                }
                loc
            } else {
                // في حال عدم تحديد موقع استلام صريح، يتم اختيار أول موقع استلام غير هيكلي فعلي في النظام مع التحقق من وجوده
                locationDao.getAllLocations().firstOrNull { !it.structural && it.locationType == "RECEIVING" }
                    ?: locationDao.getAllLocations().firstOrNull { !it.structural }
                    ?: throw IllegalStateException("لا يوجد أي موقع تخزين غير هيكلي صالح للاستلام في النظام")
            }
            val finalLocationUuid = targetLocation.uuid

            // 4. تحديث كمية الاستلام المشروطة في purchase_order_lines مع فحص الصفوف المتأثرة
            val updateSuccess = orderDao.updateLineReceivedQuantityConditional(lineEntity.uuid, qty, now)
            if (!updateSuccess) {
                throw IllegalStateException("تعارض متزامن أو تجاوز للكمية المصرح بها أثناء تحديث استلام البند #${lineEntity.uuid}")
            }

            val finalBatch = batchName ?: orderRef

            // 5. إدراج سجل المخزون الجديد في stock_items
            val stockItemUuid = AppUuid.generate()
            stockDao.insertOrUpdate(
                StockItemEntity(
                    uuid = stockItemUuid,
                    partUuid = "part-${lineEntity.supplierPartId}",
                    locationUuid = finalLocationUuid,
                    quantity = qty,
                    purchasePrice = lineEntity.purchasePrice,
                    purchasePriceCurrency = orderEntity.orderCurrency,
                    batch = finalBatch,
                    purchaseOrderUuid = orderEntity.uuid,
                    statusCode = 10,
                    updatedAt = now
                )
            )

            // 6. تحديث الرصيد التراكمي في parts
            partDao.adjustTotalInStock(lineEntity.supplierPartId, qty)

            // 7. توثيق حركة الاستلام في stock_item_tracking
            trackingDao.insertOrUpdate(
                StockItemTrackingEntity(
                    uuid = AppUuid.generate(),
                    stockItemUuid = stockItemUuid,
                    trackingTypeCode = StockTrackingType.CREATED.code,
                    label = "استلام توريد من أمر شراء #$orderRef",
                    notes = "تم توريد $qty وحدة من القطعة #${lineEntity.supplierPartId} للموقع $finalLocationUuid",
                    deltas = "{\"receivedQty\": $qty, \"orderUuid\": \"${orderEntity.uuid}\", \"lineUuid\": \"${lineEntity.uuid}\", \"partId\": ${lineEntity.supplierPartId}}",
                    createdAt = now
                )
            )

            // 8. التحقق من اكتمال كافة بنود أمر الشراء وتحديث حالته
            val allOrderLines = orderDao.getLinesForOrder(orderEntity.uuid)
            val newTotalReceivedForLine = currentReceived + qty
            val isOrderFullyReceived = if (allOrderLines.isNotEmpty()) {
                allOrderLines.all { line ->
                    val lineRec = if (line.uuid == lineEntity.uuid) newTotalReceivedForLine else line.receivedQuantity
                    lineRec >= line.quantity - 0.0001
                }
            } else {
                newTotalReceivedForLine >= totalQty - 0.0001
            }

            val finalStatusCode = if (isOrderFullyReceived) POStatus.COMPLETE.code else POStatus.PLACED.code
            orderDao.updateOrderStatus(orderEntity.uuid, finalStatusCode, now)

            conn.prepare("COMMIT;").use { it.step() }
            return true
        } catch (e: Throwable) {
            runCatching { conn.prepare("ROLLBACK;").use { it.step() } }
            throw e
        }
    }

    /**
     * تغيير حالة أمر الشراء (Placed / Complete / Cancelled).
     */
    fun updateOrderStatus(orderId: Long, status: POStatus): Boolean {
        val order = orderDao.getOrderById(orderId) ?: return false
        val now = Clock.System.now().toEpochMilliseconds()
        return orderDao.updateOrderStatus(order.uuid, status.code, now)
    }

    /**
     * تنفيذ المزامنة الدفعية (Batch Sync) مع Cloudflare Worker.
     */
    suspend fun syncPendingOrders(): Int {
        val pending = orderDao.getPendingSyncOrders(limit = 50, offset = 0)
        if (pending.isEmpty()) return 0

        val payloads = pending.map { entity ->
            SyncPayload(
                uuid = entity.uuid,
                entityType = "PurchaseOrder",
                payloadJson = "{\"reference\":\"${entity.reference}\",\"supplierId\":${entity.supplierId},\"totalCost\":${entity.totalCost}}",
                isDeleted = entity.isDeleted,
                updatedAt = entity.updatedAt
            )
        }

        val response = batchSyncService.performBatchSync(payloads, Clock.System.now().toEpochMilliseconds() - 86400000)
        orderDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }
}
