package com.nextstepai.inventory.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.ui.theme.AppIcons
import org.jetbrains.compose.resources.stringResource
import nextstepai_inventory.shared.generated.resources.*

private enum class ManagementSubView {
    MAIN_DASHBOARD,
    COMPANIES,
    PURCHASE_ORDERS
}

/**
 * شاشة الإدارة الرئيسية: تعرض صفحة هبوط ذات بطاقتين تفاعليتين
 * تتيحان الانتقال المستقل إلى (قسم الشركات والعلاقات) أو (قسم أوامر الشراء وبنودها).
 */
@Composable
fun ManagementScreen(
    companyViewModel: CompanyViewModel,
    purchaseOrderViewModel: PurchaseOrderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentSubView by remember { mutableStateOf(ManagementSubView.MAIN_DASHBOARD) }

    CommonBackHandler(enabled = currentSubView != ManagementSubView.MAIN_DASHBOARD) {
        currentSubView = ManagementSubView.MAIN_DASHBOARD
    }

    val companyUiState by companyViewModel.uiState.collectAsState()
    val poUiState by purchaseOrderViewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = currentSubView,
            label = "ManagementSubViewTransition",
        ) { subView ->
            when (subView) {
                ManagementSubView.MAIN_DASHBOARD -> {
                    ManagementMainDashboard(
                        companiesCount = companyUiState.companies.size,
                        suppliersCount = companyUiState.totalSuppliersCount,
                        ordersCount = poUiState.orders.size,
                        activeOrdersCount = poUiState.orders.count { (it.status == POStatus.PLACED || it.status == POStatus.PENDING) },
                        onOpenCompaniesClick = { currentSubView = ManagementSubView.COMPANIES },
                        onOpenOrdersClick = { currentSubView = ManagementSubView.PURCHASE_ORDERS },
                    )
                }
                ManagementSubView.COMPANIES -> {
                    CompanyScreen(
                        viewModel = companyViewModel,
                        onBackClick = {
                            if (currentSubView != ManagementSubView.MAIN_DASHBOARD) {
                                currentSubView = ManagementSubView.MAIN_DASHBOARD
                            } else {
                                onBackClick()
                            }
                        },
                    )
                }
                ManagementSubView.PURCHASE_ORDERS -> {
                    PurchaseOrderScreen(
                        viewModel = purchaseOrderViewModel,
                        onBackClick = {
                            if (currentSubView != ManagementSubView.MAIN_DASHBOARD) {
                                currentSubView = ManagementSubView.MAIN_DASHBOARD
                            } else {
                                onBackClick()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagementMainDashboard(
    companiesCount: Int,
    suppliersCount: Int,
    ordersCount: Int,
    activeOrdersCount: Int,
    onOpenCompaniesClick: () -> Unit,
    onOpenOrdersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // 1. هيدر عنوان قسم الإدارة
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Management,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.nav_management),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "مركز إدارة الموردين، المصنعين والعلاقات اللوجستية وأوامر الشراء",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = "أقسام الإدارة الرئيسية:",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        // 2. بطاقة قسم الشركات والعلاقات (Companies Card)
        ElevatedCard(
            onClick = onOpenCompaniesClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.Companies,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.card_companies_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.card_companies_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("🏢 $companiesCount شركة") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFEEF2FF),
                                labelColor = Color(0xFF3730A3),
                            ),
                            border = null,
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("🚛 $suppliersCount مورد") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155),
                            ),
                            border = null,
                        )
                    }

                    Button(
                        onClick = onOpenCompaniesClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.open_companies),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }

        // 3. بطاقة قسم أوامر الشراء (Purchase Orders Card)
        ElevatedCard(
            onClick = onOpenOrdersClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.Orders,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.card_orders_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.card_orders_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("🛒 $ordersCount أمر شراء") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFECFDF5),
                                labelColor = Color(0xFF065F46),
                            ),
                            border = null,
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("🚚 $activeOrdersCount نشط") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155),
                            ),
                            border = null,
                        )
                    }

                    Button(
                        onClick = onOpenOrdersClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.open_orders),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
