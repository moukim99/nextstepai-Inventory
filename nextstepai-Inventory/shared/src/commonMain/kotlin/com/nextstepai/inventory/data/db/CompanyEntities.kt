package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextstepai.inventory.sync.SyncStatus
import com.nextstepai.inventory.sync.SyncableEntity
import kotlin.time.Clock

/**
 * تمثيل كيان جهة الاتصال (ContactEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    override val uuid: String,
    val companyUuid: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val role: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity

/**
 * تمثيل كيان مرفقات الشركات العامة (CompanyAttachmentEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "company_attachments")
data class CompanyAttachmentEntity(
    @PrimaryKey
    override val uuid: String,
    val companyUuid: String,
    val attachmentPath: String = "",
    val link: String = "",
    val comment: String = "",
    val uploadDate: Long = Clock.System.now().toEpochMilliseconds(),
    val userId: Long? = 1L,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity

/**
 * تمثيل كيان العنوان (AddressEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey
    override val uuid: String,
    val companyUuid: String,
    val title: String = "الفرع الرئيسي",
    val isPrimary: Boolean = false,
    val line1: String,
    val line2: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val shippingNotes: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity

/**
 * تمثيل كيان قطعة المصنّع الأصلي (ManufacturerPartEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "manufacturer_parts")
data class ManufacturerPartEntity(
    @PrimaryKey
    override val uuid: String,
    val partUuid: String,
    val manufacturerUuid: String,
    val mpn: String,
    val description: String = "",
    val link: String = "",
    val metadata: String = "{}",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity

/**
 * تمثيل كيان الخصائص والمعاملات الفنية لقطعة المصنّع (ManufacturerPartParameterEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "manufacturer_part_parameters")
data class ManufacturerPartParameterEntity(
    @PrimaryKey
    override val uuid: String,
    val manufacturerPartUuid: String,
    val name: String,
    val value: String,
    val units: String = "",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity

/**
 * تمثيل كيان مرفق قطعة المصنّع (ManufacturerPartAttachmentEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "manufacturer_part_attachments")
data class ManufacturerPartAttachmentEntity(
    @PrimaryKey
    override val uuid: String,
    val manufacturerPartUuid: String,
    val attachmentPath: String = "",
    val link: String = "",
    val comment: String = "",
    val uploadDate: Long = Clock.System.now().toEpochMilliseconds(),
    val userId: Long? = 1L,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity

/**
 * تمثيل كيان قطعة المورد (SupplierPartEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "supplier_parts")
data class SupplierPartEntity(
    @PrimaryKey
    override val uuid: String,
    val partUuid: String,
    val supplierUuid: String,
    val sku: String,
    val manufacturerPartUuid: String? = null,
    val description: String = "",
    val link: String = "",
    val note: String = "",
    val packaging: String = "",
    val packQuantity: String = "1",
    val availableForPurchase: Boolean = true,
    val active: Boolean = true,
    val metadata: String = "{}",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity

/**
 * تمثيل كيان شريحة سعر المورد (SupplierPriceBreakEntity) في قاعدة البيانات المحلية.
 */
@Entity(tableName = "supplier_price_breaks")
data class SupplierPriceBreakEntity(
    @PrimaryKey
    override val uuid: String,
    val supplierPartUuid: String,
    val quantity: Double,
    val price: Double,
    val priceCurrency: String = "USD",
    val packQuantity: String = "1",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val isDeleted: Boolean = false,
    override val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) : SyncableEntity
