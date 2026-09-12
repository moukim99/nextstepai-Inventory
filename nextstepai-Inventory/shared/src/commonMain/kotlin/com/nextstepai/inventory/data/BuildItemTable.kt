package com.nextstepai.inventory.data

/**
 * تمثيل سجل تخصيص واستهلاك عناصر المخزون الفعلي لأمر التصنيع (BuildItem Stock Allocation)
 * الملتزم بمواصفات نظام InvenTree.
 *
 * @property id المعرف الرقمي الفريد لسجل التخصيص
 * @property buildId معرف أمر التصنيع الرئيسي المرتبط (ForeignKey -> BuildOrder)
 * @property buildLineId معرف البند في أمر التصنيع (ForeignKey -> BuildOrderLineItem)
 * @property stockItemId عنصر أو دفعة المخزون الفيزيائية المسحوب منها (ForeignKey -> StockItem)
 * @property stockItemName اسم قطعة أو دفعة المخزون
 * @property quantity الكمية المحجوزة/المخصصة من عنصر المخزون لصالح أمر التصنيع
 * @property installIntoStockItemId معرف الرقم التسلسلي للمنتج النهائي المجمع المركبة بداخله القطعة (Genealogy Tree)
 * @property notes ملاحظات إضافية على عملية الحجز والسحب
 */
data class BuildItem(
    val id: Long = 0L,
    val buildId: Long,
    val buildLineId: Long? = null,
    val stockItemId: Long,
    val stockItemName: String = "",
    val quantity: Double = 1.0,
    val installIntoStockItemId: Long? = null,
    val notes: String = ""
)

/**
 * محاكاة جدول تخصيصات المخزون لأوامر التصنيع (Build Items Stock Allocations Table).
 */
class BuildItemTable {
    private val buildItems = mutableListOf<BuildItem>()
    private var nextBuildItemId = 1L

    init {
        seedSampleBuildItems()
    }

    private fun seedSampleBuildItems() {
        insertBuildItem(
            BuildItem(
                buildId = 1L,
                buildLineId = 1L,
                stockItemId = 501L,
                stockItemName = "دُفعة حساسات DHT22 #BATCH-DHT-01 (الرف A1)",
                quantity = 20.0,
                notes = "تخصيص تلقائي FIFO من المستودع الرئيسي"
            )
        )
    }

    fun insertBuildItem(item: BuildItem): BuildItem {
        val newItem = item.copy(
            id = if (item.id == 0L) nextBuildItemId++ else item.id
        )
        buildItems.add(newItem)
        return newItem
    }

    fun getBuildItemsForBuild(buildId: Long): List<BuildItem> {
        return buildItems.filter { it.buildId == buildId }
    }

    fun removeBuildItem(id: Long): Boolean {
        return buildItems.removeAll { it.id == id }
    }
}
