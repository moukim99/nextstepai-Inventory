package com.nextstepai.inventory.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/**
 * مدير قاعدة بيانات SQLite المحلية النظيفة المعتمدة بنسبة 100% على معرّفات UUID و UUIDv7.
 * تدعم القفل المتفائل (version) وتتبع جهاز التعديل (lastModifiedByDeviceUuid) وسجلات الحركات الإلحاقية (Append-only Ledger).
 */
object SqliteDatabaseManager {
    private var customDatabasePath: String? = null
    private var connection: SQLiteConnection? = null

    fun setCustomDatabasePath(path: String?) {
        synchronized(this) {
            closeDatabase()
            customDatabasePath = path
        }
    }

    fun getActiveDatabasePath(): String {
        return customDatabasePath ?: getDatabasePath()
    }

    fun getConnection(): SQLiteConnection {
        return connection ?: synchronized(this) {
            connection ?: openDatabase().also { connection = it }
        }
    }

    fun closeDatabase() {
        synchronized(this) {
            runCatching { connection?.close() }
            connection = null
        }
    }

    private fun openDatabase(): SQLiteConnection {
        val dbPath = getActiveDatabasePath()
        val dbFile = File(dbPath)

        val rawConn = try {
            val driver = BundledSQLiteDriver()
            driver.open(dbPath)
        } catch (e: Throwable) {
            runCatching {
                val jdbcConn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
                JdbcSqliteConnection(jdbcConn)
            }.getOrElse {
                throw IllegalStateException(
                    "Unable to open the inventory database without risking data loss.",
                    e
                )
            }
        }

        return try {
            runCatching {
                rawConn.prepare("PRAGMA journal_mode = DELETE;").use { it.step() }
                rawConn.prepare("PRAGMA busy_timeout = 10000;").use { it.step() }
            }

            // 1. إنشاء الجداول الأساسية إن لم تكن موجودة مسبقاً
            createTables(rawConn)

            // 2. الترحيل التلقائي: تجنب الفشل في حال كان العمود أو الجدول موجوداً مسبقاً
            addColumnIfMissing(
                rawConn,
                "ALTER TABLE notifications_history ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;"
            )
            addColumnIfMissing(
                rawConn,
                "ALTER TABLE notifications_history ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;"
            )
            addColumnIfMissing(
                rawConn,
                "ALTER TABLE notifications_history ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0;"
            )
            addColumnIfMissing(
                rawConn,
                "ALTER TABLE app_settings ADD COLUMN uuid TEXT NOT NULL DEFAULT 'default-settings';"
            )
            addColumnIfMissing(
                rawConn,
                "ALTER TABLE bom_items ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;"
            )
            addColumnIfMissing(
                rawConn,
                "ALTER TABLE bom_items ADD COLUMN subPartId INTEGER NOT NULL DEFAULT 0;"
            )

            ThreadSafeSQLiteConnection(rawConn)
        } catch (e: Throwable) {
            try { rawConn.close() } catch (_: Throwable) {}
            throw IllegalStateException(
                "Inventory database initialization or migration failed; existing data was preserved.",
                e
            )
        }
    }

    private fun addColumnIfMissing(conn: SQLiteConnection, sql: String) {
        try {
            conn.prepare(sql).use { it.step() }
        } catch (e: Throwable) {
            val msg = e.message.orEmpty()
            val isDuplicate = msg.contains("duplicate column name", ignoreCase = true)
            val isNoSuchTable = msg.contains("no such table", ignoreCase = true)
            if (!isDuplicate && !isNoSuchTable) {
                throw e
            }
        }
    }

