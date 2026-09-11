package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * تمثيل كيان شرائح أسعار البيع والتكلفة الداخلية (PartInternalPriceEntity) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لشريحة السعر الداخلي
 * @property partId معرف القطعة المستهدفة بالسعر الداخلي (Part)
 * @property quantity الحد الأدنى للكمية المؤهلة للحصول على السعر (quantity >= 1)
 * @property price القيمة المالية المحددة لسعر الوحدة الواحدة عند هذه الشريحة
 * @property currency رمز العملة المالية المسعر بها السجل
 */
@Entity(tableName = "part_internal_prices")
data class PartInternalPriceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val quantity: Double = 1.0,
    val price: Double = 0.0,
    val currency: String = "USD"
)
