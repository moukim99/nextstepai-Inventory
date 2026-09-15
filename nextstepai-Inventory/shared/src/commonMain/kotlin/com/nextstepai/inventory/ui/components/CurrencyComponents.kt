package com.nextstepai.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * نموذج بيانات العملة المعيارية ISO 4217.
 */
data class CurrencyData(
    val code: String,       // كود العملة ISO 4217 e.g. "USD", "SAR", "EUR"
    val nameAr: String,     // اسم العملة بالعربية
    val nameEn: String,     // اسم العملة بالإنجليزية
    val symbol: String,     // رمز العملة e.g. "$", "ر.س", "€"
    val flagEmoji: String = "" // العلم الإرشادي للبلد المصدر
)

/**
 * دليل ومستودع العملات الدولية والعربية المعيارية ISO 4217.
 * مستوحى ومحمل ببيانات العملات المعيارية الشاملة للأسواق العربية والعالمية.
 */
object CurrencyRepository {
    val currencies = listOf(
        CurrencyData("USD", "دولار أمريكي", "US Dollar", "$", "🇺🇸"),
        CurrencyData("SAR", "ريال سعودي", "Saudi Riyal", "ر.س", "🇸🇦"),
        CurrencyData("EUR", "يورو أوروبي", "Euro", "€", "🇪🇺"),
        CurrencyData("AED", "درهم إماراتي", "UAE Dirham", "د.إ", "🇦🇪"),
        CurrencyData("EGP", "جنيه مصري", "Egyptian Pound", "ج.م", "🇪🇬"),
        CurrencyData("KWD", "دينار كويتي", "Kuwaiti Dinar", "د.ك", "🇰🇼"),
        CurrencyData("QAR", "ريال قطري", "Qatari Riyal", "ر.ق", "🇶🇦"),
        CurrencyData("BHD", "دينار بحريني", "Bahraini Dinar", "د.ب", "🇧🇭"),
        CurrencyData("OMR", "ريال عماني", "Omani Rial", "ر.ع.", "🇴🇲"),
        CurrencyData("JOD", "دينار أردني", "Jordanian Dinar", "د.أ", "🇯🇴"),
        CurrencyData("DZD", "دينار جزائري", "Algerian Dinar", "د.ج", "🇩🇿"),
        CurrencyData("MAD", "درهم مغربي", "Moroccan Dirham", "د.م.", "🇲🇦"),
        CurrencyData("TND", "دينار تونسي", "Tunisian Dinar", "د.ت", "🇹🇳"),
        CurrencyData("IQD", "دينار عراقي", "Iraqi Dinar", "ع.د", "🇮🇶"),
        CurrencyData("LYD", "دينار ليبي", "Libyan Dinar", "د.ل", "🇱🇾"),
        CurrencyData("SDG", "جنيه سوداني", "Sudanese Pound", "ج.س", "🇸🇩"),
        CurrencyData("LBP", "ليرة لبنانية", "Lebanese Pound", "ل.ل", "🇱🇧"),
        CurrencyData("SYP", "ليرة سورية", "Syrian Pound", "ل.س", "🇸🇾"),
        CurrencyData("YER", "ريال يمني", "Yemeni Rial", "ر.ي", "🇾🇪"),
        CurrencyData("CNY", "يوان صيني", "Chinese Yuan", "¥", "🇨🇳"),
        CurrencyData("GBP", "جنيه إسترليني", "British Pound", "£", "🇬🇧"),
        CurrencyData("JPY", "ين ياباني", "Japanese Yen", "¥", "🇯🇵"),
        CurrencyData("TRY", "ليرة تركية", "Turkish Lira", "₺", "🇹🇷"),
        CurrencyData("INR", "روبية هندية", "Indian Rupee", "₹", "🇮🇳"),
        CurrencyData("CAD", "دولار كندي", "Canadian Dollar", "C$", "🇨🇦"),
        CurrencyData("AUD", "دولار أسترالي", "Australian Dollar", "A$", "🇦🇺"),
        CurrencyData("CHF", "فرنك سويسري", "Swiss Franc", "CHF", "🇨🇭"),
        CurrencyData("RUB", "روبل روسي", "Russian Ruble", "₽", "🇷🇺"),
        CurrencyData("SGD", "دولار سنغافوري", "Singapore Dollar", "S$", "🇸🇬"),
        CurrencyData("MYR", "رينغيت ماليزي", "Malaysian Ringgit", "RM", "🇲🇾"),
        CurrencyData("KRW", "وون كوريا الجنوبية", "South Korean Won", "₩", "🇰🇷"),
        CurrencyData("BRL", "ريال برازيلي", "Brazilian Real", "R$", "🇧🇷"),
        CurrencyData("SEK", "كرونة سويدية", "Swedish Krona", "kr", "🇸🇪"),
        CurrencyData("NOK", "كرونة نرويجية", "Norwegian Krone", "kr", "🇳🇴"),
        CurrencyData("DKK", "كرونة دنماركية", "Danish Krone", "kr", "🇩🇰"),
        CurrencyData("HKD", "دولار هونغ كونغ", "Hong Kong Dollar", "HK$", "🇭🇰"),
        CurrencyData("TWD", "دولار تايواني جديد", "New Taiwan Dollar", "NT$", "🇹🇼"),
        CurrencyData("MXN", "بيزو مكسيكي", "Mexican Peso", "Mex$", "🇲🇽"),
        CurrencyData("ZAR", "راند جنوب إفريقي", "South African Rand", "R", "🇿🇦")
    )

