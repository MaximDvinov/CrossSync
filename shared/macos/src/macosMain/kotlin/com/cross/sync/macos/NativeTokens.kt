package com.cross.sync.macos

import com.cross.sync.syncing.network.ServerTokens
import com.cross.sync.syncing.network.crypto.CryptoEngine
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.time.Clock

internal class NativeCryptoEngine(private val crypto: CryptoOperations) : CryptoEngine {
    override fun generateKey() = crypto.generateKey()
    override fun encrypt(plainText: String, secretKey: String) = crypto.seal(plainText, secretKey).also { require(it.isNotEmpty()) { "Cannot encrypt sync data" } }
    override fun decrypt(cipherText: String, secretKey: String): Result<String> = runCatching {
        crypto.open(cipherText, secretKey) ?: error("Cannot decrypt sync data")
    }
}

internal class NativeTokens(private val crypto: CryptoOperations, settings: Settings) : ServerTokens {
    private val key = settings.getStringOrNull("native.jwtSigningKey") ?: crypto.generateKey().also {
        settings.putString("native.jwtSigningKey", it)
    }
    override fun access(deviceId: String) = issue(deviceId, "access", 15 * 60)
    override fun refresh(deviceId: String) = issue(deviceId, "refresh", 30L * 24 * 60 * 60)
    override fun verifyAccess(token: String) = verify(token, "access")
    override fun verifyRefresh(token: String) = verify(token, "refresh")
    private fun issue(deviceId: String, type: String, lifetime: Long): String {
        val header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}")
        val payload = buildJsonObject {
            put("aud", AUDIENCE); put("iss", ISSUER); put("deviceId", deviceId)
            put("type", type); put("exp", Clock.System.now().epochSeconds + lifetime)
        }
        val message = "$header.${encode(payload.toString())}"
        return "$message.${signature(message)}"
    }
    private fun verify(token: String, type: String): String? = runCatching {
        val parts = token.split('.')
        require(parts.size == 3)
        val header = Json.parseToJsonElement(decode(parts[0])).jsonObject
        require(header["alg"]?.jsonPrimitive?.content == "HS256")
        val expected = signature("${parts[0]}.${parts[1]}")
        require(constantTimeEquals(expected, parts[2]))
        val claims = Json.parseToJsonElement(decode(parts[1])).jsonObject
        require(claims["aud"]?.jsonPrimitive?.content == AUDIENCE)
        require(claims["iss"]?.jsonPrimitive?.content == ISSUER)
        require(claims["type"]?.jsonPrimitive?.content == type)
        require((claims["exp"]?.jsonPrimitive?.longOrNull ?: 0) > Clock.System.now().epochSeconds)
        claims["deviceId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
    }.getOrNull()
    private fun signature(message: String) = crypto.hmac(message, key).replace('+', '-').replace('/', '_').trimEnd('=')
    private fun encode(value: String) = Base64.UrlSafe.encode(value.encodeToByteArray()).trimEnd('=')
    private fun decode(value: String) = Base64.UrlSafe.decode(value.padEnd((value.length + 3) / 4 * 4, '=')).decodeToString()
    private companion object {
        const val AUDIENCE = "clipboard-app-users"
        const val ISSUER = "http://0.0.0.0:33333/"
    }
}

internal fun constantTimeEquals(left: String, right: String): Boolean {
    var difference = left.length xor right.length
    for (i in 0 until maxOf(left.length, right.length)) {
        difference = difference or ((left.getOrNull(i)?.code ?: 0) xor (right.getOrNull(i)?.code ?: 0))
    }
    return difference == 0
}
