package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import com.nextstepai.inventory.util.AppUuid
import kotlin.time.Clock

/**
 * تمثيل جدول الكيان المكتمل للقطعة في قاعدة بيانات Room و SQLite (PartEntity) المعتمد بنسبة 100% على UUIDv7.
 * يحتوي على كافة حقول التوجيه والتصنيف والقفل المتفائل وتتبع الأجهزة.
 */
@Entity(tableName = "parts")
data class PartEntity(
    @PrimaryKey
    override val uuid: String = AppUuid.generate(),
    val name: String,
    val ipn: String = "",
    val description: String = "",
    val categoryUuid: String? = null,
    val units: String = "pcs",
    val minimumStock: Double = 0.0,
    val maximumStock: Double? = null,
    val totalInStock: Double = 0.0,
    val revision: String = "",
    val keywords: String = "",
    val assembly: Boolean = false,
    val component: Boolean = true,
    val isTemplate: Boolean = false,
    val variantOfUuid: String? = null,
    val trackable: Boolean = false,
    val purchaseable: Boolean = true,
    val salable: Boolean = false,
    val virtual: Boolean = false,
    val active: Boolean = true,
    val locked: Boolean = false,
    val defaultLocationUuid: String? = null,
    val defaultExpiryDays: Int? = null,
    val link: String = "",
    val localImagePath: String? = null,
    val metadata: String = "{}",
    val version: Int = 1,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val lastModifiedByDeviceUuid: String? = null
) : SyncableEntity
