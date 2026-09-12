package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * نموذج بيانات الدولة ومفتاح الاتصال الدولي.
 */
data class CountryData(
    val code: String,       // كود الدولة ISO e.g. "SA", "DZ"
    val nameAr: String,     // اسم الدولة بالعربية
    val nameEn: String,     // اسم الدولة بالإنجليزية
    val dialCode: String,   // مفتاح الاتصال الدولي e.g. "+966"
    val flagEmoji: String,  // علم الدولة
    val minLength: Int = 8, // الحد الأدنى لأرقام الهاتف
    val maxLength: Int = 11 // الحد الأقصى لأرقام الهاتف
)

/**
 * قائمة معيارية شاملة للدول العربية والدولية الصناعية.
 */
object CountryRepository {
    val countries = listOf(
        CountryData("SA", "المملكة العربية السعودية", "Saudi Arabia", "+966", "🇸🇦", 9, 9),
        CountryData("DZ", "الجزائر", "Algeria", "+213", "🇩🇿", 9, 9),
        CountryData("EG", "مصر", "Egypt", "+20", "🇪🇬", 10, 10),
        CountryData("AE", "الإمارات العربية المتحدة", "United Arab Emirates", "+971", "🇦🇪", 9, 9),
        CountryData("QA", "قطر", "Qatar", "+974", "🇶🇦", 8, 8),
        CountryData("KW", "الكويت", "Kuwait", "+965", "🇰🇼", 8, 8),
        CountryData("OM", "عُمان", "Oman", "+968", "🇴🇲", 8, 8),
        CountryData("BH", "البحرين", "Bahrain", "+973", "🇧🇭", 8, 8),
        CountryData("JO", "الأردن", "Jordan", "+962", "🇯🇴", 9, 9),
        CountryData("IQ", "العراق", "Iraq", "+964", "🇮🇶", 10, 10),
        CountryData("MA", "المغرب", "Morocco", "+212", "🇲🇦", 9, 9),
        CountryData("TN", "تونس", "Tunisia", "+216", "🇹🇳", 8, 8),
        CountryData("LY", "ليبيا", "Libya", "+218", "🇱🇾", 9, 9),
        CountryData("SD", "السودان", "Sudan", "+249", "🇸🇩", 9, 9),
        CountryData("LB", "لبنان", "Lebanon", "+961", "🇱🇧", 8, 8),
        CountryData("SY", "سوريا", "Syria", "+963", "🇸🇾", 9, 9),
        CountryData("PS", "فلسطين", "Palestine", "+970", "🇵🇸", 9, 9),
        CountryData("YE", "اليمن", "Yemen", "+967", "🇾🇪", 9, 9),
        CountryData("CN", "الصين", "China", "+86", "🇨🇳", 11, 11),
        CountryData("US", "الولايات المتحدة", "United States", "+1", "🇺🇸", 10, 10),
        CountryData("GB", "المملكة المتحدة", "United Kingdom", "+44", "🇬🇧", 10, 10),
        CountryData("DE", "ألمانيا", "Germany", "+49", "🇩🇪", 10, 11),
        CountryData("FR", "فرنسا", "France", "+33", "🇫🇷", 9, 9),
        CountryData("JP", "اليابان", "Japan", "+81", "🇯🇵", 10, 10),
        CountryData("TR", "تركيا", "Turkey", "+90", "🇹🇷", 10, 10),
        CountryData("IN", "الهند", "India", "+91", "🇮🇳", 10, 10),
        CountryData("IT", "إيطاليا", "Italy", "+39", "🇮🇹", 9, 10),
        CountryData("ES", "إسبانيا", "Spain", "+34", "🇪🇸", 9, 9),
        CountryData("CA", "كندا", "Canada", "+1", "🇨🇦", 10, 10)
    )

    fun defaultCountry(): CountryData = countries.first()

    fun findByCode(code: String): CountryData {
        val uppercase = code.trim().uppercase()
        return countries.find { it.code == uppercase } ?: defaultCountry()
    }

    fun findByNameOrCode(text: String): CountryData? {
        val q = text.trim().lowercase()
        if (q.isBlank()) return null
        return countries.find {
            it.nameAr.lowercase().contains(q) ||
                    it.nameEn.lowercase().contains(q) ||
                    it.code.lowercase() == q ||
                    it.dialCode == q
        }
    }
}

