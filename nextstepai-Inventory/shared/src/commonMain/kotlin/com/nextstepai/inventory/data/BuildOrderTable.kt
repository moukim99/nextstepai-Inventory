package com.nextstepai.inventory.data

/**
 * حالة أمر الإنتاج والتصنيع التشغيلية (Build Order Status).
 */
enum class BuildStatus(val code: Int, val label: String) {
    PENDING(10, "مسودة (Pending)"),
    IN_PRODUCTION(20, "قيد التصنيع (In Production)"),
    COMPLETE(30, "مكتمل (Complete)"),
    CANCELLED(40, "ملغي (Cancelled)");

    companion object {
        fun fromCode(code: Int): BuildStatus = entries.find { it.code == code } ?: PENDING
    }
}

/**
 * تمثيل وثيقة أمر الإنتاج والتصنيع (Build Order) المستوحاة من نظام InvenTree.
 *
 * @property id المعرف الرقمي الفريد لأمر الإنتاج
 * @property reference الكود المرجعي الفريد للأمر (مثل BO-0001)
 * @property title عنوان أو وصف موجز لهدف الإنتاج
 * @property partId معرف القطعة المجمعة الرئيسية (المنتج الأب - assembly = true)
 * @property partName اسم القطعة المجمعة المخصصة للإنتاج
 * @property quantity إجمالي الكمية المستهدفة للبناء والإنتاج
 * @property completedQuantity الكمية التي تم الانتهاء من تجميعها وتوريدها فعلياً
 * @property status كود الحالة التشغيلية
 * @property batch رقم التشغيلة الافتراضي للوحدات المخرجة (Batch/Lot)
 * @property creationDate تاريخ إنشاء السجل
 * @property startDate تاريخ بدء تنفيذ الإنتاج الفعلي
 * @property targetDate التاريخ المستهدف لإنهاء الإنتاج
 * @property completionDate التاريخ الفعلي لإغلاق واكتمال الأمر
 * @property takeFromLocationId موقع التخزين لسحب المكونات
 * @property destinationLocationId موقع التخزين لاستقبال المنتجات المكتملة
 * @property parentId معرف أمر الإنتاج الأب في حالات Sub-builds
 * @property salesOrderId ربط بأمر بيع محدد (Build-to-Order)
 * @property issuedBy المستخدم الذي قام بإنشاء أو تعميد أمر التصنيع
 * @property responsible الموظف أو الفريق المسؤول عن تشغيل وتنفيذ الأمر
 * @property notes تعليمات وااشتراطات التصنيع والهندسة
 * @property link رابط خارجي للوثائق والمخططات
 */
