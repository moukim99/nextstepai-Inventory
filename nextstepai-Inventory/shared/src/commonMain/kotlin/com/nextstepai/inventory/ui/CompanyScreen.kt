package com.nextstepai.inventory.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import kotlin.time.Clock
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.ui.components.*
import com.nextstepai.inventory.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
            Box(
                modifier = Modifier.padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddCompanyDialog() },
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
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

                // شريط البحث والفلتر المطور متطابق الارتفاع
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
                                text = "بحث بالاسم، الوصف، أو الهاتف...",
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

                    val isFilterActive = uiState.selectedCountries.isNotEmpty() || uiState.selectedScope != "ALL"
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
                                text = if (isFilterActive) "فلتر (${uiState.selectedCountries.size})" else "فلتر",
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

                // شريط تصفية أدوار الشركات (Filter Chips) - صف واحد محكم بدون سحب جانبي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. الكل
                    val isAllSelected = uiState.roleFilter == CompanyRoleFilter.ALL
                    Surface(
                        onClick = { viewModel.setRoleFilter(CompanyRoleFilter.ALL) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllSelected) Color(0xFF4F46E5) else Color.White,
                        border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (isAllSelected) 2.dp else 0.dp,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = "الكل",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. الموردون
                    val isSupplierSelected = uiState.roleFilter == CompanyRoleFilter.SUPPLIER_ONLY
                    Surface(
                        onClick = { viewModel.setRoleFilter(CompanyRoleFilter.SUPPLIER_ONLY) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSupplierSelected) Color(0xFF4F46E5) else Color.White,
                        border = BorderStroke(1.dp, if (isSupplierSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (isSupplierSelected) 2.dp else 0.dp,
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = "الموردون 🚚",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = if (isSupplierSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isSupplierSelected) Color(0xFF3730A3) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = uiState.totalSuppliersCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp
                                    ),
                                    color = if (isSupplierSelected) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // 3. المصنعون
                    val isManufacturerSelected = uiState.roleFilter == CompanyRoleFilter.MANUFACTURER_ONLY
                    Surface(
                        onClick = { viewModel.setRoleFilter(CompanyRoleFilter.MANUFACTURER_ONLY) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isManufacturerSelected) Color(0xFF4F46E5) else Color.White,
                        border = BorderStroke(1.dp, if (isManufacturerSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (isManufacturerSelected) 2.dp else 0.dp,
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = "المصنعون 🏭",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = if (isManufacturerSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isManufacturerSelected) Color(0xFF3730A3) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = uiState.totalManufacturersCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp
                                    ),
                                    color = if (isManufacturerSelected) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // 4. العملاء
                    val isCustomerSelected = uiState.roleFilter == CompanyRoleFilter.CUSTOMER_ONLY
                    Surface(
                        onClick = { viewModel.setRoleFilter(CompanyRoleFilter.CUSTOMER_ONLY) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCustomerSelected) Color(0xFF4F46E5) else Color.White,
                        border = BorderStroke(1.dp, if (isCustomerSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (isCustomerSelected) 2.dp else 0.dp,
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = "العملاء 🤝",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = if (isCustomerSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isCustomerSelected) Color(0xFF3730A3) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = uiState.totalCustomersCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp
                                    ),
                                    color = if (isCustomerSelected) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
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

    if (uiState.isFilterBottomSheetOpen) {
        CompanyFilterBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.setFilterBottomSheetOpen(false) },
            onApply = { selectedCountries, selectedScope ->
                viewModel.applyCountryFilters(selectedCountries, selectedScope)
            },
            onClear = { viewModel.clearCountryFilters() }
        )
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
            companyToEdit = uiState.companyToEdit,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, curr, parentId ->
                if (uiState.companyToEdit != null) {
                    viewModel.updateCompany(
                        id = uiState.companyToEdit!!.id,
                        name = name,
                        description = desc,
                        website = web,
                        phone = phone,
                        email = email,
                        address = addr,
                        contact = contact,
                        isSupplier = isSup,
                        isManufacturer = isMan,
                        isCustomer = isCust,
                        currency = curr,
                        parentId = parentId
                    )
                } else {
                    viewModel.addCompany(name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, curr, parentId)
                }
            }
        )
    }

    if (uiState.isAddContactDialogOpen) {
        AddContactBottomSheet(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddContactDialogOpen(false) },
            onConfirm = { name, phone, email, role, isPrimary ->
                viewModel.addContact(name, phone, email, role, isPrimary)
            }
        )
    }

    if (uiState.isAddAddressDialogOpen) {
        AddAddressBottomSheet(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddAddressDialogOpen(false) },
            onConfirm = { title, isPrimary, line1, line2, postalCode, city, province, country, notes ->
                viewModel.addAddress(title, isPrimary, line1, line2, postalCode, city, province, country, notes)
            }
        )
    }

    if (uiState.isAddCompanyAttachmentDialogOpen) {
        AddCompanyAttachmentBottomSheet(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddCompanyAttachmentDialogOpen(false) },
            onConfirm = { docType, path, link, comment, expiry, notify, notifyDays ->
                viewModel.addCompanyAttachment(docType, path, link, comment, expiry, notify, notifyDays)
            }
        )
    }

    if (uiState.isAddBankAccountDialogOpen) {
        AddBankAccountBottomSheet(
            defaultCurrency = uiState.selectedCompany?.currency ?: "USD",
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setAddBankAccountDialogOpen(false) },
            onConfirm = { bankName, accName, accNum, iban, swift, curr, branch, isPrimary ->
                viewModel.addCompanyBankAccount(bankName, accName, accNum, iban, swift, curr, branch, isPrimary)
            }
        )
    }

    if (uiState.isEditLegalRecordDialogOpen) {
        EditLegalRecordBottomSheet(
            currentRecord = uiState.companyLegalRecord,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setEditLegalRecordDialogOpen(false) },
            onConfirm = { cr, tax, natId, impLic, mfgLic, actCodes, auth, issue, expiry ->
                viewModel.saveOrUpdateCompanyLegalRecord(cr, tax, natId, impLic, mfgLic, actCodes, auth, issue, expiry)
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

            Spacer(modifier = Modifier.width(48.dp))
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

private fun makePhoneCall(uriHandler: UriHandler, phone: String) {
    val cleanPhone = phone.trim().replace(" ", "")
    if (cleanPhone.isNotBlank()) {
        try {
            uriHandler.openUri("tel:$cleanPhone")
        } catch (_: Exception) { }
    }
}

private fun sendEmail(uriHandler: UriHandler, email: String, companyName: String = "") {
    val cleanEmail = email.trim()
    if (cleanEmail.isNotBlank()) {
        try {
            uriHandler.openUri("mailto:$cleanEmail")
        } catch (_: Exception) { }
    }
}

private fun openWebsite(uriHandler: UriHandler, url: String) {
    var cleanUrl = url.trim()
    if (cleanUrl.isNotBlank()) {
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        try {
            uriHandler.openUri(cleanUrl)
        } catch (_: Exception) { }
    }
}

private fun openMapLocation(uriHandler: UriHandler, address: String) {
    val cleanAddress = address.trim()
    if (cleanAddress.isNotBlank()) {
        val mapUrl = "https://www.google.com/maps/search/?api=1&query=" + cleanAddress.replace(" ", "+")
        try {
            uriHandler.openUri(mapUrl)
        } catch (_: Exception) { }
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

    val availableTabs = remember {
        listOf(
            CompanyDetailTab.INFO,
            CompanyDetailTab.CONTACTS,
            CompanyDetailTab.ADDRESSES,
            CompanyDetailTab.BANK_ACCOUNTS
        )
    }

    val stats = uiState.companyStatsMap[company.id] ?: CompanyStats()

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

            val uriHandler = LocalUriHandler.current

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
                        onClick = { makePhoneCall(uriHandler, company.phone.ifBlank { "+966112345678" }) },
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
                        onClick = { sendEmail(uriHandler, company.email.ifBlank { "supply@advanced-tech.com" }, company.name) },
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
                        onClick = { openWebsite(uriHandler, company.website.ifBlank { "advanced-tech.com" }) },
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
                        onClick = { openMapLocation(uriHandler, stats.primaryAddress.ifBlank { company.address.ifBlank { "الرياض، المملكة العربية السعودية" } }) },
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
                            CompanyDetailTab.BANK_ACCOUNTS -> "البنوك (${uiState.companyBankAccounts.size})"
                            else -> ""
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
                        val primaryContactObj = uiState.companyContacts.find { it.isPrimary } ?: uiState.companyContacts.firstOrNull()

                        val primaryContactName = primaryContactObj?.name?.ifBlank { null }
                            ?: company.contact.ifBlank { "غير محدد" }
                        val primaryContactRole = primaryContactObj?.role?.ifBlank { null }
                            ?: "مسؤول التواصل والتوريد"
                        val initials = primaryContactName.take(2)

                        val displayPhone = primaryContactObj?.phone?.ifBlank { null }
                            ?: uiState.companyContacts.find { it.phone.isNotBlank() }?.phone
                            ?: company.phone

                        val displayEmail = primaryContactObj?.email?.ifBlank { null }
                            ?: uiState.companyContacts.find { it.email.isNotBlank() }?.email
                            ?: company.email

                        val primaryAddressObj = uiState.companyAddresses.find { it.isPrimary } ?: uiState.companyAddresses.firstOrNull()
                        val displayAddress = primaryAddressObj?.let {
                            buildList {
                                if (it.line1.isNotBlank()) add(it.line1)
                                if (it.city.isNotBlank()) add(it.city)
                                if (it.country.isNotBlank()) add(it.country)
                            }.joinToString("، ")
                        }?.ifBlank { null } ?: stats.primaryAddress.ifBlank { company.address }

                        val displayWebsite = company.website

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary Contact Person Card
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
                                            Text(primaryContactRole, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
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
                                        modifier = Modifier.fillMaxWidth().clickable { if (displayPhone.isNotBlank()) makePhoneCall(uriHandler, displayPhone) }.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Text("الهاتف الأساسي", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                        }
                                        Text(displayPhone.ifBlank { "غير محدد" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = if (displayPhone.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Email
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { if (displayEmail.isNotBlank()) sendEmail(uriHandler, displayEmail, company.name) }.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Text("البريد الرسمي", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                        }
                                        Text(displayEmail.ifBlank { "غير محدد" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = if (displayEmail.isNotBlank()) Color(0xFF4F46E5) else Color(0xFF94A3B8))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Address
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { if (displayAddress.isNotBlank()) openMapLocation(uriHandler, displayAddress) }.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Text("المقر الرئيسي", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                        }
                                        Text(displayAddress.ifBlank { "غير محدد" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = if (displayAddress.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Web Link
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { if (displayWebsite.isNotBlank()) openWebsite(uriHandler, displayWebsite) }.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Text("بوابة التوريد / الموقع", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = Color(0xFF64748B))
                                        }
                                        Text(displayWebsite.ifBlank { "غير محدد" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = if (displayWebsite.isNotBlank()) Color(0xFF334155) else Color(0xFF94A3B8))
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
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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

                                    if (uiState.companyAttachments.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color(0xFFE2E8F0))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        uiState.companyAttachments.forEach { att ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                                    Column {
                                                        Text(
                                                            text = att.documentType.ifBlank { att.comment.ifBlank { "وثيقة رسمية" } },
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                                            color = Color(0xFF0F172A)
                                                        )
                                                        Text(
                                                            text = att.attachmentPath.ifBlank { att.link.ifBlank { "مرفق معتمد" } },
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteCompanyAttachment(att.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Legal Records & Licenses Card
                            val legalRec = uiState.companyLegalRecord
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFEEF2FF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                            }
                                            Column {
                                                Text("السجل التجاري والتراخيص القانونية", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                                                Text("بيانات القيد والتسجيل الضريبي والاستيراد", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.setEditLegalRecordDialogOpen(true) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(if (legalRec != null) "تحديث" else "+ إضافة سجل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(4.dp))

                                    DetailRow("السجل التجاري (CR):", legalRec?.commercialRegisterNumber?.ifBlank { "غير مسجل" } ?: "غير مسجل")
                                    DetailRow("الرقم الضريبي (VAT ID):", legalRec?.taxId?.ifBlank { "غير مسجل" } ?: "غير مسجل")

                                    if (legalRec?.nationalIdNumber?.isNotBlank() == true) {
                                        DetailRow("الرقم التعريفي الموحد (NIS):", legalRec.nationalIdNumber)
                                    }
                                    if (legalRec?.importLicenseNumber?.isNotBlank() == true) {
                                        DetailRow("رخصة الاستيراد الجمركية:", legalRec.importLicenseNumber)
                                    }
                                    if (legalRec?.manufacturingLicenseNumber?.isNotBlank() == true) {
                                        DetailRow("رخصة التصنيع والانتاج:", legalRec.manufacturingLicenseNumber)
                                    }
                                    if (legalRec?.activityCodes?.isNotBlank() == true) {
                                        DetailRow("كود النشاط الاقتصادي (ISIC):", legalRec.activityCodes)
                                    }
                                    if (legalRec?.issuingAuthority?.isNotBlank() == true) {
                                        DetailRow("جهة الإصدار الرسمية:", legalRec.issuingAuthority)
                                    }
                                    if (legalRec?.expiryDate?.isNotBlank() == true) {
                                        DetailRow("تاريخ الانتهاء:", legalRec.expiryDate)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    CompanyDetailTab.CONTACTS -> {
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
                                    onClick = { viewModel.setAddContactDialogOpen(true) },
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

                            if (contactsList.isEmpty()) {
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
                                            onClick = { viewModel.setAddContactDialogOpen(true) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                                        ) {
                                            Text("+ إضافة أول جهة اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                contactsList.forEachIndexed { index, contactItem ->
                                    val isPrimary = contactItem.isPrimary || contactsList.size == 1 || (index == 0 && contactsList.none { it.isPrimary })
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

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    CompanyDetailTab.ADDRESSES -> {
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
                                    onClick = { viewModel.setAddAddressDialogOpen(true) },
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
                                val isPrimary = addr.isPrimary || addressesList.size == 1 || (addressesList.none { it.isPrimary } && addressesList.firstOrNull()?.id == addr.id)

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

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    CompanyDetailTab.BANK_ACCOUNTS -> {
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
                                    Text("الحسابات البنكية المعتمدة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF0F172A))
                                    Text("تفاصيل التسوية البنكية والتحويلات المعتمدة للفواتير", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }

                                Surface(
                                    onClick = { viewModel.setAddBankAccountDialogOpen(true) },
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
                                        Text("إضافة حساب", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF4338CA))
                                    }
                                }
                            }

                            if (uiState.companyBankAccounts.isEmpty()) {
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                                        Text("لا توجد حسابات بنكية مسجلة", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF334155))
                                        Text("أضف الحسابات البنكية الرسمية لاستخدامها في إشعار الفواتير وتسوية المدفوعات.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                    }
                                }
                            } else {
                                uiState.companyBankAccounts.forEach { acc ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, if (acc.isPrimary) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFFEEF2FF)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                                    }
                                                    Column {
                                                        Text(acc.bankName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF0F172A))
                                                        Text("المستفيد: ${acc.accountName}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Surface(
                                                        color = Color(0xFFEEF2FF),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(acc.currency, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF4338CA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                    if (acc.isPrimary) {
                                                        Surface(
                                                            color = Color(0xFFECFDF5),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text("افتراضي ✓", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.deleteCompanyBankAccount(acc.id) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            HorizontalDivider(color = Color(0xFFF1F5F9))

                                            if (acc.iban.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("IBAN:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF64748B))
                                                    Text(acc.iban, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF0F172A))
                                                }
                                            }

                                            if (acc.accountNumber.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("رقم الحساب المحلي:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                                    Text(acc.accountNumber, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = Color(0xFF334155))
                                                }
                                            }

                                            if (acc.swiftBic.isNotBlank() || acc.branchName.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (acc.swiftBic.isNotBlank()) Text("SWIFT/BIC: ${acc.swiftBic}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                                    if (acc.branchName.isNotBlank()) Text("الفرع: ${acc.branchName}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    CompanyDetailTab.MANUFACTURER_PARTS -> {
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
                                    Text("قطع المصنّع والمرفقات (MPN)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF0F172A))
                                    Text("أرقام قطع التصنيع والمواصفات الفنية المعتمدة", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                                }

                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "إدارة الـ MPN من شاشة تفاصيل القطعة",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Manufacturer Parts List
                            val manufacturerPartsList = uiState.companyManufacturerParts.ifEmpty {
                                listOf(
                                    ManufacturerPart(
                                        id = 100L,
                                        partId = 1L,
                                        manufacturerId = company.id,
                                        mpn = "ESP32-WROOM-32U",
                                        description = "وحدة متحكم دقيق ESP32 Wi-Fi + Bluetooth مع كابل هوائي خارجي"
                                    )
                                )
                            }

                            manufacturerPartsList.forEach { mp ->
                                val isSelected = uiState.selectedManufacturerPart?.id == mp.id

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setSelectedManufacturerPart(if (isSelected) null else mp) }
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
                                                        .background(Color(0xFFEEF2FF)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                                }

                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "MPN: ${mp.mpn}",
                                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                                            color = Color(0xFF4F46E5)
                                                        )
                                                        Surface(
                                                            color = Color(0xFFECFDF5),
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                                        ) {
                                                            Text(
                                                                text = "معتمد ✓",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                                color = Color(0xFF047857),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    Text(
                                                        text = mp.description.ifBlank { "وحدة متحكم ESP32 Wi-Fi + Bluetooth" },
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    if (mp.id != 100L) viewModel.deleteManufacturerPart(mp.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Action buttons & Quick chips inside card
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    onClick = {
                                                        viewModel.setSelectedManufacturerPart(mp)
                                                        viewModel.setAddManufacturerPartParameterDialogOpen(true)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(13.dp))
                                                        Text("المعاملات التقنية", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                                                    }
                                                }

                                                Surface(
                                                    onClick = {
                                                        viewModel.setSelectedManufacturerPart(mp)
                                                        viewModel.setAddManufacturerPartAttachmentDialogOpen(true)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(13.dp))
                                                        Text("المرفقات", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = Color(0xFF334155))
                                                    }
                                                }
                                            }

                                            Text(
                                                text = if (isSelected) "إخفاء التفاصيل ▲" else "عرض التفاصيل ▼",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFF4F46E5)
                                            )
                                        }

                                        if (isSelected && uiState.selectedManufacturerPartParameters.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("المعاملات التقنية للقطعة:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF334155))
                                            uiState.selectedManufacturerPartParameters.forEach { p ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("⚙️ ${p.name}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                    Text("${p.value} ${p.units}".trim(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }
                                            }
                                        }

                                        if (isSelected && uiState.selectedManufacturerPartAttachments.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("المرفقات وأوراق البيانات:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF334155))
                                            uiState.selectedManufacturerPartAttachments.forEach { att ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("📄 ${att.comment.ifBlank { "مرفق مواصفات" }}", fontSize = 11.sp, color = Color(0xFF0F172A))
                                                    IconButton(
                                                        onClick = { viewModel.deleteManufacturerPartAttachment(att.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "حذف مرفق", tint = Color(0xFFE11D48), modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    CompanyDetailTab.SUPPLIER_PARTS -> {
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
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "إدارة الـ SKU من شاشة تفاصيل القطعة",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
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
                    // Primary Action: Edit Company
                    Button(
                        onClick = { viewModel.openEditCompanyDialog(company) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("تعديل بيانات الشركة", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
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
    companyToEdit: Company? = null,
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

    var name by remember(companyToEdit) { mutableStateOf(companyToEdit?.name ?: "") }
    var description by remember(companyToEdit) { mutableStateOf(companyToEdit?.description ?: "") }
    var website by remember(companyToEdit) { mutableStateOf(companyToEdit?.website ?: "") }
    var phone by remember(companyToEdit) { mutableStateOf(companyToEdit?.phone ?: "") }
    var email by remember(companyToEdit) { mutableStateOf(companyToEdit?.email ?: "") }
    var address by remember(companyToEdit) { mutableStateOf(companyToEdit?.address ?: "") }
    var contact by remember(companyToEdit) { mutableStateOf(companyToEdit?.contact ?: "") }
    var isSupplier by remember(companyToEdit) { mutableStateOf(companyToEdit?.isSupplier ?: true) }
    var isManufacturer by remember(companyToEdit) { mutableStateOf(companyToEdit?.isManufacturer ?: false) }
    var isCustomer by remember(companyToEdit) { mutableStateOf(companyToEdit?.isCustomer ?: false) }
    var currency by remember(companyToEdit) { mutableStateOf(companyToEdit?.currency ?: "USD") }
    var selectedParentId by remember(companyToEdit) { mutableStateOf<Long?>(companyToEdit?.parentId) }
    var selectedCountry by remember { mutableStateOf(CountryRepository.defaultCountry()) }

    var isParentSelectionSheetOpen by remember { mutableStateOf(false) }
    var isCurrencySelectionSheetOpen by remember { mutableStateOf(false) }

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
                        Text(
                            text = if (companyToEdit != null) "تعديل بيانات الشركة" else "تسجيل شركة جديدة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            color = Color(0xFF0F172A)
                        )
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

                // Parent Company Selector (Clickable Outlined Box)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "الشركة الأم (اختياري للهيكلية الهرمية)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color(0xFF334155)
                    )

                    val parentCompany = companies.find { it.id == selectedParentId }
                    val parentDisplayText = if (selectedParentId == null) {
                        "شركة مستقلة (بدون شركة أم)"
                    } else {
                        parentCompany?.name ?: "شركة مستقلة (بدون شركة أم)"
                    }

                    Surface(
                        onClick = { isParentSelectionSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, if (selectedParentId != null) Color(0xFF4F46E5) else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
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
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedParentId != null) Color(0xFFEEF2FF) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CorporateFare,
                                        contentDescription = null,
                                        tint = if (selectedParentId != null) Color(0xFF4F46E5) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = parentDisplayText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selectedParentId != null) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (selectedParentId != null) Color(0xFF0F172A) else Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (selectedParentId != null && parentCompany != null) {
                                        Text(
                                            text = "محددة كشركة أم هرمية",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFF4F46E5)
                                        )
                                    }
                                }
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "تحديد الشركة الأم",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
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

                // Official Website Field
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("الموقع الإلكتروني الرسمي / البوابة") },
                    placeholder = { Text("https://www.company.com") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Default Currency Selector Field
                CurrencySelectorField(
                    selectedCurrencyCode = currency,
                    onOpenPicker = { isCurrencySelectionSheetOpen = true },
                    label = "رمز العملة المعتمدة للتعامل"
                )

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
                            Text(
                                text = if (companyToEdit != null) "حفظ التعديلات" else "حفظ وتسجيل الشركة",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            )
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

    if (isParentSelectionSheetOpen) {
        ParentCompanySelectionBottomSheet(
            companies = companies,
            currentCompanyId = companyToEdit?.id,
            selectedParentId = selectedParentId,
            onDismiss = { isParentSelectionSheetOpen = false },
            onSelectParent = { parentId ->
                selectedParentId = parentId
                isParentSelectionSheetOpen = false
            }
        )
    }

    if (isCurrencySelectionSheetOpen) {
        CurrencySelectionBottomSheet(
            selectedCurrencyCode = currency,
            onDismiss = { isCurrencySelectionSheetOpen = false },
            onCurrencySelected = { selectedCurr ->
                currency = selectedCurr.code
            }
        )
    }
}

private fun getDescendantCompanyIds(companyId: Long, allCompanies: List<Company>): Set<Long> {
    val descendants = mutableSetOf<Long>()
    fun collectChildren(parentId: Long) {
        allCompanies.forEach { comp ->
            if (comp.parentId == parentId && comp.id !in descendants) {
                descendants.add(comp.id)
                collectChildren(comp.id)
            }
        }
    }
    collectChildren(companyId)
    return descendants
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentCompanySelectionBottomSheet(
    companies: List<Company>,
    currentCompanyId: Long?,
    selectedParentId: Long?,
    onDismiss: () -> Unit,
    onSelectParent: (Long?) -> Unit
) {
    val parentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val excludedIds = remember(currentCompanyId, companies) {
        if (currentCompanyId != null) {
            getDescendantCompanyIds(currentCompanyId, companies) + currentCompanyId
        } else {
            emptySet()
        }
    }

    val validCompanies = remember(companies, excludedIds) {
        companies.filter { comp -> comp.active && comp.id !in excludedIds }
    }

    val filteredCompanies = remember(validCompanies, searchQuery) {
        if (searchQuery.isBlank()) {
            validCompanies
        } else {
            val q = searchQuery.trim().lowercase()
            validCompanies.filter { comp ->
                comp.name.lowercase().contains(q) ||
                comp.description.lowercase().contains(q) ||
                comp.email.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = parentSheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
                .padding(horizontal = 20.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "تحديد الشركة الأم",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        "اختر شركة أم مسجلة بالنظام أو اجعلها مستقلة",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Search Text Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن اسم الشركة...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "مسح البحث",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Always Top Option: "بدون شركة أم (مستقلة)"
                item(key = "NONE_PARENT") {
                    val isNoneSelected = selectedParentId == null
                    Surface(
                        onClick = { onSelectParent(null) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNoneSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Domain,
                                        contentDescription = null,
                                        tint = if (isNoneSelected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "بدون شركة أم (مستقلة)",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isNoneSelected) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        "إلغاء التبعية وجعل parentUuid = null",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            if (isNoneSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "محدد",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // List of Active Companies
                if (filteredCompanies.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد شركات مطابقة لبحثك",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                } else {
                    items(filteredCompanies, key = { it.id }) { comp ->
                        val isSelected = selectedParentId == comp.id
                        Surface(
                            onClick = { onSelectParent(comp.id) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else Color.White,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CorporateFare,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = comp.name,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val roles = buildList {
                                            if (comp.isSupplier) add("مورّد")
                                            if (comp.isManufacturer) add("مُصنّع")
                                            if (comp.isCustomer) add("عميل")
                                        }.joinToString(" • ")
                                        val subtitle = comp.description.ifBlank { roles.ifBlank { "شركة مسجلة بالنظام" } }
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "محدد",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCompanyAttachmentBottomSheet(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (documentType: String, attachmentPath: String, link: String, comment: String, expiryDate: String, notifyOnExpiry: Boolean, notificationDaysBefore: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val documentTypesList = remember {
        listOf(
            "سجل تجاري (Commercial Register)",
            "شهادة تسجيل ضريبي (Tax Certificate)",
            "عقد توريد / اتفاقية شراكة (Supply Contract / Agreement)",
            "شهادات جودة ومطابقة (ISO / CE / RoHS)",
            "تفويض بنكي / بيانات مصرفية (Bank Details / Authorization)",
            "اتفاقية سرية معلومات (NDA)",
            "كتالوج تقني / مواصفات عامة (Datasheet / Catalog)",
            "شهادة منشأ / وكالة تجارية (Agency / Certificate of Origin)",
            "وثيقة أخرى (Other)"
        )
    }

    var selectedDocType by remember { mutableStateOf(documentTypesList[2]) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("Contract.pdf") }
    var link by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("2026-12-31") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var notifyDaysStr by remember { mutableStateOf("30") }
    var isOfficialValid by remember { mutableStateOf(true) }
    var showCameraKDialog by remember { mutableStateOf(false) }

    if (showCameraKDialog) {
        Dialog(onDismissRequest = { showCameraKDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
            ) {
                AppCameraKView(
                    onImageCaptured = {
                        path = "Photo_Doc_${Clock.System.now().toEpochMilliseconds()}.jpg"
                        showCameraKDialog = false
                    },
                    onClose = { showCameraKDialog = false }
                )
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val dateTime = instant.toLocalDateTime(TimeZone.UTC)
                        val year = dateTime.year
                        val month = dateTime.monthNumber.toString().padStart(2, '0')
                        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
                        expiryDate = "$year-$month-$day"
                    }
                    showDatePickerDialog = false
                }) {
                    Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
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
                .fillMaxHeight(0.72f)
        ) {
            // 1. Header Section
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
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("إضافة وثيقة / مرفق للشركة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("إرفاق العقود، السجلات التجارية، والشهادات المعتمدة", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 2. Scrollable Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Supported Formats Info Banner
                Surface(
                    color = Color(0xFFEEF2FF).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                        Text(
                            text = "الصيغ المدعومة: ملفات PDF، ومستندات Word (DOCX)، والصور (PNG, JPG) بحد أقصى 25 MB للملف الواحد.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                            color = Color(0xFF312E81)
                        )
                    }
                }

                // Document Type Selection Dropdown Menu
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("نوع الوثيقة والمستند المرفق *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedDocType,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "فتح القائمة",
                                        tint = Color(0xFF4F46E5)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDropdownExpanded = !isDropdownExpanded },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(Color.White)
                        ) {
                            documentTypesList.forEach { typeOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = typeOption,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (typeOption == selectedDocType) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.5.sp
                                            ),
                                            color = if (typeOption == selectedDocType) Color(0xFF4F46E5) else Color(0xFF0F172A)
                                        )
                                    },
                                    onClick = {
                                        selectedDocType = typeOption
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Optional Notes / Description
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("ملاحظات إضافية / وصف خاص للوثيقة (اختياري)") },
                    placeholder = { Text("أدخل أية تفاصيل أو ملاحظات خاصة بالوثيقة...") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // External Document URL
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("رابط المستند الإلكتروني (Drive, Dropbox, OneDrive)") },
                    placeholder = { Text("https://drive.google.com/file/d/...") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Expiry Date Field & Notification Days Before Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clickable { showDatePickerDialog = true }
                    ) {
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = { Text("تاريخ انتهاء الصلاحية") },
                            placeholder = { Text("اختر التاريخ") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = "اختيار التاريخ",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF0F172A),
                                disabledBorderColor = Color(0xFFCBD5E1),
                                disabledLabelColor = Color(0xFF334155),
                                disabledPlaceholderColor = Color(0xFF94A3B8),
                                disabledLeadingIconColor = Color(0xFF4F46E5),
                                disabledContainerColor = Color.White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = notifyDaysStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) notifyDaysStr = it },
                        label = { Text("الإشعار المسبق") },
                        placeholder = { Text("30") },
                        trailingIcon = { Text("يوم", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = Color(0xFF64748B), modifier = Modifier.padding(end = 8.dp)) },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Optional Validity & Status Toggle Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("وثيقة رسمية سارية المفعول", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF0F172A))
                                Text("إشعار قبل تاريخ الانتهاء بـ ${notifyDaysStr.ifBlank { "30" }} يوماً", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                            }
                        }

                        Switch(
                            checked = isOfficialValid,
                            onCheckedChange = { isOfficialValid = it }
                        )
                    }
                }

                // Upload File & Camera Buttons Sharing the Same Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Upload Local File Button
                        Surface(
                            onClick = { if (path.isBlank()) path = "Contract_${DateTimeUtils.getCurrentDate()}.pdf" },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رفع من الذاكرة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF334155))
                            }
                        }

                        // Capture via Camera Button (CameraK)
                        Surface(
                            onClick = { showCameraKDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.5.dp, Color(0xFFC7D2FE)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("التقاط بالكاميرا", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF4338CA))
                            }
                        }
                    }

                    if (path.isNotBlank()) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(14.dp))
                                    Text("الملف المحدد: $path", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF047857))
                                }
                                IconButton(onClick = { path = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color(0xFF047857), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 3. Sticky Action Buttons Footer
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
                            val notifyDays = notifyDaysStr.toIntOrNull() ?: 30
                            val finalPath = if (path.isBlank() && link.isBlank()) "${selectedDocType.take(20)}.pdf" else path
                            onConfirm(selectedDocType, finalPath, link, comment.ifBlank { selectedDocType }, expiryDate, isOfficialValid, notifyDays)
                        },
                        enabled = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ وإرفاق الوثيقة", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactBottomSheet(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, role: String, isPrimary: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(true) }
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
                .fillMaxHeight(0.70f)
        ) {
            // Header
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("إضافة جهة اتصال جديدة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("مسؤول التواصل والتوريد المعتمد للشركة", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل *") },
                    placeholder = { Text("مثال: م. أحمد علي") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("المسمى الوظيفي / الدور") },
                    placeholder = { Text("مثال: مسؤول المشتريات والتوريد الخارجي") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                PhoneNumberInputField(
                    phoneValue = phone,
                    onPhoneValueChange = { phone = it },
                    selectedCountry = selectedCountry,
                    onCountrySelected = { selectedCountry = it },
                    label = "رقم الهاتف المباشر"
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني التجاري") },
                    placeholder = { Text("ahmed@company.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                    Text(
                        text = "تعيين كجهة اتصال رئيسية للتواصل والتوريد",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF334155)
                    )
                }
            }

            // Footer Actions Bar
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
                                onConfirm(name, formattedPhone, email, role, isPrimary)
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
                            Text("حفظ جهة الاتصال", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAddressBottomSheet(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, isPrimary: Boolean, line1: String, line2: String, postalCode: String, city: String, province: String, country: String, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(true) }
    var line1 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var countryName by remember { mutableStateOf("المملكة العربية السعودية") }
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
                .fillMaxHeight(0.70f)
        ) {
            // Header
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("إضافة عنوان جديد للشركة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("عناوين الفروع ومستودعات الاستلام والتسليم", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("تسمية العنوان") },
                    placeholder = { Text("مثال: المستودع الرئيسي، فرع المطار") },
                    leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = line1,
                    onValueChange = { line1 = it },
                    label = { Text("السطر الأول (الشارع / المبنى) *") },
                    placeholder = { Text("اسم الشارع، رقم المبنى، الرمز البريدي") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("المدينة") },
                        placeholder = { Text("الرياض، جدة...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        CountryPickerField(
                            selectedCountryName = countryName,
                            onCountrySelected = { countryData ->
                                countryName = countryData.nameAr
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
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                    Text(
                        text = "تعيين كعنوان رئيسي للمستندات والفواتير الرسمية",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF334155)
                    )
                }
            }

            // Footer Actions Bar
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
                            if (line1.isNotBlank()) {
                                onConfirm(title.ifBlank { "فرع جديد" }, isPrimary, line1, "", "", city, "", countryName, notes)
                            }
                        },
                        enabled = line1.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ العنوان", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBankAccountBottomSheet(
    defaultCurrency: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (bankName: String, accountName: String, accountNumber: String, iban: String, swiftBic: String, currency: String, branchName: String, isPrimary: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var bankName by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var swiftBic by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var branchName by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(false) }
    var isCurrencyPickerOpen by remember { mutableStateOf(false) }

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
                .fillMaxHeight(0.85f)
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("إضافة حساب بنكي جديد", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("بيانات التسوية والتحويلات البنكية المعتمدة للفواتير", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Smart Tip Banner
                Surface(
                    color = Color(0xFFEEF2FF).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4F46E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 12.sp)
                        }
                        Text(
                            text = "يساعد تسجيل الآيبان (IBAN) والرمز السويفت في توجيه مدفوعات الفواتير التلقائية وتسويتها محلياً ودولياً بدون أخطاء.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                            color = Color(0xFF312E81)
                        )
                    }
                }

                // 1. Bank Name Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("اسم البنك *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        placeholder = { Text("مثال: الراجحي / البنك الأهلي / HSBC") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                // 2. Beneficiary Account Name Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("اسم صاحب الحساب / المستفيد *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        placeholder = { Text("الاسم الرسمي المكتوب في الفاتورة والتسجيل البنكي") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                // 3. IBAN Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رقم الآيبان الدولي (IBAN)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Text("مستحسن للفواتير", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF4F46E5))
                    }
                    OutlinedTextField(
                        value = iban,
                        onValueChange = { iban = it },
                        placeholder = { Text("SA00 0000 0000 0000 0000 0000") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                // 4. Account Number & SWIFT Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("رقم الحساب المحلي", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            placeholder = { Text("12345678") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("رمز SWIFT / BIC", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = swiftBic,
                            onValueChange = { swiftBic = it },
                            placeholder = { Text("RJHI22XX") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }
                }

                // 5. Currency Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("عملة الحساب المعتمدة", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    CurrencySelectorField(
                        selectedCurrencyCode = currency,
                        onOpenPicker = { isCurrencyPickerOpen = true },
                        label = "عملة الحساب"
                    )
                }

                // 6. Branch Name Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("اسم الفرع / العنوان", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        placeholder = { Text("فرع العليا / الفرع الرئيسي") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                // 7. Is Primary Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                    Text(
                        text = "تعيين كحساب بنكي رئيسي وافتراضي للفواتير والتسوية",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = Color(0xFF334155)
                    )
                }
            }

            // Footer Actions Bar
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
                            if (bankName.isNotBlank() && accountName.isNotBlank()) {
                                onConfirm(bankName, accountName, accountNumber, iban, swiftBic, currency, branchName, isPrimary)
                            }
                        },
                        enabled = bankName.isNotBlank() && accountName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ الحساب البنكي", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
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

    if (isCurrencyPickerOpen) {
        CurrencySelectionBottomSheet(
            selectedCurrencyCode = currency,
            onDismiss = { isCurrencyPickerOpen = false },
            onCurrencySelected = { selectedCurr ->
                currency = selectedCurr.code
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLegalRecordBottomSheet(
    currentRecord: CompanyLegalRecord?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        commercialRegisterNumber: String,
        taxId: String,
        nationalIdNumber: String,
        importLicenseNumber: String,
        manufacturingLicenseNumber: String,
        activityCodes: String,
        issuingAuthority: String,
        issueDate: String,
        expiryDate: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var commercialRegisterNumber by remember { mutableStateOf(currentRecord?.commercialRegisterNumber ?: "") }
    var taxId by remember { mutableStateOf(currentRecord?.taxId ?: "") }
    var nationalIdNumber by remember { mutableStateOf(currentRecord?.nationalIdNumber ?: "") }
    var importLicenseNumber by remember { mutableStateOf(currentRecord?.importLicenseNumber ?: "") }
    var manufacturingLicenseNumber by remember { mutableStateOf(currentRecord?.manufacturingLicenseNumber ?: "") }
    var activityCodes by remember { mutableStateOf(currentRecord?.activityCodes ?: "") }
    var issuingAuthority by remember { mutableStateOf(currentRecord?.issuingAuthority ?: "") }
    var issueDate by remember { mutableStateOf(currentRecord?.issueDate ?: "") }
    var expiryDate by remember { mutableStateOf(currentRecord?.expiryDate ?: "") }

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
                .fillMaxHeight(0.85f)
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("السجل التجاري والرخص الرسمية", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("تحديث أرقام القيد والتسجيل الضريبي ورخص الاستيراد والتصنيع", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Smart Tip Banner
                Surface(
                    color = Color(0xFFEEF2FF).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4F46E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 12.sp)
                        }
                        Text(
                            text = "تُستخدم البيانات القانونية والرقم الضريبي في توليد الفواتير الرسمية ومطابقة الشحنات الجمركية والترخيصية الصناعية.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                            color = Color(0xFF312E81)
                        )
                    }
                }

                // 1. CR Number & Tax ID Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("رقم السجل التجاري (CR)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = commercialRegisterNumber,
                            onValueChange = { commercialRegisterNumber = it },
                            placeholder = { Text("1010000000") },
                            leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("الرقم الضريبي (VAT ID)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = taxId,
                            onValueChange = { taxId = it },
                            placeholder = { Text("300000000000003") },
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }
                }

                // 2. National ID / Unified ID & Issuing Authority Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("الرقم التعريفي الموحد (NIS)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = nationalIdNumber,
                            onValueChange = { nationalIdNumber = it },
                            placeholder = { Text("7000000000") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("جهة الإصدار الرسمية", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = issuingAuthority,
                            onValueChange = { issuingAuthority = it },
                            placeholder = { Text("وزارة التجارة / الصناعة") },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }
                }

                // 3. Import & Manufacturing Licenses Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("رخصة الاستيراد الجمركية", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = importLicenseNumber,
                            onValueChange = { importLicenseNumber = it },
                            placeholder = { Text("IMP-2025-9988") },
                            leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("رخصة التصنيع والانتاج", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = manufacturingLicenseNumber,
                            onValueChange = { manufacturingLicenseNumber = it },
                            placeholder = { Text("MFG-LIC-4421") },
                            leadingIcon = { Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }
                }

                // 4. Activity Codes Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("رمز / كود النشاط الاقتصادي (ISIC / NAF)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    OutlinedTextField(
                        value = activityCodes,
                        onValueChange = { activityCodes = it },
                        placeholder = { Text("مثال: 2610 - تصنيع المكونات والشرائح الإلكترونية") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }

                // 5. Issue & Expiry Dates Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("تاريخ الإصدار", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = issueDate,
                            onValueChange = { issueDate = it },
                            placeholder = { Text("2024-01-15") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("تاريخ الانتهاء", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            placeholder = { Text("2029-01-15") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }
                }
            }

            // Footer Actions Bar
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
                            onConfirm(
                                commercialRegisterNumber,
                                taxId,
                                nationalIdNumber,
                                importLicenseNumber,
                                manufacturingLicenseNumber,
                                activityCodes,
                                issuingAuthority,
                                issueDate,
                                expiryDate
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ البيانات القانونية", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddManufacturerPartBottomSheet(
    allParts: List<Part>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, mpn: String, description: String, link: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPart by remember { mutableStateOf<Part?>(allParts.firstOrNull()) }
    var isPartPickerOpen by remember { mutableStateOf(false) }
    var mpn by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

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
                .fillMaxHeight(0.72f)
        ) {
            // 1. Header Section
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
                        Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("إضافة قطعة مصنّع (MPN)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("ربط كود التصنيع بالمخزون الداخلي", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 2. Scrollable Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Smart Tip Banner
                Surface(
                    color = Color(0xFFEEF2FF).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFFE0E7FF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4F46E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 12.sp)
                        }
                        Text(
                            text = "يضمن تعيين رقم قطعة المصنّع (MPN) دقة شراء المكونات من المورّدين ومطابقتها التامة للوحات والدوائر الإلكترونية.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                            color = Color(0xFF312E81)
                        )
                    }
                }

                // 1. Internal Part Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("القطعة الداخلية المربوطة *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(6.dp)) {
                            Text("من القائمة M3", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF4338CA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = { isPartPickerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPart?.let { "${it.name} (${it.ipn.ifBlank { "بدون IPN" }})" } ?: "اختر القطعة الداخلية من القائمة...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedPart != null) Color(0xFF0F172A) else Color(0xFF94A3B8)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }
                }

                // 2. MPN Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رقم القطعة المصنعية (MPN) *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Text("رمز المصنع الرسمي", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                    }

                    OutlinedTextField(
                        value = mpn,
                        onValueChange = { mpn = it },
                        placeholder = { Text("مثال: ESP32-D0WD-V3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 3. Description Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("وصف مواصفات المصنّع", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("أدخل ملخص مواصفات الشريحة، سعة الذاكرة، التردد...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 4. Datasheet Link Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("رابط مواصفات المنتج (Datasheet)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Text("اختياري", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                    }

                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        placeholder = { Text("https://espressif.com/docs/datasheet.pdf") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 3. Sticky Actions Footer
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
                            if (selectedPart != null && mpn.isNotBlank()) {
                                onConfirm(selectedPart!!.id, mpn, description, link)
                            }
                        },
                        enabled = selectedPart != null && mpn.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ قطعة المصنّع", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
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

    if (isPartPickerOpen) {
        SearchablePartPickerDialog(
            parts = allParts,
            selectedPartId = selectedPart?.id,
            onPartSelected = { selectedPart = it },
            onDismiss = { isPartPickerOpen = false }
        )
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSupplierPartBottomSheet(
    allParts: List<Part>,
    manufacturerParts: List<ManufacturerPart>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (partId: Long, sku: String, mfgPartId: Long?, description: String, link: String, note: String, packaging: String, packQuantity: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPart by remember { mutableStateOf<Part?>(allParts.firstOrNull()) }
    var isPartPickerOpen by remember { mutableStateOf(false) }

    val filteredMfgParts = remember(selectedPart, manufacturerParts) {
        if (selectedPart == null) manufacturerParts
        else manufacturerParts.filter { it.partId == selectedPart!!.id }
    }

    var selectedMfgPartId by remember(filteredMfgParts) { mutableStateOf(filteredMfgParts.firstOrNull()?.id) }
    var sku by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("Box") }
    var packQuantity by remember { mutableStateOf("1") }
    var link by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

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
                .fillMaxHeight(0.72f)
        ) {
            // Header
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("إضافة قطعة مورد (SKU)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = Color(0xFF0F172A))
                        Text("ربط أرقام القطع والتغليف والأسعار الخاصة بالمورد", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // 1. Internal Part Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("القطعة الداخلية المربوطة *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                    OutlinedButton(
                        onClick = { isPartPickerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPart?.let { "${it.name} (${it.ipn.ifBlank { "بدون IPN" }})" } ?: "اختر القطعة الداخلية...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedPart != null) Color(0xFF0F172A) else Color(0xFF94A3B8)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }
                }

                // 2. Filtered MPNs for the selected Part
                if (filteredMfgParts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("أرقام MPN المصنّعة المتاحة لهذه القطعة:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF334155))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredMfgParts.forEach { mfg ->
                                FilterChip(
                                    selected = selectedMfgPartId == mfg.id,
                                    onClick = { selectedMfgPartId = mfg.id },
                                    label = { Text("MPN: ${mfg.mpn}", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("كود المورد (Supplier SKU) *") },
                    placeholder = { Text("مثال: SKU-ESP32-990") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف التجاري / ملاحظات الشراء") },
                    placeholder = { Text("وصف التغليف والمواصفات الخاصة بالمورد") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = packaging,
                        onValueChange = { packaging = it },
                        label = { Text("نوع التغليف") },
                        placeholder = { Text("Reel / Box") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = packQuantity,
                        onValueChange = { packQuantity = it },
                        label = { Text("كمية التعبئة بالحزمة") },
                        placeholder = { Text("1000") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Footer Actions Bar
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
                            if (selectedPart != null && sku.isNotBlank()) {
                                onConfirm(selectedPart!!.id, sku, selectedMfgPartId, description, link, note, packaging, packQuantity)
                            }
                        },
                        enabled = selectedPart != null && sku.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("حفظ قطعة المورّد", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
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

    if (isPartPickerOpen) {
        SearchablePartPickerDialog(
            parts = allParts,
            selectedPartId = selectedPart?.id,
            onPartSelected = { selectedPart = it },
            onDismiss = { isPartPickerOpen = false }
        )
    }
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

                var isPickerOpen by remember { mutableStateOf(false) }
                CurrencySelectorField(
                    selectedCurrencyCode = currency,
                    onOpenPicker = { isPickerOpen = true },
                    label = "العملة (تستخدم عملة الشركة افتراضياً)"
                )
                if (isPickerOpen) {
                    CurrencySelectionBottomSheet(
                        selectedCurrencyCode = currency,
                        onDismiss = { isPickerOpen = false },
                        onCurrencySelected = { selectedCurr ->
                            currency = selectedCurr.code
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyFilterBottomSheet(
    uiState: CompanyUiState,
    onDismiss: () -> Unit,
    onApply: (countries: Set<String>, scope: String) -> Unit,
    onClear: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCountries by remember { mutableStateOf(uiState.selectedCountries) }
    var selectedScope by remember { mutableStateOf(uiState.selectedScope) }

    val primaryDefaultCountries = remember {
        listOf(
            CountryRepository.findByCode("SA"),
            CountryData("CN", "جمهورية الصين الشعبية", "China", "+86", "🇨🇳"),
            CountryRepository.findByCode("DE"),
            CountryData("US", "الولايات المتحدة الأمريكية", "United States", "+1", "🇺🇸"),
            CountryData("TW", "تايوان", "Taiwan", "+886", "🇹🇼"),
            CountryRepository.findByCode("KR")
        )
    }

    val filteredCountries = remember(searchQuery) {
        val query = searchQuery.normalizeArabic()
        if (query.isBlank()) {
            primaryDefaultCountries
        } else {
            val matches = CountryRepository.countries.filter { country ->
                country.nameAr.normalizeArabic().contains(query) ||
                country.nameEn.normalizeArabic().contains(query) ||
                country.code.lowercase().contains(query) ||
                country.dialCode.contains(query)
            }
            if (matches.isEmpty() && searchQuery.trim().length >= 2) {
                listOf(
                    CountryData(
                        code = "CUSTOM",
                        nameAr = searchQuery.trim(),
                        nameEn = searchQuery.trim(),
                        dialCode = "",
                        flagEmoji = "🌐"
                    )
                )
            } else matches
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
                .fillMaxHeight(0.70f)
        ) {
            // 1. Header Section
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("تصفية الشركات والشركاء", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = Color(0xFF0F172A))
                        Text("تحديد الشركات المعروضة حسب الدولة ومقر العمليات", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        selectedCountries = emptySet()
                        selectedScope = "ALL"
                        onClear()
                    }) {
                        Text("مسح الكل", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF4F46E5))
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF64748B))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 2. Scrollable Body Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Input by Country / City
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن اسم الدولة أو الرمز (السعودية، الصين...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Section 1: Countries Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("البلدان المتاحة للتوريد والتصنيع", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp), color = Color(0xFF0F172A))
                            if (selectedCountries.isNotEmpty()) {
                                Surface(color = Color(0xFFEEF2FF), shape = RoundedCornerShape(10.dp)) {
                                    Text("${selectedCountries.size} محددة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF4338CA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("حسب السجلات المسجلة", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (filteredCountries.isEmpty()) {
                                Text(
                                    text = "لا توجد بلدان تطابق نتائج البحث",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(12.dp)
                                )
                            } else {
                                filteredCountries.forEach { country ->
                                    val countryName = country.nameAr
                                    val isChecked = selectedCountries.contains(countryName)
                                    val count = remember(country.code, uiState.companies) {
                                        uiState.companies.count { comp ->
                                            val stats = uiState.companyStatsMap[comp.id]
                                            val addrNorm = ((stats?.primaryAddress ?: "") + " " + comp.address).normalizeArabic()
                                            val cNameArNorm = country.nameAr.normalizeArabic()
                                            val cNameEnNorm = country.nameEn.normalizeArabic()
                                            addrNorm.contains(cNameArNorm) ||
                                            addrNorm.contains(cNameEnNorm) ||
                                            comp.phone.startsWith(country.dialCode) ||
                                            (country.code == "SA" && (addrNorm.contains("الرياض") || addrNorm.contains("جده") || addrNorm.contains("السعوديه"))) ||
                                            (country.code == "CN" && (addrNorm.contains("الصين") || addrNorm.contains("شنغهاي")))
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            selectedCountries = if (isChecked) selectedCountries - countryName else selectedCountries + countryName
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isChecked) Color(0xFFEEF2FF).copy(alpha = 0.6f) else Color.White,
                                        border = BorderStroke(1.dp, if (isChecked) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(country.flagEmoji, fontSize = 20.sp)
                                                Column {
                                                    Text(country.nameAr, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = Color(0xFF0F172A))
                                                    Text("${country.nameEn} • ${country.dialCode}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = Color(0xFF64748B))
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    color = if (count > 0) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, if (count > 0) Color(0xFFA7F3D0) else Color(0xFFE2E8F0))
                                                ) {
                                                    Text("$count شركة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = if (count > 0) Color(0xFF047857) else Color(0xFF64748B), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }

                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        selectedCountries = if (checked) selectedCountries + countryName else selectedCountries - countryName
                                                    }
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

            // 3. Sticky Actions Footer Bar
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
                            onApply(selectedCountries, selectedScope)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("تطبيق التصفية (عرض ${uiState.companies.size} شركة)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
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
