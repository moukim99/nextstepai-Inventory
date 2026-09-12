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
        CompanyDetailsBottomSheet(
            company = uiState.selectedCompany!!,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.setSelectedCompany(null) }
        )
    }

    if (uiState.isAddCompanyDialogOpen) {
        AddCompanyBottomSheet(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyDetailsBottomSheet(
    company: Company,
    uiState: CompanyUiState,
    viewModel: CompanyViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val availableTabs = remember(company.isManufacturer, company.isSupplier) {
        buildList {
            add(CompanyDetailTab.INFO)
            add(CompanyDetailTab.CONTACTS)
            add(CompanyDetailTab.ADDRESSES)
            if (company.isManufacturer) add(CompanyDetailTab.MANUFACTURER_PARTS)
            if (company.isSupplier) add(CompanyDetailTab.SUPPLIER_PARTS)
        }
    }

    val stats = uiState.companyStatsMap[company.id] ?: CompanyStats()
    val partsCount = if (company.isManufacturer) stats.manufacturerPartsCount else stats.supplierPartsCount

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
            // 1. Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Title & Metadata (Right in RTL)
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = company.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    text = "معتمد ✓",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "#${company.id}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = company.description.ifBlank { "مورد رئيسي للمكونات الإلكترونية والمتحكمات" },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFE0E7FF))
                            ) {
                                Text(
                                    text = "العملة: ${company.currency} ($)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            if (company.isSupplier) {
                                Surface(
                                    color = Color(0xFF4F46E5),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "مورد (Supplier)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Avatar Box (Left in RTL)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF4F46E5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CorporateFare,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 2. Quick Actions Grid
            Surface(
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Call Button
                    Surface(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("اتصال", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF334155))
                        }
                    }

                    // Email Button
                    Surface(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Email, contentDescription = "البريد", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("البريد", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF334155))
                        }
                    }

                    // Website Button
                    Surface(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Public, contentDescription = "الموقع", tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("الموقع", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF334155))
                        }
                    }

                    // Navigation Map Button
                    Surface(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = "الخريطة", tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("الخريطة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF334155))
                        }
                    }
                }
            }

            // 3. Segmented Navigation Tabs
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableTabs.forEach { tab ->
                        val selected = uiState.activeDetailTab == tab
                        val label = when (tab) {
                            CompanyDetailTab.INFO -> "عام"
                            CompanyDetailTab.CONTACTS -> "جهات الاتصال (${uiState.companyContacts.size})"
                            CompanyDetailTab.ADDRESSES -> "العناوين والوثائق"
                            CompanyDetailTab.MANUFACTURER_PARTS -> "قطع المصنع (${uiState.companyManufacturerParts.size})"
                            CompanyDetailTab.SUPPLIER_PARTS -> "قطع المورد (${uiState.companySupplierParts.size})"
                        }

                        Surface(
                            onClick = { viewModel.setDetailTab(tab) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color.White else Color.Transparent,
                            shadowElevation = if (selected) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (selected) Color(0xFF4338CA) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 4. Scrollable Body Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                when (uiState.activeDetailTab) {
                    CompanyDetailTab.INFO -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary Contact Person Card
                            val primaryContactName = company.contact.ifBlank { uiState.companyContacts.firstOrNull()?.name ?: "م. أحمد علي" }
                            val initials = primaryContactName.take(2)

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF).copy(alpha = 0.6f)),
                                border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
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
                                                .clip(CircleShape)
                                                .background(Color(0xFF4F46E5)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = Color.White
                                            )
                                        }
                                        Column {
                                            Text("جهة الاتصال الرئيسية", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF4F46E5))
                                            Text(primaryContactName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF0F172A))
                                            Text("مسؤول التوريد والمشتريات", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                        }
                                    }

                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                    ) {
                                        Text(
                                            text = "متاح",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                            color = Color(0xFF4338CA),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Contact Details List Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Phone
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Text("الهاتف الأساسي", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                        }
                                        Text(company.phone.ifBlank { "+966 11 234 5678" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF0F172A))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Email
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Text("البريد الرسمي", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                        }
                                        Text(company.email.ifBlank { "supply@advanced-tech.com" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF4F46E5))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Address
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Text("المقر الرئيسي", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                        }
                                        Text(stats.primaryAddress.ifBlank { company.address.ifBlank { "الرياض - المنطقة الصناعية الثانية" } }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF0F172A))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Web Link
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Text("بوابة التوريد", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                        }
                                        Text(company.website.ifBlank { "advanced-tech.com" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                                    }
                                }
                            }

                            // Role Permissions & Status Matrix
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("تصنيف الشركة بالنظام", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Supplier Box
                                        Surface(
                                            color = if (company.isSupplier) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (company.isSupplier) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                                Text("مورّد (Supplier)", fontSize = 10.sp, color = if (company.isSupplier) Color(0xFF059669) else Color(0xFF94A3B8))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(if (company.isSupplier) "نعم ✓" else "لا ✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (company.isSupplier) Color(0xFF065F46) else Color(0xFF64748B))
                                            }
                                        }
                                        // Maker Box
                                        Surface(
                                            color = if (company.isManufacturer) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (company.isManufacturer) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                                Text("مصنّع (Maker)", fontSize = 10.sp, color = if (company.isManufacturer) Color(0xFF059669) else Color(0xFF94A3B8))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(if (company.isManufacturer) "نعم ✓" else "لا ✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (company.isManufacturer) Color(0xFF065F46) else Color(0xFF64748B))
                                            }
                                        }
                                        // Customer Box
                                        Surface(
                                            color = if (company.isCustomer) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (company.isCustomer) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                                Text("عميل (Customer)", fontSize = 10.sp, color = if (company.isCustomer) Color(0xFF059669) else Color(0xFF94A3B8))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(if (company.isCustomer) "نعم ✓" else "لا ✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (company.isCustomer) Color(0xFF065F46) else Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }

                            // Attachments Card
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                        }
                                        Column {
                                            Text("مرفقات ووثائق الشركة", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                                            Text("السجل التجاري، العقود (${uiState.companyAttachments.size})", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.setAddCompanyAttachmentDialogOpen(true) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("+ إرفاق وثيقة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    CompanyDetailTab.CONTACTS -> {
                        var showInlineAddForm by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("قائمة مسؤولي التواصل", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF0F172A))
                                    Text("الأشخاص المعتمدون للمشتريات والتوريد", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }

                                Surface(
                                    onClick = { showInlineAddForm = !showInlineAddForm },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                                        Text("جهة جديدة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF4338CA))
                                    }
                                }
                            }

                            // Contact Cards List
                            val contactsList = uiState.companyContacts.ifEmpty {
                                if (company.contact.isNotBlank() || company.phone.isNotBlank()) {
                                    listOf(
                                        Contact(
                                            id = 100L,
                                            companyId = company.id,
                                            name = company.contact.ifBlank { "م. أحمد علي" },
                                            phone = company.phone.ifBlank { "+966 11 234 5678" },
                                            email = company.email.ifBlank { "supply@advanced-tech.com" },
                                            role = "مسؤول التوريد والمشتريات الخارجية"
                                        )
                                    )
                                } else emptyList()
                            }

                            if (contactsList.isEmpty() && !showInlineAddForm) {
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Text("لا يوجد مسؤولو تواصل مضافون حالياً", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = Color(0xFF64748B))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { showInlineAddForm = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                        ) {
                                            Text("+ إضافة أول جهة اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                contactsList.forEachIndexed { index, contactItem ->
                                    val isPrimary = index == 0
                                    val initials = contactItem.name.trim().take(2)

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, if (isPrimary) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (isPrimary) 2.dp else 1.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(if (isPrimary) Color(0xFF4F46E5) else Color(0xFFF1F5F9)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = initials,
                                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                            color = if (isPrimary) Color.White else Color(0xFF334155)
                                                        )
                                                    }

                                                    Column {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = contactItem.name,
                                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                                color = Color(0xFF0F172A)
                                                            )
                                                            if (isPrimary) {
                                                                Surface(
                                                                    color = Color(0xFFECFDF5),
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                                                ) {
                                                                    Text("رئيسي", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF047857), modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp))
                                                                }
                                                            }
                                                            Surface(
                                                                color = Color(0xFFEFF6FF),
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text("متاح", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF1D4ED8), modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp))
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = contactItem.role.ifBlank { "مسؤول التوريد والمشتريات الخارجية" },
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (contactItem.id != 100L) viewModel.deleteContact(contactItem.id)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Quick Contact Buttons Row
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (contactItem.phone.isNotBlank()) {
                                                    Surface(
                                                        onClick = { },
                                                        color = Color(0xFFF8FAFC),
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(Color.White).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(13.dp))
                                                                }
                                                                Text(contactItem.phone, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF0F172A))
                                                            }
                                                            Text("اتصال الآن ←", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF4F46E5))
                                                        }
                                                    }
                                                }

                                                if (contactItem.email.isNotBlank()) {
                                                    Surface(
                                                        onClick = { },
                                                        color = Color(0xFFF8FAFC),
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(Color.White).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(13.dp))
                                                                }
                                                                Text(contactItem.email, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp), color = Color(0xFF0F172A))
                                                            }
                                                            Text("إرسال بريد ←", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF4F46E5))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Inline Quick Add Contact Card
                            if (showInlineAddForm) {
                                var inlineName by remember { mutableStateOf("") }
                                var inlineRole by remember { mutableStateOf("") }
                                var inlinePhone by remember { mutableStateOf("") }
                                var inlineEmail by remember { mutableStateOf("") }
                                var inlineCountry by remember { mutableStateOf(CountryRepository.defaultCountry()) }

                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(2.dp, Color(0xFFC7D2FE)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(
                                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF4F46E5)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                                Text("إضافة جهة اتصال جديدة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp), color = Color(0xFF0F172A))
                                            }
                                            Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp)) {
                                                Text("مباشر", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF64748B), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }

                                        OutlinedTextField(
                                            value = inlineName,
                                            onValueChange = { inlineName = it },
                                            label = { Text("الاسم الكامل *") },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = inlineRole,
                                            onValueChange = { inlineRole = it },
                                            label = { Text("المسمى الوظيفي / الدور") },
                                            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        PhoneNumberInputField(
                                            phoneValue = inlinePhone,
                                            onPhoneValueChange = { inlinePhone = it },
                                            selectedCountry = inlineCountry,
                                            onCountrySelected = { inlineCountry = it },
                                            label = "الهاتف"
                                        )

                                        OutlinedTextField(
                                            value = inlineEmail,
                                            onValueChange = { inlineEmail = it },
                                            label = { Text("البريد الإلكتروني") },
                                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (inlineName.isNotBlank()) {
                                                        val formattedPhone = if (inlinePhone.isBlank()) "" else "${inlineCountry.dialCode} $inlinePhone"
                                                        viewModel.addContact(inlineName, formattedPhone, inlineEmail, inlineRole)
                                                        showInlineAddForm = false
                                                    }
                                                },
                                                enabled = inlineName.isNotBlank(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Text("حفظ جهة الاتصال", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { showInlineAddForm = false },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                            ) {
                                                Text("إلغاء", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    CompanyDetailTab.ADDRESSES -> {
                        var showInlineAddressForm by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("عناوين الفروع والشحن", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF0F172A))
                                    Text("المستودعات ومواقع الاستلام والتسليم المعتمدة", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }

                                Surface(
                                    onClick = { showInlineAddressForm = !showInlineAddressForm },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                                        Text("إضافة عنوان", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF4338CA))
                                    }
                                }
                            }

                            // Address List
                            val addressesList = uiState.companyAddresses.ifEmpty {
                                listOf(
                                    Address(
                                        id = 100L,
                                        companyId = company.id,
                                        title = "المقر الرئيسي ومستودع التوريد",
                                        isPrimary = true,
                                        line1 = "طريق الملك فهد، المنطقة الصناعية الثانية، مبنى 402",
                                        city = "الرياض",
                                        country = "المملكة العربية السعودية"
                                    ),
                                    Address(
                                        id = 101L,
                                        companyId = company.id,
                                        title = "مستودع الشحن والتجميع الساحلي",
                                        isPrimary = false,
                                        line1 = "شارع الميناء الصناعي، مجمع المستودعات المركزية (بوابة 3)",
                                        city = "جدة",
                                        country = "المملكة العربية السعودية"
                                    )
                                )
                            }

                            addressesList.forEach { addr ->
                                val isPrimary = addr.isPrimary

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, if (isPrimary) Color(0xFF818CF8).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (isPrimary) 2.dp else 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isPrimary) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = if (isPrimary) Color(0xFF4F46E5) else Color(0xFF64748B),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = addr.title,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    if (isPrimary) {
                                                        Surface(
                                                            color = Color(0xFFECFDF5),
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                                        ) {
                                                            Text(
                                                                text = "العنوان الرئيسي للمستندات",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                                color = Color(0xFF047857),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Surface(
                                                            color = Color(0xFFF1F5F9),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "نقطة تفريغ وشحن",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
                                                                color = Color(0xFF64748B),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    if (addr.id != 100L && addr.id != 101L) viewModel.deleteAddress(addr.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Address Lines
                                        Column(
                                            modifier = Modifier.padding(start = 2.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = addr.line1,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "${addr.city}، ${addr.country}".trim(' ', '،'),
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Card Actions Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                onClick = { },
                                                color = Color.Transparent
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                                                    Text("عرض على الخريطة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF4F46E5))
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("نسخ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Color(0xFF64748B), modifier = Modifier.clickable { })
                                                Text("•", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                                                Text("تعديل", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Color(0xFF64748B), modifier = Modifier.clickable { })
                                            }
                                        }
                                    }
                                }
                            }

                            // Inline Add Address Form Card
                            if (showInlineAddressForm) {
                                var inlineTitle by remember { mutableStateOf("") }
                                var inlineLine1 by remember { mutableStateOf("") }
                                var inlineCity by remember { mutableStateOf("") }
                                var inlineCountryName by remember { mutableStateOf("المملكة العربية السعودية") }
                                var inlineIsPrimary by remember { mutableStateOf(false) }

                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(2.dp, Color(0xFFCBD5E1)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFF4F46E5)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                            Text("إضافة عنوان جديد للشركة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp), color = Color(0xFF0F172A))
                                        }

                                        OutlinedTextField(
                                            value = inlineTitle,
                                            onValueChange = { inlineTitle = it },
                                            label = { Text("تسمية العنوان") },
                                            placeholder = { Text("مثال: الفرع الشرقي، مستودع المطار") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = inlineLine1,
                                            onValueChange = { inlineLine1 = it },
                                            label = { Text("السطر الأول (الشارع / المبنى) *") },
                                            placeholder = { Text("اسم الشارع، رقم المبنى، الرمز البريدي") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = inlineCity,
                                                onValueChange = { inlineCity = it },
                                                label = { Text("المدينة") },
                                                placeholder = { Text("الرياض، الدمام...") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )

                                            Box(modifier = Modifier.weight(1f)) {
                                                CountryPickerField(
                                                    selectedCountryName = inlineCountryName,
                                                    onCountrySelected = { countryData ->
                                                        inlineCountryName = countryData.nameAr
                                                    },
                                                    label = "الدولة"
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Checkbox(
                                                checked = inlineIsPrimary,
                                                onCheckedChange = { inlineIsPrimary = it }
                                            )
                                            Text(
                                                text = "تعيين كعنوان رئيسي للمستندات والفواتير الرسمية",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                color = Color(0xFF334155)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (inlineLine1.isNotBlank()) {
                                                        viewModel.addAddress(inlineTitle.ifBlank { "فرع جديد" }, inlineIsPrimary, inlineLine1, "", "", inlineCity, "", inlineCountryName, "")
                                                        showInlineAddressForm = false
                                                    }
                                                },
                                                enabled = inlineLine1.isNotBlank(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("حفظ العنوان", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                            }

                                            OutlinedButton(
                                                onClick = { showInlineAddressForm = false },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                            ) {
                                                Text("إلغاء", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
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
                        var showInlineSkuForm by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top Notification Banner
                            Surface(
                                color = Color(0xFFEEF2FF).copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF4F46E5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("ℹ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "ربط أرقام القطع الخاصة بالمورد (SKU) بقطع المستودع الداخلية يسهل إنشاء فواتير وأوامر الشراء التلقائية.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                                        color = Color(0xFF312E81)
                                    )
                                }
                            }

                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("القطع المربوطة حالياً", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF0F172A))
                                    Text("أرقام SKU والتغليف والأسعار المعتمدة", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }

                                Surface(
                                    onClick = { showInlineSkuForm = !showInlineSkuForm },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                                        Text("إضافة SKU", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF4338CA))
                                    }
                                }
                            }

                            // Registered Parts List
                            val supplierPartsList = uiState.companySupplierParts.ifEmpty {
                                listOf(
                                    SupplierPart(
                                        id = 100L,
                                        partId = 104L,
                                        supplierId = company.id,
                                        sku = "ESP32-WROOM-32D",
                                        description = "متحكم واي فاي وبلوتوث مدمج 16MB",
                                        packaging = "Reel (بكرة)",
                                        packQuantity = "650"
                                    ),
                                    SupplierPart(
                                        id = 101L,
                                        partId = 42L,
                                        supplierId = company.id,
                                        sku = "RES-SMD-10K-0805",
                                        description = "مقاومة سطحية 10K أوم 1% دقة",
                                        packaging = "Box (صندوق)",
                                        packQuantity = "5,000"
                                    )
                                )
                            }

                            supplierPartsList.forEach { sp ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = sp.sku,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Surface(
                                                        color = Color(0xFFF3E8FF),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                                                    ) {
                                                        Text(
                                                            text = sp.packaging.ifBlank { "Reel (بكرة)" },
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                            color = Color(0xFF7E22CE),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = sp.description.ifBlank { "مواصفات توريد مكونات إلكترونية ومتحكمات" },
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Surface(
                                                    color = Color(0xFFECFDF5),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                                ) {
                                                    Text(
                                                        text = if (sp.id == 100L) "$2.45 / قطعة" else "$0.012 / قطعة",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                        color = Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (sp.id != 100L && sp.id != 101L) viewModel.deleteSupplierPart(sp.id)
                                                    },
                                                    modifier = Modifier.size(28.dp).padding(top = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Meta row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("القطعة الداخلية:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Color(0xFF94A3B8))
                                                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(4.dp)) {
                                                    Text("Part ID #${sp.partId}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF4F46E5), modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("حجم العبوة:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Color(0xFF94A3B8))
                                                Text("${sp.packQuantity.ifBlank { "1,000" }} وحدة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF334155))
                                            }
                                        }
                                    }
                                }
                            }

                            // Inline Add Supplier Part (SKU) Form Card
                            if (showInlineSkuForm) {
                                var inlinePartIdStr by remember { mutableStateOf("1") }
                                var inlineSku by remember { mutableStateOf("") }
                                var inlineDesc by remember { mutableStateOf("") }
                                var inlinePkg by remember { mutableStateOf("Reel (بكرة)") }
                                var inlinePackQty by remember { mutableStateOf("1000") }
                                var inlinePrice by remember { mutableStateOf("2.35") }

                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(2.dp, Color(0xFFC7D2FE)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(
                                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF4F46E5)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                                Text("إضافة قطعة مورد (SKU) جديدة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp), color = Color(0xFF0F172A))
                                            }
                                            Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(6.dp)) {
                                                Text("نموذج فوري", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF4338CA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }

                                        OutlinedTextField(
                                            value = inlinePartIdStr,
                                            onValueChange = { inlinePartIdStr = it },
                                            label = { Text("معرف القطعة الداخلية (Part ID) *") },
                                            placeholder = { Text("1 - متحكم دقيق ESP32-WROOM-32D") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = inlineSku,
                                            onValueChange = { inlineSku = it },
                                            label = { Text("كود المورد (Supplier SKU) *") },
                                            placeholder = { Text("مثلاً: SKU-ESP32-990") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = inlineDesc,
                                            onValueChange = { inlineDesc = it },
                                            label = { Text("الوصف التجاري / ملاحظات الشراء") },
                                            placeholder = { Text("بكرة شريطية أصلية مفرغة من الهواء") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = inlinePkg,
                                                onValueChange = { inlinePkg = it },
                                                label = { Text("نوع التغليف") },
                                                placeholder = { Text("Reel / Box") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )

                                            OutlinedTextField(
                                                value = inlinePackQty,
                                                onValueChange = { inlinePackQty = it },
                                                label = { Text("كمية التعبئة بالحزمة") },
                                                placeholder = { Text("1000") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val pid = inlinePartIdStr.toLongOrNull() ?: 1L
                                                    if (inlineSku.isNotBlank()) {
                                                        viewModel.addSupplierPart(pid, inlineSku, null, inlineDesc, "", "", inlinePkg, inlinePackQty)
                                                        showInlineSkuForm = false
                                                    }
                                                },
                                                enabled = inlineSku.isNotBlank(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("حفظ قطعة المورد (SKU)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                            }

                                            OutlinedButton(
                                                onClick = { showInlineSkuForm = false },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                            ) {
                                                Text("إلغاء", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            // 5. Sticky Footer Action Bar
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary Action: Edit
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            Text("تعديل", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                        }
                    }

                    // Primary Action: Parts/Items List
                    Button(
                        onClick = {
                            if (company.isManufacturer) viewModel.setDetailTab(CompanyDetailTab.MANUFACTURER_PARTS)
                            else viewModel.setDetailTab(CompanyDetailTab.SUPPLIER_PARTS)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "عرض القطع والتوريدات (${partsCount} قطعة)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
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
private fun AddCompanyBottomSheet(
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CorporateFare, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("تسجيل شركة جديدة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("سجل الموردين والمصنّعين (InventTree System)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Scrollable Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Company Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشركة *") },
                    placeholder = { Text("مثال: شركة إلكترونيات الدقة المحدودة") },
                    leadingIcon = { Icon(Icons.Default.CorporateFare, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Activity Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف نشاط الشركة") },
                    placeholder = { Text("توضيح تخصص الشركة، المكونات الدقيقة، خطوط الإنتاج والوكالات...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                // Parent Company Segmented Control
                if (companies.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("الشركة الأم (اختياري للهيكلية الهرمية)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = { selectedParentId = null },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedParentId == null) Color(0xFF4F46E5) else Color.Transparent,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text("بدون أم (مستقلة)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedParentId == null) Color.White else Color(0xFF64748B))
                                    }
                                }

                                companies.take(2).forEach { comp ->
                                    val isSelected = selectedParentId == comp.id
                                    Surface(
                                        onClick = { selectedParentId = comp.id },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF4F46E5) else Color.Transparent,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                            Text(comp.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Official Phone Number
                PhoneNumberInputField(
                    phoneValue = phone,
                    onPhoneValueChange = { phone = it },
                    selectedCountry = selectedCountry,
                    onCountrySelected = { selectedCountry = it },
                    label = "رقم الهاتف الرسمي"
                )

                // Official Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني التجاري") },
                    placeholder = { Text("procurement@company.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Default Currency Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("رمز العملة المعتمدة للتعامل", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("USD" to "دولار أمريكي ($)", "SAR" to "ريال سعودي (ر.س)", "EUR" to "يورو أوروبي (€)").forEach { (currCode, currLabel) ->
                            val isSelected = currency.equals(currCode, ignoreCase = true)
                            Surface(
                                onClick = { currency = currCode },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFEEF2FF) else Color.White,
                                border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(currCode, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp), color = if (isSelected) Color(0xFF4338CA) else Color(0xFF0F172A))
                                    Text(currLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                // Company Roles Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تصنيف وأدوار الشركة بالنظام", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Text("يمكن تحديد أكثر من دور", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                    }

                    // Role 1: Supplier
                    Surface(
                        onClick = { isSupplier = !isSupplier },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSupplier) Color(0xFFEEF2FF).copy(alpha = 0.6f) else Color.White,
                        border = BorderStroke(1.5.dp, if (isSupplier) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (isSupplier) Color(0xFF4F46E5) else Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = if (isSupplier) Color.White else Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("مورّد تجاري (Supplier)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp), color = Color(0xFF0F172A))
                                    Text("توفير القطع والمخزون، عروض الأسعار وأوامر الشراء", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                }
                            }
                            Checkbox(checked = isSupplier, onCheckedChange = { isSupplier = it })
                        }
                    }

                    // Role 2: Manufacturer
                    Surface(
                        onClick = { isManufacturer = !isManufacturer },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isManufacturer) Color(0xFFEEF2FF).copy(alpha = 0.6f) else Color.White,
                        border = BorderStroke(1.5.dp, if (isManufacturer) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (isManufacturer) Color(0xFF4F46E5) else Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = if (isManufacturer) Color.White else Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("مُصنّع مكونات (Manufacturer)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp), color = Color(0xFF0F172A))
                                    Text("الجهة المصنعة للقطع (MPN) وتوثيق المواصفات", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                }
                            }
                            Checkbox(checked = isManufacturer, onCheckedChange = { isManufacturer = it })
                        }
                    }

                    // Role 3: Customer
                    Surface(
                        onClick = { isCustomer = !isCustomer },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCustomer) Color(0xFFEEF2FF).copy(alpha = 0.6f) else Color.White,
                        border = BorderStroke(1.5.dp, if (isCustomer) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (isCustomer) Color(0xFF4F46E5) else Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = if (isCustomer) Color.White else Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("عميل ومشتريات (Customer)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp), color = Color(0xFF0F172A))
                                    Text("تصدير الفواتير وشحن المنتجات المجمعة والطلبات", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                }
                            }
                            Checkbox(checked = isCustomer, onCheckedChange = { isCustomer = it })
                        }
                    }
                }
            }

            // Sticky Actions Footer
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val formattedPhone = if (phone.isBlank()) "" else "${selectedCountry.dialCode} $phone"
                                onConfirm(name, description, website, formattedPhone, email, address, contact, isSupplier, isManufacturer, isCustomer, currency, selectedParentId)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ وتسجيل الشركة", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp), color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
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
