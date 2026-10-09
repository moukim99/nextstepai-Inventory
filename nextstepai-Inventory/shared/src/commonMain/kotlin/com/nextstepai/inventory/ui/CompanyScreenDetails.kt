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
internal fun CompanyDetailsBottomSheet(
    company: Company,
    uiState: CompanyUiState,
    viewModel: CompanyViewModel,
    onCreateSalesOrder: ((Company) -> Unit)? = null,
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
                .imePadding()
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

                            if (company.isCustomer && onCreateSalesOrder != null) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onCreateSalesOrder(company)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("إنشاء أمر بيع جديد لهذا العميل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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


