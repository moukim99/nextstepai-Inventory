package com.nextstepai.inventory.data

import com.nextstepai.inventory.util.DateTimeUtils

/**
 * تمثيل سجل التتبع والتغييرات التاريخية غير القابلة للتعديل للوحدة المخزنية (StockItemTracking - 8 أعمدة) مع التاريخ اللحظي الديناميكي.
 *
 * @property id المفتاح الأساسي الفريد للسجل
 * @property stockItemId رابط العنصر المخزني الفيزيائي (StockItem.id)
 * @property date الختم الزمني الديناميكي اللحظي لوقت وقوع الحركة
 * @property trackingType كود نوع الحركة المخزنية المعتمد
 * @property userId معرف المستخدم المنفذ للعملية (يمكن أن يكون null للعمليات الآلية)
 * @property label عنوان أو ملخص مختصر يصف الحركة
 * @property notes ملاحظات وشروحات مخصصة يدخلها المستخدم
 * @property deltas كائن JSON يحفظ الفروقات بين الحالة السابقة والحالية (مثل: الكمية والموقع والحالة)
 */
data class StockItemTracking(
    val id: Long = 0L,
    val stockItemId: Long,
    val date: String = DateTimeUtils.getCurrentDateTime(),
    val trackingType: StockTrackingType = StockTrackingType.CREATED,
    val userId: Long? = null,
    val label: String = trackingType.label,
    val notes: String = "",
    val deltas: String = "{}"
)
