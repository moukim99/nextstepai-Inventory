package com.nextstepai.inventory.data

/**
 * تمثيل بند ومخرجات أمر التصنيع (BuildOrderLineItem) المستوحى من نظام InvenTree.
 * يربط أمر الإنتاج الرئيسي ببنود الـ BOM لحساب الكميات المطلوبة والمحجوزة والمستهلكة.
 *
 * @property id المعرف الرقمي الفريد للبند
 * @property buildId معرف أمر التصنيع الرئيسي التابع له (ForeignKey -> BuildOrder)
 * @property bomItemId معرف المكون المحدد في قائمة مواد التصنيع (ForeignKey -> BomItem)
 * @property subPartId معرف القطعة المكونة المطلوبة
 * @property subPartName اسم القطعة المكونة
 * @property quantity الكمية الإجمالية المطلوبة لتغطية أمر التصنيع كاملاً (build.quantity * bom_item.quantity)
 * @property allocatedQuantity الكمية المحجوزة والمخصصة فعلياً من المخزون لهذا البند
 * @property consumedQuantity الكمية التي تم استهلاكها وحرقها فعلياً أثناء عمليات التجميع
 * @property notes ملاحظات تشغيلية خاصة بالبند
 */
data class BuildOrderLineItem(
    val id: Long = 0L,
    val buildId: Long,
    val bomItemId: Long,
    val subPartId: Long = 0L,
    val subPartName: String = "",
    val quantity: Double = 1.0,
    val allocatedQuantity: Double = 0.0,
    val consumedQuantity: Double = 0.0,
    val notes: String = ""
) {
    /**
     * نسبة التخصيص المحجوزة من المخزون للبند.
     */
    val allocationPercentage: Float
        get() = if (quantity > 0) ((allocatedQuantity / quantity) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f

    /**
     * هل تم استيفاء حجز وتخصيص كافة الكمية المطلوبة للبند.
     */
    val isFullyAllocated: Boolean
        get() = allocatedQuantity >= quantity

    /**
     * هل تم استهلاك كافة الكمية المخصصة للبند.
     */
    val isFullyConsumed: Boolean
        get() = consumedQuantity >= quantity
}

/**
 * محاكاة جدول بنود أوامر الإنتاج والتصنيع (Build Order Line Items Table).
 */
class BuildOrderLineItemTable {
    private val lineItems = mutableListOf<BuildOrderLineItem>()
    private var nextLineItemId = 1L

    init {
        seedSampleLineItems()
    }

    private fun seedSampleLineItems() {
        insertLineItem(
            BuildOrderLineItem(
                buildId = 1L,
                bomItemId = 101L,
                subPartId = 10L,
                subPartName = "حساس الحرارة DHT22 Sensor",
                quantity = 50.0,
                allocatedQuantity = 50.0,
                consumedQuantity = 20.0,
                notes = "سحب الدفعة رقم BATCH-DHT-01"
            )
        )
        insertLineItem(
            BuildOrderLineItem(
                buildId = 1L,
                bomItemId = 102L,
                subPartId = 11L,
                subPartName = "مقاومة 10k Ohm",
                quantity = 50.0,
                allocatedQuantity = 50.0,
                consumedQuantity = 20.0,
                notes = "تركيب ثطحي SMD"
            )
        )
    }

    fun insertLineItem(item: BuildOrderLineItem): BuildOrderLineItem {
        val newItem = item.copy(
            id = if (item.id == 0L) nextLineItemId++ else item.id
        )
        lineItems.add(newItem)
        return newItem
    }

    fun getLineItemsForBuild(buildId: Long): List<BuildOrderLineItem> {
        return lineItems.filter { it.buildId == buildId }
    }

    fun allocateStock(lineItemId: Long, quantity: Double): Boolean {
        require(quantity > 0) { "كمية التخصيص يجب أن تكون أكبر من الصفر" }
        val index = lineItems.indexOfFirst { it.id == lineItemId }
        if (index != -1) {
            val current = lineItems[index]
            val newAllocated = (current.allocatedQuantity + quantity).coerceAtMost(current.quantity * 1.2) // سماحية الهدر حتى 20%
            lineItems[index] = current.copy(allocatedQuantity = newAllocated)
            return true
        }
        return false
    }

    fun consumeStock(lineItemId: Long, quantity: Double): Boolean {
        require(quantity > 0) { "كمية الاستهلاك يجب أن تكون أكبر من الصفر" }
        val index = lineItems.indexOfFirst { it.id == lineItemId }
        if (index != -1) {
            val current = lineItems[index]
            val newConsumed = (current.consumedQuantity + quantity).coerceAtMost(current.allocatedQuantity)
            lineItems[index] = current.copy(consumedQuantity = newConsumed)
            return true
        }
        return false
    }
}
