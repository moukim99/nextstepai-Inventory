package com.nextstepai.inventory

import com.nextstepai.inventory.data.Part
import com.nextstepai.inventory.data.PartCategory
import com.nextstepai.inventory.ui.components.Code128Encoder
import com.nextstepai.inventory.ui.components.SmartIpnGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartIpnAndBarcode128Test {

    @Test
    fun testSmartIpnAutoIncrementWithExistingPattern() {
        val catId = 10L
        val categories = listOf(PartCategory(id = catId, name = "Resistors"))
        val existingParts = listOf(
            Part(id = 1, name = "Resistor 10K", categoryId = catId, ipn = "ELEC-RES-0041"),
            Part(id = 2, name = "Resistor 100K", categoryId = catId, ipn = "ELEC-RES-0042")
        )

        val nextIpn = SmartIpnGenerator.generateNextIpn(
            categoryId = catId,
            allParts = existingParts,
            categories = categories
        )

        assertEquals("ELEC-RES-0043", nextIpn)
    }

    @Test
    fun testSmartIpnCollisionAvoidance() {
        val catId = 10L
        val categories = listOf(PartCategory(id = catId, name = "Resistors"))
        val existingParts = listOf(
            Part(id = 1, name = "Resistor 10K", categoryId = catId, ipn = "ELEC-RES-0041"),
            Part(id = 2, name = "Resistor 100K", categoryId = catId, ipn = "ELEC-RES-0042"),
            Part(id = 3, name = "Resistor 1K", categoryId = catId, ipn = "ELEC-RES-0043") // Collision candidate
        )

        val nextIpn = SmartIpnGenerator.generateNextIpn(
            categoryId = catId,
            allParts = existingParts,
            categories = categories
        )

        assertEquals("ELEC-RES-0044", nextIpn)
    }

    @Test
    fun testSmartIpnCategoryPrefixFallback() {
        val catId = 20L
        val categories = listOf(PartCategory(id = catId, name = "Microcontrollers"))
        val existingParts = emptyList<Part>()

        val nextIpn = SmartIpnGenerator.generateNextIpn(
            categoryId = catId,
            allParts = existingParts,
            categories = categories
        )

        assertEquals("MICR-0001", nextIpn)
    }

    @Test
    fun testSmartIpnNeutralFallback() {
        val nextIpn = SmartIpnGenerator.generateNextIpn(
            categoryId = null,
            allParts = emptyList(),
            categories = emptyList()
        )

        assertEquals("GEN-0001", nextIpn)
    }

    @Test
    fun testCode128EncoderPatternGeneration() {
        val pattern = Code128Encoder.generateBarPattern("ELEC-RES-0042")
        assertTrue(pattern.isNotEmpty(), "Bar pattern should not be empty")
        assertTrue(pattern.any { it }, "Should contain true (bars)")
        assertTrue(pattern.any { !it }, "Should contain false (spaces)")
    }
}
