package com.nextstepai.inventory.data.db

/**
 * Allocates the next numeric domain id for legacy entities whose canonical
 * SQLite identity is currently encoded as a `<prefix><numeric-id>` UUID string.
 *
 * This is intentionally isolated from the legacy in-memory *Table classes so
 * cold-restart writes cannot reuse ids from a freshly constructed cache.
 */
object SqliteNumericIdAllocator {

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
        Key("supplier_price_breaks", "price-break-")
    )

    /**
     * Returns the next numeric id persisted in SQLite for the requested entity family.
     *
     * Non-numeric UUID suffixes are ignored deliberately. The current domain model
     * still exposes Long ids, so new writes must use the established numeric form.
     */
    fun nextId(table: String, prefix: String): Long {
        require(Key(table, prefix) in allowed) {
            "Unsupported numeric-id allocation target: $table / $prefix"
        }

        val suffixStart = prefix.length + 1
        val conn = SqliteDatabaseManager.getConnection()
        conn.prepare(
            """
            SELECT COALESCE(
                MAX(CAST(SUBSTR(uuid, $suffixStart) AS INTEGER)),
                0
            )
            FROM $table
            WHERE uuid LIKE ?
              AND SUBSTR(uuid, $suffixStart) GLOB '[0-9]*'
            """.trimIndent()
        ).use { stmt ->
            stmt.bindText(1, "$prefix%")
            if (stmt.step()) {
                val maxId = stmt.getLong(0)
                return if (maxId == Long.MAX_VALUE) {
                    throw IllegalStateException("No more numeric ids available for $table")
                } else {
                    maxId + 1L
                }
            }
        }

        return 1L
    }
}
