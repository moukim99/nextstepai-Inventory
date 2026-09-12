package com.nextstepai.inventory.data

/**
 * تمثيل أنواع وقوالب مواقع التخزين المعيارية (StockLocationType) بكافة أعمدتها الـ 6 المعتمدة.
 *
 * @property id المفتاح الأساسي التلقائي للسجل
 * @property name اسم نوع أو نمط الموقع التخزيني (مثل: "رف"، "ممر"، "حاوية")
 * @property description وصف تفصيلي لطبيعة استخدام نوع الموقع
 * @property icon اسم الأيقونة المعيارية
 * @property customIcon مسار أو بيانات الأيقونة المخصصة المرفوعة يدوياً
 * @property metadata بيانات إضافية بصيغة JSON
 */
data class StockLocationType(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val icon: String = "warehouse",
    val customIcon: String = "",
    val metadata: String = "{}"
) {
    /**
     * الأيقونة المعتمدة للعرض البصري؛ تُعطى الأولوية للأيقونة المخصصة المرفوعة يدوياً على الأيقونة المعيارية.
     */
    val effectiveIcon: String
        get() = customIcon.ifBlank { icon }
}
