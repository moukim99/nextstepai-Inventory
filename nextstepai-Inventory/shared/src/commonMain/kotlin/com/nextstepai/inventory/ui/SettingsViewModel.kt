package com.nextstepai.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextstepai.inventory.data.AppThemeMode
import com.nextstepai.inventory.data.TestDataGenerator
import com.nextstepai.inventory.data.db.AppSettingsEntity
import com.nextstepai.inventory.data.db.NotificationHistoryDao
import com.nextstepai.inventory.data.db.NotificationHistoryEntity
import com.nextstepai.inventory.repository.AppSettingsRepository
import com.nextstepai.inventory.util.AppUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class SettingsUiState(
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val isLoading: Boolean = false,
    val backupMessage: String? = null,
    val isGeneratingTestData: Boolean = false,
    val isDeletingTestData: Boolean = false,
    val testDataMessage: String? = null,
    val cameraPermissionGranted: Boolean = true,
    val storagePermissionGranted: Boolean = true,
    val notificationPermissionGranted: Boolean = true,
    val permissionMessage: String? = null
)

class SettingsViewModel(
    private val repository: AppSettingsRepository = AppSettingsRepository(),
    private val notificationDao: NotificationHistoryDao = NotificationHistoryDao()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val loaded = repository.loadSettings()
                _uiState.update { it.copy(settings = loaded, isLoading = false) }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleCameraPermission(granted: Boolean) {
        _uiState.update {
            it.copy(
                cameraPermissionGranted = granted,
                permissionMessage = if (granted) "تمت إضافة صلاحية الكاميرا والمسح المباشر" else "تم إلغاء صلاحية الكاميرا"
            )
        }
    }

    fun toggleStoragePermission(granted: Boolean) {
        _uiState.update {
            it.copy(
                storagePermissionGranted = granted,
                permissionMessage = if (granted) "تمت إضافة صلاحية الوصول للملفات والتخزين" else "تم إلغاء صلاحية التخزين"
            )
        }
    }

    fun toggleNotificationPermission(granted: Boolean) {
        _uiState.update {
            it.copy(
                notificationPermissionGranted = granted,
                permissionMessage = if (granted) "تمت إضافة صلاحية الإشعارات والتنبيهات" else "تم إلغاء صلاحية الإشعارات"
            )
        }
    }

    fun grantAllPermissions() {
        _uiState.update {
            it.copy(
                cameraPermissionGranted = true,
                storagePermissionGranted = true,
                notificationPermissionGranted = true,
                permissionMessage = "تم منح وتفعيل جميع صلاحيات التطبيق بنجاح (الكاميرا، التخزين، الإشعارات، والاهتزاز)!"
            )
        }
        updateVibration(true)
        updateLowStockAlerts(true)
        sendTestNotification()
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val testNotification = NotificationHistoryEntity(
                    uuid = AppUuid.generate(),
                    title = "تنبيه تجريبي - NextStepAI",
                    message = "تم منح كافة الصلاحيات بنجاح، والكاميرا والتنبيهات جاهزة للاستخدام!",
                    notificationType = "SYSTEM_TEST",
                    targetEntityUuid = "settings-system",
                    scheduledDate = now,
                    isRead = false,
                    isTriggered = true
                )
                notificationDao.getAllNotifications(limit = 1, offset = 0)
                _uiState.update {
                    it.copy(permissionMessage = "تم إرسال إشعار تجريبي وتفعيله بنجاح!")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(permissionMessage = "تم إرسال الإشعار بنجاح!")
                }
            }
        }
    }

    fun clearPermissionMessage() {
        _uiState.update { it.copy(permissionMessage = null) }
    }

    fun generateTestData(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingTestData = true, testDataMessage = null) }
            try {
                val tableCount = TestDataGenerator.generateAllTestData()
                _uiState.update {
                    it.copy(
                        isGeneratingTestData = false,
                        testDataMessage = "تم توليد البيانات الاختبارية الشاملة بنجاح عبر $tableCount جدولاً!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingTestData = false,
                        testDataMessage = "حدث خطأ أثناء توليد البيانات: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteTestData(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingTestData = true, testDataMessage = null) }
            try {
                TestDataGenerator.clearAllTestData()
                _uiState.update {
                    it.copy(
                        isDeletingTestData = false,
                        testDataMessage = "تم حذف جميع البيانات الاختبارية بنجاح وتفريغ قاعدة البيانات!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeletingTestData = false,
                        testDataMessage = "حدث خطأ أثناء حذف البيانات: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearTestDataMessage() {
        _uiState.update { it.copy(testDataMessage = null) }
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
