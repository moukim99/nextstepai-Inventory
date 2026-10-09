package com.nextstepai.inventory

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.NotificationHistoryEntity
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.repository.*
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * اختبار الـ Verification الشامل لطبقة الـ Repositories فوق قاعدة بيانات SQLite:
 * 1. كتابة البيانات باستخدام Repositories أولية في ملف قاعدة بيانات مؤقت معزول.
 * 2. إغلاق قاعدة البيانات لتصفير كافة الكائنات والذاكرة المؤقتة (Clear In-Memory State).
 * 3. إعادة فتح قاعدة البيانات من القرص الصلب.
 * 4. إنشاء كائنات Repositories جديدة بالكامل.
 * 5. التأكد من أن الـ Repositories تسترجع البيانات مباشرة ودون أي فقدان من قاعدة بيانات SQLite.
 */
class RepositoryPersistenceVerificationTest {

    @Test
    fun testRepositoriesPersistenceAcrossDatabaseReopen() {
        runBlocking {
            val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
            val tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_repo_persistence_$runToken.db")
            if (tempDbFile.exists()) tempDbFile.delete()
            tempDbFile.deleteOnExit()

            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)

            try {
                // Step 0: Ensure database is initialized
                val conn = SqliteDatabaseManager.getConnection()
                assertNotNull(conn, "فشل تهيئة الاتصال الأولي بقاعدة البيانات")

                // 1. إنشاء كائنات Repositories أولية
                val partRepo = PartRepository()
                val stockRepo = StockRepository()
                val bomRepo = BomRepository()
                val companyRepo = CompanyRepository()
                val allocationRepo = PartAllocationRepository()
                val notifRepo = NotificationRepository()

                // 2. إدخال سجلات عبر الـ Repositories
                val part = partRepo.addPart(
                    Part(
                        name = "قطعة اختبار المستودعات $runToken",
                        ipn = "REPO-IPN-$runToken",
                        description = "اختبار الـ Repo للجلسة $runToken",
                        component = true,
                        assembly = false
                    )
                )
                assertNotNull(part)

                val location = stockRepo.addLocation(
                    StockLocation(
                        name = "مستودع الـ Repo $runToken",
                        description = "مستودع معزول للجلسة $runToken",
                        locationType = "WAREHOUSE"
                    )
                )
                assertNotNull(location)

                val stockItem = stockRepo.addStockItem(
                    StockItem(
                        partId = part.id,
                        locationId = location.id,
                        quantity = 75.0,
                        batch = "BATCH-REPO-$runToken"
                    )
                )
                assertNotNull(stockItem)

                val subPart = partRepo.addPart(
                    Part(
                        name = "مكون فرعي للـ Repo $runToken",
                        ipn = "SUB-IPN-$runToken",
                        description = "مكون فرعي للجلسة $runToken",
                        component = true,
                        assembly = false
                    )
                )
                assertNotNull(subPart)

                val bomItem = bomRepo.addBomItem(
                    BomItem(
                        partId = part.id,
                        subPartId = subPart.id,
                        quantity = 2.0,
                        note = "BOM REPO $runToken"
                    )
                )
                assertNotNull(bomItem)

                val company = companyRepo.addCompany(
                    Company(
                        name = "شركة الـ Repo $runToken",
                        description = "شركة اختبار $runToken",
                        isSupplier = true
                    )
                )
                assertNotNull(company)

                val allocationResult = allocationRepo.createAllocation(
                    partId = part.id,
                    quantity = 10.0,
                    type = "HARD",
                    refType = "BUILD_ORDER",
                    refId = "REF-$runToken",
                    refTitle = "حجز الـ Repo $runToken"
                )
                assertTrue(allocationResult.isSuccess, "فشل إنشاء حجز المخزون عبر PartAllocationRepository")

                val notifUuid = "notif-repo-$runToken"
                notifRepo.saveNotification(
                    NotificationHistoryEntity(
                        uuid = notifUuid,
                        title = "تنبيه الـ Repo $runToken",
                        message = "رسالة اختبار $runToken",
                        targetEntityUuid = part.effectiveUuid,
                        scheduledDate = Clock.System.now().toEpochMilliseconds()
                    )
                )

                // 3. إغلاق قاعدة البيانات لتفريغ الاتصال والكائنات المؤقتة
                SqliteDatabaseManager.closeDatabase()

                // 4. إعادة فتح قاعدة البيانات من القرص الصلب المعزول
                val reopenedConn = SqliteDatabaseManager.getConnection()
                assertNotNull(reopenedConn, "فشل إعادة فتح قاعدة البيانات المعزولة من القرص")

                // 5. إنشاء كائنات Repositories جديدة بالكامل (Fresh Repository Instances)
                val freshPartRepo = PartRepository()
                val freshStockRepo = StockRepository()
                val freshBomRepo = BomRepository()
                val freshCompanyRepo = CompanyRepository()
                val freshAllocationRepo = PartAllocationRepository()
                val freshNotifRepo = NotificationRepository()

                // 6. التحقق من استرجاع كامل السجلات والبيانات عبر الكائنات الجديدة من SQLite
                val savedParts = freshPartRepo.getParts()
                val savedPart = savedParts.find { it.ipn == "REPO-IPN-$runToken" }
                assertNotNull(savedPart, "فشل PartRepository الجديد في استرجاع القطعة من SQLite")
                assertEquals("قطعة اختبار المستودعات $runToken", savedPart.name)

                val savedLocations = freshStockRepo.getLocations()
                val savedLocation = savedLocations.find { it.name == "مستودع الـ Repo $runToken" }
                assertNotNull(savedLocation, "فشل StockRepository الجديد في استرجاع المستودع من SQLite")
                assertEquals("WAREHOUSE", savedLocation.locationType)

                val savedStockItems = freshStockRepo.getStockItems()
                val savedStock = savedStockItems.find { it.batch == "BATCH-REPO-$runToken" }
                assertNotNull(savedStock, "فشل StockRepository الجديد في استرجاع عناصر المخزون من SQLite")
                assertEquals(75.0, savedStock.quantity)

                val savedBomItems = freshBomRepo.getBomItemsForPart(part.id)
                assertTrue(savedBomItems.isNotEmpty(), "فشل BomRepository الجديد في استرجاع بنود الـ BOM من SQLite")
                assertEquals("BOM REPO $runToken", savedBomItems.first().note)

                val savedCompanies = freshCompanyRepo.searchCompanies("شركة الـ Repo $runToken")
                assertTrue(savedCompanies.isNotEmpty(), "فشل CompanyRepository الجديد في استرجاع الشركة من SQLite")

                val savedAllocations = freshAllocationRepo.getActiveAllocationsForPart(part.id)
                assertTrue(savedAllocations.isNotEmpty(), "فشل PartAllocationRepository الجديد في استرجاع الحجوزات من SQLite")
                assertEquals("حجز الـ Repo $runToken", savedAllocations.first().referenceTitle)

                val savedNotifs = freshNotifRepo.getAllNotifications(limit = 10, offset = 0)
                val savedNotif = savedNotifs.find { it.uuid == notifUuid }
                assertNotNull(savedNotif, "فشل NotificationRepository الجديد في استرجاع الإشعار من SQLite")
                assertEquals("تنبيه الـ Repo $runToken", savedNotif.title)

            } finally {
                SqliteDatabaseManager.closeDatabase()
                SqliteDatabaseManager.setCustomDatabasePath(null)
                runCatching { tempDbFile.delete() }
            }
        }
    }
}
