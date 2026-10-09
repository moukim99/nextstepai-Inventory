package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.*

/**
 * مستودع إدارة تصنيفات القطع وقوالب المعاملات التقنية (Part Categories & Parameter Templates).
 * معزول عن تفاصيل القطع والمخزون لضمان فصل المسؤوليات (Separation of Concerns).
 */
class PartCategoryRepository(
    @Suppress("DEPRECATION")
    private val partTable: PartTable = PartTable(),
    @Suppress("DEPRECATION")
    private val categoryParameterTable: PartCategoryParameterTable = PartCategoryParameterTable(partTable)
) {
    /**
     * جلب جميع التصنيفات المتاحة.
     */
    fun getCategories(): List<PartCategory> = partTable.getAllCategories()

    /**
     * إنشاء تصنيف جديد وإضافته لجدول التصنيفات.
     */
    fun addCategory(name: String, description: String = ""): PartCategory {
        return partTable.insertCategory(name = name, description = description)
    }

    /**
     * حذف تصنيف محدد بواسطة المعرف الفريد.
     */
    fun deleteCategory(categoryId: Long): Boolean {
        return partTable.deleteCategory(categoryId)
    }

    /**
     * جلب قوالب المعاملات المرتبطة بتصنيف معين مع دعم التوريث.
     */
    fun getCategoryParameterTemplatesForCategory(categoryId: Long): List<CategoryParameterTemplateView> {
        return categoryParameterTable.getCategoryParameterTemplatesForCategory(categoryId)
    }

    /**
     * ربط قالب معامل بتصنيف محدد.
     */
    fun linkParameterTemplateToCategory(
        categoryId: Long,
        parameterTemplateId: Long,
        defaultValue: String? = null
    ): PartCategoryParameterTemplate {
        return categoryParameterTable.insertCategoryParameterTemplate(
            categoryId = categoryId,
            parameterTemplateId = parameterTemplateId,
            defaultValue = defaultValue
        )
    }

    /**
     * جلب جميع قوالب المعاملات الفنية العامة.
     */
    fun getAllParameterTemplates(): List<PartParameterTemplate> {
        return categoryParameterTable.getAllParameterTemplates()
    }

    /**
     * إضافة قالب معامل فني جديد.
     */
    fun addParameterTemplate(template: PartParameterTemplate): PartParameterTemplate {
        return categoryParameterTable.insertParameterTemplate(template)
    }

    /**
     * حذف قالب معامل فني.
     */
    fun deleteParameterTemplate(templateId: Long): Boolean {
        return categoryParameterTable.deleteParameterTemplate(templateId)
    }

    /**
     * إزالة ربط قالب المعامل بالتصنيف.
     */
    fun unlinkParameterTemplateFromCategory(categoryId: Long, templateId: Long): Boolean {
        val templates = categoryParameterTable.getAllParameterTemplates()
        val match = templates.find { it.id == templateId }
        return if (match != null) {
            categoryParameterTable.deleteParameterTemplate(match.id)
            true
        } else false
    }

    /**
     * توليد الخصائص آلياً من قوالب التصنيف للقطعة الجديدة.
     */
    fun autoGenerateParametersForPart(partId: Long, categoryId: Long?) {
        categoryParameterTable.autoGenerateParametersForPart(partId, categoryId)
    }

    /**
     * جلب المعاملات الفنية لقطعة محددة.
     */
    fun getParametersForPart(partId: Long): List<PartParameter> {
        return categoryParameterTable.getParametersForPart(partId)
    }

    /**
     * جلب المعاملات الفنية لقطعة محددة مع قوالبها.
     */
    fun getPartParametersWithTemplates(partId: Long): List<PartParameterView> {
        return categoryParameterTable.getPartParametersWithTemplates(partId)
    }

    /**
     * إضافة معامل فني لقطعة.
     */
    fun addPartParameter(partId: Long, templateId: Long, data: String): PartParameter {
        return categoryParameterTable.insertPartParameter(
            partId = partId,
            templateId = templateId,
            data = data
        )
    }
}
