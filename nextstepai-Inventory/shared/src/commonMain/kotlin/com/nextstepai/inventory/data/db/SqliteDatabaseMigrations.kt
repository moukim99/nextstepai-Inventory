package com.nextstepai.inventory.data.db

import androidx.sqlite.SQLiteConnection

/**
 * وحدة إدارة وتنفيذ ترقيات وترحيلات الـ Schema التلقائية (Database Migrations).
 * تضمن ترقية الجداول دون التسبب في أخطاء تكرار الأعمدة أو فقدان البيانات.
 */
internal object SqliteDatabaseMigrations {

    val essentialTables: Set<String> = setOf(
        "parts", "companies", "stock_items", "bom_items", "part_pricing",
        "contacts", "addresses", "build_orders", "purchase_orders", "purchase_order_lines",
        "stock_locations", "stock_location_types", "part_categories", "part_parameters",
        "part_notes", "part_attachments", "part_test_templates", "part_related",
        "part_stars", "part_internal_prices", "part_sale_prices", "bom_item_substitutes",
        "stock_item_attachments", "stock_item_test_results", "company_attachments",
        "sales_orders", "sales_order_lines", "manufacturing_phases", "part_allocations",
        "manufacturer_parts", "supplier_parts", "notifications_history", "app_settings",
        "build_order_line_items", "build_items", "manufacturer_part_attachments",
        "part_parameter_templates", "part_category_parameter_templates", "stock_item_tracking"
    ).map { it.lowercase() }.toSet()

    private val dynamicOptionalTables = mutableSetOf<String>()

    val optionalTables: Set<String>
        get() = synchronized(dynamicOptionalTables) { dynamicOptionalTables.toSet() }

    /**
     * تسجيل جدول كاختياري في بيئة الاختبارات أو الوحدات الإضافية المعتمدة.
     * يفرض حماية صارمة لمنع تسجيل أي جدول أساسي (Essential) كجدول اختياري،
     * حفاظاً على صلابة المخطط وتفادي تجاوز أخطاء ترحيل الجداول الحيوية.
     */
    fun registerOptionalTable(tableName: String) {
        val normalized = tableName.lowercase().trim()
        require(normalized.isNotEmpty()) { "Table name cannot be empty." }
        require(normalized !in essentialTables) {
            "Security/Integrity violation: Cannot register essential core table '$tableName' as an optional table."
        }
        synchronized(dynamicOptionalTables) {
            dynamicOptionalTables.add(normalized)
        }
    }

    fun unregisterOptionalTable(tableName: String) {
        synchronized(dynamicOptionalTables) {
            dynamicOptionalTables.remove(tableName.lowercase().trim())
        }
    }

    fun extractTableName(sql: String): String? {
        val match = Regex(
            """ALTER\s+TABLE\s+(?:(?:`([^`]+)`|"([^"]+)"|\[([^\]]+)\]|([a-zA-Z0-9_]+))\.)?(?:`([^`]+)`|"([^"]+)"|\[([^\]]+)\]|([a-zA-Z0-9_]+))""",
            RegexOption.IGNORE_CASE
        ).find(sql) ?: return null

        return match.groupValues.slice(5..8).firstOrNull { it.isNotEmpty() }
    }

    fun addColumnIfMissing(conn: SQLiteConnection, sql: String) {
        val tableName = extractTableName(sql)
        val normalizedTable = tableName?.lowercase()?.trim()

        try {
            conn.prepare(sql).use { it.step() }
        } catch (e: Throwable) {
            val msg = e.message.orEmpty()
            val isDuplicate = msg.contains("duplicate column name", ignoreCase = true)
            if (isDuplicate) {
                // Column already exists - migration already completed for this column
                return
            }
            val isNoSuchTable = msg.contains("no such table", ignoreCase = true)
            if (isNoSuchTable) {
                if (normalizedTable.isNullOrEmpty()) {
                    throw IllegalStateException("Migration halted: cannot determine table name from SQL '$sql' upon missing table error.", e)
                }
                if (normalizedTable in essentialTables) {
                    throw IllegalStateException("Migration halted: required essential table '$tableName' is missing from schema.", e)
                }
                if (normalizedTable in optionalTables) {
                    // Known optional table that may not be present in legacy variant
                    return
                }
                // Unclassified or unrecognized table: halt migration under strict fail-closed policy
                throw IllegalStateException("Migration halted: unclassified table '$tableName' is missing from schema.", e)
            }
            throw e
        }
    }

