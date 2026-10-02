package com.cross.sync.notifications.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthorizationCodeExtractorTest {
    @Test
    fun recognizesMessageFormatsAndLanguages() {
        val examples = listOf(
            "Код подтверждения: 012345. Никому не сообщайте его." to "012345",
            "123456 — ваш код для входа" to "123456",
            "Ваш код: 1234" to "1234",
            "Одноразовый пароль: 987654" to "987654",
            "Your verification code is 123456. Valid for 10 minutes." to "123456",
            "123456 is your security code" to "123456",
            "OTP: 12345678" to "12345678",
            "Login code: aB12Cd" to "aB12Cd",
            "Your verification code: ABCDE" to "ABCDE",
            "ABCDE is your verification code" to "ABCDE",
            "Your STEAM verification code: 123456" to "123456",
            "BANK24 verification code: 123456" to "123456",
            "YOUR verification code is 123456" to "123456",
            "2FA code: ABCDE" to "ABCDE",
            "Code de vérification: ABCDE" to "ABCDE",
            "Login code: AB12-CD34" to "AB12CD34",
            "Login code: ABCD-EFGH" to "ABCDEFGH",
            "G-123456 is your Google verification code" to "123456",
            "Code: 123 456" to "123456",
            "OTP: 123-456" to "123456",
            "OTP: 12 34 56" to "123456",
            "OTP: 1 2 3 4" to "1234",
            "Код: 123\u202F456" to "123456",
            "Код: 123\u2011456" to "123456",
            "Код: \u200E12\u200B3456\u200F" to "123456",
            "OTP: ١٢٣٤٥٦" to "123456",
            "OTP: １２３４５６" to "123456",
            "Код підтвердження: 123456" to "123456",
            "Ihr Bestätigungscode: 123456" to "123456",
            "Code de vérification : 123456" to "123456",
            "Código de verificación: 123456" to "123456",
            "Código de verificação: 123456" to "123456",
            "Codice di verifica: 123456" to "123456",
            "Kod weryfikacyjny: 123456" to "123456",
            "Doğrulama kodu: 123456" to "123456",
            "验证码：123456" to "123456",
            "驗證碼：123456" to "123456",
            "인증번호: 123456" to "123456",
            "رمز التحقق: ١٢٣٤٥٦" to "123456",
            "کد تأیید: ۱۲۳۴۵۶" to "123456",
            "सत्यापन कोड: 123456" to "123456",
            "@example.com #aB1234" to "aB1234",
            "<#> Your Example code: 123456\nFA+9qCX9VSu" to "123456",
        )
        examples.forEach { (message, expected) ->
            assertEquals(expected, extractAuthorizationCode(message), message)
        }
    }

    @Test
    fun combinesPushTitleAndBodyAndDuplicateRepresentations() {
        assertEquals("123456", extractAuthorizationCode("Код авторизации", "123456"))
        assertEquals("123456", extractAuthorizationCode("Your login code: 123456", "123456"))
        assertEquals("123456", extractAuthorizationCode("Code: 123456", "Code: 123456"))
        assertEquals("123456", extractAuthorizationCode("Code: 123456. Expires in 2026 seconds."))
        assertEquals("123456", extractAuthorizationCode("Order 987654. Login code: 123456. Amount: 1500 USD."))
        assertEquals("123456", extractAuthorizationCode("Код: 123456. Поддержка: +7 (999) 123-45-67"))
    }

    @Test
    fun ignoresUnrelatedNumbersAndAmbiguousCodes() {
        val messages = listOf(
            "Заказ 123456 готов к выдаче",
            "Order code: 123456",
            "Tracking code: 123456",
            "Your postal code: 12345",
            "Promo code: SAVE1234",
            "Код заказа: 123456",
            "Код товара: 123456",
            "Ваш баланс 1234 руб.",
            "Code: 123.45 USD",
            "Code: https://example.com/123456",
            "Verification code: +1234567890",
            "Verification code: 02.10.2026",
            "Verification code: 2026-10-02",
            "Code: 123",
            "Code: 12345678901",
            "Code: AB12-CD3456789",
            "Code: 123456-654321",
            "Code: welcome",
            "Code: ABCDE",
            "Code: 123456; code: 654321",
            "Code: 123456 or 654321",
            "@example.com #123456\n@example.com #654321",
            "Verification code: 123456\n@example.com #654321",
            "Verification code: 123456\nVerification code: 654321",
            "Nothing to see here",
        )
        messages.forEach { assertNull(extractAuthorizationCode(it), it) }
    }
}
