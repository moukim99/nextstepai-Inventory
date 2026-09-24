package com.nextstepai.inventory.data

import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * مولد البيانات الاختبارية الشاملة وعالية الكثافة لجميع جداول وأعمدة قاعدة البيانات.
 * يحاكي بيئة عمل مؤسسية وصناعية متكاملة ومترابطة ببيانات نموذجية واقعية ذات حجم كبير.
 */
object TestDataGenerator {

    private val tablesToClear = listOf(
        "notifications_history",
        "build_items",
        "build_order_line_items",
        "build_orders",
        "purchase_order_lines",
        "purchase_orders",
        "supplier_price_breaks",
        "supplier_parts",
        "manufacturer_part_attachments",
        "manufacturer_part_parameters",
        "manufacturer_parts",
        "company_legal_records",
        "company_bank_accounts",
        "addresses",
        "contacts",
        "company_attachments",
        "companies",
        "stock_item_attachments",
        "stock_item_test_results",
        "stock_item_tracking",
        "stock_items",
        "stock_locations",
        "bom_item_substitutes",
        "bom_items",
        "part_pricing",
        "part_sale_prices",
        "part_stars",
        "part_internal_prices",
        "part_notes",
        "part_attachments",
        "part_test_templates",
        "part_related",
        "part_parameters",
        "part_category_parameter_templates",
        "part_parameter_templates",
        "parts",
        "part_categories"
    )

    /**
     * حذف جميع البيانات الاختبارية من كافة جداول النظام دون المساس بإعدادات التطبيق (app_settings).
     */
    suspend fun clearAllTestData() = withContext(Dispatchers.IO) {
        val conn = SqliteDatabaseManager.getConnection()
        tablesToClear.forEach { table ->
            runCatching {
                conn.prepare("DELETE FROM $table").use { it.step() }
            }
        }
        // تصفير الذاكرة المؤقتة لمنع إظهار أية بيانات افتراضية متبقية
        runCatching { PartTable().clearAll() }
        runCatching { BomItemTable().clearAll() }
        runCatching { StockItemTable().clearAll() }
    }

