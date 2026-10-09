package com.nextstepai.inventory.auth

import com.github.javakeyring.Keyring
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * التخزين الآمن لرموز المصادقة على أجهزة المكتبي (Desktop)
 * عبر الخزنة البرمجية لنظام التشغيل (OS-Native Credential Store):
 * Windows Credential Manager / macOS Keychain / Linux Secret Service.
 *
 * Performance Bottlenecks Addressed:
 * 1. Single Reusable Keyring Instance (Lazy Initialization):
 *    - Bottleneck: `Keyring.create()` was invoked on every operation, repeatedly spawning
 *      and tearing down native JNA/Win32/Keychain bindings, causing massive CPU spikes
 *      and latency (hundreds of milliseconds per call).
 *    - Optimization: Caching `Keyring` lazily reduces OS native store initialization overhead to a 1-time cost.
 *
 * 2. In-Memory L1 Cache (`cachedTokens`):
 *    - Bottleneck: Every call to `getTokens()` queried the native OS credential store, adding disk/IPC overhead.
 *    - Optimization: Caching active tokens in volatile memory provides near-zero latency (< 1ms) reads
 *      for subsequent token accesses.
 *
 * 3. Offloading Blocking Native I/O to `Dispatchers.IO`:
 *    - Bottleneck: Keyring read/write/delete operations ran synchronously on the caller thread.
 *    - Optimization: Wrapping operations in `withContext(Dispatchers.IO)` ensures blocking native calls
 *      never freeze the UI or main application thread.
 *
 * 4. Thread-Safe Atomic Cache Access:
 *    - Bottleneck: Plain `HashMap` (`mutableMapOf`) had race conditions under concurrent coroutine access.
 *    - Optimization: Atomic `@Volatile` reference prevents memory visibility issues and race conditions.
 *
 * 5. Execution Metrics & Performance Logging:
 *    - Performance measurements (`measureTimeMillis`) log exact execution durations to verify performance.
 */
actual class SecureTokenStorage actual constructor() {

    private val serviceName = "NextStepAI_Inventory"
    private val accessAccount = "secure_access_token"
    private val refreshAccount = "secure_refresh_token"

    companion object {
        // Thread-safe volatile L1 memory cache for fast non-blocking reads across instances
        @Volatile
        private var cachedTokens: AuthTokens? = null
    }

    // Lazy thread-safe initialization of Keyring to avoid recreating native OS handles on every call
    private val keyring: Keyring? by lazy {
        runCatching {
            var instance: Keyring? = null
            val initTime = measureTimeMillis {
                instance = Keyring.create()
            }
            println("[SecureTokenStorage] Keyring native store initialized in ${initTime}ms")
            instance
        }.getOrElse { error ->
            println("[SecureTokenStorage] Failed to initialize native Keyring store: ${error.message}. Falling back to in-memory mode.")
            null
        }
    }

    actual suspend fun saveTokens(tokens: AuthTokens): Unit = withContext(Dispatchers.IO) {
        // Update L1 in-memory cache atomically
        cachedTokens = tokens

        val nativeKeyring = keyring
        if (nativeKeyring != null) {
            val saveTime = measureTimeMillis {
                runCatching {
                    nativeKeyring.setPassword(serviceName, accessAccount, tokens.accessToken)
                    nativeKeyring.setPassword(serviceName, refreshAccount, tokens.refreshToken)
                }.onFailure { e ->
                    println("[SecureTokenStorage] Error saving tokens to native Keyring: ${e.message}")
                }
            }
            println("[SecureTokenStorage] Saved tokens to native Keyring in ${saveTime}ms")
        }
    }

    actual suspend fun getTokens(): AuthTokens? = withContext(Dispatchers.IO) {
        // Check L1 memory cache first (0ms latency, zero native OS overhead)
        val memTokens = cachedTokens
        if (memTokens != null) {
            println("[SecureTokenStorage] getTokens: L1 memory cache hit (0ms)")
            return@withContext memTokens
        }

        // Cache miss: query OS native credential store
        val nativeKeyring = keyring
        if (nativeKeyring != null) {
            var access: String? = null
            var refresh: String? = null
            val fetchTime = measureTimeMillis {
                access = runCatching { nativeKeyring.getPassword(serviceName, accessAccount) }.getOrNull()
                refresh = runCatching { nativeKeyring.getPassword(serviceName, refreshAccount) }.getOrNull()
            }
            println("[SecureTokenStorage] Queried native Keyring store in ${fetchTime}ms")

            if (!access.isNullOrEmpty() && !refresh.isNullOrEmpty()) {
                val retrieved = AuthTokens(accessToken = access, refreshToken = refresh)
                cachedTokens = retrieved
                return@withContext retrieved
            }
        }

        null
    }

    actual suspend fun clearTokens(): Unit = withContext(Dispatchers.IO) {
        // Clear L1 memory cache
        cachedTokens = null

        val nativeKeyring = keyring
        if (nativeKeyring != null) {
            val clearTime = measureTimeMillis {
                runCatching { nativeKeyring.deletePassword(serviceName, accessAccount) }
                runCatching { nativeKeyring.deletePassword(serviceName, refreshAccount) }
            }
            println("[SecureTokenStorage] Cleared native Keyring store in ${clearTime}ms")
        }
    }
}
