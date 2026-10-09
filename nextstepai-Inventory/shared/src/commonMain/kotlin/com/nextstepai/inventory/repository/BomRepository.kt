package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemSubstitute
import com.nextstepai.inventory.data.BomItemSubstituteView
import com.nextstepai.inventory.data.BomItemTable
import com.nextstepai.inventory.data.ManufacturingPhase
import com.nextstepai.inventory.data.ManufacturingPhaseTable
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.db.BomItemDao
import com.nextstepai.inventory.data.db.BomItemEntity
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن إدارة بنود قائمة مواد التصنيع (BOM)
 * والربط مع المزامنة الدفعية والاستعلام المحدث بالصفحات.
 */
class BomRepository(
    private val bomItemTable: BomItemTable = BomItemTable(),
    private val bomItemDao: BomItemDao = BomItemDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService(),
    private val manufacturingPhaseTable: ManufacturingPhaseTable = ManufacturingPhaseTable()
) {
    /**
     * جلب بنود قائمة المواد مجزأة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getBomItemsPaged(partId: Long? = null, limit: Int = 20, offset: Int = 0): List<BomItemEntity> {
        return bomItemDao.getBomItemsPaged(partId = partId, limit = limit, offset = offset)
    }

    /**
     * جلب بنود قائمة المواد الخاصة بمنتج أب محدد من قاعدة البيانات المستمرة (SQLite).
     */
    fun getBomItemsForPart(partId: Long): List<BomItem> {
        val entities = bomItemDao.getBomItemsPaged(partId = partId, limit = 500, offset = 0)
        return entities.mapIndexed { index, entity ->
            val parsedId = entity.uuid.removePrefix("bom-").toLongOrNull() ?: (index + 1L)
            BomItem(
                id = parsedId,
                uuid = entity.uuid,
                partId = entity.partId,
                subPartId = entity.subPartId,
                quantity = entity.quantity,
                reference = entity.reference,
                optional = entity.optional,
                consumable = entity.consumable,
                allowVariants = entity.allowVariants,
                inherited = entity.inherited,
                note = entity.note,
                checksum = entity.checksum,
                phaseUuid = entity.phaseUuid
            )
        }
    }

    /**
     * إضافة بند جديد إلى قائمة المواد مع تحقق قيد الفرادة والقيد ضد التكرار الحلزوني.
     */
    fun addBomItem(bomItem: BomItem): BomItem {
        val inserted = bomItemTable.insertBomItem(bomItem)
        val targetUuid = if (bomItem.uuid.isNotBlank()) bomItem.uuid else "bom-${inserted.id}"
        val itemWithUuid = inserted.copy(uuid = targetUuid)
        bomItemDao.insertOrUpdate(
            BomItemEntity(
                uuid = targetUuid,
                partId = itemWithUuid.partId,
                subPartId = itemWithUuid.subPartId,
                quantity = itemWithUuid.quantity,
                reference = itemWithUuid.reference,
                optional = itemWithUuid.optional,
                consumable = itemWithUuid.consumable,
                allowVariants = itemWithUuid.allowVariants,
                inherited = itemWithUuid.inherited,
                note = itemWithUuid.note,
                checksum = itemWithUuid.checksum,
                phaseUuid = itemWithUuid.phaseUuid,
                syncStatus = SyncStatus.PENDING,
                isDeleted = false,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        return itemWithUuid
    }

    /**
     * تحديث بند قائمة مواد موجود.
     */
    fun updateBomItem(bomItem: BomItem): BomItem {
        val updated = bomItemTable.updateBomItem(bomItem)
        val targetUuid = if (bomItem.uuid.isNotBlank()) bomItem.uuid else "bom-${updated.id}"
        val itemWithUuid = updated.copy(uuid = targetUuid)
        bomItemDao.insertOrUpdate(
            BomItemEntity(
                uuid = targetUuid,
                partId = itemWithUuid.partId,
                subPartId = itemWithUuid.subPartId,
                quantity = itemWithUuid.quantity,
                reference = itemWithUuid.reference,
                optional = itemWithUuid.optional,
                consumable = itemWithUuid.consumable,
                allowVariants = itemWithUuid.allowVariants,
                inherited = itemWithUuid.inherited,
                note = itemWithUuid.note,
                checksum = itemWithUuid.checksum,
                phaseUuid = itemWithUuid.phaseUuid,
                syncStatus = SyncStatus.PENDING,
                isDeleted = false,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        return itemWithUuid
    }

    /**
     * إضافة قطعة بديلة لبند قائمة المواد في BomItemSubstitute.
     */
    fun addSubstitute(
        bomItemId: Long,
        partId: Long,
        partsList: List<Part> = emptyList()
    ): BomItemSubstitute {
        return bomItemTable.insertSubstitute(
            bomItemId = bomItemId,
            substitutePartId = partId,
            partsList = partsList
        )
    }

    /**
     * جلب قائمة البدائل المعتمدة لبند قائمة مواد محدد.
     */
    fun getSubstitutesForBomItem(
        bomItemId: Long,
        partsList: List<Part> = emptyList()
    ): List<BomItemSubstituteView> {
        return bomItemTable.getSubstitutesForBomItem(bomItemId, partsList)
    }

    /**
     * حذف قطعة بديلة.
     */
    fun deleteSubstitute(id: Long): Boolean {
        return bomItemTable.deleteSubstitute(id)
    }

    /**
     * حذف بند من قائمة المواد وتفريغ ارتباطاته من SQLite والذاكرة المؤقتة بالـ UUID.
     */
    fun deleteBomItem(uuid: String, id: Long = 0L): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        if (uuid.isNotBlank()) {
            bomItemDao.softDeleteByIdOrUuid(id = id, uuid = uuid, updatedAt = now)
        } else if (id > 0L) {
            bomItemDao.softDeleteByIdOrUuid(id = id, uuid = "bom-$id", updatedAt = now)
        }
        bomItemTable.deleteBomItemByUuid(uuid)
        return bomItemTable.deleteBomItem(id)
    }

    fun deleteBomItem(id: Long): Boolean {
        return deleteBomItem(uuid = "bom-$id", id = id)
    }

    /**
     * تنفيذ المزامنة الدفعية (Batch Sync) مع Cloudflare Worker في طلب شبكي واحد.
     */
    suspend fun syncPendingBomChanges(): Int {
        val pending = bomItemDao.getPendingSyncBomItems(limit = 50, offset = 0)
        if (pending.isEmpty()) return 0

        val payloads = pending.map { entity ->
            SyncPayload(
                uuid = entity.uuid,
                entityType = "BomItem",
                payloadJson = "{\"partId\":${entity.partId},\"subPartId\":${entity.subPartId},\"quantity\":${entity.quantity}}",
                isDeleted = entity.isDeleted,
                updatedAt = entity.updatedAt
            )
        }

        val response = batchSyncService.performBatchSync(
            pendingPushes = payloads,
            lastSyncTimestamp = Clock.System.now().toEpochMilliseconds() - 86400000
        )

        bomItemDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }

    /**
     * جلب كافة المراحل التصنيعية العامة والخاصة بالمنتج الأب المجمع.
     */
    fun getAllPhases(partUuid: String? = null): List<ManufacturingPhase> {
        return manufacturingPhaseTable.getAllPhases(partUuid)
    }

    /**
     * إضافة مرحلة تصنيعية معيارية جديدة وقيدها بقاعدة البيانات.
     */
    fun addPhase(name: String, description: String = "", sequenceOrder: Int = 1, partUuid: String? = null): ManufacturingPhase {
        return manufacturingPhaseTable.insertPhase(
            ManufacturingPhase(
                name = name,
                description = description,
                sequenceOrder = sequenceOrder,
                partUuid = partUuid
            )
        )
    }

    /**
     * حذف مرحلة تصنيعية مخصصة بحذف متتابع.
     */
    fun deletePhase(uuid: String): Boolean {
        return manufacturingPhaseTable.deletePhase(uuid)
    }
}
