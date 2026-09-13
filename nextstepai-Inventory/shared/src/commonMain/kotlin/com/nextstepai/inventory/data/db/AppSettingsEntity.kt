package com.nextstepai.inventory.data.db

data class AppSettingsEntity(
    val id: Int = 1,
    val notificationTime: String = "09:00",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val docExpiryWarningDays: Int = 30,
    val lowStockAlertsEnabled: Boolean = true,
    val themeMode: String = "SYSTEM",
    val language: String = "ar",
    val defaultCurrency: String = "USD",
    val scannerBeepEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val syncWifiOnly: Boolean = false
)
