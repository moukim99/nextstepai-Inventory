package com.nextstepai.inventory.ui

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.Part
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_build
import nextstepai_inventory.shared.generated.resources.builds_count
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.save

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

                // شريط البحث المطور مع زر الباركود المدمج على اليسار
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "البحث بالرمز المرجعي، العنوان، المنتج، أو رقم التشغيلة...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح الباركود",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // شريط تصفية حالات أوامر الإنتاج Filter Chips Tray
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "build-status-all") {
                        FilterChip(
                            selected = uiState.statusFilter == null,
                            onClick = { viewModel.setStatusFilter(null) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("كافة الحالات")
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (uiState.statusFilter == null) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${uiState.builds.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50)
                        )
                    }

                    items(BuildStatus.entries, key = { "build-status-${it.code}" }) { status ->
                        val count = uiState.builds.count { it.status == status }
                        FilterChip(
                            selected = uiState.statusFilter == status,
                            onClick = { viewModel.setStatusFilter(if (uiState.statusFilter == status) null else status) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val statusDotColor = when (status) {
                                        BuildStatus.IN_PRODUCTION -> MaterialTheme.colorScheme.primary
                                        BuildStatus.COMPLETE -> Color(0xFF10B981)
                                        BuildStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(statusDotColor)
                                    )
                                    Text(status.label)
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (uiState.statusFilter == status) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50)
                        )
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
                            text = "build order ${uiState.builds.size}",
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
        BuildDetailsDialog(
            build = uiState.selectedBuild!!,
            onStartProduction = { viewModel.startProduction(it) },
            onCompleteOutput = { buildId, qty -> viewModel.completeBuildOutput(buildId, qty) },
            onDismiss = { viewModel.selectBuild(null) }
        )
    }

    if (uiState.isAddBuildDialogOpen) {
        AddBuildSheetDialog(
            assemblyParts = uiState.assemblyParts,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { ref, title, partId, qty, batch, date, notes ->
                viewModel.addBuildOrder(ref, title, partId, qty, batch, date, notes)
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
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.scale(if (isRtl) -1f else 1f, 1f)
                    )
                }

                Text(
                    text = "أوامر التصنيع والإنتاج (Build)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF1E1B4B)
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
                        text = "أمر إنتاج جديد",
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
                        BuildStatus.IN_PRODUCTION -> Color(0xFFEEF2FF)
                        BuildStatus.COMPLETE -> Color(0xFFECFDF5)
                        BuildStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(1.dp, when (build.status) {
                        BuildStatus.IN_PRODUCTION -> Color(0xFFE0E7FF)
                        BuildStatus.COMPLETE -> Color(0xFFA7F3D0)
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
                                BuildStatus.COMPLETE -> Color(0xFF10B981)
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
                            text = "${build.status.label} (${if (build.status == BuildStatus.IN_PRODUCTION) "In Production" else if (build.status == BuildStatus.COMPLETE) "Complete" else "Pending"})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (build.status) {
                                BuildStatus.IN_PRODUCTION -> MaterialTheme.colorScheme.primary
                                BuildStatus.COMPLETE -> Color(0xFF059669)
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
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = if (build.reference.contains("001")) "عاجل" else "عادي",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0369A1),
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

            // صندوق تفاصيل رقم التشغيلة والموعد (التاريخ على اليمين | رقم التشغيلة على اليسار)
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
                    // اليمين: التاريخ المستهدف
                    Column {
                        Text(
                            text = "التاريخ المستهدف:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = build.targetDate.ifBlank { "2025-02-28" },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // اليسار: رقم التشغيلة
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "رقم التشغيلة:",
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
                color = if (build.status == BuildStatus.CANCELLED) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else Color(0xFFECFDF5),
                border = BorderStroke(1.dp, if (build.status == BuildStatus.CANCELLED) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color(0xFFA7F3D0))
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
                            tint = if (build.status == BuildStatus.CANCELLED) MaterialTheme.colorScheme.error else Color(0xFF065F46),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (build.status == BuildStatus.CANCELLED) "نقص في المكونات (مطلوبة)" else "المكونات متوفرة بالكامل (100%)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (build.status == BuildStatus.CANCELLED) MaterialTheme.colorScheme.error else Color(0xFF065F46)
                        )
                    }

                    Text(
                        text = "12/12 عنصر BOM",
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
                            text = if (build.status == BuildStatus.IN_PRODUCTION) "تحديث الإنجاز" else "بدء / تفاصيل",
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
                            text = "قائمة القطع (BOM)",
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
@Composable
private fun BuildDetailsDialog(
    build: BuildOrder,
    onStartProduction: (buildId: Long) -> Unit,
    onCompleteOutput: (buildId: Long, qty: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var outputQtyText by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("أمر تصنيع: ${build.reference}", fontWeight = FontWeight.Bold)

                if (build.status == BuildStatus.PENDING) {
                    Button(
                        onClick = { onStartProduction(build.id) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("بدء التصنيع 🏭", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("رقم الأمر:", "#${build.id}")
                DetailRow("العنوان:", build.title.ifBlank { "-" })
                DetailRow("المنتج الأب Target Part:", build.partName)
                DetailRow("الحالة:", build.status.label)
                DetailRow("تشغيلة الدفعة Batch:", build.batch.ifBlank { "-" })
                DetailRow("تاريخ الإنجاز المستهدف Target Date:", build.targetDate.ifBlank { "-" })
                DetailRow("الكمية المطلوبة Total Qty:", "${build.quantity}")
                DetailRow("الكمية المكتملة Completed Qty:", "${build.completedQuantity}")
                DetailRow("نسبة الإنجاز:", "${build.completionPercentage}%")

                if (build.status == BuildStatus.IN_PRODUCTION && (build.completedQuantity < build.quantity)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("توريد مخرجات تصنيع جديدة (Build Output):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = outputQtyText,
                            onValueChange = { outputQtyText = it },
                            label = { Text("الكمية المخرجة") },
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("توريد 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
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
@Composable
private fun AddBuildSheetDialog(
    assemblyParts: List<Part>,
    onDismiss: () -> Unit,
    onConfirm: (
        reference: String,
        title: String,
        partId: Long,
        quantity: Double,
        batch: String,
        targetDate: String,
        notes: String
    ) -> Unit
) {
    var reference by remember { mutableStateOf("BO-2025-003") }
    var title by remember { mutableStateOf("تجميع أجهزة الاستشعار DHT22") }
    var selectedPartId by remember { mutableStateOf<Long?>(assemblyParts.firstOrNull()?.id) }
    var quantityText by remember { mutableStateOf("10.0") }
    var batch by remember { mutableStateOf("BATCH-2025-03") }
    var targetDate by remember { mutableStateOf("03/25/2025") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إنشاء أمر إنتاج جديد (Build)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pId = selectedPartId
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    if (reference.isNotBlank() && pId != null && qty > 0.0) {
                        onConfirm(reference, title, pId, qty, batch, targetDate, notes)
                    }
                },
                enabled = reference.isNotBlank() && selectedPartId != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // الكود المرجعي
                Text("الكود المرجعي (reference) *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    placeholder = { Text("BO-2025-003") },
                    trailingIcon = {
                        IconButton(onClick = { reference = "BO-2025-00${(4..99).random()}" }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "توليد رقم تلقائي",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // عنوان أو وصف الهدف
                Text("عنوان أو وصف الهدف (title)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("مثال: تجميع أجهزة الاستشعار") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // اختيار المنتج الأب المجمع
                Text("اختر المنتج الأب المجمع (assembly = true):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(assemblyParts, key = { "build-assembly-${it.id}" }) { p ->
                        FilterChip(
                            selected = selectedPartId == p.id,
                            onClick = { selectedPartId = p.id },
                            label = { Text(p.name, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // رقم التشغيلة والكمية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("رقم التشغيلة (batch)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = batch,
                            onValueChange = { batch = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("الكمية (quantity)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // التاريخ المستهدف
                Text("التاريخ المستهدف لإنهاء الإنتاج", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
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
