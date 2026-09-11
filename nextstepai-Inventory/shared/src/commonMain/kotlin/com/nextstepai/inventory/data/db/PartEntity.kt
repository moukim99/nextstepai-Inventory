package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل جدول الكيان المكتمل للقطعة في قاعدة بيانات Room و SQLite (PartEntity).
 * يحتوي على كافة مفاتيح التوجيه المنطقي والتفاضلي (assembly, component, purchaseable, salable, trackable, isTemplate, variantOfId, virtual, active, locked).
 */
@Entity(tableName = "parts")
data class PartEntity(
    @PrimaryKey
    override val uuid: String,
    val id: Long = 0L,
    val name: String,
    val ipn: String = "",
    val description: String = "",
    val revision: String = "",
    val keywords: String = "",
    val categoryId: Long? = null,
    val units: String = "pcs",
    val assembly: Boolean = false,
    val component: Boolean = true,
    val isTemplate: Boolean = false,
    val variantOfId: Long? = null,
    val trackable: Boolean = false,
    val purchaseable: Boolean = true,
    val salable: Boolean = false,
    val virtual: Boolean = false,
    val active: Boolean = true,
    val locked: Boolean = false,
    val minimumStock: Double = 0.0,
    val maximumStock: Double? = null,
    val defaultLocationId: Long? = null,
    val defaultExpiryDays: Int? = null,
    val totalInStock: Double = 0.0,
    val localImagePath: String? = null,
    val link: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
) : SyncableEntity
