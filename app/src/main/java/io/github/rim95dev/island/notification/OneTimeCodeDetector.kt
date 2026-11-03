package io.github.rim95dev.island.notification

/**
 * Finds a one-time code, such as a login or verification code, in a notification's text.
 *
 * A code is looked for only when the text has a code word such as "인증번호" or "verification", so numbers
 * in ordinary messages are left alone. Among the numbers that look like a code, the one nearest to a code
 * word wins, with a bonus for codes in brackets and for six digits. Phone numbers, dates, times, amounts and
 * masked card numbers are never codes.
 */
object OneTimeCodeDetector {

    private val BUILT_IN_KEYWORDS = listOf(
        "인증번호", "인증 번호", "인증코드", "인증 코드", "인증문자", "본인확인", "본인 확인",
        "확인코드", "확인 코드", "보안코드", "보안 코드", "일회용", "로그인 코드",
        "otp", "code", "codes", "passcode", "verification", "verify", "2fa", "pin",
        "验证码", "驗證碼", "校验码", "动态码", "認証", "確認コード", "ワンタイム", "コード",
    )

    /** Words of messages whose codes are not for signing in, such as coupons and parcel tracking. */
    private val BLOCKING_WORDS = listOf(
        "쿠폰", "할인코드", "할인 코드", "프로모션", "추천인", "초대코드", "초대 코드", "운송장", "송장번호",
        "coupon", "promo", "referral", "invite", "tracking",
    )

    /** The last line of the standard format for codes bound to a website: `@example.com #123456`. */
    private val ORIGIN_BOUND = Regex("""(?m)^@[A-Za-z0-9.-]+ #([A-Za-z0-9]{4,10})(?=\s|$)""")

    /** Digits, joined by separators such as `-`, `.`, `:`, `/` or `,`. */
    private val NUMBER_GROUP = Regex("""(?<![0-9])[0-9]+(?:[-.:/,][0-9]+)*(?![0-9])""")

    /** Two groups of three digits split by a space, such as `123 456`. */
    private val SPLIT_CODE = Regex("""(?<![0-9])([0-9]{3}) ([0-9]{3})(?![0-9])""")

    /** Upper-case letters mixed with digits, such as `A7K9Q2`. */
    private val ALPHANUMERIC = Regex("""(?<![A-Za-z0-9])(?=[A-Z0-9]*[0-9])(?=[A-Z0-9]*[A-Z])[A-Z0-9]{5,8}(?![A-Za-z0-9])""")

    /** Toll-free and business numbers like `1588-1234` look like `1234-5678` codes. */
    private val BUSINESS_NUMBER_PREFIX = Regex("""1[5-9][0-9]{2}""")

    private const val OPENERS = "[(<【「『［（\"'"
    private const val CLOSERS = "])>】」』］）\"'"
    private const val NOT_AFTER = "*+₩$€£¥"
    private val UNITS = listOf("원", "円", "元", "달러", "개", "건", "회", "명", "년", "월", "일", "시", "분", "초", "점", "%")

    private const val BRACKET_BONUS = 20
    private const val SIX_DIGIT_BONUS = 5

    /** Returns the code in [text], or null. [extraKeywords] are code words the user added. */
    fun find(text: String, extraKeywords: List<String> = emptyList()): String? {
        ORIGIN_BOUND.findAll(text).lastOrNull()?.let { return it.groupValues[1] }
        if (BLOCKING_WORDS.any { text.wordRanges(it).isNotEmpty() }) return null
        val keywordRanges = (BUILT_IN_KEYWORDS + extraKeywords).flatMap { text.wordRanges(it) }
        if (keywordRanges.isEmpty()) return null
        val candidates = numericCandidates(text).ifEmpty { alphanumericCandidates(text) }
        return candidates.minByOrNull { it.score(keywordRanges) }?.code
    }

    private data class Candidate(val code: String, val range: IntRange, val bracketed: Boolean) {
        fun score(keywordRanges: List<IntRange>): Int {
            val distance = keywordRanges.minOf { keyword ->
                when {
                    range.first > keyword.last -> range.first - keyword.last
                    keyword.first > range.last -> keyword.first - range.last
                    else -> 0
                }
            }
            return distance -
                (if (bracketed) BRACKET_BONUS else 0) -
                (if (code.length == 6) SIX_DIGIT_BONUS else 0)
        }
    }

    private fun numericCandidates(text: String): List<Candidate> {
        val fromGroups = NUMBER_GROUP.findAll(text).mapNotNull { match ->
            val group = match.value
            val parts = group.split('-', '.', ':', '/', ',')
            val code = when {
                parts.size == 1 && group.length in 4..8 && !text.hasNumberAcrossSpace(match.range) -> group
                parts.size == 2 &&
                    group[parts[0].length] == '-' &&
                    parts.all { it.length in 3..4 } &&
                    !(parts[0].length == 4 && BUSINESS_NUMBER_PREFIX.matches(parts[0])) -> parts.joinToString("")
                else -> null
            }
            code?.let { text.candidateAt(it, match.range) }
        }
        val fromSplit = SPLIT_CODE.findAll(text).mapNotNull { match ->
            text.candidateAt(match.groupValues[1] + match.groupValues[2], match.range)
        }
        return (fromGroups + fromSplit).toList()
    }

    /** Phone numbers written with spaces, such as `010 1234 5678`, split into several groups. */
    private fun String.hasNumberAcrossSpace(range: IntRange): Boolean =
        (getOrNull(range.first - 1) == ' ' && getOrNull(range.first - 2)?.isDigit() == true) ||
            (getOrNull(range.last + 1) == ' ' && getOrNull(range.last + 2)?.isDigit() == true)

    private fun alphanumericCandidates(text: String): List<Candidate> =
        ALPHANUMERIC.findAll(text).mapNotNull { text.candidateAt(it.value, it.range) }.toList()

    /** Builds a candidate unless the text around [range] shows it is a phone number, amount or the like. */
    private fun String.candidateAt(code: String, range: IntRange): Candidate? {
        val before = getOrNull(range.first - 1)
        val after = getOrNull(range.last + 1)
        if (before.isAsciiLetter() || after.isAsciiLetter()) return null
        val previous = substring(0, range.first).trimEnd().lastOrNull()
        if (previous != null && previous in NOT_AFTER) return null
        // Units are written right after the number, as in 12,500원 or 3분, so a following word is not one.
        val rest = substring(range.last + 1)
        if (UNITS.any { rest.startsWith(it) }) return null
        val following = rest.trimStart()
        val bracketed = previous != null && previous in OPENERS && following.firstOrNull()?.let { it in CLOSERS } == true
        return Candidate(code, range, bracketed)
    }

    /**
     * Where [word] appears, ignoring case. Words written in Latin letters must stand on their own, so "pin"
     * does not match "shopping".
     */
    private fun String.wordRanges(word: String): List<IntRange> {
        if (word.isBlank()) return emptyList()
        val pattern = if (word.all { it.code < 128 }) {
            """(?<![A-Za-z])${Regex.escape(word)}(?![A-Za-z])"""
        } else {
            Regex.escape(word)
        }
        return Regex(pattern, RegexOption.IGNORE_CASE).findAll(this).map { it.range }.toList()
    }

    private fun Char?.isAsciiLetter(): Boolean = this != null && (this in 'a'..'z' || this in 'A'..'Z')
}
