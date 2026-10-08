package com.nextstepai.inventory.data

/**
 * حالة أمر الشراء التشغيلية (PurchaseOrder Status).
 */
enum class POStatus(val code: Int, val label: String) {
    PENDING(10, "مسودة (Pending)"),
    PLACED(20, "معتمد ومصدر (Placed)"),
    COMPLETE(30, "مكتمل ومستلم (Complete)"),
    CANCELLED(40, "ملغي (Cancelled)");

    companion object {
        fun fromCode(code: Int): POStatus = entries.find { it.code == code } ?: PENDING
    }
}

/**
 * تمثيل بند فردي داخل أمر الشراء (PurchaseOrderLineItem).
 */
data class PurchaseOrderLineItem(
    val id: Long = 0L,
    val orderId: Long,
    val supplierPartId: Long,
    val partName: String = "",
    val quantity: Double = 1.0,
    val receivedQuantity: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val targetDate: String = "",
    val destinationLocationId: Long? = null,
    val notes: String = ""
) {
    /**
     * التكلفة الإجمالية للبند (الكمية × سعر الشراء).
     */
    val lineTotal: Double
        get() = quantity * purchasePrice

    /**
     * هل البند مستوفى ومكتلم الاستلام بالكامل.
     */
    val isFullyReceived: Boolean
        get() = receivedQuantity >= quantity
}

/**
 * تمثيل وثيقة أمر الشراء الرئيسية (PurchaseOrder) المستوحاة من نظام InvenTree.
 */
data class PurchaseOrder(
    val id: Long = 0L,
    val reference: String,
    val supplierId: Long,
    val supplierName: String = "",
    val status: POStatus = POStatus.PENDING,
    val description: String = "",
    val creationDate: String = "2025-02-15",
    val createdByUserId: Long = 1L,
    val issueDate: String = "",
    val completeDate: String = "",
    val targetDate: String = "",
    val responsibleUserId: Long? = null,
    val orderCurrency: String = "USD",
    val destinationLocationId: Long? = null,
    val sourceType: String = "MANUAL",
    val sourceReferenceUuid: String? = null,
    val destinationLocationUuid: String? = null,
    val notes: String = "",
    val link: String = "",
    val lineItems: List<PurchaseOrderLineItem> = emptyList()
) {
    /**
     * التكلفة الإجمالية لأمر الشراء وتُحسب بجمع إجمالي جميع البنود.
     */
    val totalCost: Double
        get() = lineItems.sumOf { it.lineTotal }

    /**
     * إجمالي عدد البنود المسجلة داخل الأمر.
     */
    val lineItemsCount: Int
        get() = lineItems.size

    /**
     * هل الأمر معتمد أو مكتمل ومقفل للتعديل.
     */
    val isStateLocked: Boolean
        get() = status == POStatus.PLACED || status == POStatus.COMPLETE
}

/**
 * محاكاة جدول أوامر الشراء وبنودها (PurchaseOrder & LineItems Table).
 */
@Deprecated("Legacy in-memory storage table. Scheduled for migration to SQLite DAOs.")
class PurchaseOrderTable {
    private val orders = mutableListOf<PurchaseOrder>()
    private val lineItems = mutableListOf<PurchaseOrderLineItem>()
    private var nextOrderId = 1L
    private var nextLineId = 1L

    init {
        seedSampleOrders()
    }

    private fun seedSampleOrders() {
        val po1 = insertOrder(
            PurchaseOrder(
                reference = "PO-2025-001",
                supplierId = 1L,
                supplierName = "شركة التقنيات المتقدمة للتوريد",
                description = "توريد مكونات إلكترونية ومقومات للإنتاج",
                status = POStatus.PENDING,
                issueDate = "2025-02-10",
                targetDate = "2025-02-25",
                orderCurrency = "USD"
            )
        )

        insertLineItem(
            PurchaseOrderLineItem(
                orderId = po1.id,
                supplierPartId = 1L,
                partName = "مقاومة 10K Ohm",
                quantity = 500.0,
                receivedQuantity = 200.0,
                purchasePrice = 0.05,
                notes = "تغليف بكرات Reels"
            )
        )

        insertLineItem(
            PurchaseOrderLineItem(
                orderId = po1.id,
                supplierPartId = 2L,
                partName = "متحكم ESP32 Wi-Fi/BT",
                quantity = 50.0,
                receivedQuantity = 0.0,
                purchasePrice = 3.50,
                notes = "شحنة حاسمة للخط الرئيسي"
            )
        )

        updateOrderStatus(po1.id, POStatus.PLACED)
    }

