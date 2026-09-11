package com.nextstepai.inventory.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PartEntity::class,
        PartCategoryEntity::class,
        BomItemEntity::class,
        BomItemSubstituteEntity::class,
        StockItemEntity::class,
        CompanyEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderLineEntity::class,
        BuildOrderEntity::class,
        PartParameterTemplateEntity::class,
        PartCategoryParameterTemplateEntity::class,
        PartParameterEntity::class,
        PartRelatedEntity::class,
        PartTestTemplateEntity::class,
        PartAttachmentEntity::class,
        PartNotesEntity::class,
        PartPricingEntity::class,
        PartInternalPriceEntity::class,
        PartStarEntity::class,
        PartSalePriceEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SyncStatusConverter::class)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun partDao(): PartDao
    abstract fun bomItemDao(): BomItemDao
    abstract fun stockItemDao(): StockItemDao
    abstract fun companyDao(): CompanyDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun buildOrderDao(): BuildOrderDao
}
