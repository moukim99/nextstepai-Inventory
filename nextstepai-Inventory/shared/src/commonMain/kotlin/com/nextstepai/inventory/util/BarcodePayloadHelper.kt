package com.nextstepai.inventory.util

/**
 * أنواع الكيانات الممثلة في رموز الباركود وشفات الـ QR (Barcode Entity Types).
 */
enum class BarcodeEntityType(val prefix: String, val schemePath: String, val label: String) {
    LOCATION("loc-", "location", "موقع تخزيني"),
    STOCK_ITEM("stock-", "stock", "وحدة مخزنية"),
    PART("part-", "part", "قطعة ومكون"),
    UNKNOWN("", "", "كيان غير معروف");

    companion object {
        fun fromSchemePath(path: String): BarcodeEntityType {
            return entries.find { it.schemePath.equals(path, ignoreCase = true) } ?: UNKNOWN
        }

        fun fromUuidPrefix(uuid: String): BarcodeEntityType {
            val lower = uuid.lowercase()
            return entries.find { it.prefix.isNotBlank() && lower.startsWith(it.prefix) } ?: UNKNOWN
        }
    }
}

/**
 * الحمولة المنسقة المفرغة من الباركود أو رمز الـ QR (Barcode Payload).
 */
data class BarcodePayload(
    val entityType: BarcodeEntityType,
    val uuid: String,
    val rawContent: String,
    val isValidUri: Boolean = false
) {
    val formattedUri: String
        get() = if (entityType != BarcodeEntityType.UNKNOWN) "inv://${entityType.schemePath}/$uuid" else rawContent
}

/**
 * كائن مساعد لتوليد وتفكيك روابط الحمولات الموحدة للباركود ورموز الـ QR (BarcodePayloadHelper).
 */
object BarcodePayloadHelper {

    private const val URI_SCHEME = "inv://"

    /**
     * توليد نص حمولة موحدة لموقع تخزيني.
     */
    fun generateLocationPayload(locationUuid: String): String {
        return "${URI_SCHEME}location/${locationUuid.trim()}"
    }

    /**
     * توليد نص حمولة موحدة لوحدة مخزنية.
     */
    fun generateStockItemPayload(stockUuid: String): String {
        return "${URI_SCHEME}stock/${stockUuid.trim()}"
    }

    /**
     * توليد نص حمولة موحدة لقطعة/مكون.
     */
    fun generatePartPayload(partUuid: String): String {
        return "${URI_SCHEME}part/${partUuid.trim()}"
    }

    /**
     * تفكيك النص المقروء من الماسح الضوئي إلى كائن `BarcodePayload` مع الدعم المزدوج للـ Deep Links والـ UUIDs المباشرة.
     */
    fun parsePayload(rawInput: String): BarcodePayload {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return BarcodePayload(BarcodeEntityType.UNKNOWN, "", rawInput, isValidUri = false)
        }

        // 1. فحص نمط الـ Deep Link: inv://[type]/[uuid]
        if (trimmed.lowercase().startsWith(URI_SCHEME)) {
            val pathContent = trimmed.substring(URI_SCHEME.length)
            val parts = pathContent.split('/', limit = 2)
            if (parts.size == 2) {
                val typeStr = parts[0]
                val uuidStr = parts[1]
                val entityType = BarcodeEntityType.fromSchemePath(typeStr)
                if (entityType != BarcodeEntityType.UNKNOWN && uuidStr.isNotBlank()) {
                    return BarcodePayload(
                        entityType = entityType,
                        uuid = uuidStr,
                        rawContent = trimmed,
                        isValidUri = true
                    )
                }
            }
        }

        // 2. فحص الـ UUID المباشر (مثل loc-001 أو stock-10)
        val directType = BarcodeEntityType.fromUuidPrefix(trimmed)
        return BarcodePayload(
            entityType = directType,
            uuid = trimmed,
            rawContent = trimmed,
            isValidUri = false
        )
    }
}
