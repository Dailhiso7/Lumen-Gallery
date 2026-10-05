package org.lumengallery.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "lumen_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    const val LANG_SYSTEM = "system"
    const val LANG_RU = "ru"
    const val LANG_EN = "en"
    const val LANG_DE = "de"
    const val LANG_ES = "es"
    const val LANG_FR = "fr"
    const val LANG_IT = "it"
    const val LANG_JA = "ja"
    const val LANG_ZH_CN = "zh-CN"
    const val LANG_ZH_TW = "zh-TW"
    const val LANG_KO = "ko"

    data class LanguageItem(
        val code: String,
        val displayName: String,
        val nativeName: String
    )

    val SUPPORTED_LANGUAGES = listOf(
        LanguageItem(LANG_SYSTEM, "System Default", "По умолчанию / System"),
        LanguageItem(LANG_RU, "Russian", "Русский"),
        LanguageItem(LANG_EN, "English", "English"),
        LanguageItem(LANG_DE, "German", "Deutsch"),
        LanguageItem(LANG_ES, "Spanish", "Español"),
        LanguageItem(LANG_FR, "French", "Français"),
        LanguageItem(LANG_IT, "Italian", "Italiano"),
        LanguageItem(LANG_JA, "Japanese", "日本語"),
        LanguageItem(LANG_ZH_CN, "Chinese (Simplified)", "简体中文"),
        LanguageItem(LANG_ZH_TW, "Chinese (Traditional)", "繁體中文"),
        LanguageItem(LANG_KO, "Korean", "한국어")
    )

    private val _currentLanguage = MutableStateFlow(LANG_SYSTEM)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun init(context: Context) {
        val savedLang = getSavedLanguage(context)
        _currentLanguage.value = savedLang
        applyLocale(context)
    }

    fun setLanguage(context: Context, langCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Synchronous commit ensures preference is written to disk immediately
        prefs.edit().putString(KEY_LANGUAGE, langCode).commit()
        _currentLanguage.value = langCode

        applyLocale(context)
        applyLocale(context.applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? android.app.LocaleManager
            if (localeManager != null) {
                try {
                    if (langCode == LANG_SYSTEM) {
                        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
                    } else {
                        localeManager.applicationLocales = LocaleList.forLanguageTags(langCode)
                    }
                } catch (_: Exception) {
                }
            }
        }

        try {
            if (langCode == LANG_SYSTEM) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
            }
        } catch (_: Exception) {
        }
    }

    fun getLocaleForLanguage(langCode: String): Locale {
        return if (langCode == LANG_SYSTEM) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales.get(0) ?: Locale.getDefault()
            } else {
                @Suppress("DEPRECATION")
                Resources.getSystem().configuration.locale ?: Locale.getDefault()
            }
        } else {
            Locale.forLanguageTag(langCode)
        }
    }

    fun applyLocale(context: Context): Context {
        val langCode = getSavedLanguage(context)
        val targetLocale = getLocaleForLanguage(langCode)
        Locale.setDefault(targetLocale)

        return try {
            val res = context.resources
            val config = Configuration(res.configuration)
            config.setLocale(targetLocale)
            config.setLayoutDirection(targetLocale)

            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)

            context.createConfigurationContext(config)
        } catch (_: Exception) {
            context
        }
    }

    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)

            var ctx: Context? = context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) {
                    ctx.finish()
                    break
                }
                ctx = ctx.baseContext
            }
        }
    }
}
