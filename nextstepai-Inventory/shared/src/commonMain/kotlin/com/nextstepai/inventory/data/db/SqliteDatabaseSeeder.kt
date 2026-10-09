package com.nextstepai.inventory.data.db

import androidx.sqlite.SQLiteConnection

/**
 * وحدة إدراج الإعدادات الافتراضية والبيانات الأولية بأمان (Database Seeder).
 */
internal object SqliteDatabaseSeeder {

    fun seedDefaultsAndInitialData(conn: SQLiteConnection) {
        runCatching {
            conn.prepare("""
                INSERT OR IGNORE INTO app_settings (
                    uuid, notificationTime, soundEnabled, vibrationEnabled, docExpiryWarningDays,
                    lowStockAlertsEnabled, themeMode, language, defaultCurrency, scannerBeepEnabled,
                    biometricLockEnabled, syncWifiOnly
                ) VALUES ('default-settings', '09:00', 1, 1, 30, 1, 'SYSTEM', 'ar', 'USD', 1, 0, 0);
            """.trimIndent()).use { it.step() }
        }

        runCatching {
            conn.prepare("""
                INSERT OR IGNORE INTO inflow_preferences (uuid, pinnedInflowIds, customInflowText)
                VALUES ('default-inflow', 'PURCHASE_ORDER,INTERNAL_BUILD', '');
            """.trimIndent()).use { it.step() }
        }

        seedInitialData(conn)
    }

