package com.cross.sync.syncing.network.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesGcmCryptoEngine : CryptoEngine {
    override fun generateKey(): String {
        val bytes = ByteArray(KEY_SIZE_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    override fun encrypt(plainText: String, secretKey: String): String {
        val key = decodeKey(secretKey)
        val iv = ByteArray(IV_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        }
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(iv.size + encrypted.size)
        iv.copyInto(payload, destinationOffset = 0)
        encrypted.copyInto(payload, destinationOffset = iv.size)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    override fun decrypt(cipherText: String, secretKey: String): Result<String> = runCatching {
        val key = decodeKey(secretKey)
        val payload = Base64.decode(cipherText, Base64.DEFAULT)
        require(payload.size > IV_SIZE_BYTES) { "Cipher payload is too short" }
        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
        }
        val decrypted = cipher.doFinal(encrypted)
        String(decrypted, Charsets.UTF_8)
    }

    private fun decodeKey(secretKey: String): SecretKeySpec {
        val keyBytes = Base64.decode(secretKey, Base64.DEFAULT)
        require(keyBytes.size == KEY_SIZE_BYTES) { "Invalid AES key length: ${keyBytes.size}" }
        return SecretKeySpec(keyBytes, AES)
    }

    companion object {
        private const val AES = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BYTES = 32
        private const val IV_SIZE_BYTES = 12
        private const val GCM_TAG_SIZE_BITS = 128
        private val secureRandom = SecureRandom()
    }
}
