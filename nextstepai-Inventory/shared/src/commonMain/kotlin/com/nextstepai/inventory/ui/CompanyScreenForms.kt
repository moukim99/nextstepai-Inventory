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
internal fun AddCompanyBottomSheet(
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
        parentId: Long?,
        imageUrl: String?,
        active: Boolean,
        notes: String
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

    var imageUrl by remember(companyToEdit) { mutableStateOf(companyToEdit?.imageUrl ?: "") }
    var active by remember(companyToEdit) { mutableStateOf(companyToEdit?.active ?: true) }
    var notes by remember(companyToEdit) { mutableStateOf(companyToEdit?.notes ?: "") }

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
                .imePadding()
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
                        Text("سجل الموردين والمصنّعين (InvenTree System)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color(0xFF64748B))
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

                // Logo Path / Image Selection Field (logoPath -> Image Picker / URL)
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("مسار الشعار / صورة الشركة (logoPath)") },
                    placeholder = { Text("https://example.com/logo.png أو/images/logo.png") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (imageUrl.isNotEmpty()) {
                            IconButton(onClick = { imageUrl = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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

                // Active / Archive Status Switch Field (active)
                Surface(
                    onClick = { active = !active },
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.5.dp, if (active) Color(0xFF10B981) else Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                    .background(if (active) Color(0xFF10B981) else Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (active) "الشركة مفعلة ونشطة في النظام" else "الشركة مؤرشفة (غير نشطة)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (active) "تظهر في قوائم الموردين وأوامر الشراء" else "مخفية من المعاملات النشطة والطلبات الجديدة",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        Switch(
                            checked = active,
                            onCheckedChange = { active = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                // Multiline Additional Notes Field (notes)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات تعاقدية وفنية إضافية (notes)") },
                    placeholder = { Text("اكتب أي شروط تعاقدية، شروط شحن، أو ملاحظات خاصة بالتعامل...") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                                onConfirm(
                                    name,
                                    description,
                                    website,
                                    formattedPhone,
                                    email,
                                    address,
                                    contact,
                                    isSupplier,
                                    isManufacturer,
                                    isCustomer,
                                    currency,
                                    selectedParentId,
                                    imageUrl.ifBlank { null },
                                    active,
                                    notes
                                )
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddCompanyAttachmentBottomSheet(
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
internal fun AddContactBottomSheet(
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
                .imePadding()
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
internal fun AddAddressBottomSheet(
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
                .imePadding()
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
internal fun AddBankAccountBottomSheet(
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
                .imePadding()
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
internal fun EditLegalRecordBottomSheet(
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
    var showExpiryDatePicker by remember { mutableStateOf(false) }

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
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = { expiryDate = it },
                                readOnly = true,
                                placeholder = { Text("2029-01-15") },
                                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showExpiryDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showExpiryDatePicker = true }
                            )
                        }

                        if (showExpiryDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
                            )
                            DatePickerDialog(
                                onDismissRequest = { showExpiryDatePicker = false },
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
                                        showExpiryDatePicker = false
                                    }) {
                                        Text("تأكيد الاختيار", fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showExpiryDatePicker = false }) {
                                        Text("إلغاء")
                                    }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }
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


