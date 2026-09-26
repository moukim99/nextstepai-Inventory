package com.nextstepai.inventory.auth

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.measureTime

class SecureTokenStorageJvmTest {

    @Test
    fun testSaveGetAndClearTokensWithPerformance() = runBlocking {
        val storage = SecureTokenStorage()

        val tokens = AuthTokens(
            accessToken = "test_access_token_123",
            refreshToken = "test_refresh_token_456"
        )

        // Save tokens
        storage.saveTokens(tokens)

        // First fetch (populates/verifies cache)
        val retrieved1 = storage.getTokens()
        assertNotNull(retrieved1)
        assertEquals("test_access_token_123", retrieved1.accessToken)
        assertEquals("test_refresh_token_456", retrieved1.refreshToken)

        // Second fetch (should hit L1 cache sub-millisecond)
        val cacheDuration = measureTime {
            val retrieved2 = storage.getTokens()
            assertNotNull(retrieved2)
            assertEquals("test_access_token_123", retrieved2.accessToken)
        }
        println("Subsequent L1 cache token read duration: ${cacheDuration}")

        // Clear tokens
        storage.clearTokens()

        val retrievedAfterClear = storage.getTokens()
        assertNull(retrievedAfterClear)
    }
}
