package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.ScannedInflowItem
import com.nextstepai.inventory.data.StockInflowSessionState
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.PartEntity
import com.nextstepai.inventory.data.db.PurchaseOrderDao
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.data.db.StockItemAttachmentDao
import com.nextstepai.inventory.data.db.StockItemAttachmentEntity
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.data.db.StockItemTrackingEntity
import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.util.AppUuid
import com.nextstepai.inventory.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * مستودع معالجة الاستلام الميداني السريع (StockInflowRepository).
 * يدير عمليات التحقق من الأصناف، وتحديد موقع الاستلام الذكي، والاعتماد الفوري المجمع للمعاملة الذرية.
 */
class StockInflowRepository(
    private val partDao: PartDao = PartDao(),
    private val stockItemDao: StockItemDao = StockItemDao(),
    private val trackingDao: StockItemTrackingDao = StockItemTrackingDao(),
    private val attachmentDao: StockItemAttachmentDao = StockItemAttachmentDao(),
    private val locationDao: StockLocationDao = StockLocationDao(),
    private val purchaseOrderDao: PurchaseOrderDao = PurchaseOrderDao(),
    private val imageProcessor: ImageProcessor = ImageProcessor(maxDimension = 800, compressionQuality = 80)
) {

    /**
     * البحث اللحظي عن الصنف برقم الباركود أو الكود الداخلي IPN باستعلامات معلّمة صريحة.
     */
    suspend fun findPartByBarcode(barcode: String): PartEntity? = withContext(Dispatchers.IO) {
        if (barcode.isBlank()) return@withContext null
        runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            var matchedPart: PartEntity? = null

            val selectColumns = """
                uuid, name, ipn, description, categoryUuid, units, minimumStock, maximumStock,
                totalInStock, revision, keywords, assembly, component, isTemplate, variantOfUuid,
                trackable, purchaseable, salable, virtual, active, locked, defaultLocationUuid,
                defaultExpiryDays, link, localImagePath, metadata, version, syncStatus, isDeleted,
                updatedAt, lastModifiedByDeviceUuid
            """.trimIndent()
            val sql = """
                SELECT $selectColumns
                FROM parts
                WHERE isDeleted = 0 AND (ipn = ? OR uuid = ?)
                LIMIT 1
            """.trimIndent()

            conn.prepare(sql).use { stmt ->
                stmt.bindText(1, barcode.trim())
                stmt.bindText(2, barcode.trim())
                if (stmt.step()) {
                    matchedPart = PartEntity(
                        uuid = stmt.getText(0),
                        name = stmt.getText(1),
                        ipn = stmt.getText(2),
                        description = stmt.getText(3),
                        categoryUuid = if (stmt.isNull(4)) null else stmt.getText(4),
                        units = stmt.getText(5),
                        minimumStock = stmt.getDouble(6),
                        maximumStock = if (stmt.isNull(7)) null else stmt.getDouble(7),
                        totalInStock = stmt.getDouble(8),
                        revision = stmt.getText(9),
                        keywords = stmt.getText(10),
                        assembly = stmt.getLong(11) != 0L,
                        component = stmt.getLong(12) != 0L,
                        isTemplate = stmt.getLong(13) != 0L,
                        variantOfUuid = if (stmt.isNull(14)) null else stmt.getText(14),
                        trackable = stmt.getLong(15) != 0L,
                        purchaseable = stmt.getLong(16) != 0L,
                        salable = stmt.getLong(17) != 0L,
                        virtual = stmt.getLong(18) != 0L,
                        active = stmt.getLong(19) != 0L,
                        locked = stmt.getLong(20) != 0L,
                        defaultLocationUuid = if (stmt.isNull(21)) null else stmt.getText(21),
                        defaultExpiryDays = if (stmt.isNull(22)) null else stmt.getLong(22).toInt(),
                        link = stmt.getText(23),
                        localImagePath = if (stmt.isNull(24)) null else stmt.getText(24),
                        metadata = stmt.getText(25),
                        version = stmt.getLong(26).toInt(),
                        syncStatus = SyncStatus.fromString(stmt.getText(27)),
                        isDeleted = stmt.getLong(28) != 0L,
                        updatedAt = stmt.getLong(29),
                        lastModifiedByDeviceUuid = if (stmt.isNull(30)) null else stmt.getText(30)
                    )
                }
            }
            matchedPart
        }.getOrNull()
    }

    /**
     * جلب المعرف والاسم لموقع الاستلام الافتراضي الذكي.
     */
    suspend fun getDefaultReceivingLocation(): Pair<Long, String> = withContext(Dispatchers.IO) {
        val locations = locationDao.getAllLocations()
        val receivingLoc = locations.firstOrNull { it.locationType == "RECEIVING" || !it.structural }
            ?: locations.firstOrNull()

        if (receivingLoc != null) {
            val parsedId = receivingLoc.uuid.removePrefix("loc-").toLongOrNull() ?: 1L
            Pair(parsedId, receivingLoc.name)
        } else {
            Pair(1L, "الرف الرئيسي - المستودع العام")
        }
    }

    /**
     * تنفيذ المعاملة الذرية الشاملة للاستلام الميداني السريع دون أي أخطاء أو تعارضات.
     */
    suspend fun commitInflowSession(sessionState: StockInflowSessionState): Result<Boolean> = withContext(Dispatchers.IO) {
        if (sessionState.scannedItems.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("السلة فارغة، لا توجد عناصر للترحيل"))
        }

        runCatching {
            val conn = SqliteDatabaseManager.getConnection()
            val now = Clock.System.now().toEpochMilliseconds()

            try {
                conn.prepare("BEGIN TRANSACTION").use { it.step() }

                sessionState.scannedItems.forEach { item ->
                    var activePartUuid = item.partUuid ?: "part-${item.partId}"

                    // 2. إنشاء الصنف الجديد بحساب المعرف الذري
                    if (item.isNewPart || item.partUuid == null) {
                        activePartUuid = item.partUuid ?: AppUuid.generate()

                        val newPart = PartEntity(
                            uuid = activePartUuid,
                            name = item.name.ifBlank { "صنف جديد (${item.barcode})" },
                            ipn = item.barcode,
                            units = "pcs",
                            defaultLocationUuid = "loc-${item.locationId}",
                            totalInStock = 0.0,
                            version = 1,
                            updatedAt = now
                        )
                        partDao.insertOrUpdate(newPart)
                    }

                    // 3. إدراج سجل المخزون الفعلي في stock_items
                    val stockItemUuid = AppUuid.generate()
                    val stockItem = StockItemEntity(
                        uuid = stockItemUuid,
                        partUuid = activePartUuid,
                        locationUuid = "loc-${item.locationId}",
                        quantity = item.quantity,
                        batch = "INFLOW-${DateTimeUtils.getCurrentDate()}",
                        purchaseOrderUuid = sessionState.purchaseOrderId?.let { "po-$it" },
                        statusCode = 10, // 10 = In Stock
                        updatedAt = now
                    )
                    stockItemDao.insertOrUpdate(stockItem)

                    // 4. تحديث الرصيد التراكمي في parts
                    partDao.addStockToPart(partUuid = activePartUuid, qty = item.quantity)

                    // 5. تسجيل حركة التتبع في stock_item_tracking
                    val tracking = StockItemTrackingEntity(
                        uuid = AppUuid.generate(),
                        stockItemUuid = stockItemUuid,
                        trackingTypeCode = 10, // Inflow / Receipt
                        label = "استلام توريد ميداني",
                        notes = "مرجع الوثيقة: ${sessionState.poReference ?: "استلام حر"}",
                        createdAt = now
                    )
                    trackingDao.insertOrUpdate(tracking)

                    // 6. ضغط واقتطاع صورة الملصق بصيغة WebP وربطها بالمرفقات
                    item.croppedImageBytes?.let { bytes ->
                        val processed = imageProcessor.processAndCompressProductImage(bytes, 800, 800)
                        if (processed.bytes.isNotEmpty()) {
                            val attachment = StockItemAttachmentEntity(
                                uuid = "att-${now}-${item.tempId}",
                                attachmentId = now,
                                stockItemId = item.partId ?: 1L,
                                stockItemUuid = stockItemUuid,
                                attachment = "attachments/webp_${stockItemUuid}.webp",
                                comment = "ملصق باركود مقتطع",
                                uploadDate = DateTimeUtils.getCurrentDate(),
                                updatedAt = now
                            )
                            attachmentDao.insertOrUpdate(attachment)
                        }
                    }
                }

                // 7. تحديث كمية الاستلام في purchase_order_lines إن وجد أمر شراء مرتبط
                sessionState.purchaseOrderId?.let { poId ->
                    updatePoLinesReceivedQuantities(poId, sessionState.scannedItems)
                }

                conn.prepare("COMMIT").use { it.step() }
                true
            } catch (e: Exception) {
                runCatching { conn.prepare("ROLLBACK").use { it.step() } }
                throw e
            }
        }
    }

    private suspend fun updatePoLinesReceivedQuantities(poId: Long, items: List<ScannedInflowItem>) {
        val conn = SqliteDatabaseManager.getConnection()
        val partQtyMap = items.groupBy { it.partId }.mapValues { entry -> entry.value.sumOf { it.quantity } }
        val now = Clock.System.now().toEpochMilliseconds()
        val orderUuid = "po-$poId"

        partQtyMap.forEach { (partId, totalQty) ->
            if (partId != null) {
                val sql = """
                    UPDATE purchase_order_lines
                    SET receivedQuantity = receivedQuantity + ?, updatedAt = ?
                    WHERE orderUuid = ? AND supplierPartId = ?
                """.trimIndent()
                conn.prepare(sql).use { stmt ->
                    stmt.bindDouble(1, totalQty)
                    stmt.bindLong(2, now)
                    stmt.bindText(3, orderUuid)
                    stmt.bindLong(4, partId)
                    stmt.step()
                }
            }
        }

        // فحص اكتمال أمر الشراء تلقائياً عند استيفاء كامل البنود
        val lines = purchaseOrderDao.getLinesForOrder(orderUuid)
        if (lines.isNotEmpty() && lines.all { it.receivedQuantity >= it.quantity }) {
            purchaseOrderDao.updateOrderStatus(orderUuid, POStatus.COMPLETE.code, now)
        }
    }
}
