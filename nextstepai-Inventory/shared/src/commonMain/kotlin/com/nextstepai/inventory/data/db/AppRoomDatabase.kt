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
        StockLocationEntity::class,
        StockItemTrackingEntity::class,
        StockItemTestResultEntity::class,
        StockItemAttachmentEntity::class,
        CompanyEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderLineEntity::class,
        BuildOrderEntity::class,
        BuildOrderLineItemEntity::class,
        BuildItemEntity::class,
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
        PartSalePriceEntity::class,
        NotificationHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SyncStatusConverter::class)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun partDao(): PartDao
    abstract fun bomItemDao(): BomItemDao
    abstract fun stockItemDao(): StockItemDao
    abstract fun stockLocationDao(): StockLocationDao
    abstract fun stockItemTrackingDao(): StockItemTrackingDao
    abstract fun stockItemTestResultDao(): StockItemTestResultDao
    abstract fun stockItemAttachmentDao(): StockItemAttachmentDao
    abstract fun companyDao(): CompanyDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun buildOrderDao(): BuildOrderDao
    abstract fun buildOrderLineItemDao(): BuildOrderLineItemDao
    abstract fun buildItemDao(): BuildItemDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao
}
