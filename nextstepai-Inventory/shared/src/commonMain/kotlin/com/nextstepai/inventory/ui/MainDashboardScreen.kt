package com.nextstepai.inventory.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.session_active
import nextstepai_inventory.shared.generated.resources.session_id
import nextstepai_inventory.shared.generated.resources.session_info_header
import nextstepai_inventory.shared.generated.resources.session_status
import nextstepai_inventory.shared.generated.resources.session_user

/**
 * الشاشة الرئيسية الموحدة لتطبيق إدارة المخزون والتصنيع الذكي.
 * مطابقة تماماً للتصميم المستهدف (Target Design) مع الألوان المحسنة، وترتيب الكروت، وبار الباركود السريع.
 */
@Composable
fun MainDashboardScreen(
    loginUiState: LoginUiState,
    onLogoutClick: () -> Unit,
    onOpenPartsClick: () -> Unit,
    onOpenBomClick: () -> Unit,
    onOpenStockClick: () -> Unit,
    onOpenCompaniesClick: () -> Unit,
    onOpenOrdersClick: () -> Unit,
    onOpenBuildsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit = {}
) {
    var hasNotifications by remember { mutableStateOf(value = true) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ترويسة التطبيق العلوية Top App Header
            DashboardTopHeader(
                onLogoutClick = onLogoutClick,
                hasNotifications = hasNotifications,
                onNotificationClick = { hasNotifications = !hasNotifications }
            )

            // محتوى لوحة التحكم القابل للتمرير Dashboard Scrollable Content
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val isWideScreen = maxWidth >= 600.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. كارت الترحيب الرئيسي Welcome Hero Banner
                    WelcomeHeroBanner(username = loginUiState.currentSession?.username ?: "زائر المستودع")

                    // 2. كروت المؤشرات التشغيلية KPIs Metric Grid (أوامر التصنيع، نواقص الرفوف، طلبات شراء)
                    OperationalKpiSection()

                    // 3. كارت إجراء مسح الباركود السريع (Barcode / QR Action Card) مباشرة بعد الـ KPIs
                    BarcodeScannerActionCard(onScanClick = onScanClick)

                    // 4. عنوان قسم الجداول والعمليات مع رابط التقرير الشامل
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الجداول وقواعد العمليات",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { }
                        ) {
                            Text(
                                text = "عرض التقرير الشامل",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .scale(if (isRtl) -1f else 1f, 1f)
                            )
                        }
                    }

                    // 5. شبكة وحدات وجداول النظام Domain Cards Grid
                    if (isWideScreen) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BuildModuleCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenBuildsClick
                                )
                                PurchaseOrderModuleCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenOrdersClick
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CompanyModuleCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenCompaniesClick
                                )
                                StockModuleCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenStockClick
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PartModuleCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenPartsClick
                                )
                                BomModuleCard(
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenBomClick
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BuildModuleCard(onClick = onOpenBuildsClick)
                            PurchaseOrderModuleCard(onClick = onOpenOrdersClick)
                            CompanyModuleCard(onClick = onOpenCompaniesClick)
                            StockModuleCard(onClick = onOpenStockClick)
                            PartModuleCard(onClick = onOpenPartsClick)
                            BomModuleCard(onClick = onOpenBomClick)
                        }
                    }

                    // 6. تفاصيل الجلسة النشطة Active Session Info Card
                    val session = loginUiState.currentSession
                    if (session != null) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.session_info_header),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(Res.string.session_id, session.id),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(Res.string.session_user, session.username),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(Res.string.session_status, session.authStatus),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(Res.string.session_active),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // مساحة سفلية كافية لتفادي تداخل شريط التنقل السفي
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * ترويسة لوحة التحكم العلوية Top App Header
 */
