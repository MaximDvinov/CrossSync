package com.cross.sync.syncing.network

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date

object JwtConfig {
    // В реальном проекте эти константы лучше брать из config-файла (application.conf)
    private const val SECRET = "secret-key-that-should-be-complex"
    const val ISSUER = "http://0.0.0.0:33333/" // Кто выдал токен (наш сервер)
    const val AUDIENCE = "clipboard-app-users"  // Для кого токен
    const val REALM = "Access to clipboard sync" // Описание зоны доступа

    private val algorithm = Algorithm.HMAC256(SECRET)

    // Генерация Access Token (живет 15 минут)
    fun generateAccessToken(deviceId: String): String = JWT.create()
        .withAudience(AUDIENCE)
        .withIssuer(ISSUER)
        .withClaim("deviceId", deviceId)
        .withClaim("type", "access") // Метка типа
        .withExpiresAt(Date(System.currentTimeMillis() + 15 * 60 * 1000))
        .sign(algorithm)

    // Генерация Refresh Token (живет 30 дней)
    fun generateRefreshToken(deviceId: String): String = JWT.create()
        .withAudience(AUDIENCE)
        .withIssuer(ISSUER)
        .withClaim("deviceId", deviceId)
        .withClaim("type", "refresh") // Метка типа
        .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
        .sign(algorithm)

    // Верификатор (только для Access токенов!)
    val verifier = JWT.require(algorithm)
        .withAudience(AUDIENCE)
        .withIssuer(ISSUER)
        .withClaim("type", "access") // Важно: пускаем только Access токены
        .build()

    // Вспомогательный метод для проверки Refresh токена вручную
    fun verifyRefreshToken(token: String): DecodedJWT? {
        return try {
            JWT.require(algorithm)
                .withAudience(AUDIENCE)
                .withIssuer(ISSUER)
                .withClaim("type", "refresh") // Проверяем, что это именно Refresh
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }
}