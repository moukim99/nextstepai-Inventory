package com.nextstepai.inventory.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.AppThemeMode
import org.jetbrains.compose.resources.stringResource
import nextstepai_inventory.shared.generated.resources.*

/**
 * شاشة الإعدادات المخصصة للتحكم في الحساب والجلسة والمظهر والمخزون والأمان والنسخ الاحتياطي.
 */
@Composable
fun SettingsScreen(
    loginUiState: LoginUiState,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = remember { SettingsViewModel() }
) {
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val settings = settingsUiState.settings

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. ترويسة شاشة الإعدادات المزدوجة الموحدة
            SettingsTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. كارت معلومات الجلسة والحساب (Session & Account Info)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = loginUiState.currentSession?.username ?: stringResource(Res.string.username),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = stringResource(
                                        Res.string.session_id,
                                        loginUiState.currentSession?.id ?: 1
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Text(
                                        text = stringResource(Res.string.session_active),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. كارت اختيار المظهر واللغة (Theme & Language)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            title = stringResource(Res.string.settings_preferences_title),
                            icon = Icons.Default.Palette
                        )

                        Text(
                            text = stringResource(Res.string.nav_theme),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf(
                                AppThemeMode.SYSTEM to (stringResource(Res.string.theme_system) to Icons.Default.SettingsSuggest),
                                AppThemeMode.LIGHT to (stringResource(Res.string.theme_light) to Icons.Default.LightMode),
                                AppThemeMode.DARK to (stringResource(Res.string.theme_dark) to Icons.Default.DarkMode)
                            )

                            themes.forEach { (mode, pair) ->
                                val (title, icon) = pair
                                val isSelected = themeMode == mode
                                Surface(
                                    onClick = {
                                        onThemeModeChange(mode)
                                        settingsViewModel.updateThemeMode(mode)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            ),
                                            color = if (isSelected) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Text(
                            text = "اللغة (Language)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val languages = listOf("ar" to "العربية", "en" to "English", "fr" to "Français")
                            languages.forEach { (code, name) ->
                                val isSelected = settings.language == code
                                Surface(
                                    onClick = { settingsViewModel.updateLanguage(code) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. كارت إعدادات المخزون والتوريد (Inventory & Supply Settings)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            title = "إعدادات المخزون والتوريد",
                            icon = Icons.Default.Inventory2
                        )

                        Text(
                            text = stringResource(Res.string.settings_currency_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val currencies = listOf("USD", "DZD", "EUR", "SAR", "AED")
                            currencies.forEach { curr ->
                                val isSelected = settings.defaultCurrency == curr
                                Surface(
                                    onClick = { settingsViewModel.updateDefaultCurrency(curr) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = curr,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // تنبيهات انخفاض المخزون
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تنبيهات انخفاض المخزون",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "إشعار عند وصول القطع للحد الأدنى للمخزون",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = settings.lowStockAlertsEnabled,
                                onCheckedChange = { settingsViewModel.updateLowStockAlerts(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4F46E5)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // حد التنبيه المبكر لانتهاء الوثائق والعقود
                        Text(
                            text = "تنبيه انتهاء العقود/الوثائق (بالأيام)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val daysList = listOf(7, 15, 30, 60)
                            daysList.forEach { days ->
                                val isSelected = settings.docExpiryWarningDays == days
                                Surface(
                                    onClick = { settingsViewModel.updateDocExpiryDays(days) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$days يومًا",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp).fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. كارت الأجهزة وقارئ الباركود (Hardware & Scanner)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            title = "إعدادات الأجهزة والتنبيهات",
                            icon = Icons.Default.QrCodeScanner
                        )

                        // نغمة قارئ الباركود
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "صوت قارئ الباركود (Beep)",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "إصدار نغمة تأكيد عند مسح الكود بنجاح",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = settings.scannerBeepEnabled,
                                onCheckedChange = { settingsViewModel.updateScannerBeep(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4F46E5)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // الأصوات والاهتزاز
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "الاهتزاز والتغذية الراجعة",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "تشغيل الاهتزاز عند التفاعل والملمس الرقمي",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = settings.vibrationEnabled,
                                onCheckedChange = { settingsViewModel.updateVibration(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4F46E5)
                                )
                            )
                        }
                    }
                }

                // 5. كارت الأمان والمزامنة (Security & Sync)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            title = "الأمان والمزامنة",
                            icon = Icons.Default.Security
                        )

                        // القفل الحيوي
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "القفل الحيوي (Biometric Lock)",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "قفل فتح التطبيق بفرز البصمة/الوجه",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = settings.biometricLockEnabled,
                                onCheckedChange = { settingsViewModel.updateBiometricLock(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4F46E5)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // المزامنة عبر Wi-Fi فقط
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مزامنة Wi-Fi فقط",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "تقييد مزامنة البيانات عبر شبكة Wi-Fi لتوفير الباقة",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = settings.syncWifiOnly,
                                onCheckedChange = { settingsViewModel.updateSyncWifiOnly(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4F46E5)
                                )
                            )
                        }
                    }
                }

                // 6. كارت النسخ الاحتياطي (Backup)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSectionHeader(
                            title = stringResource(Res.string.settings_backup_label),
                            icon = Icons.Default.Backup
                        )

                        Button(
                            onClick = { settingsViewModel.performBackup() },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.settings_backup_btn),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        settingsUiState.backupMessage?.let { status ->
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 7. زر تسجيل الخروج (Logout Button)
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC2626)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFDC2626))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.logout),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFFDC2626)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * الترويسة العلوية لشاشة الإعدادات
 */
@Composable
private fun SettingsTopBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "الإعدادات والتفضيلات",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "إدارة الحساب، المظهر، خيارات المخزون، الأمان والنسخ الاحتياطي",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * عنوان القسم الفرعي مع صندوق الأيقونة النيلي
 */
@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEEF2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF4F46E5),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = Color(0xFF0F172A)
        )
    }
}
