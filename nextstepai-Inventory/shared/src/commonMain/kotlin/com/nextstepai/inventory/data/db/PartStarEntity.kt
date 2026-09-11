package com.nextstepai.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * تمثيل كيان تفضيل ومتابعة القطع (PartStarEntity) المستوحى من InvenTree.
 *
 * @property id المعرف الرقمي الفريد لسجل التفضيل والمتابعة
 * @property partId معرف القطعة المستهدفة بالمتابعة (Part)
 * @property userId معرف المستخدم الذي قام بتمييز القطعة (User)
 */
@Entity(tableName = "part_stars")
data class PartStarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val userId: Long = 1L
)
