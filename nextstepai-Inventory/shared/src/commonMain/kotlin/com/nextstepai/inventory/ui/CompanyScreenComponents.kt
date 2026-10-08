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

@Composable
internal fun CompanyCard(
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


internal fun makePhoneCall(uriHandler: UriHandler, phone: String) {
    val cleanPhone = phone.trim().replace(" ", "")
    if (cleanPhone.isNotBlank()) {
        try {
            uriHandler.openUri("tel:$cleanPhone")
        } catch (_: Exception) { }
    }
}


internal fun sendEmail(uriHandler: UriHandler, email: String, companyName: String = "") {
    val cleanEmail = email.trim()
    if (cleanEmail.isNotBlank()) {
        try {
            uriHandler.openUri("mailto:$cleanEmail")
        } catch (_: Exception) { }
    }
}


internal fun openWebsite(uriHandler: UriHandler, url: String) {
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


internal fun openMapLocation(uriHandler: UriHandler, address: String) {
    val cleanAddress = address.trim()
    if (cleanAddress.isNotBlank()) {
        val mapUrl = "https://www.google.com/maps/search/?api=1&query=" + cleanAddress.replace(" ", "+")
        try {
            uriHandler.openUri(mapUrl)
        } catch (_: Exception) { }
    }
}


