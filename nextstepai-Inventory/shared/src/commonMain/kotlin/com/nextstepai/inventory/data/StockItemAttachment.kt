package com.nextstepai.inventory.data

import com.nextstepai.inventory.util.DateTimeUtils

/**
 * تمثيل مستندات ومرفقات العناصر المخزنية (StockItemAttachment - 8 أعمدة) مع تاريخ الرفع اللحظي الديناميكي.
 *
 * @property id المفتاح الأساسي التسلسلي للسجل
 * @property stockItemId رابط العنصر المخزني الفيزيائي المرتبط به المرفق (StockItem.id)
 * @property attachment مسار الملف المرفوع المخزن على الخادم أو السحابة (يمكن أن يكون null إذا وُجد رابط)
 * @property link رابط تشعبي خارجي للوثيقة بدلاً من رفع ملف محلي (يمكن أن يكون null إذا وُجد ملف)
 * @property comment وصف أو تعليق يوضح نوع ومضمون الوثيقة (مثل: "شهادة مطابقة CoC"، "فاتورة مورد")
 * @property uploadDate تاريخ رفع المرفق اللحظي الديناميكي
 * @property userId معرف المستخدم الذي رفع المستند
 * @property metadata بيانات إضافية ديناميكية بصيغة JSON
 */
data class StockItemAttachment(
    val id: Long = 0L,
    val stockItemId: Long,
    val attachment: String? = null,
    val link: String? = null,
    val comment: String = "",
    val uploadDate: String = DateTimeUtils.getCurrentDate(),
    val userId: Long? = null,
    val metadata: String = "{}"
)
