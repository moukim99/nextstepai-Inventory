package com.nextstepai.inventory.sync

/**
 * حالة المزامنة لكل سجل قابل للمزامنة السحابية.
 * تقتصر القيم حصرياً على PENDING و SYNCED.
 */
enum class SyncStatus {
    PENDING,
    SYNCED
}

/**
 * الواجهة الأساسية للكيانات القابلة للمزامنة مع السحابة (Cloudflare Worker).
 */
interface SyncableEntity {
    val uuid: String
    val syncStatus: SyncStatus
    val isDeleted: Boolean
    val updatedAt: Long
}

/**
 * حمولة المزامنة الجماعية (Batch Sync Payload) لإرسال واستقبال السجلات دفعة واحدة.
 */
data class SyncPayload(
    val uuid: String,
    val entityType: String,
    val payloadJson: String,
    val isDeleted: Boolean,
    val updatedAt: Long
)

/**
 * نموذج طلب المزامنة الدفعية (Batch Sync Request) إلى Cloudflare Worker.
 */
data class BatchSyncRequest(
    val pendingPushes: List<SyncPayload>,
    val lastSyncTimestamp: Long
)

/**
 * نموذج استجابة المزامنة الدفعية (Batch Sync Response) من Cloudflare Worker.
 */
data class BatchSyncResponse(
    val acceptedUuids: List<String>,
    val remoteChanges: List<SyncPayload>,
    val serverTimestamp: Long
)
