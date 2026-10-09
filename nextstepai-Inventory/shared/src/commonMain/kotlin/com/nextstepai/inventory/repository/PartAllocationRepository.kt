package com.nextstepai.inventory.repository

import com.nextstepai.inventory.data.db.BuildOrderDao
import com.nextstepai.inventory.data.db.BuildOrderLineItemDao
import com.nextstepai.inventory.data.db.NotificationHistoryDao
import com.nextstepai.inventory.data.db.NotificationHistoryEntity
import com.nextstepai.inventory.data.db.PartAllocationDao
import com.nextstepai.inventory.data.db.PartAllocationEntity
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.util.AppUuid
import kotlin.time.Clock

/**
 * المستودع (Repository) المسؤول عن تتبع وإدارة مخصصات وحجوزات المخزون الذكية (Part Allocations & Commitments).
 * يضمن سرعة الاستعلامات الذرية وحساب الأرصدة الحية ومعالجة الآثار التابعة عند فك أو استهلاك الحجوزات.
 */
class PartAllocationRepository(
    private val allocationDao: PartAllocationDao = PartAllocationDao(),
    private val partDao: PartDao = PartDao(),
    private val buildOrderDao: BuildOrderDao = BuildOrderDao(),
    private val buildOrderLineItemDao: BuildOrderLineItemDao = BuildOrderLineItemDao(),
    private val notificationDao: NotificationHistoryDao = NotificationHistoryDao()
) {

    /**
     * جلب كافة الحجوزات النشطة لقطعة محددة مع البذر التلقائي لبيانات العرض التوضيحي إذا لم توجد حجوزات.
     */
    fun getActiveAllocationsForPart(partId: Long): List<PartAllocationEntity> {
        seedSampleAllocationsIfEmpty(partId)
        return allocationDao.getActiveAllocationsForPart(partId)
    }

    /**
     * حساب إجمالي الكمية المحجوزة حجزاً مؤكداً (Committed HARD) لقطعة.
     */
    fun getCommittedQuantity(partId: Long): Double {
        seedSampleAllocationsIfEmpty(partId)
        return allocationDao.getTotalCommittedQuantity(partId)
    }

    /**
     * حساب إجمالي الكمية المحجوزة حجزاً مبدئياً (Reserved SOFT) لقطعة.
     */
    fun getSoftQuantity(partId: Long): Double {
        seedSampleAllocationsIfEmpty(partId)
        return allocationDao.getTotalSoftQuantity(partId)
    }

    /**
     * إنشاء حجز مخزون جديد مع التسجيل الذري.
     */
    suspend fun createAllocation(
        partId: Long,
        quantity: Double,
        type: String = "HARD",
        refType: String,
        refId: String,
        refTitle: String,
        userId: String = "usr-001",
        notes: String? = null
    ): Result<Long> = runCatching {
        require(quantity > 0) { "كمية الحجز يجب أن تكون أكبر من الصفر" }
        val entity = PartAllocationEntity(
            partId = partId,
            allocatedQuantity = quantity,
            allocationType = type,
            referenceType = refType,
            referenceId = refId,
            referenceTitle = refTitle,
            status = "ACTIVE",
            createdAt = Clock.System.now().toEpochMilliseconds(),
            createdByUserId = userId,
            notes = notes
        )
        val insertedId = allocationDao.insertAllocation(entity)

        // تسجيل حركة التدقيق
        logAuditEntry(
            title = "إنشاء حجز مخزون جديد",
            message = "تم حجز $quantity قطعة ($type) للوثيقة [$refId] ($refTitle)",
            partId = partId
        )

        insertedId
    }

    /**
     * فك/إلغاء حجز مخزون وتحديث الحالات التابعة في أمر الإنتاج وسجل التدقيق.
     */
    suspend fun releaseAllocation(allocationId: Long, userId: String = "usr-001"): Result<Boolean> = runCatching {
        val allocation = allocationDao.getAllocationById(allocationId)
            ?: throw IllegalArgumentException("سجل الحجز غير موجود: $allocationId")

        if (allocation.status != "ACTIVE") {
            return Result.success(false)
        }

        // 1. تحديث حالة الحجز إلى RELEASED
        val success = allocationDao.releaseAllocation(allocationId)
        if (!success) return Result.success(false)

        // 2. معالجة الآثار التابعة بحسب نوع الوثيقة
        when (allocation.referenceType) {
            "BUILD_ORDER" -> {
                // تحديث بند المكون داخل أمر الإنتاج إلى غير مخصص/ناقص
                runCatching {
                    val buildOrders = buildOrderDao.getBuildOrdersPaged(limit = 100)
                    val targetBO = buildOrders.find { it.reference == allocation.referenceId }
                    if (targetBO != null) {
                        val lineItems = buildOrderLineItemDao.getLineItemsForBuildUuid(targetBO.uuid)
                        val targetLine = lineItems.find { it.subPartId == allocation.partId }
                        if (targetLine != null) {
                            val newAllocated = (targetLine.allocatedQuantity - allocation.allocatedQuantity).coerceAtLeast(0.0)
                            buildOrderLineItemDao.insertOrUpdate(
                                targetLine.copy(allocatedQuantity = newAllocated)
                            )
                        }
                    }
                }
            }
            "SALES_ORDER" -> {
                // إشعار أمر البيع بنقص التغطية المخزنية
                logAuditEntry(
                    title = "تنبيه أمر البيع",
                    message = "تم فك حجز ${allocation.allocatedQuantity} قطعة الخاصة بأمر البيع [${allocation.referenceId}] - يلزم إعادة الحجز قبل الشحن",
                    partId = allocation.partId
                )
            }
        }

        // 3. توثيق العملية في سجل الحركات العام (Audit Ledger)
        logAuditEntry(
            title = "فك حجز مخزون",
            message = "تم فك حجز ${allocation.allocatedQuantity} قطعة الخاصة بـ [${allocation.referenceId}] بواسطة $userId وإعادة الكمية فوراً للمخزون الحر",
            partId = allocation.partId
        )

        true
    }

    /**
     * تحويل الحجز إلى مستهلك (CONSUMED) عند الصرف الفعلي للمخزون (STOCK_OUT).
     */
    suspend fun consumeAllocation(allocationId: Long): Result<Boolean> = runCatching {
        val allocation = allocationDao.getAllocationById(allocationId)
            ?: throw IllegalArgumentException("سجل الحجز غير موجود: $allocationId")

        val success = allocationDao.consumeAllocation(allocationId)
        if (success) {
            logAuditEntry(
                title = "استهلاك حجز مخزون",
                message = "تم صرف واستهلاك ${allocation.allocatedQuantity} قطعة المخصصة للوثيقة [${allocation.referenceId}]",
                partId = allocation.partId
            )
        }
        success
    }

    private suspend fun logAuditEntry(title: String, message: String, partId: Long) {
        runCatching {
            val now = Clock.System.now().toEpochMilliseconds()
            val notif = NotificationHistoryEntity(
                uuid = AppUuid.generate(),
                title = title,
                message = message,
                notificationType = "AUDIT_LEDGER",
                targetEntityUuid = "part-$partId",
                scheduledDate = now,
                createdAt = now,
                updatedAt = now
            )
            notificationDao.insertOrUpdate(notif)
        }
    }

    companion object {
        private val seededPartIds = mutableSetOf<Long>()
    }

    private fun seedSampleAllocationsIfEmpty(partId: Long) {
        if (seededPartIds.contains(partId)) return
        seededPartIds.add(partId)

        val allList = allocationDao.getAllAllocationsForPart(partId)
        if (allList.isNotEmpty()) return

        // بذر بيانات توضيحية لبعض القطع
        val now = Clock.System.now().toEpochMilliseconds()
        when (partId) {
            1L, 2L, 7L -> {
                // ESP32 or MCU Sample Allocations
                allocationDao.insertAllocation(
                    PartAllocationEntity(
                        partId = partId,
                        allocatedQuantity = 2.0,
                        allocationType = "HARD",
                        referenceType = "BUILD_ORDER",
                        referenceId = "BO-2026-089",
                        referenceTitle = "أمر تصنيع محول طاقة ذكي",
                        status = "ACTIVE",
                        createdAt = now - 3600000L * 5,
                        createdByUserId = "مهندس أحمد",
                        notes = "مخصص لخط التجميع الرئيسي"
                    )
                )
                allocationDao.insertAllocation(
                    PartAllocationEntity(
                        partId = partId,
                        allocatedQuantity = 1.0,
                        allocationType = "SOFT",
                        referenceType = "QC_HOLD",
                        referenceId = "QC-2026-012",
                        referenceTitle = "حجز مبدئي للفحص الفني والقياس",
                        status = "ACTIVE",
                        createdAt = now - 3600000L * 24,
                        createdByUserId = "فني جودة",
                        notes = "طلب عينة اختبار"
                    )
                )
            }
            3L, 11L -> {
                allocationDao.insertAllocation(
                    PartAllocationEntity(
                        partId = partId,
                        allocatedQuantity = 5.0,
                        allocationType = "HARD",
                        referenceType = "SALES_ORDER",
                        referenceId = "SO-2026-104",
                        referenceTitle = "أمر بيع شركة التقنية العالية",
                        status = "ACTIVE",
                        createdAt = now - 3600000L * 12,
                        createdByUserId = "م. خالد",
                        notes = "شحنة منتجات مستشعرات"
                    )
                )
            }
            6L -> {
                allocationDao.insertAllocation(
                    PartAllocationEntity(
                        partId = partId,
                        allocatedQuantity = 3.0,
                        allocationType = "HARD",
                        referenceType = "RETURN_ORDER",
                        referenceId = "RET-2026-005",
                        referenceTitle = "طلب إرجاع وصيانة محول",
                        status = "ACTIVE",
                        createdAt = now - 3600000L * 2,
                        createdByUserId = "سارة علي",
                        notes = "تبديل قطعة تالفة"
                    )
                )
            }
        }
    }
}
