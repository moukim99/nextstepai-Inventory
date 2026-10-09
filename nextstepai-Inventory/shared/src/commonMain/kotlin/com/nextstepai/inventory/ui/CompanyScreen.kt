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
import androidx.compose.foundation.lazy.itemsIndexed
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
                        .height(48.dp),
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
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(uiState.companies, key = { index, company -> "company-${company.id}-$index" }) { _, company ->
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

            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddCompanyDialog() },
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
            onConfirm = { name, desc, web, phone, email, addr, contact, isSup, isMan, isCust, curr, parentId, imageUrl, active, notes ->
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
                        parentId = parentId,
                        imageUrl = imageUrl,
                        active = active,
                        notes = notes
                    )
                } else {
                    viewModel.addCompany(
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
                        parentId = parentId,
                        imageUrl = imageUrl,
                        active = active,
                        notes = notes
                    )
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
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
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


