package com.nextstepai.inventory.data

/**
 * تمثيل موقع وأماكن التخزين في المستودعات (StockLocation) بجميع حقوله الـ 15 المعتمدة مع الهيكلية الشجرية MPTT.
 */
data class StockLocation(
    val id: Long = 0L,
    val uuid: String = "",
    val name: String,
    val description: String = "",
    val parentId: Long? = null,
    val structural: Boolean = false,
    val external: Boolean = false,
    val locationType: String = "SHELF",
    val ownerId: Long? = null,
    val icon: String = "warehouse",
    val customIcon: String = "",
    val address: String = "",
    val customCapacity: Double? = null,
    val isBulkGenerated: Boolean = false,
    val level: Int = 0,
    val lft: Int = 0,
    val rght: Int = 0,
    val treeId: Int = 1,
    val metadata: String = "{}"
) {
    val effectiveUuid: String
        get() = uuid.ifBlank { "location-$id" }
}

/**
 * دالة تنسيق الكميات والسعات مع دعم الفواصل العشرية للأوزان والأحجام بمرونة.
 */
fun Double.formatQuantity(): String {
    return if (this % 1.0 == 0.0) this.toInt().toString() else "%.2f".format(this)
}

/**
 * السعة التخزينية الفعالة للموقع مع توفير قيمة افتراضية احتياطية (1000.0) عند كون القيمة غير محددة أو صفرية.
 */
val StockLocation.effectiveCapacity: Double
    get() = customCapacity?.takeIf { it > 0.0 } ?: 1000.0

/**
 * استخراج وحدة قياس السعة التخزينية من عمود metadata JSON (القيمة الافتراضية: "قطعة").
 */
val StockLocation.capacityUnit: String
    get() {
        val idx = metadata.indexOf("\"capacityUnit\":")
        if (idx == -1) return "قطعة"
        val sub = metadata.substring(idx + 15).trimStart()
        if (sub.startsWith("null")) return "قطعة"
        val quoteStart = sub.indexOf('"')
        if (quoteStart == -1) return "قطعة"
        val quoteEnd = sub.indexOf('"', quoteStart + 1)
        if (quoteEnd == -1) return "قطعة"
        return sub.substring(quoteStart + 1, quoteEnd).ifBlank { "قطعة" }
    }

/**
 * حساب نسبة إشغال الموقع التخزيني بناءً على الكمية الحالية المتواجدة فيه مقارنة بالسعة الفعالة.
 */
fun StockLocation.calculateOccupancyPercentage(currentQuantity: Double): Double {
    val cap = effectiveCapacity
    return if (cap <= 0.0) 0.0 else ((currentQuantity / cap) * 100.0).coerceIn(0.0, 100.0)
}

/**
 * استخراج مسار صورة الملصق المحفوظة من عمود metadata JSON.
 */
val StockLocation.labelImagePath: String?
    get() {
        val idx = metadata.indexOf("\"labelImagePath\":")
        if (idx == -1) return null
        val sub = metadata.substring(idx + 17).trimStart()
        if (sub.startsWith("null")) return null
        val quoteStart = sub.indexOf('"')
        if (quoteStart == -1) return null
        val quoteEnd = sub.indexOf('"', quoteStart + 1)
        if (quoteEnd == -1) return null
        return sub.substring(quoteStart + 1, quoteEnd)
    }

/**
 * استخراج الطابع الزمني لتوليد بطاقة الملصق من عمود metadata JSON.
 */
val StockLocation.labelGeneratedAt: Long?
    get() {
        val idx = metadata.indexOf("\"labelGeneratedAt\":")
        if (idx == -1) return null
        val sub = metadata.substring(idx + 19).trimStart()
        val numStr = sub.takeWhile { it.isDigit() }
        return numStr.toLongOrNull()
    }

/**
 * استخراج بيانات اللقطة المطبوعة من عمود metadata JSON.
 */
val StockLocation.labelSnapshotData: String?
    get() {
        val idx = metadata.indexOf("\"labelSnapshotData\":")
        if (idx == -1) return null
        val sub = metadata.substring(idx + 20).trimStart()
        val quoteStart = sub.indexOf('"')
        if (quoteStart == -1) return null
        val quoteEnd = sub.indexOf('"', quoteStart + 1)
        if (quoteEnd == -1) return null
        return sub.substring(quoteStart + 1, quoteEnd)
    }

/**
 * اكتشاف البيانات القديمة (Stale Label Detection) بمقارنة بيانات الموقع اللحظية باللقطة المطبوعة.
 */
val StockLocation.isLabelStale: Boolean
    get() {
        val genAt = labelGeneratedAt ?: return false
        val snapshot = labelSnapshotData ?: return true
        val currentSnapshot = "$name|$parentId|${effectiveCapacity.toInt()}|$locationType"
        return currentSnapshot != snapshot
    }

/**
 * دمج الخواص والمفاتيح بذكاء داخل نص الـ JSON لعمود metadata دون مسح الخواص السابقة.
 */
private fun updateJsonMetadata(existingJson: String, updates: Map<String, String>): String {
    val map = mutableMapOf<String, String>()
    val clean = existingJson.trim().removePrefix("{").removeSuffix("}").trim()
    if (clean.isNotBlank()) {
        val regex = """"(.*?)"\s*:\s*("(.*?)"|[\d\.]+|true|false|null)""".toRegex()
        regex.findAll(clean).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            map[key] = value
        }
    }
    updates.forEach { (k, v) -> map[k] = v }
    return map.entries.joinToString(prefix = "{", postfix = "}") { (k, v) -> "\"$k\":$v" }
}

/**
 * تحديث وحدة قياس سعة الموقع مع الحفاظ على بقية بيانات metadata.
 */
fun StockLocation.withCapacityUnit(unit: String): StockLocation {
    val cleanUnit = unit.ifBlank { "قطعة" }
    val updatedMetadata = updateJsonMetadata(metadata, mapOf("capacityUnit" to "\"$cleanUnit\""))
    return this.copy(metadata = updatedMetadata)
}

/**
 * تحديث بيانات لقطة أرشفة الملصق المودعة في عمود metadata دون مسح الخواص السابقة.
 */
fun StockLocation.withLabelSnapshot(imagePath: String, genAt: Long, snapshotData: String): StockLocation {
    val safeData = snapshotData.replace("\"", "\\\"")
    val updatedMetadata = updateJsonMetadata(
        metadata,
        mapOf(
            "labelImagePath" to "\"$imagePath\"",
            "labelGeneratedAt" to genAt.toString(),
            "labelSnapshotData" to "\"$safeData\""
        )
    )
    return this.copy(metadata = updatedMetadata)
}
