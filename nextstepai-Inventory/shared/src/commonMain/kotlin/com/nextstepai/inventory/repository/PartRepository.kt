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

import com.nextstepai.inventory.data.db.PartAttachmentDao
import com.nextstepai.inventory.data.db.PartNotesDao
import com.nextstepai.inventory.data.db.PartInternalPriceDao
import com.nextstepai.inventory.data.db.PartSalePriceDao
import com.nextstepai.inventory.domain.PartPricingCalculator
import com.nextstepai.inventory.data.db.PartStarDao
import com.nextstepai.inventory.data.db.PartPricingDao
import com.nextstepai.inventory.data.db.PartTestTemplateDao
import com.nextstepai.inventory.data.db.PartParameterDao
import com.nextstepai.inventory.data.db.PartRelatedDao
import com.nextstepai.inventory.data.db.PartParameterTemplateDao
import com.nextstepai.inventory.data.db.PartCategoryParameterTemplateDao
import com.nextstepai.inventory.data.db.getRoomDatabase
import com.nextstepai.inventory.data.db.SqliteDatabaseManager
import com.nextstepai.inventory.data.db.SqliteNumericIdAllocator
import com.nextstepai.inventory.util.AppUuid

/**
 * المستودع (Repository) المسؤول عن إدارة عمليات القطع والمكونات الأساسية (Part Management) وقوالب معامل التصنيف.
 */