    private fun seedInitialData(conn: SQLiteConnection) {
        // إدراج المستخدمين الافتراضيين
        runCatching {
            val userCount = conn.prepare("SELECT COUNT(*) FROM app_users").use { stmt ->
                if (stmt.step()) stmt.getLong(0) else 0L
            }
            if (userCount == 0L) {
                val seedUsers = listOf(
                    Triple("usr-001", "مدير الإنتاج والتصنيع", "مدير الإنتاج والتصنيع"),
                    Triple("usr-002", "مشرف خط التجميع", "مشرف خط التجميع"),
                    Triple("usr-003", "مهندس الجودة والسلامة", "مهندس الجودة السلامة"),
                    Triple("usr-004", "مدير المستودع والخدمات اللوجستية", "مدير المستودع"),
                    Triple("usr-005", "مدير النظام (Admin)", "مدير النظام"),
                    Triple("usr-006", "فريق التشغيل والتجميع", "فريق التشغيل")
                )
                seedUsers.forEach { (uuid, name, role) ->
                    conn.prepare("""
                        INSERT OR IGNORE INTO app_users (uuid, name, role, active, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid)
                        VALUES (?, ?, ?, 1, 1, 'SYNCHRONIZED', 0, 1700000000000, 'dev-local');
                    """.trimIndent()).use { stmt ->
                        stmt.bindText(1, uuid)
                        stmt.bindText(2, name)
                        stmt.bindText(3, role)
                        stmt.step()
                    }
                }
            }
        }

        // إدراج أنواع مواقع التخزين الافتراضية
        runCatching {
            val typeCount = conn.prepare("SELECT COUNT(*) FROM stock_location_types").use { stmt ->
                if (stmt.step()) stmt.getLong(0) else 0L
            }
            if (typeCount == 0L) {
                conn.prepare("""
                    INSERT OR IGNORE INTO stock_location_types (
                        uuid, name, description, icon, customIcon,
                        length, width, height, maxWeight, maxVolume,
                        metadata, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
                    ) VALUES ('location-type-4', 'moukim', '', 'warehouse', '', 0.0, 0.0, 0.0, 0.0, 0.0, '{}', 1, 'SYNCHRONIZED', 0, 1789227837167, 'dev-local');
                """.trimIndent()).use { stmt -> stmt.step() }
            }
        }

        // إدراج التصنيفات والموقع والمحتويات النموذجية إذا كانت الجداول فارغة
        runCatching {
            val partCount = conn.prepare("SELECT COUNT(*) FROM parts").use { stmt ->
                if (stmt.step()) stmt.getLong(0) else 0L
            }
            if (partCount == 0L) {
                val seedCategories = listOf(
                    listOf("cat-001","الكترونيات وصنع اللوحات",null,"المكونات الإلكترونية الدقيقة والشرائح",1,"loc-001"),
                    listOf("cat-002","المكونات السلبية (Passive)","cat-001","المكثفات والمقاومات والملفات",0,"loc-002"),
                    listOf("cat-003","المتحكمات والمعالجات","cat-001","المتحكمات الدقيقة ARM و AVR و ESP",0,"loc-002"),
                    listOf("cat-004","أنظمة وإمدادات الطاقة",null,"محولات الجهد والبطاريات والمزودات",0,"loc-001"),
                    listOf("cat-005","الهياكل والأجزاء الميكانيكية",null,"علب التغليف والمشتتات والزنبركات",0,"loc-003"),
                    listOf("cat-006","المستشعرات والمقاييس","cat-001","مستشعرات الحرارة والرطوبة والضغط والحركة",0,"loc-002"),
                    listOf("cat-007","الموصلات والكابلات (Connectors)","cat-001","كابلات الشريط والمقابس والمنافذ",0,"loc-002"),
                    listOf("cat-008","الشاشات ووحدات العرض","cat-001","شاشات OLED و LCD ومصفوفات LED",0,"loc-002"),
                    listOf("cat-009","المواد الخام والكيميائية",null,"قصدير اللحام ومذيبات IPA والمعجون الحراري",0,"loc-004"),
                    listOf("cat-010","قطع الغيار والصيانة",null,"شفرات قطع، محركات بديلة، ورؤوس الكاوية",0,"loc-004"),
                    listOf("cat-011","مواد التغليف والتعبئة",null,"كراتين وأكياس مضادة للكهرباء الساكنة",0,"loc-004"),
                    listOf("cat-012","المنتجات التامة والتجميعات",null,"المنتجات المكتملة المصنعة الجاهزة للبيع",1,"loc-003")
                )
                seedCategories.forEach { row ->
                    conn.prepare("""
                        INSERT OR IGNORE INTO part_categories (uuid, name, parentUuid, description, structural, defaultLocationUuid, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid)
                        VALUES (?, ?, ?, ?, ?, ?, 1, 'SYNCHRONIZED', 0, 1738000000000, 'dev-local');
                    """.trimIndent()).use { stmt ->
                        stmt.bindText(1, row[0] as String)
                        stmt.bindText(2, row[1] as String)
                        if (row[2] != null) stmt.bindText(3, row[2] as String) else stmt.bindNull(3)
                        stmt.bindText(4, row[3] as String)
                        stmt.bindLong(5, (row[4] as Int).toLong())
                        if (row[5] != null) stmt.bindText(6, row[5] as String) else stmt.bindNull(6)
                        stmt.step()
                    }
                }

                val seedParts = listOf(
                    listOf("part-1","مقاومة 100K Ohm","RES-10K-001","مقاومة كربونية 1/4 واط بنسبة سماحية 5%","cat-001","pcs",100.0,250.0,0,1,0,0,1,0),
                    listOf("part-2","متحكم ESP32 Wi-Fi/BT","MCU-ESP32-WROOM","وحدة متحكم دقيق ESP32 مزود بـ Wi-Fi و Bluetooth","cat-002","pcs",10.0,8.0,0,1,0,0,1,1),
                    listOf("part-3","قالب مستشعر الحرارة والرطوبة","TMP-SENSOR-TMPL","قالب تجريدي لسلسلة مستشعرات الحرارة","cat-008","pcs",0.0,0.0,1,0,1,0,0,0),
                    listOf("part-4","مستشعر DHT22 الدقيق","TMP-SENSOR-DHT22","مستشعر حرارة ورطوبة رقمي عالي الدقة","cat-008","pcs",5.0,30.0,1,1,0,0,1,1),
                    listOf("part-5","محرك تحريك ميكانيكي Servo","GEN-0001","محرك سيرفو صغير للتجميعات","cat-005","unit",20.0,0.0,1,0,1,0,0,0)
                )
                seedParts.forEach { row ->
                    conn.prepare("""
                        INSERT OR IGNORE INTO parts (
                            uuid, id, name, ipn, description, categoryUuid, units, minimumStock, totalInStock,
                            assembly, component, isTemplate, trackable, purchaseable, salable,
                            version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'SYNCHRONIZED', 0, 1738000000000, 'dev-local');
                    """.trimIndent()).use { stmt ->
                        stmt.bindText(1, row[0] as String)
                        stmt.bindLong(2, (row[0] as String).removePrefix("part-").toLongOrNull() ?: 0L)
                        stmt.bindText(3, row[1] as String)
                        stmt.bindText(4, row[2] as String)
                        stmt.bindText(5, row[3] as String)
                        stmt.bindText(6, row[4] as String)
                        stmt.bindText(7, row[5] as String)
                        stmt.bindDouble(8, row[6] as Double)
                        stmt.bindDouble(9, row[7] as Double)
                        stmt.bindLong(10, (row[8] as Int).toLong())
                        stmt.bindLong(11, (row[9] as Int).toLong())
                        stmt.bindLong(12, (row[10] as Int).toLong())
                        stmt.bindLong(13, (row[11] as Int).toLong())
                        stmt.bindLong(14, (row[12] as Int).toLong())
                        stmt.bindLong(15, (row[13] as Int).toLong())
                        stmt.step()
                    }
                }

                val seedLocations = listOf(
                    listOf("loc-001","المستودع المركزي - الجزائر العاصمة","المستودع الرئيسي للمواد والقطع",null,1,0,"WAREHOUSE"),
                    listOf("loc-002","رف الشرائح والدائريات A-01","مخصص للمتحكمات والشريحات SMD","loc-001",1,0,"SHELF"),
                    listOf("loc-003","رف المكونات السلبية B-02","مخصص للمكثفات والمقاومات والملفات","loc-001",1,0,"SHELF"),
                    listOf("loc-004","خط الإنتاج والتجميع الرئيسي P-10","موقع تجميع اللوحات وأوامر البناء","loc-001",1,0,"LINE"),
                    listOf("loc-005","مخزن وهران للتوزيع الغربي","مستودع فرعي للشحن الإقليمي",null,1,0,"WAREHOUSE"),
                    listOf("loc-006","منطقة الفحص والجودة (Quarantine Zone)","منطقة عزل المنتجات قيد الفحص","loc-001",1,0,"AREA")
                )
                seedLocations.forEach { row ->
                    conn.prepare("""
                        INSERT OR IGNORE INTO stock_locations (uuid, name, description, parentUuid, structural, external, locationType, version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1, 'SYNCHRONIZED', 0, 1738000000000, 'dev-local');
                    """.trimIndent()).use { stmt ->
                        stmt.bindText(1, row[0] as String)
                        stmt.bindText(2, row[1] as String)
                        stmt.bindText(3, row[2] as String)
                        if (row[3] != null) stmt.bindText(4, row[3] as String) else stmt.bindNull(4)
                        stmt.bindLong(5, (row[4] as Int).toLong())
                        stmt.bindLong(6, (row[5] as Int).toLong())
                        stmt.bindText(7, row[6] as String)
                        stmt.step()
                    }
                }
            }
        }
    }
}
