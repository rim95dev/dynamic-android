package io.github.rim95dev.island.settings

/** How the island treats notifications that carry a login or verification code. */
data class OneTimeCodeSettings(
    /** The island shows the code in large type instead of the message. */
    val enabled: Boolean = true,
    /** The code is copied to the clipboard when the island shows it. */
    val autoCopy: Boolean = true,
    /** Code words the user added on top of the built-in ones. */
    val keywords: List<String> = emptyList(),
)
