package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.db.AppSettingsDao
import com.nextstepai.inventory.data.db.AppSettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(
    private val dao: AppSettingsDao = AppSettingsDao()
) {
    private val _settings = MutableStateFlow(AppSettingsEntity())
    val settings: StateFlow<AppSettingsEntity> = _settings.asStateFlow()

    suspend fun loadSettings(): AppSettingsEntity {
        val loaded = dao.getSettings()
        _settings.value = loaded
        return loaded
    }

    suspend fun updateSettings(newSettings: AppSettingsEntity) {
        dao.saveSettings(newSettings)
        _settings.value = newSettings
    }
}
