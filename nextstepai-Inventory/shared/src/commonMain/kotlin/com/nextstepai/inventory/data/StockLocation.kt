package com.nextstepai.inventory.data

/**
 * تمثيل موقع وأماكن التخزين في المستودعات (StockLocation) بجميع حقوله الـ 15 المعتمدة مع الهيكلية الشجرية MPTT.
 *
 * @property id المفتاح الأساسي الفريد للسجل
 * @property name اسم الموقع التخزيني (مثل: "مستودع أ"، "الرف 3"، "صندوق B12")
 * @property description وصف تفصيلي لطبيعة الموقع أو محتواه
 * @property parentId رابط الموقع الأب ضمن الهرمية الشجرية (يمكن أن يكون null للمواقع الجذرية)
 * @property structural موقع هيكلي فقط لتجميع العقد الفرعية ولا يُسمح بتخزين بضائع مباشرة فيه
 * @property external موقع خارجي أو مملوك لطرف ثالث (مثل موقع العميل أو المورد)
 * @property locationTypeId معرف نوع الموقع وقوالبه (ForeignKey)
 * @property ownerId معرف المالك أو المستخدم/المجموعة المسؤولة عن الموقع (ForeignKey)
 * @property icon اسم الأيقونة الرسومية المخصصة للموقع
 * @property customIcon مسار أو بيانات الأيقونة المخصصة المرفوعة
 * @property level عمق الموقع داخل الشجرة الهرمية (MPTT)
 * @property lft القيمة الحسابية اليسرى لتنظيم العقد الشجرية (MPTT)
 * @property rght القيمة الحسابية اليمنى لتنظيم العقد الشجرية (MPTT)
 * @property treeId معرف الشجرة المستقلة (MPTT)
 * @property metadata بيانات إضافية ديناميكية (JSON)
 */
data class StockLocation(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val parentId: Long? = null,
    val structural: Boolean = false,
    val external: Boolean = false,
    val locationTypeId: Long? = null,
    val ownerId: Long? = null,
    val icon: String = "warehouse",
    val customIcon: String = "",
    val level: Int = 0,
    val lft: Int = 0,
    val rght: Int = 0,
    val treeId: Int = 1,
    val metadata: String = "{}"
)
