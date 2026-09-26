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

object SqliteDatabaseManager {
    private var connection: SQLiteConnection? = null

    fun getConnection(): SQLiteConnection {
        return connection ?: synchronized(this) {
            connection ?: openDatabase().also { connection = it }
        }
    }

    private fun openDatabase(): SQLiteConnection {
        val dbPath = getDatabasePath()
        val dbFile = File(dbPath)

        fun createFreshConnection(): SQLiteConnection {
            try {
                if (dbFile.exists()) dbFile.delete()
            } catch (_: Throwable) {}
            val driver = BundledSQLiteDriver()
            val conn = driver.open(dbPath)
            runCatching {
                conn.prepare("PRAGMA journal_mode = DELETE;").use { it.step() }
                conn.prepare("PRAGMA busy_timeout = 5000;").use { it.step() }
            }
            createTables(conn)
            return conn
        }

        val conn = try {
            val driver = BundledSQLiteDriver()
            driver.open(dbPath)
        } catch (e: Throwable) {
            return createFreshConnection()
        }

        return try {
            conn.prepare("SELECT count(*) FROM sqlite_master;").use { it.step() }
            conn.prepare("SELECT locationType FROM stock_locations LIMIT 1;").use { it.step() }
            runCatching {
                conn.prepare("PRAGMA journal_mode = DELETE;").use { it.step() }
                conn.prepare("PRAGMA busy_timeout = 5000;").use { it.step() }
            }
            createTables(conn)
            conn
        } catch (e: Throwable) {
            try { conn.close() } catch (_: Throwable) {}
            createFreshConnection()
        }
    }

    private fun createJdbcConnection(dbPath: String): SQLiteConnection {
        runCatching { Class.forName("org.sqlite.JDBC") }
        val jdbcConn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        return object : SQLiteConnection {
            override fun prepare(sql: String): SQLiteStatement {
                val stmt = jdbcConn.prepareStatement(sql)
                return JdbcSqliteStatement(stmt)
            }

            override fun close() {
                jdbcConn.close()
            }
        }
    }

