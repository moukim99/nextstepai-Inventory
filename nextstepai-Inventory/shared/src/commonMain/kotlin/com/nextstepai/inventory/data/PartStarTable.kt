package com.nextstepai.inventory.data

import com.nextstepai.inventory.data.db.PartStarEntity

/**
 * إدارة وتخزين تفضيلات ومتابعات القطع (PartStarTable) في الذاكرة مع مفتاح التبديل وقيد الفرادة المركب.
 */
@Deprecated("Legacy in-memory storage table. Scheduled for migration to SQLite DAOs.")
class PartStarTable(
    private val partTable: PartTable = PartTable()
) {
    private val starredList = mutableListOf<PartStarEntity>()
    private var nextId = 1L

    init {
        seedSampleStars()
    }

    private fun seedSampleStars() {
        val allParts = partTable.getAllParts()
        if (allParts.isNotEmpty()) {
            val part = allParts.first()
            toggleStarForPart(partId = part.id, userId = 1L)
        }
    }

    /**
     * تبديل حالة التفضيل والمتابعة للقطعة (Toggle Star Action):
     * - إذا لم تكن مجهزة بالنجمة: تنشئ سجلاً جديداً وتعود بـ true.
     * - إذا كانت مجهزة بالنجمة: تحذف السجل القائم وتعود بـ false.
     * مع تطبيق قيد الفرادة المركب (unique_together = ['part', 'user']).
     */
    fun toggleStarForPart(partId: Long, userId: Long = 1L): Boolean {
        val existingIndex = starredList.indexOfFirst { it.partId == partId && it.userId == userId }
        return if (existingIndex != -1) {
            starredList.removeAt(existingIndex)
            false // Unstarred
        } else {
            val record = PartStarEntity(
                id = nextId++,
                partId = partId,
                userId = userId
            )
            starredList.add(record)
            true // Starred
        }
    }

    /**
     * التحقق مما إذا كانت القطعة مميزة بنجمة للمستخدم الحالي.
     */
    fun isPartStarred(partId: Long, userId: Long = 1L): Boolean {
        return starredList.any { it.partId == partId && it.userId == userId }
    }

    /**
     * جلب جميع معرفات القطع المفضلة للمستخدم الحالي.
     */
    fun getStarredPartIdsForUser(userId: Long = 1L): List<Long> {
        return starredList.filter { it.userId == userId }.map { it.partId }
    }

    /**
     * الحذف المتتابع (CASCADE Delete) لاشتراكات تفضيل القطعة عند مسحها من النظام.
     */
    fun cascadeDeleteForPart(partId: Long) {
        starredList.removeIf { it.partId == partId }
    }
}