    fun applyMigrations(conn: SQLiteConnection) {
        addColumnIfMissing(
            conn,
            "ALTER TABLE notifications_history ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE notifications_history ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE notifications_history ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE app_settings ADD COLUMN uuid TEXT NOT NULL DEFAULT 'default-settings';"
        )
        if (tableExists(conn, "app_settings")) {
            conn.prepare("UPDATE app_settings SET uuid = 'default-settings' WHERE uuid IS NULL OR uuid = '';").use { it.step() }
        }
        addColumnIfMissing(
            conn,
            "ALTER TABLE bom_items ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE bom_items ADD COLUMN subPartId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_orders ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_orders ADD COLUMN parentId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_orders ADD COLUMN salesOrderId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_orders ADD COLUMN takeFromLocationId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_orders ADD COLUMN destinationLocationId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_order_line_items ADD COLUMN id INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_order_line_items ADD COLUMN buildId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_order_line_items ADD COLUMN bomItemId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_order_line_items ADD COLUMN subPartId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_items ADD COLUMN id INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_items ADD COLUMN buildId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_items ADD COLUMN buildLineId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_items ADD COLUMN stockItemId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE build_items ADD COLUMN installIntoStockItemId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE purchase_orders ADD COLUMN supplierId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE purchase_order_lines ADD COLUMN supplierPartId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE stock_item_attachments ADD COLUMN attachmentId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE stock_item_attachments ADD COLUMN stockItemId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE stock_item_attachments ADD COLUMN userId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE stock_item_test_results ADD COLUMN resultId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE stock_item_test_results ADD COLUMN stockItemId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE stock_item_test_results ADD COLUMN templateId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE stock_item_test_results ADD COLUMN userId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE company_attachments ADD COLUMN userId INTEGER;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE stock_location_types ADD COLUMN typeId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE sales_orders ADD COLUMN customerId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE sales_order_lines ADD COLUMN orderId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE sales_order_lines ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE manufacturing_phases ADD COLUMN id INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE part_allocations ADD COLUMN id INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE part_allocations ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE part_allocations ADD COLUMN referenceId TEXT NOT NULL DEFAULT '';"
        )
        addColumnIfMissing(
            conn,
            "ALTER TABLE part_allocations ADD COLUMN createdByUserId TEXT NOT NULL DEFAULT '1';"
        )

        // Core tables UUID and numeric ID migrations
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN uuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        if (tableExists(conn, "parts") && hasColumn(conn, "parts", "uuid") && hasColumn(conn, "parts", "id")) {
            conn.prepare("UPDATE parts SET uuid = 'part-' || id WHERE (uuid IS NULL OR uuid = '') AND id > 0;").use { it.step() }
            conn.prepare("UPDATE parts SET uuid = 'part-' || rowid WHERE (uuid IS NULL OR uuid = '');").use { it.step() }
        }

        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN uuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN phone TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN email TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN isSupplier INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN isManufacturer INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN isCustomer INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD';")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN logoPath TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN parentUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN website TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN active INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN notes TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}';")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH';")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN lastModifiedByDeviceUuid TEXT;")
        if (tableExists(conn, "companies") && hasColumn(conn, "companies", "uuid") && hasColumn(conn, "companies", "id")) {
            conn.prepare("UPDATE companies SET uuid = 'company-' || id WHERE (uuid IS NULL OR uuid = '') AND id > 0;").use { it.step() }
            conn.prepare("UPDATE companies SET uuid = 'company-' || rowid WHERE (uuid IS NULL OR uuid = '');").use { it.step() }
        }

        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN uuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN locationId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN parentId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN ownerId INTEGER;")
        if (tableExists(conn, "stock_locations") && hasColumn(conn, "stock_locations", "uuid") && hasColumn(conn, "stock_locations", "id")) {
            conn.prepare("UPDATE stock_locations SET uuid = 'loc-' || id WHERE (uuid IS NULL OR uuid = '') AND id > 0;").use { it.step() }
            conn.prepare("UPDATE stock_locations SET uuid = 'loc-' || rowid WHERE (uuid IS NULL OR uuid = '');").use { it.step() }
        }

        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN uuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN parentId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN defaultLocationId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN parentUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN defaultLocationUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN structural INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN description TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN icon TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN lft INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN rght INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN treeId INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}';")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING';")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN lastModifiedByDeviceUuid TEXT;")
        if (tableExists(conn, "part_categories") && hasColumn(conn, "part_categories", "uuid") && hasColumn(conn, "part_categories", "id")) {
            conn.prepare("UPDATE part_categories SET uuid = 'cat-' || id WHERE (uuid IS NULL OR uuid = '') AND id > 0;").use { it.step() }
            conn.prepare("UPDATE part_categories SET uuid = 'cat-' || rowid WHERE (uuid IS NULL OR uuid = '');").use { it.step() }
        }

        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN uuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        if (tableExists(conn, "stock_items") && hasColumn(conn, "stock_items", "uuid") && hasColumn(conn, "stock_items", "id")) {
            conn.prepare("UPDATE stock_items SET uuid = 'stock-' || id WHERE (uuid IS NULL OR uuid = '') AND id > 0;").use { it.step() }
            conn.prepare("UPDATE stock_items SET uuid = 'stock-' || rowid WHERE (uuid IS NULL OR uuid = '');").use { it.step() }
        }

        // Parts table migrations
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN categoryId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN defaultLocationId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN variantOfId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN ipn TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN description TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN categoryUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN units TEXT NOT NULL DEFAULT 'pcs';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN minimumStock REAL NOT NULL DEFAULT 0.0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN maximumStock REAL;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN totalInStock REAL NOT NULL DEFAULT 0.0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN revision TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN keywords TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN assembly INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN component INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN variantOfUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN trackable INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN purchaseable INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN salable INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN virtual INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN active INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN locked INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN defaultLocationUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN defaultExpiryDays INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN link TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN localImagePath TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN lastModifiedByDeviceUuid TEXT;")

        // Stock locations table migrations
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN description TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN parentUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN structural INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN external INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN locationTypeUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN locationType TEXT NOT NULL DEFAULT 'SHELF';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN customCapacity REAL;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN isBulkGenerated INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN address TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN icon TEXT NOT NULL DEFAULT 'warehouse';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN customIcon TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN level INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN lft INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN rght INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN treeId INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN lastModifiedByDeviceUuid TEXT;")

        // Stock items table migrations
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN locationId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN purchaseOrderId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN supplierPartId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN salesOrderId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN customerId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN buildId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN parentId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN parentUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN stocktakeUserId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN partUuid TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN locationUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN quantity REAL NOT NULL DEFAULT 1.0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN serial TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN batch TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN statusCode INTEGER NOT NULL DEFAULT 10;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN packaging TEXT NOT NULL DEFAULT 'Box';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN expiryDate TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN notes TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN purchasePrice REAL NOT NULL DEFAULT 0.0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN purchasePriceCurrency TEXT NOT NULL DEFAULT 'USD';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN purchaseOrderUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN supplierPartUuid TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN salesOrderUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN customerUuid TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN buildUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN isBuilding INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN parentStockItemUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN stocktakeDate TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN stocktakeUserUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN reviewNeeded INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN deleteOnDeplete INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN link TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN lastModifiedByDeviceUuid TEXT;")

        // Additional legacy compatibility columns for seamless dual-schema operations
        addColumnIfMissing(conn, "ALTER TABLE manufacturer_part_attachments ADD COLUMN userId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN parentId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE part_categories ADD COLUMN defaultLocationId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE part_parameter_templates ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_category_parameter_templates ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_category_parameter_templates ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_category_parameter_templates ADD COLUMN parameterTemplateId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN locationId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN parentId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN ownerId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN categoryId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN defaultLocationId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN variantOfId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN locationId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN purchaseOrderId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN supplierPartId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN salesOrderId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN customerId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN buildId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN parentId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN parentUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN stocktakeUserId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_item_tracking ADD COLUMN trackingId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_item_tracking ADD COLUMN stockItemId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_item_tracking ADD COLUMN date TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_item_tracking ADD COLUMN userId INTEGER;")
        addColumnIfMissing(conn, "ALTER TABLE stock_item_tracking ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_item_tracking ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE companies ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN uuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN companyId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN companyUuid TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN phone TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN email TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN role TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN isPrimary INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING';")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN lastModifiedByDeviceUuid TEXT;")

        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN uuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN companyId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN companyUuid TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN title TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN isPrimary INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN line1 TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN line2 TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN postalCode TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN city TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN province TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN country TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN shippingNotes TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING';")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN lastModifiedByDeviceUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE manufacturer_parts ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE manufacturer_parts ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE manufacturer_parts ADD COLUMN manufacturerId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE supplier_parts ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE supplier_parts ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE supplier_parts ADD COLUMN manufacturerPartId INTEGER NOT NULL DEFAULT 0;")

        addColumnIfMissing(conn, "ALTER TABLE part_parameters ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_parameters ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_parameters ADD COLUMN templateId INTEGER NOT NULL DEFAULT 0;")

        addColumnIfMissing(conn, "ALTER TABLE part_notes ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_notes ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_notes ADD COLUMN userId INTEGER;")

        addColumnIfMissing(conn, "ALTER TABLE part_attachments ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_attachments ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_attachments ADD COLUMN userId INTEGER;")

        addColumnIfMissing(conn, "ALTER TABLE part_test_templates ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_test_templates ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")

        addColumnIfMissing(conn, "ALTER TABLE part_related ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_related ADD COLUMN part1Id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_related ADD COLUMN part2Id INTEGER NOT NULL DEFAULT 0;")

        addColumnIfMissing(conn, "ALTER TABLE part_stars ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_stars ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_stars ADD COLUMN userId INTEGER NOT NULL DEFAULT 1;")

        addColumnIfMissing(conn, "ALTER TABLE part_internal_prices ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_internal_prices ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")

        addColumnIfMissing(conn, "ALTER TABLE part_sale_prices ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_sale_prices ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")

        addColumnIfMissing(conn, "ALTER TABLE part_pricing ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE part_pricing ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")

        // Explicitly backfill legacy part_pricing references from parts table without suppressing errors
        if (tableExists(conn, "part_pricing")) {
            conn.prepare("""
                UPDATE part_pricing 
                SET partId = (SELECT parts.id FROM parts WHERE parts.uuid = part_pricing.partUuid)
                WHERE (partId = 0 OR partId IS NULL)
                  AND EXISTS (SELECT 1 FROM parts WHERE parts.uuid = part_pricing.partUuid AND parts.id > 0);
            """.trimIndent()).use { it.step() }
            conn.prepare("UPDATE part_pricing SET id = rowid WHERE id = 0;").use { it.step() }
        }

        addColumnIfMissing(conn, "ALTER TABLE bom_item_substitutes ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE bom_item_substitutes ADD COLUMN bomItemId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE bom_item_substitutes ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
    }

    fun tableExists(conn: SQLiteConnection, tableName: String): Boolean {
        conn.prepare("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1").use { stmt ->
            stmt.bindText(1, tableName)
            return stmt.step()
        }
    }

    fun hasColumn(conn: SQLiteConnection, tableName: String, columnName: String): Boolean {
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
}
