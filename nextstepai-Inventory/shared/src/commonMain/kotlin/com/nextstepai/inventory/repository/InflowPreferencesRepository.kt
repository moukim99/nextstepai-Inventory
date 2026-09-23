package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.ui.InflowOptionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object InflowPreferencesRepository {

    suspend fun loadPreferences(): Pair<List<String>, List<InflowOptionItem>> = withContext(Dispatchers.IO) {
        try {
            val conn = SqliteDatabaseManager.getConnection()
            var pinnedIdsStr = "PURCHASE_ORDER,INTERNAL_BUILD"
            var customInflowText = ""

            val sql = "SELECT pinnedInflowIds, customInflowText FROM inflow_preferences WHERE id = 1"
            conn.prepare(sql).use { stmt ->
                if (stmt.step()) {
                    pinnedIdsStr = stmt.getText(0)
                    customInflowText = stmt.getText(1)
                }
            }

            val pinnedIds = pinnedIdsStr.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf("PURCHASE_ORDER", "INTERNAL_BUILD") }

            val customOptions = parseCustomOptions(customInflowText)
            Pair(pinnedIds, customOptions)
        } catch (e: Exception) {
            Pair(listOf("PURCHASE_ORDER", "INTERNAL_BUILD"), emptyList())
        }
    }

    suspend fun savePreferences(pinnedIds: List<String>, options: List<InflowOptionItem>) = withContext(Dispatchers.IO) {
        try {
            val conn = SqliteDatabaseManager.getConnection()
            val pinnedIdsStr = pinnedIds.joinToString(",")
            val customOptions = options.filter { it.isCustom }
            val customInflowText = serializeCustomOptions(customOptions)

            val sql = """
                INSERT INTO inflow_preferences (id, pinnedInflowIds, customInflowText)
                VALUES (1, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    pinnedInflowIds = excluded.pinnedInflowIds,
                    customInflowText = excluded.customInflowText
            """.trimIndent()

            conn.prepare(sql).use { stmt ->
                stmt.bindText(1, pinnedIdsStr)
                stmt.bindText(2, customInflowText)
                stmt.step()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseCustomOptions(text: String): List<InflowOptionItem> {
        if (text.isBlank()) return emptyList()
        return text.split(";;;").mapNotNull { chunk ->
            val parts = chunk.split("|||")
            if (parts.size >= 4) {
                InflowOptionItem(
                    id = parts[0].trim(),
                    title = parts[1].trim(),
                    description = parts[2].trim(),
                    iconEmoji = parts[3].trim(),
                    isCustom = true
                )
            } else null
        }
    }

    private fun serializeCustomOptions(options: List<InflowOptionItem>): String {
        if (options.isEmpty()) return ""
        return options.joinToString(";;;") { opt ->
            "${opt.id}|||${opt.title}|||${opt.description}|||${opt.iconEmoji}"
        }
    }
}
