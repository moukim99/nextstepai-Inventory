package com.nextstepai.inventory.data

import kotlin.time.Clock

/**
 * تمثيل تصنيف القطعة (PartCategory) ضمن الهيكل الشجري للمؤسسة.
 *
 * @property id المعرف الفريد للتصنيف
 * @property name اسم التصنيف (مثال: "مكونات إلكترونية", "مواد خام")
 * @property parentId معرف التصنيف الأب في الهيكل الشجري
 * @property description وصف التصنيف
 */
data class PartCategory(
    val id: Long,
    val name: String,
    val parentId: Long? = null,
    val description: String = ""
)

/**
 * نموذج البيانات الخاص بالقطعة/المكون الأساسي (Part) المستوحى من نظام InvenTree.
 * يمثل الكيان المرجعي والتجريدي لكل عنصر أو منتج أو مادة خام.
 */
data class Part(
    val id: Long = 0L,
    val name: String,
    val ipn: String = "",
    val description: String = "",
    val revision: String = "",
    val keywords: String = "",
    val categoryId: Long? = null,
    val units: String = "pcs",
    val assembly: Boolean = false,
    val component: Boolean = true,
    val isTemplate: Boolean = false,
    val variantOfId: Long? = null,
    val trackable: Boolean = false,
    val purchaseable: Boolean = true,
    val salable: Boolean = false,
    val virtual: Boolean = false,
    val active: Boolean = true,
    val locked: Boolean = false,
    val defaultLocationId: Long? = null,
    val defaultSupplierId: Long? = null,
    val defaultExpiryDays: Int? = null,
    val minimumStock: Double = 0.0,
    val maximumStock: Double? = null,
    val imageUrl: String? = null,
    val link: String = "",
    val notes: String = "",
    val creationDate: String = "",
    val creationUserId: Long = 1L,
    val responsibleUserId: Long? = null,
    val metadata: String = "{}",

    // الحقول المحسوبة والكميات التراكمية للعرض في الواجهات
    val totalInStock: Double = 0.0,
    val allocatedToBuildOrders: Double = 0.0,
    val allocatedToSalesOrders: Double = 0.0,
    val totalHardAllocated: Double = 0.0,
    val totalSoftAllocated: Double = 0.0,
    val orderingQuantity: Double = 0.0
) {
    /**
     * المعرف الفريد السلسلي للقطعة للاستخدام الموحد في المسارات وحفظ الملفات.
     */
    val effectiveUuid: String
        get() = "part-$id"

    /**
     * حساب إجمالي المخصصات المحجوزة المؤكدة (Committed Allocated = HARD)
     */
    val committedAllocated: Double
        get() = maxOf(totalHardAllocated, allocatedToBuildOrders + allocatedToSalesOrders)

    /**
     * حساب الكمية المتاحة الصافية للاستخدام (Live Balances Formula: Net Available = Total On-Hand - Committed Allocated)
     */
    val availableStock: Double
        get() = (totalInStock - committedAllocated).coerceAtLeast(0.0)

    /**
     * إجمالي المحجوز الكلي (مؤكد + مبدئي)
     */
    val totalAllocatedQuantity: Double
        get() = committedAllocated + totalSoftAllocated

    /**
     * التحقق مما إذا كان مستوى المخزون المتاح أقل من الحد الأدنى المطلوب للتنبيه.
     */
    val isLowStock: Boolean
        get() = active && minimumStock > 0 && availableStock < minimumStock
}

/**
 * استخراج مسار صورة الملصق المحفوظة للقطعة من عمود metadata JSON.
 */
val Part.labelImagePath: String?
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
 * استخراج الطابع الزمني لتوليد بطاقة ملصق القطعة من عمود metadata JSON.
 */
val Part.labelGeneratedAt: Long?
    get() {
        val idx = metadata.indexOf("\"labelGeneratedAt\":")
        if (idx == -1) return null
        val sub = metadata.substring(idx + 19).trimStart()
        val numStr = sub.takeWhile { it.isDigit() }
        return numStr.toLongOrNull()
    }

/**
 * استخراج بيانات لقطة ملصق القطعة من عمود metadata JSON.
 */
