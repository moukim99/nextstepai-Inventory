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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nextstepai.inventory.ui.theme.AppIcons
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import nextstepai_inventory.shared.generated.resources.*

import com.nextstepai.inventory.repository.InflowPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private enum class WarehouseSubView {
    MAIN_DASHBOARD,
    STOCK,
    PARTS,
    STOCK_INFLOW,
    LOCATIONS
}

/**
 * تمثيل عنصر خيارات مصادر التغذية المخزنية (InflowOptionItem)
 * تدعم الخيارات المدمجة مسبقاً والخيارات المخصصة المعرفة من قبل المستخدم.
 */
data class InflowOptionItem(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String = "",
    val iconVector: ImageVector? = null,
    val isCustom: Boolean = false
)

private val defaultInflowOptions = listOf(
    InflowOptionItem(
        id = "PURCHASE_ORDER",
        title = "استلام توريد خارجي",
        description = "مورد تجاري (supplierUuid) + أمر شراء معلق (purchaseOrderId) + كود صنف المورد (supplier_parts.sku)",
        iconVector = Icons.Default.ShoppingCart
    ),
    InflowOptionItem(
        id = "INTERNAL_BUILD",
        title = "استلام إنتاج محلي",
        description = "أمر تصنيع وبناء نشط (buildId عبر build_orders)، ويمثل استلام مخرجات التجميع النهائية أو نصف المصنعة",
        iconVector = Icons.Default.PrecisionManufacturing
    ),
    InflowOptionItem(
        id = "WAREHOUSE_TRANSFER",
        title = "تحويل بين الفروع والمستودعات",
        description = "موقع المصدر وموقع الوجهة (locationUuid) مع توثيق مسار الشحن الداخلي",
        iconVector = Icons.Default.SwapHoriz
    ),
    InflowOptionItem(
        id = "CUSTOMER_RETURN",
        title = "مرتجع مبيعات / عملاء",
        description = "العميل (customerUuid / customerId) + أمر البيع الأصلي (salesOrderId)",
        iconVector = Icons.Default.ShoppingCart
    ),
    InflowOptionItem(
        id = "MANUAL_ADJUSTMENT",
        title = "تسوية رصيد مباشر / جرد افتتاحي",
        description = "حركة مخزنية مباشرة دون مستند مسبق (تسجيل رصيد مستودع أولي أو فائض جرد)",
        iconVector = Icons.Default.Tune
    )
)

/**
 * مدير التفضيلات المقترنة بدورة استخدام التطبيق وبقاعدة البيانات الدائمة (InflowPreferencesManager)
 * يحفظ تفضيلات المستخدم للخيارات الهيكلية والخيارات المخصصة بشكل دائم في قاعدة البيانات محلياً وعبر الجلسات.
 */
