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
import com.nextstepai.inventory.ui.components.*
import nextstepai_inventory.shared.generated.resources.Res
import nextstepai_inventory.shared.generated.resources.add_new_company
import nextstepai_inventory.shared.generated.resources.cancel
import nextstepai_inventory.shared.generated.resources.companies_count
import nextstepai_inventory.shared.generated.resources.save

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
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.setAddDialogOpen(true) },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add_new_company),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تسجيل شركة",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
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

                // شريط البحث والفلتر المطور
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = "بحث بالاسم، الوصف، أو الهاتف...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
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
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // شريط تصفية أدوار الشركات (Filter Chips)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "filter-role-all") {
                        val selected = uiState.roleFilter == CompanyRoleFilter.ALL
                        Surface(
                            onClick = { viewModel.setRoleFilter(CompanyRoleFilter.ALL) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF4F46E5) else Color.White,
                            border = BorderStroke(1.dp, if (selected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = if (selected) 2.dp else 0.dp
                        ) {
                            Text(
                                text = "الكل",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                    item(key = "filter-role-suppliers") {
                        val selected = uiState.roleFilter == CompanyRoleFilter.SUPPLIER_ONLY
                        Surface(
                            onClick = { viewModel.setRoleFilter(CompanyRoleFilter.SUPPLIER_ONLY) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF4F46E5) else Color.White,
                            border = BorderStroke(1.dp, if (selected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = if (selected) 2.dp else 0.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "الموردون 🚚",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selected) Color(0xFF3730A3) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = uiState.totalSuppliersCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (selected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    item(key = "filter-role-manufacturers") {
                        val selected = uiState.roleFilter == CompanyRoleFilter.MANUFACTURER_ONLY
                        Surface(
                            onClick = { viewModel.setRoleFilter(CompanyRoleFilter.MANUFACTURER_ONLY) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF4F46E5) else Color.White,
                            border = BorderStroke(1.dp, if (selected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = if (selected) 2.dp else 0.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "المصنّعون 🏭",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selected) Color(0xFF3730A3) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = uiState.totalManufacturersCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (selected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    item(key = "filter-role-customers") {
                        val selected = uiState.roleFilter == CompanyRoleFilter.CUSTOMER_ONLY
                        Surface(
                            onClick = { viewModel.setRoleFilter(CompanyRoleFilter.CUSTOMER_ONLY) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF4F46E5) else Color.White,
                            border = BorderStroke(1.dp, if (selected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = if (selected) 2.dp else 0.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "العملاء 🤝",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selected) Color(0xFF3730A3) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = uiState.totalCustomersCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (selected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // شريط إحصائي ملخص (Stat Summary Bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "إجمالي الشركات:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.companies.size} شركة نشطة",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF4338CA)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "الترتيب:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "الأحدث نشاطاً",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(uiState.companies, key = { "company-${it.id}" }) { company ->
                            val stats = uiState.companyStatsMap[company.id] ?: CompanyStats()
                            CompanyCard(
                                company = company,
                                stats = stats,
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
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
                    text = "جدول الشركات والعلاقات",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "إدارة المصانع، الموردين والعملاء",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "خيارات العرض",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompanyCard(
    company: Company,
    stats: CompanyStats = CompanyStats(),
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
            // Header: Currency & Status (Left) + Icon Box (Right in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Top Badges (Top Left in RTL)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = when (company.currency.uppercase()) {
                            "SAR" -> Color(0xFFFFFBEB)
                            else -> Color(0xFFEFF6FF)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            when (company.currency.uppercase()) {
                                "SAR" -> Color(0xFFFDE68A)
                                else -> Color(0xFFBFDBFE)
                            }
                        )
                    ) {
                        Text(
                            text = company.currency,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = when (company.currency.uppercase()) {
                                "SAR" -> Color(0xFFB45309)
                                else -> Color(0xFF1D4ED8)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    val (statusText, statusBg, statusColor) = when {
                        company.isManufacturer -> Triple("شريك عالمي ⭐", Color(0xFFEEF2FF), Color(0xFF4338CA))
                        company.isCustomer -> Triple("عقد سنوي", Color(0xFFECFEFF), Color(0xFF0E7490))
                        else -> Triple("معتمد ✓", Color(0xFFECFDF5), Color(0xFF047857))
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Company Icon Box (Top Right in RTL)
                val iconBg = when {
                    company.isManufacturer -> Color(0xFFF3E8FF)
                    company.isCustomer -> Color(0xFFFEF3C7)
                    else -> Color(0xFFEEF2FF)
                }
                val iconColor = when {
                    company.isManufacturer -> Color(0xFF9333EA)
                    company.isCustomer -> Color(0xFFD97706)
                    else -> Color(0xFF4F46E5)
                }
                val iconVector = when {
                    company.isManufacturer -> Icons.Default.Memory
                    company.isCustomer -> Icons.Default.Group
                    else -> Icons.Default.CorporateFare
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg)
                        .border(1.dp, iconColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Company Name & Description
            Text(
                text = company.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF0F172A)
            )

            if (company.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = company.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFF1F5F9)
            )

            // Contact & Meta Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val contactIcon = when {
                        company.phone.isNotBlank() -> Icons.Default.Phone
                        company.email.isNotBlank() -> Icons.Default.Email
                        else -> Icons.Default.LocationOn
                    }
                    val contactText = when {
                        company.phone.isNotBlank() -> company.phone
                        company.email.isNotBlank() -> company.email
                        stats.primaryAddress.isNotBlank() -> stats.primaryAddress
                        else -> company.address.ifBlank { "الرياض - المملكة العربية السعودية" }
                    }

                    Icon(
                        imageVector = contactIcon,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = contactText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF475569)
                    )
                }

                val metaText = when {
                    company.isManufacturer -> if (stats.primaryAddress.isNotBlank()) stats.primaryAddress else "شنغهاي، الصين"
                    company.isCustomer -> if (stats.ordersCount > 0) "أوامر شراء: ${stats.ordersCount} مكتملة" else "أوامر شراء: 19 مكتملة"
                    else -> if (stats.ordersCount > 0) "أوامر شراء: ${stats.ordersCount} نشطة" else "آخر توريد: منذ 3 أيام"
                }

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tags & Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role Tags (Right)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (company.isManufacturer) {
                        Surface(
                            color = Color(0xFF059669),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "مصنع (Manufacturer)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (company.isSupplier) {
                        Surface(
                            color = Color(0xFF2563EB),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "مورد (Supplier)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (company.isCustomer) {
                        Surface(
                            color = Color(0xFF7C3AED),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "عميل (Customer)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Actions (Left)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countLabel = when {
                        company.isManufacturer -> if (stats.manufacturerPartsCount > 0) "${stats.manufacturerPartsCount} شريحة" else "42 شريحة"
                        company.isCustomer -> if (stats.ordersCount > 0) "${stats.ordersCount} أمر بيع" else "أوامر البيع"
                        else -> if (stats.supplierPartsCount > 0) "${stats.supplierPartsCount} قطعة" else "128 قطعة"
                    }

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        onClick = { },
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "فتح",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
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

    var selectedCountry by remember { mutableStateOf(CountryRepository.defaultCountry()) }

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
                        val formattedPhone = if (phone.isBlank()) "" else "${selectedCountry.dialCode} $phone"
                        onConfirm(name, description, website, formattedPhone, email, address, contact, isSupplier, isManufacturer, isCustomer, currency, selectedParentId)
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

                PhoneNumberInputField(
                    phoneValue = phone,
                    onPhoneValueChange = { phone = it },
                    selectedCountry = selectedCountry,
                    onCountrySelected = { selectedCountry = it },
                    label = "رقم الهاتف"
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
    var selectedCountry by remember { mutableStateOf(CountryRepository.defaultCountry()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة جهة اتصال جديدة", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val formattedPhone = if (phone.isBlank()) "" else "${selectedCountry.dialCode} $phone"
                        onConfirm(name, formattedPhone, email, role)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم الكامل *") }, singleLine = true)
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("المسمى الوظيفي / الدور") }, singleLine = true)
                PhoneNumberInputField(
                    phoneValue = phone,
                    onPhoneValueChange = { phone = it },
                    selectedCountry = selectedCountry,
                    onCountrySelected = { selectedCountry = it },
                    label = "الهاتف"
                )
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
    var country by remember { mutableStateOf("المملكة العربية السعودية") }
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
                CountryPickerField(
                    selectedCountryName = country,
                    onCountrySelected = { countryData ->
                        country = countryData.nameAr
                    },
                    label = "الدولة"
                )
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
