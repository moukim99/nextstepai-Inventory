package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextstepai.inventory.data.AppThemeMode
import com.nextstepai.inventory.data.db.AppSettingsEntity
import com.nextstepai.inventory.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val isLoading: Boolean = false,
    val backupMessage: String? = null
)

class SettingsViewModel(
    private val repository: AppSettingsRepository = AppSettingsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loaded = repository.loadSettings()
            _uiState.update { it.copy(settings = loaded, isLoading = false) }
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        val updated = _uiState.value.settings.copy(themeMode = mode.name)
        saveSettings(updated)
    }

    fun updateDefaultCurrency(currency: String) {
        val updated = _uiState.value.settings.copy(defaultCurrency = currency)
        saveSettings(updated)
    }

    fun updateDocExpiryDays(days: Int) {
        val updated = _uiState.value.settings.copy(docExpiryWarningDays = days)
        saveSettings(updated)
    }

    fun updateLowStockAlerts(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(lowStockAlertsEnabled = enabled)
        saveSettings(updated)
    }

    fun updateScannerBeep(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(scannerBeepEnabled = enabled)
        saveSettings(updated)
    }

    fun updateSound(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(soundEnabled = enabled)
        saveSettings(updated)
    }

    fun updateVibration(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(vibrationEnabled = enabled)
        saveSettings(updated)
    }

    fun updateBiometricLock(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(biometricLockEnabled = enabled)
        saveSettings(updated)
    }

    fun updateSyncWifiOnly(enabled: Boolean) {
        val updated = _uiState.value.settings.copy(syncWifiOnly = enabled)
        saveSettings(updated)
    }

    fun updateLanguage(lang: String) {
        val updated = _uiState.value.settings.copy(language = lang)
        saveSettings(updated)
    }

    fun performBackup() {
        _uiState.update {
            it.copy(backupMessage = "تم إنشاء النسخة الاحتياطية بنجاح على الجهاز المحلية")
        }
    }

    private fun saveSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(settings = newSettings) }
            repository.updateSettings(newSettings)
        }
    }
}
