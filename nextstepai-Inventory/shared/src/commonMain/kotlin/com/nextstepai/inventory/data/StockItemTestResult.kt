package com.nextstepai.inventory.data

/**
 * تمثيل نتائج فحوصات الجودة والأداء الفني للوحدة المخزنية (StockItemTestResult - 11 عموداً).
 *
 * @property id المفتاح الأساسي التسلسلي للسجل
 * @property stockItemId رابط العنصر المخزني الخاضع للفحص (StockItem.id)
 * @property templateId رابط قالب الاختبار المعياري من جدول (PartTestTemplate.id)
 * @property test اسم الاختبار الفني (مثل: "فحص الجهد الكهربائي"، "اختبار العزل")
 * @property result النتيجة الإجمالية للاختبار (true = ناجح Pass / false = راسب Fail)
 * @property value القيمة المقاسة الفعلية أثناء الاختبار (مثل: "5.02V"، "12.4 Ohm")
 * @property attachment مسار ملف مرفق يوثق نتائج القياس (PDF أو صورة تقرير)
 * @property notes ملاحظات الفني أو ظروف أداء الفحص
 * @property date تاريخ إجراء الاختبار
 * @property userId معرف المستخدم/المفتش الفني المسجل للفحص
 * @property metadata بيانات وصفية إضافية بصيغة JSON لأجهزة الفحص الآلي
 */
data class StockItemTestResult(
    val id: Long = 0L,
    val stockItemId: Long,
    val templateId: Long? = null,
    val test: String,
    val result: Boolean = true,
    val value: String = "Passed",
    val attachment: String = "",
    val notes: String = "",
    val date: String = "2025-02-15",
    val userId: Long? = null,
    val metadata: String = "{}"
)
