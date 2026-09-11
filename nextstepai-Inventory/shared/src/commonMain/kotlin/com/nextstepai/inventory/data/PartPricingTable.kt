package com.nextstepai.inventory.data

import com.nextstepai.inventory.data.db.PartPricingEntity

/**
 * إدارة وتخزين جدول حساب وتتبع تكاليف وأسعار القطع (PartPricingTable) في الذاكرة مع خوارزميات التلخيص وإعادة الحساب.
 */
class PartPricingTable(
    private val partTable: PartTable = PartTable(),
    private val bomItemTable: BomItemTable = BomItemTable(),
    private val internalPriceTable: PartInternalPriceTable = PartInternalPriceTable(partTable)
) {
    private val pricingList = mutableListOf<PartPricingEntity>()
    private var nextId = 1L

    init {
        seedSamplePricing()
    }

    private fun seedSamplePricing() {
        val allParts = partTable.getAllParts()
        for (part in allParts) {
            val bomItems = bomItemTable.getBomItemsForPart(part.id)
            recalculatePricingForPart(part, bomItems)
        }
    }

    /**
     * إعادة حساب وتحديث التكاليف المجمعة لقطعة معينة (Pricing Update Engine):
     * 1. حساب نطاق أسعار الشراء (purchase_cost_min/max) إذا كانت القطعة purchaseable.
     * 2. حساب نطاق تكلفة المواد (bom_cost_min/max) إذا كانت القطعة assembly بناءً على عناصر الـ BOM.
     * 3. جلب أدنى وأقصى سعر بيع داخلي من PartInternalPrice (internal_cost_min/max).
     * 4. حساب النطاق الإجمالي النهائي (overall_min/max) بجمع ومقارنة جميع المصادر المتاحة.
     */
    fun recalculatePricingForPart(
        part: Part,
        bomItems: List<BomItem> = emptyList(),
        currentTimestamp: String = "2025-02-15 15:00:00"
    ): PartPricingEntity {
        var purchaseMin: Double? = null
        var purchaseMax: Double? = null

        if (part.purchaseable) {
            // تقدير نطاق تكلفة الشراء المبدئي بناءً على الحد الأدنى المتاح
            purchaseMin = if (part.minimumStock > 0) part.minimumStock * 0.05 else 1.25
            purchaseMax = purchaseMin * 1.35
        }

        var bomMin: Double? = null
        var bomMax: Double? = null

        if (part.assembly && bomItems.isNotEmpty()) {
            val totalQty = bomItems.sumOf { it.quantity }
            bomMin = totalQty * 2.50
            bomMax = totalQty * 3.80
        }

        val (internalMin, internalMax) = internalPriceTable.getInternalPriceRangeForPart(part.id)

        val allMins = listOfNotNull(purchaseMin, bomMin, internalMin)
        val allMaxs = listOfNotNull(purchaseMax, bomMax, internalMax)

        val overallMin = if (allMins.isNotEmpty()) allMins.minOrNull() else 0.0
        val overallMax = if (allMaxs.isNotEmpty()) allMaxs.maxOrNull() else overallMin

        val existingIndex = pricingList.indexOfFirst { it.partId == part.id }
        val record = PartPricingEntity(
            id = if (existingIndex != -1) pricingList[existingIndex].id else nextId++,
            partId = part.id,
            currency = "USD",
            overallMin = overallMin,
            overallMax = overallMax,
            purchaseCostMin = purchaseMin,
            purchaseCostMax = purchaseMax,
            bomCostMin = bomMin,
            bomCostMax = bomMax,
            internalCostMin = internalMin,
            internalCostMax = internalMax,
            updatedAt = currentTimestamp
        )

        if (existingIndex != -1) {
            pricingList[existingIndex] = record
        } else {
            pricingList.add(record)
        }

        return record
    }

    /**
     * جلب سجل التسعير والتكاليف الخاص بقطعة محددة.
     */
    fun getPricingForPart(partId: Long): PartPricingEntity? {
        return pricingList.find { it.partId == partId }
    }

    /**
     * الحذف المتتابع (CASCADE Delete) لسجل تسعير القطعة عند مسحها من النظام.
     */
    fun cascadeDeleteForPart(partId: Long) {
        pricingList.removeIf { it.partId == partId }
    }
}
