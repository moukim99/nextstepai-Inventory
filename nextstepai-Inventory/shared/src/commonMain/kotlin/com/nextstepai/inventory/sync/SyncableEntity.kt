package com.nextstepai.inventory.sync

/**
 * حالة المزامنة لكل سجل قابل للمزامنة السحابية.
 * تقتصر القيم حصرياً على PENDING و SYNCED.
 */
enum class SyncStatus {
    PENDING,
    SYNCED;

    companion object {
        fun fromString(value: String?): SyncStatus = when (value?.uppercase()?.trim()) {
            "SYNCED", "SYNCHRONIZED" -> SYNCED
            else -> PENDING
        }
    }
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
    val lastSyncTimestamp: Long,
    val idempotencyKey: String = ""
)

/**
 * نموذج استجابة المزامنة الدفعية (Batch Sync Response) من Cloudflare Worker.
 */
data class BatchSyncResponse(
    val acceptedUuids: List<String>,
    val remoteChanges: List<SyncPayload>,
    val serverTimestamp: Long
)

/**
 * استراتيجية حل تعارضات المزامنة بين البيانات المحلية والبعيدة.
 */
enum class ConflictResolutionStrategy {
    LAST_WRITE_WINS,
    CLIENT_WINS,
    SERVER_WINS
}

/**
 * معالج تعارضات المزامنة لتحديد السجل الرابح بشكل حتمي (Deterministic Conflict Resolution).
 */
object SyncConflictResolver {
    fun resolve(
        local: SyncPayload,
        remote: SyncPayload,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
    ): SyncPayload {
        return when (strategy) {
            ConflictResolutionStrategy.CLIENT_WINS -> local
            ConflictResolutionStrategy.SERVER_WINS -> remote
            ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                if (local.updatedAt >= remote.updatedAt) local else remote
            }
        }
    }

    fun mergeChanges(
        localPushes: List<SyncPayload>,
        remoteChanges: List<SyncPayload>,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
    ): List<SyncPayload> {
        val localMap = localPushes.associateBy { it.uuid }
        val merged = mutableListOf<SyncPayload>()

        for (remote in remoteChanges) {
            val local = localMap[remote.uuid]
            if (local == null) {
                merged.add(remote)
            } else {
                merged.add(resolve(local, remote, strategy))
            }
        }
        return merged
    }
}
