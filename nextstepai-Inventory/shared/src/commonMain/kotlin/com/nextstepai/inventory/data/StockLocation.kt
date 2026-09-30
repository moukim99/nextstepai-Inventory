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
 * استخراج وحدة قياس السعة التخزينية من عمود metadata JSON (إرجاع سلسلة فارغة "" إذا لم تكن محددة).
 */
val StockLocation.capacityUnit: String
    get() {
        val idx = metadata.indexOf("\"capacityUnit\":")
        if (idx == -1) return ""
        val sub = metadata.substring(idx + 15).trimStart()
        if (sub.startsWith("null")) return ""
        val quoteStart = sub.indexOf('"')
        if (quoteStart == -1) return ""
        val quoteEnd = sub.indexOf('"', quoteStart + 1)
        if (quoteEnd == -1) return ""
        return sub.substring(quoteStart + 1, quoteEnd)
    }

/**
 * حساب نسبة إشغال الموقع التخزيني بناءً على الكمية الحالية المتواجدة فيه مقارنة بالسعة الفعالة.
 */
fun StockLocation.calculateOccupancyPercentage(currentQuantity: Double): Double {
    val cap = effectiveCapacity
    return if (cap <= 0.0) 0.0 else ((currentQuantity / cap) * 100.0).coerceIn(0.0, 100.0)
}

/**
 * حساب نسبة الإشغال الحقيقي/الفيزيائي للموقع مع دعم تحويل الأوزان التلقائي.
 * عند كون سعة الرف معرّفة بالوزن (كغ/kg أو طن/ton) والصنف معرّف بوحدة أخرى (متر/m أو قطعة/pcs):
 * يتم تحويل كمية الصنف إلى وزن باستخدام totalWeight أو (quantity * unitWeight).
 */
fun StockLocation.calculatePhysicalOccupancy(
    itemsInLocation: List<StockItem>,
    parts: List<Part>
): Double {
    val cap = effectiveCapacity
    if (cap <= 0.0) return 0.0

    val totalLoad = calculateTotalLoad(itemsInLocation, parts)
    return ((totalLoad / cap) * 100.0).coerceIn(0.0, 100.0)
}

/**
 * حساب الحمولة الإجمالية الفعلية للرف بنفس وحدة قياس السعة.
 */
fun StockLocation.calculateTotalLoad(
    itemsInLocation: List<StockItem>,
    parts: List<Part>
): Double {
    val isWeightCapacity = capacityUnit.equals("kg", ignoreCase = true) ||
            capacityUnit.equals("كغ", ignoreCase = true) ||
            capacityUnit.equals("كيلوغرام", ignoreCase = true) ||
            capacityUnit.equals("ton", ignoreCase = true) ||
            capacityUnit.equals("طن", ignoreCase = true)

    return itemsInLocation.sumOf { item ->
        val part = parts.find { it.id == item.partId }
        val partUnit = part?.units ?: "pcs"
        val isSameUnit = partUnit.equals(capacityUnit, ignoreCase = true)

        if (isSameUnit) {
            item.quantity
        } else if (isWeightCapacity) {
            val weight = item.totalWeight ?: (item.unitWeight?.let { it * item.quantity }) ?: item.quantity
            // إذا كانت السعة بالطن والوزن المسجل بالكيلوغرام:
            if ((capacityUnit.equals("ton", ignoreCase = true) || capacityUnit.equals("طن", ignoreCase = true)) &&
                !partUnit.equals("ton", ignoreCase = true) && !partUnit.equals("طن", ignoreCase = true)) {
                weight / 1000.0
            } else {
                weight
            }
        } else {
            item.quantity
        }
    }
}

/**
 * توليد نص الملخص الثنائي المشترك (Dual Preview Summary Text) لعرض الحمولة الفعلية والمحتوى الميداني التفصيلي.
 * مثال:
 *   الحمل الوزني: "31% (310 / 1000 كغ)"
 *   السطر التوضيحي الميداني: "📦 يحتوي على: 80 متر أعمدة حديدية + 150 قطعة"
 */
