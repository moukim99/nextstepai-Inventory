package com.nextstepai.inventory.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * مستودع حفظ وتفضيلات مظهر التطبيق (Dark/Light/System) باستخدام DataStore.
 */
class ThemePreferenceRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val themeKey = stringPreferencesKey("app_theme_mode")

    val themeMode: Flow<AppThemeMode> = dataStore.data.map { prefs ->
        val raw = prefs[themeKey] ?: AppThemeMode.SYSTEM.name
        runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }
}
