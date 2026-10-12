package com.nextstepai.inventory

import com.nextstepai.inventory.auth.PasswordHasher
import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.*
import com.nextstepai.inventory.di.AppContainer
import java.io.File
import kotlin.test.*
import kotlin.time.Clock

/**
 * اختبار تكاملي شامل وعميق (High-Volume System Integration & Stress Verification Test):
 * يحاكي إدخال كميات مكثفة ومترابطة من البيانات في كافة أجزاء وشاشات التطبيق
 * (الأصناف، الفئات، المستودعات، المواقع التخزينية، بنود المخزون، الشركاء والموردين،
 * أوامر الشراء، أوامر التصنيع، BOM، وإدارة الموظفين والصلاحيات)
 * ثم يتحقق من الحفظ المادي واستقرار شجرة العلاقات بعد إعادة التشغيل الباردة (Cold Restart).
 */
class ComprehensiveHighVolumeSystemIntegrationTest {

    private lateinit var testDbFile: File

    @BeforeTest
    fun setUp() {
        val runToken = Clock.System.now().toEpochMilliseconds().toString().takeLast(6)
        testDbFile = File(System.getProperty("java.io.tmpdir"), "test_high_volume_$runToken.db")
        if (testDbFile.exists()) testDbFile.delete()
        testDbFile.deleteOnExit()
        SqliteDatabaseManager.setCustomDatabasePath(testDbFile.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        SqliteDatabaseManager.closeDatabase()
        SqliteDatabaseManager.setCustomDatabasePath(null)
        if (::testDbFile.isInitialized && testDbFile.exists()) {
            testDbFile.delete()
        }
    }

    @Test
    fun testHighVolumeDataInjectionAndPersistenceIntegrity() {
        // -------------------------------------------------------------
        // 1. تشغيل بيئة العمل وتسجيل الدخول عبر ViewModels
        // -------------------------------------------------------------
        val container = AppContainer()
        val loginVm = container.createLoginViewModel()
        val partVm = container.createPartViewModel()
        val stockVm = container.createStockViewModel()
        val companyVm = container.createCompanyViewModel()
        val poVm = container.createPurchaseOrderViewModel()
        val buildVm = container.createBuildOrderViewModel()
        val bomVm = container.createBomViewModel()
        val staffVm = container.createStaffManagementViewModel(ownerId = "CORP-OWNER-01")
        val appUserDao = AppUserDao()

        // تسجيل الدخول السريع والتحقق من الجلسة
        kotlinx.coroutines.runBlocking {
            container.loginRepository.verifyAndLogin()
        }
        assertTrue(container.loginRepository.isLoggedIn(), "يجب تسجيل الدخول بنجاح")

        // -------------------------------------------------------------
        // 2. إدخال تصنيفات الأصناف بكثافة (Categories)
        // -------------------------------------------------------------
        val catElec = partVm.addNewCategory("مكونات إلكترونية دقيقة", "مقاومات ومكثفات ودوائر متكاملة")
        val catSensors = partVm.addNewCategory("مستشعرات صناعية", "حساسات حرارة وضغط وتدفق")
        val catMcu = partVm.addNewCategory("متحكمات ومعالجات مدمجة", "وحدات STM32 و ESP32 و ARM")
        val catPower = partVm.addNewCategory("وحدات الطاقة والتغذية", "محولات ومنظمات جهد وبطاريات")
        val catAssemblies = partVm.addNewCategory("تجميعات نهائية ذكية", "لوحات تحكم وأنظمة مجمعة كاملة")

        assertTrue(catElec.id > 0)
        assertTrue(catSensors.id > 0)
        assertTrue(catMcu.id > 0)

        // -------------------------------------------------------------
        // 3. إدخال قطع ومكونات بكثافة (Parts) وتخصيص تفاصيلها
        // -------------------------------------------------------------
        val partsCreated = mutableListOf<Part>()

        val mcuPart = container.partRepository.addPart(
            Part(
                name = "متحكم STM32F407VGT6",
                ipn = "MCU-STM32-001",
                description = "معالج ARM Cortex-M4 بسرعة 168MHz",
                categoryId = catMcu.id,
                units = "pcs",
                assembly = false,
                component = true,
                minimumStock = 20.0,
                maximumStock = 500.0,
                purchaseable = true,
                salable = false
            )
        )
        partsCreated.add(mcuPart)

        val regPart = container.partRepository.addPart(
            Part(
                name = "منظم جهد LDO AMS1117-3.3",
                ipn = "PWR-AMS-33",
                description = "منظم جهد خطي 3.3 فولت 1 أمبير",
                categoryId = catPower.id,
                units = "pcs",
                assembly = false,
                component = true,
                minimumStock = 50.0,
                maximumStock = 1000.0,
                purchaseable = true,
                salable = false
            )
        )
        partsCreated.add(regPart)

        val capPart = container.partRepository.addPart(
            Part(
                name = "مكثف سيراميكي SMD 100nF",
                ipn = "CAP-SMD-104",
                description = "مكثف 50V فئة 0805",
                categoryId = catElec.id,
                units = "pcs",
                assembly = false,
                component = true,
                minimumStock = 200.0,
                maximumStock = 5000.0,
                purchaseable = true,
                salable = false
            )
        )
        partsCreated.add(capPart)

        val resPart = container.partRepository.addPart(
            Part(
                name = "مقاومة SMD 10k 1%",
                ipn = "RES-SMD-1002",
                description = "مقاومة دقيقة فئة 0805",
                categoryId = catElec.id,
                units = "pcs",
                assembly = false,
                component = true,
                minimumStock = 300.0,
                maximumStock = 10000.0,
                purchaseable = true,
                salable = false
            )
        )
        partsCreated.add(resPart)

        val sensorPart = container.partRepository.addPart(
            Part(
                name = "مستشعر حرارة صناعي PT100",
                ipn = "SNS-PT100-RTD",
                description = "مستشعر بلاتيني عالي الدقة",
                categoryId = catSensors.id,
                units = "pcs",
                assembly = false,
                component = true,
                minimumStock = 10.0,
                maximumStock = 100.0,
                purchaseable = true,
                salable = false
            )
        )
        partsCreated.add(sensorPart)

        // قطعة مجمعة نهائية (Assembly)
        val finalProductPart = container.partRepository.addPart(
            Part(
                name = "لوحة التحكم الذكية NX-Pro 500",
                ipn = "ASM-NX-500",
                description = "وحدة تحكم صناعية متكاملة مع حساسات",
                categoryId = catAssemblies.id,
                units = "unit",
                assembly = true,
                component = false,
                minimumStock = 5.0,
                maximumStock = 50.0,
                purchaseable = false,
                salable = true
            )
        )
        partsCreated.add(finalProductPart)

        // إضافة 6 قطع إضافية لتعزيز كثافة البيانات
        for (i in 1..6) {
            val genericPart = container.partRepository.addPart(
                Part(
                    name = "موصل صناعي Connector Con-$i",
                    ipn = "CON-PIN-0$i",
                    description = "موصل طرفي ذو $i أقطاب",
                    categoryId = catElec.id,
                    units = "pcs",
                    minimumStock = 25.0
                )
            )
            partsCreated.add(genericPart)
        }

        assertEquals(12, partsCreated.size)

        // -------------------------------------------------------------
        // 4. إدخال تفاصيل ومعاملات وملاحظات ومرفقات للقطعة
        // -------------------------------------------------------------
        partVm.selectPart(mcuPart)
        partVm.savePartNotes("ملاحظات هندسية: يجب تخزين المتحكم في أكياس مضادة للشحنات الساكنة ESD.")
        partVm.addPartAttachment(
            attachmentPath = "/docs/stm32f407_datasheet.pdf",
            linkUrl = "https://st.com/stm32f407",
            comment = "ورقة المواصفات الفنية الرسمية"
        )
        partVm.addPartTestTemplate(
            testName = "فحص الجهد الأولي VDD",
            description = "قياس الجهد بين الطرفين VDD و VSS للتأكد من 3.3V",
            required = true,
            requiresValue = true,
            requiresAttachment = false
        )

        // -------------------------------------------------------------
        // 5. بناء هيكل المستودعات والمواقع التخزينية بكثافة (Warehouse Hierarchy)
        // -------------------------------------------------------------
        val rootWarehouse = stockVm.quickCreateParentLocation(
            name = "المستودع المركزي - الدمام",
            locationType = "WAREHOUSE",
            description = "المركز اللوجستي والمستودع الرئيسي"
        )
        assertTrue(rootWarehouse.id > 0)

        val zoneElectronics = stockVm.quickCreateParentLocation(
            name = "منطقة المكونات الإلكترونية Zone-E",
            locationType = "ZONE",
            parentId = rootWarehouse.id,
            description = "منطقة مكيفة مع حماية كاملة ضد ESD"
        )
        assertEquals(rootWarehouse.id, zoneElectronics.parentId)

        val rack01 = stockVm.quickCreateParentLocation(
            name = "الحامل Rack-01",
            locationType = "RACK",
            parentId = zoneElectronics.id,
            description = "حامل مخصص للشرائح الدقيقة"
        )

        val shelf01 = stockVm.addLocation(
            name = "الرف Shelf-A",
            description = "رف مخصص للرقائق الحساسة",
            parentId = rack01.id,
            structural = false,
            locationType = "SHELF",
            customCapacity = 2000.0,
            capacityUnit = "pcs"
        )

        val shelf02 = stockVm.addLocation(
            name = "الرف Shelf-B",
            description = "رف مخصص للمقاومات والمكثفات",
            parentId = rack01.id,
            structural = false,
            locationType = "SHELF",
            customCapacity = 50000.0,
            capacityUnit = "pcs"
        )

        // توليد مواقع متسلسلة بالجملة (Bulk Location Generator)
        val bulkBins = stockVm.generateBulkLocations(
            parentId = shelf02.id,
            locationType = "BIN",
            prefix = "BIN-",
            startNumber = 1,
            endNumber = 10,
            padZeros = true,
            customCapacity = 1000.0,
            description = "صندوق تخزين سريع للمكونات السطحية"
        )
        assertEquals(10, bulkBins.size)

        // -------------------------------------------------------------
        // 6. إدخال بنود المخزون بكثافة (Stock Items) وتنفيذ عمليات نقل
        // -------------------------------------------------------------
        stockVm.addStockItem(
            partId = mcuPart.id,
            locationId = shelf01.id,
            quantity = 150.0,
            batch = "LOT-2026-MCU-01",
            serial = "SN-STM32-9001",
            packaging = "Tray",
            purchasePrice = 8.50,
            purchasePriceCurrency = "USD"
        )

        stockVm.addStockItem(
            partId = regPart.id,
            locationId = shelf02.id,
            quantity = 500.0,
            batch = "LOT-2026-REG-01",
            packaging = "Reel",
            purchasePrice = 0.25,
            purchasePriceCurrency = "USD"
        )

        stockVm.addStockItem(
            partId = capPart.id,
            locationId = bulkBins[0].id,
            quantity = 2500.0,
            batch = "LOT-2026-CAP-01",
            packaging = "Reel",
            purchasePrice = 0.02,
            purchasePriceCurrency = "USD"
        )

        stockVm.addStockItem(
            partId = resPart.id,
            locationId = bulkBins[1].id,
            quantity = 5000.0,
            batch = "LOT-2026-RES-01",
            packaging = "Reel",
            purchasePrice = 0.01,
            purchasePriceCurrency = "USD"
        )

        stockVm.addStockItem(
            partId = sensorPart.id,
            locationId = shelf01.id,
            quantity = 40.0,
            batch = "LOT-2026-SNS-01",
            packaging = "Box",
            purchasePrice = 15.00,
            purchasePriceCurrency = "USD"
        )

        val stockItemsAfterAdd = container.stockRepository.getStockItems()
        assertTrue(stockItemsAfterAdd.size >= 5, "يجب أن تحتوي قاعدة البيانات على بنود المخزون المدخلة")

        // تنفيذ نقل مخزني جزئي لأحد البنود
        val itemToTransfer = stockItemsAfterAdd.first { it.partId == mcuPart.id }
        val transferred = container.stockRepository.transferStockItem(
            itemId = itemToTransfer.id,
            sourceLocationId = shelf01.id,
            targetLocationId = shelf02.id,
            quantityToTransfer = 20.0,
            reason = "نقل لتجهيز خط التجميع التجريبي",
            notes = "تم النقل بواسطة المشرف الفني"
        )
        assertTrue(transferred, "يجب أن تنجح عملية النقل المخزني")

        // -------------------------------------------------------------
        // 7. إدخال الشركات والموردين وجهات الاتصال (Companies & Suppliers)
        // -------------------------------------------------------------
        companyVm.addCompany(
            name = "شركة تايوان لأشباه الموصلات TSMC Local",
            description = "المورد المعتمد للرقائق والمتحكمات",
            website = "https://tsmc-distributor.com",
            phone = "+966114002233",
            email = "sales@tsmc-dist.sa",
            address = "طريق الملك فهد، الرياض، المملكة العربية السعودية",
            contact = "م. خالد السبيعي",
            isSupplier = true,
            isManufacturer = true,
            isCustomer = false,
            currency = "SAR"
        )

        companyVm.addCompany(
            name = "شركة المستقبل لتوزيع الإلكترونيات Future Electronics",
            description = "موزع رئيسي للمكونات السلبية والمستشعرات",
            website = "https://futurelec.sa",
            phone = "+966138001122",
            email = "info@futurelec.sa",
            address = "حي الراكة، الخبر، المملكة العربية السعودية",
            contact = "سعيد الغامدي",
            isSupplier = true,
            isManufacturer = false,
            isCustomer = false,
            currency = "SAR"
        )

        companyVm.addCompany(
            name = "شركة حلول الطاقة الذكية (العميل الرئيسي)",
            description = "عميل معتمد لحلول أنظمة التحكم المدمجة",
            website = "https://smartpower.sa",
            phone = "+966126005544",
            email = "orders@smartpower.sa",
            address = "المدينة الصناعية الثانية، جدة، المملكة العربية السعودية",
            contact = "أنس الزهراني",
            isSupplier = false,
            isManufacturer = false,
            isCustomer = true,
            currency = "SAR"
        )

        val companiesList = container.companyRepository.getCompanies()
        assertTrue(companiesList.size >= 3)
        val supplierCompany = companiesList.first { it.isSupplier }

        // إضافة عنوان وجهة اتصال مرتبطة بالمورد
        val contact = container.companyRepository.addContact(
            Contact(
                companyId = supplierCompany.id,
                name = "م. عمر التميمي",
                phone = "+966501234567",
                email = "omar@supplier.sa",
                role = "مدير المبيعات الهندسية",
                isPrimary = true
            )
        )
        assertTrue(contact.id > 0)

        // -------------------------------------------------------------
        // 8. إنشاء قائمة المواد وقواعد التجميع (BOM Items)
        // -------------------------------------------------------------
        bomVm.selectParentPart(finalProductPart.id)
        bomVm.addBomItem(
            subPartId = mcuPart.id,
            quantity = 1.0,
            reference = "U1",
            optional = false,
            consumable = false,
            allowVariants = false,
            inherited = false,
            note = "المتحكم الرئيسي في النظام"
        )
        bomVm.addBomItem(
            subPartId = regPart.id,
            quantity = 1.0,
            reference = "U2",
            optional = false,
            consumable = false,
            allowVariants = false,
            inherited = false,
            note = "منظم جهد 3.3V"
        )
        bomVm.addBomItem(
            subPartId = capPart.id,
            quantity = 4.0,
            reference = "C1, C2, C3, C4",
            optional = false,
            consumable = true,
            allowVariants = true,
            inherited = false,
            note = "مكثفات فك الارتباط"
        )
        bomVm.addBomItem(
            subPartId = resPart.id,
            quantity = 2.0,
            reference = "R1, R2",
            optional = false,
            consumable = true,
            allowVariants = true,
            inherited = false,
            note = "مقاومات رفع I2C"
        )

        val bomItems = container.bomRepository.getBomItemsForPart(finalProductPart.id)
        assertEquals(4, bomItems.size, "يجب ربط 4 مكونات رئيسية بقائمة مواد المنتج المجمع")

        // -------------------------------------------------------------
        // 9. إنشاء أوامر الشراء (Purchase Orders)
        // -------------------------------------------------------------
        val poLines = listOf(
            PurchaseOrderLineItem(
                orderId = 0L,
                supplierPartId = mcuPart.id,
                partName = mcuPart.name,
                quantity = 100.0,
                purchasePrice = 8.20,
                notes = "دفعة إضافية لخطوط التصنيع"
            ),
            PurchaseOrderLineItem(
                orderId = 0L,
                supplierPartId = sensorPart.id,
                partName = sensorPart.name,
                quantity = 50.0,
                purchasePrice = 14.50,
                notes = "مستشعرات PT100"
            )
        )
        poVm.addPurchaseOrder(
            reference = "PO-2026-0089",
            supplierId = supplierCompany.id,
            description = "شراء كميات إنتاج الربع الرابع",
            targetDate = "2026-11-15",
            currency = "USD",
            initialLines = poLines
        )

        val ordersList = container.poRepository.searchOrders()
        assertTrue(ordersList.any { it.reference == "PO-2026-0089" })

        // -------------------------------------------------------------
        // 10. إنشاء أوامر التصنيع والإنتاج (Build Orders)
        // -------------------------------------------------------------
        buildVm.addBuildOrder(
            reference = "BO-2026-042",
            title = "أمر تجميع 20 لوحة تحكم NX-Pro 500",
            partId = finalProductPart.id,
            quantity = 20.0,
            batch = "BUILD-BATCH-ALPHA",
            targetDate = "2026-11-20",
            startDate = "2026-10-15",
            takeFromLocationId = shelf01.id,
            destinationLocationId = rootWarehouse.id,
            notes = "أمر إنتاجي عاجل لمشروع الطاقة"
        )

        val buildsList = container.buildRepository.searchBuilds()
        val createdBuild = buildsList.first { it.reference == "BO-2026-042" }
        assertNotNull(createdBuild)
        assertEquals(finalProductPart.id, createdBuild.partId)
        assertEquals(20.0, createdBuild.quantity)

        // -------------------------------------------------------------
        // 11. إدارة الموظفين والصلاحيات وأكواد التفعيل (Staff Management)
        // -------------------------------------------------------------
        val staffUser1 = AppUserEntity(
            uuid = "usr-eng-01",
            ownerId = "CORP-OWNER-01",
            name = "م. يوسف الهاشمي",
            phoneNumber = "+966551122334",
            passwordHash = PasswordHasher.hashPassword("Secure@2026Pass"),
            role = "PRODUCTION_LEAD",
            active = true
        )
        val staffUser2 = AppUserEntity(
            uuid = "usr-inv-02",
            ownerId = "CORP-OWNER-01",
            name = "فيصل القحطاني",
            phoneNumber = "+966559988776",
            passwordHash = PasswordHasher.hashPassword("Store#Password2026"),
            role = "STOREKEEPER",
            active = true
        )
        appUserDao.insertOrUpdate(staffUser1)
        appUserDao.insertOrUpdate(staffUser2)

        // توليد كود تفعيل للموظف الجديد مع صلاحيات مخصصة
        staffVm.onLabelChange("كود تفعيل مهندس الاختبارات والجودة")
        staffVm.applyPreset("PRODUCTION")
        staffVm.generateActivationCode()

        val activeCodes = container.staffManagementDao.getActiveInvitationCodes("CORP-OWNER-01")
        assertTrue(activeCodes.isNotEmpty(), "يجب توليد كود تفعيل نشط للموظف")

        val staffUsers = container.staffManagementDao.getUsersByOwner("CORP-OWNER-01")
        assertTrue(staffUsers.size >= 2, "يجب حفظ مستخدمي الموظفين بنجاح")

        // -------------------------------------------------------------
        // 12. محاكاة الإغلاق البارد الكامل (Full Cold Shutdown & Restart)
        // -------------------------------------------------------------
        SqliteDatabaseManager.closeDatabase()

        // -------------------------------------------------------------
        // 13. إعادة تشغيل التطبيق بحاويات جديدة وفحص سلامة استرجاع كافة البيانات (Verification)
        // -------------------------------------------------------------
        val freshContainer = AppContainer()
        val freshParts = freshContainer.partRepository.getParts()
        val freshCategories = freshContainer.partCategoryRepository.getCategories()
        val freshLocations = freshContainer.stockLocationRepository.getLocations()
        val freshStockItems = freshContainer.stockRepository.getStockItems()
        val freshCompanies = freshContainer.companyRepository.getCompanies()
        val freshBomItems = freshContainer.bomRepository.getBomItemsForPart(finalProductPart.id)
        val freshPOs = freshContainer.poRepository.searchOrders()
        val freshBuilds = freshContainer.buildRepository.searchBuilds()
        val freshStaff = freshContainer.staffManagementDao.getUsersByOwner("CORP-OWNER-01")
        val freshCodes = freshContainer.staffManagementDao.getActiveInvitationCodes("CORP-OWNER-01")

        // تحقق التصنيفات
        assertTrue(freshCategories.size >= 5)
        assertNotNull(freshCategories.find { it.name == "متحكمات ومعالجات مدمجة" })

        // تحقق الأصناف وتفاصيلها
        assertTrue(freshParts.size >= 12)
        val reloadedMcu = freshParts.find { it.ipn == "MCU-STM32-001" }
        assertNotNull(reloadedMcu)
        assertEquals("متحكم STM32F407VGT6", reloadedMcu.name)
        assertEquals(20.0, reloadedMcu.minimumStock)

        // تحقق ملاحظات ومرفقات وقوالب فحص الصنف
        val reloadedNotes = freshContainer.partRepository.getPartNotes(reloadedMcu.id)
        assertNotNull(reloadedNotes)
        assertTrue(reloadedNotes.notes.contains("ESD"))

        val reloadedAttachments = freshContainer.partRepository.getPartAttachments(reloadedMcu.id)
        assertTrue(reloadedAttachments.any { it.link == "https://st.com/stm32f407" })

        val reloadedTemplates = freshContainer.partRepository.getPartTestTemplates(reloadedMcu.id)
        assertTrue(reloadedTemplates.any { it.testName == "فحص الجهد الأولي VDD" })

        // تحقق هيكل المستودعات والمواقع والمسار الهرمي
        val reloadedRoot = freshLocations.find { it.name == "المستودع المركزي - الدمام" }
        assertNotNull(reloadedRoot)
        assertEquals("WAREHOUSE", reloadedRoot.locationType)

        val reloadedShelf = freshLocations.find { it.name == "الرف Shelf-A" }
        assertNotNull(reloadedShelf)
        val fullHierarchyPath = reloadedShelf.getFullHierarchyPath(freshLocations)
        assertTrue(
            fullHierarchyPath.contains("المستودع المركزي") && fullHierarchyPath.contains("الرف Shelf-A"),
            "المسار الهرمي للمواقع يجب أن يكون متصلاً وسليماً: $fullHierarchyPath"
        )

        // تحقق الصناديق المولدة بالجملة
        val reloadedBins = freshLocations.filter { it.name.startsWith("BIN-") }
        assertEquals(10, reloadedBins.size)

        // تحقق بنود المخزون وحالاتها
        assertTrue(freshStockItems.size >= 5)
        val reloadedMcuStock = freshStockItems.filter { it.partId == reloadedMcu.id }
        assertTrue(reloadedMcuStock.isNotEmpty())
        val totalMcuQuantity = reloadedMcuStock.sumOf { it.quantity }
        assertEquals(150.0, totalMcuQuantity, "إجمالي كمية المخزون يجب أن تكون متطابقة تماماً بعد النقل")

        // تحقق الشركات والاتصال
        val reloadedSupplier = freshCompanies.find { it.id == supplierCompany.id }
        assertNotNull(reloadedSupplier)
        assertTrue(reloadedSupplier.isSupplier)
        val reloadedContacts = freshContainer.companyRepository.getContactsForCompany(reloadedSupplier.id)
        assertTrue(reloadedContacts.any { it.name == "م. عمر التميمي" })

        // تحقق قائمة المواد BOM
        assertEquals(4, freshBomItems.size)
        assertTrue(freshBomItems.any { it.reference == "U1" && it.subPartId == reloadedMcu.id })

        // تحقق أمر الشراء والبنود
        val reloadedPO = freshPOs.find { it.reference == "PO-2026-0089" }
        assertNotNull(reloadedPO)
        assertEquals(2, reloadedPO.lineItems.size)

        // تحقق أمر التصنيع
        val reloadedBuild = freshBuilds.find { it.reference == "BO-2026-042" }
        assertNotNull(reloadedBuild)
        assertEquals(20.0, reloadedBuild.quantity)
        assertEquals(finalProductPart.id, reloadedBuild.partId)

        // تحقق الموظفين وكلمات المرور المشفرة وأكواد التفعيل
        assertEquals(2, freshStaff.size)
        val yusuf = freshStaff.find { it.name == "م. يوسف الهاشمي" }
        assertNotNull(yusuf)
        assertTrue(PasswordHasher.verifyPassword("Secure@2026Pass", yusuf.passwordHash))
        assertEquals("PRODUCTION_LEAD", yusuf.role)

        assertTrue(freshCodes.isNotEmpty())
        assertEquals("CORP-OWNER-01", freshCodes[0].ownerId)
    }
}
