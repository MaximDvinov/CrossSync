package com.cross.sync.notifications.domain

/** Returns only an unambiguous code. Service names and notification categories are irrelevant. */
fun extractAuthorizationCode(vararg texts: String): String? {
    val text = normalizeCodeText(texts.joinToString("\n").take(16_384))
    val webCodes = webOtpCode.findAll(text).map { it.groupValues[1] }.distinct().toList()
    if (webCodes.size > 1) return null
    val webCode = webCodes.singleOrNull()
    val anchors = authorizationWords.findAll(text).toList()
    if (anchors.isEmpty()) return webCode
    if (codeAlternatives.containsMatchIn(text)) return null

    val excluded = nonCodeValues.findAll(text).map { it.range }.toList()
    val negative = nonAuthorizationWords.findAll(text).toList()
    val candidates = mutableListOf<CodeCandidate>()
    val groupedRanges = mutableListOf<IntRange>()
    for (match in groupedDigits.findAll(text)) {
        val groups = match.value.split(' ', '-')
        val code = groups.joinToString("")
        if (code.length in 4..10 && groups.all { it.length == groups.first().length } &&
            groups.first().length in 1..4
        ) {
            groupedRanges += match.range
            candidates += CodeCandidate(code, match.range)
        }
    }
    for (match in hyphenatedTokens.findAll(text)) {
        if (groupedRanges.any { rangesOverlap(it, match.range) }) continue
        val groups = match.value.split('-')
        // Google's "G-123456" prefix is a label, rather than part of the code.
        if (groups.size == 2 && groups.first() == "G" && groups.last().all { it.isDigit() }) continue
        groupedRanges += match.range
        val code = groups.joinToString("")
        if (code.length in 4..10 && groups.all { it.length in 2..5 }) {
            candidates += CodeCandidate(code, match.range)
        }
    }
    for (match in codeTokens.findAll(text)) {
        if (groupedRanges.any { match.range.first in it }) continue
        val value = match.value
        if (value.length !in 4..10) continue
        // Ordinary words must never compete with numeric codes.
        if (value.any { it.isLetter() } && value.none { it.isDigit() } &&
            (value.any { it.isLowerCase() } || value.length !in 4..8 || value.lowercase() in ordinaryWords)
        ) continue
        candidates += CodeCandidate(value, match.range)
    }

    val ranked = candidates.mapNotNull { candidate ->
        if (anchors.any { rangesOverlap(it.range, candidate.range) }) return@mapNotNull null
        if (excluded.any { rangesOverlap(it, candidate.range) }) return@mapNotNull null
        val anchor = anchors.minByOrNull { rangeDistance(it.range, candidate.range) }
            ?: return@mapNotNull null
        val distance = rangeDistance(anchor.range, candidate.range)
        val strong = strongAuthorizationWords.containsMatchIn(anchor.value)
        if (distance > if (strong) 80 else 32) return@mapNotNull null
        if (candidate.code.all { it.isLetter() } && (!strong || distance > 32)) return@mapNotNull null
        if (!strong && negative.any { label ->
                label.range.last < anchor.range.first &&
                    text.substring(label.range.last + 1, anchor.range.first).let { gap ->
                        gap.length <= 8 && gap.isBlank()
                    }
            }
        ) return@mapNotNull null
        if (negative.any {
                val negativeDistance = rangeDistance(it.range, candidate.range)
                it.range.last < candidate.range.first &&
                    negativeDistance <= 32 && negativeDistance <= distance
            }
        ) return@mapNotNull null
        val gap = if (anchor.range.last < candidate.range.first) {
            text.substring(anchor.range.last + 1, candidate.range.first)
        } else {
            text.substring(candidate.range.last + 1, anchor.range.first)
        }
        // Uppercase service names before "verification code" are not the code itself.
        if (candidate.code.any { it.isLetter() } && candidate.range.last < anchor.range.first &&
            gap.isBlank()
        ) return@mapNotNull null
        if (gap.any { it.isDigit() }) return@mapNotNull null
        val direct = codeConnector.matches(gap)
        val score = when {
            direct && strong -> 120
            direct -> 100
            strong -> 70
            else -> 50
        }
        candidate.code to score
    }.groupBy({ it.first }, { it.second }).mapValues { it.value.max() }
        .entries.sortedByDescending { it.value }

    val best = ranked.firstOrNull() ?: return webCode
    // Distinct similarly plausible codes are ambiguous; repeating the same code is fine.
    if (ranked.drop(1).any { best.value - it.value < 20 }) return null
    if (webCode != null && best.key != webCode) return null
    return best.key
}

private data class CodeCandidate(val code: String, val range: IntRange)

private fun normalizeCodeText(text: String): String = buildString(text.length) {
    for (character in text) {
        when (character) {
            '\u200B', '\u200C', '\u200D', '\u200E', '\u200F', '\u202A', '\u202B',
            '\u202C', '\u202D', '\u202E', '\u2066', '\u2067', '\u2068', '\u2069', '\uFEFF' -> Unit
            '\u00A0', '\u202F', '\u2007' -> append(' ')
            '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2212' -> append('-')
            else -> append(character.digitToIntOrNull()?.let { '0' + it } ?: character)
        }
    }
}

