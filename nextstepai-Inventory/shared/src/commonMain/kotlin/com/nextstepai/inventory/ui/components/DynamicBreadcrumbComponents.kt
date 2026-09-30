package com.nextstepai.inventory.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategory
import kotlin.math.roundToInt

class ShakeController {
    var shakeTrigger by mutableStateOf(0)
        private set
    fun trigger() { shakeTrigger++ }
}

fun Modifier.shake(shakeController: ShakeController?): Modifier = composed {
    if (shakeController == null) return@composed this
    val offset = remember { Animatable(0f) }
    LaunchedEffect(shakeController.shakeTrigger) {
        if (shakeController.shakeTrigger > 0) {
            repeat(4) {
                offset.animateTo(10f, animationSpec = tween(50))
                offset.animateTo(-10f, animationSpec = tween(50))
            }
            offset.animateTo(0f, animationSpec = tween(50))
        }
    }
    this.offset { IntOffset(x = offset.value.dp.toPx().roundToInt(), y = 0) }
}

data class AddressFieldState(
    val computedAddress: String = "",
    val customAddress: String = "",
    val isConfirmed: Boolean = false,
    val isManuallyEdited: Boolean = false,
    val isLocked: Boolean = true,
    val hasDuplicateError: Boolean = false
) {
    val currentDisplayText: String
        get() = if (isManuallyEdited) customAddress else computedAddress
}

