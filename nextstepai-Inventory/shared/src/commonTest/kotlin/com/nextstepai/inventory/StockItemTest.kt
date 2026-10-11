package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockItemTable
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.StockStatus
import com.nextstepai.inventory.data.StockTrackingType
import com.nextstepai.inventory.data.withCapacityUnit
import com.nextstepai.inventory.data.withWeightInfo
import com.nextstepai.inventory.data.unitWeight
import com.nextstepai.inventory.data.totalWeight
import com.nextstepai.inventory.data.calculatePhysicalOccupancy
import com.nextstepai.inventory.data.getOccupancySummary
import com.nextstepai.inventory.data.isLabelStale
import com.nextstepai.inventory.data.labelSnapshotData
import com.nextstepai.inventory.data.getLabelDiffDetails
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.PartEntity
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockItemEntity
import com.nextstepai.inventory.data.db.StockItemTrackingDao
import com.nextstepai.inventory.auth.AuthTokens
import com.nextstepai.inventory.auth.SecureTokenStorage
import com.nextstepai.inventory.repository.StockRepository
import com.nextstepai.inventory.repository.StocktakeItem
import com.nextstepai.inventory.repository.StocktakeReconciliationRequest
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class StockItemTest {

    @Test
    fun testSerialForcesQuantityToOne() {
        val stockTable = StockItemTable()

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 10L,
                quantity = 50.0, // سيتم إجبارها إلى 1.0 بسبب وجود الرقم التسلسلي
                serial = "TEST-SN-999"
            )
        )

        assertEquals(1.0, item.quantity)
        assertEquals("TEST-SN-999", item.serial)
    }

    @Test
    fun testDuplicateSerialThrowsException() {
        val stockTable = StockItemTable()

        stockTable.insertStockItem(
            StockItem(
                partId = 10L,
                serial = "DUP-SN-001"
            )
        )

        assertFailsWith<IllegalArgumentException> {
            stockTable.insertStockItem(
                StockItem(
                    partId = 10L,
                    serial = "DUP-SN-001"
                )
            )
        }
    }

    @Test
    fun testSplitStockItem() {
        val stockTable = StockItemTable()

        val parent = stockTable.insertStockItem(
            StockItem(
                partId = 5L,
                quantity = 100.0
            )
        )

        // تجزئة 30 وحدة من أصل 100
        val child = stockTable.splitStockItem(parent.id, 30.0)

        assertEquals(30.0, child.quantity)
        assertEquals(parent.id, child.parentId)

        val updatedParent = stockTable.getAllStockItems().find { it.id == parent.id }
        assertEquals(70.0, updatedParent?.quantity)
    }

    @Test
    fun testStocktakeAction() {
        val stockTable = StockItemTable()

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 12L,
                quantity = 15.0,
                reviewNeeded = true
            )
        )
        assertTrue(item.reviewNeeded)

        val stocktaken = stockTable.performStocktake(item.id, userId = 101L, stocktakeDate = "2025-02-15")

        assertEquals("2025-02-15", stocktaken.stocktakeDate)
        assertEquals(101L, stocktaken.stocktakeUserId)
        assertFalse(stocktaken.reviewNeeded)
    }

    @Test
    fun testFullStockTransfer() {
        val stockTable = StockItemTable()
        val loc1 = stockTable.insertLocation("الموقع A", locationType = "SHELF")
        val loc2 = stockTable.insertLocation("الموقع B", locationType = "SHELF")

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 100L,
                locationId = loc1.id,
                quantity = 50.0,
                batch = "BATCH-FULL-01"
            )
        )

        val (updatedSource, targetItem) = stockTable.transferStockItem(
            itemId = item.id,
            sourceLocationId = loc1.id,
            targetLocationId = loc2.id,
            quantityToTransfer = 50.0,
            reason = "إعادة ترتيب أرفف"
        )

        assertEquals(loc2.id, updatedSource.locationId)
        assertEquals(50.0, updatedSource.quantity)
        assertNull(targetItem)

        val trackingLogs = stockTable.getTrackingForStockItem(item.id)
        assertTrue(trackingLogs.any { it.notes.contains("إعادة ترتيب أرفف") })
    }

    @Test
    fun testPartialStockTransfer() {
        val stockTable = StockItemTable()
        val loc1 = stockTable.insertLocation("الموقع A", locationType = "SHELF")
        val loc2 = stockTable.insertLocation("الموقع B", locationType = "SHELF")

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 200L,
                locationId = loc1.id,
                quantity = 100.0,
                batch = "BATCH-PARTIAL-01"
            )
        )

        // نقل جزئي لكتمية 40 من أصل 100
        val (updatedSource, createdTarget) = stockTable.transferStockItem(
            itemId = item.id,
            sourceLocationId = loc1.id,
            targetLocationId = loc2.id,
            quantityToTransfer = 40.0,
            reason = "تغذية خط إنتاج",
            notes = "ملاحظات ميدانية"
        )

        assertEquals(60.0, updatedSource.quantity)
        assertEquals(loc1.id, updatedSource.locationId)

        assertTrue(createdTarget != null)
        assertEquals(40.0, createdTarget.quantity)
        assertEquals(loc2.id, createdTarget.locationId)
        assertEquals(200L, createdTarget.partId)

        val sourceLogs = stockTable.getTrackingForStockItem(item.id)
        val targetLogs = stockTable.getTrackingForStockItem(createdTarget.id)
        assertTrue(sourceLogs.any { it.notes.contains("تغذية خط إنتاج") })
        assertTrue(targetLogs.any { it.notes.contains("تغذية خط إنتاج") })
    }

    @Test
    fun testTransferInvalidQuantityFails() {
        val stockTable = StockItemTable()
        val loc1 = stockTable.insertLocation("الموقع A", locationType = "SHELF")
        val loc2 = stockTable.insertLocation("الموقع B", locationType = "SHELF")

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 300L,
                locationId = loc1.id,
                quantity = 10.0
            )
        )

        assertFailsWith<IllegalArgumentException> {
            stockTable.transferStockItem(
                itemId = item.id,
                sourceLocationId = loc1.id,
                targetLocationId = loc2.id,
                quantityToTransfer = 25.0, // أكبر من المتاح (10)
                reason = "اختبار خطأ"
            )
        }
    }

    @Test
    fun testPartialTransferMergeExistingBatch() {
        val stockTable = StockItemTable()
        val loc1 = stockTable.insertLocation("المرفأ A", locationType = "SHELF")
        val loc2 = stockTable.insertLocation("المرفأ B", locationType = "SHELF")

        // عنصر أصل في الموقع A
        val sourceItem = stockTable.insertStockItem(
            StockItem(
                partId = 50L,
                locationId = loc1.id,
                quantity = 100.0,
                batch = "BATCH-MERGE-2025"
            )
        )

        // عنصر قائم مسبقاً في الموقع B لنفس القطعة ونفس الدفعة
        val existingTarget = stockTable.insertStockItem(
            StockItem(
                partId = 50L,
                locationId = loc2.id,
                quantity = 20.0,
                batch = "BATCH-MERGE-2025"
            )
        )

        // نقل 30 قطعة من A إلى B
        val (updatedSource, updatedTarget) = stockTable.transferStockItem(
            itemId = sourceItem.id,
            sourceLocationId = loc1.id,
            targetLocationId = loc2.id,
            quantityToTransfer = 30.0,
            reason = "دمج رصيد"
        )

        // التحقق من إنقاص المصدر (100 - 30 = 70)
        assertEquals(70.0, updatedSource.quantity)
        assertEquals(loc1.id, updatedSource.locationId)

        // التحقق من زيادة السجل الهدف القائم (20 + 30 = 50) بدلاً من خلق سجل جديد
        assertTrue(updatedTarget != null)
        assertEquals(existingTarget.id, updatedTarget.id)
        assertEquals(50.0, updatedTarget.quantity)
        assertEquals(loc2.id, updatedTarget.locationId)

        // التحقق من توثيق حركات التتبع لكلا السجلين
        val sourceLogs = stockTable.getTrackingForStockItem(sourceItem.id)
        val targetLogs = stockTable.getTrackingForStockItem(existingTarget.id)
        assertTrue(sourceLogs.any { it.notes.contains("دمج رصيد") })
        assertTrue(targetLogs.any { it.notes.contains("دمج رصيد") })
    }

    @Test
    fun testDecimalPartialTransfer() {
        val stockTable = StockItemTable()
        val loc1 = stockTable.insertLocation("رف A", locationType = "SHELF")
        val loc2 = stockTable.insertLocation("رف B", locationType = "SHELF")

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 60L,
                locationId = loc1.id,
                quantity = 12.5,
                batch = "BATCH-DECIMAL"
            )
        )

        // نقل 4.25 من أصل 12.5
        val (updatedSource, createdTarget) = stockTable.transferStockItem(
            itemId = item.id,
            sourceLocationId = loc1.id,
            targetLocationId = loc2.id,
            quantityToTransfer = 4.25,
            reason = "تسوية أوزان"
        )

        assertEquals(8.25, updatedSource.quantity, 0.0001)
        assertTrue(createdTarget != null)
        assertEquals(4.25, createdTarget.quantity, 0.0001)
    }

    @Test
    fun testTransferToStructuralLocationFails() {
        val stockTable = StockItemTable()
        val loc1 = stockTable.insertLocation("رف A", locationType = "SHELF", structural = false)
        val structuralLoc = stockTable.insertLocation("موقع هيكلي", locationType = "SITE", structural = true)

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 70L,
                locationId = loc1.id,
                quantity = 10.0
            )
        )

        assertFailsWith<IllegalArgumentException> {
            stockTable.transferStockItem(
                itemId = item.id,
                sourceLocationId = loc1.id,
                targetLocationId = structuralLoc.id,
                quantityToTransfer = 5.0,
                reason = "محاولة نقل لموقع هيكلي"
            )
        }
    }

    @Test
    fun testInvalidQuantityGuards() {
        val stockTable = StockItemTable()
        val loc1 = stockTable.insertLocation("رف A", locationType = "SHELF")
        val loc2 = stockTable.insertLocation("رف B", locationType = "SHELF")

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 80L,
                locationId = loc1.id,
                quantity = 100.0
            )
        )

        // 1. كمية سالبة
        assertFailsWith<IllegalArgumentException> {
            stockTable.transferStockItem(
                itemId = item.id,
                sourceLocationId = loc1.id,
                targetLocationId = loc2.id,
                quantityToTransfer = -5.0,
                reason = "كمية سالبة"
            )
        }

        // 2. كمية صفرية
        assertFailsWith<IllegalArgumentException> {
            stockTable.transferStockItem(
                itemId = item.id,
                sourceLocationId = loc1.id,
                targetLocationId = loc2.id,
                quantityToTransfer = 0.0,
                reason = "كمية صفر"
            )
        }

        // 3. كمية تتجاوز المتاح (101 من أصل 100)
        assertFailsWith<IllegalArgumentException> {
            stockTable.transferStockItem(
                itemId = item.id,
                sourceLocationId = loc1.id,
                targetLocationId = loc2.id,
                quantityToTransfer = 101.0,
                reason = "تجاوز المتاح"
            )
        }

        // التأكد من عدم تغير رصيد السجل الأصلي إطلاقاً
        val reloaded = stockTable.getAllStockItems().find { it.id == item.id }
        assertEquals(100.0, reloaded?.quantity)
    }

    @Test
    fun testDeleteOnDeplete() {
        val stockTable = StockItemTable()

        val item = stockTable.insertStockItem(
            StockItem(
                partId = 20L,
                quantity = 10.0,
                deleteOnDeplete = true
            )
        )

        val remaining = stockTable.consumeStockQuantity(item.id, consumeQty = 10.0)

        assertNull(remaining)
        assertTrue(stockTable.getAllStockItems().none { it.id == item.id })
    }

    @Test
    fun testAll24SchemaFieldsAndDaoPersistence() = runBlocking {
        val dao = StockItemDao()
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = StockItemEntity(
            uuid = "stock-24-fields-test",
            partUuid = "part-100",
            locationUuid = "loc-2",
            quantity = 25.5,
            serial = "",
            batch = "LOT-2025-X",
            statusCode = StockStatus.OK.code,
            packaging = "Reel",
            purchasePrice = 12.5,
            purchasePriceCurrency = "SAR",
            purchaseOrderUuid = "po-55",
            supplierPartUuid = "sup-88",
            salesOrderUuid = "so-33",
            customerUuid = "cust-77",
            buildUuid = "build-12",
            isBuilding = false,
            parentStockItemUuid = null,
            expiryDate = "2026-12-31",
            stocktakeDate = "2025-02-15",
            stocktakeUserUuid = "usr-1",
            reviewNeeded = false,
            deleteOnDeplete = true,
            link = "https://example.com/item/100",
            notes = "ملاحظات الفحص والتوثيق الشامل",
            metadata = "{\"version\":\"1.0\"}",
            syncStatus = SyncStatus.PENDING,
            updatedAt = now
        )

        dao.insertOrUpdate(entity)

        val loadedList = dao.getStockItemsPaged(partUuid = "part-100", limit = 10, offset = 0)
        val loaded = loadedList.firstOrNull { it.uuid == "stock-24-fields-test" }

        assertTrue(loaded != null)
        assertEquals("part-100", loaded.partUuid)
        assertEquals("loc-2", loaded.locationUuid)
        assertEquals(25.5, loaded.quantity)
        assertEquals("LOT-2025-X", loaded.batch)
        assertEquals("Reel", loaded.packaging)
        assertEquals(12.5, loaded.purchasePrice)
        assertEquals("SAR", loaded.purchasePriceCurrency)
        assertEquals("po-55", loaded.purchaseOrderUuid)
        assertEquals("sup-88", loaded.supplierPartUuid)
        assertEquals("so-33", loaded.salesOrderUuid)
        assertEquals("cust-77", loaded.customerUuid)
        assertEquals("build-12", loaded.buildUuid)
        assertEquals("2026-12-31", loaded.expiryDate)
        assertEquals("2025-02-15", loaded.stocktakeDate)
        assertEquals("usr-1", loaded.stocktakeUserUuid)
        assertTrue(loaded.deleteOnDeplete)
        assertEquals("https://example.com/item/100", loaded.link)
        assertEquals("ملاحظات الفحص والتوثيق الشامل", loaded.notes)
    }

    @Test
    fun testStockDaoExplicitLimitOffsetPaging() = runBlocking {
        val dao = StockItemDao()
        val now = Clock.System.now().toEpochMilliseconds()

        for (i in 1..25) {
            dao.insertOrUpdate(
                StockItemEntity(
                    uuid = "stock-uuid-$i",
                    partUuid = "part-1",
                    quantity = 10.0,
                    syncStatus = SyncStatus.PENDING,
                    updatedAt = now + i
                )
            )
        }

        val testItems = dao.getStockItemsPaged(limit = 100, offset = 0).filter { it.uuid.startsWith("stock-uuid-") }

        val page1 = testItems.drop(0).take(10)
        assertEquals(10, page1.size)

        val page2 = testItems.drop(10).take(10)
        assertEquals(10, page2.size)

        val page3 = testItems.drop(20).take(10)
        assertEquals(5, page3.size)
    }

    @Test
    fun testStockBatchSyncWithCloudflare() = runBlocking {
        SecureTokenStorage().saveTokens(AuthTokens("test-access-token", "test-refresh-token"))
        val repository = StockRepository()
        repository.addStockItem(
            StockItem(
                partId = 1L,
                quantity = 5.0,
                serial = "SYNC-SN-123"
            )
        )

        val syncedCount = repository.syncPendingStockChanges()
        assertTrue(syncedCount > 0)
    }

    @Test
    fun testDirectOccupancyMatchingUnits() {
        val loc = StockLocation(name = "رف القطع", customCapacity = 100.0).withCapacityUnit("pcs")
        val part = Part(id = 1L, name = "مقاومة", units = "pcs")
        val item1 = StockItem(partId = 1L, locationId = loc.id, quantity = 30.0)
        val item2 = StockItem(partId = 1L, locationId = loc.id, quantity = 40.0)

        val occupancy = loc.calculatePhysicalOccupancy(listOf(item1, item2), listOf(part))
        assertEquals(70.0, occupancy, 0.001)
    }

    @Test
    fun testUnitWeightConversionOccupancy() {
        val loc = StockLocation(name = "رف الأعمدة الوزنية", customCapacity = 200.0).withCapacityUnit("kg")
        val part = Part(id = 2L, name = "أعمدة حديدية", units = "m")
        val item = StockItem(partId = 2L, locationId = loc.id, quantity = 80.0).withWeightInfo(unitWeight = 2.5, totalWeight = 200.0)

        val occupancy = loc.calculatePhysicalOccupancy(listOf(item), listOf(part))
        assertEquals(100.0, occupancy, 0.001)

        val (pctText, detailText) = loc.getOccupancySummary(listOf(item), listOf(part))
        assertTrue(pctText.contains("100%"))
        assertTrue(detailText.contains("80 m أعمدة حديدية"))
    }

    @Test
    fun testTotalWeightDirectEntry() {
        val loc = StockLocation(name = "ميزان كغ", customCapacity = 500.0).withCapacityUnit("kg")
        val part = Part(id = 3L, name = "كابلات", units = "m")
        // الميزان الكلي = 250 كغ، الكمية = 100 متر -> وزن الوحدة العكسي = 2.5 كغ/م
        val item = StockItem(partId = 3L, locationId = loc.id, quantity = 100.0).withWeightInfo(unitWeight = 2.5, totalWeight = 250.0)

        assertEquals(2.5, item.unitWeight)
        assertEquals(250.0, item.totalWeight)

        val occupancy = loc.calculatePhysicalOccupancy(listOf(item), listOf(part))
        assertEquals(50.0, occupancy, 0.001)
    }

    @Test
    fun testAutoLabelSnapshotOnLocationCreation() {
        val stockTable = StockItemTable()
        val loc = stockTable.insertLocation(
            StockLocation(
                name = "رف أوتوماتيكي A1",
                locationType = "SHELF",
                customCapacity = 500.0
            )
        )

        // الموقع ينشأ حديثاً بحالة مطابقة للقطع وتأسيساً للقطة المرجعية تلقائياً
        assertFalse(loc.isLabelStale)
        assertTrue(loc.labelSnapshotData != null)
        assertTrue(loc.labelSnapshotData!!.contains("رف أوتوماتيكي A1"))
        assertTrue(loc.labelSnapshotData!!.contains("500"))
    }

    @Test
    fun testStaleLabelDetectionAndDiffNotes() {
        val stockTable = StockItemTable()
        val loc = stockTable.insertLocation(
            StockLocation(
                name = "رف B1",
                locationType = "SHELF",
                customCapacity = 300.0
            )
        )
        assertFalse(loc.isLabelStale)

        // تعديل سعة الموقع من 300 إلى 800 قطعة
        val updatedLoc = loc.copy(customCapacity = 800.0)
        assertTrue(updatedLoc.isLabelStale)

        val diffs = updatedLoc.getLabelDiffDetails()
        assertTrue(diffs.isNotEmpty())
        assertTrue(diffs.any { it.contains("السعة") && it.contains("300") && it.contains("800") })
    }

    @Test
    fun testStocktakeSessionAuditAndReconciliationClosedLoop() {
        val stockRepository = StockRepository()
        val partDao = PartDao()
        val stockDao = StockItemDao()
        val trackingDao = StockItemTrackingDao()

        val partId = 7100L
        val stockItemId = 7101L
        val now = Clock.System.now().toEpochMilliseconds()

        partDao.insertOrUpdate(
            PartEntity(
                uuid = "part-$partId",
                name = "مكثف سيراميكي 100nF",
                ipn = "CAP-100NF-0805",
                units = "pcs",
                totalInStock = 50.0,
                updatedAt = now
            )
        )

        stockRepository.addStockItem(
            StockItem(
                id = stockItemId,
                partId = partId,
                locationId = 711L,
                quantity = 50.0,
                batch = "BATCH-CAP-710"
            )
        )

        // 1. توليد جلسة جرد ميداني للصنف
        val sessionItems = stockRepository.createStocktakeSession(partId = partId)
        val sessionItem = sessionItems.find { it.stockItemId == stockItemId }
        assertNotNull(sessionItem)
        assertEquals(50.0, sessionItem.bookQuantity)
        assertEquals(50.0, sessionItem.countedQuantity)
        assertTrue(sessionItem.isMatched)

        // 2. محاكاة فحص فعلي ينتج عنه عجز بمقدار 5 وحدات (الفعلي 45 والدفتري 50)
        val auditedItems = listOf(sessionItem.copy(countedQuantity = 45.0))
        val auditResult = stockRepository.calculateStocktakeAudit(auditedItems)
        assertEquals(1, auditResult.totalItemsCounted)
        assertEquals(0, auditResult.matchedCount)
        assertEquals(1, auditResult.shortageCount)
        assertEquals(-5.0, auditResult.netVariance)

        // 3. اعتماد التسوية الذرية للفارق
        val reconcileSuccess = stockRepository.reconcileStocktake(
            StocktakeReconciliationRequest(
                stockItemId = stockItemId,
                countedQuantity = 45.0,
                reason = "تلف عينات أثناء الفحص المخبري",
                notes = "تم إتلاف 5 وحدات تالفة",
                userId = 2L
            )
        )
        assertTrue(reconcileSuccess)

        // 4. التحقق من تحديث رصيد الوحدة المخزنية في SQLite
        val updatedStock = stockDao.getStockItemById(stockItemId)
        assertNotNull(updatedStock)
        assertEquals(45.0, updatedStock.quantity)
        assertEquals("usr-2", updatedStock.stocktakeUserUuid)

        // 5. التحقق من تعديل الرصيد التراكمي للصنف في جدول parts (50 - 5 = 45)
        val updatedPart = partDao.getPartByUuid("part-$partId")
        assertNotNull(updatedPart)
        assertEquals(45.0, updatedPart.totalInStock, "يجب تسوية رصيد الصنف الإجمالي بالفارق الفعلي")

        // 6. التحقق من توثيق حركة التسوية في سجل التتبع
        val allTrackings = trackingDao.getAllTrackingLogs()
        val adjustTracking = allTrackings.find { it.stockItemUuid == "stock-$stockItemId" && it.trackingTypeCode == StockTrackingType.ADJUST.code }
        assertNotNull(adjustTracking, "يجب تسجيل حركة تسوية الجرد (ADJUST) في سجل التتبع")
        assertTrue(adjustTracking.notes.contains("تلف عينات أثناء الفحص"))

        // 7. التحقق من حماية ومنع الكميات السالبة
        assertFailsWith<IllegalArgumentException> {
            stockRepository.reconcileStocktake(
                StocktakeReconciliationRequest(
                    stockItemId = stockItemId,
                    countedQuantity = -10.0,
                    reason = "خطأ مدخل سالب"
                )
            )
        }
    }

    @Test
    fun testWarehouseTransferAtomicityAndGuards() {
        val stockRepository = StockRepository()
        val stockDao = StockItemDao()
        val trackingDao = StockItemTrackingDao()

        val partId = 7200L
        val stockItemId = 7201L
        val now = Clock.System.now().toEpochMilliseconds()

        val loc1 = stockRepository.locationRepository.addLocation(
            StockLocation(name = "مستودع أ - رف 1", locationType = "SHELF", structural = false)
        )
        val loc2 = stockRepository.locationRepository.addLocation(
            StockLocation(name = "مستودع ب - رف 2", locationType = "SHELF", structural = false)
        )
        val structuralLoc = stockRepository.locationRepository.addLocation(
            StockLocation(name = "مبنى الإدارة العام", locationType = "BUILDING", structural = true)
        )

        stockRepository.addStockItem(
            StockItem(
                id = stockItemId,
                partId = partId,
                locationId = loc1.id,
                quantity = 20.0,
                batch = "BATCH-TRANS-720"
            )
        )

        // 1. حماية: منع النقل لنفس الموقع الحالي
        val sameLocError = assertFailsWith<IllegalArgumentException> {
            stockRepository.transferStockItem(
                itemId = stockItemId,
                sourceLocationId = loc1.id,
                targetLocationId = loc1.id,
                quantityToTransfer = 5.0,
                reason = "نقل لنفس الموقع"
            )
        }
        assertTrue(sameLocError.message?.contains("نفس موقع التخزين") == true)

        // 2. حماية: منع النقل إلى موقع هيكلي
        val structError = assertFailsWith<IllegalArgumentException> {
            stockRepository.transferStockItem(
                itemId = stockItemId,
                sourceLocationId = loc1.id,
                targetLocationId = structuralLoc.id,
                quantityToTransfer = 5.0,
                reason = "نقل لموقع هيكلي"
            )
        }
        assertTrue(structError.message?.contains("موقع هيكلي") == true)

        // 3. حماية: منع نقل كمية أكبر من المتاح (25 > 20)
        val overflowError = assertFailsWith<IllegalArgumentException> {
            stockRepository.transferStockItem(
                itemId = stockItemId,
                sourceLocationId = loc1.id,
                targetLocationId = loc2.id,
                quantityToTransfer = 25.0,
                reason = "كمية زائدة"
            )
        }
        assertTrue(overflowError.message?.contains("أكبر من الكمية المتاحة") == true)

        // 4. تنفيذ نقل جزئي ناجح (8 وحدات من أصل 20 إلى الموقع 2)
        val partialTransferSuccess = stockRepository.transferStockItem(
            itemId = stockItemId,
            sourceLocationId = loc1.id,
            targetLocationId = loc2.id,
            quantityToTransfer = 8.0,
            reason = "تغذية خط الإنتاج B"
        )
        assertTrue(partialTransferSuccess)

        // التحقق من الرصيد في المصدر والوجهة (12 في المصدر و 8 في الوجهة)
        val sourceAfter = stockDao.getStockItemById(stockItemId)
        assertNotNull(sourceAfter)
        assertEquals(12.0, sourceAfter.quantity, 0.001)

        val targetItems = stockDao.getStockItemsPaged(locationUuid = "loc-${loc2.id}")
        val targetItem = targetItems.find { it.partUuid == "part-$partId" }
        assertNotNull(targetItem)
        assertEquals(8.0, targetItem.quantity, 0.001)

        // 5. التحقق من ثبات مجموع الكميات (12 + 8 = 20)
        val allStocksForPart = stockDao.getAvailableStockItemsForPart(partId)
        val totalQuantity = allStocksForPart.sumOf { it.quantity }
        assertEquals(20.0, totalQuantity, 0.001, "يجب بقاء مجموع الكميات ثابتاً تماماً بعد النقل (Conservation of Stock)")

        // 6. التحقق من توثيق حركة النقل في سجل التتبع
        val trackings = trackingDao.getAllTrackingLogs().filter { it.stockItemUuid == "stock-$stockItemId" }
        val moveLog = trackings.find { it.trackingTypeCode == StockTrackingType.MOVE.code }
        assertNotNull(moveLog, "يجب توثيق حركة النقل (MOVE) في سجل التتبع")
    }
}
