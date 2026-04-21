package com.cross.sync.syncing.network

import com.cross.sync.syncing.network.crypto.CryptoEngine
import io.ktor.util.encodeBase64
import java.security.SecureRandom

class TestCryptoEngine : CryptoEngine {
    override fun generateKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)

        return bytes.encodeBase64()
    }

    override fun encrypt(plainText: String, secretKey: String): String {
        return plainText
    }

    override fun decrypt(
        cipherText: String,
        secretKey: String
    ): Result<String> {
        return Result.success(cipherText)
    }
}