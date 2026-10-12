package com.nextstepai.inventory.data.db

import kotlin.time.Clock

/**
 * Allocates the next numeric domain id for legacy entities whose canonical
 * SQLite identity is currently encoded as a `<prefix><numeric-id>` UUID string.
 *
 * Uses an SQLite IMMEDIATE write transaction around sequence initialization,
 * increment, and read. SQLite serializes writers across independent connections
 * and processes; the JVM lock additionally protects the shared in-process connection.
 */
object SqliteNumericIdAllocator {

    private val lock = Any()

    private data class Key(
        val table: String,
        val prefix: String
    )

    private val allowed = setOf(
        Key("parts", "part-"),
        Key("companies", "company-"),
        Key("stock_locations", "loc-"),
        Key("stock_locations", "location-"),
        Key("company_attachments", "company-att-"),
        Key("contacts", "contact-"),
        Key("addresses", "address-"),
        Key("company_bank_accounts", "bank-"),
        Key("company_legal_records", "legal-"),
        Key("manufacturer_parts", "mfg-part-"),
        Key("manufacturer_part_parameters", "mfg-param-"),
        Key("manufacturer_part_attachments", "mfg-part-att-"),
        Key("supplier_parts", "sup-part-"),
        Key("supplier_price_breaks", "price-break-"),
        Key("part_attachments", "part-att-"),
        Key("part_internal_prices", "part-iprice-"),
        Key("part_sale_prices", "part-sprice-"),
        Key("part_test_templates", "part-test-"),
        Key("part_notes", "part-note-"),
        Key("part_pricing", "part-pricing-"),
        Key("part_stars", "part-star-"),
        Key("part_parameters", "part-param-"),
        Key("part_related", "part-rel-"),
        Key("part_category_parameter_templates", "cat-param-tpl-"),
        Key("part_categories", "cat-"),
        Key("purchase_orders", "po-"),
        Key("purchase_order_lines", "po-line-"),
        Key("sales_orders", "so-"),
        Key("sales_order_lines", "so-line-"),
        Key("build_orders", "build-"),
        Key("build_orders", "bo-"),
        Key("build_order_line_items", "bo-line-"),
        Key("build_items", "build-item-"),
        Key("stock_items", "stock-"),
        Key("stock_item_attachments", "attachment-"),
        Key("stock_item_test_results", "test-result-")
    )

    /**
     * Returns the next numeric id persisted in SQLite for the requested entity family.
     * Guaranteed to be strictly monotonic and thread-safe.
     * Allows passing an explicit connection for multi-connection / multi-process environments.
     */
    fun nextId(
        table: String,
        prefix: String,
        connection: androidx.sqlite.SQLiteConnection? = null
    ): Long {
        val allocate = {
            require(Key(table, prefix) in allowed) {
                "Unsupported numeric-id allocation target: $table / $prefix"
            }

            val suffixStart = prefix.length + 1
            val conn = connection ?: SqliteDatabaseManager.getConnection()
        // Parts, companies, and stock locations persist numeric IDs independently of the UUID,
        // so their sequence must consider both custom-UUID rows and legacy prefixed UUIDs.
        val maxIdQuery = if (table == "parts" || table == "companies" || table == "stock_locations") {
            """
            SELECT MAX(
                COALESCE((SELECT MAX(id) FROM $table), 0),
                COALESCE((
                    SELECT MAX(CAST(SUBSTR(uuid, $suffixStart) AS INTEGER))
                    FROM $table
                    WHERE uuid LIKE ? AND SUBSTR(uuid, $suffixStart) GLOB '[0-9]*'
                ), 0)
            )
            """.trimIndent()
        } else {
            """
            SELECT COALESCE(
                MAX(CAST(SUBSTR(uuid, $suffixStart) AS INTEGER)),
                0
            )
            FROM $table
            WHERE uuid LIKE ? AND SUBSTR(uuid, $suffixStart) GLOB '[0-9]*'
            """.trimIndent()
        }

        // The write lock is acquired before reading or changing the sequence. This
        // closes the race where two processes could both read the same last_id.
        // If an outer transaction is already active on this connection, nest via SAVEPOINT.
        var startedTx = false
        var savepointName: String? = null
        try {
            conn.prepare("BEGIN IMMEDIATE").use { it.step() }
            startedTx = true
        } catch (e: Throwable) {
            val msg = e.message ?: ""
            if (msg.contains("cannot start a transaction within a transaction", ignoreCase = true)) {
                val spName = "id_alloc_${table}_${Clock.System.now().toEpochMilliseconds()}"
                savepointName = spName
                conn.prepare("SAVEPOINT $spName;").use { it.step() }
            } else {
                throw e
            }
        }

        try {
            conn.prepare(
                """
                INSERT OR IGNORE INTO id_sequences (table_name, last_id)
                VALUES (?, ($maxIdQuery))
                """.trimIndent()
            ).use { stmt ->
                stmt.bindText(1, table)
                stmt.bindText(2, "$prefix%")
                stmt.step()
            }

            conn.prepare(
                """
                UPDATE id_sequences
                SET last_id = MAX(
                    last_id + 1,
                    ($maxIdQuery) + 1
                )
                WHERE table_name = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.bindText(1, "$prefix%")
                stmt.bindText(2, table)
                stmt.step()
            }

            var allocated: Long? = null
            conn.prepare("SELECT last_id FROM id_sequences WHERE table_name = ?").use { stmt ->
                stmt.bindText(1, table)
                if (stmt.step()) allocated = stmt.getLong(0)
            }
            val result = allocated
                ?: throw IllegalStateException("Unable to allocate a numeric id for $table")
            check(result < Long.MAX_VALUE) { "No more numeric ids available for $table" }

            if (startedTx) {
                conn.prepare("COMMIT").use { it.step() }
            } else if (savepointName != null) {
                conn.prepare("RELEASE SAVEPOINT $savepointName;").use { it.step() }
            }
            result
        } catch (failure: Throwable) {
            if (startedTx) {
                runCatching { conn.prepare("ROLLBACK").use { it.step() } }
            } else if (savepointName != null) {
                runCatching { conn.prepare("ROLLBACK TO SAVEPOINT $savepointName;").use { it.step() } }
            }
            throw failure
        }
    }
    return if (connection != null) allocate() else synchronized(lock) { allocate() }
}
}
