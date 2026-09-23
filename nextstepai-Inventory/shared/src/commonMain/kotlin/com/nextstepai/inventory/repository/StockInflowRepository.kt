package com.nextstepai.inventory.repository

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

            val sql = """
                SELECT uuid, id, name, ipn, description, revision, keywords, categoryId, units,
                       assembly, component, isTemplate, variantOfId, trackable, purchaseable,
                       salable, virtual, active, locked, minimumStock, maximumStock,
                       defaultLocationId, defaultExpiryDays, totalInStock, localImagePath, link,
                       syncStatus, isDeleted, updatedAt
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
                        id = stmt.getLong(1),
                        name = stmt.getText(2),
                        ipn = stmt.getText(3),
                        description = stmt.getText(4),
                        revision = stmt.getText(5),
                        keywords = stmt.getText(6),
                        categoryId = if (stmt.isNull(7)) null else stmt.getLong(7),
                        units = stmt.getText(8),
                        assembly = stmt.getLong(9) != 0L,
                        component = stmt.getLong(10) != 0L,
                        isTemplate = stmt.getLong(11) != 0L,
                        variantOfId = if (stmt.isNull(12)) null else stmt.getLong(12),
                        trackable = stmt.getLong(13) != 0L,
                        purchaseable = stmt.getLong(14) != 0L,
                        salable = stmt.getLong(15) != 0L,
                        virtual = stmt.getLong(16) != 0L,
                        active = stmt.getLong(17) != 0L,
                        locked = stmt.getLong(18) != 0L,
                        minimumStock = stmt.getDouble(19),
                        maximumStock = if (stmt.isNull(20)) null else stmt.getDouble(20),
                        defaultLocationId = if (stmt.isNull(21)) null else stmt.getLong(21),
                        defaultExpiryDays = if (stmt.isNull(22)) null else stmt.getLong(22).toInt(),
                        totalInStock = stmt.getDouble(23),
                        localImagePath = if (stmt.isNull(24)) null else stmt.getText(24),
                        link = stmt.getText(25),
                        syncStatus = SyncStatus.PENDING,
                        isDeleted = stmt.getLong(27) != 0L,
                        updatedAt = stmt.getLong(28)
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
            Pair(receivingLoc.locationId, receivingLoc.name)
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

                // 1. استعلام عن أعلى ID رقمي للأصناف لضمان التسلسل وعدم التعارض
                var maxPartId = 0L
                conn.prepare("SELECT COALESCE(MAX(id), 0) FROM parts").use { stmt ->
                    if (stmt.step()) {
                        maxPartId = stmt.getLong(0)
                    }
                }

                sessionState.scannedItems.forEach { item ->
                    var activePartId = item.partId

                    // 2. إنشاء الصنف الجديد بحساب المعرف الذري والتسلسلي
                    if (activePartId == null || item.isNewPart) {
                        maxPartId++
                        activePartId = maxPartId

                        val newPart = PartEntity(
                            uuid = item.partUuid ?: "part-${now}-${item.barcode.take(12)}",
                            id = activePartId,
                            name = item.name.ifBlank { "صنف جديد (${item.barcode})" },
                            ipn = item.barcode,
                            units = "pcs",
                            defaultLocationId = item.locationId,
                            totalInStock = 0.0,
                            updatedAt = now
                        )
                        partDao.insertOrUpdate(newPart)
                    }

                    // 3. إدراج سجل المخزون الفعلي في stock_items
                    val stockItemUuid = "stock-inflow-${now}-${item.tempId}"
                    val stockItem = StockItemEntity(
                        uuid = stockItemUuid,
                        partId = activePartId,
                        locationId = item.locationId,
                        quantity = item.quantity,
                        batch = "INFLOW-${DateTimeUtils.getCurrentDate()}",
                        purchaseOrderId = sessionState.purchaseOrderId,
                        statusCode = 10, // 10 = In Stock
                        updatedAt = now
                    )
                    stockItemDao.insertOrUpdate(stockItem)

                    // 4. تحديث الرصيد التراكمي في parts
                    partDao.addStockToPart(partId = activePartId, qty = item.quantity)

                    // 5. تسجيل حركة التتبع في stock_item_tracking
                    val tracking = StockItemTrackingEntity(
                        uuid = "track-${now}-${item.tempId}",
                        trackingId = now,
                        stockItemId = activePartId,
                        stockItemUuid = stockItemUuid,
                        date = DateTimeUtils.getCurrentDateTime(),
                        trackingTypeCode = 10, // Inflow / Receipt
                        label = "استلام توريد ميداني",
                        notes = "مرجع الوثيقة: ${sessionState.poReference ?: "استلام حر"}",
                        updatedAt = now
                    )
                    trackingDao.insertOrUpdate(tracking)

                    // 6. ضغط واقتطاع صورة الملصق بصيغة WebP وربطها بالمرفقات
                    item.croppedImageBytes?.let { bytes ->
                        val processed = imageProcessor.processAndCompressProductImage(bytes, 800, 800)
                        if (processed.bytes.isNotEmpty()) {
                            val attachment = StockItemAttachmentEntity(
                                uuid = "att-${now}-${item.tempId}",
                                attachmentId = now,
                                stockItemId = activePartId,
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

        partQtyMap.forEach { (partId, totalQty) ->
            if (partId != null) {
                val sql = """
                    UPDATE purchase_order_lines
                    SET receivedQuantity = receivedQuantity + ?, updatedAt = ?
                    WHERE orderUuid IN (SELECT uuid FROM purchase_orders WHERE supplierId = ? OR id = ?)
                """.trimIndent()
                conn.prepare(sql).use { stmt ->
                    stmt.bindDouble(1, totalQty)
                    stmt.bindLong(2, Clock.System.now().toEpochMilliseconds())
                    stmt.bindLong(3, poId)
                    stmt.bindLong(4, poId)
                    stmt.step()
                }
            }
        }
    }
}
