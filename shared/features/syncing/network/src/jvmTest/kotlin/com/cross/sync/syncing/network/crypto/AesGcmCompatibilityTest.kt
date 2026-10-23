package com.cross.sync.syncing.network.crypto

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AesGcmCompatibilityTest {
    @Test
    fun decryptsSameAes256FixtureAsCryptoKitAndRejectsTampering() {
        val crypto = AesGcmCryptoEngine()
        val key = Base64.getEncoder().encodeToString(ByteArray(32))
        val tag = "530f8afbc74536b9a963b4f1c4cb738b".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val payload = ByteArray(12) + tag
        assertEquals("", crypto.decrypt(Base64.getEncoder().encodeToString(payload), key).getOrThrow())
        payload[payload.lastIndex] = (payload.last().toInt() xor 1).toByte()
        assertTrue(crypto.decrypt(Base64.getEncoder().encodeToString(payload), key).isFailure)
    }
}
