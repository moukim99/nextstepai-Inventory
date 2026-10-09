package com.nextstepai.inventory

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemTable
import com.nextstepai.inventory.auth.AuthTokens
import com.nextstepai.inventory.auth.SecureTokenStorage
import com.nextstepai.inventory.repository.BomRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BomItemTest {

    @Test
    fun testAntiRecursionConstraint() {
        val bomTable = BomItemTable()

        // محاولة ربط القطعة بنفسها كمكون فرعي يجب أن تسبب استثناء (partId != subPartId)
        assertFailsWith<IllegalArgumentException> {
            bomTable.insertBomItem(
                BomItem(
                    partId = 5L,
                    subPartId = 5L,
                    quantity = 1.0
                )
            )
        }
    }

    @Test
    fun testUniquenessConstraintOnPartAndSubPart() {
        val bomTable = BomItemTable()

        bomTable.insertBomItem(
            BomItem(
                partId = 10L,
                subPartId = 20L,
                quantity = 2.0
            )
        )

        // محاولة إضافة نفس المكون الفرعي لنفس المنتج الأب مجدداً يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            bomTable.insertBomItem(
                BomItem(
                    partId = 10L,
                    subPartId = 20L,
                    quantity = 5.0
                )
            )
        }
    }

    @Test
    fun testPositiveQuantityConstraint() {
        val bomTable = BomItemTable()

        // كمية صفر أو سالبة يجب أن تطلق استثناء
        assertFailsWith<IllegalArgumentException> {
            bomTable.insertBomItem(
                BomItem(
                    partId = 10L,
                    subPartId = 30L,
                    quantity = 0.0
                )
            )
        }
    }

    @Test
    fun testBomSubstituteConstraintsAndCascadeDelete() {
        val bomTable = BomItemTable()

        val item = bomTable.insertBomItem(
            BomItem(
                partId = 100L,
                subPartId = 200L,
                quantity = 1.0
            )
        )

        // 1. إضافة قطعة بديلة أولى صحيحة (300L)
        val sub1 = bomTable.insertSubstitute(
            bomItemId = item.id,
            substitutePartId = 300L
        )
        assertTrue(sub1.id > 0)

        // 2. محاولة إضافة نفس البديل لنفس البند يجب أن تفشل لقيد التفرد (unique_together)
        assertFailsWith<IllegalArgumentException> {
            bomTable.insertSubstitute(
                bomItemId = item.id,
                substitutePartId = 300L
            )
        }

        // 3. محاولة إضافة المكون الأساسي كبديل لنفسه يجب أن تفشل (Self-Substitution Prevention)
        assertFailsWith<IllegalArgumentException> {
            bomTable.insertSubstitute(
                bomItemId = item.id,
                substitutePartId = 200L
            )
        }

        // 4. محاولة إضافة القطعة التجميعية الأصل كبديل يجب أن تفشل
        assertFailsWith<IllegalArgumentException> {
            bomTable.insertSubstitute(
                bomItemId = item.id,
                substitutePartId = 100L
            )
        }

        // 5. الحذف المتتابع CASCADE عند مسح بند قائمة المواد الأصلي
        bomTable.deleteBomItem(item.id)
        val substitutesAfterDelete = bomTable.getSubstitutesForBomItem(item.id)
        assertTrue(substitutesAfterDelete.isEmpty())
    }

    @Test
    fun testBomBatchSyncWithCloudflare() = runBlocking {
        SecureTokenStorage().saveTokens(AuthTokens("test-access-token", "test-refresh-token"))
        val repository = BomRepository()
        repository.addBomItem(
            BomItem(
                partId = 100L,
                subPartId = 200L,
                quantity = 3.0,
                reference = "R5"
            )
        )

        val syncedCount = repository.syncPendingBomChanges()
        assertTrue(syncedCount > 0)
    }
}
