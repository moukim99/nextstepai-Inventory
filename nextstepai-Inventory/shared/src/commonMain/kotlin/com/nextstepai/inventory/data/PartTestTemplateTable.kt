package com.nextstepai.inventory.data

/**
 * نموذج بيانات قالب الفحوصات والاختبارات لضمان الجودة (PartTestTemplate) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لقالب الفحص
 * @property partId معرف القطعة المستهدفة بالاختبار (Part)
 * @property testName اسم الاختبار الفني (مثل: "قياس الجهد", "فحص العزل") - فريد للقطعة
 * @property description وصف تفصيلي لخطوات وإرشادات الفحص
 * @property required يحدد ما إذا كان اجتياز الاختبار إلزامياً لقبول القطعة
 * @property requiresValue يحدد ما إذا كان الاختبار يتطلب تسجيل قيمة عددية مقروءة
 * @property requiresAttachment يحدد ما إذا كان الاختبار يتطلب رفع ملف تقرير إثبات
 */
data class PartTestTemplate(
    val id: Long = 0L,
    val partId: Long,
    val testName: String,
    val description: String = "",
    val required: Boolean = true,
    val requiresValue: Boolean = false,
    val requiresAttachment: Boolean = false
)

/**
 * إدارة وتخزين قوالب الفحوصات والاختبارات (PartTestTemplate) في الذاكرة مع القواعد والقيود المنطقية.
 */
class PartTestTemplateTable(
    private val partTable: PartTable = PartTable()
) {
    private val testTemplates = mutableListOf<PartTestTemplate>()
    private var nextId = 1L

    init {
        seedSampleTestTemplates()
    }

    private fun seedSampleTestTemplates() {
        val allParts = partTable.getAllParts()
        if (allParts.isNotEmpty()) {
            val part = allParts.first()
            insertTestTemplate(
                PartTestTemplate(
                    partId = part.id,
                    testName = "قياس جهد التشغيل الأساسي",
                    description = "فحص استقرار جهد الدخل باستخدام جهاز الفولتميتر الفني",
                    required = true,
                    requiresValue = true,
                    requiresAttachment = false
                )
            )
            insertTestTemplate(
                PartTestTemplate(
                    partId = part.id,
                    testName = "فحص العزل الحراري والهيكلي",
                    description = "فحص بصري وهيكلي لمعاينة سلامة طبقة العزل الخارجي",
                    required = false,
                    requiresValue = false,
                    requiresAttachment = true
                )
            )
        }
    }

    /**
     * إدراج قالب فحص جديد مع تطبيق القيود:
     * 1. اسم الفحص إلزامي ولا يمكن أن يكون فارغاً.
     * 2. قيد التفرد المركب (unique_together = ['part', 'test_name']): يمنع تكرار اسم الاختبار لنفس القطعة.
     */
    fun insertTestTemplate(template: PartTestTemplate): PartTestTemplate {
        require(template.testName.isNotBlank()) {
            "اسم اختبار الفحص (test_name) إلزامي ولا يمكن أن يكون فارغاً."
        }

        val exists = testTemplates.any {
            it.partId == template.partId && it.testName.trim().equals(template.testName.trim(), ignoreCase = true)
        }
        require(!exists) {
            "اختبار بنفس الاسم '${template.testName.trim()}' مضاف بالفعل لهذه القطعة! (قيد التفرد unique_together مُفعّل)"
        }

        val record = template.copy(
            id = if (template.id == 0L) nextId++ else template.id,
            testName = template.testName.trim()
        )
        testTemplates.add(record)
        return record
    }

    /**
     * جلب قوالب الفحوصات الخاصة بقطعة معينة.
     */
    fun getTestTemplatesForPart(partId: Long): List<PartTestTemplate> {
        return testTemplates.filter { it.partId == partId }
    }

    /**
     * حذف قالب فحص محدد.
     */
    fun deleteTestTemplate(id: Long): Boolean {
        return testTemplates.removeIf { it.id == id }
    }

    /**
     * الحذف المتتابع (CASCADE Delete) لكافة قوالب الفحوصات التابعة لقطعة محددة عند مسحها.
     */
    fun cascadeDeleteForPart(partId: Long) {
        testTemplates.removeIf { it.partId == partId }
    }
}
