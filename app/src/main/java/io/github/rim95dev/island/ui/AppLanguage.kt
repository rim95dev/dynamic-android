package io.github.rim95dev.island.ui

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/** The app's own language, set through Android's per-app language preference. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    KOREAN("ko"),
    ;

    companion object {
        fun current(context: Context): AppLanguage {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) return SYSTEM
            val language = locales[0].language
            return entries.firstOrNull { it.tag == language } ?: SYSTEM
        }
    }

    /** Applies this language to the whole app. The system recreates visible activities in the new language. */
    fun apply(context: Context) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
    }
}
