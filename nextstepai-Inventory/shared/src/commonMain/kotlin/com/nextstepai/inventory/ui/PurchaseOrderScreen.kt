package com.nextstepai.inventory.ui

import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.data.SalesOrder
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.ui.components.CurrencySelectionBottomSheet
import com.nextstepai.inventory.ui.components.CurrencySelectorField
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_order
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.orders_count
import nextstepai_inventory.shared.generated.resources.save

/**
 * شاشة إدارة أوامر الشراء (PurchaseOrder Management Screen).
 * تم تحديثها لتتبع الهيكل القياسي للتطبيق، شريط البحث والباركود المدمج، وشريط التصفية التفاعلي.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderScreen(
    viewModel: PurchaseOrderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            PurchaseOrdersTopBar(
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier.padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setAddOrderDialogOpen(true) },
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_new_order),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "أمر شراء جديد",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Start,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // شريط البحث المطور مع دمج الفلتر الخارجي الجانبي المتناسق
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = "البحث بالرمز المرجعي، المورد، أو الوصف...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح البحث",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    val isFilterActive = uiState.statusFilter != null || uiState.searchQuery.isNotBlank()
                    var isFilterPressed by remember { mutableStateOf(false) }
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isFilterPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    )
                    val buttonBgColor by animateColorAsState(
                        targetValue = if (isFilterActive) Color(0xFFEEF2FF) else Color.White,
                        animationSpec = tween(durationMillis = 250)
                    )
                    val buttonBorderColor by animateColorAsState(
                        targetValue = if (isFilterActive) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(durationMillis = 250)
                    )

                    LaunchedEffect(isFilterPressed) {
                        if (isFilterPressed) {
                            delay(150)
                            isFilterPressed = false
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            isFilterPressed = true
                            viewModel.setFilterBottomSheetOpen(true)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, buttonBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = buttonBgColor,
                            contentColor = if (isFilterActive) Color(0xFF4F46E5) else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .scale(buttonScale)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "فلتر",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "فلتر",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (isFilterActive) Color(0xFF4338CA) else MaterialTheme.colorScheme.onSurface
                            )
                            if (isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.orders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.orders_count, 0, 0),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.orders_count, uiState.orders.size, uiState.orders.size),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(uiState.orders, key = { index, order -> "po-${order.id}-$index" }) { _, order ->
                            PurchaseOrderCard(
                                order = order,
                                onClick = { viewModel.selectOrder(order) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.selectedOrder != null) {
        OrderDetailsBottomSheet(
            order = uiState.selectedOrder!!,
            onIssueOrder = { viewModel.issueOrder(it) },
            onAddLineClick = { viewModel.setAddLineDialogOpen(true) },
            onReceiveLine = { lineId, qty -> viewModel.receiveLineItem(lineId, qty) },
            onDismiss = { viewModel.selectOrder(null) }
        )
    }

    if (uiState.isAddOrderDialogOpen) {
        AddPurchaseOrderBottomSheet(
            suppliers = uiState.suppliers,
            parts = uiState.parts,
            buildOrders = uiState.buildOrders,
            salesOrders = uiState.salesOrders,
            onGenerateShortage = { viewModel.generateShortageLinesForBuild(it) },
            onGenerateSalesOrderShortage = { viewModel.generateSalesOrderShortageLines(it) },
            onGenerateLowStock = { viewModel.generateLowStockLinesForSupplier(it) },
            onDismiss = { viewModel.setAddOrderDialogOpen(false) },
            onConfirm = { ref, supId, desc, targetDate, curr, sourceType, sourceRef, lines ->
                viewModel.addPurchaseOrder(ref, supId, desc, targetDate, curr, sourceType, sourceRef, null, lines)
            }
        )
    }

    if (uiState.isAddLineDialogOpen && uiState.selectedOrder != null) {
        AddPOLineItemBottomSheet(
            parts = uiState.parts,
            onDismiss = { viewModel.setAddLineDialogOpen(false) },
            onConfirm = { partId, qty, price, notes ->
                viewModel.addLineItem(partId, qty, price, notes)
            }
        )
    }

    // نافذة الفلترة التفاعلية بتبويبين
    if (uiState.isFilterBottomSheetOpen) {
        PurchaseOrderFilterBottomSheet(
            suppliers = uiState.suppliers,
            selectedSupplierId = uiState.selectedSupplierId,
            orders = uiState.orders,
            initialStatus = uiState.statusFilter,
            onDismiss = { viewModel.setFilterBottomSheetOpen(false) },
            onReset = { viewModel.resetFilters() },
            onApply = { supplierId, status ->
                viewModel.applyFilters(supplierId, status)
            }
        )
    }
}

/**
 * الترويسة العلوية لشاشة أوامر الشراء Top App Bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseOrdersTopBar(
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.scale(if (isRtl) -1f else 1f, 1f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "جدول أوامر الشراء وبنودها",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "متابعة الموردين، اعتمادات توريد الشراء، وتأكيد الاستلام",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            var isSalesOrderDialogOpen by remember { mutableStateOf(false) }

            Surface(
                onClick = { isSalesOrderDialogOpen = true },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEEF2FF),
                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "فتح شاشة المبيعات",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "المبيعات",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                        color = Color(0xFF4F46E5)
                    )
                }
            }

            if (isSalesOrderDialogOpen) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { isSalesOrderDialogOpen = false },
                    sheetState = sheetState,
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
                        SalesOrderScreen(
                            onNavigateBack = { isSalesOrderDialogOpen = false }
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة امر الشراء التفاعلية المطورة
 */
