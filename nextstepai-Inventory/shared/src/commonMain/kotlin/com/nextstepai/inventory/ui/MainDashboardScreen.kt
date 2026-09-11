package com.nextstepai.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.ui.theme.AppIcons
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.card_bom_subtitle
import nextstepai_inventory.shared.generated.resources.card_bom_title
import nextstepai_inventory.shared.generated.resources.card_builds_subtitle
import nextstepai_inventory.shared.generated.resources.card_builds_title
import nextstepai_inventory.shared.generated.resources.card_companies_subtitle
import nextstepai_inventory.shared.generated.resources.card_companies_title
import nextstepai_inventory.shared.generated.resources.card_orders_subtitle
import nextstepai_inventory.shared.generated.resources.card_orders_title
import nextstepai_inventory.shared.generated.resources.card_parts_subtitle
import nextstepai_inventory.shared.generated.resources.card_parts_title
import nextstepai_inventory.shared.generated.resources.card_stock_subtitle
import nextstepai_inventory.shared.generated.resources.card_stock_title
import nextstepai_inventory.shared.generated.resources.dashboard_title
import nextstepai_inventory.shared.generated.resources.login_success_subtitle
import nextstepai_inventory.shared.generated.resources.logout
import nextstepai_inventory.shared.generated.resources.open_bom
import nextstepai_inventory.shared.generated.resources.open_builds
import nextstepai_inventory.shared.generated.resources.open_companies
import nextstepai_inventory.shared.generated.resources.open_orders
import nextstepai_inventory.shared.generated.resources.open_parts
import nextstepai_inventory.shared.generated.resources.open_stock
import nextstepai_inventory.shared.generated.resources.session_active
import nextstepai_inventory.shared.generated.resources.session_id
import nextstepai_inventory.shared.generated.resources.session_info_header
import nextstepai_inventory.shared.generated.resources.session_status
import nextstepai_inventory.shared.generated.resources.session_user
import nextstepai_inventory.shared.generated.resources.welcome_message

/**
 * الشاشة الرئيسية للتطبيق التي تنتقل إليها الواجهة فور تسجيل الدخول بنجاح.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.dashboard_title)) },
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Icon(
                            painter = painterResource(AppIcons.Logout),
                            contentDescription = stringResource(Res.string.logout),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            stringResource(Res.string.logout),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(Res.string.welcome_message),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.login_success_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (this@BoxWithConstraints.maxWidth > 600.dp) 2 else 1
                ) {
                    // زر الانتقال إلى جدول أوامر الإنتاج والتصنيع
                    ElevatedCard(
                        onClick = onOpenBuildsClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.card_builds_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.card_builds_subtitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = stringResource(Res.string.open_builds),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // زر الانتقال إلى جدول أوامر الشراء
                    ElevatedCard(
                        onClick = onOpenOrdersClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.card_orders_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.card_orders_subtitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = stringResource(Res.string.open_orders),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (this@BoxWithConstraints.maxWidth > 600.dp) 2 else 1
                ) {
                    // زر الانتقال إلى جدول الشركات
                    ElevatedCard(
                        onClick = onOpenCompaniesClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.card_companies_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.card_companies_subtitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = stringResource(Res.string.open_companies),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // زر الانتقال إلى المخزون الفعلي
                    ElevatedCard(
                        onClick = onOpenStockClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.card_stock_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.card_stock_subtitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = stringResource(Res.string.open_stock),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (this@BoxWithConstraints.maxWidth > 600.dp) 2 else 1
                ) {
                    // زر الانتقال إلى جدول القطع
                    ElevatedCard(
                        onClick = onOpenPartsClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.card_parts_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.card_parts_subtitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = stringResource(Res.string.open_parts),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // زر الانتقال إلى BOM
                    ElevatedCard(
                        onClick = onOpenBomClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.card_bom_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.card_bom_subtitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = stringResource(Res.string.open_bom),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // تفاصيل بيانات سجل الجلسة
                val session = loginUiState.currentSession
                if (session != null) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(Res.string.session_info_header),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = stringResource(Res.string.session_id, session.id), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = stringResource(Res.string.session_user, session.username), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = stringResource(Res.string.session_status, session.authStatus), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = stringResource(Res.string.session_active), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
