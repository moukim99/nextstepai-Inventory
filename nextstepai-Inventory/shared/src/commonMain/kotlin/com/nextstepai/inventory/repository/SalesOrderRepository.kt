package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.data.SOStatus
import com.nextstepai.inventory.data.SalesOrder
import com.nextstepai.inventory.data.SalesOrderLineItem
import com.nextstepai.inventory.data.db.PartPricingDao
import com.nextstepai.inventory.data.db.SalesOrderDao
import com.nextstepai.inventory.data.db.SalesOrderEntity
import com.nextstepai.inventory.data.db.SalesOrderLineEntity
import com.nextstepai.inventory.data.db.SqliteNumericIdAllocator
import com.nextstepai.inventory.sync.SyncStatus

/**
 * نتيجة تشغيل محرك تلبية الطلبات (Order Fulfillment Engine Result).
 */
data class SalesOrderFulfillmentResult(
    val salesOrder: SalesOrder,
    val buildOrdersCreated: List<BuildOrder> = emptyList(),
    val purchaseOrdersCreated: List<PurchaseOrder> = emptyList(),
    val allocatedStockItemsCount: Int = 0,
    val summaryMessage: String = ""
)

/**
 * المستودع المسؤول عن المبيعات ومحرك تلبية الطلبات (Order Fulfillment Engine).
 */
