package com.nextstepai.inventory.data.db

import androidx.room.Dao
import androidx.sqlite.SQLiteStatement

@Dao
class AppSettingsDao {

    suspend fun getSettings(): AppSettingsEntity {
        val conn = SqliteDatabaseManager.getConnection()
        var entity: AppSettingsEntity? = null

        val sql = """
            SELECT uuid, notificationTime, soundEnabled, vibrationEnabled, docExpiryWarningDays,
                   lowStockAlertsEnabled, themeMode, language, defaultCurrency, scannerBeepEnabled,
                   biometricLockEnabled, syncWifiOnly
            FROM app_settings
            WHERE uuid = 'default-settings'
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            if (stmt.step()) {
                entity = mapAppSettings(stmt)
            }
        }

        if (entity == null) {
            val defaultSettings = AppSettingsEntity()
            saveSettings(defaultSettings)
            return defaultSettings
        }
        return entity!!
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        val conn = SqliteDatabaseManager.getConnection()
        val sql = """
            INSERT INTO app_settings (
                uuid, notificationTime, soundEnabled, vibrationEnabled, docExpiryWarningDays,
                lowStockAlertsEnabled, themeMode, language, defaultCurrency, scannerBeepEnabled,
                biometricLockEnabled, syncWifiOnly
            ) VALUES ('default-settings', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                notificationTime = excluded.notificationTime,
                soundEnabled = excluded.soundEnabled,
                vibrationEnabled = excluded.vibrationEnabled,
                docExpiryWarningDays = excluded.docExpiryWarningDays,
                lowStockAlertsEnabled = excluded.lowStockAlertsEnabled,
                themeMode = excluded.themeMode,
                language = excluded.language,
                defaultCurrency = excluded.defaultCurrency,
                scannerBeepEnabled = excluded.scannerBeepEnabled,
                biometricLockEnabled = excluded.biometricLockEnabled,
                syncWifiOnly = excluded.syncWifiOnly
        """.trimIndent()

        conn.prepare(sql).use { stmt ->
            stmt.bindText(1, settings.notificationTime)
            stmt.bindLong(2, if (settings.soundEnabled) 1L else 0L)
            stmt.bindLong(3, if (settings.vibrationEnabled) 1L else 0L)
            stmt.bindLong(4, settings.docExpiryWarningDays.toLong())
            stmt.bindLong(5, if (settings.lowStockAlertsEnabled) 1L else 0L)
            stmt.bindText(6, settings.themeMode)
            stmt.bindText(7, settings.language)
            stmt.bindText(8, settings.defaultCurrency)
            stmt.bindLong(9, if (settings.scannerBeepEnabled) 1L else 0L)
            stmt.bindLong(10, if (settings.biometricLockEnabled) 1L else 0L)
            stmt.bindLong(11, if (settings.syncWifiOnly) 1L else 0L)
            stmt.step()
        }
    }

    private fun mapAppSettings(stmt: SQLiteStatement): AppSettingsEntity {
        return AppSettingsEntity(
            uuid = stmt.getText(0),
            id = 1,
            notificationTime = stmt.getText(1),
            soundEnabled = stmt.getLong(2) == 1L,
            vibrationEnabled = stmt.getLong(3) == 1L,
            docExpiryWarningDays = stmt.getLong(4).toInt(),
            lowStockAlertsEnabled = stmt.getLong(5) == 1L,
            themeMode = stmt.getText(6),
            language = stmt.getText(7),
            defaultCurrency = stmt.getText(8),
            scannerBeepEnabled = stmt.getLong(9) == 1L,
            biometricLockEnabled = stmt.getLong(10) == 1L,
            syncWifiOnly = stmt.getLong(11) == 1L
        )
    }
}
