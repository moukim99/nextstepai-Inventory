package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildOrderLineItemTable
import com.nextstepai.inventory.data.BuildOrderTable
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.db.BuildOrderDao
import com.nextstepai.inventory.data.db.BuildOrderEntity
import com.nextstepai.inventory.data.db.BuildOrderLineItemDao
import com.nextstepai.inventory.data.db.BuildOrderLineItemEntity
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن إدارة أوامر التصنيع والإنتاج (Build Orders) وبنودها والمزامنة الدفعية.
 */
class BuildOrderRepository(
    private val buildTable: BuildOrderTable = BuildOrderTable(),
    private val lineItemTable: BuildOrderLineItemTable = BuildOrderLineItemTable(),
    private val buildDao: BuildOrderDao = BuildOrderDao(),
    private val lineItemDao: BuildOrderLineItemDao = BuildOrderLineItemDao(),
    private val batchSyncService: BatchSyncService = BatchSyncService()
) {
    /**
     * جلب أوامر الإنتاج مجزأة صفحات (LIMIT & OFFSET) لمنع التحميل الكامل في الذاكرة.
     */
    suspend fun getBuildOrdersPaged(
        partId: Long? = null,
        status: BuildStatus? = null,
        limit: Int = 20,
        offset: Int = 0
    ): List<BuildOrderEntity> {
        return buildDao.getBuildOrdersPaged(partId = partId, statusCode = status?.code, limit = limit, offset = offset)
    }

    /**
     * البحث والفلترة في أوامر التصنيع.
     */
    fun searchBuilds(
        query: String = "",
        partId: Long? = null,
        status: BuildStatus? = null
    ): List<BuildOrder> {
        return buildTable.searchBuilds(query = query, partId = partId, status = status)
    }

    /**
     * جلب بنود ومخرجات أمر التصنيع المحددة.
     */
    fun getLineItemsForBuild(buildId: Long): List<BuildOrderLineItem> {
        return lineItemTable.getLineItemsForBuild(buildId)
    }

    /**
     * إضافة بند جديد لأمر التصنيع.
     */
    fun addLineItem(item: BuildOrderLineItem): BuildOrderLineItem {
        val inserted = lineItemTable.insertLineItem(item)
        runBlocking {
            lineItemDao.insertOrUpdate(
                BuildOrderLineItemEntity(
                    uuid = "lineitem-${inserted.id}",
                    id = inserted.id,
                    buildId = inserted.buildId,
                    bomItemId = inserted.bomItemId,
                    subPartId = inserted.subPartId,
                    subPartName = inserted.subPartName,
                    quantity = inserted.quantity,
                    allocatedQuantity = inserted.allocatedQuantity,
                    consumedQuantity = inserted.consumedQuantity,
                    notes = inserted.notes,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    /**
     * تخصيص مخزون لبند في أمر التصنيع (Allocate Stock).
     */
    fun allocateStock(lineItemId: Long, quantity: Double): Boolean {
        return lineItemTable.allocateStock(lineItemId, quantity)
    }

    /**
     * استهلاك مخزون مخصص للبند (Consume Stock).
     */
    fun consumeStock(lineItemId: Long, quantity: Double): Boolean {
        return lineItemTable.consumeStock(lineItemId, quantity)
    }

    /**
     * إضافة أمر إنتاج جديد مع حفظ الكيان المحلي القابل للمزامنة.
     */
    fun addBuildOrder(build: BuildOrder): BuildOrder {
        val inserted = buildTable.insertBuild(build)
        runBlocking {
            buildDao.insertOrUpdate(
                BuildOrderEntity(
                    uuid = "build-${inserted.id}",
                    reference = inserted.reference,
                    title = inserted.title,
                    partId = inserted.partId,
                    partName = inserted.partName,
                    quantity = inserted.quantity,
                    completedQuantity = inserted.completedQuantity,
                    statusCode = inserted.status.code,
                    batch = inserted.batch,
                    targetDate = inserted.targetDate,
                    startDate = inserted.startDate,
                    completionDate = inserted.completionDate,
                    creationDate = inserted.creationDate,
                    parentId = inserted.parentId,
                    salesOrderId = inserted.salesOrderId,
                    takeFromLocationId = inserted.takeFromLocationId,
                    destinationLocationId = inserted.destinationLocationId,
                    issuedBy = inserted.issuedBy,
                    responsible = inserted.responsible,
                    notes = inserted.notes,
                    link = inserted.link,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    /**
     * بدء الإنتاج الفعلي.
     */
    fun startProduction(buildId: Long): Boolean {
        return buildTable.startProduction(buildId)
    }

    /**
     * إلغاء أمر التصنيع.
     */
    fun cancelBuildOrder(buildId: Long): Boolean {
        return buildTable.cancelBuildOrder(buildId)
    }

    /**
     * إنهاء وتوريد كمية مخرجة جديدة (Complete Build Output).
     */
    fun completeBuildOutput(buildId: Long, completedQty: Double): Boolean {
        return buildTable.completeBuildOutput(buildId, completedQty)
    }

    /**
     * المزامنة الدفعية (Batch Sync) مع Cloudflare Worker.
     */
    suspend fun syncPendingBuilds(): Int {
        val pending = buildDao.getPendingSyncBuilds(limit = 50, offset = 0)
        if (pending.isEmpty()) return 0

        val payloads = pending.map { entity ->
            SyncPayload(
                uuid = entity.uuid,
                entityType = "BuildOrder",
                payloadJson = "{\"reference\":\"${entity.reference}\",\"quantity\":${entity.quantity},\"completedQuantity\":${entity.completedQuantity}}",
                isDeleted = entity.isDeleted,
                updatedAt = entity.updatedAt
            )
        }

        val response = batchSyncService.performBatchSync(payloads, Clock.System.now().toEpochMilliseconds() - 86400000)
        buildDao.updateSyncStatusForUuids(response.acceptedUuids, SyncStatus.SYNCED)
        return response.acceptedUuids.size
    }
}
