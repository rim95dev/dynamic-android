package io.github.rim95dev.island.settings

enum class KeywordRuleMode {
    /** Notifications that contain any of the keywords stay out of the island. */
    HIDE_MATCHING,

    /** Only notifications that contain one of the keywords appear in the island. */
    SHOW_MATCHING,
}

/**
 * Hides or shows one app's notifications by the words in them.
 *
 * Keywords match anywhere in the notification text, ignoring case. A rule without keywords changes nothing.
 */
data class KeywordRule(
    val mode: KeywordRuleMode = KeywordRuleMode.HIDE_MATCHING,
    val keywords: List<String> = emptyList(),
) {
    fun allows(content: List<String>): Boolean {
        if (keywords.isEmpty()) return true
        val matches = keywords.any { keyword -> content.any { it.contains(keyword, ignoreCase = true) } }
        return when (mode) {
            KeywordRuleMode.HIDE_MATCHING -> !matches
            KeywordRuleMode.SHOW_MATCHING -> matches
        }
    }

    /** Adds [keyword] unless it is blank or already listed, ignoring case. */
    fun withKeyword(keyword: String): KeywordRule {
        val cleaned = keyword.replace(LINE_BREAK, " ").trim()
        if (cleaned.isEmpty() || keywords.any { it.equals(cleaned, ignoreCase = true) }) return this
        return copy(keywords = keywords + cleaned)
    }

    fun withoutKeyword(keyword: String): KeywordRule = copy(keywords = keywords - keyword)

    /** The mode on the first line and one keyword per line after it. Keywords never contain line breaks. */
    fun encode(): String = (listOf(mode.name) + keywords).joinToString("\n")

    companion object {
        private val LINE_BREAK = Regex("[\\r\\n]+")

        fun decode(value: String?): KeywordRule? {
            val lines = value?.split("\n") ?: return null
            val mode = KeywordRuleMode.entries.firstOrNull { it.name == lines.first() } ?: return null
            return KeywordRule(mode, lines.drop(1).filter { it.isNotEmpty() })
        }
    }
}