    private fun createTables(conn: SQLiteConnection) {
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS parts (
                uuid TEXT PRIMARY KEY NOT NULL,
                id INTEGER NOT NULL DEFAULT 0,
                name TEXT NOT NULL,
                ipn TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                revision TEXT NOT NULL DEFAULT '',
                keywords TEXT NOT NULL DEFAULT '',
                categoryId INTEGER,
                units TEXT NOT NULL DEFAULT 'pcs',
                assembly INTEGER NOT NULL DEFAULT 0,
                component INTEGER NOT NULL DEFAULT 1,
                isTemplate INTEGER NOT NULL DEFAULT 0,
                variantOfId INTEGER,
                trackable INTEGER NOT NULL DEFAULT 0,
                purchaseable INTEGER NOT NULL DEFAULT 1,
                salable INTEGER NOT NULL DEFAULT 0,
                virtual INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                locked INTEGER NOT NULL DEFAULT 0,
                minimumStock REAL NOT NULL DEFAULT 0.0,
                maximumStock REAL,
                defaultLocationId INTEGER,
                defaultExpiryDays INTEGER,
                totalInStock REAL NOT NULL DEFAULT 0.0,
                localImagePath TEXT,
                link TEXT NOT NULL DEFAULT '',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        // ترقية الأعمدة المضافة تلقائياً في حالة وجود قاعدة بيانات قديمة على القرص
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN id INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN revision TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN keywords TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN assembly INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN component INTEGER NOT NULL DEFAULT 1").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN variantOfId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN trackable INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN purchaseable INTEGER NOT NULL DEFAULT 1").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN salable INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN virtual INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN active INTEGER NOT NULL DEFAULT 1").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN locked INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN maximumStock REAL").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN defaultLocationId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN defaultExpiryDays INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE parts ADD COLUMN link TEXT NOT NULL DEFAULT ''").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS bom_items (
                uuid TEXT PRIMARY KEY NOT NULL,
                partId INTEGER NOT NULL,
                subPartId INTEGER NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                reference TEXT NOT NULL DEFAULT '',
                optional INTEGER NOT NULL DEFAULT 0,
                consumable INTEGER NOT NULL DEFAULT 0,
                allowVariants INTEGER NOT NULL DEFAULT 0,
                inherited INTEGER NOT NULL DEFAULT 0,
                note TEXT NOT NULL DEFAULT '',
                checksum TEXT NOT NULL DEFAULT '',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE bom_items ADD COLUMN phaseUuid TEXT").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_items (
                uuid TEXT PRIMARY KEY NOT NULL,
                partId INTEGER NOT NULL,
                locationId INTEGER,
                locationUuid TEXT,
                quantity REAL NOT NULL DEFAULT 1.0,
                serial TEXT NOT NULL DEFAULT '',
                batch TEXT NOT NULL DEFAULT '',
                statusCode INTEGER NOT NULL DEFAULT 10,
                packaging TEXT NOT NULL DEFAULT 'Box',
                purchasePrice REAL NOT NULL DEFAULT 0.0,
                purchasePriceCurrency TEXT NOT NULL DEFAULT 'USD',
                purchaseOrderId INTEGER,
                supplierPartId INTEGER,
                supplierPartUuid TEXT NOT NULL DEFAULT '',
                salesOrderId INTEGER,
                customerId INTEGER,
                customerUuid TEXT NOT NULL DEFAULT '',
                buildId INTEGER,
                isBuilding INTEGER NOT NULL DEFAULT 0,
                parentId INTEGER,
                parentUuid TEXT,
                expiryDate TEXT NOT NULL DEFAULT '',
                stocktakeDate TEXT NOT NULL DEFAULT '',
                stocktakeUserId INTEGER,
                reviewNeeded INTEGER NOT NULL DEFAULT 0,
                deleteOnDeplete INTEGER NOT NULL DEFAULT 0,
                link TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                metadata TEXT NOT NULL DEFAULT '{}',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN locationUuid TEXT").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN partUuid TEXT").use { it.step() } }
        runCatching { conn.prepare("UPDATE stock_items SET partUuid = 'part-' || partId WHERE partUuid IS NULL OR partUuid = ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN purchasePrice REAL NOT NULL DEFAULT 0.0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN purchasePriceCurrency TEXT NOT NULL DEFAULT 'USD'").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN purchaseOrderId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN supplierPartId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN supplierPartUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN salesOrderId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN customerId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN customerUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN buildId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN isBuilding INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN parentId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN parentUuid TEXT").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN expiryDate TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN stocktakeDate TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN stocktakeUserId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN reviewNeeded INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN deleteOnDeplete INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN link TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN notes TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_items ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}'").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_locations (
                uuid TEXT PRIMARY KEY NOT NULL,
                locationId INTEGER NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                parentId INTEGER,
                parentUuid TEXT,
                structural INTEGER NOT NULL DEFAULT 0,
                external INTEGER NOT NULL DEFAULT 0,
                locationType TEXT NOT NULL DEFAULT 'SHELF',
                ownerId INTEGER,
                icon TEXT NOT NULL DEFAULT 'warehouse',
                customIcon TEXT NOT NULL DEFAULT '',
                address TEXT NOT NULL DEFAULT '',
                level INTEGER NOT NULL DEFAULT 0,
                lft INTEGER NOT NULL DEFAULT 0,
                rght INTEGER NOT NULL DEFAULT 0,
                treeId INTEGER NOT NULL DEFAULT 1,
                metadata TEXT NOT NULL DEFAULT '{}',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE stock_locations ADD COLUMN parentUuid TEXT").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_locations ADD COLUMN locationType TEXT NOT NULL DEFAULT 'SHELF'").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_locations ADD COLUMN customCapacity REAL").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_locations ADD COLUMN isBulkGenerated INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_locations ADD COLUMN address TEXT NOT NULL DEFAULT ''").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_location_types (
                uuid TEXT PRIMARY KEY NOT NULL,
                typeId INTEGER NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                icon TEXT NOT NULL DEFAULT 'warehouse',
                customIcon TEXT NOT NULL DEFAULT '',
                length REAL NOT NULL DEFAULT 0.0,
                width REAL NOT NULL DEFAULT 0.0,
                height REAL NOT NULL DEFAULT 0.0,
                maxWeight REAL NOT NULL DEFAULT 0.0,
                maxVolume REAL NOT NULL DEFAULT 0.0,
                metadata TEXT NOT NULL DEFAULT '{}',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE stock_location_types ADD COLUMN length REAL NOT NULL DEFAULT 0.0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_location_types ADD COLUMN width REAL NOT NULL DEFAULT 0.0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_location_types ADD COLUMN height REAL NOT NULL DEFAULT 0.0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_location_types ADD COLUMN maxWeight REAL NOT NULL DEFAULT 0.0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE stock_location_types ADD COLUMN maxVolume REAL NOT NULL DEFAULT 0.0").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_item_tracking (
                uuid TEXT PRIMARY KEY NOT NULL,
                trackingId INTEGER NOT NULL,
                stockItemId INTEGER NOT NULL,
                stockItemUuid TEXT NOT NULL DEFAULT '',
                date TEXT NOT NULL DEFAULT (DATETIME('now')),
                trackingTypeCode INTEGER NOT NULL DEFAULT 10,
                userId INTEGER,
                label TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                deltas TEXT NOT NULL DEFAULT '{}',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE stock_item_tracking ADD COLUMN stockItemUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_item_test_results (
                uuid TEXT PRIMARY KEY NOT NULL,
                resultId INTEGER NOT NULL,
                stockItemId INTEGER NOT NULL,
                stockItemUuid TEXT NOT NULL DEFAULT '',
                templateId INTEGER,
                test TEXT NOT NULL,
                result INTEGER NOT NULL DEFAULT 1,
                value TEXT NOT NULL DEFAULT 'Passed',
                attachment TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                date TEXT NOT NULL DEFAULT (DATE('now')),
                userId INTEGER,
                metadata TEXT NOT NULL DEFAULT '{}',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE stock_item_test_results ADD COLUMN stockItemUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_item_attachments (
                uuid TEXT PRIMARY KEY NOT NULL,
                attachmentId INTEGER NOT NULL,
                stockItemId INTEGER NOT NULL,
                stockItemUuid TEXT NOT NULL DEFAULT '',
                attachment TEXT,
                link TEXT,
                comment TEXT NOT NULL DEFAULT '',
                uploadDate TEXT NOT NULL DEFAULT (DATE('now')),
                userId INTEGER,
                metadata TEXT NOT NULL DEFAULT '{}',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE stock_item_attachments ADD COLUMN stockItemUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }

        // إنشاء فهارس الأداء لحقول المزامنة والربط المحلي (Performance Indexes)
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_bom_items_sync ON bom_items(syncStatus, isDeleted, updatedAt)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_phases_sync ON manufacturing_phases(syncStatus, isDeleted, updatedAt)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_items_partId ON stock_items(partId)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_items_partUuid ON stock_items(partUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_items_locationUuid ON stock_items(locationUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_locations_parentUuid ON stock_locations(parentUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_tracking_itemUuid ON stock_item_tracking(stockItemUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_tests_itemUuid ON stock_item_test_results(stockItemUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_attachments_itemUuid ON stock_item_attachments(stockItemUuid)").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS companies (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                website TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                isSupplier INTEGER NOT NULL DEFAULT 1,
                isManufacturer INTEGER NOT NULL DEFAULT 0,
                isCustomer INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                currency TEXT NOT NULL DEFAULT 'USD',
                logoPath TEXT,
                notes TEXT NOT NULL DEFAULT '',
                metadata TEXT NOT NULL DEFAULT '{}',
                parentUuid TEXT,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE companies ADD COLUMN website TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE companies ADD COLUMN active INTEGER NOT NULL DEFAULT 1").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE companies ADD COLUMN notes TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE companies ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}'").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE companies ADD COLUMN parentUuid TEXT").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS company_attachments (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL,
                documentType TEXT NOT NULL DEFAULT 'سجل تجاري',
                attachmentPath TEXT NOT NULL DEFAULT '',
                link TEXT NOT NULL DEFAULT '',
                comment TEXT NOT NULL DEFAULT '',
                uploadDate INTEGER NOT NULL DEFAULT 0,
                userId INTEGER,
                expiryDate TEXT NOT NULL DEFAULT '',
                notifyOnExpiry INTEGER NOT NULL DEFAULT 1,
                notificationDaysBefore INTEGER NOT NULL DEFAULT 30,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE company_attachments ADD COLUMN documentType TEXT NOT NULL DEFAULT 'سجل تجاري'").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE company_attachments ADD COLUMN expiryDate TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE company_attachments ADD COLUMN notifyOnExpiry INTEGER NOT NULL DEFAULT 1").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE company_attachments ADD COLUMN notificationDaysBefore INTEGER NOT NULL DEFAULT 30").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS contacts (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                role TEXT NOT NULL DEFAULT '',
                isPrimary INTEGER NOT NULL DEFAULT 0,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
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
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
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
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE manufacturer_parts ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}'").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS manufacturer_part_parameters (
                uuid TEXT PRIMARY KEY NOT NULL,
                manufacturerPartUuid TEXT NOT NULL,
                name TEXT NOT NULL,
                value TEXT NOT NULL DEFAULT '',
                units TEXT NOT NULL DEFAULT '',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
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
                userId INTEGER,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
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
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE supplier_parts ADD COLUMN availableForPurchase INTEGER NOT NULL DEFAULT 1").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE supplier_parts ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}'").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS supplier_price_breaks (
                uuid TEXT PRIMARY KEY NOT NULL,
                supplierPartUuid TEXT NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                price REAL NOT NULL DEFAULT 0.0,
                priceCurrency TEXT NOT NULL DEFAULT 'USD',
                packQuantity TEXT NOT NULL DEFAULT '1',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE supplier_price_breaks ADD COLUMN packQuantity TEXT NOT NULL DEFAULT '1'").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE contacts ADD COLUMN isPrimary INTEGER NOT NULL DEFAULT 0").use { it.step() } }

        // قيود التفرد المركبة والجزئية على مستوى DDL (Composite & Partial Unique Indexes)
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_primary_address ON addresses(companyUuid) WHERE isPrimary = 1").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_primary_contact ON contacts(companyUuid) WHERE isPrimary = 1").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_mfg_parts_mfg_mpn ON manufacturer_parts(manufacturerUuid, mpn)").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_supplier_parts_sup_sku ON supplier_parts(supplierUuid, sku)").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_price_breaks_part_qty ON supplier_price_breaks(supplierPartUuid, quantity)").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_mfg_param_part_name ON manufacturer_part_parameters(manufacturerPartUuid, name)").use { it.step() } }

        // فهارس استعلامات الأداء للقطع الداخلية
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_supplier_parts_partUuid ON supplier_parts(partUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_parts_partUuid ON manufacturer_parts(partUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_company_att_compUuid ON company_attachments(companyUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_param_mfgPartUuid ON manufacturer_part_parameters(manufacturerPartUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_contacts_companyUuid ON contacts(companyUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_addresses_companyUuid ON addresses(companyUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_parts_mfgUuid ON manufacturer_parts(manufacturerUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_mfg_attachments_mfgPartUuid ON manufacturer_part_attachments(manufacturerPartUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_supplier_parts_supUuid ON supplier_parts(supplierUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_price_breaks_partUuid ON supplier_price_breaks(supplierPartUuid)").use { it.step() } }

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
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_bank_accounts_companyUuid ON company_bank_accounts(companyUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_primary_bank_account ON company_bank_accounts(companyUuid) WHERE isPrimary = 1").use { it.step() } }

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
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_legal_records_companyUuid ON company_legal_records(companyUuid)").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS purchase_orders (
                uuid TEXT PRIMARY KEY NOT NULL,
                reference TEXT NOT NULL,
                supplierId INTEGER NOT NULL,
                supplierUuid TEXT NOT NULL DEFAULT '',
                supplierName TEXT NOT NULL DEFAULT '',
                statusCode INTEGER NOT NULL DEFAULT 10,
                description TEXT NOT NULL DEFAULT '',
                orderCurrency TEXT NOT NULL DEFAULT 'USD',
                targetDate TEXT NOT NULL DEFAULT '',
                totalCost REAL NOT NULL DEFAULT 0.0,
                sourceType TEXT NOT NULL DEFAULT 'MANUAL',
                sourceReferenceUuid TEXT,
                destinationLocationUuid TEXT,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE purchase_orders ADD COLUMN supplierUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE purchase_orders ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'MANUAL'").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE purchase_orders ADD COLUMN sourceReferenceUuid TEXT").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE purchase_orders ADD COLUMN destinationLocationUuid TEXT").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS purchase_order_lines (
                uuid TEXT PRIMARY KEY NOT NULL,
                orderUuid TEXT NOT NULL,
                supplierPartId INTEGER NOT NULL,
                supplierPartUuid TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                receivedQuantity REAL NOT NULL DEFAULT 0.0,
                purchasePrice REAL NOT NULL DEFAULT 0.0,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE purchase_order_lines ADD COLUMN supplierPartUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS sales_orders (
                uuid TEXT PRIMARY KEY NOT NULL,
                reference TEXT NOT NULL,
                customerId INTEGER NOT NULL DEFAULT 0,
                customerUuid TEXT NOT NULL DEFAULT '',
                customerName TEXT NOT NULL DEFAULT '',
                statusCode INTEGER NOT NULL DEFAULT 10,
                description TEXT NOT NULL DEFAULT '',
                orderCurrency TEXT NOT NULL DEFAULT 'USD',
                targetDate TEXT NOT NULL DEFAULT '',
                totalPrice REAL NOT NULL DEFAULT 0.0,
                sourceType TEXT NOT NULL DEFAULT 'MANUAL',
                sourceReferenceUuid TEXT,
                notes TEXT NOT NULL DEFAULT '',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE sales_orders ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'MANUAL'").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE sales_orders ADD COLUMN sourceReferenceUuid TEXT").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_sales_orders_customerId ON sales_orders(customerId)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_sales_orders_statusCode ON sales_orders(statusCode)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_sales_order_lines_orderUuid ON sales_order_lines(orderUuid)").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS sales_order_lines (
                uuid TEXT PRIMARY KEY NOT NULL,
                orderUuid TEXT NOT NULL,
                orderId INTEGER NOT NULL DEFAULT 0,
                partId INTEGER NOT NULL DEFAULT 0,
                partUuid TEXT NOT NULL DEFAULT '',
                partName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                unitPrice REAL NOT NULL DEFAULT 0.0,
                allocatedQuantity REAL NOT NULL DEFAULT 0.0,
                shippedQuantity REAL NOT NULL DEFAULT 0.0,
                notes TEXT NOT NULL DEFAULT '',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS build_orders (
                uuid TEXT PRIMARY KEY NOT NULL,
                reference TEXT NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                partId INTEGER NOT NULL,
                partName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                completedQuantity REAL NOT NULL DEFAULT 0.0,
                statusCode INTEGER NOT NULL DEFAULT 10,
                batch TEXT NOT NULL DEFAULT '',
                targetDate TEXT NOT NULL DEFAULT '',
                startDate TEXT NOT NULL DEFAULT '',
                completionDate TEXT NOT NULL DEFAULT '',
                creationDate TEXT NOT NULL DEFAULT '',
                parentId INTEGER,
                salesOrderId INTEGER,
                takeFromLocationId INTEGER,
                destinationLocationId INTEGER,
                issuedBy TEXT NOT NULL DEFAULT '',
                responsible TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                link TEXT NOT NULL DEFAULT '',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN startDate TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN completionDate TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN creationDate TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN parentId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN salesOrderId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN takeFromLocationId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN destinationLocationId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN issuedBy TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN responsible TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN notes TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_orders ADD COLUMN link TEXT NOT NULL DEFAULT ''").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS build_items (
                uuid TEXT PRIMARY KEY NOT NULL,
                id INTEGER NOT NULL DEFAULT 0,
                buildId INTEGER NOT NULL,
                buildUuid TEXT NOT NULL DEFAULT '',
                buildLineId INTEGER,
                buildLineUuid TEXT NOT NULL DEFAULT '',
                stockItemId INTEGER NOT NULL,
                stockItemUuid TEXT NOT NULL DEFAULT '',
                stockItemName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                installIntoStockItemId INTEGER,
                installIntoStockItemUuid TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE build_items ADD COLUMN buildLineId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_items ADD COLUMN installIntoStockItemId INTEGER").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_items ADD COLUMN buildUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_items ADD COLUMN buildLineUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_items ADD COLUMN stockItemUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_items ADD COLUMN installIntoStockItemUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_buildId ON build_items(buildId)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_buildUuid ON build_items(buildUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_buildLineId ON build_items(buildLineId)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_buildLineUuid ON build_items(buildLineUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_stockItemUuid ON build_items(stockItemUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_items_installIntoStockItemUuid ON build_items(installIntoStockItemUuid)").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS build_order_line_items (
                uuid TEXT PRIMARY KEY NOT NULL,
                id INTEGER NOT NULL DEFAULT 0,
                buildId INTEGER NOT NULL,
                buildUuid TEXT NOT NULL DEFAULT '',
                bomItemId INTEGER NOT NULL,
                bomItemUuid TEXT NOT NULL DEFAULT '',
                subPartId INTEGER NOT NULL DEFAULT 0,
                subPartName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                allocatedQuantity REAL NOT NULL DEFAULT 0.0,
                consumedQuantity REAL NOT NULL DEFAULT 0.0,
                notes TEXT NOT NULL DEFAULT '',
                phaseUuid TEXT,
                unitCost REAL NOT NULL DEFAULT 0.0,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE build_order_line_items ADD COLUMN buildUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_order_line_items ADD COLUMN bomItemUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_order_line_items ADD COLUMN phaseUuid TEXT").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_order_line_items ADD COLUMN unitCost REAL NOT NULL DEFAULT 0.0").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS manufacturing_phases (
                uuid TEXT PRIMARY KEY NOT NULL,
                id INTEGER NOT NULL DEFAULT 0,
                partUuid TEXT,
                name TEXT NOT NULL,
                sequenceOrder INTEGER NOT NULL DEFAULT 1,
                description TEXT NOT NULL DEFAULT '',
                isSystemDefault INTEGER NOT NULL DEFAULT 0,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_line_items_buildId ON build_order_line_items(buildId)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_line_items_buildUuid ON build_order_line_items(buildUuid)").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_build_line_items_bomItemUuid ON build_order_line_items(bomItemUuid)").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                parentId INTEGER,
                description TEXT NOT NULL DEFAULT '',
                structural INTEGER NOT NULL DEFAULT 0,
                defaultLocationId INTEGER
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE part_categories ADD COLUMN description TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_categories ADD COLUMN structural INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_categories ADD COLUMN defaultLocationId INTEGER").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS bom_item_substitutes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bomItemId INTEGER NOT NULL,
                partId INTEGER NOT NULL
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_parameter_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL UNIQUE,
                units TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                choices TEXT NOT NULL DEFAULT '',
                checkbox INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE part_parameter_templates ADD COLUMN units TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_parameter_templates ADD COLUMN description TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_parameter_templates ADD COLUMN choices TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_parameter_templates ADD COLUMN checkbox INTEGER NOT NULL DEFAULT 0").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_category_parameter_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                parameterTemplateId INTEGER NOT NULL,
                defaultValue TEXT
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE part_category_parameter_templates ADD COLUMN defaultValue TEXT").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_parameters (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                templateId INTEGER NOT NULL,
                data TEXT NOT NULL DEFAULT '',
                dataNumeric REAL
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE part_parameters ADD COLUMN dataNumeric REAL").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_related (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                part1Id INTEGER NOT NULL,
                part2Id INTEGER NOT NULL
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_test_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                testName TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                required INTEGER NOT NULL DEFAULT 1,
                requiresValue INTEGER NOT NULL DEFAULT 0,
                requiresAttachment INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE part_test_templates ADD COLUMN description TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_test_templates ADD COLUMN required INTEGER NOT NULL DEFAULT 1").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_test_templates ADD COLUMN requiresValue INTEGER NOT NULL DEFAULT 0").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE part_test_templates ADD COLUMN requiresAttachment INTEGER NOT NULL DEFAULT 0").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_attachments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                attachment TEXT,
                link TEXT,
                comment TEXT NOT NULL DEFAULT '',
                uploadDate TEXT NOT NULL DEFAULT '',
                userId INTEGER
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL UNIQUE,
                notes TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT '',
                userId INTEGER
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_internal_prices (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                price REAL NOT NULL DEFAULT 0.0,
                currency TEXT NOT NULL DEFAULT 'USD'
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_stars (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                userId INTEGER NOT NULL DEFAULT 1
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_sale_prices (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                quantity REAL NOT NULL DEFAULT 1.0,
                price REAL NOT NULL DEFAULT 0.0,
                currency TEXT NOT NULL DEFAULT 'USD'
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_pricing (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL UNIQUE,
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
                updatedAt TEXT NOT NULL DEFAULT ''
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
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
                id, notificationTime, soundEnabled, vibrationEnabled, docExpiryWarningDays,
                lowStockAlertsEnabled, themeMode, language, defaultCurrency, scannerBeepEnabled,
                biometricLockEnabled, syncWifiOnly
            ) VALUES (1, '09:00', 1, 1, 30, 1, 'SYSTEM', 'ar', 'USD', 1, 0, 0);
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS inflow_preferences (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                pinnedInflowIds TEXT NOT NULL DEFAULT 'PURCHASE_ORDER,INTERNAL_BUILD',
                customInflowText TEXT NOT NULL DEFAULT ''
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            INSERT OR IGNORE INTO inflow_preferences (id, pinnedInflowIds, customInflowText)
            VALUES (1, 'PURCHASE_ORDER,INTERNAL_BUILD', '');
        """.trimIndent()).use { it.step() }

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
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_notifications_targetEntity ON notifications_history(targetEntityUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_notifications_companyUuid ON notifications_history(companyUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_notifications_triggered_schedule ON notifications_history(isTriggered, scheduledDate);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications_history(isRead, isDeleted);").use { it.step() } }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS app_users (
                uuid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT '',
                active INTEGER NOT NULL DEFAULT 1,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

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
                        INSERT OR IGNORE INTO app_users (uuid, name, role, active, isDeleted, updatedAt)
                        VALUES (?, ?, ?, 1, 0, 1700000000000);
                    """.trimIndent()).use { stmt ->
                        stmt.bindText(1, uuid)
                        stmt.bindText(2, name)
                        stmt.bindText(3, role)
                        stmt.step()
                    }
                }
            }
        }

        runCatching {
            val typeCount = conn.prepare("SELECT COUNT(*) FROM stock_location_types").use { stmt ->
                if (stmt.step()) stmt.getLong(0) else 0L
            }
            if (typeCount == 0L) {
                val seedTypes = listOf(
                    listOf("type-000", 0L, "SITE", "منشأة تخزينية أو مجمع لوجستي جغرافي مستودعي", "place", "", 50.0, 50.0, 10.0, 0.0, 0.0),
                    listOf("type-001", 1L, "SHELF", "رف تخزين قياسي لقطع ومكونات الإنتاج", "shelves", "", 1.2, 0.5, 2.0, 150.0, 1.2),
                    listOf("type-002", 2L, "PALLET_RACK", "رف طبالي صناعي ثقيل في المستودع الرئيسي", "warehouse", "", 2.7, 1.1, 4.5, 2500.0, 13.3),
                    listOf("type-003", 3L, "BIN", "صندوق/درج حفظ معزول للمكونات والدائريات الصغيرة", "inventory_2", "", 0.3, 0.2, 0.15, 25.0, 0.009),
                    listOf("type-004", 4L, "AISLE", "ممر مرور وتنظيم أرفف التخزين", "door", "", 10.0, 2.5, 5.0, 0.0, 0.0),
                    listOf("type-005", 5L, "ZONE", "قسم أو منطقة تخزينية معتمدة", "grid_view", "", 15.0, 10.0, 6.0, 0.0, 0.0)
                )
                seedTypes.forEach { row ->
                    conn.prepare("""
                        INSERT OR IGNORE INTO stock_location_types (
                            uuid, typeId, name, description, icon, customIcon,
                            length, width, height, maxWeight, maxVolume,
                            metadata, syncStatus, isDeleted, updatedAt
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}', 'SYNCED', 0, 1700000000000);
                    """.trimIndent()).use { stmt ->
                        stmt.bindText(1, row[0] as String)
                        stmt.bindLong(2, row[1] as Long)
                        stmt.bindText(3, row[2] as String)
                        stmt.bindText(4, row[3] as String)
                        stmt.bindText(5, row[4] as String)
                        stmt.bindText(6, row[5] as String)
                        stmt.bindDouble(7, row[6] as Double)
                        stmt.bindDouble(8, row[7] as Double)
                        stmt.bindDouble(9, row[8] as Double)
                        stmt.bindDouble(10, row[9] as Double)
                        stmt.bindDouble(11, row[10] as Double)
                        stmt.step()
                    }
                }
            }
        }
    }
}

expect fun getDatabasePath(): String

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
        stmt.setNull(index, java.sql.Types.NULL)
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
