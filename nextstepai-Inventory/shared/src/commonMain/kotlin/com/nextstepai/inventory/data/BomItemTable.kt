package com.nextstepai.inventory.data

/**
 * نموذج بيانات بند قائمة مواد التصنيع (BomItem) المستوحى من نظام InvenTree.
 * يحدد المكونات والكميات والشروط الفنية المطلوبة لتجميع منتج معين.
 *
 * @property id المعرف الرقمي الفريد لبند قائمة المواد
 * @property partId معرف القطعة المجمعة الرئيسية (المنتج الأب)
 * @property subPartId معرف القطعة المكونة المدمجة (المكون الفرعي)
 * @property quantity الكمية المطلوبة من المكون الفرعي لإنتاج وحدة واحدة من المنتج الأب
 * @property reference المرجع الهندسي أو المكاني للمكون (Designator) مثل R1, C3
 * @property optional يحدد ما إذا كان المكون اختيارياً في التجميع أم إلزامياً
 * @property consumable يحدد ما إذا كان المكون استهلاكياً (مثل: الغراء أو أسلاك اللحام)
 * @property allowVariants يسمح باستبدال المكون ببدائل مشتقة من نفس قالب القطعة تلقائياً
 * @property inherited يسمح للقطع المتفرعة وراثة هذا البند تلقائياً
 * @property note ملاحظات وتعليمات فنية خاصة بالبند
 * @property checksum بصمة تحقق رقمية لتتبع التعديلات وحماية تكامل بيانات القائمة
 */
data class BomItem(
    val id: Long = 0L,
    val partId: Long,
    val subPartId: Long,
    val quantity: Double = 1.0,
    val reference: String = "",
    val optional: Boolean = false,
    val consumable: Boolean = false,
    val allowVariants: Boolean = false,
    val inherited: Boolean = false,
    val note: String = "",
    val checksum: String = ""
)

/**
 * نموذج بيانات القطعة البديلة لبند قائمة المواد (BomItemSubstitute) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لسجل البديل
 * @property bomItemId معرف بند قائمة المواد الأصلي (BomItem)
 * @property partId معرف القطعة البديلة المقترحة (Part)
 */
data class BomItemSubstitute(
    val id: Long = 0L,
    val bomItemId: Long,
    val partId: Long
)

/**
 * نموذج عرض مركّب للبديل يتضمن بيانات السجل والمعلومات الفنية للقطعة البديلة.
 */
data class BomItemSubstituteView(
    val substitute: BomItemSubstitute,
    val substitutePart: Part
)

/**
 * محاكاة جدول بنود قائمة المواد (BOM Table) وحفظ الشروط والقيود المنطقية.
 */
class BomItemTable {
    private val bomItems = mutableListOf<BomItem>()
    private val substitutes = mutableListOf<BomItemSubstitute>()
    private var nextId = 1L
    private var nextSubstituteId = 1L

    init {
        seedSampleBomData()
    }

    private fun seedSampleBomData() {
        // إضافة بنود BOM تجريبية للقطعة المجمعة (المنتج الأب)
        insertBomItem(
            BomItem(
                partId = 3L, // قالب/منتج مستشعر
                subPartId = 1L, // مقاومة 10K
                quantity = 2.0,
                reference = "R1, R2",
                note = "لحام دقيق بشريحة المقاومة"
            )
        )

        insertBomItem(
            BomItem(
                partId = 3L,
                subPartId = 2L, // متحكم ESP32
                quantity = 1.0,
                reference = "U1",
                allowVariants = true,
                note = "تركيب المتحكم على القاعدة الرئيسية"
            )
        )
    }

    /**
     * إدراج بند جديد في قائمة المواد مع تطبيق القيود المنطقية:
     * 1. القيد ضد التكرار الحلزوني (Anti-Recursion): $part \neq sub\_part$
     * 2. قيد الفرادة (Uniqueness Constraint): منع تكرار نفس (partId + subPartId)
     */
    fun insertBomItem(bomItem: BomItem): BomItem {
        require(bomItem.partId != bomItem.subPartId) {
            "خطأ في قيد التجميع: لا يمكن للقطعة أن تحتوي على نفسها كمكون فرعي (partId != subPartId)"
        }

        require(bomItem.quantity > 0.0) {
            "الكمية المطلوبة يجب أن تكون قيمة موجبة أكبر من صفر (quantity > 0)."
        }

        val exists = bomItems.any { it.partId == bomItem.partId && it.subPartId == bomItem.subPartId }
        require(!exists) {
            "المكون الفرعي مضاف بالفعل لهذا المنتج الأب. يرجى تعديل الكمية أو المراجع الهندسية بدلاً من التكرار."
        }

        val generatedChecksum = calculateChecksum(bomItem)
        val newItem = bomItem.copy(
            id = if (bomItem.id == 0L) nextId++ else bomItem.id,
            checksum = generatedChecksum
        )
        bomItems.add(newItem)
        return newItem
    }

