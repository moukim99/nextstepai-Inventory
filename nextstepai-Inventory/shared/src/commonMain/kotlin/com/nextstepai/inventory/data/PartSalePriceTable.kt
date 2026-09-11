package com.nextstepai.inventory.data

import com.nextstepai.inventory.data.db.PartSalePriceEntity

/**
 * إدارة وتخزين شرائح أسعار البيع للعملاء (PartSalePriceTable) في الذاكرة مع شرط القابلية للبيع والقواعد المنطقية.
 */
class PartSalePriceTable(
    private val partTable: PartTable = PartTable()
) {
    private val salePrices = mutableListOf<PartSalePriceEntity>()
    private var nextId = 1L

    init {
        seedSampleSalePrices()
    }

    private fun seedSampleSalePrices() {
        val allParts = partTable.getAllParts()
        // التأكد من وجود قطعة قابلة للبيع لاحتضان عينات الأسعار
        val salablePart = allParts.firstOrNull { it.salable } ?: run {
            val created = partTable.insertPart(
                Part(
                    name = "منتج جاهز للبيع",
                    salable = true,
                    minimumStock = 10.0
                )
            )
            created
        }

        insertSalePrice(part = salablePart, quantity = 1.0, price = 49.99, currency = "USD")
        insertSalePrice(part = salablePart, quantity = 10.0, price = 42.50, currency = "USD")
        insertSalePrice(part = salablePart, quantity = 50.0, price = 35.00, currency = "USD")
    }

    /**
     * إدراج شريحة سعر بيع للعملاء مع تطبيق القيود:
     * 1. القطعة قابلة للبيع (part.salable == true).
     * 2. الكمية أكبر من أو تساوي 1 (quantity >= 1.0).
     * 3. السعر أكبر من صفر (price > 0.0).
     * 4. قيد التفرد المركب (unique_together = ['part', 'quantity']).
     */
    fun insertSalePrice(
        part: Part,
        quantity: Double,
        price: Double,
        currency: String = "USD"
    ): PartSalePriceEntity {
        require(part.salable) {
            "شرائح أسعار البيع للعملاء مخصصة فقط للقطع القابلة للبيع (salable = true)."
        }

        require(quantity >= 1.0) {
            "الحد الأدنى للكمية المؤهلة لتطبيق الشريحة يجب أن يكون أكبر من أو يساوي 1."
        }

        require(price > 0.0) {
            "سعر بيع الوحدة الواحدة لشريحة الكمية يجب أن يكون أكبر من صفر."
        }

        val exists = salePrices.any { it.partId == part.id && it.quantity == quantity }
        require(!exists) {
            "شريحة سعر بيع بنفس الكمية ($quantity) مضافة بالفعل لهذه القطعة! (قيد التفرد unique_together مُفعّل)"
        }

        val record = PartSalePriceEntity(
            id = nextId++,
            partId = part.id,
            quantity = quantity,
            price = price,
            currency = currency.ifBlank { "USD" }
        )
        salePrices.add(record)
        return record
    }

    /**
     * جلب شرائح أسعار البيع لقطعة معينة مرتبة تصاعدياً حسب الكمية.
     */
    fun getSalePricesForPart(partId: Long): List<PartSalePriceEntity> {
        return salePrices.filter { it.partId == partId }.sortedBy { it.quantity }
    }

    /**
     * تحديد أنسب وأفضل شريحة سعر بيع لطلب عميل بناءً على الكمية المطلوبة.
     */
    fun getBestSalePriceForQuantity(partId: Long, quantity: Double): PartSalePriceEntity? {
        return getSalePricesForPart(partId)
            .filter { it.quantity <= quantity }
            .maxByOrNull { it.quantity }
    }

    /**
     * حذف شريحة سعر بيع محددة.
     */
    fun deleteSalePrice(id: Long): Boolean {
        return salePrices.removeIf { it.id == id }
    }

    /**
     * الحذف المتتابع (CASCADE Delete) لشرائح أسعار البيع عند مسح القطعة من النظام.
     */
    fun cascadeDeleteForPart(partId: Long) {
        salePrices.removeIf { it.partId == partId }
    }
}