object InflowPreferencesManager {
    var allInflowOptionsState by mutableStateOf(defaultInflowOptions)
    var pinnedInflowIdsState by mutableStateOf(listOf("PURCHASE_ORDER", "INTERNAL_BUILD"))
    var isLoaded = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun loadFromStorage(force: Boolean = false) {
        if (isLoaded && !force) return
        scope.launch {
            try {
                val (savedPinnedIds, savedCustomOptions) = InflowPreferencesRepository.loadPreferences()
                val mergedOptions = defaultInflowOptions + savedCustomOptions.filter { custom ->
                    defaultInflowOptions.none { it.id == custom.id }
                }
                allInflowOptionsState = mergedOptions
                pinnedInflowIdsState = savedPinnedIds.filter { id -> mergedOptions.any { it.id == id } }
                    .ifEmpty { listOf("PURCHASE_ORDER", "INTERNAL_BUILD") }
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateAllOptions(options: List<InflowOptionItem>) {
        allInflowOptionsState = options
        scope.launch {
            InflowPreferencesRepository.savePreferences(pinnedInflowIdsState, options)
        }
    }

    fun updatePinnedIds(ids: List<String>) {
        pinnedInflowIdsState = ids
        scope.launch {
            InflowPreferencesRepository.savePreferences(ids, allInflowOptionsState)
        }
    }
}

/**
 * شاشة المستودع الرئيسية: تعرض صفحة هبوط ذات بطاقتين تفاعليتين
 * تتيحان الانتقال المستقل إلى (قسم المخزون والمواقع) أو (قسم دليل القطع والمكونات)
 * بالإضافة إلى زر ورسالة "الإدخال السريع" المنبثق ذو الهياكل الثلاثة التكيفية والمخصصة.
 */
@Composable
fun WarehouseScreen(
    partViewModel: PartViewModel,
    stockViewModel: StockViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit = {}
) {
    var currentSubView by remember { mutableStateOf(WarehouseSubView.MAIN_DASHBOARD) }
    var isQuickEntryOpen by remember { mutableStateOf(false) }
    val inflowViewModel = remember { StockInflowViewModel() }
    var activeInflowOption by remember { mutableStateOf<InflowOptionItem?>(null) }

    LaunchedEffect(isQuickEntryOpen) {
        InflowPreferencesManager.loadFromStorage(force = true)
    }

    val stockUiState by stockViewModel.uiState.collectAsState()
    val partUiState by partViewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentSubView,
            label = "WarehouseSubViewTransition"
        ) { subView ->
            when (subView) {
                WarehouseSubView.MAIN_DASHBOARD -> {
                    Scaffold(
                        floatingActionButton = {
                            Box(
                                modifier = Modifier.padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
                            ) {
                                ExtendedFloatingActionButton(
                                    onClick = { isQuickEntryOpen = true },
                                    containerColor = Color(0xFF4F46E5),
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(18.dp),
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "الادخال السريع",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الادخال السريع",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                }
                            }
                        },
                        floatingActionButtonPosition = FabPosition.Start
                    ) { paddingValues ->
                        WarehouseMainDashboard(
                            stockItemsCount = stockUiState.stockItems.size,
                            locationsCount = stockUiState.locations.size,
                            partsCount = partUiState.parts.size,
                            categoriesCount = partUiState.categories.size,
                            onOpenStockClick = { currentSubView = WarehouseSubView.STOCK },
                            onOpenPartsClick = { currentSubView = WarehouseSubView.PARTS },
                            onOpenLocationsClick = { currentSubView = WarehouseSubView.LOCATIONS },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }

                    if (isQuickEntryOpen) {
                        QuickEntryBottomSheet(
                            allInflowOptions = InflowPreferencesManager.allInflowOptionsState,
                            pinnedInflowIds = InflowPreferencesManager.pinnedInflowIdsState,
                            onUpdateAllOptions = { InflowPreferencesManager.updateAllOptions(it) },
                            onUpdatePinnedIds = { InflowPreferencesManager.updatePinnedIds(it) },
                            onStartInflow = { option ->
                                activeInflowOption = option
                                isQuickEntryOpen = false
                                inflowViewModel.startSession(option.id, poReference = "PO-2025-001")
                                currentSubView = WarehouseSubView.STOCK_INFLOW
                            },
                            onDismiss = { isQuickEntryOpen = false }
                        )
                    }
                }
                WarehouseSubView.STOCK -> {
                    StockScreen(
                        viewModel = stockViewModel,
                        onBackClick = { currentSubView = WarehouseSubView.MAIN_DASHBOARD }
                    )
                }
                WarehouseSubView.PARTS -> {
                    PartManagementScreen(
                        viewModel = partViewModel,
                        onBackClick = { currentSubView = WarehouseSubView.MAIN_DASHBOARD }
                    )
                }
                WarehouseSubView.LOCATIONS -> {
                    LocationManagementScreen(
                        viewModel = stockViewModel,
                        onBackClick = { currentSubView = WarehouseSubView.MAIN_DASHBOARD }
                    )
                }
                WarehouseSubView.STOCK_INFLOW -> {
                    Popup(
                        onDismissRequest = { currentSubView = WarehouseSubView.MAIN_DASHBOARD },
                        properties = PopupProperties(focusable = true, clippingEnabled = false)
                    ) {
                        StockInflowScreen(
                            viewModel = inflowViewModel,
                            inflowTitle = activeInflowOption?.title ?: "استلام توريد خارجي",
                            poReference = "PO-2025-001",
                            onClose = { currentSubView = WarehouseSubView.MAIN_DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseMainDashboard(
    stockItemsCount: Int,
    locationsCount: Int,
    partsCount: Int,
    categoriesCount: Int,
    onOpenStockClick: () -> Unit,
    onOpenPartsClick: () -> Unit,
    onOpenLocationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. هيدر عنوان قسم المستودع
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Warehouse,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.nav_warehouse),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "مركز إدارة المكونات والوحدات المادية والمواقع التخزينية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "أقسام المستودع الرئيسية:",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // 2. بطاقة قسم المخزون والمواقع (Stock & Locations Card)
        ElevatedCard(
            onClick = onOpenStockClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Stock,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.card_stock_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.card_stock_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("📦 $stockItemsCount وحدة") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFEEF2FF),
                                labelColor = Color(0xFF3730A3)
                            ),
                            border = null
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("📍 $locationsCount موقع") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            ),
                            border = null
                        )
                    }

                    Button(
                        onClick = onOpenStockClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.open_stock),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // 3. بطاقة قسم دليل القطع والمكونات (Parts Catalog Card)
        ElevatedCard(
            onClick = onOpenPartsClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Parts,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.card_parts_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.card_parts_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("🧩 $partsCount قطعة") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFECFDF5),
                                labelColor = Color(0xFF065F46)
                            ),
                            border = null
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("🏷️ $categoriesCount فئات") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            ),
                            border = null
                        )
                    }

                    Button(
                        onClick = onOpenPartsClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.open_parts),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // 4. بطاقة قسم أماكن وهيكل التخزين (Storage Locations Card)
        ElevatedCard(
            onClick = onOpenLocationsClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "أماكن وهيكل التخزين",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "إدارة المستودعات، الأرفف، ضبط السعات، وطباعة ملصقات الـ QR الميدانية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("📍 $locationsCount موقع") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFFEF3C7),
                                labelColor = Color(0xFF92400E)
                            ),
                            border = null
                        )
                    }