class SalesOrderRepository(
    private val salesOrderDao: SalesOrderDao = SalesOrderDao(),
    private val partRepository: PartRepository = PartRepository(),
    private val partPricingDao: PartPricingDao = PartPricingDao(),
    private val buildOrderRepository: BuildOrderRepository = BuildOrderRepository(),
    private val purchaseOrderRepository: PurchaseOrderRepository = PurchaseOrderRepository()
) {

    fun searchOrders(
        query: String = "",
        customerId: Long? = null,
        status: SOStatus? = null
    ): List<SalesOrder> {
        val entities = salesOrderDao.getOrdersPaged(
            customerId = customerId,
            statusCode = status?.code,
            limit = 500,
            offset = 0
        )
        if (entities.isNotEmpty()) {
            var result = entities.mapIndexed { index, entity ->
                val numericId = entity.uuid.removePrefix("so-").toLongOrNull() ?: (index + 1L)
                val dbLines = salesOrderDao.getLinesForOrderUuid(entity.uuid)
                val lineItems = dbLines.mapIndexed { lineIdx, lineEntity ->
                    val lineId = lineEntity.uuid.removePrefix("so-line-").toLongOrNull() ?: (lineIdx + 1L)
                    SalesOrderLineItem(
                        id = lineId,
                        orderId = numericId,
                        orderUuid = entity.uuid,
                        partId = lineEntity.partId,
                        partUuid = lineEntity.partUuid,
                        partName = lineEntity.partName,
                        quantity = lineEntity.quantity,
                        unitPrice = lineEntity.unitPrice,
                        allocatedQuantity = lineEntity.allocatedQuantity,
                        shippedQuantity = lineEntity.shippedQuantity,
                        notes = lineEntity.notes
                    )
                }
                SalesOrder(
                    id = numericId,
                    uuid = entity.uuid,
                    reference = entity.reference,
                    customerId = entity.customerId,
                    customerUuid = entity.customerUuid,
                    customerName = entity.customerName,
                    status = SOStatus.fromCode(entity.statusCode),
                    description = entity.description,
                    orderCurrency = entity.orderCurrency,
                    targetDate = entity.targetDate,
                    notes = entity.notes,
                    lineItems = lineItems
                )
            }
            if (query.isNotBlank()) {
                result = result.filter {
                    it.reference.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.customerName.contains(query, ignoreCase = true)
                }
            }
            return result
        }

        return emptyList()
    }

    /**
     * حفظ أمر البيع وتفعيل محرك اتخاذ القرار التلقائي (Order Fulfillment Decision Flow).
     */
    fun addSalesOrderWithFulfillment(
        order: SalesOrder,
        autoFulfill: Boolean = true
    ): SalesOrderFulfillmentResult {
        require(order.reference.isNotBlank()) { "مرجع أمر البيع إلزامي" }
        require(order.lineItems.isNotEmpty()) { "أمر البيع يجب أن يحتوي على بند واحد على الأقل" }
        order.lineItems.forEach { line ->
            require(line.quantity > 0.0) { "كمية البند يجب أن تكون أكبر من الصفر (${line.partName})" }
            require(line.unitPrice >= 0.0) { "سعر الوحدة لا يمكن أن يكون سالباً (${line.partName})" }
        }

        val existingOrder = salesOrderDao.getOrderByReference(order.reference)
        if (existingOrder != null) {
            throw IllegalArgumentException("أمر البيع ذو المرجع '${order.reference}' موجود بالفعل مسبقاً")
        }

        val numericId = if (order.id > 0L) order.id else SqliteNumericIdAllocator.nextId("sales_orders", "so-")
        val orderUuid = "so-$numericId"
        val inserted = order.copy(id = numericId, uuid = orderUuid)

        salesOrderDao.insertOrUpdateOrder(
            SalesOrderEntity(
                uuid = orderUuid,
                reference = inserted.reference,
                customerId = inserted.customerId,
                customerUuid = inserted.customerUuid.ifBlank { "cust-${inserted.customerId}" },
                customerName = inserted.customerName,
                statusCode = inserted.status.code,
                description = inserted.description,
                orderCurrency = inserted.orderCurrency,
                targetDate = inserted.targetDate,
                totalPrice = inserted.totalPrice,
                notes = inserted.notes,
                syncStatus = SyncStatus.PENDING
            )
        )
        val createdLines = inserted.lineItems.map { line ->
            val lineNumericId = if (line.id > 0L) line.id else SqliteNumericIdAllocator.nextId("sales_order_lines", "so-line-")
            val lineUuid = "so-line-$lineNumericId"
            salesOrderDao.insertOrUpdateLine(
                SalesOrderLineEntity(
                    uuid = lineUuid,
                    orderUuid = orderUuid,
                    orderId = numericId,
                    partId = line.partId,
                    partName = line.partName,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    allocatedQuantity = line.allocatedQuantity,
                    shippedQuantity = line.shippedQuantity,
                    notes = line.notes,
                    syncStatus = SyncStatus.PENDING
                )
            )
            line.copy(id = lineNumericId, orderId = numericId, orderUuid = orderUuid)
        }
        val finalInserted = inserted.copy(lineItems = createdLines)

        if (!autoFulfill) {
            return SalesOrderFulfillmentResult(
                salesOrder = finalInserted,
                summaryMessage = "تم حفظ أمر البيع '${finalInserted.reference}' بدون تفعيل محرك التلبية الآلي."
            )
        }

        // ================= محرك اتخاذ القرار والربط التلقائي (Fulfillment Engine) =================
        val createdBuildOrders = mutableListOf<BuildOrder>()
        val createdPurchaseOrders = mutableListOf<PurchaseOrder>()
        var allocatedCount = 0

        val allParts = partRepository.getParts()

        inserted.lineItems.forEach { line ->
            val part = allParts.find { it.id == line.partId }
            val netAvailable = part?.availableStock ?: 0.0
            val shortage = (line.quantity - netAvailable).coerceAtLeast(0.0)
            val allocatedQty = minOf(line.quantity, netAvailable)

            if (allocatedQty > 0.0) {
                salesOrderDao.updateLineAllocatedQuantity("so-line-${line.id}", allocatedQty)
                allocatedCount++
            }

            if (part != null && part.assembly && shortage > 0.0) {
                // 1. منتج يُصنع داخلياً وبه عجز ➔ إنشاء أمر تصنيع BuildOrder
                val boRef = "BO-SO-${inserted.reference.removePrefix("SO-")}"
                val buildOrder = BuildOrder(
                    reference = boRef,
                    title = "إنتاج تلبية طلب العميل ${inserted.customerName}",
                    partId = part.id,
                    partName = part.name,
                    quantity = shortage,
                    status = BuildStatus.PENDING,
                    batch = "BATCH-${boRef}",
                    targetDate = inserted.targetDate,
                    salesOrderId = inserted.id,
                    issuedBy = "محرك التلبية التلقائي",
                    responsible = "خط الإنتاج المباشر",
                    notes = "أمر بناء آلي ناتج عن أمر البيع #${inserted.reference}"
                )
                val createdBo = buildOrderRepository.addBuildOrder(buildOrder)
                createdBuildOrders.add(createdBo)

                // فحص مواد الـ BOM العجز التابعة لأمر التصنيع المصنع حديثاً
                val bomShortageLines = buildOrderRepository.getLineItemsForBuild(createdBo.id)
                    .filter { it.quantity > it.allocatedQuantity }

                if (bomShortageLines.isNotEmpty()) {
                    val poRef = "PO-BO-${createdBo.reference.removePrefix("BO-")}"
                    val poLines = bomShortageLines.map { bomLine ->
                        PurchaseOrderLineItem(
                            orderId = 0L,
                            supplierPartId = bomLine.subPartId,
                            partName = bomLine.subPartName,
                            quantity = bomLine.quantity - bomLine.allocatedQuantity,
                            purchasePrice = bomLine.unitCost,
                            notes = "خامات لأمر التجميع #${createdBo.reference}"
                        )
                    }
                    val po = PurchaseOrder(
                        reference = poRef,
                        supplierId = part.defaultSupplierId ?: 1L,
                        supplierName = "المورد الافتراضي لخامات الإنتاج",
                        description = "أمر شراء مواد خام لمكونات أمر التجميع #${createdBo.reference}",
                        status = POStatus.PENDING,
                        orderCurrency = inserted.orderCurrency,
                        targetDate = inserted.targetDate,
                        sourceType = "BUILD_ORDER",
                        sourceReferenceUuid = "build-${createdBo.id}",
                        lineItems = poLines
                    )
                    createdPurchaseOrders.add(purchaseOrderRepository.addOrder(po))
                }

            } else if (part != null && !part.assembly && shortage > 0.0) {
                // 2. منتج يُشترى جاهزاً وبه عجز ➔ إنشاء أمر شراء PurchaseOrder بمصدر SALES_ORDER
                val poRef = "PO-SO-${inserted.reference.removePrefix("SO-")}"
                val pricing = partPricingDao.getForPart("part-${part.id}")
                val resolvedPurchasePrice = pricing?.purchaseCostMin ?: pricing?.overallMin ?: 12.0

                val poLine = PurchaseOrderLineItem(
                    orderId = 0L,
                    supplierPartId = part.id,
                    partName = part.name,
                    quantity = shortage,
                    purchasePrice = resolvedPurchasePrice,
                    notes = "شراء باك تو باك لتلبية أمر البيع #${inserted.reference}"
                )
                val po = PurchaseOrder(
                    reference = poRef,
                    supplierId = part.defaultSupplierId ?: 1L,
                    supplierName = "المورد المعتمد للقطعة ${part.name}",
                    description = "شراء مباشر للتغطية بطلب العميل ${inserted.customerName}",
                    status = POStatus.PENDING,
                    orderCurrency = inserted.orderCurrency,
                    targetDate = inserted.targetDate,
                    sourceType = "SALES_ORDER",
                    sourceReferenceUuid = "so-${inserted.id}",
                    lineItems = listOf(poLine)
                )
                createdPurchaseOrders.add(purchaseOrderRepository.addOrder(po))
            }
        }

        val msg = StringBuilder("تم تسجيل أمر البيع '${inserted.reference}' بنجاح: ")
        if (createdBuildOrders.isNotEmpty()) msg.append("توليد ${createdBuildOrders.size} أوامر تصنيع، ")
        if (createdPurchaseOrders.isNotEmpty()) msg.append("توليد ${createdPurchaseOrders.size} أوامر شراء مباشرة، ")
        if (allocatedCount > 0) msg.append("تأمين ${allocatedCount} مواد من المخزون المتوفر.")

        return SalesOrderFulfillmentResult(
            salesOrder = finalInserted,
            buildOrdersCreated = createdBuildOrders,
            purchaseOrdersCreated = createdPurchaseOrders,
            allocatedStockItemsCount = allocatedCount,
            summaryMessage = msg.toString()
        )
    }
}