class PartRepository(
    private val partTable: PartTable = PartTable(),
    private val partDao: PartDao = PartDao(),
    private val partAttachmentDao: PartAttachmentDao = PartAttachmentDao(),
    private val partNotesDao: PartNotesDao = PartNotesDao(),
    private val partInternalPriceDao: PartInternalPriceDao = PartInternalPriceDao(),
    private val partSalePriceDao: PartSalePriceDao = PartSalePriceDao(),
    private val partStarDao: PartStarDao = PartStarDao(),
    private val partPricingDao: PartPricingDao = PartPricingDao(),
    private val partTestTemplateDao: PartTestTemplateDao = PartTestTemplateDao(),
    private val partParameterDao: PartParameterDao = PartParameterDao(),
    private val partRelatedDao: PartRelatedDao = PartRelatedDao(),
    private val partParameterTemplateDao: PartParameterTemplateDao = PartParameterTemplateDao(),
    private val partCategoryParameterTemplateDao: PartCategoryParameterTemplateDao = PartCategoryParameterTemplateDao(),
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
        val parsedId = id.takeIf { it > 0L } ?: uuid.removePrefix("part-").toLongOrNull() ?: 0L
        return Part(
            id = parsedId,
            uuid = uuid,
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
     * جلب قطعة محددة بواسطة المعرف الفريد.
     */
    fun getPartById(id: Long): Part? {
        val byUuid = partDao.getPartByUuid("part-$id")?.toPart()
        if (byUuid != null) return byUuid
        return getParts().firstOrNull { it.id == id }
    }

    /**
     * جلب قطعة محددة بواسطة UUID الصريح.
     */
    fun getPartByUuid(uuid: String): Part? {
        return partDao.getPartByUuid(uuid)?.toPart()
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
        val targetPart = getPartById(partId) ?: return null
        val updated = partDao.addStockToPart(targetPart.effectiveUuid, quantity)
        if (!updated) {
            return null
        }
        val updatedFromDb = getPartById(partId)
        if (updatedFromDb != null) {
            runCatching { partTable.updatePart(updatedFromDb) }
        }
        return updatedFromDb
    }

    /**
     * زيادة رصيد المخزون لقطعة محددة بواسطة المعرف النصي UUID.
     */
    fun addStockToPartByUuid(uuid: String, quantity: Double): Part? {
        val targetPart = getPartByUuid(uuid) ?: return null
        val updated = partDao.addStockToPart(targetPart.effectiveUuid, quantity)
        if (!updated) {
            return null
        }
        val updatedFromDb = getPartByUuid(uuid)
        if (updatedFromDb != null) {
            runCatching { partTable.updatePart(updatedFromDb) }
        }
        return updatedFromDb
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
        val partUuid = if (part.uuid.isNotBlank()) part.uuid else "part-$allocatedId"
        val partToInsert = part.copy(id = allocatedId, uuid = partUuid, imageUrl = resolvedImageUrl, link = resolvedLink)
        val inserted = partDao.insert(
            PartEntity(
                uuid = partUuid,
                id = allocatedId,
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
        if (!inserted) {
            throw IllegalStateException("فشل إدراج القطعة في قاعدة بيانات SQLite")
        }
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
        // إنشاء سجلات المعاملات الفنية تلقائياً بناءً على PartCategoryParameterTemplate للتصنيف وحفظها
        val generatedParams = categoryParameterTable.autoGenerateParametersForPart(allocatedId, partToInsert.categoryId)
        generatedParams.forEach { param ->
            runCatching {
                partParameterDao.insertOrUpdate(
                    partUuid = partUuid,
                    templateUuid = "param-tpl-${param.templateId}",
                    data = param.data,
                    dataNumeric = param.dataNumeric,
                    id = param.id
                )
            }
        }
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
        val existingEntity = partDao.getPartByUuid(targetPart.effectiveUuid)
        val updated = partDao.update(
            PartEntity(
                uuid = targetPart.effectiveUuid,
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
                version = (existingEntity?.version ?: 1) + 1,
                syncStatus = SyncStatus.PENDING,
                isDeleted = false,
                updatedAt = genAt
            )
        )
        if (!updated) return null
        runCatching { partTable.updatePart(updatedPart) }
        return updatedPart
    }

    /**
     * حذف أمني محمي للقطعة وإزالتها من الجداول مع التنظيف الفيزيائي لملف الملصق وحذف الكيانات التابعة.
     */
    fun deletePartWithValidation(partId: Long): Result<Boolean> {
        val part = getPartById(partId)
            ?: return Result.failure(IllegalArgumentException("القطعة المطلوب حذفها غير موجودة بالمنظومة"))
        return deletePartByUuid(part.effectiveUuid)
    }

    /**
     * حذف أمني للقطعة باستخدام المعرف النصي UUID.
     */
    fun deletePartByUuid(uuid: String): Result<Boolean> {
        val conn = SqliteDatabaseManager.getConnection()
        val part = try {
            conn.prepare("BEGIN IMMEDIATE").use { it.step() }
            val existing = getPartByUuid(uuid)
                ?: throw IllegalArgumentException("القطعة المطلوب حذفها غير موجودة بالمنظومة")

            val deleted = partDao.softDeleteByUuid(existing.effectiveUuid)
            if (!deleted) {
                throw IllegalStateException("فشل حذف القطعة من قاعدة البيانات")
            }

            // جميع تغييرات الحذف التابعة تُنفذ في المعاملة نفسها؛ أي خطأ يؤدي إلى rollback.
            partAttachmentDao.deleteForPart(existing.effectiveUuid)
            partNotesDao.deleteForPart(existing.effectiveUuid)
            partInternalPriceDao.deleteForPart(existing.effectiveUuid)
            partSalePriceDao.deleteForPart(existing.effectiveUuid)
            partStarDao.deleteForPart(existing.effectiveUuid)
            partPricingDao.deleteForPart(existing.effectiveUuid)
            partTestTemplateDao.deleteForPart(existing.effectiveUuid)
            partParameterDao.deleteForPart(existing.effectiveUuid)
            partRelatedDao.deleteForPart(existing.effectiveUuid)

            conn.prepare("COMMIT").use { it.step() }
            existing
        } catch (failure: Throwable) {
            runCatching { conn.prepare("ROLLBACK").use { it.step() } }
            return Result.failure(failure)
        }

        // الجداول القديمة والملفات ليست جزءاً من معاملة SQLite؛ تُعامل كمزامنة أفضلية بعد نجاحها.
        runCatching { partTable.deletePart(part.id) }
        part.labelImagePath?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
        return Result.success(true)
    }

    /**
     * جلب قوالب المعاملات المرتبطة بتصنيف محدد مع دعم التوريث من التصنيف الأب.
     */
    fun getCategoryParameterTemplates(categoryId: Long): List<CategoryParameterTemplateView> {
        val categoriesById = categoryRepository.getCategories().associateBy { it.id }
        val result = mutableListOf<CategoryParameterTemplateView>()
        val visitedTemplateIds = mutableSetOf<Long>()
        var currentCategoryId: Long? = categoryId
        var inherited = false
        var depth = 0

        // Bound traversal to the known category count so malformed parent cycles cannot loop forever.
        while (currentCategoryId != null && depth <= categoriesById.size) {
            val category = categoriesById[currentCategoryId] ?: break
            val categoryUuid = "cat-${currentCategoryId.toString().padStart(3, '0')}"
            for (link in partCategoryParameterTemplateDao.getForCategory(categoryUuid)) {
                if (!visitedTemplateIds.add(link.parameterTemplateId)) continue
                val template = partParameterTemplateDao.getById(link.parameterTemplateId) ?: continue
                result += CategoryParameterTemplateView(
                    categoryTemplate = link,
                    template = template,
                    isInherited = inherited,
                    sourceCategoryName = category.name
                )
            }
            currentCategoryId = category.parentId
            inherited = true
            depth++
        }
        return result
    }

    /**
     * إضافة قالب معامل تصنيف جديد لجدول PartCategoryParameterTemplate مع التحقق من الشروط.
     */
    fun addCategoryParameterTemplate(
        categoryId: Long,
        parameterTemplateId: Long,
        defaultValue: String? = null
    ) {
        val catUuid = "cat-${categoryId.toString().padStart(3, '0')}"
        val tplUuid = "param-tpl-$parameterTemplateId"
        runCatching {
            partCategoryParameterTemplateDao.insert(catUuid, tplUuid, defaultValue)
        }
        categoryParameterTable.insertCategoryParameterTemplate(
            categoryId = categoryId,
            parameterTemplateId = parameterTemplateId,
            defaultValue = defaultValue
        )
    }

    /**
     * جلب كافة قوالب المعاملات القياسية العامة (PartParameterTemplate).
     */
    fun getAllParameterTemplates(): List<PartParameterTemplate> =
        partParameterTemplateDao.getAll()

    /**
     * إدراج قالب معامل أو وحدة قياسية جديدة إلى النظام في قاعدة البيانات.
     */
    fun addParameterTemplate(name: String, units: String = "", description: String = ""): PartParameterTemplate {
        val template = PartParameterTemplate(
            name = name,
            units = units.ifBlank { name },
            description = description
        )
        val saved = partParameterTemplateDao.insert(template)
        runCatching { categoryParameterTable.insertParameterTemplate(saved) }
        return saved
    }

    /**
     * حذف قالب معامل/وحدة من النظام بحذف متتابع (CASCADE) في قاعدة البيانات.
     */
    fun deleteParameterTemplate(templateId: Long): Boolean {
        val deleted = partParameterTemplateDao.delete(templateId)
        if (deleted) {
            runCatching { categoryParameterTable.deleteParameterTemplate(templateId) }
        }
        return deleted
    }

    /**
     * حذف قالب معامل/وحدة عن طريق الاسم أو رمز الوحدة في قاعدة البيانات.
     */
    fun deleteParameterTemplateByNameOrUnit(unitCode: String): Boolean {
        val templates = getAllParameterTemplates()
        val match = templates.find {
            it.name.equals(unitCode, ignoreCase = true) ||
            it.units.equals(unitCode, ignoreCase = true)
        }
        return if (match != null) {
            deleteParameterTemplate(match.id)
        } else {
            false
        }
    }

    /**
     * جلب قيم المعاملات الفنية المحددة لقطعة معينة (PartParameter) من SQLite.
     */
    fun getPartParameters(partId: Long): List<PartParameter> {
        val targetPart = getPartById(partId) ?: return emptyList()
        return partParameterDao.getForPart(targetPart.effectiveUuid).map { it.copy(partId = partId) }
    }

    /**
     * إدراج خاصية فنية محددة لقطعة (PartParameter) مع تحويل القيمة العددية data_numeric وحفظها في SQLite.
     */
    fun addPartParameter(partId: Long, templateId: Long, data: String): PartParameter {
        val targetPart = getPartById(partId)
        val partUuid = targetPart?.effectiveUuid ?: "part-$partId"
        val templateUuid = "param-tpl-$templateId"
        val dataNumeric = data.toDoubleOrNull()
        val saved = partParameterDao.insertOrUpdate(partUuid, templateUuid, data, dataNumeric)
        runCatching {
            categoryParameterTable.insertPartParameter(
                partId = partId,
                templateId = templateId,
                data = data
            )
        }
        return saved
    }

    /**
     * إدراج صلة ربط تبادلية بين قطعتين في PartRelated وحفظها في SQLite.
     */
    fun addPartRelated(part1Id: Long, part2Id: Long): PartRelated {
        val p1 = getPartById(part1Id) ?: throw IllegalArgumentException("القطعة الأولى غير موجودة")
        val p2 = getPartById(part2Id) ?: throw IllegalArgumentException("القطعة الثانية غير موجودة")
        val saved = partRelatedDao.insert(p1.effectiveUuid, p2.effectiveUuid)
        runCatching { partRelatedTable.insertPartRelated(part1Id, part2Id) }
        return saved.copy(part1Id = part1Id, part2Id = part2Id)
    }

    /**
     * جلب القطع ذات الصلة لقطعة معينة بالاتجاهين المتبادلين من SQLite.
     */
    fun getRelatedPartsForPart(partId: Long): List<PartRelatedView> {
        val targetPart = getPartById(partId) ?: return emptyList()
        val relatedRecords = partRelatedDao.getRelatedForPart(targetPart.effectiveUuid)
        val allParts = getParts().associateBy { it.id }
        return relatedRecords.mapNotNull { rel ->
            val otherId = if (rel.part1Id == partId) rel.part2Id else rel.part1Id
            val otherPart = allParts[otherId] ?: getPartById(otherId)
            if (otherPart != null) {
                PartRelatedView(relatedRecord = rel, relatedPart = otherPart)
            } else null
        }
    }

    /**
     * حذف سجل صلة محدد من SQLite.
     */
    fun deletePartRelated(id: Long): Boolean {
        val deleted = partRelatedDao.delete(id)
        if (deleted) {
            runCatching { partRelatedTable.deletePartRelated(id) }
        }
        return deleted
    }

    /**
     * إدراج قالب فحص وضمان جودة جديد لقطعة في PartTestTemplate وحفظه في SQLite.
     */
    fun addPartTestTemplate(template: PartTestTemplate): PartTestTemplate {
        val targetPart = getPartById(template.partId)
        val partUuid = targetPart?.effectiveUuid ?: "part-${template.partId}"
        val saved = partTestTemplateDao.insert(template, partUuid)
        runCatching { partTestTemplateTable.insertTestTemplate(saved) }
        return saved
    }

    /**
     * جلب قوالب الفحوصات الخاصة بقطعة معينة من SQLite.
     */
    fun getPartTestTemplates(partId: Long): List<PartTestTemplate> {
        val targetPart = getPartById(partId) ?: return emptyList()
        return partTestTemplateDao.getForPart(targetPart.effectiveUuid).map { it.copy(partId = partId) }
    }

    /**
     * حذف قالب فحص محدد من SQLite.
     */
    fun deletePartTestTemplate(id: Long): Boolean {
        val uuid = "part-test-$id"
        val deleted = partTestTemplateDao.delete(uuid)
        if (deleted) {
            runCatching { partTestTemplateTable.deleteTestTemplate(id) }
        }
        return deleted
    }

    /**
     * إدراج مرفق أو رابط وثيقة جديد لقطعة في PartAttachment وحفظه في SQLite.
     */
    fun addPartAttachment(attachmentItem: PartAttachment): PartAttachment {
        val targetPart = getPartById(attachmentItem.partId)
        val partUuid = targetPart?.effectiveUuid ?: "part-${attachmentItem.partId}"
        val saved = partAttachmentDao.insertOrUpdate(attachmentItem, partUuid)
        runCatching { partAttachmentTable.insertAttachment(saved) }
        return saved
    }

    /**
     * جلب المرفقات والوثائق التابعة لقطعة معينة من SQLite.
     */
    fun getPartAttachments(partId: Long): List<PartAttachment> {
        val targetPart = getPartById(partId) ?: return emptyList()
        return partAttachmentDao.getForPart(targetPart.effectiveUuid).map { it.copy(partId = partId) }
    }

    /**
     * حذف مرفق محدد من SQLite.
     */
    fun deletePartAttachment(id: Long): Boolean {
        val uuid = "part-att-$id"
        val deleted = partAttachmentDao.delete(uuid)
        if (deleted) {
            runCatching { partAttachmentTable.deleteAttachment(id) }
        }
        return deleted
    }

    /**
     * حفظ أو تحديث سجل الملاحظات التفصيلية لقطعة معينة في PartNotes وحفظها في SQLite.
     */
    fun saveOrUpdatePartNotes(partId: Long, notes: String, userId: Long? = 1L): PartNotes {
        val targetPart = getPartById(partId)
        val partUuid = targetPart?.effectiveUuid ?: "part-$partId"
        val saved = partNotesDao.saveOrUpdate(partUuid, notes, userId)
        runCatching { partNotesTable.saveOrUpdateNotes(partId = partId, newNotes = notes, userId = userId) }
        return saved
    }

    /**
     * جلب الملاحظات التفصيلية الخاصة بقطعة معينة من SQLite.
     */
    fun getPartNotes(partId: Long): PartNotes? {
        val targetPart = getPartById(partId) ?: return null
        return partNotesDao.getForPart(targetPart.effectiveUuid)?.copy(partId = partId)
    }

    /**
     * جلب سجل حساب وتسعير التكاليف لقطعة معينة في PartPricing من SQLite.
     */
    fun getPartPricing(partId: Long): PartPricingEntity? {
        val targetPart = getPartById(partId) ?: return null
        return partPricingDao.getForPart(targetPart.effectiveUuid)?.copy(partId = partId)
    }

    /**
     * إعادة حساب وتحديث التكاليف المجمعة لقطعة معينة في PartPricing وحفظها في SQLite.
     */
    fun recalculatePartPricing(part: Part, bomItems: List<BomItem> = emptyList()): PartPricingEntity {
        val internalPrices = partInternalPriceDao.getForPart(part.effectiveUuid)
        val calculated = PartPricingCalculator.calculate(
            part = part,
            bomItems = bomItems,
            internalPrices = internalPrices
        )
        return partPricingDao.saveOrUpdate(calculated, part.effectiveUuid).copy(partId = part.id)
    }

    /**
     * إدراج شريحة سعرية داخلية جديدة لقطعة في PartInternalPrice وحفظها في SQLite.
     */
    fun addPartInternalPrice(
        partId: Long,
        quantity: Double,
        price: Double,
        currency: String = "USD"
    ): PartInternalPriceEntity {
        val targetPart = getPartById(partId)
        val partUuid = targetPart?.effectiveUuid ?: "part-$partId"
        val saved = partInternalPriceDao.insert(
            partUuid = partUuid,
            quantity = quantity,
            price = price,
            currency = currency
        )
        runCatching {
            partInternalPriceTable.insertInternalPrice(
                partId = partId,
                quantity = quantity,
                price = price,
                currency = currency
            )
        }
        return saved
    }

    /**
     * جلب شرائح الأسعار والتكاليف الداخلية لقطعة معينة من SQLite.
     */
    fun getPartInternalPrices(partId: Long): List<PartInternalPriceEntity> {
        val targetPart = getPartById(partId) ?: return emptyList()
        return partInternalPriceDao.getForPart(targetPart.effectiveUuid).map { it.copy(partId = partId) }
    }

    /**
     * حذف شريحة سعرية محددة من SQLite.
     */
    fun deletePartInternalPrice(id: Long): Boolean {
        val uuid = "part-iprice-$id"
        val deleted = partInternalPriceDao.delete(uuid)
        if (deleted) {
            runCatching { partInternalPriceTable.deleteInternalPrice(id) }
        }
        return deleted
    }

    /**
     * إدراج شريحة سعر بيع جديدة للعملاء مع شرط القابلية للبيع (salable = true) وحفظها في SQLite.
     */
    fun addPartSalePrice(
        part: Part,
        quantity: Double,
        price: Double,
        currency: String = "USD"
    ): PartSalePriceEntity {
        require(part.salable) { "لا يمكن إضافة سعر بيع لقطعة غير قابلة للبيع (salable must be true)" }
        val saved = partSalePriceDao.insert(
            partUuid = part.effectiveUuid,
            quantity = quantity,
            price = price,
            currency = currency
        )
        runCatching {
            partSalePriceTable.insertSalePrice(
                part = part,
                quantity = quantity,
                price = price,
                currency = currency
            )
        }
        return saved
    }

    /**
     * جلب شرائح أسعار البيع للعملاء لقطعة معينة من SQLite.
     */
    fun getPartSalePrices(partId: Long): List<PartSalePriceEntity> {
        val targetPart = getPartById(partId) ?: return emptyList()
        return partSalePriceDao.getForPart(targetPart.effectiveUuid).map { it.copy(partId = partId) }
    }

    /**
     * حساب أنسب شريحة سعر بيع لطلب عميل بناءً على الكمية المطلوبة من SQLite.
     */
    fun getBestPartSalePriceForQuantity(partId: Long, quantity: Double): PartSalePriceEntity? {
        val prices = getPartSalePrices(partId)
        if (prices.isEmpty()) return null
        return prices.filter { it.quantity <= quantity }.maxByOrNull { it.quantity }
            ?: prices.minByOrNull { it.quantity }
    }

    /**
     * حذف شريحة سعر بيع محددة من SQLite.
     */
    fun deletePartSalePrice(id: Long): Boolean {
        val uuid = "part-sprice-$id"
        val deleted = partSalePriceDao.delete(uuid)
        if (deleted) {
            runCatching { partSalePriceTable.deleteSalePrice(id) }
        }
        return deleted
    }

    /**
     * تبديل حالة تفضيل ومتابعة القطعة (Star / Unstar Toggle Action) في SQLite.
     */
    fun togglePartStar(partId: Long, userId: Long = 1L): Boolean {
        val targetPart = getPartById(partId)
        val partUuid = targetPart?.effectiveUuid ?: "part-$partId"
        val userUuid = "usr-${userId.toString().padStart(3, '0')}"
        val result = partStarDao.toggleStar(partUuid = partUuid, userUuid = userUuid, partId = partId)
        runCatching { partStarTable.toggleStarForPart(partId, userId) }
        return result
    }

    /**
     * التحقق مما إذا كانت القطعة مميزة بنجمة ومفضلة للمستخدم الحالي من SQLite.
     */
    fun isPartStarred(partId: Long, userId: Long = 1L): Boolean {
        val targetPart = getPartById(partId)
        val partUuid = targetPart?.effectiveUuid ?: "part-$partId"
        val userUuid = "usr-${userId.toString().padStart(3, '0')}"
        return partStarDao.isStarred(partUuid = partUuid, userUuid = userUuid, partId = partId)
    }

    /**
     * جلب قائمة معرفات القطع المفضلة والمتابعة للمستخدم الحالي من SQLite.
     */
    fun getStarredPartIdsForUser(userId: Long = 1L): List<Long> {
        val userUuid = "usr-${userId.toString().padStart(3, '0')}"
        val uuids = partStarDao.getStarredPartUuids(userUuid)
        return uuids.mapNotNull { uuid ->
            partDao.getPartByUuid(uuid)?.id
                ?: uuid.removePrefix("part-uuid-").removePrefix("part-").toLongOrNull()
        }.distinct()
    }

    /**
     * تحديث بيانات قطعة موجودة مع التحقق الصارم من وجودها في SQLite وفصل التحديث عن الإنشاء.
     */
    fun updatePart(part: Part, pendingAttachments: List<PendingAttachment> = emptyList()): Boolean {
        val targetUuid = part.effectiveUuid
        val existingEntity = partDao.getPartByUuid(targetUuid) ?: return false

        val resolvedImageUrl = pendingAttachments.firstOrNull { it.type == AttachmentType.IMAGE }?.pathOrUrl ?: part.imageUrl
        val resolvedLink = pendingAttachments.firstOrNull { it.type == AttachmentType.LINK }?.pathOrUrl 
            ?: pendingAttachments.firstOrNull { it.type == AttachmentType.DOCUMENT }?.pathOrUrl 
            ?: part.link

        val partToUpdate = part.copy(imageUrl = resolvedImageUrl, link = resolvedLink)

        val updatedEntity = PartEntity(
            uuid = targetUuid,
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
            version = existingEntity.version + 1,
            syncStatus = SyncStatus.PENDING,
            isDeleted = false,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )

        val updated = partDao.update(updatedEntity)
        if (!updated) return false

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
