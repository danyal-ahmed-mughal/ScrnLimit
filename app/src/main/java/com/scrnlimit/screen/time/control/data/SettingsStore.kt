package com.scrnlimit.screen.time.control.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.scrnlimit.screen.time.control.domain.AppSettings
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.i18n.UiPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    val flow: Flow<AppSettings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun update(block: (AppSettings) -> AppSettings) {
        var theme = "dark"
        var language = "en"
        context.settingsDataStore.edit { prefs ->
            val next = block(prefs.toSettings())
            prefs[Keys.onboarded] = next.onboarded
            prefs[Keys.seenHow] = next.seenHow
            prefs[Keys.cap] = next.capMinutes
            prefs[Keys.pause] = next.pauseSeconds
            prefs[Keys.burst] = next.burstMinutes
            prefs[Keys.grow] = next.growPause
            prefs[Keys.breath] = next.breath
            prefs[Keys.passes] = next.passesPerDay
            prefs[Keys.strict] = next.strict
            prefs[Keys.salt] = next.pinSalt
            prefs[Keys.hash] = next.pinHash
            prefs[Keys.biometric] = next.biometric
            prefs[Keys.reels] = next.blockReels
            prefs[Keys.admin] = next.uninstallProtection
            prefs[Keys.theme] = next.theme
            prefs[Keys.language] = next.language
            prefs[Keys.languageChosen] = true
            theme = next.theme
            language = next.language
        }
        UiPrefs.write(context, theme, language, chosen = true)
        Phrases.apply(language)
    }

    suspend fun adoptEnglishDefault() {
        var theme = "dark"
        var language = "en"
        var changed = false
        context.settingsDataStore.edit { prefs ->
            if (prefs[Keys.languageChosen] == true) return@edit
            val raw = prefs[Keys.language]
            language = if (raw == null || raw == "system") "en" else raw
            if (raw != language) prefs[Keys.language] = language
            prefs[Keys.languageChosen] = true
            theme = prefs[Keys.theme] ?: "dark"
            changed = true
        }
        if (changed) {
            UiPrefs.write(context, theme, language, chosen = true)
            Phrases.apply(language)
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        ready = true,
        onboarded = this[Keys.onboarded] ?: false,
        seenHow = this[Keys.seenHow] ?: false,
        capMinutes = this[Keys.cap] ?: 60,
        pauseSeconds = this[Keys.pause] ?: 8,
        burstMinutes = this[Keys.burst] ?: 5,
        growPause = this[Keys.grow] ?: true,
        breath = this[Keys.breath] ?: true,
        passesPerDay = this[Keys.passes] ?: 3,
        strict = this[Keys.strict] ?: false,
        pinSalt = this[Keys.salt] ?: "",
        pinHash = this[Keys.hash] ?: "",
        biometric = this[Keys.biometric] ?: false,
        blockReels = this[Keys.reels] ?: false,
        uninstallProtection = this[Keys.admin] ?: false,
        theme = this[Keys.theme] ?: "dark",
        language = resolvedLanguage(),
    )

    private fun Preferences.resolvedLanguage(): String {
        val raw = this[Keys.language] ?: "en"
        if (this[Keys.languageChosen] == true) return raw
        return if (raw == "system") "en" else raw
    }

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val seenHow = booleanPreferencesKey("seen_how")
        val cap = intPreferencesKey("cap")
        val pause = intPreferencesKey("pause")
        val burst = intPreferencesKey("burst")
        val grow = booleanPreferencesKey("grow")
        val breath = booleanPreferencesKey("breath")
        val passes = intPreferencesKey("passes")
        val strict = booleanPreferencesKey("strict")
        val salt = stringPreferencesKey("salt")
        val hash = stringPreferencesKey("hash")
        val biometric = booleanPreferencesKey("biometric")
        val reels = booleanPreferencesKey("reels")
        val admin = booleanPreferencesKey("admin")
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val languageChosen = booleanPreferencesKey("language_chosen")
    }
}
