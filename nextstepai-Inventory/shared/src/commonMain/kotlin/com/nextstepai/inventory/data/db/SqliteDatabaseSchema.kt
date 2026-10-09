package com.nextstepai.inventory.data.db

import androidx.sqlite.SQLiteConnection

/**
 * مخطط الجداول الكاملة لقاعدة بيانات SQLite (Database Schema DDL).
 * يتضمن إنشاء كافة الجداول الـ 28 مع الفهارس الأساسية.
 */
internal object SqliteDatabaseSchema {
    fun createTables(conn: SQLiteConnection) {
        // 1. Ø¬Ø¯Ø§ÙˆÙ„ Ø§Ù„Ù†Ø¸Ø§Ù… Ø§Ù„Ù…Ø­Ù„ÙŠØ© (Local-Only - Ù„Ø§ ØªØªØ²Ø§Ù…Ù†)
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
            CREATE TABLE IF NOT EXISTS inflow_preferences (
                uuid TEXT PRIMARY KEY NOT NULL DEFAULT 'default-inflow',
                pinnedInflowIds TEXT NOT NULL DEFAULT 'PURCHASE_ORDER,INTERNAL_BUILD',
                customInflowText TEXT NOT NULL DEFAULT ''
            );
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

        // 2. Ø§Ù„ØªØµÙ†ÙŠÙØ§Øª ÙˆØ§Ù„Ù…ÙˆØ§Ø¯ (Parts & Categories)
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
                id INTEGER NOT NULL DEFAULT 0,
                categoryId INTEGER,
                defaultLocationId INTEGER,
                variantOfId INTEGER,
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

        // Upgrade existing databases where the compatibility ID column is absent.
        val partColumns = mutableSetOf<String>()
        conn.prepare("PRAGMA table_info(parts)").use { stmt ->
            while (stmt.step()) partColumns.add(stmt.getText(1))
        }
        if ("id" !in partColumns) {
            conn.prepare("ALTER TABLE parts ADD COLUMN id INTEGER NOT NULL DEFAULT 0").use { it.step() }
        }

        // Backfill legacy/domain IDs for existing rows before repositories map them.
        conn.prepare("UPDATE parts SET id = CAST(SUBSTR(uuid, 6) AS INTEGER) WHERE id = 0 AND uuid LIKE 'part-%' AND SUBSTR(uuid, 6) GLOB '[0-9]*'").use { it.step() }
        backfillMissingNumericIds(conn, "parts", "part-")


        // 3. Ù‚Ø§Ø¦Ù…Ø© Ø§Ù„Ù…ÙˆØ§Ø¯ ÙˆØ§Ù„ØªØµÙ†ÙŠØ¹ (BOM)
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

        // 4. Ø§Ù„Ù…ÙˆØ§Ù‚Ø¹ ÙˆØ§Ù„Ù…Ø®Ø²ÙˆÙ† (Locations & Stock)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_location_types (
                uuid TEXT PRIMARY KEY NOT NULL,
                typeId INTEGER NOT NULL DEFAULT 0,
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
                id INTEGER NOT NULL DEFAULT 0,
                locationId INTEGER NOT NULL DEFAULT 0,
                parentId INTEGER,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                parentUuid TEXT,
                structural INTEGER NOT NULL DEFAULT 0,
                external INTEGER NOT NULL DEFAULT 0,
                locationTypeUuid TEXT,
                locationType TEXT NOT NULL DEFAULT 'SHELF',
                ownerId INTEGER,
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
                id INTEGER NOT NULL DEFAULT 0,
                partId INTEGER NOT NULL DEFAULT 0,
                locationId INTEGER,
                purchaseOrderId INTEGER,
                supplierPartId INTEGER,
                salesOrderId INTEGER,
                customerId INTEGER,
                buildId INTEGER,
                parentId INTEGER,
                parentUuid TEXT,
                stocktakeUserId INTEGER,
                partUuid TEXT NOT NULL DEFAULT '',
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

        // Ø¬Ø¯ÙˆÙ„ Ø³Ø¬Ù„Ø§Øª Ø§Ù„Ø­Ø±ÙƒØ§Øª (Append-only Ledger)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_item_tracking (
                uuid TEXT PRIMARY KEY NOT NULL,
                trackingId INTEGER NOT NULL DEFAULT 0,
                stockItemId INTEGER NOT NULL DEFAULT 0,
                stockItemUuid TEXT NOT NULL,
                date TEXT NOT NULL DEFAULT '',
                trackingTypeCode INTEGER NOT NULL DEFAULT 10,
                userId INTEGER,
                label TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                deltas TEXT NOT NULL DEFAULT '{}',
                userUuid TEXT,
                createdAt INTEGER NOT NULL DEFAULT 0,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        conn.prepare("""
            CREATE TABLE IF NOT EXISTS stock_item_test_results (
                uuid TEXT PRIMARY KEY NOT NULL,
                resultId INTEGER NOT NULL DEFAULT 0,
                stockItemId INTEGER NOT NULL DEFAULT 0,
                stockItemUuid TEXT NOT NULL DEFAULT '',
                templateId INTEGER,
                templateUuid TEXT,
                test TEXT NOT NULL DEFAULT '',
                result INTEGER NOT NULL DEFAULT 1,
                value TEXT NOT NULL DEFAULT 'Passed',
                attachment TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                date TEXT NOT NULL DEFAULT '2025-02-15',
                userId INTEGER,
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
                attachmentId INTEGER NOT NULL DEFAULT 0,
                stockItemId INTEGER NOT NULL DEFAULT 0,
                stockItemUuid TEXT NOT NULL DEFAULT '',
                attachment TEXT,
                link TEXT,
                comment TEXT NOT NULL DEFAULT '',
                uploadDate TEXT NOT NULL DEFAULT '2025-02-15',
                userId INTEGER,
                userUuid TEXT,
                metadata TEXT NOT NULL DEFAULT '{}',
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 5. Ø§Ù„Ø´Ø±ÙƒØ§Øª ÙˆØ§Ù„Ø¹Ù†Ø§ÙˆÙŠÙ† ÙˆØ§Ù„Ù…ÙˆØ±Ø¯ÙŠÙ† (Companies & Partners)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS companies (
                uuid TEXT PRIMARY KEY NOT NULL,
                id INTEGER NOT NULL DEFAULT 0,
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

        // Upgrade databases created before companies carried a stable numeric ID.
        val companyColumns = mutableSetOf<String>()
        conn.prepare("PRAGMA table_info(companies)").use { stmt ->
            while (stmt.step()) companyColumns.add(stmt.getText(1))
        }
        if ("id" !in companyColumns) {
            conn.prepare("ALTER TABLE companies ADD COLUMN id INTEGER NOT NULL DEFAULT 0").use { it.step() }
        }
        conn.prepare("UPDATE companies SET id = CAST(SUBSTR(uuid, 9) AS INTEGER) WHERE id = 0 AND uuid LIKE 'company-%' AND SUBSTR(uuid, 9) GLOB '[0-9]*'").use { it.step() }
        backfillMissingNumericIds(conn, "companies", "company-")


        conn.prepare("""
            CREATE TABLE IF NOT EXISTS company_attachments (
                uuid TEXT PRIMARY KEY NOT NULL,
                companyUuid TEXT NOT NULL,
                documentType TEXT NOT NULL DEFAULT 'Ø³Ø¬Ù„ ØªØ¬Ø§Ø±ÙŠ',
                attachmentPath TEXT NOT NULL DEFAULT '',
                link TEXT NOT NULL DEFAULT '',
                comment TEXT NOT NULL DEFAULT '',
                uploadDate INTEGER NOT NULL DEFAULT 0,
                userId INTEGER,
                expiryDate TEXT NOT NULL DEFAULT '',
                notifyOnExpiry INTEGER NOT NULL DEFAULT 1,
                notificationDaysBefore INTEGER NOT NULL DEFAULT 30,
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
                title TEXT NOT NULL DEFAULT 'Ø§Ù„ÙØ±Ø¹ Ø§Ù„Ø±Ø¦ÙŠØ³ÙŠ',
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
                userId INTEGER,
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

        // 6. Ø£ÙˆØ§Ù…Ø± Ø§Ù„Ø´Ø±Ø§Ø¡ ÙˆØ§Ù„ØªØµÙ†ÙŠØ¹ ÙˆØ§Ù„Ù…Ø¨ÙŠØ¹Ø§Øª (Operations & Orders)
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS purchase_orders (
                uuid TEXT PRIMARY KEY NOT NULL,
                reference TEXT NOT NULL,
                supplierId INTEGER NOT NULL DEFAULT 0,
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
                supplierPartId INTEGER NOT NULL DEFAULT 0,
                supplierPartUuid TEXT NOT NULL DEFAULT '',
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
                id INTEGER NOT NULL DEFAULT 0,
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
                partId INTEGER NOT NULL DEFAULT 0,
                partUuid TEXT NOT NULL DEFAULT '',
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
                parentBuildUuid TEXT,
                salesOrderId INTEGER,
                salesOrderUuid TEXT,
                takeFromLocationId INTEGER,
                takeFromLocationUuid TEXT,
                destinationLocationId INTEGER,
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
                id INTEGER NOT NULL DEFAULT 0,
                buildId INTEGER NOT NULL DEFAULT 0,
                buildUuid TEXT NOT NULL DEFAULT '',
                bomItemId INTEGER NOT NULL DEFAULT 0,
                bomItemUuid TEXT NOT NULL DEFAULT '',
                subPartId INTEGER NOT NULL DEFAULT 0,
                subPartUuid TEXT NOT NULL DEFAULT '',
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
                id INTEGER NOT NULL DEFAULT 0,
                buildId INTEGER NOT NULL DEFAULT 0,
                buildUuid TEXT NOT NULL DEFAULT '',
                buildLineId INTEGER,
                buildLineUuid TEXT,
                stockItemId INTEGER NOT NULL DEFAULT 0,
                stockItemUuid TEXT NOT NULL DEFAULT '',
                stockItemName TEXT NOT NULL DEFAULT '',
                quantity REAL NOT NULL DEFAULT 1.0,
                installIntoStockItemId INTEGER,
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
                orderUuid TEXT NOT NULL DEFAULT '',
                orderId INTEGER NOT NULL DEFAULT 0,
                partId INTEGER NOT NULL DEFAULT 0,
                partUuid TEXT NOT NULL DEFAULT '',
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
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL DEFAULT '',
                partId INTEGER NOT NULL DEFAULT 0,
                partUuid TEXT NOT NULL DEFAULT '',
                allocatedQuantity REAL NOT NULL DEFAULT 0.0,
                allocationType TEXT NOT NULL DEFAULT 'HARD',
                referenceType TEXT NOT NULL DEFAULT 'BUILD_ORDER',
                referenceId TEXT NOT NULL DEFAULT '',
                referenceUuid TEXT NOT NULL DEFAULT '',
                referenceTitle TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                createdAt INTEGER NOT NULL DEFAULT 0,
                createdByUserId TEXT NOT NULL DEFAULT '1',
                createdByUserUuid TEXT NOT NULL DEFAULT '',
                notes TEXT,
                version INTEGER NOT NULL DEFAULT 1,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                isDeleted INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                lastModifiedByDeviceUuid TEXT
            );
        """.trimIndent()).use { it.step() }

        // 7. Ø§Ù„Ø¬Ø¯Ø§ÙˆÙ„ Ø§Ù„Ø¥Ø¶Ø§ÙÙŠØ© ÙˆØ§Ù„Ø®ØµØ§Ø¦Øµ ÙˆØ§Ù„Ø£Ø³Ø¹Ø§Ø±
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
                id INTEGER NOT NULL DEFAULT 0,
                partId INTEGER NOT NULL DEFAULT 0,
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

        // Upgrade existing databases where part_pricing compatibility columns are absent.
        val partPricingColumns = mutableSetOf<String>()
        conn.prepare("PRAGMA table_info(part_pricing)").use { stmt ->
            while (stmt.step()) partPricingColumns.add(stmt.getText(1))
        }
        if ("id" !in partPricingColumns) {
            conn.prepare("ALTER TABLE part_pricing ADD COLUMN id INTEGER NOT NULL DEFAULT 0").use { it.step() }
        }
        if ("partId" !in partPricingColumns) {
            conn.prepare("ALTER TABLE part_pricing ADD COLUMN partId INTEGER NOT NULL DEFAULT 0").use { it.step() }
        }
        conn.prepare("""
            UPDATE part_pricing 
            SET partId = (SELECT parts.id FROM parts WHERE parts.uuid = part_pricing.partUuid) 
            WHERE (partId = 0 OR partId IS NULL) 
              AND EXISTS (SELECT 1 FROM parts WHERE parts.uuid = part_pricing.partUuid AND parts.id > 0)
        """.trimIndent()).use { it.step() }

        // Ø³Ø¬Ù„ Ø§Ù„Ø¥Ø´Ø¹Ø§Ø±Ø§Øª (Append-only Log / Ø§Ù„Ø¥Ø´Ø¹Ø§Ø±Ø§Øª Ù…Ù‚Ø±ÙˆØ¡Ø© Ù…Ø­Ù„ÙŠØ§Ù‹ ÙÙ‚Ø·)
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

        // Atomic sequence table for safe ID allocation
        conn.prepare("""
            CREATE TABLE IF NOT EXISTS id_sequences (
                table_name TEXT PRIMARY KEY NOT NULL,
                last_id INTEGER NOT NULL
            );
        """.trimIndent()).use { it.step() }

        // 8. Ø§Ù„ÙÙ‡Ø§Ø±Ø³ (Optimized Production Indexes)
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_parts_categoryUuid ON parts(categoryUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_parts_defaultLocationUuid ON parts(defaultLocationUuid);").use { it.step() } }
        runCatching { conn.prepare("CREATE INDEX IF NOT EXISTS idx_parts_sync ON parts(syncStatus, isDeleted, updatedAt);").use { it.step() } }
        ensurePartIdUniqueIndex(conn)

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
        ensureCompanyNameUniqueIndex(conn)
        ensureCompanyIdUniqueIndex(conn)
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

    }

    /**
     * Assign a stable numeric compatibility ID to custom UUID rows that predate id persistence.
     * Only zero/missing IDs are touched, so reopening the database cannot change assigned IDs.
     */
    private fun backfillMissingNumericIds(conn: SQLiteConnection, table: String, prefix: String) {
        require((table == "parts" && prefix == "part-") ||
                (table == "companies" && prefix == "company-")) {
            "Unsupported ID backfill target: $table / $prefix"
        }
        val pendingRowIds = mutableListOf<Long>()
        conn.prepare("SELECT rowid FROM $table WHERE id = 0 ORDER BY rowid").use { stmt ->
            while (stmt.step()) pendingRowIds += stmt.getLong(0)
        }
        for (rowId in pendingRowIds) {
            var nextId = 1L
            conn.prepare("SELECT COALESCE(MAX(id), 0) + 1 FROM $table").use { stmt ->
                if (stmt.step()) nextId = stmt.getLong(0)
            }
            conn.prepare("UPDATE $table SET id = ? WHERE rowid = ? AND id = 0").use { stmt ->
                stmt.bindLong(1, nextId)
                stmt.bindLong(2, rowId)
                stmt.step()
            }
        }
    }

    /**
     * Keep the uniqueness invariant explicit: legacy duplicate names stop migration with a
     * useful diagnostic instead of silently running without the database constraint.
     */
    private fun ensureCompanyNameUniqueIndex(conn: SQLiteConnection) {
        val duplicates = mutableListOf<String>()
        conn.prepare("""
            SELECT LOWER(TRIM(name)), COUNT(*)
            FROM companies
            WHERE isDeleted = 0
            GROUP BY LOWER(TRIM(name))
            HAVING COUNT(*) > 1
            ORDER BY LOWER(TRIM(name))
            LIMIT 10
        """.trimIndent()).use { stmt ->
            while (stmt.step()) {
                duplicates += "${stmt.getText(0)} (${stmt.getLong(1)})"
            }
        }
        check(duplicates.isEmpty()) {
            "Database migration blocked: duplicate active company names must be resolved before adding idx_companies_unique_name: ${duplicates.joinToString()}"
        }

        conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_unique_name ON companies(LOWER(TRIM(name))) WHERE isDeleted = 0;").use { it.step() }

        var indexExists = false
        conn.prepare("SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = 'idx_companies_unique_name' LIMIT 1").use { stmt ->
            indexExists = stmt.step()
        }
        check(indexExists) {
            "Database migration failed: idx_companies_unique_name could not be verified."
        }
    }

    /**
     * Enforce strict uniqueness for active persisted part numeric IDs,
     * automatically reconciling any historical duplicates before creating the index.
     */
    private fun ensurePartIdUniqueIndex(conn: SQLiteConnection) {
        reconcileDuplicateNumericIds(conn, "parts")

        val duplicates = mutableListOf<String>()
        conn.prepare("""
            SELECT id, COUNT(*)
            FROM parts
            WHERE isDeleted = 0 AND id > 0
            GROUP BY id
            HAVING COUNT(*) > 1
            LIMIT 10
        """.trimIndent()).use { stmt ->
            while (stmt.step()) {
                duplicates += "id ${stmt.getLong(0)} (${stmt.getLong(1)} occurrences)"
            }
        }
        check(duplicates.isEmpty()) {
            "Database migration blocked: duplicate active part IDs must be resolved before adding idx_parts_unique_id: ${duplicates.joinToString()}"
        }

        conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_parts_unique_id ON parts(id) WHERE isDeleted = 0 AND id > 0;").use { it.step() }

        var indexExists = false
        conn.prepare("SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = 'idx_parts_unique_id' LIMIT 1").use { stmt ->
            indexExists = stmt.step()
        }
        check(indexExists) {
            "Database migration failed: idx_parts_unique_id could not be verified."
        }
    }

    /**
     * Enforce strict uniqueness for active persisted company numeric IDs,
     * automatically reconciling any historical duplicates before creating the index.
     */
    private fun ensureCompanyIdUniqueIndex(conn: SQLiteConnection) {
        reconcileDuplicateNumericIds(conn, "companies")

        val duplicates = mutableListOf<String>()
        conn.prepare("""
            SELECT id, COUNT(*)
            FROM companies
            WHERE isDeleted = 0 AND id > 0
            GROUP BY id
            HAVING COUNT(*) > 1
            LIMIT 10
        """.trimIndent()).use { stmt ->
            while (stmt.step()) {
                duplicates += "id ${stmt.getLong(0)} (${stmt.getLong(1)} occurrences)"
            }
        }
        check(duplicates.isEmpty()) {
            "Database migration blocked: duplicate active company IDs must be resolved before adding idx_companies_unique_id: ${duplicates.joinToString()}"
        }

        conn.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_unique_id ON companies(id) WHERE isDeleted = 0 AND id > 0;").use { it.step() }

        var indexExists = false
        conn.prepare("SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = 'idx_companies_unique_id' LIMIT 1").use { stmt ->
            indexExists = stmt.step()
        }
        check(indexExists) {
            "Database migration failed: idx_companies_unique_id could not be verified."
        }
    }

    /**
     * Safely and deterministically resolves any historical duplicate numeric IDs in legacy tables.
     * Keeps the original row with the ID, and reallocates colliding rows to a new unique ID,
     * simultaneously cascading the updated ID to dependent tables.
     */
    private fun reconcileDuplicateNumericIds(conn: SQLiteConnection, table: String) {
        val collidingIds = mutableListOf<Long>()
        conn.prepare("""
            SELECT id
            FROM $table
            WHERE isDeleted = 0 AND id > 0
            GROUP BY id
            HAVING COUNT(*) > 1
            ORDER BY id
        """.trimIndent()).use { stmt ->
            while (stmt.step()) {
                collidingIds.add(stmt.getLong(0))
            }
        }
        if (collidingIds.isEmpty()) return

        for (collidingId in collidingIds) {
            val duplicateRows = mutableListOf<Pair<Long, String>>() // rowid, uuid
            conn.prepare("""
                SELECT rowid, uuid
                FROM $table
                WHERE id = ? AND isDeleted = 0
                ORDER BY rowid ASC
            """.trimIndent()).use { stmt ->
                stmt.bindLong(1, collidingId)
                if (stmt.step()) {
                    // First row is kept as canonical
                    while (stmt.step()) {
                        duplicateRows.add(Pair(stmt.getLong(0), stmt.getText(1)))
                    }
                }
            }

            for ((rowId, uuid) in duplicateRows) {
                var newId = 1L
                conn.prepare("SELECT COALESCE(MAX(id), 0) + 1 FROM $table").use { stmt ->
                    if (stmt.step()) newId = stmt.getLong(0)
                }

                conn.prepare("UPDATE $table SET id = ? WHERE rowid = ?").use { stmt ->
                    stmt.bindLong(1, newId)
                    stmt.bindLong(2, rowId)
                    stmt.step()
                }

                if (table == "parts") {
                    if (tableExists(conn, "bom_items")) {
                        if (!hasColumn(conn, "bom_items", "partId")) {
                            conn.prepare("ALTER TABLE bom_items ADD COLUMN partId INTEGER NOT NULL DEFAULT 0").use { it.step() }
                        }
                        if (!hasColumn(conn, "bom_items", "subPartId")) {
                            conn.prepare("ALTER TABLE bom_items ADD COLUMN subPartId INTEGER NOT NULL DEFAULT 0").use { it.step() }
                        }
                        conn.prepare("UPDATE bom_items SET partId = ? WHERE partUuid = ?").use { stmt ->
                            stmt.bindLong(1, newId)
                            stmt.bindText(2, uuid)
                            stmt.step()
                        }
                        conn.prepare("UPDATE bom_items SET subPartId = ? WHERE subPartUuid = ?").use { stmt ->
                            stmt.bindLong(1, newId)
                            stmt.bindText(2, uuid)
                            stmt.step()
                        }
                    }
                    if (tableExists(conn, "stock_items")) {
                        if (!hasColumn(conn, "stock_items", "partId")) {
                            conn.prepare("ALTER TABLE stock_items ADD COLUMN partId INTEGER NOT NULL DEFAULT 0").use { it.step() }
                        }
                        conn.prepare("UPDATE stock_items SET partId = ? WHERE partUuid = ?").use { stmt ->
                            stmt.bindLong(1, newId)
                            stmt.bindText(2, uuid)
                            stmt.step()
                        }
                    }
                    if (tableExists(conn, "part_pricing")) {
                        if (!hasColumn(conn, "part_pricing", "partId")) {
                            conn.prepare("ALTER TABLE part_pricing ADD COLUMN partId INTEGER NOT NULL DEFAULT 0").use { it.step() }
                        }
                        conn.prepare("UPDATE part_pricing SET partId = ? WHERE partUuid = ?").use { stmt ->
                            stmt.bindLong(1, newId)
                            stmt.bindText(2, uuid)
                            stmt.step()
                        }
                    }
                } else if (table == "companies") {
                    if (tableExists(conn, "contacts")) {
                        if (!hasColumn(conn, "contacts", "companyId")) {
                            conn.prepare("ALTER TABLE contacts ADD COLUMN companyId INTEGER NOT NULL DEFAULT 0").use { it.step() }
                        }
                        conn.prepare("UPDATE contacts SET companyId = ? WHERE companyUuid = ?").use { stmt ->
                            stmt.bindLong(1, newId)
                            stmt.bindText(2, uuid)
                            stmt.step()
                        }
                    }
                    if (tableExists(conn, "addresses")) {
                        if (!hasColumn(conn, "addresses", "companyId")) {
                            conn.prepare("ALTER TABLE addresses ADD COLUMN companyId INTEGER NOT NULL DEFAULT 0").use { it.step() }
                        }
                        conn.prepare("UPDATE addresses SET companyId = ? WHERE companyUuid = ?").use { stmt ->
                            stmt.bindLong(1, newId)
                            stmt.bindText(2, uuid)
                            stmt.step()
                        }
                    }
                }
            }
        }
    }

    private fun hasColumn(conn: SQLiteConnection, tableName: String, columnName: String): Boolean {
        if (!tableExists(conn, tableName)) return false
        conn.prepare("PRAGMA table_info($tableName)").use { stmt ->
            while (stmt.step()) {
                if (stmt.getText(1).equals(columnName, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun tableExists(conn: SQLiteConnection, tableName: String): Boolean {
        conn.prepare("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1").use { stmt ->
            stmt.bindText(1, tableName)
            return stmt.step()
        }
    }
}