package com.streamvault.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(context: Context) {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
    private val KEY_ALIAS = "streamvault_token_key"
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val IV_SIZE = 12
    private val TAG_SIZE = 128

    init {
        ensureKey()
    }

    private fun ensureKey() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(
                    KeyGenParameterSpec.Builder(KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                generateKey()
            }
        }
    }

    private fun getKey(): SecretKey =
        (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey

    fun encryptToken(plainToken: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv         = cipher.iv
        val encrypted  = cipher.doFinal(plainToken.toByteArray(Charsets.UTF_8))
        val combined   = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptToken(encToken: String): String {
        return runCatching {
            val combined  = Base64.decode(encToken, Base64.NO_WRAP)
            val iv        = combined.copyOfRange(0, IV_SIZE)
            val encrypted = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher    = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(TAG_SIZE, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrElse { encToken }
    }
}