    private fun createTables(conn: SQLiteConnection) {
        // 1. جداول النظام المحلية (Local-Only - لا تتزامن)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS app_settings (
                uuid TEXT PRIMARY KEY NOT NULL DEFAULT 'default-settings',
                notificationTime TEXT NOT NULL DEFAULT '09:00',
                soundEnabled INTEGER NOT NULL DEFAULT 1,
                vibrationEnabled INTEGER NOT NULL DEFAULT 1,
                docExpiryWarningDays INTEGER NOT NULL DEFAULT 30,
                lowStockAlertsEnabled INTEGER NOT NULL DEFAULT 1,
                themeMode TEXT NOT NULL DEFAULT 'SYSTEM',
                language TEXT NOT NULL DEFAULT 'ar',
                defaultCurrency TEXT NOT NULL DEFAULT 'USD',
                scannerBeepEnabled INTEGER NOT NULL DEFAULT 1,
                biometricLockEnabled INTEGER NOT NULL DEFAULT 0,
                syncWifiOnly INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            INSERT OR IGNORE INTO app_settings (
                uuid, notificationTime, soundEnabled, vibrationEnabled, docExpiryWarningDays,
                lowStockAlertsEnabled, themeMode, language, defaultCurrency, scannerBeepEnabled,
                biometricLockEnabled, syncWifiOnly
            ) VALUES ('default-settings', '09:00', 1, 1, 30, 1, 'SYSTEM', 'ar', 'USD', 1, 0, 0);
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS inflow_preferences (
                uuid TEXT PRIMARY KEY NOT NULL DEFAULT 'default-inflow',
                pinnedInflowIds TEXT NOT NULL DEFAULT 'PURCHASE_ORDER,INTERNAL_BUILD',
                customInflowText TEXT NOT NULL DEFAULT ''
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            INSERT OR IGNORE INTO inflow_preferences (uuid, pinnedInflowIds, customInflowText)
            VALUES ('default-inflow', 'PURCHASE_ORDER,INTERNAL_BUILD', '');
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS app_users (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT '',
                active INTEGER NOT NULL DEFAULT 1,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 2. التصنيفات والمواد (Parts & Categories)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_categories (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                parentUuid TEXT,
                description TEXT NOT NULL DEFAULT '',
                structural INTEGER NOT NULL DEFAULT 0,
                defaultLocationUuid TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS parts (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                ipn TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                categoryUuid TEXT,
                units TEXT NOT NULL DEFAULT 'pcs',
                minimumStock REAL NOT NULL DEFAULT 0.0,
                maximumStock REAL,
                totalInStock REAL NOT NULL DEFAULT 0.0,
                revision TEXT NOT NULL DEFAULT '',
                keywords TEXT NOT NULL DEFAULT '',
                assembly INTEGER NOT NULL DEFAULT 0,
                component INTEGER NOT NULL DEFAULT 1,
                isTemplate INTEGER NOT NULL DEFAULT 0,
                variantOfUuid TEXT,
                trackable INTEGER NOT NULL DEFAULT 0,
                purchaseable INTEGER NOT NULL DEFAULT 1,
                salable INTEGER NOT NULL DEFAULT 0,
                virtual INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                locked INTEGER NOT NULL DEFAULT 0,
                defaultLocationUuid TEXT,
                defaultExpiryDays INTEGER,
                link TEXT NOT NULL DEFAULT '',
                localImagePath TEXT,
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 3. قائمة المواد والتصنيع (BOM)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS bom_items (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL DEFAULT '',
                subPartUuid TEXT NOT NULL DEFAULT '',
                partId INTEGER NOT NULL DEFAULT 0,
                subPartId INTEGER NOT NULL DEFAULT 0,
                quantity REAL NOT NULL DEFAULT 1.0,
                reference TEXT NOT NULL DEFAULT '',
                optional INTEGER NOT NULL DEFAULT 0,
                consumable INTEGER NOT NULL DEFAULT 0,
                allowVariants INTEGER NOT NULL DEFAULT 0,
                inherited INTEGER NOT NULL DEFAULT 0,
                note TEXT NOT NULL DEFAULT '',
                checksum TEXT NOT NULL DEFAULT '',
                phaseUuid TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS bom_item_substitutes (
                uuid TEXT PRIMARY KEY NOT NULL,
                bomItemUuid TEXT NOT NULL,
                substitutePartUuid TEXT NOT NULL,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 4. المواقع والمخزون (Locations & Stock)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_location_types (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL UNIQUE,
                description TEXT NOT NULL DEFAULT '',
                icon TEXT NOT NULL DEFAULT 'warehouse',
                customIcon TEXT NOT NULL DEFAULT '',
                length REAL NOT NULL DEFAULT 0.0,
                width REAL NOT NULL DEFAULT 0.0,
                height REAL NOT NULL DEFAULT 0.0,
                maxWeight REAL NOT NULL DEFAULT 0.0,
                maxVolume REAL NOT NULL DEFAULT 0.0,
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_locations (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                parentUuid TEXT,
                structural INTEGER NOT NULL DEFAULT 0,
                external INTEGER NOT NULL DEFAULT 0,
                locationTypeUuid TEXT,
                locationType TEXT NOT NULL DEFAULT 'SHELF',
                customCapacity REAL,
                isBulkGenerated INTEGER NOT NULL DEFAULT 0,
                address TEXT NOT NULL DEFAULT '',
                icon TEXT NOT NULL DEFAULT 'warehouse',
                customIcon TEXT NOT NULL DEFAULT '',
                level INTEGER NOT NULL DEFAULT 0,
                lft INTEGER NOT NULL DEFAULT 0,
                rght INTEGER NOT NULL DEFAULT 0,
                treeId INTEGER NOT NULL DEFAULT 1,
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_items (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                locationUuid TEXT,
                quantity REAL NOT NULL DEFAULT 1.0,
                serial TEXT NOT NULL DEFAULT '',
                batch TEXT NOT NULL DEFAULT '',
                statusCode INTEGER NOT NULL DEFAULT 10,
                packaging TEXT NOT NULL DEFAULT 'Box',
                expiryDate TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                purchasePrice REAL NOT NULL DEFAULT 0.0,
                purchasePriceCurrency TEXT NOT NULL DEFAULT 'USD',
                purchaseOrderUuid TEXT,
                supplierPartUuid TEXT NOT NULL DEFAULT '',
                salesOrderUuid TEXT,
                customerUuid TEXT NOT NULL DEFAULT '',
                buildUuid TEXT,
                isBuilding INTEGER NOT NULL DEFAULT 0,
                parentStockItemUuid TEXT,
                stocktakeDate TEXT NOT NULL DEFAULT '',
                stocktakeUserUuid TEXT,
                reviewNeeded INTEGER NOT NULL DEFAULT 0,
                deleteOnDeplete INTEGER NOT NULL DEFAULT 0,
                link TEXT NOT NULL DEFAULT '',
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // جدول سجلات الحركات (Append-only Ledger)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_item_tracking (
                uuid TEXT PRIMARY KEY NOT NULL,
                stockItemUuid TEXT NOT NULL,
                trackingTypeCode INTEGER NOT NULL DEFAULT 10,
                label TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                deltas TEXT NOT NULL DEFAULT '{}',
                userUuid TEXT,
                createdAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_item_test_results (
                uuid TEXT PRIMARY KEY NOT NULL,
                stockItemUuid TEXT NOT NULL,
                templateUuid TEXT,
                test TEXT NOT NULL,
                result INTEGER NOT NULL DEFAULT 1,
                value TEXT NOT NULL DEFAULT 'Passed',
                attachment TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                date TEXT NOT NULL DEFAULT '2025-02-15',
                userUuid TEXT,
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_item_attachments (
                uuid TEXT PRIMARY KEY NOT NULL,
                stockItemUuid TEXT NOT NULL,
                attachment TEXT,
                link TEXT,
                comment TEXT NOT NULL DEFAULT '',
                uploadDate TEXT NOT NULL DEFAULT '2025-02-15',
                userUuid TEXT,
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 5. الشركات والعناوين والموردين (Companies & Partners)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS companies (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                isSupplier INTEGER NOT NULL DEFAULT 1,
                isManufacturer INTEGER NOT NULL DEFAULT 0,
                isCustomer INTEGER NOT NULL DEFAULT 0,
                currency TEXT NOT NULL DEFAULT 'USD',
                logoPath TEXT,
                parentUuid TEXT,
                website TEXT NOT NULL DEFAULT '',
                active INTEGER NOT NULL DEFAULT 1,
                notes TEXT NOT NULL DEFAULT '',
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS company_attachments (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL,
                attachmentPath TEXT NOT NULL DEFAULT '',
                link TEXT NOT NULL DEFAULT '',
                comment TEXT NOT NULL DEFAULT '',
                documentType TEXT NOT NULL DEFAULT 'سجل تجاري',
                expiryDate TEXT NOT NULL DEFAULT '',
                notifyOnExpiry INTEGER NOT NULL DEFAULT 1,
                notificationDaysBefore INTEGER NOT NULL DEFAULT 30,
                uploadDate INTEGER NOT NULL DEFAULT 0,
                userUuid TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS contacts (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                role TEXT NOT NULL DEFAULT '',
                isPrimary INTEGER NOT NULL DEFAULT 0,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS addresses (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL,
                title TEXT NOT NULL DEFAULT 'الفرع الرئيسي',
                isPrimary INTEGER NOT NULL DEFAULT 0,
                line1 TEXT NOT NULL,
                line2 TEXT NOT NULL DEFAULT '',
                postalCode TEXT NOT NULL DEFAULT '',
                city TEXT NOT NULL DEFAULT '',
                province TEXT NOT NULL DEFAULT '',
                country TEXT NOT NULL DEFAULT '',
                shippingNotes TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS company_bank_accounts (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL,
                bankName TEXT NOT NULL,
                accountName TEXT NOT NULL,
                accountNumber TEXT NOT NULL DEFAULT '',
                iban TEXT NOT NULL DEFAULT '',
                swiftBic TEXT NOT NULL DEFAULT '',
                currency TEXT NOT NULL DEFAULT 'USD',
                branchName TEXT NOT NULL DEFAULT '',
                isPrimary INTEGER NOT NULL DEFAULT 0,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS company_legal_records (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL UNIQUE,
                commercialRegisterNumber TEXT NOT NULL DEFAULT '',
                taxId TEXT NOT NULL DEFAULT '',
                nationalIdNumber TEXT NOT NULL DEFAULT '',
                importLicenseNumber TEXT NOT NULL DEFAULT '',
                manufacturingLicenseNumber TEXT NOT NULL DEFAULT '',
                activityCodes TEXT NOT NULL DEFAULT '',
                issuingAuthority TEXT NOT NULL DEFAULT '',
                issueDate TEXT NOT NULL DEFAULT '',
                expiryDate TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS manufacturer_parts (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                manufacturerUuid TEXT NOT NULL,
                mpn TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                link TEXT NOT NULL DEFAULT '',
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS manufacturer_part_parameters (
                uuid TEXT PRIMARY KEY NOT NULL,
                manufacturerPartUuid TEXT NOT NULL,
                name TEXT NOT NULL,
                value TEXT NOT NULL DEFAULT '',
                units TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS manufacturer_part_attachments (
                uuid TEXT PRIMARY KEY NOT NULL,
                manufacturerPartUuid TEXT NOT NULL,
                attachmentPath TEXT NOT NULL DEFAULT '',
                link TEXT NOT NULL DEFAULT '',
                comment TEXT NOT NULL DEFAULT '',
                uploadDate INTEGER NOT NULL DEFAULT 0,
                userUuid TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS supplier_parts (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                supplierUuid TEXT NOT NULL,
                sku TEXT NOT NULL,
                manufacturerPartUuid TEXT,
                description TEXT NOT NULL DEFAULT '',
                link TEXT NOT NULL DEFAULT '',
                note TEXT NOT NULL DEFAULT '',
                packaging TEXT NOT NULL DEFAULT '',
                packQuantity TEXT NOT NULL DEFAULT '1',
                availableForPurchase INTEGER NOT NULL DEFAULT 1,
                active INTEGER NOT NULL DEFAULT 1,
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS supplier_price_breaks (
                uuid TEXT PRIMARY KEY NOT NULL,
                supplierPartUuid TEXT NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                price REAL NOT NULL DEFAULT 0.0,
                priceCurrency TEXT NOT NULL DEFAULT 'USD',
                packQuantity TEXT NOT NULL DEFAULT '1',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 6. أوامر الشراء والتصنيع والمبيعات (Operations & Orders)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS purchase_orders (
                uuid TEXT PRIMARY KEY NOT NULL,
                reference TEXT NOT NULL,
                supplierUuid TEXT NOT NULL,
                supplierName TEXT NOT NULL DEFAULT '',
                statusCode INTEGER NOT NULL DEFAULT 10,
                description TEXT NOT NULL DEFAULT '',
                orderCurrency TEXT NOT NULL DEFAULT 'USD',
                targetDate TEXT NOT NULL DEFAULT '',
                totalCost REAL NOT NULL DEFAULT 0.0,
                sourceType TEXT NOT NULL DEFAULT 'MANUAL',
                sourceReferenceUuid TEXT,
                destinationLocationUuid TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS purchase_order_lines (
                uuid TEXT PRIMARY KEY NOT NULL,
                orderUuid TEXT NOT NULL,
                supplierPartUuid TEXT NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                receivedQuantity REAL NOT NULL DEFAULT 0.0,
                purchasePrice REAL NOT NULL DEFAULT 0.0,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS manufacturing_phases (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT,
                name TEXT NOT NULL,
                sequenceOrder INTEGER NOT NULL DEFAULT 1,
                description TEXT NOT NULL DEFAULT '',
                isSystemDefault INTEGER NOT NULL DEFAULT 0,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS build_orders (
                uuid TEXT PRIMARY KEY NOT NULL,
                reference TEXT NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                partUuid TEXT NOT NULL,
                partName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                completedQuantity REAL NOT NULL DEFAULT 0.0,
                statusCode INTEGER NOT NULL DEFAULT 10,
                batch TEXT NOT NULL DEFAULT '',
                targetDate TEXT NOT NULL DEFAULT '',
                startDate TEXT NOT NULL DEFAULT '',
                completionDate TEXT NOT NULL DEFAULT '',
                creationDate TEXT NOT NULL DEFAULT '',
                parentBuildUuid TEXT,
                salesOrderUuid TEXT,
                takeFromLocationUuid TEXT,
                destinationLocationUuid TEXT,
                issuedBy TEXT NOT NULL DEFAULT '',
                responsible TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                link TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS build_order_line_items (
                uuid TEXT PRIMARY KEY NOT NULL,
                buildUuid TEXT NOT NULL,
                bomItemUuid TEXT NOT NULL,
                subPartUuid TEXT NOT NULL,
                subPartName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                allocatedQuantity REAL NOT NULL DEFAULT 0.0,
                consumedQuantity REAL NOT NULL DEFAULT 0.0,
                unitCost REAL NOT NULL DEFAULT 0.0,
                phaseUuid TEXT,
                notes TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS build_items (
                uuid TEXT PRIMARY KEY NOT NULL,
                buildUuid TEXT NOT NULL,
                buildLineUuid TEXT,
                stockItemUuid TEXT NOT NULL,
                stockItemName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                installIntoStockItemUuid TEXT,
                notes TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS sales_orders (
                uuid TEXT PRIMARY KEY NOT NULL,
                reference TEXT NOT NULL,
                customerUuid TEXT NOT NULL,
                customerName TEXT NOT NULL DEFAULT '',
                statusCode INTEGER NOT NULL DEFAULT 10,
                description TEXT NOT NULL DEFAULT '',
                orderCurrency TEXT NOT NULL DEFAULT 'USD',
                targetDate TEXT NOT NULL DEFAULT '',
                totalPrice REAL NOT NULL DEFAULT 0.0,
                sourceType TEXT NOT NULL DEFAULT 'MANUAL',
                sourceReferenceUuid TEXT,
                notes TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS sales_order_lines (
                uuid TEXT PRIMARY KEY NOT NULL,
                orderUuid TEXT NOT NULL,
                partUuid TEXT NOT NULL,
                partName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                unitPrice REAL NOT NULL DEFAULT 0.0,
                allocatedQuantity REAL NOT NULL DEFAULT 0.0,
                shippedQuantity REAL NOT NULL DEFAULT 0.0,
                notes TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_allocations (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                allocatedQuantity REAL NOT NULL,
                allocationType TEXT NOT NULL,
                referenceType TEXT NOT NULL,
                referenceUuid TEXT NOT NULL,
                referenceTitle TEXT NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                createdByUserUuid TEXT NOT NULL,
                notes TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 7. الجداول الإضافية والخصائص والأسعار
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_parameter_templates (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL UNIQUE,
                units TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                choices TEXT NOT NULL DEFAULT '',
                checkbox INTEGER NOT NULL DEFAULT 0,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_category_parameter_templates (
                uuid TEXT PRIMARY KEY NOT NULL,
                categoryUuid TEXT NOT NULL,
                parameterTemplateUuid TEXT NOT NULL,
                defaultValue TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_parameters (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                templateUuid TEXT NOT NULL,
                data TEXT NOT NULL DEFAULT '',
                dataNumeric REAL,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_related (
                uuid TEXT PRIMARY KEY NOT NULL,
                part1Uuid TEXT NOT NULL,
                part2Uuid TEXT NOT NULL,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_test_templates (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                testName TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                required INTEGER NOT NULL DEFAULT 1,
                requiresValue INTEGER NOT NULL DEFAULT 0,
                requiresAttachment INTEGER NOT NULL DEFAULT 0,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_attachments (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                attachment TEXT,
                link TEXT,
                comment TEXT NOT NULL DEFAULT '',
                uploadDate TEXT NOT NULL DEFAULT '',
                userUuid TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_notes (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL UNIQUE,
                notes TEXT NOT NULL DEFAULT '',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                userUuid TEXT,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_internal_prices (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                price REAL NOT NULL DEFAULT 0.0,
                currency TEXT NOT NULL DEFAULT 'USD',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_sale_prices (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                price REAL NOT NULL DEFAULT 0.0,
                currency TEXT NOT NULL DEFAULT 'USD',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_stars (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL,
                userUuid TEXT NOT NULL DEFAULT 'usr-001',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_pricing (
                uuid TEXT PRIMARY KEY NOT NULL,
                partUuid TEXT NOT NULL UNIQUE,
                currency TEXT NOT NULL DEFAULT 'USD',
                overallMin REAL,
                overallMax REAL,
                purchaseCostMin REAL,
                purchaseCostMax REAL,
                bomCostMin REAL,
                bomCostMax REAL,
                variantCostMin REAL,
                variantCostMax REAL,
                internalCostMin REAL,
                internalCostMax REAL,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCHRONIZED',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // سجل الإشعارات (Append-only Log / الإشعارات مقروءة محلياً فقط)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS notifications_history (
                uuid TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                notificationType TEXT NOT NULL DEFAULT 'COMPANY_DOC_EXPIRY',
                targetEntityUuid TEXT NOT NULL,
                companyUuid TEXT,
                deepLink TEXT,
                scheduledDate INTEGER NOT NULL,
                isRead INTEGER NOT NULL DEFAULT 0,
                isTriggered INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 8. الفهارس (Optimized Production Indexes)
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_parts_categoryUuid ON parts(categoryUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_parts_defaultLocationUuid ON parts(defaultLocationUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_parts_sync ON parts(syncStatus, isDeleted, updatedAt);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_bom_items_partUuid ON bom_items(partUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_bom_items_subPartUuid ON bom_items(subPartUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_bom_items_sync ON bom_items(syncStatus, isDeleted, updatedAt);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_items_partUuid ON stock_items(partUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_items_locationUuid ON stock_items(locationUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_items_sync ON stock_items(syncStatus, isDeleted, updatedAt);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_locations_parentUuid ON stock_locations(parentUuid);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_tracking_itemUuid ON stock_item_tracking(stockItemUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_tracking_sync ON stock_item_tracking(syncStatus, createdAt);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_tests_itemUuid ON stock_item_test_results(stockItemUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_attachments_itemUuid ON stock_item_attachments(stockItemUuid);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_orders_partUuid ON build_orders(partUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_orders_sync ON build_orders(syncStatus, isDeleted, updatedAt);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_line_items_buildUuid ON build_order_line_items(buildUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_line_items_bomItemUuid ON build_order_line_items(bomItemUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_buildUuid ON build_items(buildUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_buildLineUuid ON build_items(buildLineUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_stockItemUuid ON build_items(stockItemUuid);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_companies_sync ON companies(syncStatus, isDeleted, updatedAt);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_company_att_compUuid ON company_attachments(companyUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_contacts_companyUuid ON contacts(companyUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_primary_contact ON contacts(companyUuid) WHERE isPrimary = 1;").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_addresses_companyUuid ON addresses(companyUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_primary_address ON addresses(companyUuid) WHERE isPrimary = 1;").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_bank_accounts_companyUuid ON company_bank_accounts(companyUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_primary_bank_account ON company_bank_accounts(companyUuid) WHERE isPrimary = 1;").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_legal_records_companyUuid ON company_legal_records(companyUuid);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_parts_partUuid ON manufacturer_parts(partUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_parts_mfgUuid ON manufacturer_parts(manufacturerUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_mfg_parts_mfg_mpn ON manufacturer_parts(manufacturerUuid, mpn);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_param_mfgPartUuid ON manufacturer_part_parameters(manufacturerPartUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_mfg_param_part_name ON manufacturer_part_parameters(manufacturerPartUuid, name);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_attachments_mfgPartUuid ON manufacturer_part_attachments(manufacturerPartUuid);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_supplier_parts_partUuid ON supplier_parts(partUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_supplier_parts_supUuid ON supplier_parts(supplierUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_supplier_parts_sup_sku ON supplier_parts(supplierUuid, sku);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_price_breaks_partUuid ON supplier_price_breaks(supplierPartUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_price_breaks_part_qty ON supplier_price_breaks(supplierPartUuid, quantity);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplierUuid ON purchase_orders(supplierUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_purchase_order_lines_orderUuid ON purchase_order_lines(orderUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_purchase_order_lines_supplierPartUuid ON purchase_order_lines(supplierPartUuid);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_sales_orders_customerUuid ON sales_orders(customerUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_sales_orders_statusCode ON sales_orders(statusCode);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_sales_order_lines_orderUuid ON sales_order_lines(orderUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_sales_order_lines_partUuid ON sales_order_lines(partUuid);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_part_allocations_part_status ON part_allocations(partUuid, status);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_part_allocations_ref ON part_allocations(referenceType, referenceUuid);").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_notifications_targetEntity ON notifications_history(targetEntityUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_notifications_companyUuid ON notifications_history(companyUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_notifications_triggered_schedule ON notifications_history(isTriggered, scheduledDate);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications_history(isRead);").use { it.step() } }

        // إدراج البيانات النموذجية والأولية (Fresh Seed Data)
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
                            uuid, name, ipn, description, categoryUuid, units, minimumStock, totalInStock,
                            assembly, component, isTemplate, trackable, purchaseable, salable,
                            version, syncStatus, isDeleted, updatedAt, lastModifiedByDeviceUuid
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'SYNCHRONIZED', 0, 1738000000000, 'dev-local');
                    """.trimIndent()).use { stmt ->
                        stmt.bindText(1, row[0] as String)
                        stmt.bindText(2, row[1] as String)
                        stmt.bindText(3, row[2] as String)
                        stmt.bindText(4, row[3] as String)
                        stmt.bindText(5, row[4] as String)
                        stmt.bindText(6, row[5] as String)
                        stmt.bindDouble(7, row[6] as Double)
                        stmt.bindDouble(8, row[7] as Double)
                        stmt.bindLong(9, (row[8] as Int).toLong())
                        stmt.bindLong(10, (row[9] as Int).toLong())
                        stmt.bindLong(11, (row[10] as Int).toLong())
                        stmt.bindLong(12, (row[11] as Int).toLong())
                        stmt.bindLong(13, (row[12] as Int).toLong())
                        stmt.bindLong(14, (row[13] as Int).toLong())
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

expect fun getDatabasePath(): String

private class JdbcSqliteConnection(private val conn: java.sql.Connection) : SQLiteConnection {
    override fun prepare(sql: String): SQLiteStatement {
        return JdbcSqliteStatement(conn.prepareStatement(sql))
    }

    override fun close() {
        conn.close()
    }
}

private class JdbcSqliteStatement(private val stmt: PreparedStatement) : SQLiteStatement {
    private var resultSet: ResultSet? = null
    private var isQuery: Boolean? = null

    override fun step(): Boolean {
        if (isQuery == null) {
            val hasResultSet = try {
                stmt.execute()
            } catch (e: SQLException) {
                false
            }
            if (hasResultSet) {
                resultSet = stmt.resultSet
                isQuery = true
            } else {
                isQuery = false
            }
        }

        val rs = resultSet
        return if (rs != null) {
            rs.next()
        } else {
            false
        }
    }

    override fun bindText(index: Int, value: String) {
        stmt.setString(index, value)
    }

    override fun bindLong(index: Int, value: Long) {
        stmt.setLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        stmt.setDouble(index, value)
    }

    override fun bindNull(index: Int) {
        stmt.setNull(index, Types.NULL)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        stmt.setBytes(index, value)
    }

    override fun getText(index: Int): String {
        return resultSet?.getString(index + 1) ?: ""
    }

    override fun getLong(index: Int): Long {
        return resultSet?.getLong(index + 1) ?: 0L
    }

    override fun getDouble(index: Int): Double {
        return resultSet?.getDouble(index + 1) ?: 0.0
    }

    override fun getBlob(index: Int): ByteArray {
        return resultSet?.getBytes(index + 1) ?: ByteArray(0)
    }

    override fun isNull(index: Int): Boolean {
        val rs = resultSet ?: return true
        val obj = rs.getObject(index + 1)
        return obj == null || rs.wasNull()
    }

    override fun getColumnCount(): Int {
        return resultSet?.metaData?.columnCount ?: 0
    }

    override fun getColumnName(index: Int): String {
        return resultSet?.metaData?.getColumnName(index + 1) ?: ""
    }

    override fun getColumnType(index: Int): Int {
        return resultSet?.metaData?.getColumnType(index + 1) ?: 0
    }

    override fun clearBindings() {
        stmt.clearParameters()
    }

    override fun reset() {
        resultSet?.close()
        resultSet = null
        isQuery = null
    }

    override fun close() {
        resultSet?.close()
        stmt.close()
    }
}

private class ThreadSafeSQLiteConnection(
    private val delegate: SQLiteConnection
) : SQLiteConnection by delegate {
    private val lock = Any()

    override fun prepare(sql: String): SQLiteStatement {
        synchronized(lock) {
            val stmt = delegate.prepare(sql)
            return ThreadSafeSQLiteStatement(stmt, lock)
        }
    }

    override fun close() {
        synchronized(lock) {
            delegate.close()
        }
    }
}

private class ThreadSafeSQLiteStatement(
    private val delegate: SQLiteStatement,
    private val lock: Any
) : SQLiteStatement by delegate {

    override fun step(): Boolean {
        synchronized(lock) {
            return delegate.step()
        }
    }

    override fun close() {
        synchronized(lock) {
            delegate.close()
        }
    }
}