    /**
     * توليد قاعدة بيانات ضخمة ومترابطة محاكية للاستخدام المؤسسي الحقيقي عبر الـ 37 جدولاً.
     */
    suspend fun generateAllTestData(): Int = withContext(Dispatchers.IO) {
        val conn = SqliteDatabaseManager.getConnection()

        // الترقية الهيكلية الآمنة للجداول
        runCatching { conn.prepare("ALTER TABLE part_categories ADD COLUMN description TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_categories ADD COLUMN structural INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_categories ADD COLUMN defaultLocationId INTEGER").use { it.step() } }

        clearAllTestData()

        val now = 1738000000000L // epoch millis reference

        // استخدام معاملة معاملة نصوص SQL فائقة السرعة (Transaction)
        runCatching { conn.prepare("BEGIN TRANSACTION").use { it.step() } }

        try {
            // 1. part_categories (12 تصنيف)
            conn.prepare("""
                INSERT INTO part_categories (id, name, parentId, description, structural, defaultLocationId)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val categories = listOf(
                    listOf(1L, "الكترونيات وصنع اللوحات", null, "المكونات الإلكترونية الدقيقة والشرائح", 1L, 1L),
                    listOf(2L, "المكونات السلبية (Passive)", 1L, "المكثفات والمقاومات والملفات", 0L, 2L),
                    listOf(3L, "المتحكمات والمعالجات", 1L, "المتحكمات الدقيقة ARM و AVR و ESP", 0L, 2L),
                    listOf(4L, "أنظمة وإمدادات الطاقة", null, "محولات الجهد والبطاريات والمزودات", 0L, 1L),
                    listOf(5L, "الهياكل والأجزاء الميكانيكية", null, "علب التغليف والمشتتات والزنبركات", 0L, 3L),
                    listOf(6L, "المستشعرات والمقاييس", 1L, "مستشعرات الحرارة والرطوبة والضغط والحركة", 0L, 2L),
                    listOf(7L, "الموصلات والكابلات (Connectors)", 1L, "كابلات الشريط والمقابس والمنافذ", 0L, 2L),
                    listOf(8L, "الشاشات ووحدات العرض", 1L, "شاشات OLED و LCD ومصفوفات LED", 0L, 2L),
                    listOf(9L, "المواد الخام والكيميائية", null, "قصدير اللحام ومذيبات IPA والمعجون الحراري", 0L, 4L),
                    listOf(10L, "قطع الغيار والصيانة", null, "شفرات قطع، محركات بديلة، ورؤوس الكاوية", 0L, 4L),
                    listOf(11L, "مواد التغليف والتعبئة", null, "كراتين وأكياس مضادة للكهرباء الساكنة", 0L, 4L),
                    listOf(12L, "المنتجات التامة والتجميعات", null, "المنتجات المكتملة المصنعة الجاهزة للبيع", 1L, 3L)
                )
                for (c in categories) {
                    stmt.bindLong(1, c[0] as Long)
                    stmt.bindText(2, c[1] as String)
                    if (c[2] == null) stmt.bindNull(3) else stmt.bindLong(3, c[2] as Long)
                    stmt.bindText(4, c[3] as String)
                    stmt.bindLong(5, c[4] as Long)
                    stmt.bindLong(6, c[5] as Long)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 2. part_parameter_templates (8 قوالب معاملات)
            conn.prepare("""
                INSERT INTO part_parameter_templates (id, name, units, description, choices, checkbox)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val templates = listOf(
                    listOf(1L, "الجهد الكهربائي التشغيلي", "V", "نطاق جهد التغذية المطلوب", "3.3V, 5V, 12V, 24V", 0L),
                    listOf(2L, "السعة الكهربائية", "uF", "قيمة السعة للمكثف", "0.1uF, 1uF, 10uF, 100uF", 0L),
                    listOf(3L, "المقاومة الكهربائية", "Ohm", "قيمة المقاومة الأومية", "100, 1k, 10k, 100k", 0L),
                    listOf(4L, "درجة حرارة التشغيل", "°C", "نطاق الحرارة المقبولة", "-40 to 85°C, -40 to 125°C", 0L),
                    listOf(5L, "تردد المعالج", "MHz", "سرعة النبضة التشغيلية", "16MHz, 72MHz, 168MHz, 240MHz", 0L),
                    listOf(6L, "سعة التخزين FLASH", "KB", "حجم الذاكرة البرمجية", "64KB, 256KB, 512KB, 1024KB", 0L),
                    listOf(7L, "تيار الخرج الأقصى", "A", "أقصى أمبير متحمل", "0.5A, 1A, 2A, 5A, 10A", 0L),
                    listOf(8L, "مطابق لمعايير RoHS الخضراء", "", "خالي من المواد السامة", "", 1L)
                )
                for (t in templates) {
                    stmt.bindLong(1, t[0] as Long)
                    stmt.bindText(2, t[1] as String)
                    stmt.bindText(3, t[2] as String)
                    stmt.bindText(4, t[3] as String)
                    stmt.bindText(5, t[4] as String)
                    stmt.bindLong(6, t[5] as Long)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 3. part_category_parameter_templates
            conn.prepare("""
                INSERT INTO part_category_parameter_templates (id, categoryId, parameterTemplateId, defaultValue)
                VALUES (?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val catParams = listOf(
                    listOf(1L, 2L, 2L, "10uF"),
                    listOf(2L, 2L, 3L, "10k"),
                    listOf(3L, 3L, 1L, "3.3V"),
                    listOf(4L, 3L, 5L, "168MHz"),
                    listOf(5L, 3L, 6L, "1024KB"),
                    listOf(6L, 4L, 7L, "5A")
                )
                for (cp in catParams) {
                    stmt.bindLong(1, cp[0] as Long)
                    stmt.bindLong(2, cp[1] as Long)
                    stmt.bindLong(3, cp[2] as Long)
                    stmt.bindText(4, cp[3] as String)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 4. stock_locations (6 مواقع وهياكل تخزين)
            conn.prepare("""
                INSERT INTO stock_locations (
                    uuid, locationId, name, description, parentId, parentUuid,
                    structural, external, locationType, ownerId, icon, customIcon,
                    customCapacity, level, lft, rght, treeId, metadata, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val locations = listOf(
                    listOf("loc-001", 1L, "المستودع المركزي - الجزائر العاصمة", "المستودع الرئيسي للمواد والقطع", null, null, 1L, 0L, "WAREHOUSE", 1L, "warehouse", "", 10000.0, 0L, 1L, 12L, 1L, "{}"),
                    listOf("loc-002", 2L, "رف الشرائح والدائريات A-01", "مخصص للمتحكمات والشريحات SMD", 1L, "loc-001", 0L, 0L, "SHELF", 1L, "shelves", "", 500.0, 1L, 2L, 5L, 1L, "{}"),
                    listOf("loc-003", 3L, "رف المكونات السلبية B-02", "مخصص للمكثفات والمقاومات والملفات", 1L, "loc-001", 0L, 0L, "SHELF", 1L, "shelves", "", 1000.0, 1L, 6L, 9L, 1L, "{}"),
                    listOf("loc-004", 4L, "خط الإنتاج والتجميع الرئيسي P-10", "موقع تجميع اللوحات وأوامر البناء", 1L, "loc-001", 0L, 0L, "LINE", 1L, "factory", "", 250.0, 1L, 10L, 11L, 1L, "{}"),
                    listOf("loc-005", 5L, "مخزن وهران للتوزيع الغربي", "مستودع فرعي للشحن الإقليمي", null, null, 1L, 0L, "WAREHOUSE", 1L, "warehouse", "", 5000.0, 0L, 13L, 16L, 2L, "{}"),
                    listOf("loc-006", 6L, "منطقة الفحص والجودة (Quarantine Zone)", "منطقة عزل المنتجات قيد الفحص", 1L, "loc-001", 0L, 0L, "AREA", 1L, "shield", "", 300.0, 1L, 17L, 18L, 1L, "{}")
                )
                for (l in locations) {
                    stmt.bindText(1, l[0] as String)
                    stmt.bindLong(2, l[1] as Long)
                    stmt.bindText(3, l[2] as String)
                    stmt.bindText(4, l[3] as String)
                    if (l[4] == null) stmt.bindNull(5) else stmt.bindLong(5, l[4] as Long)
                    if (l[5] == null) stmt.bindNull(6) else stmt.bindText(6, l[5] as String)
                    stmt.bindLong(7, l[6] as Long)
                    stmt.bindLong(8, l[7] as Long)
                    stmt.bindText(9, l[8] as String)
                    stmt.bindLong(10, l[9] as Long)
                    stmt.bindText(11, l[10] as String)
                    stmt.bindText(12, l[11] as String)
                    stmt.bindDouble(13, l[12] as Double)
                    stmt.bindLong(14, l[13] as Long)
                    stmt.bindLong(15, l[14] as Long)
                    stmt.bindLong(16, l[15] as Long)
                    stmt.bindLong(17, l[16] as Long)
                    stmt.bindText(18, l[17] as String)
                    stmt.bindLong(19, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 5. parts (20 قطعة ومكون مجمع وتجميعة)
            conn.prepare("""
                INSERT INTO parts (
                    uuid, id, name, ipn, description, revision, keywords, categoryId, units,
                    assembly, component, isTemplate, variantOfId, trackable, purchaseable,
                    salable, virtual, active, locked, minimumStock, maximumStock,
                    defaultLocationId, defaultExpiryDays, totalInStock, localImagePath, link,
                    syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val parts = listOf(
                    listOf("part-uuid-1", 1L, "متحكم STM32F407VGT6 ARM Cortex-M4", "MCU-STM32-001", "Microcontroller 168MHz 1MB Flash 100-LQFP", "A2", "MCU, STM32, ARM, ST", 3L, "pcs", 0L, 1L, 0L, null, 1L, 1L, 0L, 0L, 1L, 0L, 50.0, 500.0, 2L, 365L, 320.0, null, "https://www.st.com"),
                    listOf("part-uuid-2", 2L, "مكثف سيراميك 10uF 25V 0805 SMD", "CAP-0805-10U", "Multilayer Ceramic Capacitor 10uF 10% 0805", "V1", "CAP, Ceramic, SMD, 0805", 2L, "pcs", 0L, 1L, 0L, null, 0L, 1L, 0L, 0L, 1L, 0L, 1000.0, 20000.0, 3L, 730L, 8500.0, null, ""),
                    listOf("part-uuid-3", 3L, "قالب مستشعر الحرارة والرطوبة", "ASY-SENS-003", "وحدة استشعار الحرارة والرطوبة المجمعة للأنظمة الصناعية", "A1", "Sensor, Temp, Humidity, Assembly", 6L, "unit", 1L, 0L, 0L, null, 1L, 0L, 1L, 0L, 1L, 0L, 10.0, 150.0, 4L, 365L, 45.0, null, "https://nextstepai.com/sensors"),
                    listOf("part-uuid-4", 4L, "لوحة التحكم الرئيسية Industrial Mainboard v2", "ASY-MB-002", "لوحة تحكم إلكترونية صناعية متكاملة للتطبيقات المتقدمة", "B2", "Mainboard, PCB, Controller, Assembly", 12L, "unit", 1L, 0L, 0L, null, 1L, 0L, 1L, 0L, 1L, 0L, 5.0, 50.0, 4L, 365L, 18.0, null, "https://nextstepai.com/mainboard"),
                    listOf("part-uuid-5", 5L, "مقاومة سطحي 10k Ohm 1/4W 0805 1%", "RES-0805-10K", "Thick Film Chip Resistor 10k Ohm 1% 0805", "V1", "RES, SMD, 0805, 10K", 2L, "pcs", 0L, 1L, 0L, null, 0L, 1L, 0L, 0L, 1L, 0L, 2000.0, 50000.0, 3L, 1095L, 18000.0, null, ""),
                    listOf("part-uuid-6", 6L, "محول طاقة 12V 5A Power Supply Module", "PWR-12V-5A", "AC-DC Step Down Converter Module 12V 60W", "R3", "Power, Supply, 12V, Module", 4L, "pcs", 0L, 1L, 0L, null, 1L, 1L, 1L, 0L, 1L, 0L, 15.0, 200.0, 1L, 365L, 60.0, null, ""),
                    listOf("part-uuid-7", 7L, "متحكم ESP32-WROOM-32D Wi-Fi/BT", "MCU-ESP32-32D", "Wi-Fi + BT + BLE MCU Module Dual Core 240MHz", "C1", "ESP32, Wi-Fi, Bluetooth, MCU", 3L, "pcs", 0L, 1L, 0L, null, 1L, 1L, 1L, 0L, 1L, 0L, 40.0, 1000.0, 2L, 365L, 210.0, null, "https://www.espressif.com"),
                    listOf("part-uuid-8", 8L, "شاشة عرض OLED 0.96 inch I2C 128x64", "DSP-OLED-096", "Monochrome 0.96 SSD1306 OLED Display Module", "V2", "OLED, Display, I2C, SSD1306", 8L, "pcs", 0L, 1L, 0L, null, 1L, 1L, 1L, 0L, 1L, 0L, 20.0, 300.0, 2L, 365L, 95.0, null, ""),
                    listOf("part-uuid-9", 9L, "مرحل حماية Relay Module 5V 10A 2-Channel", "RLY-5V-2CH", "Optocoupler Isolated Relay Board 5V 10A 250VAC", "A1", "Relay, 5V, Module, Switch", 1L, "pcs", 0L, 1L, 0L, null, 1L, 1L, 1L, 0L, 1L, 0L, 25.0, 400.0, 1L, 365L, 110.0, null, ""),
                    listOf("part-uuid-10", 10L, "منظم جهد الخط السلس LDO 3.3V AMS1117", "IC-AMS1117-33", "800mA Low Dropout Voltage Regulator SOT-223", "V1", "Regulator, LDO, 3.3V, AMS1117", 1L, "pcs", 0L, 1L, 0L, null, 0L, 1L, 0L, 0L, 1L, 0L, 100.0, 5000.0, 2L, 730L, 1200.0, null, ""),
                    listOf("part-uuid-11", 11L, "مستشعر حرارة ورطوبة رقمي DHT22/AM2302", "SNS-DHT22-DIG", "High Precision Digital Temperature & Humidity Sensor", "B1", "Sensor, DHT22, Temperature, Humidity", 6L, "pcs", 0L, 1L, 0L, null, 1L, 1L, 1L, 0L, 1L, 0L, 30.0, 500.0, 2L, 365L, 140.0, null, ""),
                    listOf("part-uuid-12", 12L, "موصل طرفي Screw Terminal Block 2-Pin 5mm", "CON-TERM-2P", "PCB Mount Screw Terminal Block 2 Pin Pitch 5.0mm", "V1", "Connector, Terminal, Screw, PCB", 7L, "pcs", 0L, 1L, 0L, null, 0L, 1L, 0L, 0L, 1L, 0L, 500.0, 10000.0, 3L, 1095L, 3400.0, null, ""),
                    listOf("part-uuid-13", 13L, "صندوق هيكل ألومنيوم مقاوم للماء IP67", "ENC-ALU-IP67", "Diecast Aluminum Enclosure Box 120x80x40mm", "A3", "Enclosure, Aluminum, IP67, Box", 5L, "pcs", 0L, 1L, 0L, null, 1L, 1L, 1L, 0L, 1L, 0L, 10.0, 200.0, 1L, 1095L, 75.0, null, ""),
                    listOf("part-uuid-14", 14L, "قصدير لحام Sn63/Pb37 0.8mm 500g Reel", "CHM-SLD-500G", "Rosin Core Solder Wire 63/37 0.8mm Reel", "V1", "Solder, Flux, Wire, Sn63", 9L, "reel", 0L, 1L, 0L, null, 0L, 1L, 0L, 0L, 1L, 0L, 5.0, 50.0, 4L, 730L, 22.0, null, ""),
                    listOf("part-uuid-15", 15L, "جهاز التحكم بالحرارة الذكي Smart Thermostat", "PRD-THRM-ST1", "منتج نهائي ذكي للتحكم بالحرارة والتكييف عبر الإنترنت", "C1", "Product, Thermostat, IoT, Final", 12L, "unit", 1L, 0L, 0L, null, 1L, 0L, 1L, 0L, 1L, 0L, 5.0, 80.0, 5L, 365L, 28.0, null, "https://nextstepai.com/thermostat")
                )
                for (p in parts) {
                    stmt.bindText(1, p[0] as String)
                    stmt.bindLong(2, p[1] as Long)
                    stmt.bindText(3, p[2] as String)
                    stmt.bindText(4, p[3] as String)
                    stmt.bindText(5, p[4] as String)
                    stmt.bindText(6, p[5] as String)
                    stmt.bindText(7, p[6] as String)
                    stmt.bindLong(8, p[7] as Long)
                    stmt.bindText(9, p[8] as String)
                    stmt.bindLong(10, p[9] as Long)
                    stmt.bindLong(11, p[10] as Long)
                    stmt.bindLong(12, p[11] as Long)
                    if (p[12] == null) stmt.bindNull(13) else stmt.bindLong(13, p[12] as Long)
                    stmt.bindLong(14, p[13] as Long)
                    stmt.bindLong(15, p[14] as Long)
                    stmt.bindLong(16, p[15] as Long)
                    stmt.bindLong(17, p[16] as Long)
                    stmt.bindLong(18, p[17] as Long)
                    stmt.bindLong(19, p[18] as Long)
                    stmt.bindDouble(20, p[19] as Double)
                    stmt.bindDouble(21, p[20] as Double)
                    stmt.bindLong(22, p[21] as Long)
                    stmt.bindLong(23, p[22] as Long)
                    stmt.bindDouble(24, p[23] as Double)
                    if (p[24] == null) stmt.bindNull(25) else stmt.bindText(25, p[24] as String)
                    stmt.bindText(26, p[25] as String)
                    stmt.bindLong(27, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 6. part_parameters (معاملات المعايير والقياسات)
            conn.prepare("""
                INSERT INTO part_parameters (id, partId, templateId, data, dataNumeric)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val params = listOf(
                    listOf(1L, 1L, 1L, "3.3V", 3.3),
                    listOf(2L, 1L, 5L, "168MHz", 168.0),
                    listOf(3L, 1L, 6L, "1024KB", 1024.0),
                    listOf(4L, 2L, 2L, "10uF", 10.0),
                    listOf(5L, 5L, 3L, "10000", 10000.0),
                    listOf(6L, 6L, 7L, "5A", 5.0),
                    listOf(7L, 7L, 5L, "240MHz", 240.0),
                    listOf(8L, 10L, 1L, "3.3V", 3.3)
                )
                for (pm in params) {
                    stmt.bindLong(1, pm[0] as Long)
                    stmt.bindLong(2, pm[1] as Long)
                    stmt.bindLong(3, pm[2] as Long)
                    stmt.bindText(4, pm[3] as String)
                    stmt.bindDouble(5, pm[4] as Double)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 7. part_notes
            conn.prepare("""
                INSERT INTO part_notes (id, partId, notes, updatedAt, userId)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val notes = listOf(
                    listOf(1L, 1L, "حفظ الشريحة في بيئة تفريغ كهربائي ESD وصناديق مانعة للرطوبة", "2025-01-15 10:00:00", 1L),
                    listOf(2L, 3L, "وحدة استشعار حرارة دقيقة تمت معايرتها مخبرياً بنجاح", "2025-01-16 09:00:00", 1L),
                    listOf(3L, 4L, "لوحة متوافقة مع معايير الأمان الصناعية IEC 61080 وتطبيقات 24V", "2025-01-16 11:30:00", 1L),
                    listOf(4L, 15L, "منتج ذكي نهائي مبرمج بالفرموير الإصدار v3.4.1 للتحكم السحابي", "2025-01-20 14:00:00", 1L)
                )
                for (n in notes) {
                    stmt.bindLong(1, n[0] as Long)
                    stmt.bindLong(2, n[1] as Long)
                    stmt.bindText(3, n[2] as String)
                    stmt.bindText(4, n[3] as String)
                    stmt.bindLong(5, n[4] as Long)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 8. part_attachments
            conn.prepare("""
                INSERT INTO part_attachments (id, partId, attachment, link, comment, uploadDate, userId)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val attachments = listOf(
                    listOf(1L, 1L, "stm32f407_datasheet.pdf", "https://www.st.com/resource/en/datasheet/stm32f407vg.pdf", "كراسة الشروط والمواصفات الفنية الرسمية", "2025-01-10", 1L),
                    listOf(2L, 7L, "esp32_wroom_datasheet.pdf", "https://www.espressif.com/esp32_datasheet.pdf", "وثيقة المواصفات الفنية للواي فاي والبلوتوث", "2025-01-12", 1L),
                    listOf(3L, 15L, "thermostat_manual.pdf", "https://nextstepai.com/docs/thermostat.pdf", "دليل المستخدم والتركيب الهيكلي للمنتج", "2025-01-18", 1L)
                )
                for (a in attachments) {
                    stmt.bindLong(1, a[0] as Long)
                    stmt.bindLong(2, a[1] as Long)
                    stmt.bindText(3, a[2] as String)
                    stmt.bindText(4, a[3] as String)
                    stmt.bindText(5, a[4] as String)
                    stmt.bindText(6, a[5] as String)
                    stmt.bindLong(7, a[6] as Long)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 9. part_test_templates
            conn.prepare("""
                INSERT INTO part_test_templates (id, partId, testName, description, required, requiresValue, requiresAttachment)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val testTemplates = listOf(
                    listOf(1L, 3L, "فحص دقة الاستشعار", "قياس القراءة مقارنة بحرارة المرجع 25°C", 1L, 1L, 0L),
                    listOf(2L, 4L, "اختبار جهد التغذية 12V", "قياس جهد الخرج والتأكد من استقرار الإشارة", 1L, 1L, 0L),
                    listOf(3L, 4L, "فحص الاتصال البرمجي SWD", "الاتصال بالمتحكم وقراءة المعرف الوحيد", 1L, 0L, 0L),
                    listOf(4L, 15L, "اختبار الاتصال اللاسلكي Wi-Fi", "الاتصال بالسيرفر السحابي وتأكيد النبضة", 1L, 1L, 1L)
                )
                for (tt in testTemplates) {
                    stmt.bindLong(1, tt[0] as Long)
                    stmt.bindLong(2, tt[1] as Long)
                    stmt.bindText(3, tt[2] as String)
                    stmt.bindText(4, tt[3] as String)
                    stmt.bindLong(5, tt[4] as Long)
                    stmt.bindLong(6, tt[5] as Long)
                    stmt.bindLong(7, tt[6] as Long)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 10. part_related
            conn.prepare("""
                INSERT INTO part_related (id, part1Id, part2Id)
                VALUES (?, ?, ?)
            """.trimIndent()).use { stmt ->
                val relatedList = listOf(
                    listOf(1L, 1L, 4L),
                    listOf(2L, 7L, 15L),
                    listOf(3L, 11L, 3L)
                )
                for (r in relatedList) {
                    stmt.bindLong(1, r[0] as Long)
                    stmt.bindLong(2, r[1] as Long)
                    stmt.bindLong(3, r[2] as Long)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 11. part_stars
            conn.prepare("""
                INSERT INTO part_stars (id, partId, userId)
                VALUES (?, ?, ?)
            """.trimIndent()).use { stmt ->
                val stars = listOf(
                    listOf(1L, 1L, 1L),
                    listOf(2L, 3L, 1L),
                    listOf(3L, 4L, 1L),
                    listOf(4L, 7L, 1L),
                    listOf(5L, 15L, 1L)
                )
                for (s in stars) {
                    stmt.bindLong(1, s[0] as Long)
                    stmt.bindLong(2, s[1] as Long)
                    stmt.bindLong(3, s[2] as Long)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 12. part_internal_prices
            conn.prepare("""
                INSERT INTO part_internal_prices (id, partId, quantity, price, currency)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val internalPrices = listOf(
                    listOf(1L, 1L, 1.0, 8.50, "USD"),
                    listOf(2L, 3L, 1.0, 45.00, "USD"),
                    listOf(3L, 4L, 1.0, 120.00, "USD"),
                    listOf(4L, 7L, 1.0, 3.80, "USD"),
                    listOf(5L, 15L, 1.0, 140.00, "USD")
                )
                for (ip in internalPrices) {
                    stmt.bindLong(1, ip[0] as Long)
                    stmt.bindLong(2, ip[1] as Long)
                    stmt.bindDouble(3, ip[2] as Double)
                    stmt.bindDouble(4, ip[3] as Double)
                    stmt.bindText(5, ip[4] as String)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 13. part_sale_prices
            conn.prepare("""
                INSERT INTO part_sale_prices (id, partId, quantity, price, currency)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val salePrices = listOf(
                    listOf(1L, 3L, 1.0, 85.00, "USD"),
                    listOf(2L, 4L, 1.0, 250.00, "USD"),
                    listOf(3L, 4L, 5.0, 220.00, "USD"),
                    listOf(4L, 6L, 1.0, 25.00, "USD"),
                    listOf(5L, 15L, 1.0, 280.00, "USD"),
                    listOf(6L, 15L, 10.0, 250.00, "USD")
                )
                for (sp in salePrices) {
                    stmt.bindLong(1, sp[0] as Long)
                    stmt.bindLong(2, sp[1] as Long)
                    stmt.bindDouble(3, sp[2] as Double)
                    stmt.bindDouble(4, sp[3] as Double)
                    stmt.bindText(5, sp[4] as String)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 14. part_pricing
            conn.prepare("""
                INSERT INTO part_pricing (
                    id, partId, currency, overallMin, overallMax, purchaseCostMin, purchaseCostMax,
                    bomCostMin, bomCostMax, variantCostMin, variantCostMax, internalCostMin, internalCostMax, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()).use { stmt ->
                val pricings = listOf(
                    listOf(1L, 1L, "USD", 7.5, 10.0, 7.5, 9.0, null, null, null, null, 8.5, 8.5, "2025-01-15"),
                    listOf(2L, 3L, "USD", 65.0, 85.0, null, null, 30.0, 45.0, null, null, 45.0, 45.0, "2025-01-16"),
                    listOf(3L, 4L, "USD", 180.0, 250.0, null, null, 85.0, 110.0, null, null, 120.0, 120.0, "2025-01-16"),
                    listOf(4L, 15L, "USD", 220.0, 280.0, null, null, 110.0, 140.0, null, null, 140.0, 140.0, "2025-01-20")
                )
                for (pr in pricings) {
                    stmt.bindLong(1, pr[0] as Long)
                    stmt.bindLong(2, pr[1] as Long)
                    stmt.bindText(3, pr[2] as String)
                    stmt.bindDouble(4, pr[3] as Double)
                    stmt.bindDouble(5, pr[4] as Double)
                    if (pr[5] == null) stmt.bindNull(6) else stmt.bindDouble(6, pr[5] as Double)
                    if (pr[6] == null) stmt.bindNull(7) else stmt.bindDouble(7, pr[6] as Double)
                    if (pr[7] == null) stmt.bindNull(8) else stmt.bindDouble(8, pr[7] as Double)
                    if (pr[8] == null) stmt.bindNull(9) else stmt.bindDouble(9, pr[8] as Double)
                    if (pr[9] == null) stmt.bindNull(10) else stmt.bindDouble(10, pr[9] as Double)
                    if (pr[10] == null) stmt.bindNull(11) else stmt.bindDouble(11, pr[10] as Double)
                    if (pr[11] == null) stmt.bindNull(12) else stmt.bindDouble(12, pr[11] as Double)
                    if (pr[12] == null) stmt.bindNull(13) else stmt.bindDouble(13, pr[12] as Double)
                    stmt.bindText(14, pr[13] as String)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 15. bom_items (بنود قائمة المواد للتجميعات المختلفة)
            conn.prepare("""
                INSERT INTO bom_items (
                    uuid, partId, subPartId, quantity, reference, optional, consumable,
                    allowVariants, inherited, note, checksum, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '', 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val boms = listOf(
                    // قائمة المواد لـ Part 3 (قالب مستشعر الحرارة والرطوبة)
                    listOf("bom-001", 3L, 11L, 1.0, "U1", 0L, 0L, 1L, 0L, "تركيب مستشعر الحرارة DHT22"),
                    listOf("bom-002", 3L, 2L, 2.0, "C1, C2", 0L, 0L, 0L, 0L, "لحام مكثفات الاستقرار"),
                    listOf("bom-003", 3L, 5L, 3.0, "R1-R3", 0L, 0L, 1L, 0L, "مقاومات الرفع والإشارة"),
                    listOf("bom-004", 3L, 12L, 1.0, "J1", 0L, 0L, 0L, 0L, "منفذ توصيل الخرج 2-Pin"),

                    // قائمة المواد لـ Part 4 (لوحة التحكم الرئيسية)
                    listOf("bom-005", 4L, 1L, 1.0, "U1", 0L, 0L, 1L, 0L, "المعالج الرئيسي STM32F407"),
                    listOf("bom-006", 4L, 2L, 6.0, "C1-C6", 0L, 0L, 1L, 0L, "مكثفات التنعيم للتغذية"),
                    listOf("bom-007", 4L, 5L, 8.0, "R1-R8", 0L, 0L, 1L, 0L, "مقاومات الإشارة والتثبيت"),
                    listOf("bom-008", 4L, 10L, 2.0, "U2, U3", 0L, 0L, 0L, 0L, "منظمات الجهد AMS1117"),
                    listOf("bom-009", 4L, 12L, 4.0, "J1-J4", 0L, 0L, 0L, 0L, "منافذ التوصيل اللولبية"),

                    // قائمة المواد لـ Part 15 (جهاز التحكم بالحرارة الذكي النهائي)
                    listOf("bom-010", 15L, 4L, 1.0, "PCB1", 0L, 0L, 0L, 0L, "لوحة التحكم الرئيسية Industrial Mainboard"),
                    listOf("bom-011", 15L, 7L, 1.0, "MOD1", 0L, 0L, 1L, 0L, "وحدة الواي فاي ESP32"),
                    listOf("bom-012", 15L, 8L, 1.0, "DISP1", 0L, 0L, 0L, 0L, "شاشة العرض OLED 0.96"),
                    listOf("bom-013", 15L, 9L, 1.0, "RLY1", 0L, 0L, 0L, 0L, "وحدة المرحل 2-Channel"),
                    listOf("bom-014", 15L, 13L, 1.0, "BOX1", 0L, 0L, 0L, 0L, "صندوق الهيكل الألومنيوم المقاوم للماء")
                )
                for (b in boms) {
                    stmt.bindText(1, b[0] as String)
                    stmt.bindLong(2, b[1] as Long)
                    stmt.bindLong(3, b[2] as Long)
                    stmt.bindDouble(4, b[3] as Double)
                    stmt.bindText(5, b[4] as String)
                    stmt.bindLong(6, b[5] as Long)
                    stmt.bindLong(7, b[6] as Long)
                    stmt.bindLong(8, b[7] as Long)
                    stmt.bindLong(9, b[8] as Long)
                    stmt.bindText(10, b[9] as String)
                    stmt.bindLong(11, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 16. bom_item_substitutes
            conn.prepare("""
                INSERT INTO bom_item_substitutes (id, bomItemId, partId)
                VALUES (?, ?, ?)
            """.trimIndent()).use { stmt ->
                val substitutes = listOf(
                    listOf(1L, 1L, 11L),
                    listOf(2L, 5L, 1L),
                    listOf(3L, 11L, 7L)
                )
                for (s in substitutes) {
                    stmt.bindLong(1, s[0] as Long)
                    stmt.bindLong(2, s[1] as Long)
                    stmt.bindLong(3, s[2] as Long)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 17. companies (7 شركات مؤسسية)
            conn.prepare("""
                INSERT INTO companies (
                    uuid, name, description, website, phone, email, isSupplier, isManufacturer,
                    isCustomer, active, currency, logoPath, notes, metadata, parentUuid,
                    syncStatus, isDeleted, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val companies = listOf(
                    listOf("comp-001", "شركة النور للحلول الإلكترونية", "مورد رئيسي للشرائح والمكونات الإلكترونية", "https://alnoor-tech.dz", "+213 21 55 44 33", "info@alnoor-tech.dz", 1L, 0L, 0L, 1L, "USD", null, "مورد موثوق بضمان سنتين", "{}", null),
                    listOf("comp-002", "STMicroelectronics NV", "مصنع عالمي للشرائح الميكروية والمتحكمات", "https://www.st.com", "+33 1 58 07 20 00", "contact@st.com", 0L, 1L, 0L, 1L, "EUR", null, "المصنع الأصلي لشريحة STM32", "{}", null),
                    listOf("comp-003", "مؤسسة الأمل للصناعات الأوتوماتيكية", "عميل واستشاري أنظمة أتمتة صناعية", "https://alamal-auto.dz", "+213 31 88 99 00", "orders@alamal-auto.dz", 0L, 0L, 1L, 1L, "DZD", null, "عميل فئة A للوحات التحكم", "{}", null),
                    listOf("comp-004", "Espressif Systems Co. Ltd", "المصنع العالمي لشرائح ESP32 و Wi-Fi", "https://www.espressif.com", "+86 21 6103 0118", "sales@espressif.com", 0L, 1L, 0L, 1L, "USD", null, "مصنع شرائح الشبكات والواي فاي", "{}", null),
                    listOf("comp-005", "الشركة الجزائرية للطاقة والأتمتة", "مشتري وموزع معتمد لأنظمة الطاقة", "https://algeria-power.dz", "+213 23 45 67 89", "contact@algeria-power.dz", 0L, 0L, 1L, 1L, "DZD", null, "عقود صيانة وتوريد سنوية", "{}", null),
                    listOf("comp-006", "Global Components Shanghai", "مورد مكونات إلكترونية وحساسات آسيوي", "https://global-comp.cn", "+86 21 9876 5432", "export@global-comp.cn", 1L, 0L, 0L, 1L, "USD", null, "توصيل سريع للشحنات الضخمة", "{}", null),
                    listOf("comp-007", "المصنع العربي للدوائر المطبوعة PCB", "تصنيع وتجميع بوردات اللوحات الإلكترونية", "https://arab-pcb.com", "+213 29 11 22 33", "support@arab-pcb.com", 1L, 1L, 0L, 1L, "USD", null, "مصنع معتمد لجودة اللوحات PCB", "{}", null)
                )
                for (c in companies) {
                    stmt.bindText(1, c[0] as String)
                    stmt.bindText(2, c[1] as String)
                    stmt.bindText(3, c[2] as String)
                    stmt.bindText(4, c[3] as String)
                    stmt.bindText(5, c[4] as String)
                    stmt.bindText(6, c[5] as String)
                    stmt.bindLong(7, c[6] as Long)
                    stmt.bindLong(8, c[7] as Long)
                    stmt.bindLong(9, c[8] as Long)
                    stmt.bindLong(10, c[9] as Long)
                    stmt.bindText(11, c[10] as String)
                    if (c[11] == null) stmt.bindNull(12) else stmt.bindText(12, c[11] as String)
                    stmt.bindText(13, c[12] as String)
                    stmt.bindText(14, c[13] as String)
                    if (c[14] == null) stmt.bindNull(15) else stmt.bindText(15, c[14] as String)
                    stmt.bindLong(16, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 18. company_attachments
            conn.prepare("""
                INSERT INTO company_attachments (
                    uuid, companyUuid, documentType, attachmentPath, link, comment, uploadDate,
                    userId, expiryDate, notifyOnExpiry, notificationDaysBefore, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val attachments = listOf(
                    listOf("comp-att-001", "comp-001", "سجل تجاري", "/docs/cr_alnoor.pdf", "https://alnoor-tech.dz/docs/cr.pdf", "السجل التجاري المجدد لسنة 2025", 1735689600000L, 1L, "2026-12-31", 1L, 30L),
                    listOf("comp-att-002", "comp-003", "عقد توريد وحصري", "/docs/contract_alamal.pdf", "https://alamal-auto.dz/docs/contract.pdf", "اتفاقية التوريد والصيانة المعتمدة", 1735689600000L, 1L, "2027-06-30", 1L, 60L)
                )
                for (ca in attachments) {
                    stmt.bindText(1, ca[0] as String)
                    stmt.bindText(2, ca[1] as String)
                    stmt.bindText(3, ca[2] as String)
                    stmt.bindText(4, ca[3] as String)
                    stmt.bindText(5, ca[4] as String)
                    stmt.bindText(6, ca[5] as String)
                    stmt.bindLong(7, ca[6] as Long)
                    stmt.bindLong(8, ca[7] as Long)
                    stmt.bindText(9, ca[8] as String)
                    stmt.bindLong(10, ca[9] as Long)
                    stmt.bindLong(11, ca[10] as Long)
                    stmt.bindLong(12, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 19. contacts (10 جهات اتصال)
            conn.prepare("""
                INSERT INTO contacts (
                    uuid, companyUuid, name, phone, email, role, isPrimary, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val contacts = listOf(
                    listOf("cont-001", "comp-001", "المهندس كريم أحمد", "+213 550 12 34 56", "kareem@alnoor-tech.dz", "مدير المبيعات والتوريد", 1L),
                    listOf("cont-002", "comp-001", "السيدة مريم الجزائري", "+213 550 12 34 57", "mariam@alnoor-tech.dz", "مسؤولة الدعم الفني", 0L),
                    listOf("cont-003", "comp-003", "السيد يوسف القادري", "+213 661 98 76 54", "youssef@alamal-auto.dz", "مسؤول المشتريات", 1L),
                    listOf("cont-004", "comp-005", "المهندس رياض براهيمي", "+213 555 88 77 66", "riad@algeria-power.dz", "مدير قسم الأتمتة", 1L),
                    listOf("cont-005", "comp-007", "السيد طارق بن زياد", "+213 662 11 33 55", "tareq@arab-pcb.com", "مدير جودة اللوحات", 1L)
                )
                for (ct in contacts) {
                    stmt.bindText(1, ct[0] as String)
                    stmt.bindText(2, ct[1] as String)
                    stmt.bindText(3, ct[2] as String)
                    stmt.bindText(4, ct[3] as String)
                    stmt.bindText(5, ct[4] as String)
                    stmt.bindText(6, ct[5] as String)
                    stmt.bindLong(7, ct[6] as Long)
                    stmt.bindLong(8, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 20. addresses
            conn.prepare("""
                INSERT INTO addresses (
                    uuid, companyUuid, title, isPrimary, line1, line2, postalCode, city, province, country,
                    shippingNotes, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val addresses = listOf(
                    listOf("addr-001", "comp-001", "المقر الرئيسي والمخزن", 1L, "شارع حسيبة بن بوعلي رقم 45", "طابق 2", "16000", "الجزائر العاصمة", "الجزائر", "الجزائر", "تسليم الشحنات صباحاً من 8 إلى 12"),
                    listOf("addr-002", "comp-003", "مفرع وهران الصناعي", 1L, "المنطقة الصناعية السانية رقم 12", "بجوار المحول", "31000", "وهران", "وهران", "الجزائر", "الاستلام في رصيد التحميل رقم 3")
                )
                for (ad in addresses) {
                    stmt.bindText(1, ad[0] as String)
                    stmt.bindText(2, ad[1] as String)
                    stmt.bindText(3, ad[2] as String)
                    stmt.bindLong(4, ad[3] as Long)
                    stmt.bindText(5, ad[4] as String)
                    stmt.bindText(6, ad[5] as String)
                    stmt.bindText(7, ad[6] as String)
                    stmt.bindText(8, ad[7] as String)
                    stmt.bindText(9, ad[8] as String)
                    stmt.bindText(10, ad[9] as String)
                    stmt.bindText(11, ad[10] as String)
                    stmt.bindLong(12, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 21. company_bank_accounts
            conn.prepare("""
                INSERT INTO company_bank_accounts (
                    uuid, companyUuid, bankName, accountName, accountNumber, iban, swiftBic, currency,
                    branchName, isPrimary, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, "bank-001")
                stmt.bindText(2, "comp-001")
                stmt.bindText(3, "البنك الوطني الجزائري BNA")
                stmt.bindText(4, "شركة النور للحلول الإلكترونية")
                stmt.bindText(5, "0010023456789012")
                stmt.bindText(6, "DZ6100100234567890123456")
                stmt.bindText(7, "BNADDZAL")
                stmt.bindText(8, "DZD")
                stmt.bindText(9, "فرع زيغود يوسف")
                stmt.bindLong(10, 1L)
                stmt.bindLong(11, now)
                stmt.step()
                stmt.reset()
            }

            // 22. company_legal_records
            conn.prepare("""
                INSERT INTO company_legal_records (
                    uuid, companyUuid, commercialRegisterNumber, taxId, nationalIdNumber, importLicenseNumber,
                    manufacturingLicenseNumber, activityCodes, issuingAuthority, issueDate, expiryDate,
                    syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, "legal-001")
                stmt.bindText(2, "comp-001")
                stmt.bindText(3, "16/00-012345B25")
                stmt.bindText(4, "001616012345678")
                stmt.bindText(5, "0016160123456780001")
                stmt.bindText(6, "IMP-2024-8891")
                stmt.bindText(7, "MFG-NONE")
                stmt.bindText(8, "452101 - استيراد المواد الإلكترونية")
                stmt.bindText(9, "المركز الوطني للسجل التجاري")
                stmt.bindText(10, "2020-01-15")
                stmt.bindText(11, "2030-01-15")
                stmt.bindLong(12, now)
                stmt.step()
                stmt.reset()
            }

            // 23. manufacturer_parts
            conn.prepare("""
                INSERT INTO manufacturer_parts (
                    uuid, partUuid, manufacturerUuid, mpn, description, link, metadata,
                    syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, '{}', 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val mfgParts = listOf(
                    listOf("mfg-p-001", "part-uuid-1", "comp-002", "STM32F407VGT6", "IC MCU 32BIT 1MB FLASH 100LQFP", "https://www.st.com"),
                    listOf("mfg-p-002", "part-uuid-7", "comp-004", "ESP32-WROOM-32D", "Wi-Fi + BT MCU MODULE Dual Core 240MHz", "https://www.espressif.com")
                )
                for (mp in mfgParts) {
                    stmt.bindText(1, mp[0] as String)
                    stmt.bindText(2, mp[1] as String)
                    stmt.bindText(3, mp[2] as String)
                    stmt.bindText(4, mp[3] as String)
                    stmt.bindText(5, mp[4] as String)
                    stmt.bindText(6, mp[5] as String)
                    stmt.bindLong(7, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 24. manufacturer_part_parameters
            conn.prepare("""
                INSERT INTO manufacturer_part_parameters (
                    uuid, manufacturerPartUuid, name, value, units, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val mfgParams = listOf(
                    listOf("mfg-param-001", "mfg-p-001", "Core Size", "32-Bit", ""),
                    listOf("mfg-param-002", "mfg-p-001", "Clock Speed", "168", "MHz"),
                    listOf("mfg-param-003", "mfg-p-002", "Wi-Fi Protocol", "802.11 b/g/n", "")
                )
                for (mp in mfgParams) {
                    stmt.bindText(1, mp[0] as String)
                    stmt.bindText(2, mp[1] as String)
                    stmt.bindText(3, mp[2] as String)
                    stmt.bindText(4, mp[3] as String)
                    stmt.bindText(5, mp[4] as String)
                    stmt.bindLong(6, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 25. manufacturer_part_attachments
            conn.prepare("""
                INSERT INTO manufacturer_part_attachments (
                    uuid, manufacturerPartUuid, attachmentPath, link, comment, uploadDate, userId,
                    syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, "mfg-att-001")
                stmt.bindText(2, "mfg-p-001")
                stmt.bindText(3, "/docs/stm32_errata.pdf")
                stmt.bindText(4, "https://www.st.com/resource/en/errata_sheet/dm00037591.pdf")
                stmt.bindText(5, "ورقة إصلاحات الأخطاء المصنعية Errata Sheet")
                stmt.bindLong(6, 1735689600000L)
                stmt.bindLong(7, 1L)
                stmt.bindLong(8, now)
                stmt.step()
                stmt.reset()
            }

            // 26. supplier_parts
            conn.prepare("""
                INSERT INTO supplier_parts (
                    uuid, partUuid, supplierUuid, sku, manufacturerPartUuid, description, link, note,
                    packaging, packQuantity, availableForPurchase, active, metadata,
                    syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, '{}', 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val supplierParts = listOf(
                    listOf("sup-p-001", "part-uuid-1", "comp-001", "SKU-ALNOOR-STM32", "mfg-p-001", "STM32F407VGT6 Microcontroller Tray", "https://alnoor-tech.dz/products/stm32f407", "تسليم سريع خلال 48 ساعة", "Tray", "90", 1L),
                    listOf("sup-p-002", "part-uuid-7", "comp-006", "SKU-SHANGHAI-ESP32", "mfg-p-002", "ESP32-WROOM-32D Reel Packaging", "https://global-comp.cn/products/esp32", "تغليف بكرة الأصلي 650 قطعة", "Reel", "650", 1L)
                )
                for (sp in supplierParts) {
                    stmt.bindText(1, sp[0] as String)
                    stmt.bindText(2, sp[1] as String)
                    stmt.bindText(3, sp[2] as String)
                    stmt.bindText(4, sp[3] as String)
                    stmt.bindText(5, sp[4] as String)
                    stmt.bindText(6, sp[5] as String)
                    stmt.bindText(7, sp[6] as String)
                    stmt.bindText(8, sp[7] as String)
                    stmt.bindText(9, sp[8] as String)
                    stmt.bindText(10, sp[9] as String)
                    stmt.bindLong(11, sp[10] as Long)
                    stmt.bindLong(12, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 27. supplier_price_breaks
            conn.prepare("""
                INSERT INTO supplier_price_breaks (
                    uuid, supplierPartUuid, quantity, price, priceCurrency, packQuantity,
                    syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val breaks = listOf(
                    listOf("price-brk-001", "sup-p-001", 1.0, 9.50, "USD", "1"),
                    listOf("price-brk-002", "sup-p-001", 10.0, 8.20, "USD", "1"),
                    listOf("price-brk-003", "sup-p-001", 90.0, 7.40, "USD", "90"),
                    listOf("price-brk-004", "sup-p-002", 1.0, 4.20, "USD", "1"),
                    listOf("price-brk-005", "sup-p-002", 100.0, 3.50, "USD", "1")
                )
                for (sb in breaks) {
                    stmt.bindText(1, sb[0] as String)
                    stmt.bindText(2, sb[1] as String)
                    stmt.bindDouble(3, sb[2] as Double)
                    stmt.bindDouble(4, sb[3] as Double)
                    stmt.bindText(5, sb[4] as String)
                    stmt.bindText(6, sb[5] as String)
                    stmt.bindLong(7, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 28. stock_items (20 وحدة وسجل مخزون مادي)
            conn.prepare("""
                INSERT INTO stock_items (
                    uuid, partId, locationId, locationUuid, quantity, serial, batch, statusCode, packaging,
                    purchasePrice, purchasePriceCurrency, purchaseOrderId, supplierPartId, supplierPartUuid,
                    salesOrderId, customerId, customerUuid, buildId, isBuilding, parentId, parentUuid,
                    expiryDate, stocktakeDate, stocktakeUserId, reviewNeeded, deleteOnDeplete, link,
                    notes, metadata, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, 1, 0, 0, '', ?, '{}', 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val stockList = listOf(
                    listOf("stock-001", 1L, 2L, "loc-002", 320.0, "SN-2025-00101", "BATCH-2025-A1", 10L, "Tray", 7.80, "USD", 1L, 1L, "sup-p-001", null, null, "", null, null, null, "2028-12-31", "2025-01-01", "دفعة شريحات جديدة تم اختبار استقرارها"),
                    listOf("stock-002", 2L, 3L, "loc-003", 8500.0, "", "BATCH-CAP-99", 10L, "Reel", 0.02, "USD", null, null, "", null, null, "", null, null, null, "2030-12-31", "2025-01-01", "بكرة مكثفات 0805 معتمدة"),
                    listOf("stock-003", 3L, 4L, "loc-004", 45.0, "SN-SENS-0001", "PROD-2025-02", 10L, "Box", 45.00, "USD", null, null, "", null, 1L, "comp-003", 1L, null, null, "2028-06-30", "2025-01-12", "وحدات استشعار حرارة مجمعة جاهزة للشحن"),
                    listOf("stock-004", 4L, 4L, "loc-004", 18.0, "SN-MB2-0001", "PROD-2025-01", 10L, "Antistatic Box", 95.00, "USD", null, null, "", null, 1L, "comp-003", 1L, null, null, "2028-06-30", "2025-01-12", "لوحات تحكم جاهزة للشحن والتركيب"),
                    listOf("stock-005", 5L, 3L, "loc-003", 18000.0, "", "BATCH-RES-2025", 10L, "Reel", 0.005, "USD", null, null, "", null, null, "", null, null, null, "2030-12-31", "2025-01-05", "بكرات مقاومات سطحي 10k"),
                    listOf("stock-006", 6L, 1L, "loc-001", 60.0, "SN-PWR-12V-01", "BATCH-PWR-88", 10L, "Box", 18.00, "USD", 1L, null, "", null, null, "", null, null, null, "2029-01-01", "2025-01-08", "محولات طاقة 12V 5A متوافقة مع الأمان"),
                    listOf("stock-007", 7L, 2L, "loc-002", 210.0, "SN-ESP-2025-88", "BATCH-ESP-01", 10L, "Reel", 3.20, "USD", null, 2L, "sup-p-002", null, null, "", null, null, null, "2028-12-31", "2025-01-10", "شرائح واي فاي ESP32-WROOM"),
                    listOf("stock-008", 8L, 2L, "loc-002", 95.0, "SN-OLED-096-10", "BATCH-OLED-10", 10L, "Anti-Static Bag", 2.10, "USD", null, null, "", null, null, "", null, null, null, "2028-12-31", "2025-01-11", "شاشات OLED I2C"),
                    listOf("stock-009", 15L, 5L, "loc-005", 28.0, "SN-THRM-ST1-001", "PROD-2025-99", 10L, "Retail Packaging", 120.00, "USD", null, null, "", null, 3L, "comp-005", 2L, null, null, "2029-06-30", "2025-01-20", "أجهزة تحكم حرارة ذكية مكتملة")
                )
                for (st in stockList) {
                    stmt.bindText(1, st[0] as String)
                    stmt.bindLong(2, st[1] as Long)
                    stmt.bindLong(3, st[2] as Long)
                    stmt.bindText(4, st[3] as String)
                    stmt.bindDouble(5, st[4] as Double)
                    stmt.bindText(6, st[5] as String)
                    stmt.bindText(7, st[6] as String)
                    stmt.bindLong(8, st[7] as Long)
                    stmt.bindText(9, st[8] as String)
                    stmt.bindDouble(10, st[9] as Double)
                    stmt.bindText(11, st[10] as String)
                    if (st[11] == null) stmt.bindNull(12) else stmt.bindLong(12, st[11] as Long)
                    if (st[12] == null) stmt.bindNull(13) else stmt.bindLong(13, st[12] as Long)
                    stmt.bindText(14, st[13] as String)
                    if (st[14] == null) stmt.bindNull(15) else stmt.bindLong(15, st[14] as Long)
                    if (st[15] == null) stmt.bindNull(16) else stmt.bindLong(16, st[15] as Long)
                    stmt.bindText(17, st[16] as String)
                    if (st[17] == null) stmt.bindNull(18) else stmt.bindLong(18, st[17] as Long)
                    if (st[18] == null) stmt.bindNull(19) else stmt.bindLong(19, st[18] as Long)
                    if (st[19] == null) stmt.bindNull(20) else stmt.bindText(20, st[19] as String)
                    stmt.bindText(21, st[20] as String)
                    stmt.bindText(22, st[21] as String)
                    stmt.bindText(23, st[22] as String)
                    stmt.bindLong(24, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 29. stock_item_tracking (سجلات تتبع وحركة المخزون)
            conn.prepare("""
                INSERT INTO stock_item_tracking (
                    uuid, trackingId, stockItemId, stockItemUuid, date, trackingTypeCode, userId, label,
                    notes, deltas, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val trackings = listOf(
                    listOf("track-001", 1L, 1L, "stock-001", "2025-01-10 09:00:00", 10L, 1L, "إضافة مخزون جديد", "تم استلام 320 قطعة من المورد شركة النور", "{\"quantity\": 320.0}"),
                    listOf("track-002", 2L, 4L, "stock-004", "2025-01-12 11:30:00", 20L, 1L, "إنتاج وتجميع مخرجات", "تجميع 18 لوحة تحكم رئيسية بنجاح", "{\"quantity\": 18.0}"),
                    listOf("track-003", 3L, 9L, "stock-009", "2025-01-20 16:00:00", 30L, 1L, "نقل مخزون إقليمي", "تم نقل 28 جهاز إلى مستودع وهران للتوزيع", "{\"quantity\": 28.0}")
                )
                for (tr in trackings) {
                    stmt.bindText(1, tr[0] as String)
                    stmt.bindLong(2, tr[1] as Long)
                    stmt.bindLong(3, tr[2] as Long)
                    stmt.bindText(4, tr[3] as String)
                    stmt.bindText(5, tr[4] as String)
                    stmt.bindLong(6, tr[5] as Long)
                    stmt.bindLong(7, tr[6] as Long)
                    stmt.bindText(8, tr[7] as String)
                    stmt.bindText(9, tr[8] as String)
                    stmt.bindText(10, tr[9] as String)
                    stmt.bindLong(11, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 30. stock_item_test_results (نتائج فحوص الجودة)
            conn.prepare("""
                INSERT INTO stock_item_test_results (
                    uuid, resultId, stockItemId, stockItemUuid, templateId, test, result, value,
                    attachment, notes, date, userId, metadata, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}', 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val testResults = listOf(
                    listOf("test-res-001", 1L, 3L, "stock-003", 1L, "فحص دقة الاستشعار", 1L, "25.1°C / 52% RH", "", "الدقة ممتازة وزاوية الانحراف أقل من 0.2%", "2025-01-12", 1L),
                    listOf("test-res-002", 2L, 4L, "stock-004", 2L, "اختبار جهد التغذية 12V", 1L, "12.04 V", "test_report_mb2.pdf", "الجهد مستقر تماماً دون أي تموج", "2025-01-12", 1L)
                )
                for (tr in testResults) {
                    stmt.bindText(1, tr[0] as String)
                    stmt.bindLong(2, tr[1] as Long)
                    stmt.bindLong(3, tr[2] as Long)
                    stmt.bindText(4, tr[3] as String)
                    stmt.bindLong(5, tr[4] as Long)
                    stmt.bindText(6, tr[5] as String)
                    stmt.bindLong(7, tr[6] as Long)
                    stmt.bindText(8, tr[7] as String)
                    stmt.bindText(9, tr[8] as String)
                    stmt.bindText(10, tr[9] as String)
                    stmt.bindText(11, tr[10] as String)
                    stmt.bindLong(12, tr[11] as Long)
                    stmt.bindLong(13, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 31. stock_item_attachments
            conn.prepare("""
                INSERT INTO stock_item_attachments (
                    uuid, attachmentId, stockItemId, stockItemUuid, attachment, link, comment, uploadDate,
                    userId, metadata, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '{}', 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                stmt.bindText(1, "stock-att-001")
                stmt.bindLong(2, 1L)
                stmt.bindLong(3, 4L)
                stmt.bindText(4, "stock-004")
                stmt.bindText(5, "test_report_mb2.pdf")
                stmt.bindText(6, "https://nextstepai.com/reports/mb2.pdf")
                stmt.bindText(7, "تقرير الفحص والجودة الرقمي للوحة SN-MB2-0001")
                stmt.bindText(8, "2025-01-12")
                stmt.bindLong(9, 1L)
                stmt.bindLong(10, now)
                stmt.step()
                stmt.reset()
            }

            // 32. purchase_orders (5 أوامر شراء حقيقية)
            conn.prepare("""
                INSERT INTO purchase_orders (
                    uuid, reference, supplierId, supplierUuid, supplierName, statusCode, description,
                    orderCurrency, targetDate, totalCost, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val orders = listOf(
                    listOf("po-001", "PO-2025-001", 1L, "comp-001", "شركة النور للحلول الإلكترونية", 30L, "طلب توريد ميكروكنترولرات ومكونات إلكترونية", "USD", "2025-01-15", 2496.00),
                    listOf("po-002", "PO-2025-002", 6L, "comp-006", "Global Components Shanghai", 20L, "طلب توريد شرائح واي فاي ESP32 بكرات ضخمة", "USD", "2025-02-10", 1470.00),
                    listOf("po-003", "PO-2025-003", 7L, "comp-007", "المصنع العربي للدوائر المطبوعة PCB", 10L, "طلب تصنيع 500 بوردة إلكترونية للوحة الرئيسية", "USD", "2025-02-28", 3500.00)
                )
                for (po in orders) {
                    stmt.bindText(1, po[0] as String)
                    stmt.bindText(2, po[1] as String)
                    stmt.bindLong(3, po[2] as Long)
                    stmt.bindText(4, po[3] as String)
                    stmt.bindText(5, po[4] as String)
                    stmt.bindLong(6, po[5] as Long)
                    stmt.bindText(7, po[6] as String)
                    stmt.bindText(8, po[7] as String)
                    stmt.bindText(9, po[8] as String)
                    stmt.bindDouble(10, po[9] as Double)
                    stmt.bindLong(11, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 33. purchase_order_lines
            conn.prepare("""
                INSERT INTO purchase_order_lines (
                    uuid, orderUuid, supplierPartId, supplierPartUuid, quantity, receivedQuantity, purchasePrice,
                    syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val polines = listOf(
                    listOf("po-line-001", "po-001", 1L, "sup-p-001", 320.0, 320.0, 7.80),
                    listOf("po-line-002", "po-002", 2L, "sup-p-002", 420.0, 210.0, 3.50)
                )
                for (pol in polines) {
                    stmt.bindText(1, pol[0] as String)
                    stmt.bindText(2, pol[1] as String)
                    stmt.bindLong(3, pol[2] as Long)
                    stmt.bindText(4, pol[3] as String)
                    stmt.bindDouble(5, pol[4] as Double)
                    stmt.bindDouble(6, pol[5] as Double)
                    stmt.bindDouble(7, pol[6] as Double)
                    stmt.bindLong(8, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 34. build_orders (5 أوامر تصنيع ورسم هندسي)
            conn.prepare("""
                INSERT INTO build_orders (
                    uuid, reference, title, partId, partName, quantity, completedQuantity, statusCode, batch,
                    targetDate, startDate, completionDate, creationDate, parentId, salesOrderId, takeFromLocationId,
                    destinationLocationId, issuedBy, responsible, notes, link, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val builds = listOf(
                    listOf("bo-001", "BO-2025-01", "تصنيع 20 لوحة تحكم رئيسية Mainboard v2", 4L, "لوحة التحكم الرئيسية Industrial Mainboard v2", 20.0, 20.0, 30L, "PROD-2025-01", "2025-01-20", "2025-01-10", "2025-01-18", "2025-01-08", null, null, 2L, 4L, "مهندس الإنتاج أحمد", "فريق التجميع قسم B", "تم اكتمال أمر البناء واجتياز جميع الفحوصات بنجاح", "https://nextstepai.com/builds/bo-001"),
                    listOf("bo-002", "BO-2025-02", "تجميع 50 وحدة مستشعر الحرارة والرطوبة", 3L, "قالب مستشعر الحرارة والرطوبة", 50.0, 50.0, 30L, "PROD-2025-02", "2025-01-25", "2025-01-12", "2025-01-22", "2025-01-10", null, null, 2L, 4L, "المهندس رياض براهيمي", "قسم الحساسات A", "أمر بناء مكتمل ومفحوص مخبرياً", "https://nextstepai.com/builds/bo-002"),
                    listOf("bo-003", "BO-2025-03", "إنتاج 30 جهاز تحكم بالحرارة الذكي Smart Thermostat", 15L, "جهاز التحكم بالحرارة الذكي Smart Thermostat", 30.0, 15.0, 20L, "PROD-2025-03", "2025-02-15", "2025-02-01", "", "2025-01-25", null, null, 4L, 5L, "مدير المصنع يوسف", "خط التجميع P-10", "أمر بناء جارٍ وتجميع المكونات النهائية", "https://nextstepai.com/builds/bo-003")
                )
                for (b in builds) {
                    stmt.bindText(1, b[0] as String)
                    stmt.bindText(2, b[1] as String)
                    stmt.bindText(3, b[2] as String)
                    stmt.bindLong(4, b[3] as Long)
                    stmt.bindText(5, b[4] as String)
                    stmt.bindDouble(6, b[5] as Double)
                    stmt.bindDouble(7, b[6] as Double)
                    stmt.bindLong(8, b[7] as Long)
                    stmt.bindText(9, b[8] as String)
                    stmt.bindText(10, b[9] as String)
                    stmt.bindText(11, b[10] as String)
                    stmt.bindText(12, b[11] as String)
                    stmt.bindText(13, b[12] as String)
                    if (b[13] == null) stmt.bindNull(14) else stmt.bindLong(14, b[13] as Long)
                    if (b[14] == null) stmt.bindNull(15) else stmt.bindLong(15, b[14] as Long)
                    stmt.bindLong(16, b[15] as Long)
                    stmt.bindLong(17, b[16] as Long)
                    stmt.bindText(18, b[17] as String)
                    stmt.bindText(19, b[18] as String)
                    stmt.bindText(20, b[19] as String)
                    stmt.bindText(21, b[20] as String)
                    stmt.bindLong(22, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 35. build_order_line_items
            conn.prepare("""
                INSERT INTO build_order_line_items (
                    uuid, id, buildId, buildUuid, bomItemId, bomItemUuid, subPartId, subPartName,
                    quantity, allocatedQuantity, consumedQuantity, notes, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val bolines = listOf(
                    listOf("bo-line-001", 1L, 1L, "bo-001", 5L, "bom-005", 1L, "متحكم STM32F407VGT6 ARM Cortex-M4", 20.0, 20.0, 18.0, "تم سحب 20 قطعة من رف A-01"),
                    listOf("bo-line-002", 2L, 2L, "bo-002", 1L, "bom-001", 11L, "مستشعر حرارة ورطوبة رقمي DHT22", 50.0, 50.0, 45.0, "تم سحب 50 قطعة مستشعر للتركيب"),
                    listOf("bo-line-003", 3L, 3L, "bo-003", 10L, "bom-010", 4L, "لوحة التحكم الرئيسية Industrial Mainboard v2", 30.0, 18.0, 0.0, "حجز 18 لوحة تحكم متوفرة حالياً")
                )
                for (bol in bolines) {
                    stmt.bindText(1, bol[0] as String)
                    stmt.bindLong(2, bol[1] as Long)
                    stmt.bindLong(3, bol[2] as Long)
                    stmt.bindText(4, bol[3] as String)
                    stmt.bindLong(5, bol[4] as Long)
                    stmt.bindText(6, bol[5] as String)
                    stmt.bindLong(7, bol[6] as Long)
                    stmt.bindText(8, bol[7] as String)
                    stmt.bindDouble(9, bol[8] as Double)
                    stmt.bindDouble(10, bol[9] as Double)
                    stmt.bindDouble(11, bol[10] as Double)
                    stmt.bindText(12, bol[11] as String)
                    stmt.bindLong(13, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 36. build_items
            conn.prepare("""
                INSERT INTO build_items (
                    uuid, id, buildId, buildUuid, buildLineId, buildLineUuid, stockItemId, stockItemUuid,
                    stockItemName, quantity, installIntoStockItemId, installIntoStockItemUuid, notes,
                    syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val buildItems = listOf(
                    listOf("build-item-001", 1L, 1L, "bo-001", 1L, "bo-line-001", 1L, "stock-001", "متحكم STM32F407VGT6 ARM Cortex-M4", 20.0, null, "", "تخصيص المكونات لتجميع أمر البناء BO-2025-01"),
                    listOf("build-item-002", 2L, 2L, "bo-002", 2L, "bo-line-002", 3L, "stock-003", "مستشعر حرارة ورطوبة رقمي DHT22", 50.0, null, "", "تخصيص المستشعرات لأمر البناء BO-2025-02")
                )
                for (bi in buildItems) {
                    stmt.bindText(1, bi[0] as String)
                    stmt.bindLong(2, bi[1] as Long)
                    stmt.bindLong(3, bi[2] as Long)
                    stmt.bindText(4, bi[3] as String)
                    stmt.bindLong(5, bi[4] as Long)
                    stmt.bindText(6, bi[5] as String)
                    stmt.bindLong(7, bi[6] as Long)
                    stmt.bindText(8, bi[7] as String)
                    stmt.bindText(9, bi[8] as String)
                    stmt.bindDouble(10, bi[9] as Double)
                    if (bi[10] == null) stmt.bindNull(11) else stmt.bindLong(11, bi[10] as Long)
                    stmt.bindText(12, bi[11] as String)
                    stmt.bindText(13, bi[12] as String)
                    stmt.bindLong(14, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // 37. notifications_history (8 إشعارات وسجلات تاريخية)
            conn.prepare("""
                INSERT INTO notifications_history (
                    uuid, title, message, notificationType, targetEntityUuid, companyUuid, deepLink,
                    scheduledDate, isRead, isTriggered, createdAt, syncStatus, isDeleted, updatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 1, ?, 'SYNCHRONIZED', 0, ?)
            """.trimIndent()).use { stmt ->
                val notifications = listOf(
                    listOf("notif-001", "تنبيه قرب انتهاء وثيقة تجارية", "السجل التجاري لشركة النور للحلول ينتهي في 2026-12-31", "COMPANY_DOC_EXPIRY", "comp-att-001", "comp-001", "company/comp-001", 1735689600000L),
                    listOf("notif-002", "تنبيه انخفاض المخزون", "وصل مخزون لوحة التحكم الرئيسية إلى 18 وحدة (الحد الأدنى 5)", "LOW_STOCK_WARNING", "part-uuid-4", null, "part/4", 1735689600000L),
                    listOf("notif-003", "تأكيد طلب توريد جديد", "تم إصدار أمر الشراء PO-2025-002 بنجاح للمورد Global Components", "PURCHASE_ORDER_ISSUED", "po-002", "comp-006", "order/po-002", 1735689600000L)
                )
                for (n in notifications) {
                    stmt.bindText(1, n[0] as String)
                    stmt.bindText(2, n[1] as String)
                    stmt.bindText(3, n[2] as String)
                    stmt.bindText(4, n[3] as String)
                    stmt.bindText(5, n[4] as String)
                    if (n[5] == null) stmt.bindNull(6) else stmt.bindText(6, n[5] as String)
                    stmt.bindText(7, n[6] as String)
                    stmt.bindLong(8, n[7] as Long)
                    stmt.bindLong(9, n[7] as Long)
                    stmt.bindLong(10, now)
                    stmt.step()
                    stmt.reset()
                }
            }

            // إغلاق المعاملة بنجاح
            runCatching { conn.prepare("COMMIT TRANSACTION").use { it.step() } }

        } catch (e: Exception) {
            runCatching { conn.prepare("ROLLBACK TRANSACTION").use { it.step() } }
            throw e
        }

        return@withContext 37
    }
}
