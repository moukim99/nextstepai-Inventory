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
    val length: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val maxWeight: Double = 0.0,
    val maxVolume: Double = 0.0,
    val metadata: String = "{}"
) {
    /**
     * الأيقونة المعتمدة للعرض البصري؛ تُعطى الأولوية للأيقونة المخصصة المرفوعة يدوياً على الأيقونة المعيارية.
     */
    val effectiveIcon: String
        get() = customIcon.ifBlank { icon }

    val calculatedVolume: Double
        get() = if (maxVolume > 0.0) maxVolume else (length * width * height)

    fun hasPhysicalSpecs(): Boolean =
        length > 0.0 || width > 0.0 || height > 0.0 || maxWeight > 0.0 || maxVolume > 0.0

    fun formatSpecsBadge(): String {
        val parts = mutableListOf<String>()
        if (length > 0.0 && width > 0.0 && height > 0.0) {
            parts.add("الأبعاد: ${length}×${width}×${height} م")
        }
        if (maxWeight > 0.0) {
            parts.add("أقصى حمولة: ${maxWeight} كجم")
        }
        if (calculatedVolume > 0.0) {
            parts.add("الحجم: ${calculatedVolume} م³")
        }
        return if (parts.isNotEmpty()) parts.joinToString(" | ") else "مواصفات قياسية عامة"
    }
}