/**
 * محرك فحص وتصديق أرقام الهواتف المعياري لضمان صحة الأرقام وطولها لكل دولة.
 */
object PhoneValidator {
    fun isValidNumber(phone: String, country: CountryData): Boolean {
        val digitsOnly = phone.filter { it.isDigit() }
        if (digitsOnly.isBlank()) return false
        return digitsOnly.length in country.minLength..country.maxLength
    }

    fun formatNumber(phone: String, country: CountryData): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return ""
        return "${country.dialCode} $digits"
    }
}

/**
 * نافذة الحوار لاختيار الدولة (Country Picker Dialog).
 */
@Composable
fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onCountrySelected: (CountryData) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) CountryRepository.countries
        else {
            val q = searchQuery.trim().lowercase()
            CountryRepository.countries.filter {
                it.nameAr.lowercase().contains(q) ||
                        it.nameEn.lowercase().contains(q) ||
                        it.dialCode.contains(q) ||
                        it.code.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "اختر الدولة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن دولة بالاسم أو المفتاح...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                if (filteredCountries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد نتائج مطابقة للبحث", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filteredCountries, key = { it.code + it.dialCode }) { country ->
                            Surface(
                                onClick = {
                                    onCountrySelected(country)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(country.flagEmoji, fontSize = 20.sp)
                                        Column {
                                            Text(
                                                text = country.nameAr,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                text = country.nameEn,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = country.dialCode,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

/**
 * حقل اختيار الدولة التفاعلي (Country Picker Field) لاستخدامه في نماذج العناوين والدول.
 */
@Composable
fun CountryPickerField(
    selectedCountryName: String,
    onCountrySelected: (CountryData) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "الدولة"
) {
    var isPickerOpen by remember { mutableStateOf(false) }
    val matchedCountry = remember(selectedCountryName) {
        CountryRepository.findByNameOrCode(selectedCountryName) ?: CountryRepository.defaultCountry()
    }

    Surface(
        onClick = { isPickerOpen = true },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(matchedCountry.flagEmoji, fontSize = 20.sp)
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (selectedCountryName.isNotBlank()) selectedCountryName else matchedCountry.nameAr,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "اختر الدولة",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (isPickerOpen) {
        CountryPickerDialog(
            onDismiss = { isPickerOpen = false },
            onCountrySelected = { country ->
                onCountrySelected(country)
            }
        )
    }
}

/**
 * حقل إدخال رقم الهاتف المتطور المربوط بمفتاح الدولة والتحقق التلقائي من صحة الخانات في الوقت الفعلي.
 */
@Composable
fun PhoneNumberInputField(
    phoneValue: String,
    onPhoneValueChange: (String) -> Unit,
    selectedCountry: CountryData,
    onCountrySelected: (CountryData) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "الهاتف"
) {
    var isPickerOpen by remember { mutableStateOf(false) }

    val isValid = remember(phoneValue, selectedCountry) {
        if (phoneValue.isBlank()) null
        else PhoneValidator.isValidNumber(phoneValue, selectedCountry)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = phoneValue,
            onValueChange = { newValue ->
                val filteredDigits = newValue.filter { it.isDigit() || it == ' ' || it == '-' }
                onPhoneValueChange(filteredDigits)
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Surface(
                    onClick = { isPickerOpen = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(selectedCountry.flagEmoji, fontSize = 16.sp)
                        Text(
                            text = selectedCountry.dialCode,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            trailingIcon = {
                when (isValid) {
                    true -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "رقم صحيح",
                            tint = Color(0xFF10B981)
                        )
                    }
                    false -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "رقم غير صالح",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    null -> {}
                }
            },
            isError = isValid == false,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isValid == true) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        if (isValid == false) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "⚠️ رقم الهاتف غير مكتمل أو غير صالح لـ (${selectedCountry.nameAr})",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (isValid == true) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "✓ رقم هاتف صحيح ومطابق لـ (${selectedCountry.nameAr})",
                color = Color(0xFF10B981),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    if (isPickerOpen) {
        CountryPickerDialog(
            onDismiss = { isPickerOpen = false },
            onCountrySelected = { country ->
                onCountrySelected(country)
            }
        )
    }
}
