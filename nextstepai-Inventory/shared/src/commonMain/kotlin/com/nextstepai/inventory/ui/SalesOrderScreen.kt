package com.nextstepai.inventory.ui

import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
import androidx.compose.foundation.verticalScroll
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.SOStatus
import com.nextstepai.inventory.data.SalesOrder
import com.nextstepai.inventory.data.SalesOrderLineItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.repository.SalesOrderFulfillmentResult
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.save
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesOrderScreen(
    viewModel: SalesOrderViewModel = SalesOrderViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(25.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text("أوامر البيع ومحرك تلبية الطلبات", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("إدارة طلبات العملاء والتحويل التلقائي للتصنيع والشراء", fontSize = 11.5.sp, color = Color(0xFF64748B))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // شريط البحث والرسائل
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("بحث بالرمز المرجعي، العميل، أو الوصف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4F46E5), unfocusedBorderColor = Color(0xFFE2E8F0))
            )

            uiState.successMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                        Text(msg, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                    }
                }
            }

            // قائمة أوامر البيع
            Text("أوامر البيع المسجلة (${uiState.orders.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))

            if (uiState.orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("لا توجد أوامر بيع مسجلة حالياً", color = Color(0xFF64748B))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(uiState.orders, key = { index, order -> "so-item-${order.id}-$index" }) { _, order ->
                        SalesOrderCard(order = order, onClick = { viewModel.selectOrder(order) })
                    }
                }
            }
        }

            ExtendedFloatingActionButton(
                onClick = { viewModel.setAddOrderDialogOpen(true) },
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                text = { Text("أمر بيع جديد", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
            )
        }
    }

    if (uiState.selectedOrder != null) {
        SalesOrderDetailsBottomSheet(
            order = uiState.selectedOrder!!,
            onDismiss = { viewModel.selectOrder(null) },
            onFulfill = { order ->
                viewModel.addSalesOrderWithFulfillment(
                    order.reference,
                    order.customerId,
                    order.description,
                    order.targetDate,
                    order.orderCurrency,
                    order.lineItems
                )
            }
        )
    }

    if (uiState.isAddOrderDialogOpen) {
        AddSalesOrderBottomSheet(
            customers = uiState.customers,
            parts = uiState.parts,
            initialCustomerId = uiState.preselectedCustomerId,
            onDismiss = { viewModel.setAddOrderDialogOpen(false) },
            onConfirm = { ref, custId, desc, targetDate, curr, lines ->
                viewModel.addSalesOrderWithFulfillment(ref, custId, desc, targetDate, curr, lines)
            }
        )
    }

    uiState.lastFulfillmentResult?.let { result ->
        FulfillmentSummaryDialog(
            result = result,
            onDismiss = { viewModel.loadData() }
        )
    }
}

