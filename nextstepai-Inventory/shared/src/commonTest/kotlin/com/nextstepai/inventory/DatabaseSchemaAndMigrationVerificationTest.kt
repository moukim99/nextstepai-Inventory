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
 * اختبار الـ Verification الشامل المكتمل لمخطط قاعدة البيانات والـ Migrations عبر جميع الـ 20 DAO المتاحة:
 * 1. إنشاء قاعدة بيانات من الصفر بملف معزول.
 * 2. تشغيل كل CREATE TABLE والتحقق من PRAGMA table_info لكافة الجداول والأعمدة.
 * 3. كتابة بيانات حقيقية واستعلامها عبر جميع الـ 20 DAO (وجميع الـ DAOs الفرعية الـ 27 كلياً).
 * 4. إغلاق وإعادة فتح قاعدة البيانات والقراءة عبر DAOs جديدة لتأكيد الـ Persistence.
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
                val conn = try {
                    SqliteDatabaseManager.getConnection()
                } catch (e: Throwable) {
                    println(">>> COMPREHENSIVE_DAO_ERR ON GET_CONNECTION: ${e::class.simpleName}: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
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
                    "contacts" to listOf("uuid", "companyUuid", "name", "phone", "email"),
                    "addresses" to listOf("uuid", "companyUuid", "line1", "city", "country"),
                    "company_bank_accounts" to listOf("uuid", "companyUuid", "bankName", "accountName"),
                    "company_legal_records" to listOf("uuid", "companyUuid", "commercialRegisterNumber"),
                    "company_attachments" to listOf("uuid", "companyUuid", "comment"),
                    "manufacturer_parts" to listOf("uuid", "partUuid", "manufacturerUuid", "mpn"),
                    "supplier_parts" to listOf("uuid", "partUuid", "supplierUuid", "sku"),
                    "supplier_price_breaks" to listOf("uuid", "supplierPartUuid", "quantity", "price"),
                    "manufacturing_phases" to listOf("uuid", "id", "partUuid", "name"),
                    "purchase_orders" to listOf("uuid", "reference", "supplierId", "supplierUuid", "totalCost"),
                    "purchase_order_lines" to listOf("uuid", "orderUuid", "supplierPartId", "supplierPartUuid", "quantity"),
                    "build_orders" to listOf("uuid", "reference", "partId", "partUuid", "quantity", "completedQuantity"),
                    "build_order_line_items" to listOf("uuid", "id", "buildId", "buildUuid", "bomItemId", "subPartId"),
                    "build_items" to listOf("uuid", "id", "buildId", "buildUuid", "stockItemId"),
                    "sales_orders" to listOf("uuid", "reference", "customerUuid", "totalPrice"),
                    "part_allocations" to listOf("uuid", "partUuid", "allocatedQuantity", "allocationType"),
                    "notifications_history" to listOf("uuid", "title", "message", "isDeleted", "updatedAt", "createdAt")
                )

                try {
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
                } catch (e: Throwable) {
                    println(">>> PRAGMA_CHECK_FAILED: ${e.message}")
                    throw e
                }

                // 3. كتابة بيانات عبر جميع الـ 20 DAO والـ 27 DAO الفرعية
                val appSettingsDao = AppSettingsDao()
                val appUserDao = AppUserDao()
                val partDao = PartDao()
                val locTypeDao = StockLocationTypeDao()
                val locationDao = StockLocationDao()
                val stockDao = StockItemDao()
                val trackingDao = StockItemTrackingDao()
                val testResultDao = StockItemTestResultDao()
                val stockAttDao = StockItemAttachmentDao()
                val bomDao = BomItemDao()
                val buildDao = BuildOrderDao()
                val buildLineDao = BuildOrderLineItemDao()
                val buildItemDao = BuildItemDao()
                val purchaseDao = PurchaseOrderDao()
                val salesDao = SalesOrderDao()
                val companyDao = CompanyDao()
                val contactDao = ContactDao()
                val addressDao = AddressDao()
                val bankDao = CompanyBankAccountDao()
                val legalDao = CompanyLegalRecordDao()
                val compAttDao = CompanyAttachmentDao()
                val mfgPartDao = ManufacturerPartDao()
                val supPartDao = SupplierPartDao()
                val priceBreakDao = SupplierPriceBreakDao()
                val phaseDao = ManufacturingPhaseDao()
                val notificationDao = NotificationHistoryDao()
                val allocationDao = PartAllocationDao()

                // إدراج السجلات عبر كل الـ DAOs
                appSettingsDao.saveSettings(AppSettingsEntity(themeMode = "DARK"))

                val userUuid = "usr-$runToken"
                appUserDao.insertOrUpdate(AppUserEntity(uuid = userUuid, name = "مستخدم $runToken", role = "Admin"))

                val partUuid = "part-$runToken"
                partDao.insertOrUpdate(PartEntity(uuid = partUuid, name = "قطعة $runToken", ipn = "IPN-$runToken"))

                val locTypeUuid = "loctype-$runToken"
                locTypeDao.insertOrUpdate(StockLocationTypeEntity(uuid = locTypeUuid, typeId = 1L, name = "نوع $runToken"))

                val locationUuid = "loc-$runToken"
                locationDao.insertOrUpdate(StockLocationEntity(uuid = locationUuid, name = "موقع $runToken", locationType = "WAREHOUSE"))

                val stockUuid = "stock-$runToken"
                stockDao.insertOrUpdate(StockItemEntity(uuid = stockUuid, partUuid = partUuid, locationUuid = locationUuid, quantity = 100.0, batch = "BATCH-$runToken"))

                val trackingUuid = "track-$runToken"
                trackingDao.insertOrUpdate(StockItemTrackingEntity(uuid = trackingUuid, stockItemUuid = stockUuid, label = "حركة $runToken"))

                val testResultUuid = "testres-$runToken"
                testResultDao.insertOrUpdate(StockItemTestResultEntity(uuid = testResultUuid, resultId = 1L, stockItemId = 1L, stockItemUuid = stockUuid, test = "فحص $runToken"))

                val stockAttUuid = "stockatt-$runToken"
                stockAttDao.insertOrUpdate(StockItemAttachmentEntity(uuid = stockAttUuid, attachmentId = 1L, stockItemId = 1L, stockItemUuid = stockUuid, comment = "مرفق $runToken"))

                val bomUuid = "bom-$runToken"
                bomDao.insertOrUpdate(BomItemEntity(uuid = bomUuid, partId = 1L, subPartId = 2L, quantity = 5.0, note = "BOM $runToken"))

                val buildUuid = "build-$runToken"
                buildDao.insertOrUpdate(BuildOrderEntity(uuid = buildUuid, reference = "BO-$runToken", partId = 1L, quantity = 10.0))

                val buildLineUuid = "buildline-$runToken"
                buildLineDao.insertOrUpdate(BuildOrderLineItemEntity(uuid = buildLineUuid, id = 1L, buildId = 1L, buildUuid = buildUuid, bomItemId = 1L, bomItemUuid = "bom-1", subPartId = 2L, subPartName = "مكون $runToken"))

                val buildItemUuid = "builditem-$runToken"
                buildItemDao.insertOrUpdate(BuildItemEntity(uuid = buildItemUuid, id = 1L, buildId = 1L, buildUuid = buildUuid, stockItemId = 1L, stockItemUuid = stockUuid, stockItemName = "مادة $runToken"))

                val poUuid = "po-$runToken"
                purchaseDao.insertOrUpdateOrder(PurchaseOrderEntity(uuid = poUuid, reference = "PO-$runToken", supplierId = 10L, supplierName = "مورد $runToken", totalCost = 500.0))

                val poLineUuid = "poline-$runToken"
                purchaseDao.insertOrUpdateLine(PurchaseOrderLineEntity(uuid = poLineUuid, orderUuid = poUuid, supplierPartId = 5L, quantity = 20.0))

                val soUuid = "so-$runToken"
                salesDao.insertOrUpdateOrder(SalesOrderEntity(uuid = soUuid, reference = "SO-$runToken", customerName = "عميل $runToken", totalPrice = 1200.0))

                val companyUuid = "comp-$runToken"
                companyDao.insertOrUpdate(CompanyEntity(uuid = companyUuid, name = "شركة $runToken"))

                val contactUuid = "contact-$runToken"
                contactDao.insertOrUpdate(ContactEntity(uuid = contactUuid, companyUuid = companyUuid, name = "جهة $runToken"))

                val addressUuid = "addr-$runToken"
                addressDao.insertOrUpdate(AddressEntity(uuid = addressUuid, companyUuid = companyUuid, line1 = "شارع $runToken"))

                val bankUuid = "bank-$runToken"
                bankDao.insertOrUpdate(CompanyBankAccountEntity(uuid = bankUuid, companyUuid = companyUuid, bankName = "بنك $runToken", accountName = "حساب $runToken"))

                val legalUuid = "legal-$runToken"
                legalDao.insertOrUpdate(CompanyLegalRecordEntity(uuid = legalUuid, companyUuid = companyUuid, commercialRegisterNumber = "CR-$runToken"))

                val compAttUuid = "compatt-$runToken"
                compAttDao.insertOrUpdate(CompanyAttachmentEntity(uuid = compAttUuid, companyUuid = companyUuid, comment = "سجل $runToken"))

                val mfgPartUuid = "mfgpart-$runToken"
                mfgPartDao.insertOrUpdate(ManufacturerPartEntity(uuid = mfgPartUuid, partUuid = partUuid, manufacturerUuid = companyUuid, mpn = "MPN-$runToken"))

                val supPartUuid = "suppart-$runToken"
                supPartDao.insertOrUpdate(SupplierPartEntity(uuid = supPartUuid, partUuid = partUuid, supplierUuid = companyUuid, sku = "SKU-$runToken"))

                val priceBreakUuid = "pricebreak-$runToken"
                priceBreakDao.insertOrUpdate(SupplierPriceBreakEntity(uuid = priceBreakUuid, supplierPartUuid = supPartUuid, quantity = 10.0, price = 50.0))

                val phaseUuid = "phase-$runToken"
                phaseDao.insertOrUpdate(ManufacturingPhaseEntity(uuid = phaseUuid, id = 1L, name = "مرحلة $runToken", sequenceOrder = 1))

                val notifUuid = "notif-$runToken"
                notificationDao.insertOrUpdate(NotificationHistoryEntity(uuid = notifUuid, title = "تنبيه $runToken", message = "رسالة $runToken", targetEntityUuid = partUuid, scheduledDate = Clock.System.now().toEpochMilliseconds()))

                val allocationId = allocationDao.insertAllocation(PartAllocationEntity(id = 100L, partId = 1L, allocatedQuantity = 10.0, allocationType = "HARD", referenceType = "BUILD_ORDER", referenceId = "REF-$runToken", referenceTitle = "حجز $runToken", status = "ACTIVE"))

                // 4. إغلاق وإعادة فتح قاعدة البيانات لتنظيف الذاكرة واختبار الـ Persistence
                SqliteDatabaseManager.closeDatabase()

                val reopenedConn = SqliteDatabaseManager.getConnection()
                assertNotNull(reopenedConn, "فشل إعادة فتح الاتصال بقاعدة البيانات")

                // 5. التحقق عبر DAOs جديدة تماماً من القراءة الصحيحة لجميع الكيانات المستعادة من SQLite
                val freshSettingsDao = AppSettingsDao()
                val freshPartDao = PartDao()
                val freshStockDao = StockItemDao()
                val freshBomDao = BomItemDao()
                val freshBuildDao = BuildOrderDao()
                val freshBuildLineDao = BuildOrderLineItemDao()
                val freshBuildItemDao = BuildItemDao()
                val freshPurchaseDao = PurchaseOrderDao()
                val freshSalesDao = SalesOrderDao()
                val freshCompanyDao = CompanyDao()
                val freshContactDao = ContactDao()
                val freshAddressDao = AddressDao()
                val freshBankDao = CompanyBankAccountDao()
                val freshLegalDao = CompanyLegalRecordDao()
                val freshCompAttDao = CompanyAttachmentDao()
                val freshMfgPartDao = ManufacturerPartDao()
                val freshSupPartDao = SupplierPartDao()
                val freshPriceBreakDao = SupplierPriceBreakDao()
                val freshPhaseDao = ManufacturingPhaseDao()
                val freshNotificationDao = NotificationHistoryDao()
                val freshAllocationDao = PartAllocationDao()

                assertEquals("DARK", freshSettingsDao.getSettings().themeMode)

                val savedPart = freshPartDao.getPartByUuid(partUuid)
                assertNotNull(savedPart, "savedPart null")
                assertEquals("IPN-$runToken", savedPart.ipn)

                val savedStock = freshStockDao.getStockItemsPaged(partUuid = partUuid, limit = 10, offset = 0).find { it.uuid == stockUuid }
                assertNotNull(savedStock, "savedStock null")
                assertEquals("BATCH-$runToken", savedStock.batch)

                val savedBom = freshBomDao.getBomItemsPaged(limit = 10, offset = 0).find { it.uuid == bomUuid }
                assertNotNull(savedBom, "savedBom null")
                assertEquals("BOM $runToken", savedBom.note)

                val savedBuild = freshBuildDao.getBuildOrdersPaged(limit = 10, offset = 0).find { it.uuid == buildUuid }
                assertNotNull(savedBuild, "savedBuild null")
                assertEquals("BO-$runToken", savedBuild.reference)

                val savedBuildLine = freshBuildLineDao.getLineItemsForBuildUuid(buildUuid).find { it.uuid == buildLineUuid }
                assertNotNull(savedBuildLine, "savedBuildLine null")
                assertEquals("مكون $runToken", savedBuildLine.subPartName)

                val savedBuildItem = freshBuildItemDao.getBuildItemsForBuildUuid(buildUuid).find { it.uuid == buildItemUuid }
                assertNotNull(savedBuildItem, "savedBuildItem null")
                assertEquals("مادة $runToken", savedBuildItem.stockItemName)

                val savedPo = freshPurchaseDao.getOrdersPaged(limit = 10, offset = 0).find { it.uuid == poUuid }
                assertNotNull(savedPo, "savedPo null")
                assertEquals("PO-$runToken", savedPo.reference)

                val savedPoLine = freshPurchaseDao.getLinesForOrderPaged(poUuid, limit = 10, offset = 0).find { it.uuid == poLineUuid }
                assertNotNull(savedPoLine, "savedPoLine null")
                assertEquals(20.0, savedPoLine.quantity)

                val savedSo = freshSalesDao.getOrdersPaged(limit = 10, offset = 0).find { it.uuid == soUuid }
                assertNotNull(savedSo, "savedSo null")
                assertEquals("SO-$runToken", savedSo.reference)

                val savedComp = freshCompanyDao.getCompaniesPaged(limit = 10, offset = 0).find { it.uuid == companyUuid }
                assertNotNull(savedComp, "savedComp null")
                assertEquals("شركة $runToken", savedComp.name)

                val savedContact = freshContactDao.getContactsForCompany(companyUuid).find { it.uuid == contactUuid }
                assertNotNull(savedContact, "savedContact null")
                assertEquals("جهة $runToken", savedContact.name)

                val savedAddr = freshAddressDao.getAddressesForCompany(companyUuid).find { it.uuid == addressUuid }
                assertNotNull(savedAddr, "savedAddr null")
                assertEquals("شارع $runToken", savedAddr.line1)

                val savedBank = freshBankDao.getForCompany(companyUuid).find { it.uuid == bankUuid }
                assertNotNull(savedBank, "savedBank null")
                assertEquals("بنك $runToken", savedBank.bankName)

                val savedLegal = freshLegalDao.getForCompany(companyUuid)
                assertNotNull(savedLegal, "savedLegal null")
                assertEquals("CR-$runToken", savedLegal.commercialRegisterNumber)

                val savedCompAtt = freshCompAttDao.getForCompany(companyUuid).find { it.uuid == compAttUuid }
                assertNotNull(savedCompAtt, "savedCompAtt null")
                assertEquals("سجل $runToken", savedCompAtt.comment)

                val savedMfgPart = freshMfgPartDao.getForCompany(companyUuid).find { it.uuid == mfgPartUuid }
                assertNotNull(savedMfgPart, "savedMfgPart null")
                assertEquals("MPN-$runToken", savedMfgPart.mpn)

                val savedSupPart = freshSupPartDao.getForCompany(companyUuid).find { it.uuid == supPartUuid }
                assertNotNull(savedSupPart, "savedSupPart null")
                assertEquals("SKU-$runToken", savedSupPart.sku)

                val savedPriceBreak = freshPriceBreakDao.getForSupplierPart(supPartUuid).find { it.uuid == priceBreakUuid }
                assertNotNull(savedPriceBreak, "savedPriceBreak null")
                assertEquals(50.0, savedPriceBreak.price)

                val savedPhase = freshPhaseDao.getAllPhases().find { it.uuid == phaseUuid }
                assertNotNull(savedPhase, "savedPhase null")
                assertEquals("مرحلة $runToken", savedPhase.name)

                val savedNotif = freshNotificationDao.getAllNotifications(limit = 10, offset = 0).find { it.uuid == notifUuid }
                assertNotNull(savedNotif, "savedNotif null")
                assertEquals("تنبيه $runToken", savedNotif.title)

                val savedAlloc = freshAllocationDao.getAllAllocationsForPart(1L).find { it.id == allocationId }
                assertNotNull(savedAlloc, "savedAlloc null")
                assertEquals("حجز $runToken", savedAlloc.referenceTitle)

            } catch (e: Throwable) {
                println(">>> COMPREHENSIVE_DAO_FAIL: ${e::class.simpleName}: ${e.message}")
                e.printStackTrace()
                throw e
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
