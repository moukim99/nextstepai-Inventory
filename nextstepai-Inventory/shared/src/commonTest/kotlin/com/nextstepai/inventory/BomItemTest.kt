package com.nextstepai.inventory

import com.nextstepai.inventory.data.BomItem
import com.nextstepai.inventory.data.BomItemTable
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
    fun testBomBatchSyncWithCloudflare() = runBlocking {
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
