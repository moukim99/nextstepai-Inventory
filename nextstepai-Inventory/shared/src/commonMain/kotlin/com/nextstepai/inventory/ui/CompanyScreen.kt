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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.Company
import com.nextstepai.inventory.ui.theme.AppIcons
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_company
import nextstepai_inventory.shared.generated.resources.back
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.card_companies_title
import nextstepai_inventory.shared.generated.resources.companies_count
import nextstepai_inventory.shared.generated.resources.save
import nextstepai_inventory.shared.generated.resources.search_placeholder

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
                title = { Text(stringResource(Res.string.card_companies_title)) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(AppIcons.Back),
                            contentDescription = stringResource(Res.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.setAddDialogOpen(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(AppIcons.Add),
                            contentDescription = stringResource(Res.string.add_new_company),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.add_new_company))
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
                .padding(horizontal = 16.dp)
        ) {
            val isWide = this@BoxWithConstraints.maxWidth > 600.dp
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // شريط البحث المطور
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(AppIcons.Search),
                            contentDescription = stringResource(Res.string.search_placeholder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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
                            label = { Text("🛒 العملاء") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.companies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.companies_count, 0, 0),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = pluralStringResource(Res.plurals.companies_count, uiState.companies.size, uiState.companies.size),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.companies, key = { "company-${it.id}" }) { company ->
                            CompanyCard(
                                company = company,
                                onClick = { viewModel.setSelectedCompany(company) }
                            )
                        }
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
            onConfirm = { name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, curr ->
                viewModel.addCompany(name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, curr)
            }
        )
    }
}

@Composable
private fun CompanyCard(
    company: Company,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = company.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = company.currency,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (company.isSupplier) Badge("مورد (Supplier)", MaterialTheme.colorScheme.primaryContainer)
                if (company.isManufacturer) Badge("مصنع (Manufacturer)", MaterialTheme.colorScheme.tertiaryContainer)
                if (company.isCustomer) Badge("عميل (Customer)", MaterialTheme.colorScheme.secondaryContainer)
            }

            if (company.phone.isNotBlank() || company.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (company.phone.isNotBlank()) {
                        Text(text = "📞 ${company.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (company.email.isNotBlank()) {
                        Text(text = "✉️ ${company.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CompanyDetailsDialog(
    company: Company,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        title = { Text("بيانات الشركة: ${company.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailRow("رقم الشركة:", "#${company.id}")
                DetailRow("الاسم:", company.name)
                DetailRow("الوصف:", company.description.ifBlank { "-" })
                DetailRow("الموقع الإلكتروني:", company.website.ifBlank { "-" })
                DetailRow("الهاتف:", company.phone.ifBlank { "-" })
                DetailRow("البريد الإلكتروني:", company.email.ifBlank { "-" })
                DetailRow("العنوان:", company.address.ifBlank { "-" })
                DetailRow("جهة الاتصال:", company.contact.ifBlank { "-" })
                DetailRow("العملة المعتمدة:", company.currency)
                HorizontalDivider()
                DetailRow("مورد (Supplier):", if (company.isSupplier) "نعم" else "لا")
                DetailRow("مصنع (Manufacturer):", if (company.isManufacturer) "نعم" else "لا")
                DetailRow("عميل (Customer):", if (company.isCustomer) "نعم" else "لا")
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
        title = { Text(stringResource(Res.string.add_new_company), fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description, website, phone, email, address, contact, isSupplier, isManufacturer, isCustomer, currency)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشركة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف نشاط الشركة") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("رمز العملة المعتمدة (USD / EUR / SAR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSupplier, onCheckedChange = { isSupplier = it })
                    Text("مورد تجاري (Supplier)", fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isManufacturer, onCheckedChange = { isManufacturer = it })
                    Text("مصنع مكونات (Manufacturer)", fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCustomer, onCheckedChange = { isCustomer = it })
                    Text("عميل مشتريات (Customer)", fontSize = 12.sp)
                }
            }
        }
    )
}
