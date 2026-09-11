package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemSubstitute
import com.nextstepai.inventory.data.BomItemSubstituteView
import com.nextstepai.inventory.data.BomItemTable
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.db.BomItemDao
import com.nextstepai.inventory.data.db.BomItemEntity
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

import com.nextstepai.inventory.data.db.getRoomDatabase

/**
 * المستودع (Repository) المسؤول عن إدارة بنود قائمة مواد التصنيع (BOM)
 * والربط مع المزامنة الدفعية والاستعلام المحدث بالصفحات.
 */
class BomRepository(
    private val bomItemTable: BomItemTable = BomItemTable(),
    private val bomItemDao: BomItemDao = BomItemDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService()
) {
    /**
     * جلب بنود قائمة المواد مجزأة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getBomItemsPaged(partId: Long? = null, limit: Int = 20, offset: Int = 0): List<BomItemEntity> {
        return bomItemDao.getBomItemsPaged(partId = partId, limit = limit, offset = offset)
    }

    /**
     * جلب بنود قائمة المواد الخاصة بمنتج أب محدد.
     */
    fun getBomItemsForPart(partId: Long): List<BomItem> {
        return bomItemTable.getBomItemsForPart(partId)
    }

    /**
     * إضافة بند جديد إلى قائمة المواد مع تحقق قيد الفرادة والقيد ضد التكرار الحلزوني.
     */
    fun addBomItem(bomItem: BomItem): BomItem {
        val inserted = bomItemTable.insertBomItem(bomItem)
        runBlocking {
            bomItemDao.insertOrUpdate(
                BomItemEntity(
                    uuid = "bom-${inserted.id}",
                    partId = inserted.partId,
                    subPartId = inserted.subPartId,
                    quantity = inserted.quantity,
                    reference = inserted.reference,
                    optional = inserted.optional,
                    consumable = inserted.consumable,
                    allowVariants = inserted.allowVariants,
                    inherited = inserted.inherited,
                    note = inserted.note,
                    checksum = inserted.checksum,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
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
}