@Composable
private fun DashboardTopHeader(
    onLogoutClick: () -> Unit,
    hasNotifications: Boolean,
    onNotificationClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4338CA),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "نظام إدارة المخزون",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "منصة التتبع والتصنيع الذكي",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // زر الإشعارات
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "التنبيهات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (hasNotifications) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopStart)
                                .offset(x = 6.dp, y = 6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }

                // زر تسجيل الخروج
                OutlinedButton(
                    onClick = onLogoutClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "خروج",
                            modifier = Modifier
                                .size(16.dp)
                                .scale(if (isRtl) -1f else 1f, 1f)
                        )
                        Text(
                            text = "خروج",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * كارت الترحيب الترويجي الرئيسي Welcome Hero Banner
 * شارة الأمان الحامي على اليمين وشارة "الحساب نشط ومتصل" على اليسار مطابق للتصميم المستهدف.
 */
@Composable
private fun WelcomeHeroBanner(
    username: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3730A3),
                            Color(0xFF4338CA),
                            Color(0xFF4F46E5)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // السطر العلوي: الشارة النابضة على اليمين وأيقونة الأمان الدرع على اليسار
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // أيقونة الدرع المحمي على اليمين حسب التصميم Target
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // شارة الحساب النشط على اليسار
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PulsingGreenIndicator()
                            Text(
                                text = "الحساب نشط ومتصل",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF67F4B7)
                            )
                        }
                    }
                }

                // سطر الترحيب والوصف
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "مرحباً بك مجدداً، $username!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "👋",
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "تم التحقق وتسجيل الدخول بنجاح. جاهز لمتابعة تدفق الإنتاج والمخزون اللحظي.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }
            }
        }
    }
}

/**
 * قسم كروت المؤشرات التشغيلية Operational KPIs Section
 */
@Composable
private fun OperationalKpiSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // KPI 1: أوامر التصنيع
        KpiMetricCard(
            modifier = Modifier.weight(1f),
            title = "أوامر التصنيع",
            value = "14",
            valueColor = Color(0xFF1E293B),
            badgeText = "نشطة",
            badgeBgColor = Color(0xFFFFFBEB),
            badgeTextColor = Color(0xFFD97706)
        )
        // KPI 2: نواقص الرفوف
        KpiMetricCard(
            modifier = Modifier.weight(1f),
            title = "نواقص الرفوف",
            value = "3",
            valueColor = Color(0xFFE11D48),
            badgeText = "تنبيه",
            badgeBgColor = Color(0xFFFFE4E6),
            badgeTextColor = Color(0xFFE11D48)
        )
        // KPI 3: طلبات شراء
        KpiMetricCard(
            modifier = Modifier.weight(1f),
            title = "طلبات شراء",
            value = "8",
            valueColor = Color(0xFF059669),
            badgeText = "توريد",
            badgeBgColor = Color(0xFFECFDF5),
            badgeTextColor = Color(0xFF059669)
        )
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    badgeText: String,
    badgeBgColor: Color,
    badgeTextColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                ),
                color = valueColor
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeBgColor,
                border = BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * كارت إجراء المسح السريع للباركود Barcode / QR Quick Action Card
 */
@Composable
private fun BarcodeScannerActionCard(
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E1B4B)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "مسح الباركود السريع (Barcode / QR)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "مطابقة الفواتير والفحص الفوري للقطع",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Button(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "مسح الآن",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * كارت وحدة العمليات: أوامر التصنيع Build
 */
@Composable
private fun BuildModuleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainModuleCard(
        modifier = modifier,
        title = "جدول أوامر الإنتاج والتصنيع",
        tag = "Build",
        description = "إدارة وتتبع دورة تصنيع المكونات وتحويل قائمة المواد (BOM) إلى منتجات تامة الصنع.",
        actionText = "فتح الإنتاج",
        statText = "9 خطوط تصنيع",
        icon = Icons.Default.PrecisionManufacturing,
        boxBgColor = Color(0xFFFFF1F2),
        borderColor = Color(0xFFFFE4E6),
        accentColor = Color(0xFFE11D48),
        statDotColor = Color(0xFF4F46E5),
        onClick = onClick
    )
}

/**
 * كارت وحدة العمليات: أوامر الشراء PO
 */
@Composable
private fun PurchaseOrderModuleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainModuleCard(
        modifier = modifier,
        title = "جدول أوامر الشراء وبنودها",
        tag = "PO",
        description = "إدارة طلبات التوريد، تسعير البنود، دورة اعتمادات الشراء، ومطابقة استلام الشحنات.",
        actionText = "فتح الطلبات",
        statText = "بانتظار الفحص",
        icon = Icons.Default.Description,
        boxBgColor = Color(0xFFF0F9FF),
        borderColor = Color(0xFFE0F2FE),
        accentColor = Color(0xFF0284C7),
        statDotColor = Color(0xFFF59E0B),
        onClick = onClick
    )
}