val Part.labelSnapshotData: String?
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
 * نص لقطة ملصق القطعة الحالي المحسوب قياسياً للمطابقة لكشف الفروقات والأرشفة.
 */
val Part.currentLabelSnapshot: String
    get() = "$name|$ipn|$categoryId|$component|$assembly|$salable"

/**
 * اكتشاف البيانات القديمة (Stale Label Detection) بمقارنة بيانات القطعة اللحظية باللقطة المطبوعة.
 */
val Part.isLabelStale: Boolean
    get() {
        val genAt = labelGeneratedAt ?: return false
        val snapshot = labelSnapshotData ?: return true
        return currentLabelSnapshot != snapshot
    }

/**
 * تحليل الفروقات التفصيلية بين بيانات القطعة الحالية واللقطة المطبوعة في الملصق لتوضيح أسباب إعادة الطباعة للمشرف.
 */
fun Part.getLabelDiffDetails(categoryName: String? = null): List<String> {
    val snapshot = labelSnapshotData ?: return listOf("لم يتم توليد لقطة سابقة للملصق")
    val partsList = snapshot.split("|")
    if (partsList.size < 6) return listOf("بيانات اللقطة المطبوعة غير مكتملة")

    val snapName = partsList[0]
    val snapIpn = partsList[1]
    val snapCategoryId = partsList[2].takeIf { it != "null" }?.toLongOrNull()
    val snapComponent = partsList[3].toBoolean()
    val snapAssembly = partsList[4].toBoolean()
    val snapSalable = partsList[5].toBoolean()

    val diffs = mutableListOf<String>()
    if (snapName != name) {
        diffs.add("تم تغيير اسم القطعة من '$snapName' إلى '$name'")
    }
    if (snapIpn != ipn) {
        diffs.add("تم تعديل كود الـ IPN من '$snapIpn' إلى '$ipn'")
    }
    if (snapCategoryId != categoryId) {
        val catText = if (!categoryName.isNullOrBlank()) "إلى '$categoryName'" else ""
        diffs.add("تم تعديل تصنيف القطعة $catText".trim())
    }
    if (snapComponent != component || snapAssembly != assembly || snapSalable != salable) {
        diffs.add("تم تعديل طبيعة وتصنيف استخدام القطعة")
    }

    if (diffs.isEmpty() && isLabelStale) {
        diffs.add("تم تحديث بيانات تعريفية على القطعة")
    }
    return diffs
}

/**
 * دمج الخواص والمفاتيح داخل نص الـ JSON لعمود metadata دون مسح الخواص السابقة.
 */
