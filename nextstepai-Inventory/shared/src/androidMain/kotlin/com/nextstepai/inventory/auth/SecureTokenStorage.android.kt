package com.nextstepai.inventory.auth

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.nextstepai.inventory.data.db.AppContextHolder
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

actual class SecureTokenStorage actual constructor() {

    private val context: Context
        get() = AppContextHolder.appContext
            ?: throw IllegalStateException("AppContextHolder.appContext must be initialized on Android")

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { File(context.filesDir, "datastore/secure_tokens.preferences_pb") }
    )

    private val accessTokenKey = stringPreferencesKey("encrypted_access_token")
    private val refreshTokenKey = stringPreferencesKey("encrypted_refresh_token")

    private fun getAead(): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "tink_keyset_prefs", "tink_master_key_preference")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://_nextstepai_master_key_")
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(Aead::class.java)
    }

    actual suspend fun saveTokens(tokens: AuthTokens) {
        val aead = getAead()
        val encryptedAccess = Base64.encodeToString(aead.encrypt(tokens.accessToken.toByteArray(Charsets.UTF_8), null), Base64.NO_WRAP)
        val encryptedRefresh = Base64.encodeToString(aead.encrypt(tokens.refreshToken.toByteArray(Charsets.UTF_8), null), Base64.NO_WRAP)

        dataStore.edit { prefs ->
            prefs[accessTokenKey] = encryptedAccess
            prefs[refreshTokenKey] = encryptedRefresh
        }
    }

    actual suspend fun getTokens(): AuthTokens? {
        val prefs = dataStore.data.firstOrNull() ?: return null
        val encAccess = prefs[accessTokenKey] ?: return null
        val encRefresh = prefs[refreshTokenKey] ?: return null

        return try {
            val aead = getAead()
            val access = String(aead.decrypt(Base64.decode(encAccess, Base64.NO_WRAP), null), Charsets.UTF_8)
            val refresh = String(aead.decrypt(Base64.decode(encRefresh, Base64.NO_WRAP), null), Charsets.UTF_8)
            AuthTokens(accessToken = access, refreshToken = refresh)
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun clearTokens() {
        dataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
        }
    }
}
