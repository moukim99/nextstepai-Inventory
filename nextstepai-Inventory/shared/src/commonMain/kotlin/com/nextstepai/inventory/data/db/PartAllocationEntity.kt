package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Clock

/**
 * تمثيل كيان حجز ومخصصات القطعة (PartAllocationEntity) في قاعدة البيانات المحلية.
 * يدعم الحجز العام المرن (General Soft/Hard Allocation) ويربط القطعة بالوثائق المرجعية
 * (أوامر الإنتاج BUILD_ORDER، أوامر البيع SALES_ORDER، حجز الفحص QC_HOLD، أوامر الإرجاع RETURN_ORDER).
 */
@Entity(
    tableName = "part_allocations",
    indices = [
        Index(value = ["partId", "status"]),
        Index(value = ["referenceType", "referenceId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PartAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partId: Long,
    val allocatedQuantity: Double,
    val allocationType: String, // "HARD" (Committed) or "SOFT" (Reserved)
    val referenceType: String,  // "BUILD_ORDER", "SALES_ORDER", "QC_HOLD", "RETURN_ORDER"
    val referenceId: String,    // Document Code e.g. "BO-2026-089"
    val referenceTitle: String, // Brief title / description
    val status: String,         // "ACTIVE", "RELEASED", "CONSUMED"
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val createdByUserId: String = "1",
    val notes: String? = null
)