    /**
     * جلب بنود قائمة المواد لمنتج أب محدد (Part BOM Items).
     */
    fun getBomItemsForPart(partId: Long): List<BomItem> {
        return bomItems.filter { it.partId == partId }
    }

    /**
     * جلب قائمة المواد المقسمة صفحات (Paginated) لمنع تحميل كامل الجدول في الذاكرة.
     */
    fun getBomItemsPaged(partId: Long? = null, limit: Int = 20, offset: Int = 0): List<BomItem> {
        return bomItems
            .filter { partId == null || it.partId == partId }
            .drop(offset)
            .take(limit)
    }

    /**
     * حذف بند قائمة مواد وتطبيق الحذف المتتابع (CASCADE) على جميع البدائل المعرفة عليه في BomItemSubstitute.
     */
    fun deleteBomItem(id: Long): Boolean {
        substitutes.removeIf { it.bomItemId == id }
        return bomItems.removeIf { it.id == id }
    }

    /**
     * إدراج قطعة بديلة لبند قائمة المواد (BomItemSubstitute) مع تطبيق القيود:
     * 1. قيد التفرد المركب (unique_together = ['bom_item', 'part']).
     * 2. منع التطابق الذاتي (لا تكون القطعة البديلة هي المكون الأساسي sub_part نفسه).
     * 3. منع التصادم مع القطعة التجميعية الأصل (partId != bom_item.part).
     * 4. التأكد من أن القطعة صالحة كمكون تصنيعي (component = true).
     */
    fun insertSubstitute(
        bomItemId: Long,
        substitutePartId: Long,
        partsList: List<Part> = emptyList()
    ): BomItemSubstitute {
        val bomItem = bomItems.find { it.id == bomItemId }
            ?: throw IllegalArgumentException("بند قائمة المواد المطلوب غير موجود (#$bomItemId)")

        // 1. منع التطابق الذاتي مع المكون الأساسي
        require(substitutePartId != bomItem.subPartId) {
            "لا يمكن إضافة نفس المكون الأساسي كقطعة بديلة لنفس البند! (Self-Substitution Prevention)"
        }

        // 2. منع التصادم مع القطعة التجميعية الأصل
        require(substitutePartId != bomItem.partId) {
            "لا يمكن استخدام القطعة المجمعة الأصلية كقطعة بديلة لمكونها الداخلي."
        }

        // 3. قيد التفرد المركب (unique_together)
        val exists = substitutes.any { it.bomItemId == bomItemId && it.partId == substitutePartId }
        require(!exists) {
            "هذه القطعة البديلة مضافة بالفعل لهذا البند (قيد التفرد unique_together مُفعّل)."
        }

        // 4. التحقق من صلاحيتها كمكون فرعي إذا وُجدت قائمة القطع
        val targetPart = partsList.find { it.id == substitutePartId }
        if (targetPart != null) {
            require(targetPart.component) {
                "القطعة المحددة ليست معرفة كمكون تصنيعي (component = true)."
            }
        }

        val substitute = BomItemSubstitute(
            id = nextSubstituteId++,
            bomItemId = bomItemId,
            partId = substitutePartId
        )
        substitutes.add(substitute)
        return substitute
    }

    /**
     * جلب القطع البديلة المعرفة لبند قائمة مواد معين مع ربط بيانات القطعة للعرض.
     */
    fun getSubstitutesForBomItem(
        bomItemId: Long,
        partsList: List<Part> = emptyList()
    ): List<BomItemSubstituteView> {
        return substitutes
            .filter { it.bomItemId == bomItemId }
            .map { sub ->
                val part = partsList.find { it.id == sub.partId }
                    ?: Part(id = sub.partId, name = "قطعة بديلة #${sub.partId}", component = true)
                BomItemSubstituteView(substitute = sub, substitutePart = part)
            }
    }

    /**
     * حذف قطعة بديلة محددة.
     */
    fun deleteSubstitute(id: Long): Boolean {
        return substitutes.removeIf { it.id == id }
    }

    private fun calculateChecksum(item: BomItem): String {
        return "checksum-${item.partId}-${item.subPartId}-${item.quantity}"
    }
}
