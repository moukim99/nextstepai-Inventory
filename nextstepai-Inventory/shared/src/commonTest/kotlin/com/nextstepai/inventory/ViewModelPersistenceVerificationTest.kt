package com.nextstepai.inventory

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.ui.*
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * اختبار الـ Verification الشامل لتدفق البيانات الإند-تو-إند (End-to-End ViewModel Persistence Test):
 * المسار المستهدف: UI / ViewModel -> Repository -> DAO -> SQLite -> DAO -> Repository -> ViewModel -> UI
 *
 * أهداف الاختبار:
 * 1. التأكد من أن الـ ViewModels تكتب وتستعلم مباشرة عبر الـ Repositories إلى قاعدة بيانات SQLite.
 * 2. التأكد من أن الـ UI State في الـ ViewModels تعكس بصدق وبدون الاعتماد على ذاكرة مؤقتة زائفة البيانات الدائمة.
 * 3. تصفير الاتصال وإغلاق قاعدة البيانات (Cold Restart).
 * 4. إعادة إنشاء ViewModels جديدة تماماً والتأكد من استرجاع كامل حالة الشاشات من SQLite.
 * 5. التأكد من أن عمليات الحذف والتعديل (Create/Update/Delete) تستمر وتستقر عبر قاعدة البيانات بدون عودة البيانات المحذوفة.
 */
class ViewModelPersistenceVerificationTest {

