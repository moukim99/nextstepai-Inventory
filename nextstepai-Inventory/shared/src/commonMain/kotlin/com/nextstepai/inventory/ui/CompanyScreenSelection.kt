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

internal fun getDescendantCompanyIds(companyId: Long, allCompanies: List<Company>): Set<Long> {
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
internal fun ParentCompanySelectionBottomSheet(
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
                .imePadding()
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
                    itemsIndexed(filteredCompanies, key = { index, comp -> "parent-comp-${comp.id}-$index" }) { _, comp ->
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
internal fun CompanyFilterBottomSheet(
    uiState: CompanyUiState,
    onDismiss: () -> Unit,
    onApply: (countries: Set<String>, scope: String) -> Unit,
    onClear: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
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
                .imePadding()
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
                        Text("تحديد الشركات المعروضة حسب نوع العلاقة والبلدان", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
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
                    placeholder = { Text("ابحث عن اسم الشركة، الدولة أو الرمز...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. شريط التبويبات المزدوجة
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { selectedTab = 0 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🏢 نوع وطبيعة العلاقة",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == 0) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = 1 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 1) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🌐 النطاق والبلدان المعتمدة",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    // تبويب نوع العلاقة
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val scopes = listOf(
                                Triple("ALL", "جميع الشركات والشركاء", "عرض كافة الموردين والمصنعين والعملاء"),
                                Triple("SUPPLIERS", "شركات التوريد فقط (Suppliers)", "موردو المكونات والقطع الإلكترونية"),
                                Triple("MANUFACTURERS", "الشركات المصنعة فقط (Manufacturers)", "المصنعون الأصليون للشريحة أو القطعة"),
                                Triple("CUSTOMERS", "العملاء وجهات الشحن (Customers)", "عملاء المنتجات النهائية والخدمات اللوجستية")
                            )

                            scopes.forEach { (scope, label, desc) ->
                                val isSelected = selectedScope == scope
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedScope = scope }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedScope = scope },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                                    )
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Section 1: Countries Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

