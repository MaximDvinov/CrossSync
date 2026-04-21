package com.cross.sync.syncing.network.crypto

interface CryptoEngine {
    // Генерирует новый случайный ключ (для сервера)
    fun generateKey(): String

    // Шифрует строку в Base64 (для отправки)
    fun encrypt(plainText: String, secretKey: String): String

    // Дешифрует из Base64 (при получении)
    // Выбрасывает исключение, если ключ не подходит
    fun decrypt(cipherText: String, secretKey: String): Result<String>
}