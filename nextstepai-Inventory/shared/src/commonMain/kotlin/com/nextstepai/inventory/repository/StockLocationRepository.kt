package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.*
import com.nextstepai.inventory.data.db.SqliteNumericIdAllocator
import com.nextstepai.inventory.data.db.StockLocationDao
import com.nextstepai.inventory.data.db.StockLocationTypeDao
import java.io.File
import kotlin.time.Clock

/**
 * مستودع إدارة مواقع التخزين وهيكلية المستودعات (Stock Locations & Types).
 * معزول عن تفاصيل بنود المخزون لضمان فصل المسؤوليات (Separation of Concerns).
 */
class StockLocationRepository(
    private val locationDao: StockLocationDao = StockLocationDao(),
    private val locationTypeDao: StockLocationTypeDao = StockLocationTypeDao(),
    @Suppress("DEPRECATION")
    private val stockTable: StockItemTable = StockItemTable()
) {
    /**
     * جلب كافة مواقع التخزين المتاحة من SQLite مع السقوط الآمن على الجدول المحلي.
     */
    fun getLocations(): List<StockLocation> {
        val entities = runCatching { locationDao.getAllLocations() }.getOrDefault(emptyList())
        if (entities.isNotEmpty()) {
            return entities.map { entity ->
                val resolvedId = if (entity.id > 0L) entity.id else {
                    entity.uuid.removePrefix("location-").removePrefix("loc-").toLongOrNull() ?: 1L
                }
                val resolvedParentId = entity.parentId ?: entity.parentUuid?.removePrefix("location-")?.removePrefix("loc-")?.toLongOrNull()
                StockLocation(
                    id = resolvedId,
                    uuid = entity.uuid,
                    name = entity.name,
                    description = entity.description,
                    parentId = resolvedParentId,
                    structural = entity.structural,
                    external = entity.external,
                    locationType = entity.locationType,
                    ownerId = entity.ownerId,
                    icon = entity.icon,
                    customIcon = entity.customIcon,
                    address = entity.address,
                    customCapacity = entity.customCapacity,
                    isBulkGenerated = entity.isBulkGenerated,
                    level = entity.level,
                    lft = entity.lft,
                    rght = entity.rght,
                    treeId = entity.treeId,
                    metadata = entity.metadata
                )
            }
        }
        return stockTable.getAllLocations()
    }

    /**
     * جلب كافة أنواع وقوالب مواقع التخزين المتاحة مع مواصفاتها الهندسية.
     */
    fun getLocationTypes(): List<StockLocationType> {
        val entities = runCatching { locationTypeDao.getAllLocationTypes() }.getOrDefault(emptyList())
        return entities.map { entity ->
            StockLocationType(
                id = entity.typeId,
                name = entity.name,
                description = entity.description,
                icon = entity.icon,
                customIcon = entity.customIcon,
                length = entity.length,
                width = entity.width,
                height = entity.height,
                maxWeight = entity.maxWeight,
                maxVolume = entity.maxVolume,
                metadata = entity.metadata
            )
        }
    }

    /**
     * حساب توليد المسار الهرمي الكامل التراكمي للموقع من الجذر حتى النهاية.
     */
    fun getFullPathForLocation(locationId: Long?, separator: String = " / "): String {
        return stockTable.getFullPathForLocation(locationId, separator)
    }

    /**
     * كاشف التكرار الميداني للهرمية (Auto-Collision & Duplicate Guard):
     * التحقق مما إذا كان ينتج عن حفظ موقع جديد/معدل نفس الاسم أو نفس العنوان تحت نفس الأب المباشر ونفس المسار.
     */
    fun isLocationDuplicateUnderSameParent(
        name: String,
        parentId: Long?,
        excludeId: Long? = null,
        address: String? = null
    ): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return false
        val trimmedAddress = address?.trim() ?: ""

        val allLocations = getLocations()
        return allLocations.any { loc ->
            if (loc.id == excludeId) return@any false
            if (loc.parentId != parentId) return@any false

            val nameMatch = loc.name.trim().equals(trimmedName, ignoreCase = true)
            val addressMatch = trimmedAddress.isNotBlank() && loc.address.trim().isNotBlank() &&
                    loc.address.trim().equals(trimmedAddress, ignoreCase = true)

            nameMatch || addressMatch
        }
    }

    /**
     * خوارزمية منع التكرار البرمجي آلياً (Auto-Collision Prevention Algorithm):
     * توليد اسم فريد بإضافة ترقيم تسلسلي تلقائي عند وجود اسم مكرر تحت نفس الأب.
     */
    fun generateUniqueLocationName(baseName: String, parentId: Long?, excludeId: Long? = null): String {
        val trimmed = baseName.trim().ifBlank { "موقع" }
        var candidate = trimmed
        var counter = 1
        val allLocations = getLocations()

        while (allLocations.any { loc ->
            loc.id != excludeId && loc.parentId == parentId && loc.name.trim().equals(candidate, ignoreCase = true)
        }) {
            candidate = "$trimmed-${counter.toString().padStart(2, '0')}"
            counter++
        }
        return candidate
    }

    /**
     * إضافة أو تحديث موقع تخزيني جديد في الشجرة الهرمية لمواقع التخزين (StockLocation) مع حسم التعارضات المزامنة وحماية الموقع الأساسي.
     */
    fun addLocation(location: StockLocation): StockLocation {
        val existingLocs = getLocations()
        val maxExistingId = maxOf(stockTable.getAllLocations().maxOfOrNull { it.id } ?: 0L, existingLocs.maxOfOrNull { it.id } ?: 0L)
        val allocatedId = if (location.id == 0L) {
            runCatching {
                SqliteNumericIdAllocator.nextId("stock_locations", "loc-")
            }.getOrElse { maxExistingId + 1L }
        } else {
            location.id
        }
        val finalUuid = if (location.uuid.isNotBlank() && !location.uuid.startsWith("location-")) {
            location.uuid
        } else {
            "loc-$allocatedId"
        }
        val locationWithId = location.copy(
            id = allocatedId,
            uuid = finalUuid
        )
        var finalLocation = locationWithId

        if (location.isPrimary) {
            val existingPrimary = getLocations().find { existing ->
                existing.id != location.id && existing.isPrimary &&
                        when {
                            location.external -> existing.external
                            location.locationType.equals("SITE", ignoreCase = true) -> existing.locationType.equals("SITE", ignoreCase = true) && !existing.external
                            location.locationType.equals("WAREHOUSE", ignoreCase = true) -> existing.locationType.equals("WAREHOUSE", ignoreCase = true) && !existing.external
                            else -> false
                        }
            }

            if (existingPrimary != null) {
                val allEntities = runCatching { locationDao.getAllLocations() }.getOrDefault(emptyList())
                val locUpdatedAt = allEntities.find { it.uuid == location.uuid || it.uuid == "loc-${location.id}" }?.updatedAt ?: Clock.System.now().toEpochMilliseconds()
                val existingUpdatedAt = allEntities.find { it.uuid == existingPrimary.uuid || it.uuid == "loc-${existingPrimary.id}" }?.updatedAt ?: 0L

                if (locUpdatedAt >= existingUpdatedAt) {
                    val demoted = existingPrimary.withPrimary(false)
                    stockTable.insertLocation(demoted)
                    locationDao.insertOrUpdate(demoted.toEntity())
                } else {
                    finalLocation = location.withPrimary(false)
                }
            }
        }

        val inserted = stockTable.insertLocation(finalLocation)
        locationDao.insertOrUpdate(inserted.toEntity())
        return inserted
    }

    /**
     * إنشاء سلسلة هرمية ذرية للموقع المستهدف مع كافة طبقاته الوسيطة المفقودة (Atomic Transaction).
     */
    fun addLocationWithIntermediates(
        targetLocation: StockLocation,
        intermediates: List<IntermediateNodeSpec>
    ): StockLocation {
        var currentParentId = targetLocation.parentId

        for (spec in intermediates) {
            if (spec.existingId != null && spec.existingId > 0L) {
                currentParentId = spec.existingId
            } else if (spec.name.isNotBlank()) {
                val newIntermediate = StockLocation(
                    name = spec.name.trim(),
                    description = "طبقة وسيطة مضافة آلياً لحشو فجوة الهرمية",
                    parentId = currentParentId,
                    structural = true,
                    locationType = spec.locationType
                )
                val inserted = addLocation(newIntermediate)
                currentParentId = inserted.id
            }
        }

        val finalTarget = targetLocation.copy(parentId = currentParentId)
        return addLocation(finalTarget)
    }

    /**
     * تحديث بيانات موقع تخزيني قائم في الشجرة الهرمية لمواقع التخزين (StockLocation).
     */
    fun updateLocation(location: StockLocation): StockLocation {
        return addLocation(location)
    }

    /**
     * أرشفة وحفظ لقطة صورة الملصق المادية وتحديث بيانات الأرشفة لجدول الموقع.
     */
    fun saveLocationLabelSnapshot(locationId: Long, snapshotData: String): StockLocation? {
        val targetLoc = getLocations().find { it.id == locationId } ?: return null
        val genAt = Clock.System.now().toEpochMilliseconds()
        val imagePath = "files/labels/locations/loc_${targetLoc.effectiveUuid}.webp"
        val updatedLoc = targetLoc.withLabelSnapshot(imagePath, genAt, snapshotData)
        return updateLocation(updatedLoc)
    }

    /**
     * إضافة دفعة مواقع تخزينية متسلسلة جديدة (Bulk Location Generator).
     */
    fun addBatchLocations(locations: List<StockLocation>): List<StockLocation> {
        val allocatedLocations = locations.map { loc ->
            if (loc.id <= 0L) {
                val nextId = runCatching {
                    SqliteNumericIdAllocator.nextId("stock_locations", "loc-")
                }.getOrElse { (getLocations().maxOfOrNull { it.id } ?: 0L) + 1L }
                loc.copy(id = nextId, uuid = loc.uuid.ifBlank { "loc-$nextId" })
            } else {
                loc
            }
        }
        val insertedList = stockTable.insertBatchLocations(allocatedLocations)
        locationDao.insertBatchLocations(insertedList.map { it.toEntity() })
        return insertedList
    }

    /**
     * حذف موقع تخزيني حذفاً مرناً (Soft Delete).
     */
    fun deleteLocationInternal(locationId: Long): Boolean {
        val targetLoc = getLocations().find { it.id == locationId }
        val wasPrimary = targetLoc?.isPrimary == true
        val targetType = targetLoc?.locationType ?: ""

        val removedFromTable = stockTable.deleteLocation(locationId)
        locationDao.softDeleteLocation(targetLoc?.uuid ?: "loc-$locationId")
        targetLoc?.labelImagePath?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }

        if (wasPrimary && targetLoc != null) {
            val remainingOfSameType = getLocations().filter { loc ->
                loc.id != locationId &&
                        when {
                            targetLoc.external -> loc.external
                            targetType.equals("SITE", ignoreCase = true) -> loc.locationType.equals("SITE", ignoreCase = true) && !loc.external
                            targetType.equals("WAREHOUSE", ignoreCase = true) -> loc.locationType.equals("WAREHOUSE", ignoreCase = true) && !loc.external
                            else -> false
                        }
            }

            if (remainingOfSameType.isNotEmpty()) {
                val oldestRemaining = remainingOfSameType.minByOrNull { it.id } ?: remainingOfSameType.first()
                updateLocation(oldestRemaining.withPrimary(true))
            }
        }

        return removedFromTable
    }
}
