package com.mehmetbozkurt.clink.prefs

/** The app's language choice in settings; [tag] is the language tag ("" = follow the phone). */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    TURKISH("tr"),
    ENGLISH("en"),
    ;

    companion object {
        /** The choice for a stored or system tag: only the language part counts ("tr-TR" is Turkish); anything else follows the phone. */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.substringBefore('-')?.substringBefore('_')?.lowercase()
            if (language.isNullOrBlank()) return SYSTEM
            return entries.firstOrNull { it != SYSTEM && it.tag == language } ?: SYSTEM
        }
    }
}

/** Reads and changes the app's language; only the platform can do this (spec 2026-11-24-home-and-settings §5). */
interface LanguageSetting {
    fun current(): AppLanguage

    /** Applies [language] at once; the screen reloads in it. */
    fun set(language: AppLanguage)
}
