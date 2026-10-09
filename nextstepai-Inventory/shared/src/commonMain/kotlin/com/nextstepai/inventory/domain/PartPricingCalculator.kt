package com.nextstepai.inventory.domain

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.db.PartInternalPriceEntity
import com.nextstepai.inventory.data.db.PartPricingEntity
import kotlin.time.Clock

/**
 * محرك وقواعد تسعير وحساب تكلفة القطع (Part Pricing Domain Engine).
 * يفصل قواعد الأعمال والعمليات الحسابية عن مستودعات حفظ وتخزين البيانات.
 */
object PartPricingCalculator {

    const val DEFAULT_PURCHASE_PRICE_FALLBACK: Double = 1.25
    const val PURCHASE_PRICE_STOCK_FACTOR: Double = 0.05
    const val PURCHASE_PRICE_MAX_MULTIPLIER: Double = 1.35

    const val BOM_ASSEMBLY_MIN_FACTOR: Double = 2.50
    const val BOM_ASSEMBLY_MAX_FACTOR: Double = 3.80

    const val DEFAULT_CURRENCY: String = "USD"

    /**
     * حساب نطاقات الأسعار والتكاليف (الشراء، التجميع، الأسعار الداخلية، والتكلفة الإجمالية).
     */
    fun calculate(
        part: Part,
        bomItems: List<BomItem> = emptyList(),
        internalPrices: List<PartInternalPriceEntity> = emptyList(),
        defaultCurrency: String = DEFAULT_CURRENCY,
        timestamp: String = Clock.System.now().toString()
    ): PartPricingEntity {
        val purchaseMin = if (part.purchaseable) {
            if (part.minimumStock > 0) part.minimumStock * PURCHASE_PRICE_STOCK_FACTOR else DEFAULT_PURCHASE_PRICE_FALLBACK
        } else null
        val purchaseMax = purchaseMin?.times(PURCHASE_PRICE_MAX_MULTIPLIER)

        val bomTotalQuantity = bomItems.sumOf { it.quantity }
        val bomMin = if (part.assembly && bomItems.isNotEmpty()) bomTotalQuantity * BOM_ASSEMBLY_MIN_FACTOR else null
        val bomMax = if (part.assembly && bomItems.isNotEmpty()) bomTotalQuantity * BOM_ASSEMBLY_MAX_FACTOR else null

        val internalMin = internalPrices.minOfOrNull { it.price }
        val internalMax = internalPrices.maxOfOrNull { it.price }

        val allMins = listOfNotNull(purchaseMin, bomMin, internalMin)
        val allMaxs = listOfNotNull(purchaseMax, bomMax, internalMax)
        val overallMin = allMins.minOrNull() ?: 0.0
        val overallMax = allMaxs.maxOrNull() ?: overallMin

        val selectedCurrency = internalPrices.firstOrNull()?.currency?.ifBlank { defaultCurrency } ?: defaultCurrency

        return PartPricingEntity(
            partId = part.id,
            currency = selectedCurrency,
            overallMin = overallMin,
            overallMax = overallMax,
            purchaseCostMin = purchaseMin,
            purchaseCostMax = purchaseMax,
            bomCostMin = bomMin,
            bomCostMax = bomMax,
            internalCostMin = internalMin,
            internalCostMax = internalMax,
            updatedAt = timestamp
        )
    }
}
