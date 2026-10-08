package com.nextstepai.inventory.data.db

import androidx.sqlite.SQLiteConnection

/**
 * وحدة إدارة وتنفيذ ترقيات وترحيلات الـ Schema التلقائية (Database Migrations).
 * تضمن ترقية الجداول دون التسبب في أخطاء تكرار الأعمدة أو فقدان البيانات.
 */
internal object SqliteDatabaseMigrations {

    fun addColumnIfMissing(conn: SQLiteConnection, sql: String) {
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
        runCatching {
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

        // Parts table migrations
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN categoryUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN defaultLocationUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN variantOfUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN localImagePath TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_PUSH';")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE parts ADD COLUMN lastModifiedByDeviceUuid TEXT;")

        // Stock locations table migrations
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN parentUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN locationTypeUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN locationType TEXT NOT NULL DEFAULT 'SHELF';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN customCapacity REAL;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN isBulkGenerated INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN address TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN icon TEXT NOT NULL DEFAULT 'warehouse';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN customIcon TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN metadata TEXT NOT NULL DEFAULT '{}';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN version INTEGER NOT NULL DEFAULT 1;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING';")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_locations ADD COLUMN lastModifiedByDeviceUuid TEXT;")

        // Stock items table migrations
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN partUuid TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN locationUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN packaging TEXT NOT NULL DEFAULT 'Box';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN purchasePrice REAL NOT NULL DEFAULT 0.0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN purchasePriceCurrency TEXT NOT NULL DEFAULT 'USD';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN purchaseOrderUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN supplierPartUuid TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN salesOrderUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN customerUuid TEXT NOT NULL DEFAULT '';")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN buildUuid TEXT;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN isBuilding INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE stock_items ADD COLUMN parentStockItemUuid TEXT;")
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
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE contacts ADD COLUMN companyId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE addresses ADD COLUMN companyId INTEGER NOT NULL DEFAULT 0;")
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

        addColumnIfMissing(conn, "ALTER TABLE bom_item_substitutes ADD COLUMN id INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE bom_item_substitutes ADD COLUMN bomItemId INTEGER NOT NULL DEFAULT 0;")
        addColumnIfMissing(conn, "ALTER TABLE bom_item_substitutes ADD COLUMN partId INTEGER NOT NULL DEFAULT 0;")
    }
}