    fun defaultCurrency(): CurrencyData = currencies.first() // USD

    fun findByCode(code: String): CurrencyData {
        val uppercase = code.trim().uppercase()
        return currencies.find { it.code == uppercase } ?: CurrencyData(uppercase, uppercase, uppercase, uppercase)
    }

    fun search(query: String): List<CurrencyData> {
        if (query.isBlank()) return currencies
        val q = query.trim().lowercase()
        return currencies.filter {
            it.code.lowercase().contains(q) ||
            it.nameAr.lowercase().contains(q) ||
            it.nameEn.lowercase().contains(q) ||
            it.symbol.lowercase().contains(q)
        }
    }
}

/**
 * مكون بطاقة اختيار العملة المنسدلة في حقول الإدخال (Clickable Outlined Box Field).
 */
@Composable
fun CurrencySelectorField(
    selectedCurrencyCode: String,
    onOpenPicker: () -> Unit,
    label: String = "رمز العملة المعتمدة للتعامل",
    modifier: Modifier = Modifier
) {
    val currency = CurrencyRepository.findByCode(selectedCurrencyCode)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = Color(0xFF334155)
        )

        Surface(
            onClick = onOpenPicker,
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currency.flagEmoji.isNotBlank()) {
                            Text(currency.flagEmoji, fontSize = 16.sp)
                        } else {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currency.code,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = currency.symbol,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${currency.nameAr} (${currency.nameEn})",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "اختيار العملة",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * نافذة سفلية منبثقة (Bottom Sheet) لاختيار العملة المعتمدة.
 * مصممة بنفس الهيكل والأسلوب البصري لصفحة تصفية البلدان والشركات المنبثقة للأعلى.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionBottomSheet(
    selectedCurrencyCode: String,
    onDismiss: () -> Unit,
    onCurrencySelected: (CurrencyData) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredCurrencies = remember(searchQuery) {
        CurrencyRepository.search(searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                        "اختيار العملة المعتمدة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        "تحديد عملة التعامل المعتمدة للأسعار وأوامر الشراء",
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
                placeholder = { Text("بحث عن رمز العملة أو الاسم (USD, SAR, ريال...)", fontSize = 12.5.sp) },
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

            // Currencies List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredCurrencies.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد عملات مطابقة لبحثك",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                } else {
                    items(filteredCurrencies, key = { it.code }) { curr ->
                        val isSelected = selectedCurrencyCode.equals(curr.code, ignoreCase = true)
                        Surface(
                            onClick = {
                                onCurrencySelected(curr)
                                onDismiss()
                            },
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (curr.flagEmoji.isNotBlank()) {
                                            Text(curr.flagEmoji, fontSize = 18.sp)
                                        } else {
                                            Icon(
                                                Icons.Default.MonetizationOn,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = curr.code,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp
                                                ),
                                                color = if (isSelected) Color(0xFF4338CA) else Color(0xFF0F172A)
                                            )
                                            Surface(
                                                color = if (isSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = curr.symbol,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF475569),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${curr.nameAr} • ${curr.nameEn}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                                        modifier = Modifier.size(22.dp)
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
