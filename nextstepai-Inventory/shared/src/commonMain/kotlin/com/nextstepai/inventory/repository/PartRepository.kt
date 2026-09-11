package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PartTable

/**
 * المستودع (Repository) المسؤول عن إدارة عمليات القطع والمكونات الأساسية (Part Management) والربط مع الجدول.
 */
class PartRepository(
    private val partTable: PartTable = PartTable()
) {
    /**
     * جلب قائمة جميع القطع المتاحة.
     */
    fun getParts(): List<Part> = partTable.getAllParts()

    /**
     * جلب جميع التصنيفات المتاحة.
     */
    fun getCategories(): List<PartCategory> = partTable.getAllCategories()

    /**
     * جلب قطعة محددة بواسطة المعرف الفريد.
     */
    fun getPartById(id: Long): Part? = partTable.getPartById(id)

    /**
     * البحث المتقدم في قائمة القطع حسب نص البحث، التصنيف، وحالات المخزون والتجميع.
     */
    fun searchParts(
        query: String = "",
        categoryId: Long? = null,
        activeOnly: Boolean = true,
        assemblyOnly: Boolean = false,
        componentOnly: Boolean = false,
        lowStockOnly: Boolean = false
    ): List<Part> {
        return partTable.searchParts(
            query = query,
            categoryId = categoryId,
            activeOnly = activeOnly,
            assemblyOnly = assemblyOnly,
            componentOnly = componentOnly,
            lowStockOnly = lowStockOnly
        )
    }

    /**
     * إضافة قطعة جديدة إلى جدول القطع.
     */
    fun addPart(part: Part): Part = partTable.insertPart(part)

    /**
     * تحديث بيانات قطعة موجودة.
     */
    fun updatePart(part: Part): Boolean = partTable.updatePart(part)

    /**
     * جلب القوالب المتاحة لاستخدامها في خيارات التفرع (Variants).
     */
    fun getTemplateParts(): List<Part> = partTable.getTemplateParts()

    /**
     * جلب القطع المشتقة من قالب محدد.
     */
    fun getVariantsOf(templateId: Long): List<Part> = partTable.getVariantsOf(templateId)

    /**
     * الحصول على إحصائيات عامة عن القطع (إجمالي القطع، القطع منخفضة المخزون، القطع المجمعة).
     */
    fun getPartsSummary(): PartsSummary {
        val all = partTable.getAllParts()
        return PartsSummary(
            totalParts = all.size,
            activeParts = all.count { it.active },
            lowStockParts = all.count { it.isLowStock },
            assemblyParts = all.count { it.assembly },
            componentParts = all.count { it.component },
            templateParts = all.count { it.isTemplate }
        )
    }
}

/**
 * ملخص إحصائيات جدول القطع لعرضه في لوحة الإحصائيات والمؤشرات.
 */
data class PartsSummary(
    val totalParts: Int,
    val activeParts: Int,
    val lowStockParts: Int,
    val assemblyParts: Int,
    val componentParts: Int,
    val templateParts: Int
)
