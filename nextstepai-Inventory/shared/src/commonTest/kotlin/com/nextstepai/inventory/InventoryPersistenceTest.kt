package com.nextstepai.inventory

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockItem
import com.nextstepai.inventory.data.StockLocation
import com.nextstepai.inventory.data.db.BomItemDao
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.data.db.StockItemDao
import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.repository.BomRepository
import com.nextstepai.inventory.repository.PartRepository
import com.nextstepai.inventory.repository.StockRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * اختبار الـ Persistence الحقيقي لمسار العمل كاملاً مع عزل كامل لمسار ملف قاعدة البيانات:
 * إنشاء Warehouse ← إنشاء Raw Materials ← إنشاء Finished Material ← إغلاق/إعادة فتح قاعدة البيانات ← التأكد من بقاء كل البيانات.
 */
class InventoryPersistenceTest {

    @Test
    fun testCompleteInventoryLifecyclePersistenceAcrossDatabaseReopen() = runBlocking {
        // عزل ملف قاعدة البيانات بملف مؤقت مستقل ونظيف تماماً لكل تشغيلة اختبار
        val tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_inventory_${Clock.System.now().toEpochMilliseconds()}.db")
        if (tempDbFile.exists()) tempDbFile.delete()
        tempDbFile.deleteOnExit()
        SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)

