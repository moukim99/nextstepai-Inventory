package com.nextstepai.inventory.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.BuildItem
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockLocation
import nextstepai_inventory.shared.generated.resources.*

/**
 * شاشة أوامر التصنيع والإنتاج (BuildOrder Screen).
 * مطابقة تماماً للتصميم المستهدف (Target Design) مع الترتيب الصحيح للترويسة، مدخل البحث والباركود، الكروت والتقدم.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildOrderScreen(
    viewModel: BuildOrderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            BuildOrdersTopBar(
                onBackClick = onBackClick,
                onAddClick = { viewModel.setAddDialogOpen(true) }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier.padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setAddDialogOpen(true) },
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_new_build),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إنشاء أمر تصنيع",
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
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = stringResource(Res.string.search_build_hint),
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
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
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
                            if (isFilterActive) {
                                viewModel.setStatusFilter(null)
                                viewModel.onSearchQueryChanged("")
                            }
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

                Spacer(modifier = Modifier.height(12.dp))

                // شريط تصفية حالات أوامر الإنتاج Filter Chips Tray
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "build-status-all") {
                        val isAllSelected = uiState.statusFilter == null
                        Surface(
                            onClick = { viewModel.setStatusFilter(null) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAllSelected) Color(0xFF4F46E5) else Color.White,
                            border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = if (isAllSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "كافة الحالات",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isAllSelected) Color(0xFF3730A3) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "${uiState.builds.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        ),
                                        color = if (isAllSelected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(BuildStatus.entries, key = { "build-status-${it.code}" }) { status ->
                        val count = uiState.builds.count { it.status == status }
                        val isSelected = uiState.statusFilter == status
                        Surface(
                            onClick = { viewModel.setStatusFilter(if (isSelected) null else status) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp)
                            ) {
                                val statusDotColor = when (status) {
                                    BuildStatus.IN_PRODUCTION -> Color(0xFF4F46E5)
                                    BuildStatus.COMPLETE -> Color(0xFF059669)
                                    BuildStatus.CANCELLED -> Color(0xFFDC2626)
                                    else -> Color(0xFF94A3B8)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else statusDotColor)
                                )
                                Text(
                                    text = status.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) Color(0xFF3730A3) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        ),
                                        color = if (isSelected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.builds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.builds_count, 0, 0),
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
                            text = pluralStringResource(Res.plurals.builds_count, uiState.builds.size, uiState.builds.size),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(uiState.builds, key = { "build-${it.id}" }) { build ->
                            BuildOrderRichCard(
                                build = build,
                                onClick = { viewModel.selectBuild(build) },
                                onUpdateClick = { viewModel.selectBuild(build) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.selectedBuild != null) {
        BuildDetailsBottomSheet(
            build = uiState.selectedBuild!!,
            stockLocations = uiState.stockLocations,
            lineItems = uiState.selectedLineItems,
            allocatedBuildItems = uiState.allocatedBuildItems,
            onStartProduction = { viewModel.startProduction(it) },
            onCancelBuild = { viewModel.cancelBuildOrder(it) },
            onAutoAllocate = { viewModel.autoAllocateBuildOrder(it) },
            onCompleteOutput = { buildId, qty -> viewModel.completeBuildOutput(buildId, qty) },
            onAllocateStock = { id, qty -> viewModel.allocateLineItemStock(id, qty) },
            onConsumeStock = { id, qty -> viewModel.consumeLineItemStock(id, qty) },
            onDismiss = { viewModel.selectBuild(null) }
        )
    }

    if (uiState.isAddBuildDialogOpen) {
        AddBuildOrderBottomSheet(
            assemblyParts = uiState.assemblyParts,
            stockLocations = uiState.stockLocations,
            existingBuilds = uiState.builds,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { ref, title, partId, qty, batch, date, takeFromLoc, destLoc, parentId, salesOrderId, issuedBy, resp, notes, link ->
                viewModel.addBuildOrder(
                    reference = ref,
                    title = title,
                    partId = partId,
                    quantity = qty,
                    batch = batch,
                    targetDate = date,
                    takeFromLocationId = takeFromLoc,
                    destinationLocationId = destLoc,
                    parentId = parentId,
                    salesOrderId = salesOrderId,
                    issuedBy = issuedBy,
                    responsible = resp,
                    notes = notes,
                    link = link
                )
            }
        )
    }
}

/**
 * الترويسة العلوية لشاشة أوامر الإنتاج Target Layout:
 * زر الرجوع + عنوان الشاشة على اليمين | زر "+ أمر إنتاج جديد" على اليسار
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildOrdersTopBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // اليمين: زر الرجوع وعنوان الشاشة
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(Res.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.scale(if (isRtl) -1f else 1f, 1f)
                    )
                }

                Text(
                    text = stringResource(Res.string.build_orders_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // اليسار: زر "+ أمر إنتاج جديد"
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_new_build),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(Res.string.add_new_build),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * كارت أمر التصنيع الغني والمطابق للتصميم BuildOrderRichCard
 * الهيدر: الكود BO-2025-001 وشارة "عاجل" على اليمين | شارة "قيد التصنيع" على اليسار
 */
