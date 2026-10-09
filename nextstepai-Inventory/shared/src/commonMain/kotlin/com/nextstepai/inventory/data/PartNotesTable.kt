package com.nextstepai.inventory.data

/**
 * نموذج بيانات ملاحظات وسجلات القطعة التفصيلية (PartNotes) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لسجل الملاحظة
 * @property partId معرف القطعة المستهدفة بالملاحظات (Part)
 * @property notes النص التفصيلي أو التوجيهات الفنية والتنظيمية
 * @property updatedAt تاريخ ووقت آخر تحديث للنص
 * @property userId معرف المستخدم المسؤول عن التعديل الأخير
 */
data class PartNotes(
    val id: Long = 0L,
    val partId: Long,
    val notes: String = "",
    val updatedAt: String = "",
    val userId: Long? = 1L
)

/**
 * إدارة وتخزين الملاحظات الموسعة للقطع (PartNotes) في الذاكرة مع التدقيق الآلي لسجل التعديلات (Audit Trail).
 */
@Deprecated("Legacy in-memory storage table. Scheduled for migration to SQLite DAOs.")
class PartNotesTable(
    private val partTable: PartTable = PartTable()
) {
    private val partNotesList = mutableListOf<PartNotes>()
    private var nextId = 1L

    init {
        seedSampleNotes()
    }

    private fun seedSampleNotes() {
        val allParts = partTable.getAllParts()
        if (allParts.isNotEmpty()) {
            val part = allParts.first()
            saveOrUpdateNotes(
                partId = part.id,
                newNotes = "تعليمات تشغيلية خاصة: يلزم التخزين في بيئة جافة بعيداً عن الرطوبة العالية مع إجراء فحص العزل الحراري دورياً.",
                userId = 1L
            )
        }
    }

    /**
     * حفظ أو تحديث الملاحظة الموسعة لقطعة معينة مع التحديث التلقائي للزمن والمستخدم المسؤول (Audit Trail).
     */
    fun saveOrUpdateNotes(
        partId: Long,
        newNotes: String,
        userId: Long? = 1L,
        currentTimestamp: String = "2025-02-15 14:00:00"
    ): PartNotes {
        val existingIndex = partNotesList.indexOfFirst { it.partId == partId }
        return if (existingIndex != -1) {
            val updatedRecord = partNotesList[existingIndex].copy(
                notes = newNotes,
                updatedAt = currentTimestamp,
                userId = userId
            )
            partNotesList[existingIndex] = updatedRecord
            updatedRecord
        } else {
            val newRecord = PartNotes(
                id = nextId++,
                partId = partId,
                notes = newNotes,
                updatedAt = currentTimestamp,
                userId = userId
            )
            partNotesList.add(newRecord)
            newRecord
        }
    }

    /**
     * جلب سجل الملاحظات الخاص بقطعة معينة.
     */
    fun getNotesForPart(partId: Long): PartNotes? {
        return partNotesList.find { it.partId == partId }
    }

    /**
     * الحذف المتتابع (CASCADE Delete) لسجل الملاحظات الخاص بقطعة محددة عند مسحها من النظام.
     */
    fun cascadeDeleteForPart(partId: Long) {
        partNotesList.removeIf { it.partId == partId }
    }
}
