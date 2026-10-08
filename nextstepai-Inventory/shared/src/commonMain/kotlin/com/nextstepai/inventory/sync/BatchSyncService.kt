package com.nextstepai.inventory.sync

import com.nextstepai.inventory.auth.AuthTokens
import com.nextstepai.inventory.auth.AuthenticationException
import com.nextstepai.inventory.auth.SecureTokenStorage
import com.nextstepai.inventory.util.AppUuid
import kotlinx.coroutines.delay
import kotlin.time.Clock

/**
 * استثناء خطأ المزامنة السحابية.
 */
class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * واجهة الاتصال بمصدر البيانات البعيد (Cloudflare Worker أو الخادم البعيد).
 */
interface SyncRemoteDataSource {
    suspend fun sendBatch(request: BatchSyncRequest, token: String): BatchSyncResponse
}

/**
 * خادم المزامنة الافتراضي لمحاكاة وإدارة التغييرات البعيدة مع مفاتيح عدم التكرار والتحقق من الرموز.
 */
class DefaultSyncRemoteDataSource : SyncRemoteDataSource {
    private val remoteRecords = mutableMapOf<String, SyncPayload>()
    private val responseCache = mutableMapOf<String, BatchSyncResponse>()

    fun stageRemoteChange(payload: SyncPayload) {
        remoteRecords[payload.uuid] = payload
    }

    fun clear() {
        remoteRecords.clear()
        responseCache.clear()
    }

    override suspend fun sendBatch(request: BatchSyncRequest, token: String): BatchSyncResponse {
        require(token.isNotBlank()) { "رمز الوصول JWT مطلوب لمصادقة طلب المزامنة" }

        // التحقق من مفتاح عدم التكرار (Idempotency Key)
        if (request.idempotencyKey.isNotBlank()) {
            val cached = responseCache[request.idempotencyKey]
            if (cached != null) {
                return cached
            }
        }

        val currentTime = Clock.System.now().toEpochMilliseconds()
        val accepted = mutableListOf<String>()

        // تطبيق وحفظ السجلات المدفوعة من العميل
        for (push in request.pendingPushes) {
            remoteRecords[push.uuid] = push.copy(updatedAt = currentTime)
            accepted.add(push.uuid)
        }

        val pushedUuids = request.pendingPushes.map { it.uuid }.toSet()

        // سحب السجلات البعيدة التي تم تعديلها بعد آخر طابع زمني للمزامنة ولم يدفعها العميل في هذه الدفعة
        val remoteChanges = remoteRecords.values.filter { payload ->
            payload.uuid !in pushedUuids && payload.updatedAt > request.lastSyncTimestamp
        }

        val response = BatchSyncResponse(
            acceptedUuids = accepted,
            remoteChanges = remoteChanges,
            serverTimestamp = currentTime
        )

        if (request.idempotencyKey.isNotBlank()) {
            responseCache[request.idempotencyKey] = response
        }

        return response
    }
}

/**
 * خدمة المزامنة الدفعية (Batch Sync Service) المخصصة للتواصل مع Cloudflare Worker.
 * تقوم بدفع وسحب التغييرات المعلّقة في طلب شبكة واحد (Single HTTP Batch Request)
 * مع المصادقة باستخدام رمز الوصول JWT الصريح، وإعادة المحاولة مع التراجع الأسي (Exponential Backoff)،
 * وضمان عدم التكرار (Idempotency).
 */
class BatchSyncService(
    private val workerEndpointUrl: String = "https://api.inventory.nextstepai.com/sync/batch",
    private val tokenStorage: SecureTokenStorage = SecureTokenStorage(),
    private val remoteDataSource: SyncRemoteDataSource = sharedRemoteDataSource
) {
    companion object {
        val sharedRemoteDataSource = DefaultSyncRemoteDataSource()
    }

    /**
     * تنفيذ طلب المزامنة الدفعية المجمع لعدة سجلات معاً مع حماية بالرمز المميز JWT.
     *
     * @param pendingPushes قائمة السجلات المعلّقة المحليّة المراد رفعها إلى السحابة.
     * @param lastSyncTimestamp أحدث طابع زمني للمزامنة السابقة لسحب السجلات المحدثة سحابياً.
     * @param maxRetries الحد الأقصى لعدد محاولات إعادة الاتصال عند الأخطاء المؤقتة.
     * @param initialDelayMs التأخير الزمني الأولي قبل إعادة المحاولة (بالميللي ثانية).
     * @param backoffFactor معامل التراجع الأسي لمضاعفة وقت الانتظار.
     * @return استجابة المزامنة التي تحتوي على المعرفات المقبولة والتغيرات البعيدة.
     */
    suspend fun performBatchSync(
        pendingPushes: List<SyncPayload>,
        lastSyncTimestamp: Long,
        maxRetries: Int = 3,
        initialDelayMs: Long = 20L,
        backoffFactor: Double = 2.0
    ): BatchSyncResponse {
        val tokens = tokenStorage.getTokens() ?: throw AuthenticationException(
            "لم يتم العثور على رمز مصادقة صالح. يرجى تسجيل الدخول أولاً."
        )

        if (tokens.accessToken.isBlank()) {
            throw AuthenticationException("رمز المصادقة فارغ أو غير صالح.")
        }

        val idempotencyKey = AppUuid.generate()
        val request = BatchSyncRequest(
            pendingPushes = pendingPushes,
            lastSyncTimestamp = lastSyncTimestamp,
            idempotencyKey = idempotencyKey
        )

        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        for (attempt in 1..maxRetries) {
            try {
                return remoteDataSource.sendBatch(request, tokens.accessToken)
            } catch (e: AuthenticationException) {
                // خطأ مصادقة صريح - لا يتم إعادة المحاولة
                throw e
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * backoffFactor).toLong()
                }
            }
        }

        throw SyncException("فشلت المزامنة بعد $maxRetries محاولات: ${lastException?.message}", lastException)
    }
}