fun StockLocation.getOccupancySummary(
    itemsInLocation: List<StockItem>,
    parts: List<Part>
): Pair<String, String> {
    val occupancyPct = calculatePhysicalOccupancy(itemsInLocation, parts)
    val totalLoad = calculateTotalLoad(itemsInLocation, parts)
    val cap = effectiveCapacity

    val percentageText = "${occupancyPct.toInt()}% (${totalLoad.formatQuantity()} / ${cap.formatQuantity()} $capacityUnit)"

    if (itemsInLocation.isEmpty()) {
        return Pair(percentageText, "📦 الرف فارغ حالياً")
    }

    val itemSummaries = itemsInLocation.map { item ->
        val part = parts.find { it.id == item.partId }
        val partName = part?.name ?: "قطعة #${item.partId}"
        val partUnit = part?.units ?: "pcs"
        "${item.quantity.formatQuantity()} $partUnit $partName"
    }

    val previewText = "📦 يحتوي على: " + itemSummaries.joinToString(" + ")
    return Pair(percentageText, previewText)
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
 * نص لقطة ملصق الموقع الحالي المحسوب قياسياً للمطابقة لكشف الفروقات والأرشفة.
 */
val StockLocation.currentLabelSnapshot: String
    get() = "$name|$parentId|${effectiveCapacity.toInt()}|$locationType"

/**
 * اكتشاف البيانات القديمة (Stale Label Detection) بمقارنة بيانات الموقع اللحظية باللقطة المطبوعة.
 */
val StockLocation.isLabelStale: Boolean
    get() {
        val genAt = labelGeneratedAt ?: return false
        val snapshot = labelSnapshotData ?: return true
        return currentLabelSnapshot != snapshot
    }

/**
 * تحليل الفروقات التفصيلية بين بيانات الموقع الحالية واللقطة المطبوعة في الملصق لتوضيح أسباب إعادة الطباعة للمشرف.
 */
fun StockLocation.getLabelDiffDetails(): List<String> {
    val snapshot = labelSnapshotData ?: return listOf("لم يتم توليد لقطة سابقة للملصق")
    val parts = snapshot.split("|")
    if (parts.size < 4) return listOf("بيانات اللقطة المطبوعة غير مكتملة")

    val snapName = parts[0]
    val snapParentId = parts[1].takeIf { it != "null" }?.toLongOrNull()
    val snapCap = parts[2].toIntOrNull() ?: 0
    val snapType = parts[3]

    val diffs = mutableListOf<String>()
    if (snapName != name) {
        diffs.add("تم تغيير اسم الموقع من '$snapName' إلى '$name'")
    }
    if (snapCap != effectiveCapacity.toInt()) {
        diffs.add("تم تعديل السعة التصميمية من $snapCap إلى ${effectiveCapacity.toInt()} $capacityUnit")
    }
    if (snapParentId != parentId) {
        diffs.add("تم نقل الموقع إلى شجرة أب مختلفة")
    }
    if (!snapType.equals(locationType, ignoreCase = true)) {
        diffs.add("تم تعديل تصنيف الموقع من '$snapType' إلى '$locationType'")
    }

    if (diffs.isEmpty() && isLabelStale) {
        diffs.add("تم تحديث بيانات تشغيلية وهيكلية على الموقع")
    }
    return diffs
}

/**
 * استخراج اسم الشخص المسؤول للموقع الخارجي من عمود metadata JSON.
 */
val StockLocation.contactPerson: String?
    get() {
        val idx = metadata.indexOf("\"contactPerson\":")
        if (idx == -1) return null
        val sub = metadata.substring(idx + 16).trimStart()
        if (sub.startsWith("null")) return null
        val quoteStart = sub.indexOf('"')
        if (quoteStart == -1) return null
        val quoteEnd = sub.indexOf('"', quoteStart + 1)
        if (quoteEnd == -1) return null
        return sub.substring(quoteStart + 1, quoteEnd)
    }

/**
 * استخراج رقم الهاتف للتواصل للموقع الخارجي من عمود metadata JSON.
 */
val StockLocation.contactPhone: String?
    get() {
        val idx = metadata.indexOf("\"contactPhone\":")
        if (idx == -1) return null
        val sub = metadata.substring(idx + 15).trimStart()
        if (sub.startsWith("null")) return null
        val quoteStart = sub.indexOf('"')
        if (quoteStart == -1) return null
        val quoteEnd = sub.indexOf('"', quoteStart + 1)
        if (quoteEnd == -1) return null
        return sub.substring(quoteStart + 1, quoteEnd)
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
 * تحديث بيانات التواصل للموقع الخارجي مع الحفاظ على بقية بيانات metadata.
 */
fun StockLocation.withContactInfo(person: String?, phone: String?): StockLocation {
    val updates = mutableMapOf<String, String>()
    if (!person.isNullOrBlank()) {
        val safePerson = person.replace("\"", "\\\"")
        updates["contactPerson"] = "\"$safePerson\""
    } else {
        updates["contactPerson"] = "null"
    }
    if (!phone.isNullOrBlank()) {
        val safePhone = phone.replace("\"", "\\\"")
        updates["contactPhone"] = "\"$safePhone\""
    } else {
        updates["contactPhone"] = "null"
    }
    val updatedMetadata = updateJsonMetadata(metadata, updates)
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

/**
 * التحقق مما إذا كان الموقع أو المستودع معيناً كموقع/مستودع أساسي من عمود metadata JSON.
 */
val StockLocation.isPrimary: Boolean
    get() {
        val idx = metadata.indexOf("\"isPrimary\":")
        if (idx == -1) return false
        val sub = metadata.substring(idx + 12).trimStart()
        return sub.startsWith("true")
    }

/**
 * تحديث حالة التعيين الأساسي (isPrimary) للموقع مع الحفاظ على بقية بيانات metadata.
 */
fun StockLocation.withPrimary(isPrimary: Boolean): StockLocation {
    val updatedMetadata = updateJsonMetadata(metadata, mapOf("isPrimary" to isPrimary.toString()))
    return this.copy(metadata = updatedMetadata)
}

/**
 * بناء وحساب المسار الهرمي والشجري النصي الكامل للموقع التخزيني عبر كافة المستويات الآباء.
 * مثال الناتج: "المستودع الرئيسي > المنطقة الشرقية > الممر 03"
 */
fun StockLocation.getFullHierarchyPath(allLocations: List<StockLocation>): String {
    val path = mutableListOf(this.name)
    var currentParentId = this.parentId
    val visited = mutableSetOf<Long>()
    if (this.id != 0L) visited.add(this.id)

    while (currentParentId != null) {
        val parent = allLocations.find { it.id == currentParentId } ?: break
        if (!visited.add(parent.id)) break
        path.add(0, parent.name)
        currentParentId = parent.parentId
    }
    return path.joinToString(" > ")
}

/**
 * مواصفات العقدة والطبقة الهرمية الوسيطة المضافة لحشو فجوات التخزين (Hierarchy Gap In-line Filler).
 */
data class IntermediateNodeSpec(
    val locationType: String,
    val existingId: Long? = null,
    val name: String = ""
)

/**
 * نتيجة قياس اكتمال البيانات التخزينية الميدانية لبطاقة الموقع (Completeness Score).
 */
data class CompletenessResult(
    val percentage: Int,
    val colorTone: CompletenessTone
)

enum class CompletenessTone { RED, ORANGE, GREEN }

/**
 * حساب النسبة المئوية الموزونة لاكتمال بيانات بطاقة الموقع التخزيني (0 - 100%).
 * 1. العنوان والارتباط الهرمي الكامل: 60%
 * 2. المشرف المباشر (Owner): 10%
 * 3. نوع وحدة القياس المخصصة (Capacity UoM): 15%
 * 4. قيمة السعة التخزينية القصوى (Custom Capacity): 15%
 */
fun StockLocation.calculateCompleteness(): CompletenessResult {
    var score = 0

    // 1. العنوان والارتباط الهرمي الكامل (60%)
    if (address.isNotBlank() || parentId != null || (id > 0L && level == 0)) {
        score += 60
    }

    // 2. المشرف المباشر (10%)
    if (ownerId != null && ownerId > 0L) {
        score += 10
    }

    // 3. نوع وحدة القياس المخصصة الصريحة (15%)
    if (capacityUnit.isNotBlank()) {
        score += 15
    }

    // 4. قيمة السعة المادية الفعالة (15%)
    if (customCapacity != null && customCapacity > 0.0) {
        score += 15
    }

    val tone = when {
        score < 60 -> CompletenessTone.RED
        score < 100 -> CompletenessTone.ORANGE
        else -> CompletenessTone.GREEN
    }

    return CompletenessResult(percentage = score, colorTone = tone)
}

