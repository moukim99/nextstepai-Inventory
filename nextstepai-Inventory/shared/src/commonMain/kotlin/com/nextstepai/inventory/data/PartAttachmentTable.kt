package com.nextstepai.inventory.data

enum class AttachmentType { IMAGE, DOCUMENT, LINK }

data class PendingAttachment(
    val id: Long = 0L,
    val type: AttachmentType,
    val pathOrUrl: String,
    val label: String
)

/**
 * نموذج بيانات مرفقات ووثائق القطع (PartAttachment) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لسجل المرفق
 * @property partId معرف القطعة المستهدفة بربط الوثيقة (Part)
 * @property attachment مسار واسم الملف المرفق المحلي أو السحابي (PDF, CAD, Image)
 * @property link رابط إنترنت خارجي للمستندات أو مواصفات المورد
 * @property comment وصف توضيحي أو ملاحظة حول محتوى المرفق
 * @property uploadDate تاريخ ووقت رفع المرفق أو تسجيل الرابط
 * @property userId معرف المستخدم المسؤول عن رفع المرفق
 */
data class PartAttachment(
    val id: Long = 0L,
    val partId: Long,
    val attachment: String? = null,
    val link: String? = null,
    val comment: String = "",
    val uploadDate: String = "",
    val userId: Long? = 1L
)

/**
 * إدارة وتخزين مرفقات ووثائق القطع (PartAttachment) في الذاكرة مع الشروط والقيود المنطقية.
 */
class PartAttachmentTable(
    private val partTable: PartTable = PartTable()
) {
    private val attachments = mutableListOf<PartAttachment>()
    private var nextId = 1L

    init {
        seedSampleAttachments()
    }

    private fun seedSampleAttachments() {
        val allParts = partTable.getAllParts()
        if (allParts.isNotEmpty()) {
            val part = allParts.first()
            insertAttachment(
                PartAttachment(
                    partId = part.id,
                    attachment = "/docs/datasheet_res_10k.pdf",
                    comment = "الورقة الفنية القياسية للمقاومة 10K",
                    uploadDate = "2025-02-15 10:30:00"
                )
            )
            insertAttachment(
                PartAttachment(
                    partId = part.id,
                    link = "https://www.vendor.com/parts/res-10k-spec",
                    comment = "رابط مواصفات المورد الخارجي",
                    uploadDate = "2025-02-15 11:00:00"
                )
            )
        }
    }

    /**
     * إدراج سجل مرفق جديد مع تطبيق القيود المنطقية:
     * 1. قيد مصدر المرفق: يلزم ملء أحد الحقلين على الأقل (إما attachment أو link).
     * 2. الوصف الافتراضي: في حال ترك comment فارغاً، يتم تعبئته تلقائياً من اسم الملف أو الرابط.
     */
    fun insertAttachment(attachmentItem: PartAttachment): PartAttachment {
        val hasFile = !attachmentItem.attachment.isNullOrBlank()
        val hasLink = !attachmentItem.link.isNullOrBlank()

        require(hasFile || hasLink) {
            "خطأ في إدخال المرفق: يجب تقديم ملف مرفق محلي (attachment) أو رابط ويب خارجي (link) على الأقل!"
        }

        val autoComment = if (attachmentItem.comment.isBlank()) {
            val file = attachmentItem.attachment
            val url = attachmentItem.link
            when {
                !file.isNullOrBlank() -> file.substringAfterLast('/').substringAfterLast('\\')
                !url.isNullOrBlank() -> url
                else -> "مرفق قطعة"
            }
        } else {
            attachmentItem.comment
        }

        val record = attachmentItem.copy(
            id = if (attachmentItem.id == 0L) nextId++ else attachmentItem.id,
            comment = autoComment,
            uploadDate = attachmentItem.uploadDate.ifBlank { "2025-02-15 12:00:00" }
        )
        attachments.add(record)
        return record
    }

    /**
     * جلب المرفقات والوثائق التابعة لقطعة معينة.
     */
    fun getAttachmentsForPart(partId: Long): List<PartAttachment> {
        return attachments.filter { it.partId == partId }
    }

    /**
     * حذف مرفق محدد.
     */
    fun deleteAttachment(id: Long): Boolean {
        return attachments.removeIf { it.id == id }
    }

    /**
     * الحذف المتتابع (CASCADE Delete) لكافة المرفقات والملفات المادية التابعة لقطعة محددة عند مسحها.
     */
    fun cascadeDeleteForPart(partId: Long) {
        attachments.removeIf { it.partId == partId }
    }
}
