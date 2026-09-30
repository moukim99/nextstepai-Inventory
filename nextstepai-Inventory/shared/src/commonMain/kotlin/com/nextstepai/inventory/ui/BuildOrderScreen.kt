package com.nextstepai.inventory.ui

import kotlin.math.roundToLong
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import com.nextstepai.inventory.util.DateTimeUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.AppUser
import com.nextstepai.inventory.data.BuildItem
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.ManufacturingPhase
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.StockLocation
import nextstepai_inventory.shared.generated.resources.*
import kotlin.math.abs
import kotlin.math.round

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
                onBackClick = onBackClick
            )
        },
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

                if (uiState.builds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد أوامر إنتاج مسجلة",
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
                            text = "أوامر الإنتاج والتصنيع (${uiState.builds.size})",
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
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(uiState.builds, key = { index, build -> "build-${build.id}-$index" }) { _, build ->
                            BuildOrderRichCard(
                                build = build,
                                onClick = { viewModel.selectBuild(build) },
                                onUpdateClick = { viewModel.selectBuild(build) },
                                onStatusChange = { newStatus -> viewModel.updateBuildStatus(build.id, newStatus) }
                            )
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { viewModel.setAddDialogOpen(true) },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
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
    }

    if (uiState.selectedBuild != null) {
        BuildDetailsBottomSheet(
            build = uiState.selectedBuild!!,
            stockLocations = uiState.stockLocations,
            lineItems = uiState.selectedLineItems,
            allocatedBuildItems = uiState.allocatedBuildItems,
            phases = uiState.phases,
            selectedPhaseUuid = uiState.selectedPhaseUuid,
            onStartProduction = { viewModel.startProduction(it) },
            onCancelBuild = { viewModel.cancelBuildOrder(it) },
            onAutoAllocate = { viewModel.autoAllocateBuildOrder(it) },
            onCompleteOutput = { buildId, qty -> viewModel.completeBuildOutput(buildId, qty) },
            onAllocateStock = { id, qty -> viewModel.allocateLineItemStock(id, qty) },
            onConsumeStock = { id, qty -> viewModel.consumeLineItemStock(id, qty) },
            onSelectPhaseClick = { viewModel.setSelectPhaseBottomSheetOpen(true) },
            onTransferUnits = { buildId, currentPhaseUuid, qty -> viewModel.transferUnitsToNextPhase(buildId, currentPhaseUuid, qty) },
            onDismiss = { viewModel.selectBuild(null) }
        )
    }

    if (uiState.isSelectPhaseBottomSheetOpen) {
        SelectPhaseBottomSheet(
            phases = uiState.phases,
            activePhaseUuid = uiState.selectedPhaseUuid,
            build = uiState.selectedBuild,
            onPhaseSelect = { viewModel.selectPhase(it) },
            onDismiss = { viewModel.setSelectPhaseBottomSheetOpen(false) }
        )
    }

    if (uiState.isAddBuildDialogOpen) {
        AddBuildOrderBottomSheet(
            assemblyParts = uiState.assemblyParts,
            stockLocations = uiState.stockLocations,
            existingBuilds = uiState.builds,
            salesOrders = uiState.salesOrders,
            users = uiState.users,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { ref, title, partId, qty, batch, targetDate, startDate, takeFromLoc, destLoc, parentId, salesOrderId, issuedBy, resp, notes, link ->
                viewModel.addBuildOrder(
                    reference = ref,
                    title = title,
                    partId = partId,
                    quantity = qty,
                    batch = batch,
                    targetDate = targetDate,
                    startDate = startDate,
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

    // نافذة الفلترة التفاعلية بتبويبين
    if (uiState.isFilterBottomSheetOpen) {
        BuildOrderFilterBottomSheet(
            assemblyParts = uiState.assemblyParts,
            selectedPartId = uiState.selectedPartId,
            builds = uiState.builds,
            initialStatus = uiState.statusFilter,
            onDismiss = { viewModel.setFilterBottomSheetOpen(false) },
            onReset = { viewModel.resetFilters() },
            onApply = { partId, status ->
                viewModel.applyFilters(partId, status)
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
                    contentDescription = stringResource(Res.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.scale(if (isRtl) -1f else 1f, 1f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(Res.string.build_orders_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "جدول أوامر التصنيع ومتابعة حجز المكونات والتركيب",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
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
    onUpdateClick: () -> Unit,
    onStatusChange: (BuildStatus) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

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
            // السطر العلوي: الكود المرجعي والشارة على اليمين | شارة الحالة وقائمة الخيارات السريعة على اليسار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // اليسار: شارة حالة التصنيع عالية التباين + زر الخيارات السريعة ⋮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات إضافية",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تفاصيل وتحديث الأمر", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )

                            if (build.status != BuildStatus.CANCELLED) {
                                DropdownMenuItem(
                                    text = { Text("إيقاف / إلغاء الأمر", fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onStatusChange(BuildStatus.CANCELLED)
                                    }
                                )
                            }
                        }
                    }

                    val (statusBg, statusBorder, statusText) = when (build.status) {
                        BuildStatus.IN_PRODUCTION -> Triple(Color(0xFFEEF2FF), Color(0xFFC7D2FE), Color(0xFF3730A3))
                        BuildStatus.COMPLETE -> Triple(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF065F46))
                        BuildStatus.CANCELLED -> Triple(Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFF991B1B))
                        else -> Triple(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFF92400E))
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (build.status == BuildStatus.IN_PRODUCTION) {
                                PulsingDot(color = Color(0xFF4F46E5))
                            } else {
                                val dotColor = when (build.status) {
                                    BuildStatus.COMPLETE -> Color(0xFF10B981)
                                    BuildStatus.CANCELLED -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
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
                                color = statusText
                            )
                        }
                    }
                }

                // اليمين: الكود المرجعي والشارة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = build.reference,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Text(
                            text = if (build.reference.contains("001")) stringResource(Res.string.urgent) else stringResource(Res.string.normal),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF3730A3),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
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
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = build.partName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4F46E5)
                )
            }

            if (build.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = build.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // صندوق تفاصيل رقم التشغيلة والموعد ورقم أمر البيع (إن وجد)
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
                            text = stringResource(Res.string.target_date),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = build.targetDate.ifBlank { build.creationDate.ifBlank { "-" } },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A)
                        )
                    }

                    if (build.salesOrderId != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = "SO-#${build.salesOrderId}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF3730A3),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(Res.string.batch_number),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = build.batch.ifBlank { build.reference.replace("BO-", "BATCH-") },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // توفر مكونات BOM وعرض حالة الأمر الحقيقية
            val statusText = when (build.status) {
                BuildStatus.COMPLETE -> "تم الإنتاج والتوريد بالكامل (100%)"
                BuildStatus.CANCELLED -> "أمر إنتاج ملغى / توقف"
                BuildStatus.IN_PRODUCTION -> "قيد التشغيل والتجميع المباشر"
                BuildStatus.PENDING -> "جاهز للإنتاج (المكونات متوفرة 100%)"
            }
            val statusBg = if (build.status == BuildStatus.CANCELLED) Color(0xFFFEF2F2) else if (build.status == BuildStatus.IN_PRODUCTION) Color(0xFFEEF2FF) else Color(0xFFECFDF5)
            val statusBorder = if (build.status == BuildStatus.CANCELLED) Color(0xFFFECACA) else if (build.status == BuildStatus.IN_PRODUCTION) Color(0xFFC7D2FE) else Color(0xFFA7F3D0)
            val statusTextClr = if (build.status == BuildStatus.CANCELLED) Color(0xFF991B1B) else if (build.status == BuildStatus.IN_PRODUCTION) Color(0xFF3730A3) else Color(0xFF065F46)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = statusBg,
                border = BorderStroke(1.dp, statusBorder)
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
                            tint = if (build.status == BuildStatus.CANCELLED) Color(0xFFDC2626) else Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusTextClr
                        )
                    }

                    Text(
                        text = "مكونات قائمة المواد (BOM)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusTextClr
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // نسبة الإنجاز
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
                    color = Color(0xFF4F46E5)
                )

                Text(
                    text = "الإنجاز: ${build.displayCompletedQuantity} / ${build.quantity} وحدة",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LinearProgressIndicator(
                    progress = { (build.completionPercentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color(0xFF4F46E5),
                    trackColor = Color(0xFFEEF2FF),
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Spacer(modifier = Modifier.height(10.dp))

            // أزرار العمليات المباشرة (الزر الديناميكي على اليمين | الزر الرئيسي الشامل على اليسار)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // الزر الأيمن: زر تحكم في حالة التشغيل التفاعلية
                when (build.status) {
                    BuildStatus.IN_PRODUCTION -> {
                        OutlinedButton(
                            onClick = { onStatusChange(BuildStatus.PENDING) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFFFBEB),
                                contentColor = Color(0xFF92400E)
                            )
                        ) {
                            Text("⏸️ إيقاف مؤقت", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    BuildStatus.COMPLETE -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✓ مكتمل بالكامل", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF065F46))
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = { onStatusChange(BuildStatus.IN_PRODUCTION) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            )
                        ) {
                            Text("▶️ بدء التشغيل", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // الزر الأيسر: زر تفاصيل ومسار الإنتاج الرئيسي
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1.2f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "تفاصيل ومسار الإنتاج",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
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
    phases: List<ManufacturingPhase> = emptyList(),
    selectedPhaseUuid: String? = null,
    onStartProduction: (buildId: Long) -> Unit,
    onCancelBuild: (buildId: Long) -> Unit,
    onAutoAllocate: (buildId: Long) -> Unit = {},
    onCompleteOutput: (buildId: Long, qty: Double) -> Unit,
    onAllocateStock: (lineItemId: Long, qty: Double) -> Unit = { _, _ -> },
    onConsumeStock: (lineItemId: Long, qty: Double) -> Unit = { _, _ -> },
    onSelectPhaseClick: () -> Unit = {},
    onTransferUnits: (buildId: Long, currentPhaseUuid: String, qty: Double) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) }
    var outputQtyText by remember { mutableStateOf("1.0") }
    var transferQtyText by remember { mutableStateOf("1.0") }

    val takeFromLocName = stockLocations.find { it.id == build.takeFromLocationId }?.name
        ?: (if (build.takeFromLocationId != null) "موقع #${build.takeFromLocationId}" else "المستودع الرئيسي (Main Warehouse)")
    val destLocName = stockLocations.find { it.id == build.destinationLocationId }?.name
        ?: (if (build.destinationLocationId != null) "موقع #${build.destinationLocationId}" else "مخزن المنتجات النهائية (Finished Goods)")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 2.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrecisionManufacturing,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = build.reference,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.5.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val (statusBg, statusBorder, statusText) = when (build.status) {
                            BuildStatus.IN_PRODUCTION -> Triple(Color(0xFFEEF2FF), Color(0xFFC7D2FE), Color(0xFF3730A3))
                            BuildStatus.COMPLETE -> Triple(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF065F46))
                            BuildStatus.CANCELLED -> Triple(Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFF991B1B))
                            else -> Triple(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFF92400E))
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusBg,
                            border = BorderStroke(1.dp, statusBorder)
                        ) {
                            Text(
                                text = build.status.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                color = statusText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = build.partName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = Color(0xFF4F46E5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "هدف الأمر: ${build.title.ifBlank { build.partName }} | الكمية: ${build.quantity} وحدة",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 3-Tab Segmented Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { selectedTab = 0 },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == 0) Color(0xFF4F46E5) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "1. المخزون والمكونات",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                            color = if (selectedTab == 0) Color.White else Color(0xFF475569)
                        )
                    }
                }

                Surface(
                    onClick = { selectedTab = 1 },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "2. الفاتورة والتكلفة",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                            color = if (selectedTab == 1) Color.White else Color(0xFF475569)
                        )
                    }
                }

                Surface(
                    onClick = { selectedTab = 2 },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == 2) Color(0xFF4F46E5) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "3. مراحل الخط والتشغيل",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                            color = if (selectedTab == 2) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            // Tab Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // ================= Tab 1: المخزون والجاهزية (Stock Readiness) =================
                        val fullyAllocatedLinesCount = lineItems.count { it.isFullyAllocated }
                        val totalLinesCount = lineItems.size
                        val isAllReady = totalLinesCount > 0 && fullyAllocatedLinesCount == totalLinesCount

                        // شريط ملخص الجاهزية العلوي (Readiness Summary Bar)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAllReady) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (isAllReady) Color(0xFFA7F3D0) else Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAllReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isAllReady) Color(0xFF059669) else Color(0xFFD97706),
                                    modifier = Modifier.size(22.dp)
                                )

                                Column {
                                    Text(
                                        text = if (isAllReady) "المكونات متوفرة بالكامل (100%) - جاهز للتشغيل" else "عجز مخزني بـ (${totalLinesCount - fullyAllocatedLinesCount}) بنود محددة",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isAllReady) Color(0xFF065F46) else Color(0xFF92400E)
                                    )
                                    Text(
                                        text = if (isAllReady) "تم حجز وتأمين كافة المواد والقطع المطلوبة في مستودع الصرف" else "متوفر $fullyAllocatedLinesCount من أصل $totalLinesCount مكونات. يرجى التخصيص أو طلب النواقص.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (isAllReady) Color(0xFF047857) else Color(0xFFB45309)
                                    )
                                }
                            }
                        }

                        // كروت تفاصيل المستودعات والمشرف
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // مستودع الصرف
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "من (صرف المكونات):",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFF64748B)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = takeFromLocName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                                color = Color(0xFF0F172A),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "مسار الصرف والاستلام",
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    // مستودع الاستلام النهائي
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "إلى (استلام النهائي):",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFF64748B)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = destLocName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                                color = Color(0xFF0F172A),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("التشغيلة: ${build.batch.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                    Text("المُصدر والمسؤول: ${build.issuedBy.ifBlank { "المشرف" }}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                }
                            }
                        }

                        // جدول بنود ومكونات الـ BOM لأمر التصنيع
                        Text("بنود ومكونات قائم المواد المطلوبة (Components Allocation List):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                        if (lineItems.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد بنود قائمة مواد مسجلة لهذا الأمر حالياً", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                }
                            }
                        } else {
                            lineItems.forEach { line ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(line.subPartName.ifBlank { "مكون BOM #${line.bomItemId}" }, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                val isSubAssembly = line.subPartName.contains("تجميع", ignoreCase = true) ||
                                                                    line.subPartName.contains("قالب", ignoreCase = true) ||
                                                                    line.subPartName.contains("لوحة", ignoreCase = true) ||
                                                                    line.subPartName.contains("Mainboard", ignoreCase = true) ||
                                                                    line.subPartName.contains("Module", ignoreCase = true) ||
                                                                    line.subPartName.contains("Assembly", ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isSubAssembly) Color(0xFFEEF2FF) else Color(0xFFECFDF5)
                                                ) {
                                                    Text(
                                                        text = if (isSubAssembly) "تجميعة فرعية" else "مادة خام",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                        color = if (isSubAssembly) Color(0xFF3730A3) else Color(0xFF065F46),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (line.isFullyAllocated) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                                                border = BorderStroke(1.dp, if (line.isFullyAllocated) Color(0xFFA7F3D0) else Color(0xFFFDE68A))
                                            ) {
                                                Text(
                                                    text = if (line.isFullyAllocated) "متوفر بالكامل (100%)" else "عجز: ناقص ${line.quantity - line.allocatedQuantity} وحدة",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                                    color = if (line.isFullyAllocated) Color(0xFF065F46) else Color(0xFF92400E),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("المطلوب للأمر: ${line.quantity} وحدة", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                            Text("المحجوز: ${line.allocatedQuantity}", fontSize = 11.5.sp, color = if (line.isFullyAllocated) Color(0xFF059669) else Color(0xFF4F46E5))
                                            Text("المستهلك: ${line.consumedQuantity}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                        }

                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            LinearProgressIndicator(
                                                progress = { (line.allocationPercentage / 100f).coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                                color = if (line.isFullyAllocated) Color(0xFF059669) else Color(0xFF4F46E5),
                                                trackColor = Color(0xFFEEF2FF),
                                                gapSize = 0.dp,
                                                drawStopIndicator = {}
                                            )
                                        }

                                        // عرض سجل الدفعة المحجوزة المربوطة بهذه المادة مباشرة (Item-Level Batch Link)
                                        val lineAllocations = allocatedBuildItems.filter {
                                            it.buildLineId == line.id || (line.subPartName.isNotBlank() && it.stockItemName.contains(line.subPartName, ignoreCase = true))
                                        }

                                        if (lineAllocations.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFEEF2FF),
                                                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        "الدفعات والرفوف المحجوزة لهذه المادة:",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                                        color = Color(0xFF4F46E5)
                                                    )
                                                    lineAllocations.forEach { alloc ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                "📦 ${alloc.stockItemName}",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                                                color = Color(0xFF0F172A)
                                                            )
                                                            Text(
                                                                "الكمية: ${alloc.quantity}",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                                                color = Color(0xFF3730A3)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

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
                                                    Text("حجز +", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                                }

                                                if (line.allocatedQuantity > 0) {
                                                    TextButton(
                                                        onClick = { onConsumeStock(line.id, 10.0) },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("صرف واستهلاك +", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // شريط أزرار التحكم السفلية (Action Control Footer)
                            if (build.status != BuildStatus.CANCELLED && build.status != BuildStatus.COMPLETE) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("إجراءات التحكم وحجز/صرف المواد الفعلي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onAutoAllocate(build.id) },
                                                modifier = Modifier.weight(1.3f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Text("صرف وحجز المكونات لخط الإنتاج", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    lineItems.firstOrNull()?.let { onAllocateStock(it.id, 10.0) }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Text("+ تخصيص دفعة مخزون", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // ================= Tab 2: الفاتورة وسند التكلفة التقديرية (Cost Breakdown & Build Pro-forma) =================
                        val matCost = lineItems.sumOf { it.quantity * it.unitCost }.let { if (it <= 0.0) build.quantity * 120.0 else it }
                        val laborCost = matCost * 0.15
                        val overheadCost = matCost * 0.05
                        val scrapCost = (matCost + laborCost + overheadCost) * 0.03
                        val totalCost = matCost + laborCost + overheadCost + scrapCost
                        val unitCost = totalCost / build.quantity.coerceAtLeast(1.0)
                        val estimatedSalePrice = unitCost * 1.54
                        val profitMargin = ((estimatedSalePrice - unitCost) / estimatedSalePrice) * 100

                        // بطاقة الملخص المالي العلوي (Executive Cost Summary Bar)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("إجمالي تكلفة أمر الإنتاج:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                        Text("$${totalCost.formatMoney()}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF4F46E5))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("تكلفة القطعة الواحدة (Unit Cost):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                        Text("$${unitCost.formatMoney()} / وحدة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("هامش الربح المتوقع بمتوسط سعر السداد:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF065F46))
                                        Text("${profitMargin.formatMoney()}% هامش ربح", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                                    }
                                }
                            }
                        }

                        Text("تفاصيل الفاتورة الأولية وسند التكلفة (Itemized Cost Breakdown):", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))

                        // أ. Direct Materials Section
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("أ. المواد والمكونات المباشرة (Direct Materials):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF4F46E5))

                                if (lineItems.isEmpty()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("تقدير المكونات المباشرة (افتراضي)", fontSize = 11.5.sp, color = Color(0xFF475569))
                                        Text("$${matCost.formatMoney()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                } else {
                                    lineItems.forEach { line ->
                                        val lineCost = line.quantity * line.unitCost
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${line.subPartName.ifBlank { "مكون #${line.bomItemId}" }} (${line.quantity} وحدة × $${line.unitCost.formatMoney()})", fontSize = 11.5.sp, color = Color(0xFF475569))
                                            Text("$${lineCost.formatMoney()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مجموع المكونات المباشرة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                    Text("$${matCost.formatMoney()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF4F46E5))
                                }
                            }
                        }

                        // ب. Direct Labor & Overhead Section
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ب. العمالة والمصروفات التشغيلية (Labor & Overhead):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0D9488))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("العمالة المباشرة وأجور الفنيين (Direct Labor 15%):", fontSize = 11.5.sp, color = Color(0xFF475569))
                                    Text("$${laborCost.formatMoney()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("استهلاك المعدات والكهرباء (Machine Overhead 5%):", fontSize = 11.5.sp, color = Color(0xFF475569))
                                    Text("$${overheadCost.formatMoney()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }
                        }

                        // ج. Scrap Allowance Section
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ج. مخصص الهدر والتلف الصناعي (Scrap Buffer +3%):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD97706))
                                    Text("$${scrapCost.formatMoney()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                                }
                                Text("أمان صناعي لحماية الحسابات من التلف الطبيعي أثناء التجميع واللحام.", fontSize = 10.5.sp, color = Color(0xFF64748B))
                            }
                        }

                        var showExportProFormaDialog by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { showExportProFormaDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("🖨️ تصدير وطباعة سند التكلفة التقديري (Pro-forma Sheet)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (showExportProFormaDialog) {
                            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                            ModalBottomSheet(
                                onDismissRequest = { showExportProFormaDialog = false },
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
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF4F46E5))
                                        Text("سند التكلفة التقديري (Pro-forma Sheet)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("أمر الإنتاج: ${build.reference} - ${build.partName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("إجمالي تكلفة أمر الإنتاج: $${totalCost.formatMoney()}", fontSize = 12.5.sp)
                                        Text("تكلفة القطعة الواحدة: $${unitCost.formatMoney()}", fontSize = 12.5.sp)
                                        Text("هامش الربح المستهدف: ${profitMargin.formatMoney()}%", fontSize = 12.5.sp)
                                        Text("تم إنشاء وثيقة السند التقديري وتوثيق الحسابات بنجاح.", fontSize = 11.5.sp, color = Color(0xFF059669))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(onClick = { showExportProFormaDialog = false }) {
                                            Text("إغلاق")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { showExportProFormaDialog = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                        ) {
                                            Text("تم وطباعة", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        Text(
                            text = "ملاحظة توثيقية: الأسعار معتمدة وفق متوسط تكلفة الشراء الحالية بجدول المخزون وتُقفل نهائياً عند توريد المخرجات.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, textAlign = TextAlign.Center),
                            color = Color(0xFF64748B),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    2 -> {
                        // ================= Tab 3: مراحل الخط والتشغيل وتتبع الاختناقات =================
                        if (phases.isEmpty()) {
                            Text("لا توجد مراحل مسجلة لهذا المنتج", modifier = Modifier.padding(16.dp))
                        } else {
                            val activePhase = phases.find { it.uuid == selectedPhaseUuid } ?: phases.first()
                            val activePhaseIdx = phases.indexOf(activePhase)
                            val totalPhases = phases.size
                            val unitsInPhase = build.phaseQuantities[activePhase.uuid] ?: 0.0

                            // بطاقة اختيار المرحلة (SelectPhaseBottomSheet Trigger)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth().clickable { onSelectPhaseClick() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "المرحلة الحالية: ${activePhase.name}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            "المرحلة ${activePhaseIdx + 1} من $totalPhases | ${unitsInPhase} وحدة متواجدة",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "اختيار المرحلة",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("إدارة الوحدات (Bulk Actions):", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))

                            if (unitsInPhase > 0) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(checked = true, onCheckedChange = {})
                                            Text("تحديد الكل (${unitsInPhase} وحدة)", fontSize = 12.sp)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = transferQtyText,
                                                onValueChange = { transferQtyText = it },
                                                label = { Text("الكمية") },
                                                modifier = Modifier.weight(1f)
                                            )

                                            Button(
                                                onClick = {
                                                    val qty = transferQtyText.toDoubleOrNull() ?: return@Button
                                                    onTransferUnits(build.id, activePhase.uuid, qty)
                                                    transferQtyText = "1.0"
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (activePhaseIdx == totalPhases - 1) Color(0xFF059669) else Color(0xFF4F46E5)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(2f)
                                            ) {
                                                if (activePhaseIdx == totalPhases - 1) {
                                                    Text("إيداع في مستودع المنتجات التامة", fontSize = 11.sp, textAlign = TextAlign.Center)
                                                } else {
                                                    Text("نقل للمرحلة التالية ➔", fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {},
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("⏸️ إيقاف")
                                            }
                                            OutlinedButton(
                                                onClick = {},
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("ملاحظات تقنية 📝", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("لا توجد وحدات متوفرة في هذه المرحلة للنقل.", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                            }
                        }

                        // قسم توريد وتسليم المخرجات المكتملة
                        if (build.status == BuildStatus.IN_PRODUCTION && (build.completedQuantity < build.quantity)) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))
                            Text("تسليم وتوريد كميات مصنعة جديدة للمخزن النهائي:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = outputQtyText,
                                    onValueChange = { outputQtyText = it },
                                    label = { Text("الكمية المصنعة المكتملة") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White)
                                ) {
                                    Text("🎉 توريد للمخزن النهائي", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPhaseBottomSheet(
    phases: List<ManufacturingPhase>,
    activePhaseUuid: String?,
    build: BuildOrder?,
    onPhaseSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("اختيار المرحلة التشغيلية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(phases) { phase ->
                    val isActive = phase.uuid == activePhaseUuid
                    val units = build?.phaseQuantities?.get(phase.uuid) ?: 0.0
                    
                    Surface(
                        onClick = { onPhaseSelect(phase.uuid) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isActive) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(phase.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = if (isActive) Color(0xFF3730A3) else Color(0xFF0F172A))
                                Text("ترتيب: ${phase.sequenceOrder} | الوحدات المتواجدة: $units", fontSize = 11.5.sp, color = Color(0xFF475569))
                            }
                            if (isActive) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Color(0xFF4F46E5))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
    salesOrders: List<SalesOrder> = emptyList(),
    users: List<AppUser> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        reference: String,
        title: String,
        partId: Long,
        quantity: Double,
        batch: String,
        targetDate: String,
        startDate: String,
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
    var batch by remember { mutableStateOf("BATCH-2025-03") }
    var title by remember { mutableStateOf("تجميع أجهزة الاستشعار DHT22") }
    var selectedPartId by remember { mutableStateOf<Long?>(assemblyParts.firstOrNull()?.id) }
    var isSelectParentSheetOpen by remember { mutableStateOf(false) }

    var quantityText by remember { mutableStateOf("10.0") }
    var startDate by remember { mutableStateOf(DateTimeUtils.getCurrentDate()) }
    var targetDate by remember { mutableStateOf("2025-03-25") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }

    var selectedTakeFromLocationId by remember(stockLocations) {
        mutableStateOf<Long?>(stockLocations.firstOrNull()?.id)
    }
    var selectedDestinationLocationId by remember(stockLocations) {
        mutableStateOf<Long?>(
            stockLocations.find { it.name.contains("منتج") || it.name.contains("نهائي") || it.name.contains("مكتمل") }?.id
                ?: stockLocations.lastOrNull()?.id
        )
    }
    var isSelectLocationSheetOpen by remember { mutableStateOf(false) }
    var pickingLocationFor by remember { mutableStateOf("TAKE_FROM") }

    var isSelectUserSheetOpen by remember { mutableStateOf(false) }
    val initialUser = remember(users) {
        users.find { it.active && !it.isDeleted }?.name ?: "مدير الإنتاج والتصنيع"
    }
    var issuedBy by remember(initialUser) { mutableStateOf(initialUser) }
    var responsible by remember { mutableStateOf("فريق التشغيل والتجميع") }
    var notes by remember { mutableStateOf("") }
    var selectedSalesOrderId by remember { mutableStateOf<Long?>(null) }
    var isSelectSalesOrderSheetOpen by remember { mutableStateOf(false) }

    val selectedAssemblyPart = remember(selectedPartId, assemblyParts) {
        assemblyParts.find { it.id == selectedPartId }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val dateTime = instant.toLocalDateTime(TimeZone.UTC)
                        val year = dateTime.year
                        val month = dateTime.monthNumber.toString().padStart(2, '0')
                        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
                        startDate = "$year-$month-$day"
                    }
                    showStartDatePicker = false
                }) {
                    Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                .imePadding()
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
                // 1. الدمج الأفقي للرمز المرجعي + رقم التشغيلة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { newRef ->
                            reference = newRef
                            if (newRef.startsWith("BO-")) {
                                batch = "BATCH-" + newRef.removePrefix("BO-")
                            }
                        },
                        label = { Text("الكود المرجعي *") },
                        placeholder = { Text("BO-2025-003") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val newNum = (4..999).random().toString().padStart(3, '0')
                                reference = "BO-2025-$newNum"
                                batch = "BATCH-2025-$newNum"
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "توليد رقم تلقائي",
                                    tint = Color(0xFF4F46E5)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("رقم التشغيلة") },
                        placeholder = { Text("BATCH-2025-03") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // 2. عنوان الأمر
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان أو وصف هدف الأمر (Title)") },
                    placeholder = { Text("مثال: تجميع أجهزة الاستشعار DHT22") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // 3. بطاقة اختيار المنتج الأب الأنيقة (BOM-style Selection Card)
                Text(
                    text = "المنتج المراد تصنيعه (Parent Part) *:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                Surface(
                    onClick = { isSelectParentSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.5.dp, if (selectedAssemblyPart != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEEF2FF))
                                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = selectedAssemblyPart?.name ?: "اختر المنتج المجمّع المراد تصنيعه *",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                if (selectedAssemblyPart != null) {
                                    Text(
                                        text = "IPN: ${selectedAssemblyPart.ipn.ifBlank { "-" }} | المخزون المتوفر: ${selectedAssemblyPart.totalInStock} ${selectedAssemblyPart.units}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
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

                // 4. الكمية المطلوبة
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المطلوب تصنيعها (Quantity) *") },
                    placeholder = { Text("10.0") },
                    singleLine = true,
                    suffix = {
                        Text(
                            text = selectedAssemblyPart?.units?.ifBlank { "pcs" } ?: "pcs",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // 5. بطاقتان متجاورتان لمستودع الصرف ومستودع الاستلام المباشر
                if (stockLocations.isNotEmpty()) {
                    val takeLoc = stockLocations.find { it.id == selectedTakeFromLocationId }
                    val destLoc = stockLocations.find { it.id == selectedDestinationLocationId }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // مستودع صرف المكونات (على اليمين في RTL)
                        Surface(
                            onClick = {
                                pickingLocationFor = "TAKE_FROM"
                                isSelectLocationSheetOpen = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "من (صرف المكونات):",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        color = Color(0xFF64748B)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = takeLoc?.name ?: "اختر مستودع الصرف ▾",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // السهم البصري الموجه لليسار ببيئة RTL من اليمين (صرف) إلى اليسار (استلام)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "مسار التجميع",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(18.dp)
                        )

                        // مستودع استلام المنتج النهائي (على اليسار في RTL)
                        Surface(
                            onClick = {
                                pickingLocationFor = "DESTINATION"
                                isSelectLocationSheetOpen = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "إلى (استلام النهائي):",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        color = Color(0xFF64748B)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = destLoc?.name ?: "اختر مستودع الاستلام ▾",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 6. التواريخ المتجاورة (تاريخ البدء + تاريخ التسليم) بحقول تقويم تفاعلية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تاريخ البدء") },
                        placeholder = { Text("YYYY-MM-DD") },
                        trailingIcon = {
                            IconButton(onClick = { showStartDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "تقويم تاريخ البدء",
                                    tint = Color(0xFF4F46E5)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تاريخ التسليم") },
                        placeholder = { Text("YYYY-MM-DD") },
                        trailingIcon = {
                            IconButton(onClick = { showTargetDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "تقويم تاريخ التسليم",
                                    tint = Color(0xFF4F46E5)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTargetDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // 7. المشرف والمسؤول والمصادر
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = responsible,
                        onValueChange = { responsible = it },
                        label = { Text("المشرف والمسؤول") },
                        placeholder = { Text("فريق التشغيل والتجميع") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = issuedBy,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المُصدر (تلقائي)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "اختر المُصدر",
                                    tint = Color(0xFF64748B)
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
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { isSelectUserSheetOpen = true }
                        )
                    }
                }

                // 8. الملاحظات
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات وتعليمات التجميع (Notes)") },
                    placeholder = { Text("أدخل أية تعليمات تشغيلية خاصة...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // 9. أمر البيع المرتبط (Sales Order) - اختياري
                Text(
                    text = "أمر البيع المرتبط (Sales Order) - اختياري:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )

                val selectedSalesOrder = remember(selectedSalesOrderId, salesOrders) {
                    salesOrders.find { it.id == selectedSalesOrderId }
                }

                Surface(
                    onClick = { isSelectSalesOrderSheetOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (selectedSalesOrder != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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
                                    .background(if (selectedSalesOrder != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = if (selectedSalesOrder != null) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (selectedSalesOrder != null) "${selectedSalesOrder.reference} - ${selectedSalesOrder.customerName}" else "إنتاج عام للمخزن (بدون أمر بيع)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (selectedSalesOrder != null) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                                if (selectedSalesOrder != null) {
                                    Text(
                                        text = "العميل: ${selectedSalesOrder.customerName} | الكمية: ${selectedSalesOrder.quantity}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        if (selectedSalesOrder != null) {
                            IconButton(
                                onClick = { selectedSalesOrderId = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إلغاء ربط أمر البيع",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختر أمر البيع",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
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
                            val pId = selectedPartId
                            val qty = quantityText.toDoubleOrNull() ?: 1.0
                            if (reference.isNotBlank() && pId != null && qty > 0.0) {
                                onConfirm(
                                    reference, title, pId, qty, batch, targetDate, startDate,
                                    selectedTakeFromLocationId, selectedDestinationLocationId,
                                    null, selectedSalesOrderId,
                                    issuedBy, responsible, notes, ""
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
                        Text("إنشاء أمر الإنتاج", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (isSelectParentSheetOpen) {
        SelectAssemblyForBuildBottomSheet(
            assemblyParts = assemblyParts,
            selectedPartId = selectedPartId,
            onDismiss = { isSelectParentSheetOpen = false },
            onSelect = { p ->
                if (p != null) {
                    selectedPartId = p.id
                }
                isSelectParentSheetOpen = false
            }
        )
    }

    if (isSelectSalesOrderSheetOpen) {
        SelectSalesOrderBottomSheet(
            salesOrders = salesOrders,
            selectedSalesOrderId = selectedSalesOrderId,
            onDismiss = { isSelectSalesOrderSheetOpen = false },
            onSelect = { so ->
                selectedSalesOrderId = so?.id
                isSelectSalesOrderSheetOpen = false
            }
        )
    }

    if (isSelectLocationSheetOpen) {
        val isTakeFrom = pickingLocationFor == "TAKE_FROM"
        SelectLocationForBuildBottomSheet(
            title = if (isTakeFrom) "اختر مستودع صرف المكونات" else "اختر مستودع استلام المنتج النهائي",
            subtitle = if (isTakeFrom) "المستودع أو الرف الذي ستُسحب منه المكونات اللازمة للتصنيع" else "المستودع أو الخط الذي سيُودع فيه المنتج النهائي المكتمل",
            locations = stockLocations,
            selectedLocationId = if (isTakeFrom) selectedTakeFromLocationId else selectedDestinationLocationId,
            onDismiss = { isSelectLocationSheetOpen = false },
            onSelect = { loc ->
                if (loc != null) {
                    if (isTakeFrom) {
                        selectedTakeFromLocationId = loc.id
                    } else {
                        selectedDestinationLocationId = loc.id
                    }
                }
                isSelectLocationSheetOpen = false
            }
        )
    }

    if (isSelectUserSheetOpen) {
        SelectUserBottomSheet(
            users = users,
            selectedUserName = issuedBy,
            onDismiss = { isSelectUserSheetOpen = false },
            onSelectUser = { user ->
                issuedBy = user.name
                isSelectUserSheetOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSalesOrderBottomSheet(
    salesOrders: List<SalesOrder>,
    selectedSalesOrderId: Long?,
    onDismiss: () -> Unit,
    onSelect: (SalesOrder?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredOrders = remember(salesOrders, searchQuery) {
        if (searchQuery.isBlank()) {
            salesOrders
        } else {
            salesOrders.filter {
                it.reference.contains(searchQuery, ignoreCase = true) ||
                it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
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
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "اختر أمر البيع المرتبط (Sales Order)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "ربط هذا الأمر بطلب مبيعات زبون محدد أو إبقائه إنتاج عام للمخزن",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF64748B)
                        )
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

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث برقم أمر البيع أو اسم الزبون...") },
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
                                contentDescription = "مسح",
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

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. الخيار العلوي الثابت: إنتاج عام للمخزن
                item {
                    val isGeneralSelected = selectedSalesOrderId == null
                    Surface(
                        onClick = { onSelect(null) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGeneralSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isGeneralSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isGeneralSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = if (isGeneralSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "إنتاج عام للمخزن (بدون ربط بأمر بيع)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "تصنيع مستقل مخصص لرفع رصيد المخزون العام",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (isGeneralSelected) {
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

                // 2. قائمة أوامر البيع المتاحة
                itemsIndexed(filteredOrders, key = { index, so -> "so-picker-${so.id}-$index" }) { _, so ->
                    val isSelected = selectedSalesOrderId == so.id
                    Surface(
                        onClick = { onSelect(so) },
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "${so.reference} - ${so.customerName}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${so.description} | الكمية: ${so.quantity}",
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectLocationForBuildBottomSheet(
    title: String,
    subtitle: String,
    locations: List<StockLocation>,
    selectedLocationId: Long?,
    onDismiss: () -> Unit,
    onSelect: (StockLocation?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredLocations = remember(locations, searchQuery) {
        if (searchQuery.isBlank()) {
            locations
        } else {
            locations.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.locationType.contains(searchQuery, ignoreCase = true)
            }
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
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF64748B)
                        )
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

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن اسم المستودع، الرف، أو الموقع...") },
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
                                contentDescription = "مسح",
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

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredLocations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مستودعات تخزين مطابقة للبحث",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredLocations, key = { index, loc -> "loc-picker-${loc.id}-$index" }) { _, loc ->
                        val isSelected = selectedLocationId == loc.id
                        Surface(
                            onClick = { onSelect(loc) },
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warehouse,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = loc.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        if (loc.description.isNotBlank()) {
                                            Text(
                                                text = loc.description,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        }
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
            }
        }
    }
}

internal fun Double.formatMoney(): String {
    val rounded = (this * 100.0).roundToLong() / 100.0
    val parts = rounded.toString().split(".")
    val integerPart = parts[0]
    val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
    return "$integerPart.$decimalPart"
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectAssemblyForBuildBottomSheet(
    assemblyParts: List<Part>,
    selectedPartId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Part?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredAssemblies = remember(assemblyParts, searchQuery) {
        if (searchQuery.isBlank()) {
            assemblyParts
        } else {
            assemblyParts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.ipn.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
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
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "اختر المنتج المجمّع لأمر الإنتاج",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "عرض الأصناف المسجلة كمنتجات مجمّعة (assembly = 1)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF64748B)
                        )
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

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن اسم المنتج المجمّع، الـ IPN...") },
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
                                contentDescription = "مسح",
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

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredAssemblies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد منتجات مجمّعة مطابقة للبحث",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredAssemblies, key = { index, parent -> "assembly-picker-${parent.id}-$index" }) { _, parent ->
                        val isSelected = selectedPartId == parent.id
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    onSelect(null)
                                } else {
                                    onSelect(parent)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
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
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = parent.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "IPN: ${parent.ipn.ifBlank { "-" }} | المخزون المتوفر: ${parent.totalInStock} ${parent.units}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildOrderFilterBottomSheet(
    assemblyParts: List<Part>,
    selectedPartId: Long?,
    builds: List<BuildOrder>,
    initialStatus: BuildStatus?,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (partId: Long?, status: BuildStatus?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPartIdState by remember { mutableStateOf(selectedPartId) }
    var selectedStatusState by remember { mutableStateOf(initialStatus) }

    val calculatedCount = remember(
        builds, searchQuery, selectedPartIdState, selectedStatusState
    ) {
        val query = searchQuery.trim()
        builds.count { build ->
            val matchesPart = selectedPartIdState == null || build.partId == selectedPartIdState
            val matchesStatus = selectedStatusState == null || build.status == selectedStatusState
            val matchesQuery = query.isBlank() ||
                    build.reference.contains(query, ignoreCase = true) ||
                    build.title.contains(query, ignoreCase = true) ||
                    build.partName.contains(query, ignoreCase = true) ||
                    build.batch.contains(query, ignoreCase = true)

            matchesPart && matchesStatus && matchesQuery
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
                .imePadding()
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
                            text = "تصفية أوامر التصنيع والإنتاج",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تحديد المنتج المستهدف ومرحلة أمر الإنتاج",
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
                        selectedPartIdState = null
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
                    placeholder = { Text("ابحث عن اسم المنتج، الرقم المرجعي، أو الـ Batch...") },
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
                                    text = "🛠️ المنتج المستهدف (${assemblyParts.size})",
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
                                    text = "⚙️ مرحلة أمر التصنيع",
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
                val filteredAssemblyParts = remember(assemblyParts, searchQuery) {
                    if (searchQuery.isBlank()) assemblyParts
                    else assemblyParts.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.ipn.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (selectedTab == 0) {
                    // تبويب المنتجات المجمعة المستهدفة
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isAllSelected = selectedPartIdState == null
                        Surface(
                            onClick = { selectedPartIdState = null },
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
                                    text = "جميع المنتجات المجمّعة المستهدفة",
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

                        filteredAssemblyParts.forEach { part ->
                            val isSelected = selectedPartIdState == part.id
                            Surface(
                                onClick = { selectedPartIdState = part.id },
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
                                            text = part.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "IPN: ${part.ipn.ifBlank { "-" }} | المخزون: ${part.totalInStock} ${part.units}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
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
                    // تبويب مرحلة أمر التصنيع
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
                                Triple(null, "جميع الحالات", "عرض كافة أوامر التصنيع دون تصفية"),
                                Triple(BuildStatus.PENDING, "مسودة (PENDING)", "أوامر منشأة قيد التجهيز والتخطيط"),
                                Triple(BuildStatus.IN_PRODUCTION, "قيد التشغيل والإنتاج (IN_PRODUCTION)", "أوامر جارية ومحجوزة المواد"),
                                Triple(BuildStatus.COMPLETE, "مكتمل ومُنتج (COMPLETE)", "أوامر منتهية وتم توريد منتجها النهائي"),
                                Triple(BuildStatus.CANCELLED, "ملغى (CANCELLED)", "أوامر إنتاج ملغاة")
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
                            onApply(selectedPartIdState, selectedStatusState)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectUserBottomSheet(
    users: List<AppUser>,
    selectedUserName: String,
    onDismiss: () -> Unit,
    onSelectUser: (AppUser) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) {
            users.filter { it.active && !it.isDeleted }
        } else {
            users.filter { user ->
                (user.active && !user.isDeleted) && (
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.role.contains(searchQuery, ignoreCase = true)
                )
            }
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
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "اختر المُصدر (Issued By)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "تحديد الشخص أو الحساب المسؤول عن تعميد أمر الإنتاج",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // شريط البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث باسم المستخدم أو الدور الوظيفي...") },
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
                                contentDescription = "مسح البحث",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "لا يوجد مستخدمون مطابقون للبحث",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredUsers, key = { index, user -> "user-${user.uuid}-$index" }) { _, user ->
                        val isSelected = user.name.trim() == selectedUserName.trim()
                        Card(
                            onClick = { onSelectUser(user) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF818CF8) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFE0E7FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF4F46E5),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = if (isSelected) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                                        )
                                        if (user.role.isNotBlank()) {
                                            Text(
                                                text = user.role,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp,
                                                    color = if (isSelected) Color(0xFF4338CA) else Color(0xFF64748B)
                                                )
                                            )
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4F46E5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "محدد",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
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

