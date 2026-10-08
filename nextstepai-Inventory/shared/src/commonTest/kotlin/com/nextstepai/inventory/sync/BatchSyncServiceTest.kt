package com.nextstepai.inventory.sync

import com.nextstepai.inventory.auth.AuthTokens
import com.nextstepai.inventory.auth.AuthenticationException
import com.nextstepai.inventory.auth.SecureTokenStorage
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchSyncServiceTest {

    private lateinit var tokenStorage: SecureTokenStorage
    private lateinit var syncDataSource: DefaultSyncRemoteDataSource
    private lateinit var batchSyncService: BatchSyncService

    @BeforeTest
    fun setUp() {
        runBlocking {
            tokenStorage = SecureTokenStorage()
            tokenStorage.clearTokens()
            syncDataSource = DefaultSyncRemoteDataSource()
            syncDataSource.clear()
            batchSyncService = BatchSyncService(
                tokenStorage = tokenStorage,
                remoteDataSource = syncDataSource
            )
        }
    }

    @Test
    fun testBatchSyncRejectsMissingAuthTokens() {
        runBlocking {
            // التحقق من رمي AuthenticationException صراحة عند غياب الرموز
            val payloads = listOf(
                SyncPayload("part-1", "Part", "{}", false, 1000L)
            )

            assertFailsWith<AuthenticationException> {
                batchSyncService.performBatchSync(payloads, 0L)
            }
        }
    }

    @Test
    fun testBatchSyncRejectsBlankAuthToken() {
        runBlocking {
            tokenStorage.saveTokens(AuthTokens(accessToken = "   ", refreshToken = "refresh"))

            val payloads = listOf(
                SyncPayload("part-1", "Part", "{}", false, 1000L)
            )

            assertFailsWith<AuthenticationException> {
                batchSyncService.performBatchSync(payloads, 0L)
            }
        }
    }

    @Test
    fun testBatchSyncSucceedsWithValidTokens() {
        runBlocking {
            tokenStorage.saveTokens(AuthTokens(accessToken = "valid-token-xyz", refreshToken = "valid-refresh"))

            val payloads = listOf(
                SyncPayload("part-10", "Part", "{\"name\":\"Resistor\"}", false, 2000L),
                SyncPayload("part-20", "Part", "{\"name\":\"Capacitor\"}", false, 2001L)
            )

            val response = batchSyncService.performBatchSync(payloads, 1000L)

            assertEquals(2, response.acceptedUuids.size)
            assertTrue(response.acceptedUuids.contains("part-10"))
            assertTrue(response.acceptedUuids.contains("part-20"))
            assertTrue(response.serverTimestamp > 0L)
        }
    }

    @Test
    fun testBatchSyncIdempotencyReturnsCachedResponse() {
        runBlocking {
            tokenStorage.saveTokens(AuthTokens(accessToken = "valid-token", refreshToken = "refresh"))

            val request = BatchSyncRequest(
                pendingPushes = listOf(SyncPayload("part-idem-1", "Part", "{}", false, 1000L)),
                lastSyncTimestamp = 500L,
                idempotencyKey = "idempotency-key-fixed-123"
            )

            val resp1 = syncDataSource.sendBatch(request, "valid-token")
            val resp2 = syncDataSource.sendBatch(request, "valid-token")

            assertEquals(resp1.serverTimestamp, resp2.serverTimestamp)
            assertEquals(resp1.acceptedUuids, resp2.acceptedUuids)
        }
    }

    @Test
    fun testBatchSyncRetriesOnTransientErrorWithBackoff() {
        runBlocking {
            tokenStorage.saveTokens(AuthTokens(accessToken = "token", refreshToken = "refresh"))

            var attempts = 0
            val flakingDataSource = object : SyncRemoteDataSource {
                override suspend fun sendBatch(request: BatchSyncRequest, token: String): BatchSyncResponse {
                    attempts++
                    if (attempts == 1) {
                        throw RuntimeException("خطأ شبكة مؤقت")
                    }
                    return BatchSyncResponse(
                        acceptedUuids = request.pendingPushes.map { it.uuid },
                        remoteChanges = emptyList(),
                        serverTimestamp = 5000L
                    )
                }
            }

            val service = BatchSyncService(
                tokenStorage = tokenStorage,
                remoteDataSource = flakingDataSource
            )

            val response = service.performBatchSync(
                pendingPushes = listOf(SyncPayload("retry-item", "Part", "{}", false, 100L)),
                lastSyncTimestamp = 0L,
                maxRetries = 3,
                initialDelayMs = 10L
            )

            assertEquals(2, attempts)
            assertEquals(listOf("retry-item"), response.acceptedUuids)
        }
    }

    @Test
    fun testBatchSyncFailsSafelyAfterMaxRetries() {
        runBlocking {
            tokenStorage.saveTokens(AuthTokens(accessToken = "token", refreshToken = "refresh"))

            val brokenDataSource = object : SyncRemoteDataSource {
                override suspend fun sendBatch(request: BatchSyncRequest, token: String): BatchSyncResponse {
                    throw RuntimeException("الخادم غير متاح (503 Service Unavailable)")
                }
            }

            val service = BatchSyncService(
                tokenStorage = tokenStorage,
                remoteDataSource = brokenDataSource
            )

            assertFailsWith<SyncException> {
                service.performBatchSync(
                    pendingPushes = listOf(SyncPayload("fail-item", "Part", "{}", false, 100L)),
                    lastSyncTimestamp = 0L,
                    maxRetries = 2,
                    initialDelayMs = 5L
                )
            }
        }
    }

    @Test
    fun testConflictResolutionLastWriteWins() {
        val localOlder = SyncPayload("part-conflict", "Part", "{\"name\":\"Old Name\"}", false, 1000L)
        val remoteNewer = SyncPayload("part-conflict", "Part", "{\"name\":\"New Remote Name\"}", false, 2000L)

        val winning = SyncConflictResolver.resolve(localOlder, remoteNewer, ConflictResolutionStrategy.LAST_WRITE_WINS)
        assertEquals(remoteNewer.payloadJson, winning.payloadJson)

        val localNewer = SyncPayload("part-conflict", "Part", "{\"name\":\"Local Edit\"}", false, 3000L)
        val winningLocal = SyncConflictResolver.resolve(localNewer, remoteNewer, ConflictResolutionStrategy.LAST_WRITE_WINS)
        assertEquals(localNewer.payloadJson, winningLocal.payloadJson)
    }

    @Test
    fun testConflictResolutionClientAndServerStrategies() {
        val local = SyncPayload("item-1", "Stock", "{\"qty\":10}", false, 1000L)
        val remote = SyncPayload("item-1", "Stock", "{\"qty\":20}", false, 2000L)

        val clientWins = SyncConflictResolver.resolve(local, remote, ConflictResolutionStrategy.CLIENT_WINS)
        assertEquals(local.payloadJson, clientWins.payloadJson)

        val serverWins = SyncConflictResolver.resolve(local, remote, ConflictResolutionStrategy.SERVER_WINS)
        assertEquals(remote.payloadJson, serverWins.payloadJson)
    }

    @Test
    fun testPullRemoteChangesExcludesSelfPushes() {
        runBlocking {
            tokenStorage.saveTokens(AuthTokens(accessToken = "token", refreshToken = "refresh"))

            // إضافة سجل سحابي مسبق تم تعديله في وقت لاحق
            syncDataSource.stageRemoteChange(
                SyncPayload("remote-only-uuid", "Company", "{\"name\":\"External Co\"}", false, 5000L)
            )

            val response = batchSyncService.performBatchSync(
                pendingPushes = listOf(SyncPayload("local-push-uuid", "Part", "{}", false, 2000L)),
                lastSyncTimestamp = 4000L
            )

            assertEquals(1, response.acceptedUuids.size)
            assertEquals("local-push-uuid", response.acceptedUuids.first())

            assertEquals(1, response.remoteChanges.size)
            assertEquals("remote-only-uuid", response.remoteChanges.first().uuid)
        }
    }

    @Test
    fun testDeletionTombstoneSync() {
        runBlocking {
            tokenStorage.saveTokens(AuthTokens(accessToken = "token", refreshToken = "refresh"))

            val deletePayload = SyncPayload(
                uuid = "deleted-part-uuid",
                entityType = "Part",
                payloadJson = "{}",
                isDeleted = true,
                updatedAt = 8000L
            )

            val response = batchSyncService.performBatchSync(listOf(deletePayload), 0L)
            assertTrue(response.acceptedUuids.contains("deleted-part-uuid"))

            // التحقق من سحب التغيير المحذوف من عميل آخر
            val anotherSync = syncDataSource.sendBatch(
                BatchSyncRequest(emptyList(), lastSyncTimestamp = 7000L, idempotencyKey = "sync-2"),
                "token"
            )
            val fetchedRemote = anotherSync.remoteChanges.firstOrNull { it.uuid == "deleted-part-uuid" }
            assertTrue(fetchedRemote != null && fetchedRemote.isDeleted)
        }
    }
}
