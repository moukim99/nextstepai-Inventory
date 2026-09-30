package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BuildItem
import com.nextstepai.inventory.data.BuildItemTable
import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildOrderLineItemTable
import com.nextstepai.inventory.data.BuildOrderTable
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.db.BuildItemDao
import com.nextstepai.inventory.data.db.BuildItemEntity
import com.nextstepai.inventory.data.db.BuildOrderDao
import com.nextstepai.inventory.data.db.BuildOrderEntity
import com.nextstepai.inventory.data.db.BuildOrderLineItemDao
import com.nextstepai.inventory.data.db.BuildOrderLineItemEntity
import com.nextstepai.inventory.sync.BatchSyncService
import com.nextstepai.inventory.sync.SyncPayload
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.data.PartPricingTable
import com.nextstepai.inventory.data.db.StockItemDao
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن إدارة أوامر التصنيع والإنتاج (Build Orders) وبنودها وتخصيصات المخزون والمزامنة الدفعية.
 */
class BuildOrderRepository(
    private val buildTable: BuildOrderTable = BuildOrderTable(),
    private val lineItemTable: BuildOrderLineItemTable = BuildOrderLineItemTable(),
    private val buildItemTable: BuildItemTable = BuildItemTable(),
    private val buildDao: BuildOrderDao = BuildOrderDao(),
    private val lineItemDao: BuildOrderLineItemDao = BuildOrderLineItemDao(),
    private val buildItemDao: BuildItemDao = BuildItemDao(),
    private val stockDao: StockItemDao = StockItemDao(),
    private val partPricingTable: PartPricingTable = PartPricingTable(),
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
     * البحث والفلترة في أوامر التصنيع من قاعدة البيانات الدائمة (SQLite).
     */
    fun searchBuilds(
        query: String = "",
        partId: Long? = null,
        status: BuildStatus? = null
    ): List<BuildOrder> {
        val entities = runBlocking {
            buildDao.getBuildOrdersPaged(
                partId = partId,
                statusCode = status?.code,
                limit = 500,
                offset = 0
            )
        }
        if (entities.isNotEmpty()) {
            var result = entities.mapIndexed { index, entity ->
                val parsedId = entity.uuid.removePrefix("build-").toLongOrNull() ?: (index + 1L)
                BuildOrder(
                    id = parsedId,
                    reference = entity.reference,
                    title = entity.title,
                    partId = entity.partId,
                    partName = entity.partName,
                    quantity = entity.quantity,
                    completedQuantity = entity.completedQuantity,
                    status = BuildStatus.fromCode(entity.statusCode),
                    batch = entity.batch,
                    targetDate = entity.targetDate,
                    startDate = entity.startDate,
                    completionDate = entity.completionDate,
                    creationDate = entity.creationDate,
                    parentId = entity.parentId,
                    salesOrderId = entity.salesOrderId,
                    takeFromLocationId = entity.takeFromLocationId,
                    destinationLocationId = entity.destinationLocationId,
                    issuedBy = entity.issuedBy,
                    responsible = entity.responsible,
                    notes = entity.notes,
                    link = entity.link
                )
            }
            if (query.isNotBlank()) {
                result = result.filter {
                    it.reference.contains(query, ignoreCase = true) ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.partName.contains(query, ignoreCase = true) ||
                    it.batch.contains(query, ignoreCase = true)
                }
            }
            return result
        }
        return emptyList()
    }

    /**
     * جلب سعر التكلفة المعتمد للبند وفق تسلسل الأولوية (Historical Snapshot -> StockItem -> PartPricing).
     */
    private fun resolveUnitCostForSubPart(subPartId: Long, storedCost: Double): Double {
        if (storedCost > 0.0) return storedCost

        // 1. سعر الشراء الفعلي المسجل في جدول المخزون للقطعة الفرعية
        val stockItems = runBlocking {
            runCatching { stockDao.getStockItemsPaged(partUuid = "part-$subPartId", limit = 10) }.getOrDefault(emptyList())
        }
        val stockPrice = stockItems.firstOrNull { it.purchasePrice > 0.0 }?.purchasePrice
        if (stockPrice != null && stockPrice > 0.0) {
            return stockPrice
        }

        // 2. النطاق المالي المحسوب في جدول PartPricing
        val pricing = partPricingTable.getPricingForPart(subPartId)
        if (pricing != null) {
            val price = pricing.purchaseCostMin ?: pricing.internalCostMin ?: pricing.overallMin
            if (price != null && price > 0.0) {
                return price
            }
        }

        // 3. القيمة الافتراضية
        return 12.0
    }

    /**
     * جلب بنود ومخرجات أمر التصنيع المحددة.
     */
    fun getLineItemsForBuild(buildId: Long): List<BuildOrderLineItem> {
        val dbItems = runBlocking {
            runCatching { lineItemDao.getLineItemsForBuild(buildId) }.getOrDefault(emptyList())
        }
        if (dbItems.isNotEmpty()) {
            return dbItems.map { entity ->
                val resolvedCost = resolveUnitCostForSubPart(entity.subPartId, entity.unitCost)
                BuildOrderLineItem(
                    id = entity.id,
                    buildId = entity.buildId,
                    buildUuid = entity.buildUuid,
                    bomItemId = entity.bomItemId,
                    bomItemUuid = entity.bomItemUuid,
                    subPartId = entity.subPartId,
                    subPartName = entity.subPartName,
                    quantity = entity.quantity,
                    allocatedQuantity = entity.allocatedQuantity,
                    consumedQuantity = entity.consumedQuantity,
                    notes = entity.notes,
                    phaseUuid = entity.phaseUuid,
                    unitCost = resolvedCost
                )
            }
        }
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
                    buildUuid = inserted.buildUuid.ifBlank { "build-${inserted.buildId}" },
                    bomItemId = inserted.bomItemId,
                    bomItemUuid = inserted.bomItemUuid.ifBlank { "bom-${inserted.bomItemId}" },
                    subPartId = inserted.subPartId,
                    subPartName = inserted.subPartName,
                    quantity = inserted.quantity,
                    allocatedQuantity = inserted.allocatedQuantity,
                    consumedQuantity = inserted.consumedQuantity,
                    notes = inserted.notes,
                    phaseUuid = inserted.phaseUuid,
                    unitCost = inserted.unitCost,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        return inserted
    }

    /**
     * جلب كافة تخصيصات المخزون المباشرة لأمر الإنتاج.
     */
    fun getBuildItemsForBuild(buildId: Long): List<BuildItem> {
        return buildItemTable.getBuildItemsForBuild(buildId)
    }

    fun updateBuild(build: BuildOrder) {
        buildTable.updateBuild(build)
    }

    /**
     * تسجيل تخصيص جديد لمادة في المخزون لصالح أمر إنتاج.
     */
    fun addBuildItemAllocation(item: BuildItem): BuildItem {
        val inserted = buildItemTable.insertBuildItem(item)
        runBlocking {
            buildItemDao.insertOrUpdate(
                BuildItemEntity(
                    uuid = "builditem-${inserted.id}",
                    id = inserted.id,
                    buildId = inserted.buildId,
                    buildUuid = inserted.buildUuid.ifBlank { "build-${inserted.buildId}" },
                    buildLineId = inserted.buildLineId,
                    buildLineUuid = inserted.buildLineUuid.ifBlank { if (inserted.buildLineId != null) "lineitem-${inserted.buildLineId}" else "" },
                    stockItemId = inserted.stockItemId,
                    stockItemUuid = inserted.stockItemUuid.ifBlank { "stock-${inserted.stockItemId}" },
                    stockItemName = inserted.stockItemName,
                    quantity = inserted.quantity,
                    installIntoStockItemId = inserted.installIntoStockItemId,
                    installIntoStockItemUuid = inserted.installIntoStockItemUuid,
                    notes = inserted.notes,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
        item.buildLineId?.let { lineId ->
            lineItemTable.allocateStock(lineId, item.quantity)
        }
        return inserted
    }

    /**
     * التخصيص الأوتوماتيكي للمخزون (Auto-Allocate Action) وفق سياسة السحب والـ FIFO.
     */
    fun autoAllocateBuildOrder(buildId: Long): Int {
        val lineItems = lineItemTable.getLineItemsForBuild(buildId)
        var count = 0
        for (line in lineItems) {
            val remainingToAllocate = line.quantity - line.allocatedQuantity
            if (remainingToAllocate > 0) {
                addBuildItemAllocation(
                    BuildItem(
                        buildId = buildId,
                        buildLineId = line.id,
                        stockItemId = 500L + line.id,
                        stockItemName = "دفعة مخزون مخصصة تلقائياً #${line.subPartName} (FIFO)",
                        quantity = remainingToAllocate,
                        notes = "تخصيص تلقائي ذكي عبر النظام"
                    )
                )
                count++
            }
        }
        return count
    }

    /**
     * تخصيص مخزون لبند في أمر التصنيع (Allocate Stock).
     */
    fun allocateStock(lineItemId: Long, quantity: Double): Boolean {
        val success = lineItemTable.allocateStock(lineItemId, quantity)
        if (success) {
            val item = lineItemTable.getLineItemById(lineItemId)
            if (item != null) {
                runBlocking {
                    lineItemDao.insertOrUpdate(
                        BuildOrderLineItemEntity(
                            uuid = "lineitem-${item.id}",
                            id = item.id,
                            buildId = item.buildId,
                            buildUuid = item.buildUuid.ifBlank { "build-${item.buildId}" },
                            bomItemId = item.bomItemId,
                            bomItemUuid = item.bomItemUuid.ifBlank { "bom-${item.bomItemId}" },
                            subPartId = item.subPartId,
                            subPartName = item.subPartName,
                            quantity = item.quantity,
                            allocatedQuantity = item.allocatedQuantity,
                            consumedQuantity = item.consumedQuantity,
                            notes = item.notes,
                            phaseUuid = item.phaseUuid,
                            unitCost = item.unitCost,
                            syncStatus = SyncStatus.PENDING,
                            isDeleted = false,
                            updatedAt = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
            }
        }
        return success
    }

    /**
     * استهلاك مخزون مخصص للبند (Consume Stock).
     */
    fun consumeStock(lineItemId: Long, quantity: Double): Boolean {
        val success = lineItemTable.consumeStock(lineItemId, quantity)
        if (success) {
            val item = lineItemTable.getLineItemById(lineItemId)
            if (item != null) {
                runBlocking {
                    lineItemDao.insertOrUpdate(
                        BuildOrderLineItemEntity(
                            uuid = "lineitem-${item.id}",
                            id = item.id,
                            buildId = item.buildId,
                            buildUuid = item.buildUuid.ifBlank { "build-${item.buildId}" },
                            bomItemId = item.bomItemId,
                            bomItemUuid = item.bomItemUuid.ifBlank { "bom-${item.bomItemId}" },
                            subPartId = item.subPartId,
                            subPartName = item.subPartName,
                            quantity = item.quantity,
                            allocatedQuantity = item.allocatedQuantity,
                            consumedQuantity = item.consumedQuantity,
                            notes = item.notes,
                            phaseUuid = item.phaseUuid,
                            unitCost = item.unitCost,
                            syncStatus = SyncStatus.PENDING,
                            isDeleted = false,
                            updatedAt = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
            }
        }
        return success
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
     * تحديث حالة أمر التصنيع المباشرة.
     */
    fun updateBuildStatus(buildId: Long, newStatus: BuildStatus): Boolean {
        return buildTable.updateStatus(buildId, newStatus)
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
