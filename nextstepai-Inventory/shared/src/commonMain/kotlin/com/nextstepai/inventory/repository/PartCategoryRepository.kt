package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.PartCategoryDao
import com.nextstepai.inventory.data.db.SqliteNumericIdAllocator

/**
 * مستودع إدارة تصنيفات القطع وقوالب المعاملات التقنية (Part Categories & Parameter Templates).
 * معزول عن تفاصيل القطع والمخزون لضمان فصل المسؤوليات (Separation of Concerns).
 */
class PartCategoryRepository(
    private val categoryDao: PartCategoryDao = PartCategoryDao(),
    @Suppress("DEPRECATION")
    private val categoryParameterTable: PartCategoryParameterTable = PartCategoryParameterTable()
) {
    /**
     * جلب جميع التصنيفات المتاحة من SQLite.
     */
    fun getCategories(): List<PartCategory> {
        val entities = categoryDao.getAllCategories()
        return entities.map { entity ->
            val numericId = entity.uuid.removePrefix("cat-").toLongOrNull() ?: entity.id
            val parentNumericId = entity.parentId ?: entity.parentUuid?.removePrefix("cat-")?.toLongOrNull()
            PartCategory(
                id = numericId,
                name = entity.name,
                parentId = parentNumericId,
                description = entity.description
            )
        }
    }

    /**
     * جلب تصنيف محدد بواسطة المعرف الرقمي مع دعم المعرفات المبطنة وغير المبطنة.
     */
    fun getCategoryById(categoryId: Long): PartCategory? {
        val entity = categoryDao.getCategoryById(categoryId) ?: return null
        val numericId = entity.uuid.removePrefix("cat-").toLongOrNull() ?: entity.id
        val parentNumericId = entity.parentId ?: entity.parentUuid?.removePrefix("cat-")?.toLongOrNull()
        return PartCategory(
            id = numericId,
            name = entity.name,
            parentId = parentNumericId,
            description = entity.description
        )
    }

    /**
     * إنشاء تصنيف جديد وإضافته لجدول التصنيفات الدائم.
     */
    fun addCategory(name: String, description: String = "", parentId: Long? = null): PartCategory {
        val numericId = SqliteNumericIdAllocator.nextId("part_categories", "cat-")
        val uuid = "cat-${numericId.toString().padStart(3, '0')}"
        val parentUuid = parentId?.let { pid ->
            categoryDao.getCategoryById(pid)?.uuid ?: "cat-${pid.toString().padStart(3, '0')}"
        }
        categoryDao.insertOrUpdate(
            uuid = uuid,
            id = numericId,
            name = name,
            parentId = parentId,
            parentUuid = parentUuid,
            description = description
        )
        return PartCategory(
            id = numericId,
            name = name,
            parentId = parentId,
            description = description
        )
    }

    /**
     * حذف تصنيف محدد بواسطة المعرف الفريد.
     */
    fun deleteCategory(categoryId: Long): Boolean {
        val category = categoryDao.getCategoryById(categoryId)
        val targetUuid = category?.uuid ?: "cat-${categoryId.toString().padStart(3, '0')}"
        val deleted = categoryDao.deleteCategory(targetUuid)
        return if (!deleted && category == null) {
            categoryDao.deleteCategory("cat-$categoryId")
        } else {
            deleted
        }
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