private fun rangeDistance(first: IntRange, second: IntRange): Int = when {
    first.last < second.first -> second.first - first.last - 1
    second.last < first.first -> first.first - second.last - 1
    else -> 0
}

private fun rangesOverlap(first: IntRange, second: IntRange): Boolean =
    first.first <= second.last && second.first <= first.last

private val authorizationWords = Regex(
    """(?iu)(?<![\p{L}\p{N}])(?:(?:otp|2fa|mfa|pin)(?:\s+code)?|(?:one[ -]?time|verification|security|authentication|authorization|confirmation|login|sign[ -]?in|access)\s+(?:code|password|pin)|код\s+підтвердження|код(?:\s+(?:подтверждения|авторизации|проверки|верификации|безопасности|доступа|входа|для\s+входа))?|одноразов(?:ый|ий)\s+(?:код|пароль)|пароль\s+(?:для\s+входа|подтверждения)|passcode|verifizierungscode|bestätigungscode|sicherheitscode|anmeldecode|einmalpasswort|code\s+(?:de\s+)?(?:vérification|confirmation|sécurité)|c[oó]digo(?:\s+(?:de\s+)?(?:verificaci[oó]n|confirmaci[oó]n|seguridad|verifica[cç][aã]o|seguran[cç]a))?|codice(?:\s+di\s+(?:verifica|sicurezza|accesso))?|kod(?:\s+(?:weryfikacyjny|potwierdzenia))?|doğrulama\s+kodu|code)(?![\p{L}\p{N}])|验证码|驗證碼|校验码|动态密码|인증\s*번호|인증\s*코드|رمز\s+(?:التحقق|التأكيد|الدخول)|کد\s+(?:تایید|تأیید|ورود)|सत्यापन\s*कोड""",
)
private val strongAuthorizationWords = Regex(
    """(?iu)otp|2fa|mfa|pin|one[ -]?time|verification|security|authentication|authorization|confirmation|login|sign[ -]?in|access|passcode|подтвержден|авторизац|проверки|верификац|безопасност|доступа|входа|одноразов|підтверджен|verifizierung|bestätigung|sicherheits|anmelde|einmal|vérification|confirmation|sécurité|verificaci|confirmaci|seguridad|verifica[cç]|seguran[cç]|verifica|sicurezza|accesso|weryfikacyjny|potwierdzenia|doğrulama|验证码|驗證碼|校验码|动态密码|인증|التحقق|التأكيد|الدخول|تایید|تأیید|ورود|सत्यापन""",
)
private val nonAuthorizationWords = Regex(
    """(?iu)(?<!\p{L})(?:order|tracking|parcel|zip|postal|coupon|promo|product|invoice|phone|tel|card|account|amount|balance|expires?|valid|minutes?|seconds?|year|заказ\p{L}*|товар\p{L}*|доставк\p{L}*|посылк\p{L}*|индекс|промокод|купон|телефон\p{L}*|карт\p{L}*|сч[её]т\p{L}*|сумм\p{L}*|баланс|действует|минут\p{L}*|секунд\p{L}*|год)(?!\p{L})""",
)
private val codeConnector = Regex(
    """(?iu)[\s:：=,.;!#'"«»()\[\]<>-]*(?:(?:is|ist|это|est|es|e|é|ваш|your)[\s:：=,.;!#'"«»()\[\]<>-]*){0,2}""",
)
private val ordinaryWords = setOf("your", "please", "never", "share", "enter", "login", "access", "security", "verify")
private val groupedDigits = Regex("""(?<![\p{L}\p{N}])[0-9]+(?:[ -][0-9]+)+(?![\p{L}\p{N}])""")
private val codeTokens = Regex("""(?<![A-Za-z0-9])[A-Za-z0-9]+(?![A-Za-z0-9])""")
private val hyphenatedTokens = Regex("""(?<![A-Za-z0-9])[A-Za-z0-9]+(?:-[A-Za-z0-9]+)+(?![A-Za-z0-9])""")
private val nonCodeValues = Regex(
    """(?iu)https?://\S+|www\.\S+|[\w.+-]+@[\w.-]+|\+[0-9][0-9 ()-]{6,}[0-9]|\b[0-9]{1,4}[./:][0-9]{1,2}(?:[./:][0-9]{1,4})?\b|\b(?:[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}-[0-9]{2}-[0-9]{4})\b|[0-9]+[.,][0-9]{2}\b|[\$€£₽]\s*[0-9][0-9 .,]*|[0-9][0-9 .,]*\s*(?:руб\p{L}*|usd|eur|rub|₽|€|\$)(?!\p{L})""",
)
private val codeAlternatives = Regex("""(?iu)\b[A-Za-z0-9]{4,10}\s+(?:or|или)\s+[A-Za-z0-9]{4,10}\b""")
private val webOtpCode = Regex("""(?m)^@[A-Za-z0-9.-]+[ \t]+#([A-Za-z0-9]{4,10})(?:[ \t]+@[A-Za-z0-9.-]+)?[ \t]*$""")