    @Test
    fun testViewModelsEndToEndPersistenceAcrossDatabaseReopen() {
        runBlocking {
            val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
            val tempDbFile = File(System.getProperty("java.io.tmpdir"), "test_vm_persistence_$runToken.db")
            if (tempDbFile.exists()) tempDbFile.delete()
            tempDbFile.deleteOnExit()

            SqliteDatabaseManager.setCustomDatabasePath(tempDbFile.absolutePath)

            try {
                // 1. تهيئة الاتصال بقاعدة البيانات المعزولة
                val conn = SqliteDatabaseManager.getConnection()
                assertNotNull(conn, "فشل تهيئة الاتصال الأول لقاعدة البيانات المعزولة")

                // 2. إنشاء ViewModels الجيل الأول
                val partVm = PartViewModel()
                val stockVm = StockViewModel()
                val bomVm = BomViewModel()
                val companyVm = CompanyViewModel()
                val buildVm = BuildOrderViewModel()
                val purchaseVm = PurchaseOrderViewModel()

                // 3. تنفيذ عمليات إضافة (Create) عبر الـ ViewModels والانتظار لاستقرار الشحنة على الـ StateFlow
                partVm.addNewPart(
                    name = "قطعة شاشة الـ ViewModel $runToken",
                    ipn = "VM-PART-$runToken",
                    description = "وصف قطعة الـ ViewModel للجلسة $runToken"
                )
                partVm.loadData()
                awaitCondition("partVm-createdPart") { partVm.uiState.value.parts.any { it.ipn == "VM-PART-$runToken" } }

                val createdPart = partVm.uiState.value.parts.find { it.ipn == "VM-PART-$runToken" }
                assertNotNull(createdPart, "فشل PartViewModel في حفظ وإظهار القطعة الجديدة")

                partVm.addNewPart(
                    name = "مكون فرعي للـ VM $runToken",
                    ipn = "VM-SUB-$runToken",
                    description = "مكون فرعي للجلسة $runToken"
                )
                partVm.loadData()
                awaitCondition("partVm-createdSubPart") { partVm.uiState.value.parts.any { it.ipn == "VM-SUB-$runToken" } }

                val createdSubPart = partVm.uiState.value.parts.find { it.ipn == "VM-SUB-$runToken" }
                assertNotNull(createdSubPart, "فشل PartViewModel في حفظ المكون الفرعي")

                stockVm.addLocation(
                    name = "موقع شاشة الـ VM $runToken",
                    description = "موقع معزول للـ ViewModel $runToken",
                    locationType = "WAREHOUSE"
                )
                stockVm.loadData()
                awaitCondition("stockVm-createdLocation") { stockVm.uiState.value.locations.any { it.name == "موقع شاشة الـ VM $runToken" } }

                val createdLocation = stockVm.uiState.value.locations.find { it.name == "موقع شاشة الـ VM $runToken" }
                assertNotNull(createdLocation, "فشل StockViewModel في حفظ وإظهار موقع التخزين الجديد")

                stockVm.addStockItem(
                    partId = createdPart.id,
                    locationId = createdLocation.id,
                    quantity = 120.0,
                    batch = "BATCH-VM-$runToken"
                )
                stockVm.loadData()
                awaitCondition("stockVm-createdStock") { stockVm.uiState.value.stockItems.any { it.batch == "BATCH-VM-$runToken" } }

                val createdStock = stockVm.uiState.value.stockItems.find { it.batch == "BATCH-VM-$runToken" }
                assertNotNull(createdStock, "فشل StockViewModel في حفظ عنصر المخزون الجديد")

                companyVm.addCompany(
                    name = "شركة شاشة الـ VM $runToken",
                    description = "وصف شركة الـ ViewModel",
                    website = "https://example.com",
                    phone = "+966500000000",
                    email = "vm@example.com",
                    address = "الرياض",
                    contact = "مدير المشتريات",
                    isSupplier = true,
                    isManufacturer = false,
                    isCustomer = false,
                    currency = "USD"
                )
                companyVm.loadData()
                awaitCondition("companyVm-createdCompany") { companyVm.uiState.value.companies.any { it.name == "شركة شاشة الـ VM $runToken" } }

                val createdCompany = companyVm.uiState.value.companies.find { it.name == "شركة شاشة الـ VM $runToken" }
                assertNotNull(createdCompany, "فشل CompanyViewModel في حفظ وإظهار الشركة الجديدة")

                bomVm.selectParentPart(createdPart.id)
                bomVm.addBomItem(
                    subPartId = createdSubPart.id,
                    quantity = 4.0,
                    reference = "REF-BOM-$runToken",
                    optional = false,
                    consumable = false,
                    allowVariants = false,
                    inherited = false,
                    note = "BOM VM $runToken"
                )
                awaitCondition("bomVm-createdBom") { bomVm.uiState.value.bomItems.any { it.note == "BOM VM $runToken" } }

                val createdBom = bomVm.uiState.value.bomItems.find { it.note == "BOM VM $runToken" }
                assertNotNull(createdBom, "فشل BomViewModel في حفظ بند قائمة المواد BOM")

                buildVm.addBuildOrder(
                    reference = "BO-VM-$runToken",
                    title = "عنوان إشارة الـ VM $runToken",
                    partId = createdPart.id,
                    quantity = 15.0,
                    batch = "BATCH-BUILD-$runToken",
                    targetDate = "2026-12-31"
                )
                buildVm.loadData()
                awaitCondition("buildVm-createdBuild") { buildVm.uiState.value.builds.any { it.reference == "BO-VM-$runToken" } }

                val createdBuild = buildVm.uiState.value.builds.find { it.reference == "BO-VM-$runToken" }
                assertNotNull(createdBuild, "فشل BuildOrderViewModel في حفظ أمر الإنتاج")

                purchaseVm.addPurchaseOrder(
                    reference = "PO-VM-$runToken",
                    supplierId = createdCompany.id,
                    description = "أمر شراء الـ ViewModel $runToken",
                    targetDate = "2026-12-31",
                    currency = "USD"
                )
                purchaseVm.loadData()
                awaitCondition("purchaseVm-createdPo") { purchaseVm.uiState.value.orders.any { it.reference == "PO-VM-$runToken" } }

                val createdPo = purchaseVm.uiState.value.orders.find { it.reference == "PO-VM-$runToken" }
                assertNotNull(createdPo, "فشل PurchaseOrderViewModel في حفظ أمر الشراء")

                // 4. إغلاق قاعدة البيانات بالكامل وتصفير الذاكرة (Cold Restart)
                SqliteDatabaseManager.closeDatabase()

                // 5. إعادة فتح الاتصال من ملف SQLite المعزول
                val reopenedConn = SqliteDatabaseManager.getConnection()
                assertNotNull(reopenedConn, "فشل إعادة فتح قاعدة البيانات بعد الـ Cold Restart")

                // 6. إنشاء ViewModels جديدة تماماً (Fresh ViewModels) لمحاكاة إعادة فتح التطبيق
                val freshPartVm = PartViewModel()
                freshPartVm.loadData()
                awaitCondition("freshPartVm-restoredPart") { freshPartVm.uiState.value.parts.any { it.ipn == "VM-PART-$runToken" } }

                val freshStockVm = StockViewModel()
                freshStockVm.loadData()
                awaitCondition("freshStockVm-restoredStock") { freshStockVm.uiState.value.stockItems.any { it.batch == "BATCH-VM-$runToken" } }

                val freshBomVm = BomViewModel()

                val freshCompanyVm = CompanyViewModel()
                freshCompanyVm.loadData()
                awaitCondition("freshCompanyVm-restoredCompany") { freshCompanyVm.uiState.value.companies.any { it.name == "شركة شاشة الـ VM $runToken" } }

                val freshBuildVm = BuildOrderViewModel()
                freshBuildVm.loadData()
                awaitCondition("freshBuildVm-restoredBuild") { freshBuildVm.uiState.value.builds.any { it.reference == "BO-VM-$runToken" } }

                val freshPurchaseVm = PurchaseOrderViewModel()
                freshPurchaseVm.loadData()
                awaitCondition("freshPurchaseVm-restoredPo") { freshPurchaseVm.uiState.value.orders.any { it.reference == "PO-VM-$runToken" } }

                // 7. التحقق من استعادة كل حالة واجهة المستخدم مباشرة من قاعدة بيانات SQLite
                val restoredPart = freshPartVm.uiState.value.parts.find { it.ipn == "VM-PART-$runToken" }
                assertNotNull(restoredPart, "PartViewModel الجديد لم يسترجع القطعة المخزنة في SQLite")
                assertEquals("قطعة شاشة الـ ViewModel $runToken", restoredPart.name)

                val restoredLocation = freshStockVm.uiState.value.locations.find { it.name == "موقع شاشة الـ VM $runToken" }
                assertNotNull(restoredLocation, "StockViewModel الجديد لم يسترجع موقع التخزين من SQLite")

                val restoredStock = freshStockVm.uiState.value.stockItems.find { it.batch == "BATCH-VM-$runToken" }
                assertNotNull(restoredStock, "StockViewModel الجديد لم يسترجع عنصر المخزون من SQLite")
                assertEquals(120.0, restoredStock.quantity)

                freshBomVm.selectParentPart(restoredPart.id)
                awaitCondition("freshBomVm-restoredBom") { freshBomVm.uiState.value.bomItems.any { it.note == "BOM VM $runToken" } }
                val restoredBom = freshBomVm.uiState.value.bomItems.find { it.note == "BOM VM $runToken" }
                assertNotNull(restoredBom, "BomViewModel الجديد لم يسترجع بند BOM من SQLite")
                assertEquals(4.0, restoredBom.quantity)

                val restoredCompany = freshCompanyVm.uiState.value.companies.find { it.name == "شركة شاشة الـ VM $runToken" }
                assertNotNull(restoredCompany, "CompanyViewModel الجديد لم يسترجع الشركة من SQLite")

                val restoredBuild = freshBuildVm.uiState.value.builds.find { it.reference == "BO-VM-$runToken" }
                assertNotNull(restoredBuild, "BuildOrderViewModel الجديد لم يسترجع أمر الإنتاج من SQLite")

                val restoredPo = freshPurchaseVm.uiState.value.orders.find { it.reference == "PO-VM-$runToken" }
                assertNotNull(restoredPo, "PurchaseOrderViewModel الجديد لم يسترجع أمر الشراء من SQLite")

                // 8. اختبار حذف عنصر عبر الـ ViewModel والتأكد من عدم العودة بعد إعادة الفتح
                freshPartVm.deletePart(restoredPart.id)
                freshPartVm.loadData()
                awaitCondition("freshPartVm-deletedPart") { freshPartVm.uiState.value.parts.none { it.id == restoredPart.id } }

                assertTrue(
                    freshPartVm.uiState.value.parts.none { it.id == restoredPart.id },
                    "فشل PartViewModel في إزالة القطعة المحذوفة من حالة الشاشة"
                )

                // إغلاق وإعادة فتح ثانية للتأكد المطلق من استمرار الحذف
                SqliteDatabaseManager.closeDatabase()
                SqliteDatabaseManager.getConnection()

                val reVerificationPartVm = PartViewModel()
                reVerificationPartVm.loadData()
                awaitCondition("reVerificationPartVm-notLoading") { !reVerificationPartVm.uiState.value.isLoading }

                assertTrue(
                    reVerificationPartVm.uiState.value.parts.none { it.id == restoredPart.id },
                    "القطعة المحذوفة عادت للظهور بعد إغلاق وإعادة فتح التطبيق ثانية!"
                )

            } finally {
                SqliteDatabaseManager.closeDatabase()
                SqliteDatabaseManager.setCustomDatabasePath(null)
                runCatching { tempDbFile.delete() }
            }
        }
    }

    private suspend fun awaitCondition(label: String, timeoutMs: Long = 4000, condition: () -> Boolean) {
        val startTime = Clock.System.now().toEpochMilliseconds()
        while (!condition()) {
            if (Clock.System.now().toEpochMilliseconds() - startTime > timeoutMs) {
                println(">>> TIMEOUT_FAILED_ON: $label")
                throw IllegalStateException("تجاوز الوقت المحدد (Timeout) بانتظار تحديث الـ ViewModel حالة الـ StateFlow [$label]")
            }
            delay(20)
        }
    }
}
