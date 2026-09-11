package com.nextstepai.inventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.Company

/**
 * شاشة إدارة الشركات والعلاقات التجارية (Company Management Screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyScreen(
    viewModel: CompanyViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الشركات والعلاقات (Company)") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("➔ العودة", fontSize = 14.sp)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.setAddDialogOpen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ إضافة شركة")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // شريط البحث المطور
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("البحث باسم الشركة، الوصف، البريد، أو جهة الاتصال...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // شريط تصفية الأدوار (All, Suppliers, Manufacturers, Customers)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item(key = "filter-role-all") {
                    FilterChip(
                        selected = uiState.roleFilter == CompanyRoleFilter.ALL,
                        onClick = { viewModel.setRoleFilter(CompanyRoleFilter.ALL) },
                        label = { Text("الكل") }
                    )
                }
                item(key = "filter-role-suppliers") {
                    FilterChip(
                        selected = uiState.roleFilter == CompanyRoleFilter.SUPPLIER_ONLY,
                        onClick = { viewModel.setRoleFilter(CompanyRoleFilter.SUPPLIER_ONLY) },
                        label = { Text("🚚 الموردون") }
                    )
                }
                item(key = "filter-role-manufacturers") {
                    FilterChip(
                        selected = uiState.roleFilter == CompanyRoleFilter.MANUFACTURER_ONLY,
                        onClick = { viewModel.setRoleFilter(CompanyRoleFilter.MANUFACTURER_ONLY) },
                        label = { Text("🏭 المصنّعون") }
                    )
                }
                item(key = "filter-role-customers") {
                    FilterChip(
                        selected = uiState.roleFilter == CompanyRoleFilter.CUSTOMER_ONLY,
                        onClick = { viewModel.setRoleFilter(CompanyRoleFilter.CUSTOMER_ONLY) },
                        label = { Text("🤝 العملاء") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "⚠️ ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 12.sp
                    )
                }
            }

            // قائمة الشركات مع استخدام المفاتيح الثابتة المستقرة key()
            if (uiState.companies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد شركات مسجلة تطابق شروط البحث.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.companies, key = { "company-${it.id}" }) { company ->
                        CompanyItemCard(
                            company = company,
                            onClick = { viewModel.setSelectedCompany(company) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.selectedCompany != null) {
        CompanyDetailsDialog(
            company = uiState.selectedCompany!!,
            onDismiss = { viewModel.setSelectedCompany(null) }
        )
    }

    if (uiState.isAddCompanyDialogOpen) {
        AddCompanyDialog(
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, currency ->
                viewModel.addCompany(name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, currency)
            }
        )
    }
}

@Composable
private fun CompanyItemCard(
    company: Company,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = company.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "العملة: ${company.currency}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (company.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = company.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // شارات الأدوار التجاربة (Roles Badges)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (company.isSupplier) RoleBadge("مورّد (Supplier)", MaterialTheme.colorScheme.primaryContainer)
                if (company.isManufacturer) RoleBadge("مصنّع (Manufacturer)", MaterialTheme.colorScheme.tertiaryContainer)
                if (company.isCustomer) RoleBadge("عميل (Customer)", MaterialTheme.colorScheme.secondaryContainer)
            }

            if (company.email.isNotBlank() || company.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (company.email.isNotBlank()) {
                        Text(text = "✉️ ${company.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (company.phone.isNotBlank()) {
                        Text(text = "📞 ${company.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(text = text, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun CompanyDetailsDialog(
    company: Company,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text(company.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailRow("رقم السجل (id):", "#${company.id}")
                DetailRow("الاسم التجاري:", company.name)
                DetailRow("الوصف:", company.description.ifBlank { "-" })
                DetailRow("الموقع الإلكتروني:", company.website.ifBlank { "-" })
                DetailRow("رقم الهاتف:", company.phone.ifBlank { "-" })
                DetailRow("البريد الإلكتروني:", company.email.ifBlank { "-" })
                DetailRow("العنوان الجغرافي:", company.address.ifBlank { "-" })
                DetailRow("جهة الاتصال الأساسية:", company.contact.ifBlank { "-" })
                DetailRow("العملة الافتراضية:", company.currency)
                HorizontalDivider()
                DetailRow("مورّد (is_supplier):", if (company.isSupplier) "نعم" else "لا")
                DetailRow("مصنّع (is_manufacturer):", if (company.isManufacturer) "نعم" else "لا")
                DetailRow("عميل (is_customer):", if (company.isCustomer) "نعم" else "لا")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AddCompanyDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        description: String,
        website: String,
        phone: String,
        email: String,
        address: String,
        contact: String,
        isSupplier: Boolean,
        isManufacturer: Boolean,
        isCustomer: Boolean,
        currency: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var isSupplier by remember { mutableStateOf(true) }
    var isManufacturer by remember { mutableStateOf(false) }
    var isCustomer by remember { mutableStateOf(false) }
    var currency by remember { mutableStateOf("USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة شركة جديدة (Company)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description, website, phone, email, address, contact, isSupplier, isManufacturer, isCustomer, currency)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("حفظ الشركة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشركة (name) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف (description)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("الهاتف") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("العملة (e.g. USD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني (email)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان (address)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("اسم جهة الاتصال الأساسية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSupplier, onCheckedChange = { isSupplier = it })
                    Text("مورّد (is_supplier)", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isManufacturer, onCheckedChange = { isManufacturer = it })
                    Text("مصنّع (is_manufacturer)", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCustomer, onCheckedChange = { isCustomer = it })
                    Text("عميل (is_customer)", fontSize = 12.sp)
                }
            }
        }
    )
}
