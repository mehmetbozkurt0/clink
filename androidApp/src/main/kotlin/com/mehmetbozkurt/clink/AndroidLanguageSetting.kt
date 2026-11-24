package com.mehmetbozkurt.clink

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.mehmetbozkurt.clink.prefs.AppLanguage
import com.mehmetbozkurt.clink.prefs.LanguageSetting
import java.util.Locale

/**
 * The app's own language (spec 2026-11-24-home-and-settings §3). Android 13+ keeps it as the system per-app language
 * (it then also shows in the phone's settings, and the system recreates the activity). Older phones keep the tag in a
 * small file of their own, read synchronously in [wrap] when the activity starts, and the activity is recreated here.
 */
class AndroidLanguageSetting(private val activity: Activity) : LanguageSetting {
    override fun current(): AppLanguage =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = activity.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) AppLanguage.SYSTEM else AppLanguage.fromTag(locales[0].toLanguageTag())
        } else {
            AppLanguage.fromTag(savedTag(activity))
        }

    override fun set(language: AppLanguage) {
        if (language == current()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = if (language == AppLanguage.SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language.tag)
            activity.getSystemService(LocaleManager::class.java).applicationLocales = locales
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.tag).commit()
            activity.recreate()
        }
    }

    companion object {
        private const val PREFS = "clink_language"
        private const val KEY = "tag"

        private fun savedTag(context: Context): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

        /** Below Android 13: the activity's context in the saved language, or the phone's language when none is saved. */
        fun wrap(base: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
            val tag = savedTag(base)
            if (tag.isNullOrBlank()) {
                // Back to the phone's language: undo a default set earlier in this process.
                Locale.setDefault(Resources.getSystem().configuration.locales[0])
                return base
            }
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
            return base.createConfigurationContext(config)
        }
    }
}