@Composable
fun DynamicBreadcrumbAddressField(
    computedPath: String,
    customAddress: String,
    onAddressChange: (String) -> Unit,
    isConfirmed: Boolean,
    onConfirmToggle: () -> Unit,
    isManuallyEdited: Boolean,
    onReset: () -> Unit,
    isLocked: Boolean,
    hasDuplicateError: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: String = "هذا العنوان / الاسم مستخدم بالفعل ضمن هذا المسار",
    shakeController: ShakeController? = null,
    label: String = "العنوان الميداني الحي (مسار الموقع)",
    placeholder: String = "مسار الموقع التلقائي..."
) {
    val displayText = if (isManuallyEdited) customAddress else computedPath

    val textColor = when {
        isLocked -> Color(0xFF94A3B8)
        isManuallyEdited || isConfirmed -> Color(0xFF0F172A)
        else -> Color(0xFF64748B)
    }

    val focusedBorderColor = if (hasDuplicateError) Color(0xFFDC2626) else Color(0xFF4F46E5)
    val unfocusedBorderColor = if (hasDuplicateError) Color(0xFFDC2626) else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shake(shakeController)
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = { newValue ->
                if (!isLocked) {
                    onAddressChange(newValue)
                }
            },
            enabled = !isLocked,
            readOnly = isLocked,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = if (isLocked) Color(0xFF94A3B8) else Color(0xFF4F46E5)
                )
            },
            trailingIcon = {
                when {
                    isLocked -> {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "مغلق لحين استكمال الهرمية",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                    isManuallyEdited -> {
                        IconButton(onClick = onReset) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "استرجاع المسار التلقائي",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    }
                    else -> {
                        IconButton(onClick = onConfirmToggle) {
                            Icon(
                                imageVector = if (isConfirmed) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = "تثبيت العنوان",
                                tint = if (isConfirmed) Color(0xFF059669) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            },
            isError = hasDuplicateError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                disabledTextColor = Color(0xFF94A3B8),
                focusedBorderColor = focusedBorderColor,
                unfocusedBorderColor = unfocusedBorderColor,
                disabledBorderColor = Color(0xFFCBD5E1),
                errorBorderColor = Color(0xFFDC2626),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF1F5F9)
            )
        )

        if (hasDuplicateError) {
            Text(
                text = errorMessage,
                color = Color(0xFFDC2626),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

/**
 * حقل رقم القطعة الداخلي الذكي (Smart IPN Field).
 * يتضمن التوليد التلقائي القائم على النمط، التعديل اليدوي، الاسترجاع بضغطة زر، زر التأكيد، وكاشف التكرار مع الاهتزاز.
 */
@Composable
fun SmartIpnField(
    generatedIpn: String,
    manualIpn: String,
    onIpnChange: (String) -> Unit,
    isConfirmed: Boolean,
    onConfirmToggle: () -> Unit,
    isManuallyEdited: Boolean,
    onReset: () -> Unit,
    onGenerate: () -> Unit = {},
    hasDuplicateError: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: String = "رقم القطعة الداخلي (IPN) مستخدم بالفعل لقطعة أخرى",
    shakeController: ShakeController? = null,
    label: String = "رقم القطعة الداخلي (IPN) *",
    placeholder: String = "مثال: ELEC-RES-0001"
) {
    val displayText = if (isManuallyEdited) manualIpn else generatedIpn

    val textColor = when {
        isManuallyEdited || isConfirmed -> Color(0xFF0F172A)
        else -> Color(0xFF64748B)
    }

    val focusedBorderColor = if (hasDuplicateError) Color(0xFFDC2626) else Color(0xFF4F46E5)
    val unfocusedBorderColor = if (hasDuplicateError) Color(0xFFDC2626) else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shake(shakeController)
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = { newValue ->
                onIpnChange(newValue)
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    if (isManuallyEdited) {
                        IconButton(onClick = onReset) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "استرجاع الكود المولد تلقائياً",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    } else {
                        IconButton(onClick = onConfirmToggle) {
                            Icon(
                                imageVector = if (isConfirmed) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = "تثبيت وتأكيد الـ IPN",
                                tint = if (isConfirmed) Color(0xFF059669) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            },
            isError = hasDuplicateError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = focusedBorderColor,
                unfocusedBorderColor = unfocusedBorderColor,
                errorBorderColor = Color(0xFFDC2626),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        if (hasDuplicateError) {
            Text(
                text = errorMessage,
                color = Color(0xFFDC2626),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

/**
 * كائن خوارزمية التوليد الذكي القائم على النمط لكود الـ IPN (Pattern-Learning Auto-fill Generator).
 */
object SmartIpnGenerator {

    fun generateNextIpn(
        categoryId: Long?,
        allParts: List<Part>,
        categories: List<PartCategory> = emptyList()
    ): String {
        if (categoryId != null) {
            val categoryParts = allParts.filter { it.categoryId == categoryId && it.ipn.isNotBlank() }
            if (categoryParts.isNotEmpty()) {
                val lastIpn = categoryParts.lastOrNull { parseIpnPattern(it.ipn) != null }?.ipn
                    ?: categoryParts.last().ipn

                val parsed = parseIpnPattern(lastIpn)
                if (parsed != null) {
                    val (prefix, number, digitsLen) = parsed
                    var counter = number + 1
                    var candidate = "$prefix${counter.toString().padStart(digitsLen, '0')}"

                    while (allParts.any { it.ipn.equals(candidate, ignoreCase = true) }) {
                        counter++
                        candidate = "$prefix${counter.toString().padStart(digitsLen, '0')}"
                    }
                    return candidate
                }
            }

            val category = categories.find { it.id == categoryId }
            if (category != null) {
                val catPrefix = extractCategoryPrefix(category.name)
                var counter = 1
                var candidate = "$catPrefix-${counter.toString().padStart(4, '0')}"
                while (allParts.any { it.ipn.equals(candidate, ignoreCase = true) }) {
                    counter++
                    candidate = "$catPrefix-${counter.toString().padStart(4, '0')}"
                }
                return candidate
            }
        }

        var fallbackCounter = 1
        var fallbackIpn = "GEN-0001"
        while (allParts.any { it.ipn.equals(fallbackIpn, ignoreCase = true) }) {
            fallbackCounter++
            fallbackIpn = "GEN-${fallbackCounter.toString().padStart(4, '0')}"
        }
        return fallbackIpn
    }

    private data class ParsedIpn(val prefix: String, val number: Long, val digitsLen: Int)

    private fun parseIpnPattern(ipn: String): ParsedIpn? {
        val trimmed = ipn.trim()
        if (trimmed.isEmpty()) return null

        var i = trimmed.length - 1
        while (i >= 0 && trimmed[i].isDigit()) {
            i--
        }
        val digitsStr = trimmed.substring(i + 1)
        if (digitsStr.isEmpty()) return null

        val prefix = trimmed.substring(0, i + 1)
        val number = digitsStr.toLongOrNull() ?: return null
        return ParsedIpn(prefix = prefix, number = number, digitsLen = digitsStr.length)
    }

    private fun extractCategoryPrefix(categoryName: String): String {
        val latinLetters = categoryName.filter { it.isLetter() && it.code in 65..122 }.uppercase()
        if (latinLetters.length >= 2) {
            return latinLetters.take(4)
        }
        return "CAT"
    }
}
