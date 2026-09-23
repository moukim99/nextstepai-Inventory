package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemTable
import com.nextstepai.inventory.data.CategoryParameterTemplateView
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PartAttachmentTable
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.data.PartCategoryParameterTable
import com.nextstepai.inventory.data.PartInternalPriceTable
import com.nextstepai.inventory.data.PartNotes
import com.nextstepai.inventory.data.PartNotesTable
import com.nextstepai.inventory.data.PartPricingTable
import com.nextstepai.inventory.data.PartSalePriceTable
import com.nextstepai.inventory.data.PartStarTable
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import com.nextstepai.inventory.data.db.PartPricingEntity
import com.nextstepai.inventory.data.db.PartSalePriceEntity
import com.nextstepai.inventory.data.PartParameter
import com.nextstepai.inventory.data.PartParameterTemplate
import com.nextstepai.inventory.data.PartRelated
import com.nextstepai.inventory.data.PartRelatedTable
import com.nextstepai.inventory.data.PartRelatedView
import com.nextstepai.inventory.data.PartTable
import com.nextstepai.inventory.data.PartTestTemplate
import com.nextstepai.inventory.data.PartTestTemplateTable
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.PartEntity
import com.nextstepai.inventory.media.ImageProcessor
import com.nextstepai.inventory.media.ProcessedImage
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

import com.nextstepai.inventory.data.db.getRoomDatabase

/**
 * المستودع (Repository) المسؤول عن إدارة عمليات القطع والمكونات الأساسية (Part Management) وقوالب معامل التصنيف.
 */
