package com.nextstepai.inventory.data

/**
 * حالة أمر البيع للعميل (Sales Order Status).
 */
enum class SOStatus(val code: Int, val label: String) {
    PENDING(10, "مسودة (Pending)"),
    APPROVED(20, "معتمد (Approved)"),
    IN_FULFILLMENT(30, "قيد التجهيز والتلبية (In Fulfillment)"),
    COMPLETE(40, "مكتمل ومشحون (Complete)"),
    CANCELLED(50, "ملغي (Cancelled)");

    companion object {
        fun fromCode(code: Int): SOStatus = entries.find { it.code == code } ?: PENDING
    }
}

/**
 * تمثيل بند أمر البيع (SalesOrderLineItem).
 */
data class SalesOrderLineItem(
    val id: Long = 0L,
    val orderId: Long = 0L,
    val orderUuid: String = "",
    val partId: Long = 0L,
    val partUuid: String = "",
    val partName: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val allocatedQuantity: Double = 0.0,
    val shippedQuantity: Double = 0.0,
    val notes: String = ""
) {
    val lineTotal: Double
        get() = quantity * unitPrice

    val isFullyAllocated: Boolean
        get() = allocatedQuantity >= quantity

    val isFullyShipped: Boolean
        get() = shippedQuantity >= quantity
}

/**
 * تمثيل وثيقة أمر البيع للعميل (SalesOrder).
 */
data class SalesOrder(
    val id: Long = 0L,
    val uuid: String = "",
    val reference: String,
    val customerId: Long = 0L,
    val customerUuid: String = "",
    val customerName: String = "",
    val status: SOStatus = SOStatus.PENDING,
    val description: String = "",
    val orderCurrency: String = "USD",
    val targetDate: String = "",
    val sourceType: String = "MANUAL",
    val sourceReferenceUuid: String? = null,
    val notes: String = "",
    val lineItems: List<SalesOrderLineItem> = emptyList()
) {
    val totalPrice: Double
        get() = lineItems.sumOf { it.lineTotal }

    val lineItemsCount: Int
        get() = lineItems.size
}

/**
 * إدارة وتخزين أوامر البيع وبنودها في الذاكرة ومحاكاتها.
 */
class SalesOrderTable {
    private val orders = mutableListOf<SalesOrder>()
    private val lineItems = mutableListOf<SalesOrderLineItem>()
    private var nextOrderId = 1L
    private var nextLineId = 1L

    init {
        seedSampleOrders()
    }

    private fun seedSampleOrders() {
        val so1 = insertOrder(
            SalesOrder(
                reference = "SO-2025-101",
                customerId = 101L,
                customerName = "شركة التقنيات المتقدمة",
                description = "طلب توريد لوحات تحكم رئيسية v2 والمستشعرات",
                status = SOStatus.APPROVED,
                targetDate = "2025-03-30",
                orderCurrency = "USD"
            )
        )

        insertLineItem(
            SalesOrderLineItem(
                orderId = so1.id,
                partId = 4L,
                partName = "لوحة التحكم الرئيسية Industrial Mainboard v2",
                quantity = 20.0,
                unitPrice = 110.0,
                allocatedQuantity = 20.0
            )
        )

        insertLineItem(
            SalesOrderLineItem(
                orderId = so1.id,
                partId = 3L,
                partName = "قالب مستشعر الحرارة والرطوبة",
                quantity = 50.0,
                unitPrice = 15.0,
                allocatedQuantity = 30.0
            )
        )
    }

    fun insertOrder(order: SalesOrder): SalesOrder {
        require(order.reference.isNotBlank()) { "الرمز المرجعي لأمر البيع إلزامي" }
        val newOrder = order.copy(
            id = if (order.id == 0L) nextOrderId++ else order.id,
            uuid = if (order.uuid.isBlank()) "so-${order.id.takeIf { it != 0L } ?: nextOrderId}" else order.uuid
        )
        orders.add(newOrder)
        return newOrder
    }

    fun insertLineItem(item: SalesOrderLineItem): SalesOrderLineItem {
        val newLine = item.copy(
            id = if (item.id == 0L) nextLineId++ else item.id,
            orderUuid = if (item.orderUuid.isBlank()) "so-${item.orderId}" else item.orderUuid
        )
        lineItems.add(newLine)
        updateOrderLineItems(item.orderId)
        return newLine
    }

    private fun updateOrderLineItems(orderId: Long) {
        val orderIdx = orders.indexOfFirst { it.id == orderId }
        if (orderIdx != -1) {
            val itemsForOrder = lineItems.filter { it.orderId == orderId }
            orders[orderIdx] = orders[orderIdx].copy(lineItems = itemsForOrder)
        }
    }

    fun getAllOrders(): List<SalesOrder> = orders.toList()
}
