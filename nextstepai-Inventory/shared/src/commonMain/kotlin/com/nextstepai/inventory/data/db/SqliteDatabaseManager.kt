package com.nextstepai.inventory.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

object SqliteDatabaseManager {
    private var connection: SQLiteConnection? = null

    fun getConnection(): SQLiteConnection {
        return connection ?: synchronized(this) {
            connection ?: openDatabase().also { connection = it }
        }
    }

    private fun openDatabase(): SQLiteConnection {
        val dbPath = getDatabasePath()
        val driver = BundledSQLiteDriver()
        val conn = driver.open(dbPath)
        createTables(conn)
        return conn
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
                locationTypeId INTEGER,
                ownerId INTEGER,
                icon TEXT NOT NULL DEFAULT 'warehouse',
                customIcon TEXT NOT NULL DEFAULT '',
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

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_location_types (
                uuid TEXT PRIMARY KEY NOT NULL,
                typeId INTEGER NOT NULL,
                name TEXT NOT NULL UNIQUE,
                description TEXT NOT NULL DEFAULT '',
                icon TEXT NOT NULL DEFAULT 'warehouse',
                customIcon TEXT NOT NULL DEFAULT '',
                metadata TEXT NOT NULL DEFAULT '{}',
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

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
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_stock_items_partId ON stock_items(partId)").use { it.step() } }
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
            CREATE TABLE IF NOT EXISTS contacts (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                role TEXT NOT NULL DEFAULT '',
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

        // قيود التفرد المركبة والجزئية على مستوى DDL (Composite & Partial Unique Indexes)
        runCatching { conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_primary_address ON addresses(companyUuid) WHERE isPrimary = 1").use { it.step() } }
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
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE purchase_orders ADD COLUMN supplierUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }

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
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()).use { it.step() }

        runCatching { conn.prepare("ALTER TABLE build_order_line_items ADD COLUMN buildUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }
        runCatching { conn.prepare("ALTER TABLE build_order_line_items ADD COLUMN bomItemUuid TEXT NOT NULL DEFAULT ''").use { it.step() } }

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

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_category_parameter_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                parameterTemplateId INTEGER NOT NULL,
                defaultValue TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS part_parameters (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partId INTEGER NOT NULL,
                templateId INTEGER NOT NULL,
                data TEXT NOT NULL DEFAULT '',
                dataNumeric REAL
            );
        """.trimIndent()).use { it.step() }

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
    }
}

expect fun getDatabasePath(): String