@Composable
private fun PurchaseOrderCard(
    order: PurchaseOrder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // السطر العلوي: الكود المرجعي والشارة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // اليسار: شارة الحالة
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (order.status) {
                        POStatus.COMPLETE -> Color(0xFFECFDF5)
                        POStatus.PLACED -> Color(0xFFEEF2FF)
                        POStatus.CANCELLED -> Color(0xFFFEF2F2)
                        else -> Color(0xFFFFFBEB)
                    },
                    border = BorderStroke(1.dp, when (order.status) {
                        POStatus.COMPLETE -> Color(0xFFA7F3D0)
                        POStatus.PLACED -> Color(0xFFC7D2FE)
                        POStatus.CANCELLED -> Color(0xFFFCA5A5)
                        else -> Color(0xFFFDE68A)
                    })
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusDotColor = when (order.status) {
                            POStatus.COMPLETE -> Color(0xFF059669)
                            POStatus.PLACED -> Color(0xFF4F46E5)
                            POStatus.CANCELLED -> Color(0xFFDC2626)
                            else -> Color(0xFFD97706)
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Text(
                            text = order.status.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = when (order.status) {
                                POStatus.COMPLETE -> Color(0xFF059669)
                                POStatus.PLACED -> Color(0xFF4F46E5)
                                POStatus.CANCELLED -> Color(0xFFDC2626)
                                else -> Color(0xFFB45309)
                            }
                        )
                    }
                }

                // اليمين: الرمز المرجعي ومربع أيقونة التوريد
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEEF2FF)
                            ) {
                                Text(
                                    text = "PO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            val (sourceTagText, sourceTagBg, sourceTagClr) = when (order.sourceType) {
                                "BUILD_ORDER" -> Triple("⚙️ تغذية إنتاج", Color(0xFFEEF2FF), Color(0xFF3730A3))
                                "LOW_STOCK" -> Triple("📦 تموين دوري للنواقص", Color(0xFFFEF3C7), Color(0xFF92400E))
                                else -> Triple("🛒 شراء يدوي", Color(0xFFF1F5F9), Color(0xFF475569))
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = sourceTagBg
                            ) {
                                Text(
                                    text = sourceTagText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = sourceTagClr,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = order.reference,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // اسم المورد
            Text(
                text = order.supplierName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = Color(0xFF0F172A)
            )

            if (order.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = order.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // التكلفة الإجمالية وعدد البنود
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "التكلفة الإجمالية للطلب:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "${order.totalCost} ${order.orderCurrency}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                            color = Color(0xFF4F46E5)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "عدد البنود المسجلة:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "${order.lineItems.size} بنود",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDetailsBottomSheet(
    order: PurchaseOrder,
    onIssueOrder: (orderId: Long) -> Unit,
    onAddLineClick: () -> Unit,
    onReceiveLine: (lineId: Long, qty: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "أمر شراء: ${order.reference}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = order.supplierName,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF4F46E5)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (order.status == POStatus.PENDING) {
                        Button(
                            onClick = { onIssueOrder(order.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                        ) {
                            Text("اعتماد وإصدار ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("رقم الطلب:", "#${order.id}")
                DetailRow("المورد:", order.supplierName)
                DetailRow("الحالة الحالية:", order.status.label)
                DetailRow("إجمالي التكلفة:", "${order.totalCost} ${order.orderCurrency}")
                DetailRow("تاريخ التسليم المستهدف:", order.targetDate.ifBlank { "-" })

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بنود أمر الشراء (${order.lineItems.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                    if (order.status == POStatus.PENDING) {
                        OutlinedButton(
                            onClick = onAddLineClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF4F46E5))
                        ) {
                            Text("+ إضافة بند", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                        }
                    }
                }

                if (order.lineItems.isEmpty()) {
                    Text("لا توجد بنود مضافة لهذا الطلب بعد.", fontSize = 12.sp, color = Color(0xFF64748B))
                } else {
                    for (line in order.lineItems) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = line.partName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text(text = "السعر: ${line.purchasePrice} ${order.orderCurrency}", fontSize = 12.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المطلوب: ${line.quantity} | المستلم: ${line.receivedQuantity}",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B)
                                    )

                                    if (order.status == POStatus.PLACED && line.receivedQuantity < line.quantity) {
                                        Button(
                                            onClick = { onReceiveLine(line.id, line.quantity - line.receivedQuantity) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669), contentColor = Color.White)
                                        ) {
                                            Text("استلام الكامل", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPurchaseOrderBottomSheet(
    suppliers: List<Company>,
    parts: List<Part>,
    buildOrders: List<BuildOrder> = emptyList(),
    salesOrders: List<SalesOrder> = emptyList(),
    onGenerateShortage: (buildId: Long) -> List<PurchaseOrderLineItem> = { emptyList() },
    onGenerateSalesOrderShortage: (salesOrderId: Long) -> List<PurchaseOrderLineItem> = { emptyList() },
    onGenerateLowStock: (supplierId: Long?) -> List<PurchaseOrderLineItem> = { emptyList() },
    onDismiss: () -> Unit,
    onConfirm: (reference: String, supplierId: Long, description: String, targetDate: String, currency: String, sourceType: String, sourceReferenceUuid: String?, lines: List<PurchaseOrderLineItem>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var reference by remember { mutableStateOf("PO-2025-001") }
    var selectedSupplierId by remember { mutableStateOf<Long?>(suppliers.firstOrNull()?.id) }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("2025-03-30") }
    var currency by remember { mutableStateOf("USD") }

    var sourceType by remember { mutableStateOf("MANUAL") }
    var selectedBuildOrderId by remember { mutableStateOf<Long?>(null) }
    var selectedSalesOrderId by remember { mutableStateOf<Long?>(null) }

    var isSelectSupplierSheetOpen by remember { mutableStateOf(false) }
    var isSelectPartSheetOpen by remember { mutableStateOf(false) }

    // تصفية القطع المتاحة للشراء وفق المورد المختار
    val filteredParts = remember(parts, selectedSupplierId) {
        val purchaseable = parts.filter { it.purchaseable }
        if (selectedSupplierId != null) {
            val supplierMatches = purchaseable.filter { it.defaultSupplierId == selectedSupplierId }
            if (supplierMatches.isNotEmpty()) supplierMatches else purchaseable
        } else {
            purchaseable
        }
    }

    // بنود أمر الشراء المضافة مبدئياً
    val initialLines = remember { mutableStateListOf<PurchaseOrderLineItem>() }

    // حقول إضافة بند جديد داخل الحوار
    var itemPartId by remember { mutableStateOf<Long?>(filteredParts.firstOrNull()?.id) }
    var itemQuantityText by remember { mutableStateOf("100.0") }
    var itemPriceText by remember { mutableStateOf("5.0") }

    // جلب السعر التلقائي الافتراضي للقطعة فور اختيارها
    LaunchedEffect(itemPartId) {
        val selectedPart = parts.find { it.id == itemPartId }
        if (selectedPart != null) {
            val autoPrice = if (selectedPart.minimumStock > 0) selectedPart.minimumStock * 0.05 else 5.0
            itemPriceText = autoPrice.formatMoney()
        }
    }

    val totalCost = remember(initialLines.toList()) { initialLines.sumOf { it.quantity * it.purchasePrice } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "إنشاء أمر شراء جديد",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("الرمز المرجعي لأمر الشراء (Reference) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Text("نوع وهدف مصدر الشراء (Source Scenario) *:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))

                var isSelectScenarioSheetOpen by remember { mutableStateOf(false) }

                val (scenarioLabel, scenarioDesc, scenarioIcon) = when (sourceType) {
                    "BUILD_ORDER" -> Triple("⚙️ عجز إنتاج (Build Shortage)", "تغطية نواقص خامات أمر تصنيع وبناء مجمع", Icons.Default.PrecisionManufacturing)
                    "LOW_STOCK" -> Triple("📦 تموين دوري (Low Stock)", "استيراد تلقائي للقطع تحت الحد الأدنى للمخزون", Icons.Default.Inventory)
                    "SALES_ORDER" -> Triple("📋 طلب عميل (Sales Order)", "تغطية بضائع ونواقص طلب عميل مباشر (Back to Back)", Icons.Default.ShoppingCart)
                    else -> Triple("🛒 شراء يدوي مباشر (Manual PO)", "إدخال واختيار حُر المكونات والقطع والأسعار", Icons.Default.AddShoppingCart)
                }

                Surface(
                    onClick = { isSelectScenarioSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.5.dp, Color(0xFF4F46E5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEEF2FF))
                                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = scenarioIcon,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = scenarioLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = scenarioDesc,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = "تغيير",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (isSelectScenarioSheetOpen) {
                    SelectSourceScenarioBottomSheet(
                        currentScenario = sourceType,
                        onDismiss = { isSelectScenarioSheetOpen = false },
                        onSelectScenario = { newScenario ->
                            sourceType = newScenario
                            when (newScenario) {
                                "LOW_STOCK" -> {
                                    initialLines.clear()
                                    initialLines.addAll(onGenerateLowStock(selectedSupplierId))
                                }
                                "BUILD_ORDER" -> {
                                    val firstBo = buildOrders.firstOrNull()
                                    if (firstBo != null) {
                                        selectedBuildOrderId = firstBo.id
                                        initialLines.clear()
                                        initialLines.addAll(onGenerateShortage(firstBo.id))
                                    }
                                }
                                "SALES_ORDER" -> {
                                    val firstSo = salesOrders.firstOrNull()
                                    if (firstSo != null) {
                                        selectedSalesOrderId = firstSo.id
                                        initialLines.clear()
                                        initialLines.addAll(onGenerateSalesOrderShortage(firstSo.id))
                                    }
                                }
                                else -> {
                                    initialLines.clear()
                                }
                            }
                            isSelectScenarioSheetOpen = false
                        }
                    )
                }

                if (sourceType == "BUILD_ORDER") {
                    val selectedBuildOrder = buildOrders.find { it.id == selectedBuildOrderId }
                    var isSelectBuildSheetOpen by remember { mutableStateOf(false) }

                    Text("أمر الإنتاج المستهدف لتغطية العجز (Build Order) *:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))

                    Surface(
                        onClick = { isSelectBuildSheetOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (selectedBuildOrder != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEEF2FF))
                                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PrecisionManufacturing,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = selectedBuildOrder?.let { "${it.reference} - ${it.partName}" } ?: "اختر أمر الإنتاج المراد تغطية عجزه *",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (selectedBuildOrder != null) "الكمية المطلوبة: ${selectedBuildOrder.quantity} وحدة | التشغيلة: ${selectedBuildOrder.batch.ifBlank { "-" }}" else "اضغط للبحث واختيار أمر الإنتاج لتوريد عجزه",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = if (selectedBuildOrder != null) "تغيير" else "اختر ▾",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (isSelectBuildSheetOpen) {
                        SelectBuildOrderForPOBottomSheet(
                            buildOrders = buildOrders,
                            selectedBuildOrderId = selectedBuildOrderId,
                            onDismiss = { isSelectBuildSheetOpen = false },
                            onSelect = { bo ->
                                if (bo != null) {
                                    selectedBuildOrderId = bo.id
                                    val generated = onGenerateShortage(bo.id)
                                    initialLines.clear()
                                    initialLines.addAll(generated)
                                }
                                isSelectBuildSheetOpen = false
                            }
                        )
                    }
                }

                if (sourceType == "SALES_ORDER") {
                    val selectedSalesOrder = salesOrders.find { it.id == selectedSalesOrderId }
                    var isSelectSalesSheetOpen by remember { mutableStateOf(false) }

                    Text("أمر بيع العميل المستهدف لتغطية طلباته (Sales Order) *:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))

                    Surface(
                        onClick = { isSelectSalesSheetOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (selectedSalesOrder != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEEF2FF))
                                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = selectedSalesOrder?.let { "${it.reference} - ${it.customerName}" } ?: "اختر أمر بيع العميل المراد التوريد له *",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (selectedSalesOrder != null) "التسليم المتوقع: ${selectedSalesOrder.targetDate.ifBlank { "-" }} | الإجمالي: ${selectedSalesOrder.orderCurrency} ${selectedSalesOrder.totalPrice.formatMoney()}" else "اضغط للبحث واختيار أمر بيع العميل لتوريد بنوده",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = if (selectedSalesOrder != null) "تغيير" else "اختر ▾",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (isSelectSalesSheetOpen) {
                        SelectSalesOrderForPOBottomSheet(
                            salesOrders = salesOrders,
                            selectedSalesOrderId = selectedSalesOrderId,
                            onDismiss = { isSelectSalesSheetOpen = false },
                            onSelect = { so ->
                                if (so != null) {
                                    selectedSalesOrderId = so.id
                                    val generated = onGenerateSalesOrderShortage(so.id)
                                    initialLines.clear()
                                    initialLines.addAll(generated)
                                }
                                isSelectSalesSheetOpen = false
                            }
                        )
                    }
                }

                val selectedSupplier = suppliers.find { it.id == selectedSupplierId }
                var isSelectSupplierSheetOpen by remember { mutableStateOf(false) }

                Text("اختر المورد المعتمد لأمر الشراء *:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))

                Surface(
                    onClick = { isSelectSupplierSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.5.dp, if (selectedSupplier != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEEF2FF))
                                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = selectedSupplier?.name ?: "اختر المورد لأمر الشراء *",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (selectedSupplier != null) "العملة المعتمدة: ${selectedSupplier.currency} | رقم المورد: #${selectedSupplier.id}" else "اضغط للبحث واختيار المورد المعتمد",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = if (selectedSupplier != null) "تغيير" else "اختر ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الطلب وملاحظاته") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                var showTargetDatePicker by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("تاريخ التسليم المتوقع *") },
                    trailingIcon = {
                        IconButton(onClick = { showTargetDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "اختيار التاريخ من التقويم",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTargetDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (showTargetDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showTargetDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val instant = Instant.fromEpochMilliseconds(millis)
                                    val dateTime = instant.toLocalDateTime(TimeZone.UTC)
                                    val year = dateTime.year
                                    val month = dateTime.monthNumber.toString().padStart(2, '0')
                                    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
                                    targetDate = "$year-$month-$day"
                                }
                                showTargetDatePicker = false
                            }) {
                                Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTargetDatePicker = false }) {
                                Text("إلغاء")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                var isCurrencyPickerOpen by remember { mutableStateOf(false) }
                CurrencySelectorField(
                    selectedCurrencyCode = currency,
                    onOpenPicker = { isCurrencyPickerOpen = true },
                    label = "العملة المعتمدة للطلب"
                )
                if (isCurrencyPickerOpen) {
                    CurrencySelectionBottomSheet(
                        selectedCurrencyCode = currency,
                        onDismiss = { isCurrencyPickerOpen = false },
                        onCurrencySelected = { selectedCurr ->
                            currency = selectedCurr.code
                        }
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // قسم إضافة البنود والمواد مباشرة داخل الحوار
                Text(
                    text = "بنود ومكونات أمر الشراء (Purchase Order Line Items):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = Color(0xFF0F172A)
                )

                // قائمة البنود المضافة ملخصة بحساب الإجمالي لكل بند وإجمالي الطلب (تظهر بارزة فورياً)
                if (initialLines.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قائمة البنود المضافة (${initialLines.size}):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF065F46))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFD1FAE5)
                                ) {
                                    Text("التكلفة الإجمالية: $currency ${totalCost.formatMoney()}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF047857), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFA7F3D0))

                            initialLines.forEachIndexed { idx, line ->
                                val lineTotal = line.quantity * line.purchasePrice
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${idx + 1}. ${line.partName}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                                            Text("${line.quantity} وحدة × $currency ${line.purchasePrice.formatMoney()} = $currency ${lineTotal.formatMoney()}", fontSize = 11.sp, color = Color(0xFF475569))
                                        }

                                        IconButton(
                                            onClick = { initialLines.removeAt(idx) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف البند", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إضافة بند إضافي لأمر الشراء:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF4F46E5))

                        val selectedPart = parts.find { it.id == itemPartId }
                        var isSelectPartSheetOpen by remember { mutableStateOf(false) }

                        Text("اختر القطعة/المكون المتاح للشراء *:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)

                        Surface(
                            onClick = { isSelectPartSheetOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, if (selectedPart != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFEEF2FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Extension,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = selectedPart?.name ?: "اختر المكون المراد إضافته *",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (selectedPart != null) "IPN: ${selectedPart.ipn.ifBlank { "-" }} | المخزون المتوفر: ${selectedPart.totalInStock} ${selectedPart.units}" else "اضغط للبحث واختيار المكون المتاح",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFFEEF2FF),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                ) {
                                    Text(
                                        text = if (selectedPart != null) "تغيير" else "اختر ▾",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF4F46E5),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = itemQuantityText,
                                onValueChange = { itemQuantityText = it },
                                label = { Text("الكمية المطلوبة *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            OutlinedTextField(
                                value = itemPriceText,
                                onValueChange = { itemPriceText = it },
                                label = { Text("سعر الشراء ($currency) *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val selectedPart = parts.find { it.id == itemPartId }
                                val q = itemQuantityText.toDoubleOrNull() ?: 1.0
                                val p = itemPriceText.toDoubleOrNull() ?: 0.0
                                if (selectedPart != null && q > 0) {
                                    initialLines.add(
                                        PurchaseOrderLineItem(
                                            orderId = 0L,
                                            supplierPartId = selectedPart.id,
                                            partName = selectedPart.name,
                                            quantity = q,
                                            purchasePrice = p
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("+ إضافة المادة إلى قائمة أمر الشراء", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val supId = selectedSupplierId
                            if (reference.isNotBlank() && supId != null) {
                                val srcRef = if (sourceType == "BUILD_ORDER") selectedBuildOrderId?.let { "build-$it" } else if (sourceType == "SALES_ORDER") selectedSalesOrderId?.let { "so-$it" } else null
                                onConfirm(reference, supId, description, targetDate, currency, sourceType, srcRef, initialLines.toList())
                            }
                        },
                        enabled = reference.isNotBlank() && selectedSupplierId != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (isSelectSupplierSheetOpen) {
        SelectSupplierBottomSheet(
            suppliers = suppliers,
            selectedSupplierId = selectedSupplierId,
            onDismiss = { isSelectSupplierSheetOpen = false },
            onSelect = { sup ->
                if (sup != null) {
                    selectedSupplierId = sup.id
                    currency = sup.currency
                }
                isSelectSupplierSheetOpen = false
            }
        )
    }

    if (isSelectPartSheetOpen) {
        SelectPartForPOBottomSheet(
            parts = filteredParts,
            selectedPartId = itemPartId,
            onDismiss = { isSelectPartSheetOpen = false },
            onSelect = { pt ->
                if (pt != null) {
                    itemPartId = pt.id
                }
                isSelectPartSheetOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSourceScenarioBottomSheet(
    currentScenario: String,
    onDismiss: () -> Unit,
    onSelectScenario: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scenarios = listOf(
        Quadruple("MANUAL", "🛒 شراء يدوي مباشر (Manual PO)", "إدخال واختيار حُر المكونات والقطع والأسعار بدون قيود.", Icons.Default.AddShoppingCart),
        Quadruple("BUILD_ORDER", "⚙️ عجز إنتاج (Build Shortage)", "ربط بأمر تصنيع واستيراد مكونات الخامات الناقصة تلقائياً.", Icons.Default.PrecisionManufacturing),
        Quadruple("LOW_STOCK", "📦 تموين دوري (Low Stock)", "استيراد وحساب القطع تحت الحد الأدنى للمخزون المعتمد.", Icons.Default.Inventory),
        Quadruple("SALES_ORDER", "📋 طلب عميل (Sales Order Fulfillment)", "ربط بأمر بيع عميل واستيراد نواقصه للتوريد المباشر.", Icons.Default.ShoppingCart)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اختر نوع وهدف مصدر الشراء (Scenario)", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scenarios.forEach { (code, title, desc, icon) ->
                    val isSel = currentScenario == code
                    Surface(
                        onClick = { onSelectScenario(code) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (isSel) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) Color(0xFF4F46E5) else Color(0xFFE0E7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = if (isSel) Color.White else Color(0xFF4F46E5), modifier = Modifier.size(22.dp))
                                }

                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color(0xFF0F172A))
                                    Text(desc, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                }
                            }

                            if (isSel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSupplierBottomSheet(
    suppliers: List<Company>,
    selectedSupplierId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Company?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.currency.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اختر المورد المعتمد لأمر الشراء", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المورد أو العملة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSuppliers, key = { "po-sup-${it.id}" }) { sup ->
                    val isSel = selectedSupplierId == sup.id
                    Surface(
                        onClick = { onSelect(sup) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("العملة: ${sup.currency} | رقم المورد: #${sup.id}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                            if (isSel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectPartForPOBottomSheet(
    parts: List<Part>,
    selectedPartId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Part?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredParts = remember(parts, searchQuery) {
        if (searchQuery.isBlank()) parts
        else parts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.ipn.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اختر المكون المتاح لأمر الشراء", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم القطعة أو IPN...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredParts, key = { "po-pt-${it.id}" }) { pt ->
                    val isSel = selectedPartId == pt.id
                    Surface(
                        onClick = { onSelect(pt) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(pt.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("IPN: ${pt.ipn.ifBlank { "-" }} | المخزون المتوفر: ${pt.totalInStock} ${pt.units}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                            if (isSel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectBuildOrderForPOBottomSheet(
    buildOrders: List<BuildOrder>,
    selectedBuildOrderId: Long?,
    onDismiss: () -> Unit,
    onSelect: (BuildOrder?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredBuilds = remember(buildOrders, searchQuery) {
        if (searchQuery.isBlank()) buildOrders
        else buildOrders.filter { it.reference.contains(searchQuery, ignoreCase = true) || it.partName.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اختر أمر الإنتاج لتغطية العجز", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث برمز أمر البناء أو اسم المنتج...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredBuilds, key = { "po-bo-${it.id}" }) { bo ->
                    val isSel = selectedBuildOrderId == bo.id
                    Surface(
                        onClick = { onSelect(bo) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${bo.reference} - ${bo.partName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("الكمية المطلوبة: ${bo.quantity} وحدة | التشغيلة: ${bo.batch.ifBlank { "-" }}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                            if (isSel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSalesOrderForPOBottomSheet(
    salesOrders: List<SalesOrder>,
    selectedSalesOrderId: Long?,
    onDismiss: () -> Unit,
    onSelect: (SalesOrder?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredSales = remember(salesOrders, searchQuery) {
        if (searchQuery.isBlank()) salesOrders
        else salesOrders.filter { it.reference.contains(searchQuery, ignoreCase = true) || it.customerName.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اختر أمر بيع العميل المراد التوريد له", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث برمز أمر البيع أو اسم العميل...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSales, key = { "po-so-${it.id}" }) { so ->
                    val isSel = selectedSalesOrderId == so.id
                    Surface(
                        onClick = { onSelect(so) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${so.reference} - ${so.customerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("تاريخ التسليم: ${so.targetDate.ifBlank { "-" }} | الإجمالي: ${so.orderCurrency} ${so.totalPrice.formatMoney()}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                            if (isSel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPOLineItemBottomSheet(
    parts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, quantity: Double, price: Double, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPartId by remember { mutableStateOf<Long?>(parts.firstOrNull()?.id) }
    var quantityText by remember { mutableStateOf("10.0") }
    var priceText by remember { mutableStateOf("5.0") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBox,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "إضافة بند شراء جديد",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("اختر القطعة/المكون المطلوب شراءه *:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(parts, key = { "po-line-part-${it.id}" }) { part ->
                        val isSelected = selectedPartId == part.id
                        Surface(
                            onClick = { selectedPartId = part.id },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = part.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية المطلوبة") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("سعر الشراء الفردي") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات وشروط خاصة بالبند") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val pId = selectedPartId
                            val qty = quantityText.toDoubleOrNull() ?: 1.0
                            val prc = priceText.toDoubleOrNull() ?: 0.0
                            if (pId != null && qty > 0.0) {
                                onConfirm(pId, qty, prc, notes)
                            }
                        },
                        enabled = selectedPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseOrderFilterBottomSheet(
    suppliers: List<Company>,
    selectedSupplierId: Long?,
    orders: List<PurchaseOrder>,
    initialStatus: POStatus?,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (supplierId: Long?, status: POStatus?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSupplierIdState by remember { mutableStateOf(selectedSupplierId) }
    var selectedStatusState by remember { mutableStateOf(initialStatus) }

    val calculatedCount = remember(
        orders, searchQuery, selectedSupplierIdState, selectedStatusState
    ) {
        val query = searchQuery.trim()
        orders.count { order ->
            val matchesSupplier = selectedSupplierIdState == null || order.supplierId == selectedSupplierIdState
            val matchesStatus = selectedStatusState == null || order.status == selectedStatusState
            val matchesQuery = query.isBlank() ||
                    order.reference.contains(query, ignoreCase = true) ||
                    order.supplierName.contains(query, ignoreCase = true) ||
                    order.description.contains(query, ignoreCase = true)

            matchesSupplier && matchesStatus && matchesQuery
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "تصفية أوامر الشراء وبنودها",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تحديد المورد المعتمد وحالة الاعتماد والتوريد",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = {
                        searchQuery = ""
                        selectedSupplierIdState = null
                        selectedStatusState = null
                        onReset()
                    }) {
                        Text(
                            text = "مسح الكل",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Body Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. حقل البحث الرئيسي
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن اسم المورد، الرمز المرجعي، أو الوصف...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // 2. شريط التبويبات المزدوجة
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { selectedTab = 0 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🏢 المورد المعتمد (${suppliers.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == 0) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = 1 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 1) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚙️ حالة التوريد والاعتماد",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // 3. محتوى التبويب المختار
                val filteredSuppliers = remember(suppliers, searchQuery) {
                    if (searchQuery.isBlank()) suppliers
                    else suppliers.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (selectedTab == 0) {
                    // تبويب الشركات الموردة
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isAllSelected = selectedSupplierIdState == null
                        Surface(
                            onClick = { selectedSupplierIdState = null },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAllSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isAllSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "جميع الموردين المعتمدين",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                if (isAllSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        filteredSuppliers.forEach { supplier ->
                            val isSelected = selectedSupplierIdState == supplier.id
                            Surface(
                                onClick = { selectedSupplierIdState = supplier.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = supplier.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        if (supplier.description.isNotBlank()) {
                                            Text(
                                                text = supplier.description,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // تبويب حالة التوريد والاعتماد
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statuses = listOf(
                                Triple(null, "جميع الحالات", "عرض كافة أوامر الشراء دون تصفية"),
                                Triple(POStatus.PENDING, "مسودة (PENDING)", "أوامر شحنات منشأة قيد المراجعة"),
                                Triple(POStatus.PLACED, "معتمد ومُصدر (PLACED)", "أوامر صادرة ومؤكدة مع المورد"),
                                Triple(POStatus.COMPLETE, "مكتمل ومستلم (COMPLETE)", "أوامر تم توريدها بالكامل وإغلاقها"),
                                Triple(POStatus.CANCELLED, "ملغى (CANCELLED)", "أوامر شراء ملغاة")
                            )

                            statuses.forEach { (st, label, desc) ->
                                val isSelected = selectedStatusState == st
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedStatusState = st }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedStatusState = st },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                                    )
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer Section
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onApply(selectedSupplierIdState, selectedStatusState)
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text(
                            text = "تطبيق التصفية (عرض $calculatedCount أمر)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
