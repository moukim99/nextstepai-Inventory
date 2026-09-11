package com.nextstepai.inventory.data.db

import androidx.room.TypeConverter
import com.nextstepai.inventory.sync.SyncStatus

/**
 * محول الأنواع (TypeConverter) لحقل syncStatus للربط بين الكائنات والنصوص في Room / SQLite.
 */
class SyncStatusConverter {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = runCatching {
        SyncStatus.valueOf(value)
    }.getOrDefault(SyncStatus.PENDING)
}
