package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.db.PartDao
import com.nextstepai.inventory.data.db.PartEntity
import com.nextstepai.inventory.repository.PartAllocationRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * اختبارات الوحدة لمنظومة تتبع وإدارة مخصصات وحجوزات المخزون الذكية (Part Allocations & Commitments).
 */
class PartAllocationTest {

    @Test
    fun testLiveBalancesFormulaAndAvailableStock() {
        val part = Part(
            id = 100L,
            name = "متحكم ESP32",
            ipn = "MCU-ESP32-001",
            units = "pcs",
            totalInStock = 8.0,
            totalHardAllocated = 2.0, // Committed
            totalSoftAllocated = 1.0,  // Reserved (does not deduct from net available)
            minimumStock = 10.0
        )

        // Net Available = Total On-Hand - Committed Allocated = 8.0 - 2.0 = 6.0
        assertEquals(8.0, part.totalInStock)
        assertEquals(2.0, part.committedAllocated)
        assertEquals(6.0, part.availableStock)

        // Minimum stock buffer alert: Available (6.0) < minStockLimit (10.0)
        assertTrue(part.isLowStock)
    }

    @Test
    fun testSufficientStockDoesNotTriggerLowStockAlert() {
        val part = Part(
            id = 101L,
            name = "مقاومة 10k",
            units = "pcs",
            totalInStock = 100.0,
            totalHardAllocated = 10.0,
            totalSoftAllocated = 5.0,
            minimumStock = 50.0
        )

        // Net Available = 100.0 - 10.0 = 90.0
        assertEquals(90.0, part.availableStock)
        assertFalse(part.isLowStock)
    }

    @Test
    fun testPartAllocationDaoAndRepositoryWorkflow() = runBlocking {
        val partDao = PartDao()
        val repo = PartAllocationRepository()
        val partId = 777L

        // Insert part into parts table to satisfy foreign key constraint
        partDao.insertOrUpdate(
            PartEntity(
                uuid = "part-777",
                name = "قطعة تجريبية للحجز",
                ipn = "TEST-PART-777",
                totalInStock = 50.0,
                minimumStock = 10.0
            )
        )

        // 1. Create a hard allocation
        val result = repo.createAllocation(
            partId = partId,
            quantity = 4.0,
            type = "HARD",
            refType = "BUILD_ORDER",
            refId = "BO-2026-999",
            refTitle = "أمر إنتاج شاشة OLED",
            userId = "مهندس علي"
        )
        assertTrue(result.isSuccess)
        val allocId = result.getOrThrow()
        assertTrue(allocId > 0)

        // 2. Query active allocations
        val activeAllocations = repo.getActiveAllocationsForPart(partId)
        val target = activeAllocations.find { it.id == allocId }
        assertTrue(target != null)
        assertEquals(4.0, target.allocatedQuantity)
        assertEquals("HARD", target.allocationType)
        assertEquals("BUILD_ORDER", target.referenceType)
        assertEquals("ACTIVE", target.status)

        // 3. Release allocation
        val releaseResult = repo.releaseAllocation(allocId, userId = "مدير المستودع")
        assertTrue(releaseResult.isSuccess)
        assertTrue(releaseResult.getOrThrow())

        // 4. Verify allocation status changed to RELEASED and is not re-seeded
        val updatedAllocations = repo.getActiveAllocationsForPart(partId)
        val releasedTarget = updatedAllocations.find { it.id == allocId }
        assertEquals(null, releasedTarget) // Should no longer be in active allocations list
        assertEquals(0, updatedAllocations.size) // No re-seeding occurs when empty

        // 5. Verify committed quantity is 0.0
        val committedQty = repo.getCommittedQuantity(partId)
        assertEquals(0.0, committedQty)
    }
}
