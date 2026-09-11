package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * تمثيل كيان حساب وتتبع تكاليف وأسعار القطع (PartPricingEntity) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لسجل التسعير
 * @property partId معرف القطعة المستهدفة بالتسعير (Part - OneToOne)
 * @property currency العملة الافتراضية المعتمدة لحساب التكاليف
 * @property overallMin الحد الأدنى التقديري الإجمالي لتكلفة القطعة
 * @property overallMax الحد الأقصى التقديري الإجمالي لتكلفة القطعة
 * @property purchaseCostMin أدنى سعر شراء مسجل للقطعة
 * @property purchaseCostMax أقصى سعر شراء مسجل للقطعة
 * @property bomCostMin أدنى تكلفة مواد محسوبة لإنتاج القطعة من قائمة الـ BOM
 * @property bomCostMax أقصى تكلفة مواد محسوبة لإنتاج القطعة من قائمة الـ BOM
 * @property variantCostMin أدنى تكلفة مسجلة عبر المتغيرات والبدائل
 * @property variantCostMax أقصى تكلفة مسجلة عبر المتغيرات والبدائل
 * @property internalCostMin أدنى سعر بيع داخلي مسجل للقطعة
 * @property internalCostMax أقصى سعر بيع داخلي مسجل للقطعة
 * @property updatedAt الطابع الزمني لآخر عملية حساب وتحديث للتكاليف
 */
@Entity(tableName = "part_pricing")
data class PartPricingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val currency: String = "USD",
    val overallMin: Double? = null,
    val overallMax: Double? = null,
    val purchaseCostMin: Double? = null,
    val purchaseCostMax: Double? = null,
    val bomCostMin: Double? = null,
    val bomCostMax: Double? = null,
    val variantCostMin: Double? = null,
    val variantCostMax: Double? = null,
    val internalCostMin: Double? = null,
    val internalCostMax: Double? = null,
    val updatedAt: String = ""
)