                    Button(
                        onClick = onOpenLocationsClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "استعراض المواقع",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * النافذة المنبثقة (Bottom Sheet) لإجراءات "الإدخال السريع" بقسم المستودع:
 * تحوي العرض المقسم على 3 هياكل هرمية بنسب (40% - 40% - 20%) مع إمكانية التجميع التكيفي بنسبة 80% عند اختيار خيار غير مثبت.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickEntryBottomSheet(
    allInflowOptions: List<InflowOptionItem>,
    pinnedInflowIds: List<String>,
    onUpdateAllOptions: (List<InflowOptionItem>) -> Unit,
    onUpdatePinnedIds: (List<String>) -> Unit,
    onStartInflow: (InflowOptionItem) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedInflowId by remember { mutableStateOf<String?>(null) }
    var isInflowPickerOpen by remember { mutableStateOf(false) }

    val selectedOption = remember(selectedInflowId, allInflowOptions) {
        allInflowOptions.find { it.id == selectedInflowId }
    }
    val isSelectedOptionPinned = selectedInflowId == null || selectedInflowId in pinnedInflowIds

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
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF4F46E5).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF4F46E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "الادخال السريع",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "اختيار مسار ومصدر التغذية المخزنية",
                                style = MaterialTheme.typography.bodySmall,
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
            }

            // Title
            Text(
                text = "اختر مصدر التغذية المخزنية (Stock Inflow):",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = Color(0xFF334155)
            )

            // Selector Row (40% - 40% - 20% or 80% - 20% when unstarred option selected)
            if (isSelectedOptionPinned) {
                // Default Layout: 3 structures (40% | 40% | 20%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pinnedList = remember(pinnedInflowIds, allInflowOptions) {
                        allInflowOptions.filter { it.id in pinnedInflowIds }
                    }
                    val slot1 = pinnedList.getOrNull(0) ?: allInflowOptions.getOrNull(0)
                    val slot2 = pinnedList.getOrNull(1) ?: allInflowOptions.getOrNull(1)

