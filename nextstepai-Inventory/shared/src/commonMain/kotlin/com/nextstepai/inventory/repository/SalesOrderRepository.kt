package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.BuildOrder
import com.nextstepai.inventory.data.BuildOrderLineItem
import com.nextstepai.inventory.data.BuildStatus
import com.nextstepai.inventory.data.POStatus
import com.nextstepai.inventory.data.PurchaseOrder
import com.nextstepai.inventory.data.PurchaseOrderLineItem
import com.nextstepai.inventory.data.SOStatus
import com.nextstepai.inventory.data.SalesOrder
import com.nextstepai.inventory.data.SalesOrderLineItem
import com.nextstepai.inventory.data.SalesOrderTable
import com.nextstepai.inventory.data.db.SalesOrderDao
import com.nextstepai.inventory.data.db.SalesOrderEntity
import com.nextstepai.inventory.data.db.SalesOrderLineEntity
import com.nextstepai.inventory.sync.SyncStatus
import kotlinx.coroutines.runBlocking

/**
 * نتيجة تشغيل محرك تلبية الطلبات (Order Fulfillment Engine Result).
 */
data class SalesOrderFulfillmentResult(
    val salesOrder: SalesOrder,
    val buildOrdersCreated: List<BuildOrder> = emptyList(),
    val purchaseOrdersCreated: List<PurchaseOrder> = emptyList(),
    val allocatedStockItemsCount: Int = 0,
    val summaryMessage: String = ""
)

/**
 * المستودع المسؤول عن المبيعات ومحرك تلبية الطلبات (Order Fulfillment Engine).
 */