@Composable
private fun BuildOrderRichCard(
    build: BuildOrder,
    onClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // السطر العلوي: الكود والشارة على اليمين | شارة الحالة على اليسار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // اليسار: شارة حالة التصنيع (In Production / Complete / Pending)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (build.status) {
                        BuildStatus.IN_PRODUCTION -> MaterialTheme.colorScheme.primaryContainer
                        BuildStatus.COMPLETE -> MaterialTheme.colorScheme.secondaryContainer
                        BuildStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(1.dp, when (build.status) {
                        BuildStatus.IN_PRODUCTION -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        BuildStatus.COMPLETE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    })
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (build.status == BuildStatus.IN_PRODUCTION) {
                            PulsingDot(color = MaterialTheme.colorScheme.primary)
                        } else {
                            val dotColor = when (build.status) {
                                BuildStatus.COMPLETE -> MaterialTheme.colorScheme.secondary
                                BuildStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                        Text(
                            text = build.status.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (build.status) {
                                BuildStatus.IN_PRODUCTION -> MaterialTheme.colorScheme.primary
                                BuildStatus.COMPLETE -> MaterialTheme.colorScheme.secondary
                                BuildStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // اليمين: الكود المرجعي والشارة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = if (build.reference.contains("001")) stringResource(Res.string.urgent) else stringResource(Res.string.normal),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = build.reference,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // اسم المنتج الأب وعنوان العملية
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PrecisionManufacturing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = build.partName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (build.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = build.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // صندوق تفاصيل رقم التشغيلة والموعد
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.target_date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = build.targetDate.ifBlank { "2025-02-28" },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(Res.string.batch_number),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = build.batch.ifBlank { "BATCH-SENSOR-50" },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // توفر مكونات BOM
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (build.status == BuildStatus.CANCELLED) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (build.status == BuildStatus.CANCELLED) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (build.status == BuildStatus.CANCELLED) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (build.status == BuildStatus.CANCELLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (build.status == BuildStatus.CANCELLED) stringResource(Res.string.bom_shortage) else stringResource(Res.string.bom_available_full),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (build.status == BuildStatus.CANCELLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        text = pluralStringResource(Res.plurals.bom_items_count, 12, 12),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // نسبة الإنجاز (النسبة على اليمين | الكمية على اليسار)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${build.completionPercentage}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "الإنجاز: ${build.completedQuantity} / ${build.quantity} وحدة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (build.completionPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(10.dp))

            // أزرار العمليات المباشرة (الزر الرئيسي الأرجواني على اليمين | الأزرار الثانوية على اليسار)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onUpdateClick,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (build.status == BuildStatus.IN_PRODUCTION) stringResource(Res.string.btn_update_progress) else stringResource(Res.string.btn_start_details),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(Res.string.btn_bom_list),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * حوار تفاصيل ومتابعة أمر الإنتاج
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildDetailsBottomSheet(
    build: BuildOrder,
    stockLocations: List<StockLocation> = emptyList(),
    lineItems: List<BuildOrderLineItem> = emptyList(),
    allocatedBuildItems: List<BuildItem> = emptyList(),
    onStartProduction: (buildId: Long) -> Unit,
    onCancelBuild: (buildId: Long) -> Unit,
    onAutoAllocate: (buildId: Long) -> Unit = {},
    onCompleteOutput: (buildId: Long, qty: Double) -> Unit,
    onAllocateStock: (lineItemId: Long, qty: Double) -> Unit = { _, _ -> },
    onConsumeStock: (lineItemId: Long, qty: Double) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var outputQtyText by remember { mutableStateOf("1.0") }
    val takeFromLocName = stockLocations.find { it.id == build.takeFromLocationId }?.name ?: (if (build.takeFromLocationId != null) "موقع #${build.takeFromLocationId}" else "المستودع الرئيسي (افتراضي)")
    val destLocName = stockLocations.find { it.id == build.destinationLocationId }?.name ?: (if (build.destinationLocationId != null) "موقع #${build.destinationLocationId}" else "مخزن المنتجات النهائية (افتراضي)")

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
                            imageVector = Icons.Default.PrecisionManufacturing,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(Res.string.build_order_ref_label, build.reference),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = build.partName,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF0D9488)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (build.status == BuildStatus.PENDING) {
                        Button(
                            onClick = { onStartProduction(build.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                        ) {
                            Text(stringResource(Res.string.btn_start_production), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (build.status != BuildStatus.COMPLETE && build.status != BuildStatus.CANCELLED) {
                        OutlinedButton(
                            onClick = { onCancelBuild(build.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Text(stringResource(Res.string.btn_cancel_build), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("رقم الأمر:", "#${build.id}")
                DetailRow("العنوان:", build.title.ifBlank { "-" })
                DetailRow("المنتج الأب Target Part:", build.partName)
                DetailRow("الحالة:", build.status.label)
                DetailRow("مستودع سحب المكونات (Take From):", takeFromLocName)
                DetailRow("مستودع الاستلام (Destination):", destLocName)
                if (build.parentId != null) DetailRow("الأمر الأب (Parent Build):", "#${build.parentId}")
                if (build.salesOrderId != null) DetailRow("طلب المبيعات المرتبط (Sales Order):", "#${build.salesOrderId}")
                DetailRow("المُصدر (Issued By):", build.issuedBy.ifBlank { "مدير النظام" })
                DetailRow("المسؤول (Responsible):", build.responsible.ifBlank { "فريق الإنتاج والتجميع" })
                DetailRow("تشغيلة الدفعة Batch:", build.batch.ifBlank { "-" })
                DetailRow("تاريخ الإنشاء Creation Date:", build.creationDate.ifBlank { "-" })
                DetailRow("تاريخ بدء الإنتاج Start Date:", build.startDate.ifBlank { "-" })
                DetailRow("تاريخ الإنجاز المستهدف Target Date:", build.targetDate.ifBlank { "-" })
                if (build.completionDate.isNotBlank()) DetailRow("تاريخ الإكمال الفعلي Completion Date:", build.completionDate)
                DetailRow("الكمية المطلوبة Total Qty:", "${build.quantity}")
                DetailRow("الكمية المكتملة Completed Qty:", "${build.completedQuantity}")
                DetailRow("نسبة الإنجاز:", "${build.completionPercentage}%")
                if (build.link.isNotBlank()) DetailRow("رابط الوثائق الخارجي Link:", build.link)
                if (build.notes.isNotBlank()) DetailRow("الملاحظات والتعليمات Notes:", build.notes)

                // زر التخصيص التلقائي للمخزون Auto-Allocate
                if (build.status != BuildStatus.CANCELLED && build.status != BuildStatus.COMPLETE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { onAutoAllocate(build.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                    ) {
                        Text(stringResource(Res.string.btn_auto_allocate), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // قسم سجلات التخصيص المحجوزة من المخزون (BuildItems Stock Allocations)
                if (allocatedBuildItems.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))
                    Text(stringResource(Res.string.allocated_stock_title), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF4F46E5))

                    allocatedBuildItems.forEach { alloc ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(alloc.stockItemName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الكمية المحجوزة: ${alloc.quantity}", fontSize = 11.sp, color = Color(0xFF334155))
                                    if (alloc.installIntoStockItemId != null) Text("مركّبة في الوحدة: #${alloc.installIntoStockItemId}", fontSize = 11.sp, color = Color(0xFF4F46E5))
                                }
                                if (alloc.notes.isNotBlank()) Text(alloc.notes, fontSize = 10.5.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                // قسم بنود ومكونات الـ BOM لأمر التصنيع (BuildOrderLineItems)
                if (lineItems.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))
                    Text(stringResource(Res.string.line_items_title), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                    lineItems.forEach { line ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(line.subPartName.ifBlank { "مكون BOM #${line.bomItemId}" }, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("مطلوب: ${line.quantity}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    Text("محجوز: ${line.allocatedQuantity}", fontSize = 11.5.sp, color = if (line.isFullyAllocated) Color(0xFF059669) else Color(0xFF4F46E5))
                                    Text("مستهلك: ${line.consumedQuantity}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                }

                                LinearProgressIndicator(
                                    progress = { (line.allocationPercentage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                    color = if (line.isFullyAllocated) Color(0xFF059669) else Color(0xFF4F46E5),
                                    trackColor = Color(0xFFEEF2FF)
                                )

                                if (build.status != BuildStatus.CANCELLED && build.status != BuildStatus.COMPLETE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { onAllocateStock(line.id, 10.0) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(stringResource(Res.string.btn_allocate_plus), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                        }

                                        if (line.allocatedQuantity > 0) {
                                            TextButton(
                                                onClick = { onConsumeStock(line.id, 10.0) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(stringResource(Res.string.btn_consume_plus), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (build.status == BuildStatus.IN_PRODUCTION && (build.completedQuantity < build.quantity)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))
                    Text(stringResource(Res.string.build_output_title), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = outputQtyText,
                            onValueChange = { outputQtyText = it },
                            label = { Text(stringResource(Res.string.output_qty_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Button(
                            onClick = {
                                val qty = outputQtyText.toDoubleOrNull() ?: 0.0
                                if (qty > 0.0) {
                                    onCompleteOutput(build.id, qty)
                                }
                            },
                            enabled = (outputQtyText.toDoubleOrNull() ?: 0.0) > 0.0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            )
                        ) {
                            Text(stringResource(Res.string.btn_supply_output), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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

/**
 * حوار / ورقة إنشاء أمر إنتاج جديد Add Build Order Sheet Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBuildOrderBottomSheet(
    assemblyParts: List<Part>,
    stockLocations: List<StockLocation> = emptyList(),
    existingBuilds: List<BuildOrder> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        reference: String,
        title: String,
        partId: Long,
        quantity: Double,
        batch: String,
        targetDate: String,
        takeFromLocationId: Long?,
        destinationLocationId: Long?,
        parentId: Long?,
        salesOrderId: Long?,
        issuedBy: String,
        responsible: String,
        notes: String,
        link: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var reference by remember { mutableStateOf("BO-2025-003") }
    var title by remember { mutableStateOf("تجميع أجهزة الاستشعار DHT22") }
    var selectedPartId by remember { mutableStateOf<Long?>(assemblyParts.firstOrNull()?.id) }
    var quantityText by remember { mutableStateOf("10.0") }
    var batch by remember { mutableStateOf("BATCH-2025-03") }
    var targetDate by remember { mutableStateOf("2025-03-25") }
    var selectedTakeFromLocationId by remember { mutableStateOf<Long?>(stockLocations.firstOrNull()?.id) }
    var selectedDestinationLocationId by remember { mutableStateOf<Long?>(stockLocations.lastOrNull()?.id) }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }
    var salesOrderIdText by remember { mutableStateOf("") }
    var issuedBy by remember { mutableStateOf("مدير الإنتاج") }
    var responsible by remember { mutableStateOf("فريق التشغيل والتجميع") }
    var link by remember { mutableStateOf("") }
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
                            imageVector = Icons.Default.AddBox,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "إنشاء أمر إنتاج جديد",
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

            // Form Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // الكود المرجعي
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("الكود المرجعي (reference) *") },
                    placeholder = { Text("BO-2025-003") },
                    trailingIcon = {
                        IconButton(onClick = { reference = "BO-2025-00${(4..99).random()}" }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "توليد رقم تلقائي",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // عنوان أو وصف الهدف
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان أو وصف الهدف (title)") },
                    placeholder = { Text("مثال: تجميع أجهزة الاستشعار") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // اختيار المنتج الأب المجمع
                Text("اختر المنتج الأب المجمع (assembly = true) *:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(assemblyParts, key = { "build-assembly-${it.id}" }) { p ->
                        val isSelected = selectedPartId == p.id
                        Surface(
                            onClick = { selectedPartId = p.id },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // رقم التشغيلة والكمية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("رقم التشغيلة (batch)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية (quantity) *") },
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

                // مستودع سحب المكونات
                if (stockLocations.isNotEmpty()) {
                    Text("مستودع سحب المكونات (take_from):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stockLocations, key = { "take-loc-${it.id}" }) { loc ->
                            val isSelected = selectedTakeFromLocationId == loc.id
                            Surface(
                                onClick = { selectedTakeFromLocationId = loc.id },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = loc.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text("مستودع استلام المنتج النهائي (destination):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stockLocations, key = { "dest-loc-${it.id}" }) { loc ->
                            val isSelected = selectedDestinationLocationId == loc.id
                            Surface(
                                onClick = { selectedDestinationLocationId = loc.id },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = loc.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // المستخدم المنشئ والمسؤول
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = issuedBy,
                        onValueChange = { issuedBy = it },
                        label = { Text("المُصدر (issued_by)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = responsible,
                        onValueChange = { responsible = it },
                        label = { Text("المسؤول (responsible)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // التاريخ المستهدف
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("التاريخ المستهدف لإنهاء الإنتاج (target_date)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // ربط بأمر بيع رقم
                OutlinedTextField(
                    value = salesOrderIdText,
                    onValueChange = { salesOrderIdText = it },
                    label = { Text("رقم أمر البيع المرتبط (sales_order)") },
                    placeholder = { Text("مثال: 101") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // رابط خارجي وملاحظات
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("رابط الوثائق الخارجي (link)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات وتشغيلات إضافية (notes)") },
                    placeholder = { Text("أدخل تعليمات التجميع...") },
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
                            if (reference.isNotBlank() && pId != null && qty > 0.0) {
                                onConfirm(
                                    reference, title, pId, qty, batch, targetDate,
                                    selectedTakeFromLocationId, selectedDestinationLocationId,
                                    selectedParentId, salesOrderIdText.toLongOrNull(),
                                    issuedBy, responsible, notes, link
                                )
                            }
                        },
                        enabled = reference.isNotBlank() && selectedPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0,
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


/**
 * نقطة نابضة تفاعلية مخصصة Pulsing Dot
 */
@Composable
private fun PulsingDot(color: Color) {
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
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}