                    // Structure 1 (40% - Slot 1)
                    if (slot1 != null) {
                        InflowStructureCard(
                            option = slot1,
                            isSelected = selectedInflowId == slot1.id,
                            onClick = {
                                selectedInflowId = slot1.id
                                onStartInflow(slot1)
                            },
                            modifier = Modifier.weight(0.40f)
                        )
                    }

                    // Structure 2 (40% - Slot 2)
                    if (slot2 != null) {
                        InflowStructureCard(
                            option = slot2,
                            isSelected = selectedInflowId == slot2.id,
                            onClick = {
                                selectedInflowId = slot2.id
                                onStartInflow(slot2)
                            },
                            modifier = Modifier.weight(0.40f)
                        )
                    }

                    // Structure 3 (20% - "المزيد")
                    Surface(
                        onClick = { isInflowPickerOpen = true },
                        modifier = Modifier
                            .weight(0.20f)
                            .height(76.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "المزيد",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "المزيد",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }
                }
            } else if (selectedOption != null) {
                // Collapsed Layout: Single structure (80%) + "المزيد" (20%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InflowStructureCard(
                        option = selectedOption,
                        isSelected = true,
                        onClick = { onStartInflow(selectedOption) },
                        badgeText = "خيار مؤقت مختار",
                        modifier = Modifier.weight(0.80f)
                    )

                    Surface(
                        onClick = { isInflowPickerOpen = true },
                        modifier = Modifier
                            .weight(0.20f)
                            .height(76.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "المزيد",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "المزيد",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

            // Action Buttons: Swapped (Cancel on Right, Save on Left in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedInflowId = null
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "إلغاء",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                Button(
                    onClick = {
                        val option = selectedOption ?: allInflowOptions.firstOrNull()
                        if (option != null) {
                            onStartInflow(option)
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حفظ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (isInflowPickerOpen) {
        StockInflowTypePickerDialog(
            allOptions = allInflowOptions,
            pinnedIds = pinnedInflowIds,
            selectedId = selectedInflowId,
            onOptionSelected = { option ->
                selectedInflowId = option.id
                isInflowPickerOpen = false
                onStartInflow(option)
            },
            onTogglePin = { optionId ->
                val currentPinned = pinnedInflowIds.toMutableList()
                if (optionId in currentPinned) {
                    if (currentPinned.size > 1) {
                        currentPinned.remove(optionId)
                    }
                } else {
                    if (currentPinned.size >= 2) {
                        currentPinned.removeAt(1)
                    }
                    currentPinned.add(optionId)
                }
                onUpdatePinnedIds(currentPinned)
            },
            onCreateOption = { newOption ->
                val updatedOptions = allInflowOptions + newOption
                onUpdateAllOptions(updatedOptions)
                selectedInflowId = newOption.id
            },
            onDeleteOption = { optionId ->
                if (allInflowOptions.size > 1) {
                    val updatedOptions = allInflowOptions.filter { it.id != optionId }
                    var updatedPinned = pinnedInflowIds.filter { it != optionId }
                    if (updatedPinned.size < 2 && updatedOptions.size >= 2) {
                        val unpinned = updatedOptions.filter { it.id !in updatedPinned }
                        if (unpinned.isNotEmpty()) {
                            updatedPinned = updatedPinned + unpinned.first().id
                        }
                    }
                    onUpdateAllOptions(updatedOptions)
                    onUpdatePinnedIds(updatedPinned)
                    if (selectedInflowId == optionId) {
                        selectedInflowId = null
                    }
                }
            },
            onDismiss = { isInflowPickerOpen = false }
        )
    }
}

/**
 * بطاقة هيكلية لحقلي الخيارين الدائمين (40% لكل هيكل أو 80% للهيكل المجمع)
 */
@Composable
private fun InflowStructureCard(
    option: InflowOptionItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (option.iconEmoji.isNotBlank()) {
                    Text(text = option.iconEmoji, fontSize = 18.sp)
                } else {
                    Icon(
                        imageVector = option.iconVector ?: Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            lineHeight = 15.sp
                        ),
                        color = Color(0xFF1E293B),
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF4F46E5).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * النافذة المنبثقة المخصصة (Modal Bottom Sheet) لاختيار ومزامنة وإدارة الخيارات المتاحة والمفضلة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockInflowTypePickerDialog(
    allOptions: List<InflowOptionItem>,
    pinnedIds: List<String>,
    selectedId: String?,
    onOptionSelected: (InflowOptionItem) -> Unit,
    onTogglePin: (String) -> Unit,
    onCreateOption: (InflowOptionItem) -> Unit,
    onDeleteOption: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var optionToDelete by remember { mutableStateOf<InflowOptionItem?>(null) }

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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "مصادر التغذية المخزنية",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "اضغط ⭐ لتحديد التفضيلات في الهيكلين الرئيسيّين",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isCreateDialogOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("خيار جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Options List
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                allOptions.forEach { option ->
                    val isSelected = option.id == selectedId
                    val isPinned = option.id in pinnedIds

                    Surface(
                        onClick = {
                            onOptionSelected(option)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (option.iconEmoji.isNotBlank()) {
                                    Text(text = option.iconEmoji, fontSize = 20.sp)
                                } else {
                                    Icon(
                                        imageVector = option.iconVector ?: Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF1E293B)
                                )

                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    color = Color(0xFF64748B),
                                    maxLines = 2
                                )
                            }

                            // Star Button for Pinning to Main 2 Structures
                            IconButton(onClick = { onTogglePin(option.id) }) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "تحديد كفضل في الهيكل",
                                    tint = if (isPinned) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Delete Button for all options (allowed if list has > 1 option)
                            if (allOptions.size > 1) {
                                IconButton(onClick = { optionToDelete = option }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف الخيار",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (isCreateDialogOpen) {
        CreateCustomInflowOptionDialog(
            onConfirm = { newOption ->
                onCreateOption(newOption)
                isCreateDialogOpen = false
            },
            onDismiss = { isCreateDialogOpen = false }
        )
    }

    if (optionToDelete != null) {
        AlertDialog(
            onDismissRequest = { optionToDelete = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "تأكيد حذف الخيار",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف خيار '${optionToDelete?.title}' من قائمة مصادر التغذية المخزنية؟",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val opt = optionToDelete
                        if (opt != null) {
                            onDeleteOption(opt.id)
                            optionToDelete = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { optionToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * نافذة حوار منبثقة بسيطة لإنشاء وإضافة خيار تغذية مخصص جديد
 */
@Composable
private fun CreateCustomInflowOptionDialog(
    onConfirm: (InflowOptionItem) -> Unit,
    onDismiss: () -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var emojiText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
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
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "إنشاء خيار تغذية جديد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF1E293B)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("عنوان الخيار *") },
                    placeholder = { Text("مثال: استلام هدايا وعينات") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emojiText,
                    onValueChange = { emojiText = it },
                    label = { Text("الأيقونة / الرمز (من الكيبورد) *") },
                    placeholder = { Text("اختر إيموجي مثل: 🎁 أو 📦 أو 🚚") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("الوصف *") },
                    placeholder = { Text("توصيف مختصر لمصدر التغذية...") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleText.isNotBlank()) {
                        val newOption = InflowOptionItem(
                            id = "CUSTOM_${Clock.System.now().toEpochMilliseconds()}",
                            title = titleText.trim(),
                            description = descriptionText.ifBlank { "خيار إدخال مخصص" },
                            iconEmoji = emojiText.ifBlank { "📦" },
                            isCustom = true
                        )
                        onConfirm(newOption)
                    }
                },
                enabled = titleText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White
                )
            ) {
                Text("حفظ الخيار", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء")
            }
        }
    )
}