    /**
     * إدراج أمر شراء جديد.
     */
    fun insertOrder(order: PurchaseOrder): PurchaseOrder {
        require(order.reference.isNotBlank()) { "الرمز المرجعي لأمر الشراء إلزامي" }
        val duplicateRef = orders.any { it.reference.equals(order.reference.trim(), ignoreCase = true) && it.id != order.id }
        require(!duplicateRef) { "الرمز المرجعي '${order.reference}' مستخدم بالفعل لأمر شراء آخر." }

        val newOrder = order.copy(
            id = if (order.id == 0L) nextOrderId++ else order.id
        )
        orders.add(newOrder)
        return newOrder
    }

    /**
     * إدراج بند جديد داخل أمر شراء.
     */
    fun insertLineItem(item: PurchaseOrderLineItem): PurchaseOrderLineItem {
        val parentOrder = orders.find { it.id == item.orderId }
        require(parentOrder != null) { "أمر الشراء المرتبط غير موجود" }
        require(!parentOrder.isStateLocked) { "لا يمكن إضافة بنود لأمر شراء معتمد أو مكتمل ومقفل." }

        val newLine = item.copy(
            id = if (item.id == 0L) nextLineId++ else item.id
        )
        lineItems.add(newLine)
        updateOrderLineItems(item.orderId)
        return newLine
    }

    /**
     * تسجيل استلام كمية مخزنية لبند محدد (Receive Line Item).
     */
    fun receiveLineItem(lineItemId: Long, receiveQty: Double): Boolean {
        require(receiveQty > 0) { "كمية الاستلام يجب أن تكون أكبر من الصفر" }
        val index = lineItems.indexOfFirst { it.id == lineItemId }
        if (index != -1) {
            val current = lineItems[index]
            val updated = current.copy(receivedQuantity = current.receivedQuantity + receiveQty)
            lineItems[index] = updated
            updateOrderLineItems(current.orderId)
            checkAutoOrderCompletion(current.orderId)
            return true
        }
        return false
    }

    /**
     * اعتمادات وتغيير حالة أمر الشراء (Placing / Completing / Cancelling).
     */
    fun updateOrderStatus(orderId: Long, newStatus: POStatus): Boolean {
        val index = orders.indexOfFirst { it.id == orderId }
        if (index != -1) {
            val current = orders[index]
            val updatedOrder = current.copy(
                status = newStatus,
                issueDate = if (newStatus == POStatus.PLACED && current.issueDate.isBlank()) "2025-02-15" else current.issueDate,
                completeDate = if (newStatus == POStatus.COMPLETE && current.completeDate.isBlank()) "2025-02-15" else current.completeDate
            )
            orders[index] = updatedOrder
            return true
        }
        return false
    }

    private fun updateOrderLineItems(orderId: Long) {
        val orderIndex = orders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            val itemsForOrder = lineItems.filter { it.orderId == orderId }
            orders[orderIndex] = orders[orderIndex].copy(lineItems = itemsForOrder)
        }
    }

    private fun checkAutoOrderCompletion(orderId: Long) {
        val itemsForOrder = lineItems.filter { it.orderId == orderId }
        if (itemsForOrder.isNotEmpty() && itemsForOrder.all { it.isFullyReceived }) {
            updateOrderStatus(orderId, POStatus.COMPLETE)
        }
    }

    /**
     * جلب كافة أوامر الشراء المتاحة مفلترة بحسب نص البحث والحالة والمورد.
     */
    fun searchOrders(
        query: String = "",
        supplierId: Long? = null,
        status: POStatus? = null
    ): List<PurchaseOrder> {
        val q = query.trim().lowercase()
        return orders.filter { order ->
            val matchesSupplier = supplierId == null || order.supplierId == supplierId
            val matchesStatus = status == null || order.status == status
            val matchesQuery = q.isEmpty() ||
                    order.reference.lowercase().contains(q) ||
                    order.supplierName.lowercase().contains(q) ||
                    order.description.lowercase().contains(q)

            matchesSupplier && matchesStatus && matchesQuery
        }
    }

    fun getAllOrders(): List<PurchaseOrder> = orders.toList()
}
