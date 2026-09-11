package com.nextstepai.inventory.data

/**
 * نموذج بيانات علاقة الربط التبادلي بين القطع ذات الصلة (PartRelated) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لسجل العلاقة
 * @property part1Id معرف القطعة الأولى
 * @property part2Id معرف القطعة الثانية المرتبطة بها
 */
data class PartRelated(
    val id: Long = 0L,
    val part1Id: Long,
    val part2Id: Long
)

/**
 * نموذج عرض مركب لعلاقة القطع ذات الصلة لعرض بيانات القطعة المقترنة في الواجهة.
 *
 * @property relatedRecord سجل الربط
 * @property relatedPart القطعة المرتبطة المقابلة
 */
data class PartRelatedView(
    val relatedRecord: PartRelated,
    val relatedPart: Part
)

/**
 * إدارة وتخزين علاقات القطع ذات الصلة (PartRelated) في الذاكرة مع الشروط والقيود المنطقية.
 */
class PartRelatedTable(
    private val partTable: PartTable = PartTable()
) {
    private val relatedList = mutableListOf<PartRelated>()
    private var nextId = 1L

    init {
        seedSampleRelatedParts()
    }

    private fun seedSampleRelatedParts() {
        val allParts = partTable.getAllParts()
        if (allParts.size >= 2) {
            val p1 = allParts[0]
            val p2 = allParts[1]
            insertPartRelated(p1.id, p2.id)
        }
    }

    /**
     * إدراج سجل ربط جديد بين قطعتين مع تطبيق القيود:
     * 1. منع التطابق الذاتي (Self-Relation Prevention): part_1 != part_2
     * 2. قيد التماثل ومنع الازدواج العكسي (Bidirectional Uniqueness): (A,B) تمنع (B,A) و (A,B).
     */
    fun insertPartRelated(part1Id: Long, part2Id: Long): PartRelated {
        // 1. منع التطابق الذاتي
        require(part1Id != part2Id) {
            "لا يمكن ربط القطعة بنفسها كقطعة ذات صلة! (Self-Relation Prevention)"
        }

        // 2. قيد التماثل والفرادة العكسية في كلا الاتجاهين
        val exists = relatedList.any {
            (it.part1Id == part1Id && it.part2Id == part2Id) ||
                    (it.part1Id == part2Id && it.part2Id == part1Id)
        }
        require(!exists) {
            "علاقة الصلة بين هذه القطعتين موجودة بالفعل بالنظام! (قيد التماثل Bidirectional Uniqueness مُفعّل)"
        }

        val record = PartRelated(
            id = nextId++,
            part1Id = part1Id,
            part2Id = part2Id
        )
        relatedList.add(record)
        return record
    }

    /**
     * جلب القطع ذات الصلة بقطعة معينة بالاتجاهين المتبادلين (A->B و B->A).
     */
    fun getRelatedPartsForPart(
        partId: Long,
        partsList: List<Part> = partTable.getAllParts()
    ): List<PartRelatedView> {
        return relatedList
            .filter { it.part1Id == partId || it.part2Id == partId }
            .mapNotNull { record ->
                val otherPartId = if (record.part1Id == partId) record.part2Id else record.part1Id
                val otherPart = partsList.find { it.id == otherPartId }
                if (otherPart != null) {
                    PartRelatedView(relatedRecord = record, relatedPart = otherPart)
                } else null
            }
    }

    /**
     * حذف سجل صلة محدد.
     */
    fun deletePartRelated(id: Long): Boolean {
        return relatedList.removeIf { it.id == id }
    }

    /**
     * الحذف المتتابع (CASCADE) لكافة الارتباطات التابعة لقطعة محددة عند مسحها من النظام.
     */
    fun cascadeDeleteForPart(partId: Long) {
        relatedList.removeIf { it.part1Id == partId || it.part2Id == partId }
    }
}