class SalesOrderRepository(
    private val salesOrderTable: SalesOrderTable = SalesOrderTable(),
    private val salesOrderDao: SalesOrderDao = SalesOrderDao(),
    private val partRepository: PartRepository = PartRepository(),
    private val buildOrderRepository: BuildOrderRepository = BuildOrderRepository(),
    private val purchaseOrderRepository: PurchaseOrderRepository = PurchaseOrderRepository()
) {

    fun searchOrders(
        query: String = "",
        customerId: Long? = null,
        status: SOStatus? = null
    ): List<SalesOrder> {
        val entities = runBlocking {
            salesOrderDao.getOrdersPaged(
                customerId = customerId,
                statusCode = status?.code,
                limit = 500,
                offset = 0
            )
        }
        if (entities.isNotEmpty()) {
            val allTableOrders = salesOrderTable.getAllOrders()
            var result = entities.mapIndexed { index, entity ->
                val matchingTableOrder = allTableOrders.find { it.reference.equals(entity.reference, ignoreCase = true) }
                val numericId = matchingTableOrder?.id ?: (index + 1L)
                val lineItems = matchingTableOrder?.lineItems ?: emptyList()
                SalesOrder(
                    id = numericId,
                    uuid = entity.uuid,
                    reference = entity.reference,
                    customerId = entity.customerId,
                    customerUuid = entity.customerUuid,
                    customerName = entity.customerName,
                    status = SOStatus.fromCode(entity.statusCode),
                    description = entity.description,
                    orderCurrency = entity.orderCurrency,
                    targetDate = entity.targetDate,
                    notes = entity.notes,
                    lineItems = lineItems
                )
            }
            if (query.isNotBlank()) {
                result = result.filter {
                    it.reference.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.customerName.contains(query, ignoreCase = true)
                }
            }
            return result
        }
        return salesOrderTable.getAllOrders()
    }

    /**
     * حفظ أمر البيع وتفعيل محرك اتخاذ القرار التلقائي (Order Fulfillment Decision Flow).
     */
    fun addSalesOrderWithFulfillment(
        order: SalesOrder,
        autoFulfill: Boolean = true
    ): SalesOrderFulfillmentResult {
        val inserted = salesOrderTable.insertOrder(order)
        order.lineItems.forEach { line ->
            salesOrderTable.insertLineItem(line.copy(orderId = inserted.id))
        }

        runBlocking {
            salesOrderDao.insertOrUpdateOrder(
                SalesOrderEntity(
                    uuid = "so-${inserted.id}",
                    reference = inserted.reference,
                    customerId = inserted.customerId,
                    customerUuid = inserted.customerUuid.ifBlank { "cust-${inserted.customerId}" },
                    customerName = inserted.customerName,
                    statusCode = inserted.status.code,
                    description = inserted.description,
                    orderCurrency = inserted.orderCurrency,
                    targetDate = inserted.targetDate,
                    totalPrice = inserted.totalPrice,
                    notes = inserted.notes,
                    syncStatus = SyncStatus.PENDING
                )
            )
            inserted.lineItems.forEach { line ->
                salesOrderDao.insertOrUpdateLine(
                    SalesOrderLineEntity(
                        uuid = "so-line-${line.id}",
                        orderUuid = "so-${inserted.id}",
                        orderId = inserted.id,
                        partId = line.partId,
                        partName = line.partName,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        allocatedQuantity = line.allocatedQuantity,
                        shippedQuantity = line.shippedQuantity,
                        notes = line.notes,
                        syncStatus = SyncStatus.PENDING
                    )
                )
            }
        }

        if (!autoFulfill) {
            return SalesOrderFulfillmentResult(
                salesOrder = inserted,
                summaryMessage = "تم حفظ أمر البيع '${inserted.reference}' بدون تفعيل محرك التلبية الآلي."
            )
        }

        // ================= محرك اتخاذ القرار والربط التلقائي (Fulfillment Engine) =================
        val createdBuildOrders = mutableListOf<BuildOrder>()
        val createdPurchaseOrders = mutableListOf<PurchaseOrder>()
        var allocatedCount = 0

        val allParts = partRepository.getParts()

        inserted.lineItems.forEach { line ->
            val part = allParts.find { it.id == line.partId }
            val inStock = part?.totalInStock ?: 0.0
            val shortage = (line.quantity - inStock).coerceAtLeast(0.0)

            if (part != null && part.assembly && shortage > 0.0) {
                // 1. منتج يُصنع داخلياً وبه عجز ➔ إنشاء أمر تصنيع BuildOrder
                val boRef = "BO-SO-${inserted.reference.removePrefix("SO-")}"
                val buildOrder = BuildOrder(
                    reference = boRef,
                    title = "إنتاج تلبية طلب العميل ${inserted.customerName}",
                    partId = part.id,
                    partName = part.name,
                    quantity = shortage,
                    status = BuildStatus.PENDING,
                    batch = "BATCH-${boRef}",
                    targetDate = inserted.targetDate,
                    salesOrderId = inserted.id,
                    issuedBy = "محرك التلبية التلقائي",
                    responsible = "خط الإنتاج المباشر",
                    notes = "أمر بناء آلي ناتج عن أمر البيع #${inserted.reference}"
                )
                val createdBo = buildOrderRepository.addBuildOrder(buildOrder)
                createdBuildOrders.add(createdBo)

                // فحص مواد الـ BOM العجز التابعة لأمر التصنيع المصنع حديثاً
                val bomShortageLines = buildOrderRepository.getLineItemsForBuild(createdBo.id)
                    .filter { it.quantity > it.allocatedQuantity }

                if (bomShortageLines.isNotEmpty()) {
                    val poRef = "PO-BO-${createdBo.reference.removePrefix("BO-")}"
                    val poLines = bomShortageLines.map { bomLine ->
                        PurchaseOrderLineItem(
                            orderId = 0L,
                            supplierPartId = bomLine.subPartId,
                            partName = bomLine.subPartName,
                            quantity = bomLine.quantity - bomLine.allocatedQuantity,
                            purchasePrice = bomLine.unitCost,
                            notes = "خامات لأمر التجميع #${createdBo.reference}"
                        )
                    }
                    val po = PurchaseOrder(
                        reference = poRef,
                        supplierId = part.defaultSupplierId ?: 1L,
                        supplierName = "المورد الافتراضي لخامات الإنتاج",
                        description = "أمر شراء مواد خام لمكونات أمر التجميع #${createdBo.reference}",
                        status = POStatus.PENDING,
                        orderCurrency = inserted.orderCurrency,
                        targetDate = inserted.targetDate,
                        sourceType = "BUILD_ORDER",
                        sourceReferenceUuid = "build-${createdBo.id}",
                        lineItems = poLines
                    )
                    createdPurchaseOrders.add(purchaseOrderRepository.addOrder(po))
                }

            } else if (part != null && !part.assembly && shortage > 0.0) {
                // 2. منتج يُشترى جاهزاً وبه عجز ➔ إنشاء أمر شراء PurchaseOrder بمصدر SALES_ORDER
                val poRef = "PO-SO-${inserted.reference.removePrefix("SO-")}"
                val poLine = PurchaseOrderLineItem(
                    orderId = 0L,
                    supplierPartId = part.id,
                    partName = part.name,
                    quantity = shortage,
                    purchasePrice = if (part.minimumStock > 0) part.minimumStock * 0.05 else 12.0,
                    notes = "شراء باك تو باك لتلبية أمر البيع #${inserted.reference}"
                )
                val po = PurchaseOrder(
                    reference = poRef,
                    supplierId = part.defaultSupplierId ?: 1L,
                    supplierName = "المورد المعتمد للقطعة ${part.name}",
                    description = "شراء مباشر للتغطية بطلب العميل ${inserted.customerName}",
                    status = POStatus.PENDING,
                    orderCurrency = inserted.orderCurrency,
                    targetDate = inserted.targetDate,
                    sourceType = "SALES_ORDER",
                    sourceReferenceUuid = "so-${inserted.id}",
                    lineItems = listOf(poLine)
                )
                createdPurchaseOrders.add(purchaseOrderRepository.addOrder(po))

            } else {
                // 3. الكمية متوفرة بالكامل بالمخزن ➔ حجز
                allocatedCount++
            }
        }

        val msg = StringBuilder("تم تسجيل أمر البيع '${inserted.reference}' بنجاح: ")
        if (createdBuildOrders.isNotEmpty()) msg.append("توليد ${createdBuildOrders.size} أوامر تصنيع، ")
        if (createdPurchaseOrders.isNotEmpty()) msg.append("توليد ${createdPurchaseOrders.size} أوامر شراء مباشرة، ")
        if (allocatedCount > 0) msg.append("تأمين ${allocatedCount} مواد من المخزون المتوفر.")

        return SalesOrderFulfillmentResult(
            salesOrder = inserted,
            buildOrdersCreated = createdBuildOrders,
            purchaseOrdersCreated = createdPurchaseOrders,
            allocatedStockItemsCount = allocatedCount,
            summaryMessage = msg.toString()
        )
    }
}
