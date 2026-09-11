package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * تمثيل كيان شرائح أسعار البيع للعملاء (PartSalePriceEntity / PartSellPriceBreak) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لشريحة سعر البيع
 * @property partId معرف القطعة القابلة للبيع المستهدفة بالسعر (Part - salable = true)
 * @property quantity الحد الأدنى للكمية المؤهلة للحصول على هذا السعر (quantity >= 1)
 * @property price القيمة المالية المحددة لسعر بيع الوحدة الواحدة للعميل عند هذه الشريحة
 * @property currency رمز العملة المالية المسعر بها السجل
 */
@Entity(tableName = "part_sale_prices")
data class PartSalePriceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val quantity: Double = 1.0,
    val price: Double = 0.0,
    val currency: String = "USD"
)
