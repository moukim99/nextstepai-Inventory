package com.nextstepai.inventory.data

import com.nextstepai.inventory.data.db.PartInternalPriceEntity

/**
 * إدارة وتخزين شرائح أسعار البيع والتكلفة الداخلية (PartInternalPriceTable) في الذاكرة مع الشروط والقيود المنطقية.
 */
@Deprecated("Legacy in-memory storage table. Scheduled for migration to SQLite DAOs.")
class PartInternalPriceTable(
    private val partTable: PartTable = PartTable()
) {
    private val internalPrices = mutableListOf<PartInternalPriceEntity>()
    private var nextId = 1L

    init {
        seedSampleInternalPrices()
    }

    private fun seedSampleInternalPrices() {
        val allParts = partTable.getAllParts()
        if (allParts.isNotEmpty()) {
            val part = allParts.first()
            insertInternalPrice(partId = part.id, quantity = 1.0, price = 15.00, currency = "USD")
            insertInternalPrice(partId = part.id, quantity = 10.0, price = 12.50, currency = "USD")
            insertInternalPrice(partId = part.id, quantity = 100.0, price = 9.90, currency = "USD")
        }
    }

    /**
     * إدراج شريحة سعرية جديدة للقطعة مع تطبيق القيود:
     * 1. الكمية أكبر من أو تساوي 1 (quantity >= 1.0).
     * 2. السعر أكبر من صفر (price > 0.0).
     * 3. قيد التفرد المركب (unique_together = ['part', 'quantity']).
     */
    fun insertInternalPrice(
        partId: Long,
        quantity: Double,
        price: Double,
        currency: String = "USD"
    ): PartInternalPriceEntity {
        require(quantity >= 1.0) {
            "الحد الأدنى للكمية المؤهلة لتطبيق الشريحة يجب أن تكون قيمة أكبر من أو تساوي 1."
        }

        require(price > 0.0) {
            "سعر الوحدة الواحدة لشريحة الكمية يجب أن يكون أكبر من صفر."
        }

        val exists = internalPrices.any { it.partId == partId && it.quantity == quantity }
        require(!exists) {
            "شريحة سعرية بنفس الكمية ($quantity) مضافة بالفعل لهذه القطعة! (قيد التفرد unique_together مُفعّل)"
        }

        val record = PartInternalPriceEntity(
            id = nextId++,
            partId = partId,
            quantity = quantity,
            price = price,
            currency = currency.ifBlank { "USD" }
        )
        internalPrices.add(record)
        return record
    }

    /**
     * جلب شرائح الأسعار الداخلية لقطعة معينة مرتبة تصاعدياً حسب الكمية.
     */
    fun getInternalPricesForPart(partId: Long): List<PartInternalPriceEntity> {
        return internalPrices.filter { it.partId == partId }.sortedBy { it.quantity }
    }

    /**
     * حساب أدنى وأقصى سعر بيع داخلي مسجل للقطعة لتغذية PartPricing.
     */
    fun getInternalPriceRangeForPart(partId: Long): Pair<Double?, Double?> {
        val prices = getInternalPricesForPart(partId).map { it.price }
        if (prices.isEmpty()) return Pair(null, null)
        return Pair(prices.minOrNull(), prices.maxOrNull())
    }

    /**
     * حذف شريحة سعرية محددة.
     */
    fun deleteInternalPrice(id: Long): Boolean {
        return internalPrices.removeIf { it.id == id }
    }

    /**
     * الحذف المتتابع (CASCADE Delete) لشرائح أسعار القطعة عند مسحها من النظام.
     */
    fun cascadeDeleteForPart(partId: Long) {
        internalPrices.removeIf { it.partId == partId }
    }
}
