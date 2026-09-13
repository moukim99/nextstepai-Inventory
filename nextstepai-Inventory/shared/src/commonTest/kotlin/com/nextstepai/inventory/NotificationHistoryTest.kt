package com.nextstepai.inventory

import com.nextstepai.inventory.data.db.NotificationHistoryDao
import com.nextstepai.inventory.data.db.NotificationHistoryEntity
import com.nextstepai.inventory.repository.NotificationRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class NotificationHistoryTest {

    private val dao = NotificationHistoryDao()
    private val repository = NotificationRepository(notificationDao = dao)

    @Test
    fun testInsertAndGetUnreadNotifications() = runBlocking {
        val notif1 = NotificationHistoryEntity(
            uuid = "test-notif-1",
            title = "تنبيه انتهاء وثيقة 1",
            message = "رسالة اختبارية 1",
            notificationType = "COMPANY_DOC_EXPIRY",
            targetEntityUuid = "att-101",
            companyUuid = "company-5",
            scheduledDate = Clock.System.now().toEpochMilliseconds() - 1000L,
            isRead = false,
            isTriggered = false
        )

        dao.insertOrUpdate(notif1)

        val retrieved = dao.getByUuid("test-notif-1")
        assertNotNull(retrieved)
        assertEquals("تنبيه انتهاء وثيقة 1", retrieved.title)
        assertEquals("COMPANY_DOC_EXPIRY", retrieved.notificationType)
        assertFalse(retrieved.isRead)

        val unread = dao.getUnreadNotifications()
        assertTrue(unread.any { it.uuid == "test-notif-1" })
    }

    @Test
    fun testPendingTriggerAndMarkAsTriggered() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val notifPending = NotificationHistoryEntity(
            uuid = "test-notif-trigger",
            title = "تنبيه معلق للإرسال",
            message = "رسالة موعد المستند",
            notificationType = "COMPANY_DOC_EXPIRY",
            targetEntityUuid = "att-102",
            companyUuid = "company-6",
            scheduledDate = now - 5000L, // موعده قد مضى
            isRead = false,
            isTriggered = false
        )

        dao.insertOrUpdate(notifPending)

        val pendingList = repository.getPendingTriggerNotifications(now)
        assertTrue(pendingList.any { it.uuid == "test-notif-trigger" })

        // تعديل الحالة إلى تم الإرسال
        repository.markAsTriggered("test-notif-trigger")

        val updated = dao.getByUuid("test-notif-trigger")
        assertNotNull(updated)
        assertTrue(updated.isTriggered)

        // لم يعد بانتظار الإرسال
        val pendingListAfter = repository.getPendingTriggerNotifications(now)
        assertFalse(pendingListAfter.any { it.uuid == "test-notif-trigger" })
    }

    @Test
    fun testScheduleCompanyDocExpiryNotification() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val expiryDate = now + (60L * 24L * 60L * 60L * 1000L) // بعد 60 يوماً

        val scheduled = repository.scheduleCompanyDocExpiryNotification(
            documentUuid = "doc-999",
            companyUuid = "comp-88",
            documentTitle = "السجل التجاري",
            companyName = "شركة التقنية العليا",
            expiryDateMs = expiryDate
        )

        assertEquals("notif-doc-doc-999", scheduled.uuid)
        assertEquals("company/comp-88", scheduled.deepLink)
        assertFalse(scheduled.isRead)
        assertFalse(scheduled.isTriggered)

        val saved = dao.getByUuid("notif-doc-doc-999")
        assertNotNull(saved)
        assertEquals("doc-999", saved.targetEntityUuid)
    }
}
