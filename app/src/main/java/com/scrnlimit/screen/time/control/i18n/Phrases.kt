package com.scrnlimit.screen.time.control.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.scrnlimit.screen.time.control.domain.BlockReason
import com.scrnlimit.screen.time.control.domain.Category
import java.util.IllegalFormatException
import java.util.Locale

object Phrases {
    var code by mutableStateOf("en")

    val supported = listOf(
        "en", "de", "fr", "es", "it", "pt", "nl", "pl", "ro", "el", "sv",
        "uk", "ru", "tr", "ar", "fa", "zh", "ko", "vi", "fil",
    )

    fun resolve(pref: String): String {
        if (pref != "system" && pref in supported) return pref
        val language = when (val raw = Locale.getDefault().language) {
            "tl" -> "fil"
            else -> raw
        }
        return if (language in supported) language else "en"
    }

    fun apply(pref: String) {
        code = resolve(pref)
    }

    fun rtl(value: String = code): Boolean = value == "ar" || value == "fa"

    fun locale(value: String): Locale = when (value) {
        "en" -> Locale.US
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "fil" -> Locale.forLanguageTag("fil")
        else -> Locale.forLanguageTag(value)
    }

    fun text(key: String, vararg args: Any): String {
        val raw = table(code)[key] ?: EN[key] ?: key
        if (args.isEmpty()) return raw
        return try {
            raw.format(*args)
        } catch (_: IllegalFormatException) {
            val fallback = EN[key] ?: return raw
            try {
                fallback.format(*args)
            } catch (_: IllegalFormatException) {
                fallback
            }
        }
    }

    fun status(ratio: Float, permitted: Boolean): String = when {
        !permitted -> text("status_off")
        ratio >= 1f -> text("status_over")
        ratio >= 0.8f -> text("status_close")
        else -> text("status_ok")
    }

    fun versus(todayMs: Long, yesterdayMs: Long): String {
        val delta = ((todayMs - yesterdayMs) / 60_000L).toInt()
        return when {
            kotlin.math.abs(delta) < 1 -> text("same_y")
            delta < 0 -> text("under_y", -delta)
            else -> text("over_y", delta)
        }
    }

    fun reasonTitle(reason: BlockReason): String = when (reason) {
        BlockReason.CAP -> text("reason_cap")
        BlockReason.APP_CAP -> text("reason_app")
        BlockReason.SCHEDULE -> text("reason_schedule")
        BlockReason.FOCUS -> text("reason_focus")
        BlockReason.BURST_END -> text("reason_burst")
        BlockReason.SITE -> text("reason_site")
        BlockReason.NONE -> ""
    }

    fun reasonBody(reason: BlockReason): String = when (reason) {
        BlockReason.CAP -> text("body_cap")
        BlockReason.APP_CAP -> text("body_app")
        BlockReason.SCHEDULE -> text("body_schedule")
        BlockReason.FOCUS -> text("body_focus")
        BlockReason.BURST_END -> text("body_burst")
        BlockReason.SITE -> text("body_site")
        BlockReason.NONE -> ""
    }

    fun passesLine(left: Int): String = when (left) {
        0 -> text("passes_0")
        1 -> text("passes_1")
        else -> text("passes_n", left)
    }

    fun category(category: Category): String = when (category) {
        Category.DISTRACTING -> text("cat_distracting")
        Category.NEUTRAL -> text("cat_neutral")
        Category.PRODUCTIVE -> text("cat_productive")
    }

    fun time(minute: Int): String {
        val normalized = ((minute % 1440) + 1440) % 1440
        val hour24 = normalized / 60
        val min = normalized % 60
        if (code == "en" || code == "fil") {
            val suffix = if (hour24 < 12) "AM" else "PM"
            val hour = if (hour24 % 12 == 0) 12 else hour24 % 12
            return "%d:%02d %s".format(hour, min, suffix)
        }
        return "%02d:%02d".format(hour24, min)
    }

    fun dayLetters(): List<String> = listOf(
        text("day_m"), text("day_t"), text("day_w"), text("day_h"),
        text("day_f"), text("day_sa"), text("day_su"),
    )

    fun dayNames(mask: Int): String {
        val labels = listOf(
            text("mon"), text("tue"), text("wed"), text("thu"),
            text("fri"), text("sat"), text("sun"),
        )
        val on = labels.filterIndexed { index, _ -> mask and (1 shl index) != 0 }
        if (on.size == 7) return text("every_day")
        if (on == labels.take(5)) return text("weekdays")
        if (on == labels.takeLast(2)) return text("weekends")
        return on.joinToString(" ")
    }

    private fun table(value: String): Map<String, String> = when (value) {
        "de" -> DE
        "fr" -> FR
        "es" -> ES
        "it" -> IT
        "pt" -> PT
        "nl" -> NL
        "pl" -> PL
        "ro" -> RO
        "el" -> EL
        "sv" -> SV
        "uk" -> UK
        "ru" -> RU
        "tr" -> TR
        "ar" -> AR
        "fa" -> FA
        "zh" -> ZH
        "ko" -> KO
        "vi" -> VI
        "fil" -> FIL
        else -> EN
    }
}

@Composable
fun t(key: String, vararg args: Any): String {
    val current = Phrases.code
    val raw = when (current) {
        "de" -> DE
        "fr" -> FR
        "es" -> ES
        "it" -> IT
        "pt" -> PT
        "nl" -> NL
        "pl" -> PL
        "ro" -> RO
        "el" -> EL
        "sv" -> SV
        "uk" -> UK
        "ru" -> RU
        "tr" -> TR
        "ar" -> AR
        "fa" -> FA
        "zh" -> ZH
        "ko" -> KO
        "vi" -> VI
        "fil" -> FIL
        else -> EN
    }[key] ?: EN[key] ?: key
    if (args.isEmpty()) return raw
    return raw.format(*args)
}

internal fun pack(vararg pairs: Pair<String, String>): Map<String, String> = mapOf(*pairs)
