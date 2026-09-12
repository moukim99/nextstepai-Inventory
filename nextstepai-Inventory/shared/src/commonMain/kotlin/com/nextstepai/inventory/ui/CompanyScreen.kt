package com.nextstepai.inventory.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.*
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_company
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.companies_count
import nextstepai_inventory.shared.generated.resources.save

/**
 * شاشة إدارة الشركات والعلاقات التجارية (Company Management Screen).
 * تدعم إدارة الشركات والكيانات الفرعية: جهات الاتصال، العناوين، مرفقات الشركات، قطع المصنع والمعاملات التقنية والمرفقات، وقطع الموردين والأسعار.
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
            CompaniesTopBar(
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

                // شريط البحث المطور
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "البحث باسم الشركة، الوصف، البريد أو الهاتف...",
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

                // شريط تصفية أدوار الشركات
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "filter-role-all") {
                        FilterChip(
                            selected = uiState.roleFilter == CompanyRoleFilter.ALL,
                            onClick = { viewModel.setRoleFilter(CompanyRoleFilter.ALL) },
                            label = { Text("الكل", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                    item(key = "filter-role-suppliers") {
                        FilterChip(
                            selected = uiState.roleFilter == CompanyRoleFilter.SUPPLIER_ONLY,
                            onClick = { viewModel.setRoleFilter(CompanyRoleFilter.SUPPLIER_ONLY) },
                            label = { Text("🚚 الموردون", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                    item(key = "filter-role-manufacturers") {
                        FilterChip(
                            selected = uiState.roleFilter == CompanyRoleFilter.MANUFACTURER_ONLY,
                            onClick = { viewModel.setRoleFilter(CompanyRoleFilter.MANUFACTURER_ONLY) },
                            label = { Text("🏭 المصنّعون", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                    item(key = "filter-role-customers") {
                        FilterChip(
                            selected = uiState.roleFilter == CompanyRoleFilter.CUSTOMER_ONLY,
                            onClick = { viewModel.setRoleFilter(CompanyRoleFilter.CUSTOMER_ONLY) },
                            label = { Text("🛒 العملاء", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(Res.plurals.companies_count, uiState.companies.size, uiState.companies.size),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
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
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.setSelectedCompany(null) }
        )
    }

    if (uiState.isAddCompanyDialogOpen) {
        AddCompanyDialog(
            companies = uiState.companies,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, curr, parentId ->
                viewModel.addCompany(name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, curr, parentId)
            }
        )
    }

    if (uiState.isAddContactDialogOpen) {
        AddContactDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddContactDialogOpen(false) },
            onConfirm = { name, phone, email, role ->
                viewModel.addContact(name, phone, email, role)
            }
        )
    }

    if (uiState.isAddAddressDialogOpen) {
        AddAddressDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddAddressDialogOpen(false) },
            onConfirm = { title, isPrimary, line1, line2, postalCode, city, province, country, notes ->
                viewModel.addAddress(title, isPrimary, line1, line2, postalCode, city, province, country, notes)
            }
        )
    }

    if (uiState.isAddCompanyAttachmentDialogOpen) {
        AddCompanyAttachmentDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddCompanyAttachmentDialogOpen(false) },
            onConfirm = { path, link, comment ->
                viewModel.addCompanyAttachment(path, link, comment)
            }
        )
    }

    if (uiState.isAddManufacturerPartDialogOpen) {
        AddManufacturerPartDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddManufacturerPartDialogOpen(false) },
            onConfirm = { partId, mpn, desc, link ->
                viewModel.addManufacturerPart(partId, mpn, desc, link)
            }
        )
    }

    if (uiState.isAddManufacturerPartParameterDialogOpen) {
        AddManufacturerPartParameterDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddManufacturerPartParameterDialogOpen(false) },
            onConfirm = { name, value, units ->
                viewModel.addManufacturerPartParameter(name, value, units)
            }
        )
    }

    if (uiState.isAddManufacturerPartAttachmentDialogOpen) {
        AddManufacturerPartAttachmentDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddManufacturerPartAttachmentDialogOpen(false) },
            onConfirm = { path, link, comment ->
                viewModel.addManufacturerPartAttachment(path, link, comment)
            }
        )
    }

    if (uiState.isAddSupplierPartDialogOpen) {
        AddSupplierPartDialog(
            manufacturerParts = uiState.companyManufacturerParts,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddSupplierPartDialogOpen(false) },
            onConfirm = { partId, sku, mfgPartId, desc, link, note, pkg, packQty ->
                viewModel.addSupplierPart(partId, sku, mfgPartId, desc, link, note, pkg, packQty)
            }
        )
    }

    if (uiState.isAddPriceBreakDialogOpen) {
        AddPriceBreakDialog(
            defaultCurrency = uiState.selectedCompany?.currency ?: "USD",
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddPriceBreakDialogOpen(false) },
            onConfirm = { qty, price, currency ->
                viewModel.addPriceBreak(qty, price, currency)
            }
        )
    }
}

@Composable
private fun CompaniesTopBar(
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
                    text = "جدول الشركات والعلاقات",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.add_new_company),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "تسجيل شركة",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CorporateFare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = company.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = company.currency,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (company.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = company.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (company.isSupplier) BadgeTag("مورد (Supplier)", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                if (company.isManufacturer) BadgeTag("مصنع (Manufacturer)", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                if (company.isCustomer) BadgeTag("عميل (Customer)", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun BadgeTag(text: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CompanyDetailsDialog(
    company: Company,
    uiState: CompanyUiState,
    viewModel: CompanyViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق", fontWeight = FontWeight.Bold) }
        },
        title = {
            Column {
                Text(text = company.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                // التبويبات الفرعية التفصيلية
                ScrollableTabRow(
                    selectedTabIndex = uiState.activeDetailTab.ordinal,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    Tab(
                        selected = uiState.activeDetailTab == CompanyDetailTab.INFO,
                        onClick = { viewModel.setDetailTab(CompanyDetailTab.INFO) },
                        text = { Text("عام", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = uiState.activeDetailTab == CompanyDetailTab.CONTACTS,
                        onClick = { viewModel.setDetailTab(CompanyDetailTab.CONTACTS) },
                        text = { Text("الاتصال (${uiState.companyContacts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = uiState.activeDetailTab == CompanyDetailTab.ADDRESSES,
                        onClick = { viewModel.setDetailTab(CompanyDetailTab.ADDRESSES) },
                        text = { Text("العناوين (${uiState.companyAddresses.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    if (company.isManufacturer) {
                        Tab(
                            selected = uiState.activeDetailTab == CompanyDetailTab.MANUFACTURER_PARTS,
                            onClick = { viewModel.setDetailTab(CompanyDetailTab.MANUFACTURER_PARTS) },
                            text = { Text("قطع المصنع (${uiState.companyManufacturerParts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                    if (company.isSupplier) {
                        Tab(
                            selected = uiState.activeDetailTab == CompanyDetailTab.SUPPLIER_PARTS,
                            onClick = { viewModel.setDetailTab(CompanyDetailTab.SUPPLIER_PARTS) },
                            text = { Text("قطع المورد (${uiState.companySupplierParts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                when (uiState.activeDetailTab) {
                    CompanyDetailTab.INFO -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailRow("رقم الشركة:", "#${company.id}")
                            DetailRow("الاسم الرسمي:", company.name)
                            DetailRow("الوصف النشاط:", company.description.ifBlank { "-" })
                            DetailRow("الموقع الإلكتروني:", company.website.ifBlank { "-" })
                            DetailRow("الهاتف الأساسي:", company.phone.ifBlank { "-" })
                            DetailRow("البريد الإلكتروني:", company.email.ifBlank { "-" })
                            DetailRow("العنوان الرئيسي:", company.address.ifBlank { "-" })
                            DetailRow("جهة الاتصال الأساسية:", company.contact.ifBlank { "-" })
                            DetailRow("العملة المعتمدة:", company.currency)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DetailRow("دور المورد (Supplier):", if (company.isSupplier) "نعم" else "لا")
                            DetailRow("دور المصنع (Manufacturer):", if (company.isManufacturer) "نعم" else "لا")
                            DetailRow("دور العميل (Customer):", if (company.isCustomer) "نعم" else "لا")

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("مرفقات ووثائق الشركة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                IconButton(onClick = { viewModel.setAddCompanyAttachmentDialogOpen(true) }) {
                                    Icon(Icons.Default.AttachFile, contentDescription = "إضافة وثيقة", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (uiState.companyAttachments.isEmpty()) {
                                Text("لا توجد وثائق رسمية مرفقة للشركة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                uiState.companyAttachments.forEach { att ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📄 ${att.comment.ifBlank { "وثيقة رسمية" }}", fontSize = 11.sp)
                                        IconButton(onClick = { viewModel.deleteCompanyAttachment(att.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "حذف المرفق", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CompanyDetailTab.CONTACTS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قائمة مسؤولي التواصل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { viewModel.setAddContactDialogOpen(true) }) {
                                    Icon(Icons.Default.Add, contentDescription = "إضافة شخص")
                                }
                            }
                            if (uiState.companyContacts.isEmpty()) {
                                Text("لا يوجد مسؤولو تواصل مضافون حالياً", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(uiState.companyContacts) { c ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    if (c.role.isNotBlank()) Text(c.role, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                    if (c.phone.isNotBlank()) Text("📞 ${c.phone}", fontSize = 11.sp)
                                                    if (c.email.isNotBlank()) Text("✉️ ${c.email}", fontSize = 11.sp)
                                                }
                                                IconButton(onClick = { viewModel.deleteContact(c.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CompanyDetailTab.ADDRESSES -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("عناوين الفروع والشحن", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { viewModel.setAddAddressDialogOpen(true) }) {
                                    Icon(Icons.Default.Add, contentDescription = "إضافة عنوان")
                                }
                            }
                            if (uiState.companyAddresses.isEmpty()) {
                                Text("لا توجد عناوين مسجلة حالياً", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(uiState.companyAddresses) { addr ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(addr.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        if (addr.isPrimary) BadgeTag("الرئيسي", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                                    }
                                                    Text("${addr.line1} ${addr.city} ${addr.country}".trim(), fontSize = 11.sp)
                                                }
                                                IconButton(onClick = { viewModel.deleteAddress(addr.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CompanyDetailTab.MANUFACTURER_PARTS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قطع المصنّع والمرفقات (MPN)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { viewModel.setAddManufacturerPartDialogOpen(true) }) {
                                    Icon(Icons.Default.Add, contentDescription = "إضافة قطعة مصنع")
                                }
                            }
                            if (uiState.companyManufacturerParts.isEmpty()) {
                                Text("لا توجد قطع مصنّع مرادفة مسجلة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(uiState.companyManufacturerParts) { mp ->
                                        val isSelected = uiState.selectedManufacturerPart?.id == mp.id
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.setSelectedManufacturerPart(if (isSelected) null else mp) },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("MPN: ${mp.mpn}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                                        if (mp.description.isNotBlank()) Text(mp.description, fontSize = 11.sp)
                                                    }
                                                    Row {
                                                        IconButton(onClick = {
                                                            viewModel.setSelectedManufacturerPart(mp)
                                                            viewModel.setAddManufacturerPartParameterDialogOpen(true)
                                                        }) {
                                                            Icon(Icons.Default.Tune, contentDescription = "إضافة معامل تقني", tint = MaterialTheme.colorScheme.secondary)
                                                        }
                                                        IconButton(onClick = {
                                                            viewModel.setSelectedManufacturerPart(mp)
                                                            viewModel.setAddManufacturerPartAttachmentDialogOpen(true)
                                                        }) {
                                                            Icon(Icons.Default.AttachFile, contentDescription = "إضافة مرفق", tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                        IconButton(onClick = { viewModel.deleteManufacturerPart(mp.id) }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                }

                                                if (isSelected && uiState.selectedManufacturerPartParameters.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("المعاملات التقنية للقطعة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    uiState.selectedManufacturerPartParameters.forEach { p ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("⚙️ ${p.name}", fontSize = 11.sp)
                                                            Text("${p.value} ${p.units}".trim(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }

                                                if (isSelected && uiState.selectedManufacturerPartAttachments.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("المرفقات وأوراق البيانات:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    uiState.selectedManufacturerPartAttachments.forEach { att ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("📎 ${att.comment.ifBlank { "مرفق مواصفات" }}", fontSize = 11.sp)
                                                            IconButton(
                                                                onClick = { viewModel.deleteManufacturerPartAttachment(att.id) },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.Close, contentDescription = "حذف مرفق", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
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

                    CompanyDetailTab.SUPPLIER_PARTS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قطع المورد والأسعار (SKU)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { viewModel.setAddSupplierPartDialogOpen(true) }) {
                                    Icon(Icons.Default.Add, contentDescription = "إضافة قطعة مورد")
                                }
                            }
                            if (uiState.companySupplierParts.isEmpty()) {
                                Text("لا توجد قطع موردين مسجلة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(uiState.companySupplierParts) { sp ->
                                        val isSelected = uiState.selectedSupplierPart?.id == sp.id
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.setSelectedSupplierPart(if (isSelected) null else sp) },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("SKU: ${sp.sku}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                                        if (sp.packaging.isNotBlank()) Text("تغليف: ${sp.packaging} (${sp.packQuantity})", fontSize = 11.sp)
                                                    }
                                                    Row {
                                                        IconButton(onClick = {
                                                            viewModel.setSelectedSupplierPart(sp)
                                                            viewModel.setAddPriceBreakDialogOpen(true)
                                                        }) {
                                                            Icon(Icons.Default.AttachMoney, contentDescription = "إضافة سعر", tint = MaterialTheme.colorScheme.secondary)
                                                        }
                                                        IconButton(onClick = { viewModel.deleteSupplierPart(sp.id) }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                }

                                                if (isSelected && uiState.supplierPartPriceBreaks.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("شرائح الأسعار حسب الكمية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    uiState.supplierPartPriceBreaks.forEach { pb ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("الكمية ≥ ${pb.quantity}", fontSize = 11.sp)
                                                            Text("${pb.price} ${pb.priceCurrency}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
    companies: List<Company>,
    errorMessage: String?,
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
        currency: String,
        parentId: Long?
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
    var selectedParentId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.add_new_company), fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description, website, phone, email, address, contact, isSupplier, isManufacturer, isCustomer, currency, selectedParentId)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(Res.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشركة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف نشاط الشركة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (companies.isNotEmpty()) {
                    Text("الشركة الأم (اختياري للهيكلية الهرمية):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedParentId == null,
                            onClick = { selectedParentId = null },
                            label = { Text("بدون أم", fontSize = 10.sp) }
                        )
                        companies.take(3).forEach { comp ->
                            FilterChip(
                                selected = selectedParentId == comp.id,
                                onClick = { selectedParentId = comp.id },
                                label = { Text(comp.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("رمز العملة (USD / EUR / SAR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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

@Composable
private fun AddCompanyAttachmentDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (attachmentPath: String, link: String, comment: String) -> Unit
) {
    var path by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("عقد توريد / سجل تجاري") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة وثيقة / مرفق للشركة", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (path.isNotBlank() || link.isNotBlank()) onConfirm(path, link, comment)
                },
                enabled = path.isNotBlank() || link.isNotBlank()
            ) { Text("حفظ المرفق") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("وصف الوثيقة (سجل تجاري/عقد)") }, singleLine = true)
                OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("مسار الملف (Contract.pdf)") }, singleLine = true)
                OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("أو رابط المستند الإلكتروني") }, singleLine = true)
            }
        }
    )
}

@Composable
private fun AddContactDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, role: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة جهة اتصال جديدة", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, phone, email, role) },
                enabled = name.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم الكامل *") }, singleLine = true)
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("المسمى الوظيفي / الدور") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("الهاتف") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") }, singleLine = true)
            }
        }
    )
}

@Composable
private fun AddAddressDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, isPrimary: Boolean, line1: String, line2: String, postalCode: String, city: String, province: String, country: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("الفرع الرئيسي") }
    var isPrimary by remember { mutableStateOf(false) }
    var line1 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عنوان جديد", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = { if (line1.isNotBlank()) onConfirm(title, isPrimary, line1, "", "", city, "", country, notes) },
                enabled = line1.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("تسمية العنوان") }, singleLine = true)
                OutlinedTextField(value = line1, onValueChange = { line1 = it }, label = { Text("السطر الأول (الشارع/المبنى) *") }, singleLine = true)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("المدينة") }, singleLine = true)
                OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("الدولة") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it })
                    Text("تعيين كعنوان رئيسي للمستندات", fontSize = 12.sp)
                }
            }
        }
    )
}

@Composable
private fun AddManufacturerPartDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, mpn: String, description: String, link: String) -> Unit
) {
    var partIdStr by remember { mutableStateOf("1") }
    var mpn by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قطعة مصنّع (MPN)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val pId = partIdStr.toLongOrNull() ?: 1L
                    if (mpn.isNotBlank()) onConfirm(pId, mpn, description, link)
                },
                enabled = mpn.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(value = partIdStr, onValueChange = { partIdStr = it }, label = { Text("معرف القطعة الداخلية (Part ID)") }, singleLine = true)
                OutlinedTextField(value = mpn, onValueChange = { mpn = it }, label = { Text("رقم القطعة المصنعية (MPN) *") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("وصف مواصفات المصنّع") })
                OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("رابط مواصفات المنتج") }, singleLine = true)
            }
        }
    )
}

@Composable
private fun AddManufacturerPartParameterDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: String, units: String) -> Unit
) {
    var name by remember { mutableStateOf("Voltage") }
    var value by remember { mutableStateOf("5V") }
    var units by remember { mutableStateOf("V") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة معامل تقني لقطعة المصنّع", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && value.isNotBlank()) onConfirm(name, value, units)
                },
                enabled = name.isNotBlank() && value.isNotBlank()
            ) { Text("حفظ المعامل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الخاصية / المعامل (Voltage / Resistance) *") }, singleLine = true)
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("القيمة التقنية (5V / 10k) *") }, singleLine = true)
                OutlinedTextField(value = units, onValueChange = { units = it }, label = { Text("وحدة القياس (V / Ohm / uF)") }, singleLine = true)
            }
        }
    )
}

@Composable
private fun AddManufacturerPartAttachmentDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (attachmentPath: String, link: String, comment: String) -> Unit
) {
    var path by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("ورقة مواصفات فنية (Datasheet)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مرفق لقطعة المصنّع", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (path.isNotBlank() || link.isNotBlank()) onConfirm(path, link, comment)
                },
                enabled = path.isNotBlank() || link.isNotBlank()
            ) { Text("رفع الحفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("الوصف / نوع المرفق") }, singleLine = true)
                OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("مسار الملف (Datasheet.pdf)") }, singleLine = true)
                OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("أو رابط المستند الإلكتروني") }, singleLine = true)
            }
        }
    )
}

@Composable
private fun AddSupplierPartDialog(
    manufacturerParts: List<ManufacturerPart>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, sku: String, mfgPartId: Long?, description: String, link: String, note: String, packaging: String, packQuantity: String) -> Unit
) {
    var selectedMfgPartId by remember { mutableStateOf(manufacturerParts.firstOrNull()?.id) }

    val matchedPartId = remember(selectedMfgPartId) {
        manufacturerParts.find { it.id == selectedMfgPartId }?.partId ?: 1L
    }

    var partIdStr by remember(matchedPartId) { mutableStateOf(matchedPartId.toString()) }
    var sku by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Box") }
    var packQuantity by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قطعة مورد (SKU)", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val pId = partIdStr.toLongOrNull() ?: matchedPartId
                    if (sku.isNotBlank()) onConfirm(pId, sku, selectedMfgPartId, description, "", "", packaging, packQuantity)
                },
                enabled = sku.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

                if (manufacturerParts.isNotEmpty()) {
                    Text("قطعة المصنّع المرتبطة (تثبت القطعة تلقائياً):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        manufacturerParts.forEach { mfg ->
                            FilterChip(
                                selected = selectedMfgPartId == mfg.id,
                                onClick = {
                                    selectedMfgPartId = mfg.id
                                    partIdStr = mfg.partId.toString()
                                },
                                label = { Text("MPN: ${mfg.mpn}", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = partIdStr,
                    onValueChange = { if (selectedMfgPartId == null) partIdStr = it },
                    readOnly = selectedMfgPartId != null,
                    label = { Text("معرف القطعة الداخلية (Part ID)") },
                    singleLine = true
                )

                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("كود المورد (SKU) *") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("الوصف التجاري") })
                OutlinedTextField(value = packaging, onValueChange = { packaging = it }, label = { Text("نوع التغليف (Reel / Box)") }, singleLine = true)
                OutlinedTextField(value = packQuantity, onValueChange = { packQuantity = it }, label = { Text("كمية التعبئة بالحزمة") }, singleLine = true)
            }
        }
    )
}

@Composable
private fun AddPriceBreakDialog(
    defaultCurrency: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, price: Double, currency: String) -> Unit
) {
    var qtyStr by remember { mutableStateOf("10") }
    var priceStr by remember { mutableStateOf("1.5") }
    var currency by remember { mutableStateOf(defaultCurrency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة شريحة سعر متدرجة", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val pr = priceStr.toDoubleOrNull() ?: 0.0
                    onConfirm(qty, pr, currency.ifBlank { defaultCurrency })
                },
                enabled = (qtyStr.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it }, label = { Text("الحد الأدنى للكمية (Min Quantity) *") }, singleLine = true)
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("سعر الوحدة (Unit Price) *") }, singleLine = true)
                OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("العملة (تستخدم عملة الشركة افتراضياً)") }, singleLine = true)
            }
        }
    )
}