class PartRepository(
    private val partTable: PartTable = PartTable(),
    private val partDao: PartDao = PartDao(),
    private val categoryParameterTable: PartCategoryParameterTable = PartCategoryParameterTable(partTable),
    private val partRelatedTable: PartRelatedTable = PartRelatedTable(partTable),
    private val partTestTemplateTable: PartTestTemplateTable = PartTestTemplateTable(partTable),
    private val partAttachmentTable: PartAttachmentTable = PartAttachmentTable(partTable),
    private val partNotesTable: PartNotesTable = PartNotesTable(partTable),
    private val partInternalPriceTable: PartInternalPriceTable = PartInternalPriceTable(partTable),
    private val partSalePriceTable: PartSalePriceTable = PartSalePriceTable(partTable),
    private val partStarTable: PartStarTable = PartStarTable(partTable),
    private val bomItemTable: BomItemTable = BomItemTable(),
    private val partPricingTable: PartPricingTable = PartPricingTable(partTable, bomItemTable = bomItemTable, internalPriceTable = partInternalPriceTable),
    private val batchSyncService: BatchSyncService = BatchSyncService(),
    private val imageProcessor: ImageProcessor = ImageProcessor(maxDimension = 1024, compressionQuality = 85)
) {
    /**
     * جلب قائمة القطع المتاحة تجزئياً (Paginated) لمنع تحميل الجدول كاملاً في الذاكرة.
     */
    suspend fun getPartsPaged(limit: Int = 20, offset: Int = 0): List<PartEntity> {
        return partDao.getPartsPaged(limit = limit, offset = offset)
    }

    /**
     * معالجة وضغط صورة المنتج وتصغيرها إلى أبعاد قصوى قبل الحفظ المحلي.
     */
    fun saveProductImageWithCompression(
        rawImageBytes: ByteArray,
        rawWidth: Int,
        rawHeight: Int
    ): ProcessedImage {
        return imageProcessor.processAndCompressProductImage(
            rawImageBytes = rawImageBytes,
            rawWidth = rawWidth,
            rawHeight = rawHeight
        )
    }

    /**
     * تنفيذ المزامنة المجمعة (Batch Sync) مع Cloudflare Worker في طلب شبكة واحد للمجموعات المعلّقة.
     */
    suspend fun syncPendingChangesWithCloudflare(): Int {
        val pendingEntities = partDao.getPendingSyncParts(limit = 50, offset = 0)
        if (pendingEntities.isEmpty()) return 0

        val payloads = pendingEntities.map { entity ->
            SyncPayload(
                uuid = entity.uuid,
                entityType = "Part",
                payloadJson = "{\"name\":\"${entity.name}\",\"ipn\":\"${entity.ipn}\"}",
                isDeleted = entity.isDeleted,
                updatedAt = entity.updatedAt
            )
        }

        val response = batchSyncService.performBatchSync(
            pendingPushes = payloads,
            lastSyncTimestamp = Clock.System.now().toEpochMilliseconds() - 86400000
        )

        partDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }

    /**
     * جلب قائمة جميع القطع المتاحة من قاعدة البيانات الدائمة (SQLite).
     */
    fun getParts(): List<Part> {
        val entities = runBlocking { partDao.getPartsPaged(limit = 1000, offset = 0) }
        return entities.map { entity ->
            Part(
                id = entity.id,
                name = entity.name,
                ipn = entity.ipn,
                description = entity.description,
                revision = entity.revision,
                keywords = entity.keywords,
                categoryId = entity.categoryId,
                units = entity.units,
                assembly = entity.assembly,
                component = entity.component,
                isTemplate = entity.isTemplate,
                variantOfId = entity.variantOfId,
                trackable = entity.trackable,
                purchaseable = entity.purchaseable,
                salable = entity.salable,
                virtual = entity.virtual,
                active = entity.active,
                locked = entity.locked,
                defaultLocationId = entity.defaultLocationId,
                defaultExpiryDays = entity.defaultExpiryDays,
                minimumStock = entity.minimumStock,
                maximumStock = entity.maximumStock,
                imageUrl = entity.localImagePath,
                link = entity.link,
                totalInStock = entity.totalInStock
            )
        }
    }

    /**
     * جلب المنتجات الأب المؤهلة التي تفعل خيار التجميع الهندسي (assembly = true) من SQLite.
     */
    fun getParentAssemblies(): List<Part> {
        val entities = runBlocking { partDao.getParentAssemblies() }
        return entities.map { entity ->
            Part(
                id = entity.id,
                name = entity.name,
                ipn = entity.ipn,
                description = entity.description,
                revision = entity.revision,
                keywords = entity.keywords,
                categoryId = entity.categoryId,
                units = entity.units,
                assembly = entity.assembly,
                component = entity.component,
                isTemplate = entity.isTemplate,
                variantOfId = entity.variantOfId,
                trackable = entity.trackable,
                purchaseable = entity.purchaseable,
                salable = entity.salable,
                virtual = entity.virtual,
                active = entity.active,
                locked = entity.locked,
                defaultLocationId = entity.defaultLocationId,
                defaultExpiryDays = entity.defaultExpiryDays,
                minimumStock = entity.minimumStock,
                maximumStock = entity.maximumStock,
                imageUrl = entity.localImagePath,
                link = entity.link,
                totalInStock = entity.totalInStock
            )
        }
    }

    /**
     * جلب القطع الفرعية المتاحة للمكونات مع استبعاد المنتج الأب والمكونات المضافة مسبقاً من SQLite.
     */
    fun getEligibleSubParts(parentPartId: Long, existingSubPartIds: List<Long> = emptyList()): List<Part> {
        val entities = runBlocking { partDao.getEligibleSubParts(parentPartId) }
        return entities.map { entity ->
            Part(
                id = entity.id,
                name = entity.name,
                ipn = entity.ipn,
                description = entity.description,
                revision = entity.revision,
                keywords = entity.keywords,
                categoryId = entity.categoryId,
                units = entity.units,
                assembly = entity.assembly,
                component = entity.component,
                isTemplate = entity.isTemplate,
                variantOfId = entity.variantOfId,
                trackable = entity.trackable,
                purchaseable = entity.purchaseable,
                salable = entity.salable,
                virtual = entity.virtual,
                active = entity.active,
                locked = entity.locked,
                defaultLocationId = entity.defaultLocationId,
                defaultExpiryDays = entity.defaultExpiryDays,
                minimumStock = entity.minimumStock,
                maximumStock = entity.maximumStock,
                imageUrl = entity.localImagePath,
                link = entity.link,
                totalInStock = entity.totalInStock
            )
        }.filter { !existingSubPartIds.contains(it.id) }
    }

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
     * زيادة رصيد المخزون لقطعة محددة وتحديث رصيد الصنف.
     */
    fun addStockToPart(partId: Long, quantity: Double): Part? {
        return partTable.addStockToPart(partId, quantity)
    }

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
     * إضافة قطعة جديدة إلى جدول القطع ودعم الكيان المحلي وتوليد المعاملات الفنية تلقائياً من قوالب التصنيف.
     */
    fun addPart(part: Part): Part {
        val inserted = partTable.insertPart(part)
        runBlocking {
            partDao.insertOrUpdate(
                PartEntity(
                    uuid = "part-${inserted.id}",
                    id = inserted.id,
                    name = inserted.name,
                    ipn = inserted.ipn,
                    description = inserted.description,
                    revision = inserted.revision,
                    keywords = inserted.keywords,
                    categoryId = inserted.categoryId,
                    units = inserted.units,
                    assembly = inserted.assembly,
                    component = inserted.component,
                    isTemplate = inserted.isTemplate,
                    variantOfId = inserted.variantOfId,
                    trackable = inserted.trackable,
                    purchaseable = inserted.purchaseable,
                    salable = inserted.salable,
                    virtual = inserted.virtual,
                    active = inserted.active,
                    locked = inserted.locked,
                    minimumStock = inserted.minimumStock,
                    maximumStock = inserted.maximumStock,
                    defaultLocationId = inserted.defaultLocationId,
                    defaultExpiryDays = inserted.defaultExpiryDays,
                    totalInStock = inserted.totalInStock,
                    localImagePath = inserted.imageUrl,
                    link = inserted.link,
                    syncStatus = SyncStatus.PENDING,
                    isDeleted = false,
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
        // إنشاء سجلات المعاملات الفنية تلقائياً بناءً على PartCategoryParameterTemplate للتصنيف
        categoryParameterTable.autoGenerateParametersForPart(inserted.id, inserted.categoryId)
        return inserted
    }

    /**
     * حذف أمني محمي للقطعة مع تطبيق القيود والاشتراطات التشغيلية:
     * 1. يمنع الحذف إذا كان للقطعة رصيد مخزوني فني فعلي على الرفوف (> 0).
     * 2. يمنع الحذف إذا كانت القطعة تدخل كمكون في شجرة مواد BOM لقطع أخرى.
     */
    fun deletePartWithValidation(partId: Long): Result<Boolean> {
        val part = partTable.getPartById(partId)
            ?: return Result.failure(IllegalArgumentException("القطعة المطلوب حذفها غير موجودة بالمنظومة"))

        // الشرط الأول: التأكد من عدم وجود رصيد مخزوني فعلي
        if (part.totalInStock > 0.0) {
            return Result.failure(IllegalStateException("لا يمكن حذف القطعة '${part.name}' لأنها تمتلك رصيد مخزوني فني فعلي (${part.totalInStock} ${part.units}). قم بصرف أو تسوية الرصيد أولاً."))
        }

        // الشرط الثاني: التأكد من عدم استخدام القطعة كعنصر رئيسي في شجرة BOM لمنتجات أخرى
        val isUsedInBom = bomItemTable.getBomItemsPaged(partId = null, limit = 1000, offset = 0).any { it.subPartId == partId }
        if (isUsedInBom) {
            return Result.failure(IllegalStateException("لا يمكن حذف القطعة '${part.name}' لأنها تدخل كبند رئيسي ومكون أساسي في شجرة مواد BOM لمنتجات أخرى."))
        }

        // الأرشفة والحذف الناعم (Soft Delete)
        val softDeletedPart = part.copy(active = false, isTemplate = false)
        updatePart(softDeletedPart)
        return Result.success(true)
    }

    /**
     * جلب قوالب المعاملات المرتبطة بتصنيف محدد مع دعم التوريث من التصنيف الأب.
     */
    fun getCategoryParameterTemplates(categoryId: Long): List<CategoryParameterTemplateView> {
        return categoryParameterTable.getCategoryParameterTemplatesForCategory(categoryId)
    }

    /**
     * إضافة قالب معامل تصنيف جديد لجدول PartCategoryParameterTemplate مع التحقق من الشروط.
     */
    fun addCategoryParameterTemplate(
        categoryId: Long,
        parameterTemplateId: Long,
        defaultValue: String? = null
    ) {
        categoryParameterTable.insertCategoryParameterTemplate(
            categoryId = categoryId,
            parameterTemplateId = parameterTemplateId,
            defaultValue = defaultValue
        )
    }

    /**
     * جلب كافة قوالب المعاملات القياسية العامة (PartParameterTemplate).
     */
    fun getAllParameterTemplates(): List<PartParameterTemplate> {
        return categoryParameterTable.getAllParameterTemplates()
    }

    /**
     * إدراج قالب معامل أو وحدة قياسية جديدة إلى النظام في قاعدة البيانات.
     */
    fun addParameterTemplate(name: String, units: String = "", description: String = ""): PartParameterTemplate {
        val template = PartParameterTemplate(
            name = name,
            units = units.ifBlank { name },
            description = description
        )
        return categoryParameterTable.insertParameterTemplate(template)
    }

    /**
     * حذف قالب معامل/وحدة من النظام بحذف متتابع (CASCADE) في قاعدة البيانات.
     */
    fun deleteParameterTemplate(templateId: Long): Boolean {
        return categoryParameterTable.deleteParameterTemplate(templateId)
    }

    /**
     * حذف قالب معامل/وحدة عن طريق الاسم أو رمز الوحدة في قاعدة البيانات.
     */
    fun deleteParameterTemplateByNameOrUnit(unitCode: String): Boolean {
        val templates = categoryParameterTable.getAllParameterTemplates()
        val match = templates.find {
            it.name.equals(unitCode, ignoreCase = true) ||
            it.units.equals(unitCode, ignoreCase = true)
        }
        return if (match != null) {
            categoryParameterTable.deleteParameterTemplate(match.id)
        } else {
            false
        }
    }

    /**
     * جلب قيم المعاملات الفنية المحددة لقطعة معينة (PartParameter).
     */
    fun getPartParameters(partId: Long): List<PartParameter> {
        return categoryParameterTable.getParametersForPart(partId)
    }

    /**
     * إدراج خاصية فنية محددة لقطعة (PartParameter) مع تحويل القيمة العددية data_numeric.
     */
    fun addPartParameter(partId: Long, templateId: Long, data: String): PartParameter {
        return categoryParameterTable.insertPartParameter(
            partId = partId,
            templateId = templateId,
            data = data
        )
    }

    /**
     * إدراج صلة ربط تبادلية بين قطعتين في PartRelated.
     */
    fun addPartRelated(part1Id: Long, part2Id: Long): PartRelated {
        return partRelatedTable.insertPartRelated(part1Id, part2Id)
    }

    /**
     * جلب القطع ذات الصلة لقطعة معينة بالاتجاهين المتبادلين.
     */
    fun getRelatedPartsForPart(partId: Long): List<PartRelatedView> {
        return partRelatedTable.getRelatedPartsForPart(partId, partTable.getAllParts())
    }

    /**
     * حذف سجل صلة محدد.
     */
    fun deletePartRelated(id: Long): Boolean {
        return partRelatedTable.deletePartRelated(id)
    }

    /**
     * إدراج قالب فحص وضمان جودة جديد لقطعة في PartTestTemplate.
     */
    fun addPartTestTemplate(template: PartTestTemplate): PartTestTemplate {
        return partTestTemplateTable.insertTestTemplate(template)
    }

    /**
     * جلب قوالب الفحوصات الخاصة بقطعة معينة.
     */
    fun getPartTestTemplates(partId: Long): List<PartTestTemplate> {
        return partTestTemplateTable.getTestTemplatesForPart(partId)
    }

    /**
     * حذف قالب فحص محدد.
     */
    fun deletePartTestTemplate(id: Long): Boolean {
        return partTestTemplateTable.deleteTestTemplate(id)
    }

    /**
     * إدراج مرفق أو رابط وثيقة جديد لقطعة في PartAttachment.
     */
    fun addPartAttachment(attachmentItem: PartAttachment): PartAttachment {
        return partAttachmentTable.insertAttachment(attachmentItem)
    }

    /**
     * جلب المرفقات والوثائق التابعة لقطعة معينة.
     */
    fun getPartAttachments(partId: Long): List<PartAttachment> {
        return partAttachmentTable.getAttachmentsForPart(partId)
    }

    /**
     * حذف مرفق محدد.
     */
    fun deletePartAttachment(id: Long): Boolean {
        return partAttachmentTable.deleteAttachment(id)
    }

    /**
     * حفظ أو تحديث سجل الملاحظات التفصيلية لقطعة معينة في PartNotes.
     */
    fun saveOrUpdatePartNotes(partId: Long, notes: String, userId: Long? = 1L): PartNotes {
        return partNotesTable.saveOrUpdateNotes(partId = partId, newNotes = notes, userId = userId)
    }

    /**
     * جلب الملاحظات التفصيلية الخاصة بقطعة معينة.
     */
    fun getPartNotes(partId: Long): PartNotes? {
        return partNotesTable.getNotesForPart(partId)
    }

    /**
     * جلب سجل حساب وتسعير التكاليف لقطعة معينة في PartPricing.
     */
    fun getPartPricing(partId: Long): PartPricingEntity? {
        return partPricingTable.getPricingForPart(partId)
    }

    /**
     * إعادة حساب وتحديث التكاليف المجمعة لقطعة معينة في PartPricing.
     */
    fun recalculatePartPricing(part: Part, bomItems: List<BomItem> = emptyList()): PartPricingEntity {
        return partPricingTable.recalculatePricingForPart(part, bomItems)
    }

    /**
     * إدراج شريحة سعرية داخلية جديدة لقطعة في PartInternalPrice.
     */
    fun addPartInternalPrice(
        partId: Long,
        quantity: Double,
        price: Double,
        currency: String = "USD"
    ): PartInternalPriceEntity {
        return partInternalPriceTable.insertInternalPrice(
            partId = partId,
            quantity = quantity,
            price = price,
            currency = currency
        )
    }

    /**
     * جلب شرائح الأسعار والتكاليف الداخلية لقطعة معينة.
     */
    fun getPartInternalPrices(partId: Long): List<PartInternalPriceEntity> {
        return partInternalPriceTable.getInternalPricesForPart(partId)
    }

    /**
     * حذف شريحة سعرية محددة.
     */
    fun deletePartInternalPrice(id: Long): Boolean {
        return partInternalPriceTable.deleteInternalPrice(id)
    }

    /**
     * إدراج شريحة سعر بيع جديدة للعملاء مع شرط القابلية للبيع (salable = true) في PartSalePrice.
     */
    fun addPartSalePrice(
        part: Part,
        quantity: Double,
        price: Double,
        currency: String = "USD"
    ): PartSalePriceEntity {
        return partSalePriceTable.insertSalePrice(
            part = part,
            quantity = quantity,
            price = price,
            currency = currency
        )
    }

    /**
     * جلب شرائح أسعار البيع للعملاء لقطعة معينة.
     */
    fun getPartSalePrices(partId: Long): List<PartSalePriceEntity> {
        return partSalePriceTable.getSalePricesForPart(partId)
    }

    /**
     * حساب أنسب شريحة سعر بيع لطلب عميل بناءً على الكمية المطلوبة.
     */
    fun getBestPartSalePriceForQuantity(partId: Long, quantity: Double): PartSalePriceEntity? {
        return partSalePriceTable.getBestSalePriceForQuantity(partId, quantity)
    }

    /**
     * حذف شريحة سعر بيع محددة.
     */
    fun deletePartSalePrice(id: Long): Boolean {
        return partSalePriceTable.deleteSalePrice(id)
    }

    /**
     * تبديل حالة تفضيل ومتابعة القطعة (Star / Unstar Toggle Action) في PartStar.
     */
    fun togglePartStar(partId: Long, userId: Long = 1L): Boolean {
        return partStarTable.toggleStarForPart(partId, userId)
    }

    /**
     * التحقق مما إذا كانت القطعة مميزة بنجمة ومفضلة للمستخدم الحالي.
     */
    fun isPartStarred(partId: Long, userId: Long = 1L): Boolean {
        return partStarTable.isPartStarred(partId, userId)
    }

    /**
     * جلب قائمة معرفات القطع المفضلة والمتابعة للمستخدم الحالي.
     */
    fun getStarredPartIdsForUser(userId: Long = 1L): List<Long> {
        return partStarTable.getStarredPartIdsForUser(userId)
    }

    /**
     * تحديث بيانات قطعة موجودة.
     */
    fun updatePart(part: Part): Boolean {
        val result = partTable.updatePart(part)
        if (result) {
            runBlocking {
                partDao.insertOrUpdate(
                    PartEntity(
                        uuid = "part-${part.id}",
                        id = part.id,
                        name = part.name,
                        ipn = part.ipn,
                        description = part.description,
                        revision = part.revision,
                        keywords = part.keywords,
                        categoryId = part.categoryId,
                        units = part.units,
                        assembly = part.assembly,
                        component = part.component,
                        isTemplate = part.isTemplate,
                        variantOfId = part.variantOfId,
                        trackable = part.trackable,
                        purchaseable = part.purchaseable,
                        salable = part.salable,
                        virtual = part.virtual,
                        active = part.active,
                        locked = part.locked,
                        minimumStock = part.minimumStock,
                        maximumStock = part.maximumStock,
                        defaultLocationId = part.defaultLocationId,
                        defaultExpiryDays = part.defaultExpiryDays,
                        totalInStock = part.totalInStock,
                        localImagePath = part.imageUrl,
                        link = part.link,
                        syncStatus = SyncStatus.PENDING,
                        isDeleted = false,
                        updatedAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
        return result
    }

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
