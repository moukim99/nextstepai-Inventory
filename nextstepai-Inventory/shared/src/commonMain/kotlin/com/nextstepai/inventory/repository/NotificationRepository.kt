package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.db.AppSettingsDao
import com.nextstepai.inventory.data.db.NotificationHistoryDao
import com.nextstepai.inventory.data.db.NotificationHistoryEntity
import com.nextstepai.inventory.sync.SyncStatus
import kotlin.time.Clock

/**
 * مستودع إدارة واحتساب سجل التنبيهات والإشعارات (NotificationRepository).
 */
class NotificationRepository(
    private val notificationDao: NotificationHistoryDao = NotificationHistoryDao(),
    private val appSettingsDao: AppSettingsDao = AppSettingsDao()
) {

    /**
     * إدراج أو تحديث إشعار في قاعدة البيانات.
     */
    suspend fun saveNotification(notification: NotificationHistoryEntity) {
        notificationDao.insertOrUpdate(notification)
    }

    /**
     * جلب جميع الإشعارات المتاحة للصفحات.
     */
    suspend fun getAllNotifications(limit: Int = 50, offset: Int = 0): List<NotificationHistoryEntity> {
        return notificationDao.getAllNotifications(limit, offset)
    }

    /**
     * جلب الإشعارات غير المقروءة فقط.
     */
    suspend fun getUnreadNotifications(): List<NotificationHistoryEntity> {
        return notificationDao.getUnreadNotifications()
    }

    /**
     * جلب عدد الإشعارات غير المقروءة.
     */
    suspend fun getUnreadCount(): Int {
        return notificationDao.getUnreadCount()
    }

    /**
     * جلب الإشعارات المجدولة المستحقة للإرسال بالنظام (لم تُطلق بعد وق حان موعدها).
     */
    suspend fun getPendingTriggerNotifications(currentTimeMs: Long = Clock.System.now().toEpochMilliseconds()): List<NotificationHistoryEntity> {
        return notificationDao.getPendingTriggerNotifications(currentTimeMs)
    }

    /**
     * جدولة إشعار انتهاء وثيقة شركة تلقائياً بناءً على تاريخ الانتهاء وعدد الأيام المحددة في إعدادات التطبيق.
     *
     * @param documentUuid المعرف الفريد للوثيقة (company_attachments.uuid)
     * @param companyUuid المعرف الفريد للشركة
     * @param documentTitle عنوان الوثيقة
     * @param companyName اسم الشركة
     * @param expiryDateMs تاريخ انتهاء صلاحية الوثيقة بالطابع الزمني (Milliseconds)
     */
    suspend fun scheduleCompanyDocExpiryNotification(
        documentUuid: String,
        companyUuid: String,
        documentTitle: String,
        companyName: String,
        expiryDateMs: Long
    ): NotificationHistoryEntity {
        val settings = appSettingsDao.getSettings()
        val warningDaysMs = settings.docExpiryWarningDays.toLong() * 24L * 60L * 60L * 1000L
        val scheduledDateMs = expiryDateMs - warningDaysMs

        val notification = NotificationHistoryEntity(
            uuid = "notif-doc-$documentUuid",
            title = "تنبيه قرب انتهاء وثيقة",
            message = "الوثيقة '$documentTitle' التابعة لشركة '$companyName' ستنتهي قريباً.",
            notificationType = "COMPANY_DOC_EXPIRY",
            targetEntityUuid = documentUuid,
            companyUuid = companyUuid,
            deepLink = "company/$companyUuid",
            scheduledDate = scheduledDateMs,
            isRead = false,
            isTriggered = false,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            syncStatus = SyncStatus.PENDING
        )

        notificationDao.insertOrUpdate(notification)
        return notification
    }

    /**
     * تحديث إشعار كـ "مقروء".
     */
    suspend fun markAsRead(uuid: String) {
        notificationDao.markAsRead(uuid)
    }

    /**
     * تحديث إشعار كـ "تم إرساله" عبر النظام.
     */
    suspend fun markAsTriggered(uuid: String) {
        notificationDao.markAsTriggered(uuid)
    }

    /**
     * حذف إشعار (حذف منطقي soft delete).
     */
    suspend fun deleteNotification(uuid: String) {
        notificationDao.delete(uuid)
    }
}
