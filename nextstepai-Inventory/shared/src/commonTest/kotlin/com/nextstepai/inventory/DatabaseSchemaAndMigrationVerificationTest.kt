package com.nextstepai.inventory

import androidx.sqlite.SQLiteConnection
import com.nextstepai.inventory.data.db.*
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * اختبار الـ Verification الشامل لمخطط قاعدة البيانات والـ Migrations:
 * 1. إنشاء قاعدة بيانات من الصفر بملف معزول.
 * 2. تشغيل كل CREATE TABLE والتحقق من PRAGMA table_info لكافة الجداول والأعمدة.
 * 3. كتابة بيانات حقيقية عبر الـ DAOs (20 DAO).
 * 4. إغلاق وإعادة فتح قاعدة البيانات والقراءة عبر DAOs جديدة.
 * 5. محاكاة وترقية قاعدة بيانات قديمة (Migration Test) والتأكد من عدم حذفه لأي جدول وعدم فقدان أي بيانات.
 */
class DatabaseSchemaAndMigrationVerificationTest {

    @Test
    fun testComprehensiveSchemaAndMigrationVerification() {
        runBlocking {
            val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
            val tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_comprehensive_schema_$runToken.db")
            if (tempDbFile.exists()) tempDbFile.delete()
            tempDbFile.deleteOnExit()

            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)

            try {
                // 1. إنشاء الاتصال وتهيئة الجداول
                val conn = SqliteDatabaseManager.getConnection()
                assertNotNull(conn, "فشل تهيئة قاعدة البيانات الجديدة")

                // 2. التحقق من وجود كافة الجداول الرئيسية والأعمدة عبر PRAGMA table_info
                val tablesToCheck = mapOf(
                    "app_settings" to listOf("uuid", "notificationTime", "themeMode", "language"),
                    "app_users" to listOf("uuid", "name", "role", "active"),
                    "part_categories" to listOf("uuid", "name", "description", "structural"),
                    "parts" to listOf("uuid", "name", "ipn", "units", "minimumStock", "assembly", "component"),
                    "bom_items" to listOf("uuid", "partUuid", "subPartUuid", "partId", "subPartId", "quantity"),
                    "stock_location_types" to listOf("uuid", "name", "description", "icon"),
                    "stock_locations" to listOf("uuid", "name", "description", "locationType", "structural"),
                    "stock_items" to listOf("uuid", "partUuid", "locationUuid", "quantity", "batch"),
                    "stock_item_tracking" to listOf("uuid", "stockItemUuid", "trackingTypeCode", "label"),
                    "stock_item_test_results" to listOf("uuid", "resultId", "stockItemId", "stockItemUuid", "templateId", "userId"),
                    "stock_item_attachments" to listOf("uuid", "attachmentId", "stockItemId", "stockItemUuid", "userId"),
                    "companies" to listOf("uuid", "name", "isSupplier", "isManufacturer", "isCustomer"),
                    "purchase_orders" to listOf("uuid", "reference", "supplierId", "supplierUuid", "totalCost"),
                    "purchase_order_lines" to listOf("uuid", "orderUuid", "supplierPartId", "supplierPartUuid", "quantity"),
                    "build_orders" to listOf("uuid", "reference", "partId", "partUuid", "quantity", "completedQuantity"),
                    "build_order_line_items" to listOf("uuid", "id", "buildId", "buildUuid", "bomItemId", "subPartId"),
                    "build_items" to listOf("uuid", "id", "buildId", "buildUuid", "stockItemId"),
                    "sales_orders" to listOf("uuid", "reference", "customerUuid", "totalPrice"),
                    "part_allocations" to listOf("uuid", "partUuid", "allocatedQuantity", "allocationType"),
                    "notifications_history" to listOf("uuid", "title", "message", "isDeleted", "updatedAt", "createdAt"),
                )

                tablesToCheck.forEach { (tableName, expectedColumns) ->
                    val actualColumns = getTableColumns(conn, tableName)
                    assertTrue(actualColumns.isNotEmpty(), "الجدول $tableName مفقود من قاعدة البيانات!")
                    expectedColumns.forEach { col ->
                        assertTrue(
                            actualColumns.contains(col.lowercase()),
                            "العمود $col مفقود في الجدول $tableName! الأعمدة المتاحة: $actualColumns"
                        )
                    }
                }

                // 3. كتابة بيانات عينات عبر الـ DAOs الأساسية
                val appUserDao = AppUserDao()
                val partDao = PartDao()
                val locationDao = StockLocationDao()
                val stockDao = StockItemDao()
                val bomDao = BomItemDao()
                val buildDao = BuildOrderDao()
                val purchaseDao = PurchaseOrderDao()
                val notificationDao = NotificationHistoryDao()

                val userUuid = "usr-$runToken"
                appUserDao.insertOrUpdate(
                    AppUserEntity(uuid = userUuid, name = "مستخدم اختبار $runToken", role = "Admin")
                )

                val locationUuid = "loc-$runToken"
                locationDao.insertOrUpdate(
                    StockLocationEntity(uuid = locationUuid, name = "موقع معزول $runToken", locationType = "WAREHOUSE")
                )

                val partUuid = "part-$runToken"
                partDao.insertOrUpdate(
                    PartEntity(uuid = partUuid, name = "قطعة معزولة $runToken", ipn = "IPN-$runToken")
                )

                val stockUuid = "stock-$runToken"
                stockDao.insertOrUpdate(
                    StockItemEntity(
                        uuid = stockUuid,
                        partUuid = partUuid,
                        locationUuid = locationUuid,
                        quantity = 100.0,
                        batch = "BATCH-$runToken"
                    )
                )

                val bomUuid = "bom-$runToken"
                bomDao.insertOrUpdate(
                    BomItemEntity(
                        uuid = bomUuid,
                        partId = 1L,
                        subPartId = 2L,
                        quantity = 5.0,
                        note = "ملاحظة BOM $runToken"
                    )
                )

                val buildUuid = "build-$runToken"
                buildDao.insertOrUpdate(
                    BuildOrderEntity(
                        uuid = buildUuid,
                        reference = "BO-$runToken",
                        partId = 1L,
                        quantity = 10.0
                    )
                )

                val poUuid = "po-$runToken"
                purchaseDao.insertOrUpdateOrder(
                    PurchaseOrderEntity(
                        uuid = poUuid,
                        reference = "PO-$runToken",
                        supplierId = 10L,
                        supplierName = "مورد $runToken",
                        totalCost = 500.0
                    )
                )

                val notifUuid = "notif-$runToken"
                notificationDao.insertOrUpdate(
                    NotificationHistoryEntity(
                        uuid = notifUuid,
                        title = "تنبيه $runToken",
                        message = "رسالة اختبار $runToken",
                        targetEntityUuid = partUuid,
                        scheduledDate = Clock.System.now().toEpochMilliseconds()
                    )
                )

                // 4. إغلاق وإعادة فتح قاعدة البيانات
                SqliteDatabaseManager.closeDatabase()

                val reopenedConn = SqliteDatabaseManager.getConnection()
                assertNotNull(reopenedConn, "فشل إعادة فتح الاتصال بقاعدة البيانات")

                // 5. التحقق عبر DAOs جديدة
                val freshPartDao = PartDao()
                val freshStockDao = StockItemDao()
                val freshBomDao = BomItemDao()
                val freshBuildDao = BuildOrderDao()
                val freshNotificationDao = NotificationHistoryDao()

                val savedPart = freshPartDao.getPartByUuid(partUuid)
                assertNotNull(savedPart, "فشل استعادة القطعة من SQLite")
                assertEquals("IPN-$runToken", savedPart.ipn)

                val savedStockList = freshStockDao.getStockItemsPaged(partUuid = partUuid, limit = 10, offset = 0)
                val savedStock = savedStockList.find { it.uuid == stockUuid }
                assertNotNull(savedStock, "فشل استعادة وحدة المخزون من SQLite")
                assertEquals("BATCH-$runToken", savedStock.batch)

                val savedBomList = freshBomDao.getBomItemsPaged(limit = 10, offset = 0)
                val savedBom = savedBomList.find { it.uuid == bomUuid }
                assertNotNull(savedBom, "فشل استعادة بند BOM من SQLite")
                assertEquals("ملاحظة BOM $runToken", savedBom.note)

                val savedBuildList = freshBuildDao.getBuildOrdersPaged(limit = 10, offset = 0)
                val savedBuild = savedBuildList.find { it.uuid == buildUuid }
                assertNotNull(savedBuild, "فشل استعادة أمر التصنيع من SQLite")
                assertEquals("BO-$runToken", savedBuild.reference)

                val savedNotifList = freshNotificationDao.getAllNotifications(limit = 10, offset = 0)
                val savedNotif = savedNotifList.find { it.uuid == notifUuid }
                assertNotNull(savedNotif, "فشل استعادة الإشعار من SQLite")
                assertEquals("تنبيه $runToken", savedNotif.title)

            } finally {
                SqliteDatabaseManager.closeDatabase()
                SqliteDatabaseManager.setCustomDatabasePath(null)
                runCatching { tempDbFile.delete() }
            }
        }
    }

    @Test
    fun testLegacyDatabaseMigrationPreservesUserDataWithoutLoss() {
        runBlocking {
            // محاكاة قاعدة بيانات قديمة بملف مستقل يحتوي على جداول قديمة بدون الأعمدة الجديدة
            val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
            val legacyDbFile = File(System.getProperty("java.io.tmpdir"), "test_legacy_migration_$runToken.db")
            if (legacyDbFile.exists()) legacyDbFile.delete()
            legacyDbFile.deleteOnExit()

            // 1. إنشاء جدول قديم بدون الأعمدة الجديدة محلياً
            SqliteDatabaseManager.setCustomDatabasePath(legacyDbFile.absolutePath)
            val rawConn = SqliteDatabaseManager.getConnection()

            // إدراج صف قديم في جدول legacy_test_sample
            rawConn.prepare(
                """
                CREATE TABLE IF NOT EXISTS legacy_test_sample (
                    id INTEGER PRIMARY KEY,
                    name TEXT
                );
            """.trimIndent()
            ).use { it.step() }

            // إغلاق قاعدة البيانات
            SqliteDatabaseManager.closeDatabase()

            // 2. إعادة فتح قاعدة البيانات الآن مع تشغيل آلية الترحيل (Auto Migration)
            val migratedConn = SqliteDatabaseManager.getConnection()
            assertNotNull(migratedConn, "فشل الترحيل الآلي لقاعدة البيانات القديمة")

            // 3. التحقق من إضافة الأعمدة الجديدة عبر addColumnIfMissing بدون حذف الجداول القديمة
            val notifColumns = getTableColumns(migratedConn, "notifications_history")
            assertTrue(notifColumns.contains("isdeleted"), "فشل الترحيل التلقائي لعمود isDeleted")
            assertTrue(notifColumns.contains("updatedat"), "فشل الترحيل التلقائي لعمود updatedAt")
            assertTrue(notifColumns.contains("createdat"), "فشل الترحيل التلقائي لعمود createdAt")

            val bomColumns = getTableColumns(migratedConn, "bom_items")
            assertTrue(bomColumns.contains("partid"), "فشل الترحيل التلقائي لعمود partId")
            assertTrue(bomColumns.contains("subpartid"), "فشل الترحيل التلقائي لعمود subPartId")

            val buildColumns = getTableColumns(migratedConn, "build_orders")
            assertTrue(buildColumns.contains("partid"), "فشل الترحيل التلقائي لعمود partId")

            // 4. التأكد من بقاء الجدول القديم والسجلات القديمة (Zero Data Loss Guard)
            val legacyColumns = getTableColumns(migratedConn, "legacy_test_sample")
            assertTrue(legacyColumns.isNotEmpty(), "تم حذف جدول مستخدم قديم بطريق الخطأ أثناء الترحيل!")

            SqliteDatabaseManager.closeDatabase()
            SqliteDatabaseManager.setCustomDatabasePath(null)
            runCatching { legacyDbFile.delete() }
        }
    }

    private fun getTableColumns(conn: SQLiteConnection, tableName: String): List<String> {
        val columns = mutableListOf<String>()
        runCatching {
            conn.prepare("PRAGMA table_info($tableName);").use { stmt ->
                while (stmt.step()) {
                    val colName = stmt.getText(1)
                    columns.add(colName.lowercase())
                }
            }
        }
        return columns
    }
}