private fun updatePartJsonMetadata(existingJson: String, updates: Map<String, String>): String {
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
 * تحديث بيانات لقطة أرشفة ملصق القطعة المودعة في عمود metadata دون مسح الخواص السابقة.
 */
fun Part.withLabelSnapshot(imagePath: String, genAt: Long, snapshotData: String): Part {
    val safeData = snapshotData.replace("\"", "\\\"")
    val updatedMetadata = updatePartJsonMetadata(
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
 * محاكاة إدارة جدول القطع والمكونات (Part Table) في قاعدة البيانات.
 */
@Deprecated("Legacy in-memory storage table. Scheduled for migration to SQLite DAOs.")
class PartTable {
    private val parts = mutableListOf<Part>()
    private val categories = mutableListOf<PartCategory>()
    private var nextPartId = 1L
    private var nextCategoryId = 1L

    init {
        seedSampleData()
    }

    fun clearAll() {
        parts.clear()
        categories.clear()
        nextPartId = 1L
        nextCategoryId = 1L
    }

    /**
     * إضافة بيانات أولية تجريبية للقطع والتصنيفات.
     */
    private fun seedSampleData() {
        val cat1 = insertCategory("مكونات إلكترونية وكهربائية", description = "مقاومات، مكثفات، بوردات، حساسات، محولات، كابلات")
        val cat2 = insertCategory("مواد كيميائية وسوائل", description = "كحول إيزوبروبيل، مذيبات، دهانات، غراء، زيوت، شحوم")
        insertCategory("مواد ولوازم تجميع واستهلاك", description = "قصدير لحام، أسلاك لحام، معجون حراري، أشرطة لاصقة، فلاتر")
        insertCategory("قطع ميكانيكية وعتاد صلب", description = "براغي، صواميل، مسامير، حوامل معدنية، زنبركات")
        insertCategory("خامات ومواد أولية", description = "صفائح بلاستيك، ألمنيوم، خشب، أنابيب، أسلاك غير مقطوعة")
        insertCategory("تغليف ومواد شحن", description = "كراتين، لفائف فقاعية، أكياس حماية، ملصقات")
        insertCategory("قطع غيار وصيانة", description = "محركات بديلة، شفرات، رؤوس كاوية لحام، سيور نقل")
        val cat8 = insertCategory("منتجات جاهزة وتجميعات تامة", description = "أجهزة مكتملة الصنع، بضائع مستوردة معروضة للبيع")

        insertPart(
            Part(
                name = "مقاومة 10K Ohm",
                ipn = "RES-10K-001",
                description = "مقاومة كربونية 1/4 واط بنسبة سماحية 5%",
                keywords = "resistor resistance 10k electronic",
                categoryId = cat1.id,
                units = "pcs",
                assembly = false,
                component = true,
                purchaseable = true,
                minimumStock = 100.0,
                totalInStock = 250.0,
                creationDate = "2025-01-15"
            )
        )

        insertPart(
            Part(
                name = "متحكم ESP32 Wi-Fi/BT",
                ipn = "MCU-ESP32-WROOM",
                description = "وحدة متحكم دقيق ESP32 مزود بـ Wi-Fi و Bluetooth",
                keywords = "esp32 micro-controller wifi bluetooth",
                categoryId = cat2.id,
                units = "pcs",
                assembly = false,
                component = true,
                purchaseable = true,
                salable = true,
                trackable = true,
                minimumStock = 10.0,
                totalInStock = 8.0, // ينشط تنبيه انخفاض المخزون
                allocatedToBuildOrders = 2.0,
                totalHardAllocated = 2.0,
                creationDate = "2025-02-01"
            )
        )

        val templatePart = insertPart(
            Part(
                name = "قالب مستشعر الحرارة والرطوبة",
                ipn = "TMP-SENSOR-TMPL",
                description = "قالب تجريدي لسلسلة مستشعرات الحرارة",
                keywords = "sensor temperature humidity template",
                categoryId = cat8.id,
                isTemplate = true,
                assembly = true,
                component = false,
                salable = true,
                creationDate = "2025-02-10"
            )
        )

        insertPart(
            Part(
                name = "مستشعر DHT22 الدقيق",
                ipn = "TMP-SENSOR-DHT22",
                description = "مستشعر حرارة ورطوبة رقمي عالي الدقة (مشتق من القالب)",
                keywords = "dht22 sensor temperature variant",
                categoryId = cat8.id,
                isTemplate = false,
                variantOfId = templatePart.id,
                assembly = true,
                component = true,
                salable = true,
                minimumStock = 5.0,
                totalInStock = 30.0,
                allocatedToSalesOrders = 5.0,
                totalHardAllocated = 5.0,
                creationDate = "2025-02-12"
            )
        )
    }

    /**
     * إدراج تصنيف جديد في الجدول.
     */
    fun insertCategory(name: String, parentId: Long? = null, description: String = ""): PartCategory {
        val category = PartCategory(
            id = nextCategoryId++,
            name = name,
            parentId = parentId,
            description = description
        )
        categories.add(category)
        return category
    }

    /**
     * جلب جميع التصنيفات المتاحة.
     */
    fun getAllCategories(): List<PartCategory> = categories.toList()

    /**
     * حذف تصنيف محدد من القائمة وتحديث القطع المرتبطة به.
     */
    fun deleteCategory(categoryId: Long): Boolean {
        val removed = categories.removeAll { it.id == categoryId }
        if (removed) {
            parts.forEachIndexed { index, part ->
                if (part.categoryId == categoryId) {
                    parts[index] = part.copy(categoryId = null)
                }
            }
        }
        return removed
    }

    /**
     * إدراج قطعة جديدة مع التحقق من شروط التبعية الشجرية وتوليد لقطة أرشفة ملصق القطعة المبدئية تلقائياً.
     */
    fun insertPart(part: Part): Part {
        // التحقق منطقياً من تبعية القالب: لا يمكن ربط variantOfId إلا لقطعة معرّفة كـ isTemplate = true
        if (part.id == 0L && part.variantOfId != null) {
            val parentTemplate = parts.find { it.id == part.variantOfId }
            require(parentTemplate == null || parentTemplate.isTemplate) {
                "لا يمكن إنشاء قطعة مشتقة (Variant) إلا من قطعة معرفة كقالب (is_template = true)"
            }
        }

        val assignedId = if (part.id == 0L) nextPartId++ else part.id
        val isNewPart = part.id == 0L || part.labelGeneratedAt == null
        val partWithBaseInfo = part.copy(id = assignedId)

        // الأتمتة التلقائية: توليد وحفظ لقطة البيانات التأسيسية فور الإنشاء
        val finalPart = if (isNewPart) {
            val now = Clock.System.now().toEpochMilliseconds()
            val initialSnapshot = partWithBaseInfo.currentLabelSnapshot
            val imagePath = "files/labels/parts/part_${partWithBaseInfo.effectiveUuid}.webp"
            partWithBaseInfo.withLabelSnapshot(imagePath, now, initialSnapshot)
        } else {
            partWithBaseInfo
        }

        parts.removeAll { it.id == finalPart.id }
        parts.add(finalPart)
        return finalPart
    }

    /**
     * تحديث بيانات قطعة موجودة.
     */
    fun updatePart(part: Part): Boolean {
        val index = parts.indexOfFirst { it.id == part.id }
        if (index != -1) {
            parts[index] = part
            return true
        }
        return false
    }

    /**
     * حذف قطعة من القائمة المباشرة بالذاكرة.
     */
    fun deletePart(partId: Long): Boolean {
        return parts.removeAll { it.id == partId }
    }

    /**
     * زيادة رصيد المخزون لقطعة محددة وتحديث إجمالي المخزون.
     */
    fun addStockToPart(partId: Long, quantity: Double): Part? {
        val index = parts.indexOfFirst { it.id == partId }
        if (index != -1) {
            val current = parts[index]
            val updated = current.copy(
                totalInStock = current.totalInStock + quantity
            )
            parts[index] = updated
            return updated
        }
        return null
    }

    /**
     * جلب قطعة بالمعرف الفريد (ID).
     */
    fun getPartById(id: Long): Part? = parts.find { it.id == id }

    /**
     * جلب جميع القطع المتاحة.
     */
    fun getAllParts(): List<Part> = parts.toList()

    /**
     * البحث والفلترة المتقدمة للقطع حسب الكلمات المفتاحية، التصنيف، وحالة النشاط.
     */
    fun searchParts(
        query: String = "",
        categoryId: Long? = null,
        activeOnly: Boolean = true,
        assemblyOnly: Boolean = false,
        componentOnly: Boolean = false,
        lowStockOnly: Boolean = false
    ): List<Part> {
        return parts.filter { part ->
            val matchesActive = !activeOnly || part.active
            val matchesCategory = categoryId == null || part.categoryId == categoryId
            val matchesAssembly = !assemblyOnly || part.assembly
            val matchesComponent = !componentOnly || part.component
            val matchesLowStock = !lowStockOnly || part.isLowStock

            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    part.name.lowercase().contains(q) ||
                    part.ipn.lowercase().contains(q) ||
                    part.description.lowercase().contains(q) ||
                    part.keywords.lowercase().contains(q)

            matchesActive && matchesCategory && matchesAssembly && matchesComponent && matchesLowStock && matchesQuery
        }
    }

    /**
     * الحصول على جميع القطع المعرفة كقوالب (Templates) لاستخدامها في القوائم المنسدلة للقطع المشتقة.
     */
    fun getTemplateParts(): List<Part> = parts.filter { it.isTemplate && it.active }

    /**
     * جلب القطع المشتقة (Variants) لقطعة قالب معينة.
     */
    fun getVariantsOf(templateId: Long): List<Part> = parts.filter { it.variantOfId == templateId }
}