/**
 * كارت وحدة العمليات: الشركات والعلاقات Company
 */
@Composable
private fun CompanyModuleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainModuleCard(
        modifier = modifier,
        title = "جدول الشركات والعلاقات",
        tag = "Company",
        description = "سجلات الموردين، المصنعين الخارجيين، جهات الشحن، ومحددات العملات والاتفاقيات.",
        actionText = "فتح الشركات",
        statText = "24 مورد معتمد",
        icon = Icons.Default.CorporateFare,
        boxBgColor = Color(0xFFF8FAFC),
        borderColor = Color(0xFFE2E8F0),
        accentColor = Color(0xFF475569),
        statDotColor = Color(0xFF10B981),
        onClick = onClick
    )
}

/**
 * كارت وحدة العمليات: المخزون الفعلي StockItem
 */
@Composable
private fun StockModuleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainModuleCard(
        modifier = modifier,
        title = "إدارة المخزون الفعلي",
        tag = "StockItem",
        description = "تتبع الوحدات المادية على الأرفف، الأرقام التسلسلية، الشحنات، والمستودعات الفرعية.",
        actionText = "فتح المخزون",
        statText = "98% دقة الجرد",
        icon = Icons.Default.Inventory,
        boxBgColor = Color(0xFFEEF2FF),
        borderColor = Color(0xFFE0E7FF),
        accentColor = Color(0xFF4F46E5),
        statDotColor = Color(0xFF10B981),
        onClick = onClick
    )
}

/**
 * كارت وحدة العمليات: دليل القطع Part
 */
@Composable
private fun PartModuleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainModuleCard(
        modifier = modifier,
        title = "دليل القطع والمكونات",
        tag = "Part (InvenTree)",
        description = "تصفية وبحث وحفظ مواصفات القطع المعيارية وقوائم الـ IPN وخصائص التجميع.",
        actionText = "فتح الجدول",
        statText = "1,480 قطعة مدمجة",
        icon = Icons.Default.SettingsInputComponent,
        boxBgColor = Color(0xFFF0FDFA),
        borderColor = Color(0xFFCCFBF1),
        accentColor = Color(0xFF0D9488),
        statDotColor = Color(0xFF0284C7),
        onClick = onClick
    )
}

/**
 * كارت وحدة العمليات: قائمة المواد BOM
 */
@Composable
private fun BomModuleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DomainModuleCard(
        modifier = modifier,
        title = "جدول بنود قائمة المواد (BOM)",
        tag = "BomItem",
        description = "إدارة العلاقة التركيبية والكميات والشروط الفنية للقطع المجمعة.",
        actionText = "فتح قائمة المواد",
        statText = "246 مكون مجمع",
        icon = Icons.Default.AccountTree,
        boxBgColor = Color(0xFFF3E8FF),
        borderColor = Color(0xFFE9D5FF),
        accentColor = Color(0xFF7C3AED),
        statDotColor = Color(0xFF7C3AED),
        onClick = onClick
    )
}

/**
 * المكون الهيكلي الشامل لكارت الوحدة التشغيلية Domain Module Card
 */
@Composable
private fun DomainModuleCard(
    title: String,
    tag: String,
    description: String,
    actionText: String,
    statText: String,
    icon: ImageVector,
    boxBgColor: Color,
    borderColor: Color,
    accentColor: Color,
    statDotColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // صندوق الأيقونة
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(boxBgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = boxBgColor
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(10.dp))

            // السطر السفلي الخاص ببيانات الإحصائيات ورابط الفتح
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statDotColor)
                    )
                    Text(
                        text = statText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(14.dp)
                            .scale(if (isRtl) -1f else 1f, 1f)
                    )
                }
            }
        }
    }
}

/**
 * نقطة خضراء متهمجة لتأكيد اتصال الحساب النشط
 */
@Composable
private fun PulsingGreenIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.size(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha * 0.5f)
                .clip(CircleShape)
                .background(Color(0xFF10B981))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981))
        )
    }
}
