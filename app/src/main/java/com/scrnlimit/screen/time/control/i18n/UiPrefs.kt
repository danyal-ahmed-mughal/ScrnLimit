package com.scrnlimit.screen.time.control.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object UiPrefs {
    private const val NAME = "ui"

    fun theme(context: Context): String =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("theme", "dark") ?: "dark"

    fun prepare(context: Context) {
        val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean("language_chosen", false)) return
        val stored = prefs.getString("language", null)
        if (stored == null || stored == "system") {
            prefs.edit().putString("language", "en").apply()
        }
    }

    fun language(context: Context): String =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString("language", "en") ?: "en"

    fun write(context: Context, theme: String, language: String, chosen: Boolean = false) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("theme", theme)
            .putString("language", language)
            .apply {
                if (chosen) putBoolean("language_chosen", true)
            }
            .apply()
    }

    fun wrap(base: Context): Context {
        val resolved = Phrases.resolve(language(base))
        val locale = Phrases.locale(resolved)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