        try {
            // Step 0: Ensure database is initialized in isolated file
            val initialConn = try {
                SqliteDatabaseManager.getConnection()
            } catch (e: Throwable) {
                println(">>> EXCEPTION DURING INITIAL DB INITIALIZATION: ${e::class.simpleName}: ${e.message}")
                e.printStackTrace()
                throw e
            }
            assertNotNull(initialConn, "فشل تهيئة الاتصال الأولي بقاعدة البيانات المعزولة")

            val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
            val warehouseName = "مستودع الاختبار للبيانات الدائمة-$runToken"
            val raw1Ipn = "RES-10K-$runToken"
            val raw2Ipn = "MCU-ESP32-$runToken"
            val finishedIpn = "DEV-SMART-$runToken"
            val batchRaw1 = "BATCH-RES-$runToken"
            val batchRaw2 = "BATCH-MCU-$runToken"
            val batchFinished = "PROD-FINAL-$runToken"

            val partRepository = PartRepository()
            val stockRepository = StockRepository()
            val bomRepository = BomRepository()

            // 1. إنشاء Warehouse (موقع تخزيني معزول)
            val warehouse = stockRepository.addLocation(
                StockLocation(
                    name = warehouseName,
                    description = "مستودع رئيسي معزول لاختبار الاستمرارية رمز الجلسة $runToken",
                    locationType = "WAREHOUSE",
                    structural = false,
                    external = false,
                )
            )
            assertNotNull(warehouse)
            assertTrue((warehouse.id > 0L) || warehouse.uuid.isNotBlank(), "فشل إنشاء المستودع بـ ID/UUID صحيح")

            // 2. إنشاء Raw Materials (المواد الخام المعزولة)
            val rawMaterial1 = partRepository.addPart(
                Part(
                    name = "مقاومة كربونية 10K Ohm $runToken",
                    ipn = raw1Ipn,
                    description = "مقاومة كربونية معزولة للجلسة $runToken",
                    units = "pcs",
                    component = true,
                    assembly = false,
                    purchaseable = true,
                    minimumStock = 100.0
                )
            )

            val rawMaterial2 = partRepository.addPart(
                Part(
                    name = "متحكم ESP32-WROOM-32 $runToken",
                    ipn = raw2Ipn,
                    description = "متحكم دقيق معزول للجلسة $runToken",
                    units = "pcs",
                    component = true,
                    assembly = false,
                    purchaseable = true,
                    minimumStock = 20.0
                )
            )

            assertNotNull(rawMaterial1)
            assertNotNull(rawMaterial2)

            // إضافة كميات مخزنية للمواد الخام في المستودع
            val stockRaw1 = stockRepository.addStockItem(
                StockItem(
                    partId = rawMaterial1.id,
                    locationId = warehouse.id,
                    quantity = 1000.0,
                    batch = batchRaw1,
                    packaging = "Reel"
                )
            )

            val stockRaw2 = stockRepository.addStockItem(
                StockItem(
                    partId = rawMaterial2.id,
                    locationId = warehouse.id,
                    quantity = 250.0,
                    batch = batchRaw2,
                    packaging = "Tray"
                )
            )

            assertNotNull(stockRaw1)
            assertNotNull(stockRaw2)

            // 3. إنشاء Finished Material (المنتج النهائي المعزول)
            val finishedProduct = partRepository.addPart(
                Part(
                    name = "جهاز قراءة ذكي $runToken",
                    ipn = finishedIpn,
                    description = "منتج نهائي مُجمع للجلسة $runToken",
                    units = "unit",
                    assembly = true,
                    component = false,
                    salable = true
                )
            )
            assertNotNull(finishedProduct)

            // إضافة قائمة مواد التصنيع (BOM) لربط المكونات بالمنتج النهائي
            val bomItem1 = bomRepository.addBomItem(
                BomItem(
                    partId = finishedProduct.id,
                    subPartId = rawMaterial1.id,
                    quantity = 4.0,
                    reference = "R1-R4",
                    note = "مقاومات تحييد الدخل للجلسة $runToken"
                )
            )

            val bomItem2 = bomRepository.addBomItem(
                BomItem(
                    partId = finishedProduct.id,
                    subPartId = rawMaterial2.id,
                    quantity = 1.0,
                    reference = "U1",
                    note = "وحدة المعالجة الرئيسية للجلسة $runToken"
                )
            )

            assertNotNull(bomItem1)
            assertNotNull(bomItem2)

            // إضافة مخزون من المنتج النهائي في المستودع
            val stockFinished = stockRepository.addStockItem(
                StockItem(
                    partId = finishedProduct.id,
                    locationId = warehouse.id,
                    quantity = 15.0,
                    batch = batchFinished,
                    packaging = "Box"
                )
            )
            assertNotNull(stockFinished)

            // 4. إغلاق قاعدة البيانات لتصفير الاتصال بالذاكرة تماماً
            SqliteDatabaseManager.closeDatabase()

            // 5. إعادة فتح قاعدة البيانات عبر الحصول على الاتصال مجدداً من ملف القرص المؤقت المعزول
            val reopenedConn = SqliteDatabaseManager.getConnection()
            assertNotNull(reopenedConn, "فشل إعادة فتح قاعدة البيانات من ملف القرص المعزول")

            // 6. التحقق الأولي المباشر عبر كائنات DAO (استعلامات SQL صريحة ومباشرة على SQLite)
            val locationDao = StockLocationDao()
            val partDao = PartDao()
            val stockDao = StockItemDao()
            val bomDao = BomItemDao()

            // (أ) التحقق من بقاء المستودع عبر DAO المباشر
            val allLocationsDao = locationDao.getAllLocations()
            val savedWarehouseDao = allLocationsDao.find { it.name == warehouseName }
            assertNotNull(savedWarehouseDao, "لم يتم العثور على المستودع المنشأ بعد إعادة فتح قاعدة البيانات عبر DAO")
            assertEquals("WAREHOUSE", savedWarehouseDao.locationType)

            // (ب) التحقق من بقاء المواد الخام والمنتج النهائي في SQLite عبر DAO المباشر المعزول
            val allPartsDao = partDao.getPartsPaged(limit = 1000, offset = 0)
            val savedRaw1Dao = allPartsDao.find { it.ipn == raw1Ipn }
            val savedRaw2Dao = allPartsDao.find { it.ipn == raw2Ipn }
            val savedFinishedDao = allPartsDao.find { it.ipn == finishedIpn }

            assertNotNull(savedRaw1Dao, "المادة الخام الأولى ($raw1Ipn) مفقودة بعد إعادة الفتح عبر DAO")
            assertNotNull(savedRaw2Dao, "المادة الخام الثانية ($raw2Ipn) مفقودة بعد إعادة الفتح عبر DAO")
            assertNotNull(savedFinishedDao, "المنتج النهائي ($finishedIpn) مفقود بعد إعادة الفتح عبر DAO")

            assertEquals("مقاومة كربونية 10K Ohm $runToken", savedRaw1Dao.name)
            assertEquals("متحكم ESP32-WROOM-32 $runToken", savedRaw2Dao.name)
            assertTrue(savedFinishedDao.assembly)

            // (جـ) التحقق من بقاء المخزون بأرقام الشحنة والكميات المعزولة عبر DAO المباشر
            val allStockDao = stockDao.getStockItemsPaged(limit = 1000, offset = 0)
            val savedStockRaw1Dao = allStockDao.find { it.batch == batchRaw1 }
            val savedStockRaw2Dao = allStockDao.find { it.batch == batchRaw2 }
            val savedStockFinishedDao = allStockDao.find { it.batch == batchFinished }

            assertNotNull(savedStockRaw1Dao, "مخزون المادة الخام الأولى مفقود عبر DAO")
            assertNotNull(savedStockRaw2Dao, "مخزون المادة الخام الثانية مفقود عبر DAO")
            assertNotNull(savedStockFinishedDao, "مخزون المنتج النهائي مفقود عبر DAO")

            assertEquals(1000.0, savedStockRaw1Dao.quantity)
            assertEquals(250.0, savedStockRaw2Dao.quantity)
            assertEquals(15.0, savedStockFinishedDao.quantity)

            // (د) التحقق من بقاء شجرة BOM الهندسية للمنتج النهائي عبر DAO المباشر
            val savedBomItemsDao = bomDao.getBomItemsPaged(limit = 1000, offset = 0)
            val finishedBomEntriesDao = savedBomItemsDao.filter { it.partId == finishedProduct.id }
            assertTrue(finishedBomEntriesDao.isNotEmpty(), "عناصر قائمة مواد التصنيع BOM مفقودة بعد إعادة الفتح عبر DAO")

            // 7. التحقق عبر كائنات Repository جديدة تماماً أُنشئت بعد إعادة الفتح
            val freshStockRepo = StockRepository()
            val freshPartRepo = PartRepository()
            val freshBomRepo = BomRepository()

            val repoWarehouse = freshStockRepo.getLocations().find { it.name == warehouseName }
            assertNotNull(repoWarehouse, "الـ Repository الجديد المُنظّف لم يجد المستودع المسترجع من SQLite")

            val repoParts = freshPartRepo.getParts()
            val repoRaw1 = repoParts.find { it.ipn == raw1Ipn }
            val repoRaw2 = repoParts.find { it.ipn == raw2Ipn }
            val repoFinished = repoParts.find { it.ipn == finishedIpn }

            assertNotNull(repoRaw1, "الـ Repository الجديد لم يسترجع المادة الخام الأولى")
            assertNotNull(repoRaw2, "الـ Repository الجديد لم يسترجع المادة الخام الثانية")
            assertNotNull(repoFinished, "الـ Repository الجديد لم يسترجع المنتج النهائي")

            val repoStock = freshStockRepo.getStockItems()
            val repoStockRaw1 = repoStock.find { it.batch == batchRaw1 }
            val repoStockFinished = repoStock.find { it.batch == batchFinished }

            assertNotNull(repoStockRaw1, "الـ Repository الجديد لم يسترجع مخزون المادة الخام")
            assertNotNull(repoStockFinished, "الـ Repository الجديد لم يسترجع مخزون المنتج النهائي")
            assertEquals(1000.0, repoStockRaw1.quantity)
            assertEquals(15.0, repoStockFinished.quantity)

            val repoBomEntries = freshBomRepo.getBomItemsForPart(finishedProduct.id)
            assertTrue(repoBomEntries.isNotEmpty(), "الـ Repository الجديد لم يسترجع عناصر شجرة الـ BOM للمنتج النهائي")
        } finally {
            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(null)
            runCatching { tempDbFile.delete() }
        }
    }
}
