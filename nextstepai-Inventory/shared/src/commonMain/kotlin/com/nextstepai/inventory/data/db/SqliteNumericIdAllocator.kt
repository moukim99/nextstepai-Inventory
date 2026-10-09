package com.nextstepai.inventory.data.db

/**
 * Allocates the next numeric domain id for legacy entities whose canonical
 * SQLite identity is currently encoded as a `<prefix><numeric-id>` UUID string.
 *
 * Uses an atomic sequence table (`id_sequences`) combined with thread-level
 * synchronization to guarantee unique, collision-free allocations even under
 * concurrent access and cold restarts.
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
        Key("part_parameter_templates", "param-tpl-"),
        Key("part_category_parameter_templates", "cat-param-tpl-")
    )

    /**
     * Returns the next numeric id persisted in SQLite for the requested entity family.
     * Guaranteed to be strictly monotonic and thread-safe.
     */
    fun nextId(table: String, prefix: String): Long = synchronized(lock) {
        require(Key(table, prefix) in allowed) {
            "Unsupported numeric-id allocation target: $table / $prefix"
        }

        val suffixStart = prefix.length + 1
        val conn = SqliteDatabaseManager.getConnection()

        // 1. Ensure sequence row exists and reflects at least MAX(id) from the table
        conn.prepare(
            """
            INSERT OR IGNORE INTO id_sequences (table_name, last_id)
            VALUES (?, (
                SELECT COALESCE(
                    MAX(CAST(SUBSTR(uuid, $suffixStart) AS INTEGER)),
                    0
                )
                FROM $table
                WHERE uuid LIKE ?
                  AND SUBSTR(uuid, $suffixStart) GLOB '[0-9]*'
            ))
            """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, table)
            stmt.bindText(2, "$prefix%")
            stmt.step()
        }

        // 2. Increment atomically, ensuring it is at least MAX(existing_table_id) + 1
        conn.prepare(
            """
            UPDATE id_sequences
            SET last_id = MAX(
                last_id + 1,
                (
                    SELECT COALESCE(
                        MAX(CAST(SUBSTR(uuid, $suffixStart) AS INTEGER)),
                        0
                    ) + 1
                    FROM $table
                    WHERE uuid LIKE ?
                      AND SUBSTR(uuid, $suffixStart) GLOB '[0-9]*'
                )
            )
            WHERE table_name = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, "$prefix%")
            stmt.bindText(2, table)
            stmt.step()
        }

        // 3. Read the allocated ID
        conn.prepare("SELECT last_id FROM id_sequences WHERE table_name = ?").use { stmt ->
            stmt.bindText(1, table)
            if (stmt.step()) {
                val allocated = stmt.getLong(0)
                if (allocated == Long.MAX_VALUE) {
                    throw IllegalStateException("No more numeric ids available for $table")
                }
                return allocated
            }
        }

        return 1L
    }
}
