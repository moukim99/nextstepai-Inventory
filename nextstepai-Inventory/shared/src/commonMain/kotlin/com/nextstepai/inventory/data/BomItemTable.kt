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
 * محاكاة جدول بنود قائمة المواد (BOM Table) وحفظ الشروط والقيود المنطقية.
 */
class BomItemTable {
    private val bomItems = mutableListOf<BomItem>()
    private var nextId = 1L

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
     * حذف بند قائمة مواد.
     */
    fun deleteBomItem(id: Long): Boolean {
        return bomItems.removeIf { it.id == id }
    }

    private fun calculateChecksum(item: BomItem): String {
        return "checksum-${item.partId}-${item.subPartId}-${item.quantity}"
    }
}
