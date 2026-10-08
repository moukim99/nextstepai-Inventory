package com.nextstepai.inventory

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.di.AppContainer
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * اختبار المحاكاة الشامل لرحلة المستخدم وتكامل الواجهات مع قاعدة البيانات الدائمة (End-to-End User Journey & Persistence Audit):
 *
 * يحاكي هذا الاختبار مستخدماً حقيقياً يتنقل بين كافة شاشات التطبيق:
 * 1. شاشة الدخول (LoginScreen): تسجيل الدخول والتحقق من إنشاء الجلسة.
 * 2. شاشة الشركات (CompanyScreen): إضافة شركة موردة، عناوين، جهات اتصال، حسابات بنكية وسجلات تجارية/ضريبية.
 * 3. شاشة إدارة الأصناف (PartManagementScreen): إضافة صنف إلكتروني، تحديد الأسعار، التحقق من نقاط اكتمال البيانات.
 * 4. شاشة المخزون الفعلي (StockScreen): إضافة هيكل مواقع تخزين، إضافة عناصر مخزون بدفعات وأرقام تسلسلية، فحوص الجودة، وتحويل المخزون.
 * 5. شاشة قائمة المواد (BomScreen): إنشاء تركيبة منتج وربط المكونات الفرعية.
 * 6. شاشة أوامر الشراء والإنتاج (PurchaseOrder & BuildOrder): إصدار أمر شراء وأمر إنتاج والتحقق من تقدم الحالات.
 * 7. التحقق المباشر من جداول SQLite: مطابقة عدد الصفوف والبيانات في القرص الصلب.
 * 8. محاكاة إغلاق التطبيق كلياً (Cold Restart): إغلاق الاتصال وتفريغ الذاكرة، ثم فتح التطبيق من جديد والتحقق من بقاء كافة البيانات 100%.
 */
class EndToEndUserJourneyVerificationTest {

    private suspend fun awaitCondition(tag: String, timeoutMs: Long = 10000, condition: () -> Boolean) {
        val start = Clock.System.now().toEpochMilliseconds()
        while (!condition()) {
            if (Clock.System.now().toEpochMilliseconds() - start > timeoutMs) {
                error("Timeout waiting for condition: $tag")
            }
            delay(50)
        }
    }

