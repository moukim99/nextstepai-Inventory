package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان الشركة (CompanyEntity) في قاعدة بيانات Room المحلية.
 */
@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey
    override val uuid: String,
    val name: String,
    val description: String = "",
    val phone: String = "",
    val email: String = "",
    val isSupplier: Boolean = true,
    val isManufacturer: Boolean = false,
    val isCustomer: Boolean = false,
    val currency: String = "USD",
    val logoPath: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