@Composable
private fun SalesOrderCard(
    order: SalesOrder,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEEF2FF)) {
                        Text("SO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF4F46E5), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text(order.reference, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp), color = Color(0xFF0F172A))
                }

                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFECFDF5), border = BorderStroke(1.dp, Color(0xFFA7F3D0))) {
                    Text(order.status.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF065F46), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            Text("العميل: ${order.customerName}", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color(0xFF0F172A))
            if (order.description.isNotBlank()) {
                Text(order.description, fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("عدد البنود: ${order.lineItemsCount} بنود", fontSize = 11.5.sp, color = Color(0xFF64748B))
                Text("إجمالي البيع: ${order.orderCurrency} ${order.totalPrice.formatMoney()}", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = Color(0xFF059669))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesOrderDetailsBottomSheet(
    order: SalesOrder,
    onDismiss: () -> Unit,
    onFulfill: (SalesOrder) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
                    }

                    Column {
                        Text(order.reference, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Text("العميل: ${order.customerName}", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("حالة الطلب:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFECFDF5), border = BorderStroke(1.dp, Color(0xFFA7F3D0))) {
                                Text(order.status.label, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF065F46), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("العميل المستهدف:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(order.customerName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تاريخ التسليم المتوقع:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(order.targetDate.ifBlank { "-" }, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي قيمة البيع:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("${order.orderCurrency} ${order.totalPrice.formatMoney()}", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = Color(0xFF059669))
                        }
                        if (order.description.isNotBlank()) {
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Text("الوصف والملاحظات:", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            Text(order.description, fontSize = 12.sp, color = Color(0xFF0F172A))
                        }
                    }
                }

                // Line Items Table
                Text("بنود ومحتويات أمر البيع (${order.lineItems.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))

                if (order.lineItems.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("لا توجد بنود مسجلة لهذا الأمر", modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color(0xFFB45309))
                    }
                } else {
                    order.lineItems.forEachIndexed { idx, line ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${idx + 1}. ${line.partName}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                                    Text("${order.orderCurrency} ${line.lineTotal.formatMoney()}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF059669))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الكمية المطلوبة: ${line.quantity} وحدة", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    Text("سعر الوحدة: ${order.orderCurrency} ${line.unitPrice.formatMoney()}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المحجوز من المخزن: ${line.allocatedQuantity} وحدة", fontSize = 11.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
                                    Text("المشحون للعميل: ${line.shippedQuantity} وحدة", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onFulfill(order)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    modifier = Modifier.weight(2f)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("تشغيل محرك التلبية الآلي", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSalesOrderBottomSheet(
    customers: List<Company>,
    parts: List<Part>,
    initialCustomerId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (reference: String, customerId: Long, description: String, targetDate: String, currency: String, lines: List<SalesOrderLineItem>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var reference by remember { mutableStateOf("SO-2025-101") }
    var selectedCustomerId by remember(initialCustomerId) { mutableStateOf<Long?>(initialCustomerId ?: customers.firstOrNull()?.id) }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("2025-04-15") }
    var currency by remember { mutableStateOf("USD") }

    val initialLines = remember { mutableStateListOf<SalesOrderLineItem>() }

    var itemPartId by remember { mutableStateOf<Long?>(parts.firstOrNull()?.id) }
    var itemQuantityText by remember { mutableStateOf("1.0") }
    var itemPriceText by remember { mutableStateOf("0.0") }

    val totalPrice = remember(initialLines.toList()) { initialLines.sumOf { it.quantity * it.unitPrice } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEF2FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(22.dp))
                    }
                    Text("تسجيل أمر بيع جديد للعميل", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("الرمز المرجعي لأمر البيع (Reference) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                val selectedCustomer = customers.find { it.id == selectedCustomerId }
                var isSelectCustomerSheetOpen by remember { mutableStateOf(false) }

                Text("اختر العميل المستهدف *:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))

                Surface(
                    onClick = { isSelectCustomerSheetOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.5.dp, if (selectedCustomer != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
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
                                    .border(BorderStroke(1.dp, Color(0xFFC7D2FE)), RoundedCornerShape(12.dp)),
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
                                    text = selectedCustomer?.name ?: "اختر العميل المستهدف لأمر البيع *",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (selectedCustomer != null) "العملة: ${selectedCustomer.currency} | رقم العميل: #${selectedCustomer.id}" else "اضغط للبحث واختيار العميل من القائمة",
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
                                text = if (selectedCustomer != null) "تغيير" else "اختر ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (isSelectCustomerSheetOpen) {
                    SelectCustomerBottomSheet(
                        customers = customers,
                        selectedCustomerId = selectedCustomerId,
                        onDismiss = { isSelectCustomerSheetOpen = false },
                        onSelect = { cust ->
                            if (cust != null) {
                                selectedCustomerId = cust.id
                                currency = cust.currency
                            }
                            isSelectCustomerSheetOpen = false
                        }
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الطلب وهدف الشحن") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                                contentDescription = "اختر التاريخ من التقويم",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTargetDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
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

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text("بنود ومكونات أصل طلب البيع:", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val selectedPart = parts.find { it.id == itemPartId }
                        var isSelectPartSheetOpen by remember { mutableStateOf(false) }

                        Text("إضافة منتج/مادة لأمر البيع:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF4F46E5))

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
                                            text = selectedPart?.let { "${it.name} (${if (it.assembly) "تصنيع" else "مكون جاهز"})" } ?: "اختر المنتج/المادة *",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (selectedPart != null) "IPN: ${selectedPart.ipn.ifBlank { "-" }} | المخزون المتوفر: ${selectedPart.totalInStock} ${selectedPart.units}" else "اضغط للبحث واختيار المنتج من القائمة",
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

                        if (isSelectPartSheetOpen) {
                            SelectPartForSOBottomSheet(
                                parts = parts,
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

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = itemQuantityText,
                                onValueChange = { itemQuantityText = it },
                                label = { Text("الكمية المطلوبة *") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = itemPriceText,
                                onValueChange = { itemPriceText = it },
                                label = { Text("سعر البيع ($currency) *") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val pt = parts.find { it.id == itemPartId }
                                val q = itemQuantityText.toDoubleOrNull() ?: 1.0
                                val p = itemPriceText.toDoubleOrNull() ?: 0.0
                                if (pt != null && q > 0.0 && p >= 0.0) {
                                    initialLines.add(
                                        SalesOrderLineItem(
                                            orderId = 0L,
                                            partId = pt.id,
                                            partName = pt.name,
                                            quantity = q,
                                            unitPrice = p
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ إضافة المادة لأمر البيع", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (initialLines.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("البنود المضافة (${initialLines.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF065F46))
                                Text("إجمالي البيع: $currency ${totalPrice.formatMoney()}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF047857))
                            }
                            initialLines.forEachIndexed { idx, line ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${idx + 1}. ${line.partName} (${line.quantity} × $currency ${line.unitPrice.formatMoney()})", fontSize = 11.5.sp, color = Color(0xFF0F172A))
                                    IconButton(onClick = { initialLines.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val cId = selectedCustomerId
                            if (reference.isNotBlank() && cId != null && initialLines.isNotEmpty()) {
                                onConfirm(reference, cId, description, targetDate, currency, initialLines.toList())
                            }
                        },
                        enabled = reference.isNotBlank() && selectedCustomerId != null && initialLines.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("اعتماد وتلبية الأمر آلياً", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FulfillmentSummaryDialog(
    result: SalesOrderFulfillmentResult,
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(28.dp))
                Text("نتائج محرك التلبية الآلي (Order Fulfillment Engine)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(result.summaryMessage, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF065F46))

                if (result.buildOrdersCreated.isNotEmpty()) {
                    Text("أوامر التصنيع التلقائية المنشأة (${result.buildOrdersCreated.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF4F46E5))
                    result.buildOrdersCreated.forEach { bo ->
                        Text("• ${bo.reference}: ${bo.title} (${bo.quantity} وحدة)", fontSize = 11.5.sp)
                    }
                }

                if (result.purchaseOrdersCreated.isNotEmpty()) {
                    Text("أوامر الشراء التلقائية المنشأة (${result.purchaseOrdersCreated.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD97706))
                    result.purchaseOrdersCreated.forEach { po ->
                        Text("• ${po.reference}: ${po.description}", fontSize = 11.5.sp)
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("موافق ومتابعة", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectCustomerBottomSheet(
    customers: List<Company>,
    selectedCustomerId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Company?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.currency.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اختر العميل المستهدف لأمر البيع", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم العميل أو العملة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(filteredCustomers, key = { index, cust -> "so-cust-${cust.id}-$index" }) { _, cust ->
                    val isSel = selectedCustomerId == cust.id
                    Surface(
                        onClick = { onSelect(cust) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("العملة: ${cust.currency} | رقم العميل: #${cust.id}", fontSize = 11.5.sp, color = Color(0xFF64748B))
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
private fun SelectPartForSOBottomSheet(
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
        Column(modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اختر المنتج/المادة لأمر البيع", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المنتج أو IPN...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(filteredParts, key = { index, pt -> "so-pt-${pt.id}-$index" }) { _, pt ->
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
                                Text("${pt.name} (${if (pt.assembly) "تصنيع داخلي" else "مكون جاهز"})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("IPN: ${pt.ipn.ifBlank { "-" }} | المخزون: ${pt.totalInStock} ${pt.units}", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                            if (isSel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