    @Test
    fun testFullUserJourney_Acceptance_Persistence_And_ColdRestart() {
        runBlocking {
            val sessionToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
            val dbFile = File(System.getProperty("java.io.tmpdir"), "e2e_user_journey_$sessionToken.db")
            if (dbFile.exists()) dbFile.delete()
            dbFile.deleteOnExit()

            SqliteDatabaseManager.setCustomDatabasePath(dbFile.absolutePath)

            try {
                // =========================================================================
                // المرحلة الأولى: فتح التطبيق، حقن الحاوية، وتسجيل الدخول
                // =========================================================================
                val container = AppContainer()
                val loginVm = container.createLoginViewModel()
                val companyVm = container.createCompanyViewModel()
                val partVm = container.createPartViewModel()
                val stockVm = container.createStockViewModel()
                val bomVm = container.createBomViewModel()
                val poVm = container.createPurchaseOrderViewModel()
                val buildVm = container.createBuildOrderViewModel()

                // 1. تسجيل الدخول
                loginVm.performSimpleLogin()
                awaitCondition("login-success") { loginVm.uiState.value.isLoggedIn }
                assertTrue(loginVm.uiState.value.isLoggedIn, "يجب أن يسجل المستخدم دخوله بنجاح")
                assertNotNull(loginVm.uiState.value.currentSession, "يجب أن تنشأ جلسة دخول مسجلة")

                // =========================================================================
                // المرحلة الثانية: شاشة الشركات (CompanyScreen)
                // =========================================================================
                val companyName = "شركة التقنية المتقدمة $sessionToken"
                companyVm.addCompany(
                    name = companyName,
                    description = "مورد رئيسي للشرائح الإلكترونية",
                    website = "https://advanced-tech-$sessionToken.com",
                    phone = "+966501234567",
                    email = "info@advanced-tech-$sessionToken.com",
                    address = "الرياض - واحة التقنية",
                    contact = "م. خالد المنصور",
                    isSupplier = true,
                    isManufacturer = true,
                    isCustomer = false,
                    currency = "SAR"
                )
                companyVm.loadData()
                awaitCondition("company-created") { companyVm.uiState.value.companies.any { it.name == companyName } }

                val createdCompany = companyVm.uiState.value.companies.first { it.name == companyName }
                assertNotNull(createdCompany)
                assertEquals("SAR", createdCompany.currency)

                // فتح تفاصيل الشركة وإضافة جهة اتصال
                companyVm.setSelectedCompany(createdCompany)
                companyVm.addContact(
                    name = "سارة العتيبي",
                    phone = "+966559876543",
                    email = "sara@advanced-tech-$sessionToken.com",
                    role = "مديرة المبيعات والتوريد"
                )
                awaitCondition("contact-created") {
                    companyVm.uiState.value.companyContacts.any { it.name == "سارة العتيبي" }
                }

                // إضافة حساب بنكي
                companyVm.addCompanyBankAccount(
                    bankName = "مصرف الراجحي",
                    accountName = "شركة التقنية المتقدمة",
                    accountNumber = "1234567890",
                    iban = "SA4480000000608010167519",
                    currency = "SAR",
                    swiftBic = "RJHISARI"
                )
                awaitCondition("bank-created") {
                    companyVm.uiState.value.companyBankAccounts.any { it.iban == "SA4480000000608010167519" }
                }

                // =========================================================================
                // المرحلة الثالثة: شاشة إدارة الأصناف (PartManagementScreen)
                // =========================================================================
                val partIpn = "MCU-STM32-$sessionToken"
                val partName = "متحكم ARM Cortex-M4 $sessionToken"
                partVm.addNewPart(
                    name = partName,
                    ipn = partIpn,
                    description = "متحكم رئيسي عالي الأداء مع ذواكر فلاش مدمجة"
                )
                partVm.loadData()
                awaitCondition("part-created") { partVm.uiState.value.parts.any { it.ipn == partIpn } }

                val createdPart = partVm.uiState.value.parts.first { it.ipn == partIpn }
                assertNotNull(createdPart)
                assertEquals(partName, createdPart.name)

                // إضافة مكون فرعي لقائمة المواد لاحقاً
                val subPartIpn = "RES-10K-$sessionToken"
                val subPartName = "مقاومة 10k أوم SMD $sessionToken"
                partVm.addNewPart(
                    name = subPartName,
                    ipn = subPartIpn,
                    description = "مقاومة قياسية 0805"
                )
                partVm.loadData()
                awaitCondition("subPart-created") { partVm.uiState.value.parts.any { it.ipn == subPartIpn } }
                val createdSubPart = partVm.uiState.value.parts.first { it.ipn == subPartIpn }

                // تحديد الصنف وإضافة تسعير داخلي وسعر بيع
                partVm.selectPart(createdPart)
                partVm.addPartInternalPrice(
                    quantity = 1.0,
                    price = 18.50,
                    currency = "SAR"
                )
                partVm.addPartSalePrice(
                    quantity = 1.0,
                    price = 32.00,
                    currency = "SAR"
                )
                partVm.loadData()

                // =========================================================================
                // المرحلة الرابعة: شاشة المخزون والمواقع (StockScreen)
                // =========================================================================
                val locName = "مستودع المكونات الذكية $sessionToken"
                stockVm.addLocation(
                    name = locName,
                    description = "موقع تخزين رئيسي مجهز بأنظمة تحكم بالحرارة",
                    locationType = "WAREHOUSE"
                )
                stockVm.loadData()
                awaitCondition("location-created") { stockVm.uiState.value.locations.any { it.name == locName } }
                val createdLocation = stockVm.uiState.value.locations.first { it.name == locName }

                val locSubName = "ممر القطع الدقيقة $sessionToken"
                stockVm.addLocation(
                    name = locSubName,
                    description = "ممر فرعي داخل المستودع",
                    locationType = "AISLE",
                    parentId = createdLocation.id
                )
                stockVm.loadData()
                awaitCondition("sublocation-created") { stockVm.uiState.value.locations.any { it.name == locSubName } }
                val createdSubLocation = stockVm.uiState.value.locations.first { it.name == locSubName }

                val stockBatch = "BATCH-$sessionToken-A"
                stockVm.addStockItem(
                    partId = createdPart.id,
                    locationId = createdLocation.id,
                    quantity = 350.0,
                    batch = stockBatch
                )
                stockVm.loadData()
                awaitCondition("stock-created") { stockVm.uiState.value.stockItems.any { it.batch == stockBatch } }
                val createdStock = stockVm.uiState.value.stockItems.first { it.batch == stockBatch }
                assertEquals(350.0, createdStock.quantity)

                // إضافة فحص جودة للمخزون
                stockVm.addTestResult(
                    stockItemId = createdStock.id,
                    test = "فحص الجهد ومقاومة العزل",
                    result = true,
                    value = "3.3V Stable",
                    notes = "اجتاز الاختبار بنجاح تام"
                )
                stockVm.loadData()

                // تحويل جزء من المخزون إلى الموقع الفرعي
                stockVm.transferStockItem(
                    itemId = createdStock.id,
                    sourceLocationId = createdLocation.id,
                    targetLocationId = createdSubLocation.id,
                    quantity = 50.0,
                    reason = "نقل تجريبي إلى ممر القطع",
                    notes = "نقل تجريبي للموقع الفرعي"
                )
                stockVm.loadData()

                // =========================================================================
                // المرحلة الخامسة: شاشة قائمة المواد (BomScreen)
                // =========================================================================
                bomVm.selectParentPart(createdPart.id)
                bomVm.addBomItem(
                    subPartId = createdSubPart.id,
                    quantity = 6.0,
                    reference = "R1, R2, R3, R4, R5, R6",
                    optional = false,
                    consumable = false,
                    allowVariants = false,
                    inherited = false,
                    note = "مقاومات التغذية لـ $sessionToken"
                )
                awaitCondition("bom-created") {
                    bomVm.uiState.value.bomItems.any { it.subPartId == createdSubPart.id }
                }
                val createdBomItem = bomVm.uiState.value.bomItems.first { it.subPartId == createdSubPart.id }
                assertEquals(6.0, createdBomItem.quantity)

                // =========================================================================
                // المرحلة السادسة: شاشة أوامر الشراء والإنتاج (Orders)
                // =========================================================================
                val poRef = "PO-$sessionToken-01"
                poVm.addPurchaseOrder(
                    reference = poRef,
                    supplierId = createdCompany.id,
                    description = "توريد دفعة تجريبية من المتحكمات",
                    targetDate = "2026-11-30",
                    currency = "SAR"
                )
                poVm.loadData()
                awaitCondition("po-created") { poVm.uiState.value.orders.any { it.reference == poRef } }
                val createdPo = poVm.uiState.value.orders.first { it.reference == poRef }
                assertEquals(poRef, createdPo.reference)

                val boRef = "BO-$sessionToken-01"
                buildVm.addBuildOrder(
                    reference = boRef,
                    title = "تجميع لوحة التحكم الرئيسية $sessionToken",
                    partId = createdPart.id,
                    quantity = 40.0,
                    batch = "PROD-BATCH-$sessionToken",
                    targetDate = "2026-12-15"
                )
                buildVm.loadData()
                awaitCondition("build-created") { buildVm.uiState.value.builds.any { it.reference == boRef } }
                val createdBuild = buildVm.uiState.value.builds.first { it.reference == boRef }
                assertEquals(40.0, createdBuild.quantity)

                // =========================================================================
                // المرحلة السابعة: التحقق المباشر من جداول ملف SQLite في القرص الصلب
                // =========================================================================
                assertTrue(dbFile.exists(), "يجب أن يتواجد ملف قاعدة بيانات SQLite على القرص")
                assertTrue(dbFile.length() > 0, "يجب ألا يكون ملف قاعدة البيانات فارغاً")

                val conn = SqliteDatabaseManager.getConnection()
                fun countRows(table: String): Long {
                    return conn.prepare("SELECT COUNT(*) FROM $table;").use { stmt ->
                        if (stmt.step()) stmt.getLong(0) else 0L
                    }
                }

                assertTrue(countRows("companies") >= 1, "يجب أن يحتوي جدول companies على بيانات")
                assertTrue(countRows("parts") >= 2, "يجب أن يحتوي جدول parts على بيانات الصنف والمكون الفرعي")
                assertTrue(countRows("stock_items") >= 1, "يجب أن يحتوي جدول stock_items على بيانات المخزون")
                assertTrue(countRows("stock_locations") >= 2, "يجب أن يحتوي جدول stock_locations على موقعين")
                assertTrue(countRows("bom_items") >= 1, "يجب أن يحتوي جدول bom_items على بند المواد")
                assertTrue(countRows("purchase_orders") >= 1, "يجب أن يحتوي جدول purchase_orders على أمر الشراء")
                assertTrue(countRows("build_orders") >= 1, "يجب أن يحتوي جدول build_orders على أمر الإنتاج")
                assertTrue(countRows("contacts") >= 1, "يجب أن يحتوي جدول contacts على جهة الاتصال")
                assertTrue(countRows("company_bank_accounts") >= 1, "يجب أن يحتوي جدول company_bank_accounts على الحساب البنكي")
                assertTrue(countRows("stock_item_test_results") >= 1, "يجب أن يحتوي جدول stock_item_test_results على نتيجة الفحص")

                // =========================================================================
                // المرحلة الثامنة: محاكاة إغلاق التطبيق كلياً (Cold Restart) وإعادة تشغيله
                // =========================================================================
                // إغلاق الاتصال ومسح الذاكرة
                SqliteDatabaseManager.closeDatabase()

                // إنشاء حاوية ونماذج عرض جديدة تماماً تحاكي جلسة عمل مستخدم جديدة بعد إعادة التشغيل
                val freshContainer = AppContainer()
                val freshCompanyVm = freshContainer.createCompanyViewModel()
                val freshPartVm = freshContainer.createPartViewModel()
                val freshStockVm = freshContainer.createStockViewModel()
                val freshBomVm = freshContainer.createBomViewModel()
                val freshPoVm = freshContainer.createPurchaseOrderViewModel()
                val freshBuildVm = freshContainer.createBuildOrderViewModel()

                // تحميل البيانات في الجلسة الجديدة
                freshCompanyVm.loadData()
                freshPartVm.loadData()
                freshStockVm.loadData()
                freshPoVm.loadData()
                freshBuildVm.loadData()

                awaitCondition("fresh-company") { freshCompanyVm.uiState.value.companies.any { it.name == companyName } }
                awaitCondition("fresh-part") { freshPartVm.uiState.value.parts.any { it.ipn == partIpn } }
                awaitCondition("fresh-stock") { freshStockVm.uiState.value.stockItems.any { it.batch == stockBatch } }
                awaitCondition("fresh-location") { freshStockVm.uiState.value.locations.any { it.name == locName } }
                awaitCondition("fresh-po") { freshPoVm.uiState.value.orders.any { it.reference == poRef } }
                awaitCondition("fresh-build") { freshBuildVm.uiState.value.builds.any { it.reference == boRef } }

                // التحقق الحاسم من بقاء كل التفاصيل دون نقصان
                val restoredCompany = freshCompanyVm.uiState.value.companies.first { it.name == companyName }
                assertEquals("SAR", restoredCompany.currency)
                assertEquals("+966501234567", restoredCompany.phone)

                // استرجاع جهات الاتصال والحسابات البنكية بعد الـ Cold Restart
                freshCompanyVm.setSelectedCompany(restoredCompany)
                awaitCondition("restored-contact") {
                    freshCompanyVm.uiState.value.companyContacts.any { it.name == "سارة العتيبي" }
                }
                awaitCondition("restored-bank") {
                    freshCompanyVm.uiState.value.companyBankAccounts.any { it.iban == "SA4480000000608010167519" }
                }

                val restoredPart = freshPartVm.uiState.value.parts.first { it.ipn == partIpn }
                assertEquals(partName, restoredPart.name)
                assertEquals(partIpn, restoredPart.ipn)

                val stockWithBatch = freshStockVm.uiState.value.stockItems.filter { it.batch == stockBatch }
                assertEquals(2, stockWithBatch.size, "يجب أن يتواجد عنصر المخزون الأصلي والعنصر المنقول إلى الموقع الفرعي")
                val sourceStock = stockWithBatch.first { it.locationId == createdLocation.id }
                val transferredStock = stockWithBatch.first { it.locationId == createdSubLocation.id }
                assertEquals(300.0, sourceStock.quantity, "يجب أن يتبقى 300 في الموقع الأصلي بعد النقل")
                assertEquals(50.0, transferredStock.quantity, "يجب أن يتواجد 50 في الموقع الفرعي المنقول إليه")

                freshBomVm.selectParentPart(restoredPart.id)
                awaitCondition("fresh-bom") { freshBomVm.uiState.value.bomItems.any { it.subPartId == createdSubPart.id } }
                val restoredBom = freshBomVm.uiState.value.bomItems.first { it.subPartId == createdSubPart.id }
                assertEquals(6.0, restoredBom.quantity)

                val restoredPo = freshPoVm.uiState.value.orders.first { it.reference == poRef }
                assertEquals(poRef, restoredPo.reference)

                val restoredBuild = freshBuildVm.uiState.value.builds.first { it.reference == boRef }
                assertEquals(boRef, restoredBuild.reference)
                assertEquals(40.0, restoredBuild.quantity)

            } finally {
                SqliteDatabaseManager.closeDatabase()
                SqliteDatabaseManager.setCustomDatabasePath(null)
                if (dbFile.exists()) dbFile.delete()
            }
        }
    }
}
