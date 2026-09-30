package com.nextstepai.inventory

import com.nextstepai.inventory.util.AppUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppUuidTest {

    @Test
    fun testGenerateUuidv7Format() {
        val uuid = AppUuid.generate()
        assertEquals(36, uuid.length)
        val regex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
        assertTrue(regex.matches(uuid), "Generated UUID should be valid UUIDv7 format, got: $uuid")
    }

    @Test
    fun testUuidv7IsTimeOrdered() {
        val uuid1 = AppUuid.generate()
        val uuid2 = AppUuid.generate()
        assertTrue(uuid1 <= uuid2, "UUIDv7 generated first ($uuid1) should be <= second ($uuid2)")
    }
}
