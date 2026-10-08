package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemTable
import com.nextstepai.inventory.data.CategoryParameterTemplateView
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartAttachment
import com.nextstepai.inventory.data.PartAttachmentTable
import com.nextstepai.inventory.data.PendingAttachment
import com.nextstepai.inventory.data.AttachmentType
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
import com.nextstepai.inventory.data.labelImagePath
import com.nextstepai.inventory.data.withLabelSnapshot
import java.io.File
import kotlin.time.Clock

import com.nextstepai.inventory.data.db.getRoomDatabase
import com.nextstepai.inventory.data.db.SqliteNumericIdAllocator
import com.nextstepai.inventory.util.AppUuid

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
    val categoryRepository: PartCategoryRepository = PartCategoryRepository(partTable = partTable, categoryParameterTable = categoryParameterTable),
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

    private fun PartEntity.toPart(): Part {
        val parsedId = uuid.removePrefix("part-").toLongOrNull() ?: 0L
        return Part(
            id = parsedId,
            name = name,
            ipn = ipn,
            description = description,
            revision = revision,
            keywords = keywords,
            categoryId = categoryUuid?.removePrefix("cat-")?.toLongOrNull(),
            units = units,
            assembly = assembly,
            component = component,
            isTemplate = isTemplate,
            variantOfId = variantOfUuid?.removePrefix("part-")?.toLongOrNull(),
            trackable = trackable,
            purchaseable = purchaseable,
            salable = salable,
            virtual = virtual,
            active = active,
            locked = locked,
            defaultLocationId = defaultLocationUuid?.removePrefix("loc-")?.toLongOrNull(),
            defaultExpiryDays = defaultExpiryDays,
            minimumStock = minimumStock,
            maximumStock = maximumStock,
            imageUrl = localImagePath,
            link = link,
            totalInStock = totalInStock
        )
    }

    /**
     * جلب قائمة جميع القطع المتاحة من قاعدة البيانات الدائمة (SQLite).
     */
    fun getParts(): List<Part> {
        // Read every page from SQLite. Do not silently truncate inventories at 1,000 rows.
        val pageSize = 250
        val result = mutableListOf<Part>()
        var offset = 0
        while (true) {
            val page = partDao.getPartsPaged(limit = pageSize, offset = offset)
            result += page.map { it.toPart() }
            if (page.size < pageSize) break
            offset += page.size
        }
        return result
    }

    /**
     * جلب المنتجات الأب المؤهلة التي تفعل خيار التجميع الهندسي (assembly = true) من SQLite.
     */
    fun getParentAssemblies(): List<Part> {
        val entities = partDao.getParentAssemblies()
        return entities.map { it.toPart() }
    }

    /**
     * جلب القطع الفرعية المتاحة للمكونات مع استبعاد المنتج الأب والمكونات المضافة مسبقاً من SQLite.
     */
    fun getEligibleSubParts(parentPartId: Long, existingSubPartIds: List<Long> = emptyList()): List<Part> {
        val parentPartUuid = "part-$parentPartId"
        val entities = partDao.getEligibleSubParts(parentPartUuid)
        return entities.map { it.toPart() }.filter { !existingSubPartIds.contains(it.id) }
    }

    /**
     * جلب جميع التصنيفات المتاحة.
     */
    fun getCategories(): List<PartCategory> = categoryRepository.getCategories()

    /**
     * إنشاء تصنيف جديد وإضافته لجدول التصنيفات.
     */
    fun addCategory(name: String, description: String = ""): PartCategory =
        categoryRepository.addCategory(name, description)

    /**
     * حذف تصنيف محدد بواسطة المعرف الفريد.
     */
    fun deleteCategory(categoryId: Long): Boolean =
        categoryRepository.deleteCategory(categoryId)

    /**
     * زيادة رصيد المخزون لقطعة محددة وتحديث رصيد الصنف في الذاكرة والمحرك المحلي.
     */
    fun addStockToPart(partId: Long, quantity: Double): Part? {
        val partUuid = "part-$partId"
        runCatching { partDao.addStockToPart(partUuid, quantity) }
        val updatedFromDb = getPartById(partId)
        if (updatedFromDb != null) {
            runCatching { partTable.updatePart(updatedFromDb) }
            return updatedFromDb
        }
        return partTable.addStockToPart(partId, quantity)
    }

    /**
     * جلب قطعة محددة بواسطة المعرف الفريد.
     */
    fun getPartById(id: Long): Part? {
        // SQLite is authoritative: never repopulate and query the legacy in-memory table.
        return getParts().firstOrNull { it.id == id }
    }

    /**
     * البحث المتقدم في قائمة القطع حسب نص البحث، التصنيف، وحالات المخزون والتجميع من SQLite.
     */
    fun searchParts(
        query: String = "",
        categoryId: Long? = null,
        activeOnly: Boolean = true,
        assemblyOnly: Boolean = false,
        componentOnly: Boolean = false,
        lowStockOnly: Boolean = false
    ): List<Part> {
        val normalizedQuery = query.trim().lowercase()
        return getParts().filter { part ->
            val matchesActive = !activeOnly || part.active
            val matchesCategory = categoryId == null || part.categoryId == categoryId
            val matchesAssembly = !assemblyOnly || part.assembly
            val matchesComponent = !componentOnly || part.component
            val matchesLowStock = !lowStockOnly || part.isLowStock
            val matchesQuery = normalizedQuery.isEmpty() ||
                part.name.lowercase().contains(normalizedQuery) ||
                part.ipn.lowercase().contains(normalizedQuery) ||
                part.description.lowercase().contains(normalizedQuery) ||
                part.keywords.lowercase().contains(normalizedQuery)
            matchesActive && matchesCategory && matchesAssembly &&
                matchesComponent && matchesLowStock && matchesQuery
        }
    }

    /**
     * إضافة قطعة جديدة إلى جدول القطع ودعم الكيان المحلي وتوليد المعاملات الفنية تلقائياً من قوالب التصنيف.
     */
    fun addPart(part: Part, pendingAttachments: List<PendingAttachment> = emptyList()): Part {
        val resolvedImageUrl = pendingAttachments.firstOrNull { it.type == AttachmentType.IMAGE }?.pathOrUrl ?: part.imageUrl
        val resolvedLink = pendingAttachments.firstOrNull { it.type == AttachmentType.LINK }?.pathOrUrl 
            ?: pendingAttachments.firstOrNull { it.type == AttachmentType.DOCUMENT }?.pathOrUrl 
            ?: part.link

        val allocatedId = if (part.id > 0L) part.id else SqliteNumericIdAllocator.nextId("parts", "part-")
        val partToInsert = part.copy(id = allocatedId, imageUrl = resolvedImageUrl, link = resolvedLink)
        val partUuid = "part-$allocatedId"
        partDao.insertOrUpdate(
            PartEntity(
                uuid = partUuid,
                name = partToInsert.name,
                ipn = partToInsert.ipn,
                description = partToInsert.description,
                revision = partToInsert.revision,
                keywords = partToInsert.keywords,
                categoryUuid = partToInsert.categoryId?.let { "cat-${it.toString().padStart(3, '0')}" },
                units = partToInsert.units,
                assembly = partToInsert.assembly,
                component = partToInsert.component,
                isTemplate = partToInsert.isTemplate,
                variantOfUuid = partToInsert.variantOfId?.let { "part-$it" },
                trackable = partToInsert.trackable,
                purchaseable = partToInsert.purchaseable,
                salable = partToInsert.salable,
                virtual = partToInsert.virtual,
                active = partToInsert.active,
                locked = partToInsert.locked,
                minimumStock = partToInsert.minimumStock,
                maximumStock = partToInsert.maximumStock,
                defaultLocationUuid = partToInsert.defaultLocationId?.let { "loc-${it.toString().padStart(3, '0')}" },
                defaultExpiryDays = partToInsert.defaultExpiryDays,
                totalInStock = partToInsert.totalInStock,
                localImagePath = partToInsert.imageUrl,
                link = partToInsert.link,
                metadata = partToInsert.metadata,
                version = 1,
                syncStatus = SyncStatus.PENDING,
                isDeleted = false,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        runCatching { partTable.insertPart(partToInsert) }
        pendingAttachments.forEach { att ->
            addPartAttachment(
                PartAttachment(
                    partId = allocatedId,
                    attachment = if (att.type == AttachmentType.IMAGE || att.type == AttachmentType.DOCUMENT) att.pathOrUrl else null,
                    link = if (att.type == AttachmentType.LINK) att.pathOrUrl else null,
                    comment = att.label
                )
            )
        }
        // إنشاء سجلات المعاملات الفنية تلقائياً بناءً على PartCategoryParameterTemplate للتصنيف
        categoryParameterTable.autoGenerateParametersForPart(allocatedId, partToInsert.categoryId)
        return partToInsert
    }

    /**
     * أرشفة وحفظ لقطة صورة ملصق القطعة المادية وتحديث بيانات الأرشفة في جدول القطع.
     */
    fun savePartLabelSnapshot(partId: Long, snapshotData: String): Part? {
        val targetPart = getPartById(partId) ?: return null
        val genAt = Clock.System.now().toEpochMilliseconds()
        val imagePath = "files/labels/parts/part_${targetPart.effectiveUuid}.webp"
        val updatedPart = targetPart.withLabelSnapshot(imagePath, genAt, snapshotData)
        runCatching { partTable.updatePart(updatedPart) }
        val partUuid = "part-$partId"
        partDao.insertOrUpdate(
            PartEntity(
                uuid = partUuid,
                name = updatedPart.name,
                ipn = updatedPart.ipn,
                description = updatedPart.description,
                revision = updatedPart.revision,
                keywords = updatedPart.keywords,
                categoryUuid = updatedPart.categoryId?.let { "cat-${it.toString().padStart(3, '0')}" },
                units = updatedPart.units,
                assembly = updatedPart.assembly,
                component = updatedPart.component,
                isTemplate = updatedPart.isTemplate,
                variantOfUuid = updatedPart.variantOfId?.let { "part-$it" },
                trackable = updatedPart.trackable,
                purchaseable = updatedPart.purchaseable,
                salable = updatedPart.salable,
                virtual = updatedPart.virtual,
                active = updatedPart.active,
                locked = updatedPart.locked,
                minimumStock = updatedPart.minimumStock,
                maximumStock = updatedPart.maximumStock,
                defaultLocationUuid = updatedPart.defaultLocationId?.let { "loc-${it.toString().padStart(3, '0')}" },
                defaultExpiryDays = updatedPart.defaultExpiryDays,
                totalInStock = updatedPart.totalInStock,
                localImagePath = updatedPart.imageUrl,
                link = updatedPart.link,
                metadata = updatedPart.metadata,
                version = 1,
                syncStatus = SyncStatus.PENDING,
                isDeleted = false,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        return updatedPart
    }

    /**
     * حذف أمني محمي للقطعة وإزالتها من الجداول مع التنظيف الفيزيائي لملف الملصق.
     */
    fun deletePartWithValidation(partId: Long): Result<Boolean> {
        val part = getPartById(partId)
            ?: return Result.failure(IllegalArgumentException("القطعة المطلوب حذفها غير موجودة بالمنظومة"))

        // إزالة القطعة من جدول الذاكرة الداخلي
        runCatching { partTable.deletePart(partId) }

        // الحذف الفيزيائي الآلي لملف صورة الملصق لمنع الملفات المهملة
        part.labelImagePath?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }

        // أرشفة وحذف الكيان في قاعدة البيانات الدائمة (SQLite)
        val partUuid = "part-$partId"
        partDao.insertOrUpdate(
            PartEntity(
                uuid = partUuid,
                name = part.name,
                ipn = part.ipn,
                description = part.description,
                revision = part.revision,
                keywords = part.keywords,
                categoryUuid = part.categoryId?.let { "cat-${it.toString().padStart(3, '0')}" },
                units = part.units,
                assembly = part.assembly,
                component = part.component,
                isTemplate = part.isTemplate,
                variantOfUuid = part.variantOfId?.let { "part-$it" },
                trackable = part.trackable,
                purchaseable = part.purchaseable,
                salable = part.salable,
                virtual = part.virtual,
                active = false,
                locked = part.locked,
                minimumStock = part.minimumStock,
                maximumStock = part.maximumStock,
                defaultLocationUuid = part.defaultLocationId?.let { "loc-${it.toString().padStart(3, '0')}" },
                defaultExpiryDays = part.defaultExpiryDays,
                totalInStock = part.totalInStock,
                localImagePath = part.imageUrl,
                link = part.link,
                metadata = part.metadata,
                version = 1,
                syncStatus = SyncStatus.PENDING,
                isDeleted = true,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
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
        return partRelatedTable.getRelatedPartsForPart(partId, getParts())
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
    fun updatePart(part: Part, pendingAttachments: List<PendingAttachment> = emptyList()): Boolean {
        val resolvedImageUrl = pendingAttachments.firstOrNull { it.type == AttachmentType.IMAGE }?.pathOrUrl ?: part.imageUrl
        val resolvedLink = pendingAttachments.firstOrNull { it.type == AttachmentType.LINK }?.pathOrUrl 
            ?: pendingAttachments.firstOrNull { it.type == AttachmentType.DOCUMENT }?.pathOrUrl 
            ?: part.link

        val partToUpdate = part.copy(imageUrl = resolvedImageUrl, link = resolvedLink)
        val partUuid = "part-${part.id}"
        val existingEntity = partDao.getPartByUuid(partUuid)

        partDao.insertOrUpdate(
            PartEntity(
                uuid = partUuid,
                name = partToUpdate.name,
                ipn = partToUpdate.ipn,
                description = partToUpdate.description,
                revision = partToUpdate.revision,
                keywords = partToUpdate.keywords,
                categoryUuid = partToUpdate.categoryId?.let { "cat-${it.toString().padStart(3, '0')}" },
                units = partToUpdate.units,
                assembly = partToUpdate.assembly,
                component = partToUpdate.component,
                isTemplate = partToUpdate.isTemplate,
                variantOfUuid = partToUpdate.variantOfId?.let { "part-$it" },
                trackable = partToUpdate.trackable,
                purchaseable = partToUpdate.purchaseable,
                salable = partToUpdate.salable,
                virtual = partToUpdate.virtual,
                active = partToUpdate.active,
                locked = partToUpdate.locked,
                minimumStock = partToUpdate.minimumStock,
                maximumStock = partToUpdate.maximumStock,
                defaultLocationUuid = partToUpdate.defaultLocationId?.let { "loc-${it.toString().padStart(3, '0')}" },
                defaultExpiryDays = partToUpdate.defaultExpiryDays,
                totalInStock = partToUpdate.totalInStock,
                localImagePath = partToUpdate.imageUrl,
                link = partToUpdate.link,
                metadata = partToUpdate.metadata,
                version = (existingEntity?.version ?: 1) + 1,
                syncStatus = SyncStatus.PENDING,
                isDeleted = false,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        runCatching { partTable.updatePart(partToUpdate) }

        val existing = getPartAttachments(part.id)
        val existingIds = existing.map { it.id }.toSet()
        val newIds = pendingAttachments.map { it.id }.filter { it != 0L }.toSet()

        existingIds.filter { it !in newIds }.forEach { id ->
            deletePartAttachment(id)
        }

        pendingAttachments.filter { it.id == 0L }.forEach { att ->
            addPartAttachment(
                PartAttachment(
                    partId = part.id,
                    attachment = if (att.type == AttachmentType.IMAGE || att.type == AttachmentType.DOCUMENT) att.pathOrUrl else null,
                    link = if (att.type == AttachmentType.LINK) att.pathOrUrl else null,
                    comment = att.label
                )
            )
        }

        return true
    }

    /**
     * جلب القوالب المتاحة لاستخدامها في خيارات التفرع (Variants).
     */
    fun getTemplateParts(): List<Part> = getParts().filter { it.isTemplate }

    /**
     * جلب القطع المشتقة من قالب محدد.
     */
    fun getVariantsOf(templateId: Long): List<Part> = getParts().filter { it.variantOfId == templateId }

    /**
     * الحصول على إحصائيات عامة عن القطع (إجمالي القطع، القطع منخفضة المخزون، القطع المجمعة).
     */
    fun getPartsSummary(): PartsSummary {
        val all = getParts()
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
