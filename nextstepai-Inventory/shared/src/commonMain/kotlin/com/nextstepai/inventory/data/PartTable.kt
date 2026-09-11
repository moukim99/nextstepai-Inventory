package com.nextstepai.inventory.data

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
    val imageUrl: String? = null,
    val link: String = "",
    val notes: String = "",
    val creationDate: String = "",
    val creationUserId: Long = 1L,
    val responsibleUserId: Long? = null,

    // الحقول المحسوبة والكميات التراكمية للعرض في الواجهات
    val totalInStock: Double = 0.0,
    val allocatedToBuildOrders: Double = 0.0,
    val allocatedToSalesOrders: Double = 0.0,
    val orderingQuantity: Double = 0.0
) {
    /**
     * حساب الكمية المتاحة الصافية للاستخدام (المخزون الكلي - الكميات المحجوزة لأوامر الإنتاج والبيع)
     */
    val availableStock: Double
        get() = (totalInStock - allocatedToBuildOrders - allocatedToSalesOrders).coerceAtLeast(0.0)

    /**
     * التحقق مما إذا كان مستوى المخزون أقل من الحد الأدنى المطلوب للتنبيه.
     */
    val isLowStock: Boolean
        get() = active && minimumStock > 0 && availableStock < minimumStock
}

/**
 * محاكاة إدارة جدول القطع والمكونات (Part Table) في قاعدة البيانات.
 */
class PartTable {
    private val parts = mutableListOf<Part>()
    private val categories = mutableListOf<PartCategory>()
    private var nextPartId = 1L
    private var nextCategoryId = 1L

    init {
        seedSampleData()
    }

    /**
     * إضافة بيانات أولية تجريبية للقطع والتصنيفات.
     */
    private fun seedSampleData() {
        val cat1 = insertCategory("مكونات إلكترونية", description = "المقاومات والمكثفات والشرائح")
        val cat2 = insertCategory("لوحات تحكم ومتحكمات", parentId = cat1.id, description = "المتحكمات الدقيقة")
        val cat3 = insertCategory("قطع مجمعة واكسسوارات", description = "المنتجات النهائية والمجموعات")

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
                creationDate = "2025-02-01"
            )
        )

        val templatePart = insertPart(
            Part(
                name = "قالب مستشعر الحرارة والرطوبة",
                ipn = "TMP-SENSOR-TMPL",
                description = "قالب تجريدي لسلسلة مستشعرات الحرارة",
                keywords = "sensor temperature humidity template",
                categoryId = cat3.id,
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
                categoryId = cat3.id,
                isTemplate = false,
                variantOfId = templatePart.id,
                assembly = true,
                component = true,
                salable = true,
                minimumStock = 5.0,
                totalInStock = 30.0,
                allocatedToSalesOrders = 5.0,
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
     * إدراج قطعة جديدة مع التحقق من شروط التبعية الشجرية للقطع (Template/Variant Rules).
     */
    fun insertPart(part: Part): Part {
        // التحقق منطقياً من تبعية القالب: لا يمكن ربط variantOfId إلا لقطعة معرّفة كـ isTemplate = true
        if (part.variantOfId != null) {
            val parentTemplate = parts.find { it.id == part.variantOfId }
            require(parentTemplate == null || parentTemplate.isTemplate) {
                "لا يمكن إنشاء قطعة مشتقة (Variant) إلا من قطعة معرفة كقالب (is_template = true)"
            }
        }

        val newPart = part.copy(
            id = if (part.id == 0L) nextPartId++ else part.id
        )
        parts.add(newPart)
        return newPart
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