data class BuildOrder(
    val id: Long = 0L,
    val reference: String,
    val title: String = "",
    val partId: Long,
    val partName: String = "",
    val quantity: Double = 1.0,
    val completedQuantity: Double = 0.0,
    val status: BuildStatus = BuildStatus.PENDING,
    val batch: String = "",
    val creationDate: String = "2025-02-15",
    val startDate: String = "",
    val targetDate: String = "",
    val completionDate: String = "",
    val takeFromLocationId: Long? = null,
    val destinationLocationId: Long? = null,
    val parentId: Long? = null,
    val salesOrderId: Long? = null,
    val issuedBy: String = "",
    val responsible: String = "",
    val notes: String = "",
    val link: String = "",
    val phaseQuantities: Map<String, Double> = emptyMap() // map of phase uuid to quantity
) {
    /**
     * نسبة اكتمال عملية التصنيع والإنتاج الحالي بناء على الكمية المكتملة في المرحلة الأخيرة.
     */
    val completionPercentage: Float
        get() = if (status == BuildStatus.COMPLETE) 100f else if (quantity > 0) ((completedQuantity / quantity) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f

    val displayCompletedQuantity: Double
        get() = if (status == BuildStatus.COMPLETE) quantity else completedQuantity


    /**
     * هل تم استيفاء كامل الكمية المطلوبة للبناء.
     */
    val canComplete: Boolean
        get() = completedQuantity >= quantity
}

/**
 * محاكاة جدول أوامر الإنتاج والتصنيع (Build Table).
 */
class BuildOrderTable {
    private val builds = mutableListOf<BuildOrder>()
    private var nextBuildId = 1L

    init {
        seedSampleBuilds()
    }

    private fun seedSampleBuilds() {
        insertBuild(
            BuildOrder(
                reference = "BO-2025-001",
                title = "تجميع دُفعة مستشعرات الحرارة DHT22",
                partId = 3L, // قالب/منتج مستشعر
                partName = "قالب مستشعر الحرارة والرطوبة",
                quantity = 50.0,
                completedQuantity = 20.0,
                status = BuildStatus.IN_PRODUCTION,
                batch = "BATCH-SENSOR-50",
                startDate = "2025-02-10",
                targetDate = "2025-02-28",
                issuedBy = "مدير النظام (Admin)",
                responsible = "فريق تجميع الحساسات",
                notes = "الفحص المكتبي للمكونات قبل التجميع"
            )
        )
    }

    /**
     * إدراج أمر إنتاج جديد مع التحقق من الفرادة وعدم ربط الأمر بنفسه.
     */
    fun insertBuild(build: BuildOrder): BuildOrder {
        require(build.reference.isNotBlank()) { "الكود المرجعي لأمر الإنتاج إلزامي" }
        require(build.parentId != build.id || build.id == 0L) { "لا يمكن لأمر الإنتاج أن يكون أباً لنفسه (parentId != id)" }

        val duplicateRef = builds.any { it.reference.equals(build.reference.trim(), ignoreCase = true) && it.id != build.id }
        require(!duplicateRef) { "الكود المرجعي '${build.reference}' مستخدم بالفعل لأمر إنتاج آخر." }

        val newBuild = build.copy(
            id = if (build.id == 0L) nextBuildId++ else build.id
        )
        builds.add(newBuild)
        return newBuild
    }

    /**
     * بدء عملية التصنيع والإنتاج الفعلي لأمر محدد (Start Production).
     */
    fun startProduction(buildId: Long): Boolean {
        val index = builds.indexOfFirst { it.id == buildId }
        if (index != -1) {
            val current = builds[index]
            if (current.status == BuildStatus.PENDING) {
                // Initialize all quantity in the first phase
                val defaultPhaseMap = mapOf("phase-def-1" to current.quantity)
                builds[index] = current.copy(
                    status = BuildStatus.IN_PRODUCTION,
                    startDate = "2025-02-15",
                    phaseQuantities = defaultPhaseMap
                )
                return true
            }
        }
        return false
    }

    /**
     * تحديث أمر الإنتاج
     */
    fun updateBuild(updatedBuild: BuildOrder) {
        val index = builds.indexOfFirst { it.id == updatedBuild.id }
        if (index != -1) {
            builds[index] = updatedBuild
        }
    }

    /**
     * توريد وإنهاء كمية مصنعة جديدة من أمر الإنتاج (Complete Build Output).
     */
    fun completeBuildOutput(buildId: Long, completedQty: Double): Boolean {
        require(completedQty > 0) { "الكمية المكتملة يجب أن تكون أكبر من الصفر" }
        val index = builds.indexOfFirst { it.id == buildId }
        if (index != -1) {
            val current = builds[index]
            val newCompleted = current.completedQuantity + completedQty
            val isFullyCompleted = newCompleted >= current.quantity

            builds[index] = current.copy(
                completedQuantity = newCompleted,
                status = if (isFullyCompleted) BuildStatus.COMPLETE else current.status,
                completionDate = if (isFullyCompleted) "2025-02-15" else current.completionDate
            )
            return true
        }
        return false
    }

    /**
     * إلغاء أمر التصنيع (Cancel Build Order).
     */
    fun cancelBuildOrder(buildId: Long): Boolean {
        val index = builds.indexOfFirst { it.id == buildId }
        if (index != -1) {
            val current = builds[index]
            if (current.status != BuildStatus.COMPLETE) {
                builds[index] = current.copy(status = BuildStatus.CANCELLED)
                return true
            }
        }
        return false
    }

    /**
     * تحديث حالة أمر التصنيع المباشرة (Update Build Status).
     */
    fun updateStatus(buildId: Long, newStatus: BuildStatus): Boolean {
        val index = builds.indexOfFirst { it.id == buildId }
        if (index != -1) {
            val current = builds[index]
            builds[index] = current.copy(status = newStatus)
            return true
        }
        return false
    }

    /**
     * البحث والفلترة في قائمة أوامر التصنيع.
     */
    fun searchBuilds(
        query: String = "",
        partId: Long? = null,
        status: BuildStatus? = null
    ): List<BuildOrder> {
        val q = query.trim().lowercase()
        return builds.filter { build ->
            val matchesPart = partId == null || build.partId == partId
            val matchesStatus = status == null || build.status == status
            val matchesQuery = q.isEmpty() ||
                    build.reference.lowercase().contains(q) ||
                    build.title.lowercase().contains(q) ||
                    build.partName.lowercase().contains(q) ||
                    build.batch.lowercase().contains(q) ||
                    build.responsible.lowercase().contains(q) ||
                    build.issuedBy.lowercase().contains(q)

            matchesPart && matchesStatus && matchesQuery
        }
    }

    fun getAllBuilds(): List<BuildOrder> = builds.toList()
}

