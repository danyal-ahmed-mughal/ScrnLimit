package com.scrnlimit.screen.time.control.domain

import java.time.LocalDate

enum class Category { DISTRACTING, NEUTRAL, PRODUCTIVE }

enum class ScheduleKind { WORK, SLEEP }

enum class Phase { FOCUS, BREAK }

enum class BlockReason { NONE, BURST_END, CAP, APP_CAP, SCHEDULE, FOCUS, SITE }

data class AppSettings(
    val ready: Boolean = false,
    val onboarded: Boolean = false,
    val seenHow: Boolean = false,
    val capMinutes: Int = 60,
    val pauseSeconds: Int = 8,
    val burstMinutes: Int = 5,
    val growPause: Boolean = true,
    val breath: Boolean = true,
    val passesPerDay: Int = 3,
    val strict: Boolean = false,
    val pinSalt: String = "",
    val pinHash: String = "",
    val biometric: Boolean = false,
    val blockReels: Boolean = false,
    val uninstallProtection: Boolean = false,
    val theme: String = "dark",
    val language: String = "en",
) {
    val hasPin: Boolean get() = pinHash.isNotEmpty()
}

data class Segment(
    val packageName: String,
    val start: Long,
    val end: Long,
)

data class DayPoint(
    val date: LocalDate,
    val distractingMs: Long,
)

data class UsageSnapshot(
    val totals: Map<String, Long> = emptyMap(),
    val segments: List<Segment> = emptyList(),
    val permitted: Boolean = false,
    val at: Long = 0L,
)

data class Picture(
    val usage: UsageSnapshot = UsageSnapshot(),
    val history: List<DayPoint> = emptyList(),
    val logs: List<com.scrnlimit.screen.time.control.data.DayLog> = emptyList(),
)

data class InstalledApp(
    val packageName: String,
    val label: String,
    val game: Boolean,
)

data class PermissionSnapshot(
    val usage: Boolean = false,
    val overlay: Boolean = false,
    val notifications: Boolean = false,
    val accessibility: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val admin: Boolean = false,
)

object Catalog {
    data class Known(
        val packageName: String,
        val label: String,
        val color: Long,
        val suggested: Boolean,
        val defaultOn: Boolean,
    )

    val apps = listOf(
        Known("com.instagram.android", "Instagram", 0xFFE1306C, true, true),
        Known("com.google.android.youtube", "YouTube", 0xFFFF3B30, true, true),
        Known("com.zhiliaoapp.musically", "TikTok", 0xFF111111, true, true),
        Known("com.ss.android.ugc.trill", "TikTok", 0xFF111111, false, false),
        Known("com.twitter.android", "X", 0xFFF6F3EC, true, false),
        Known("com.facebook.katana", "Facebook", 0xFF1877F2, true, false),
        Known("com.reddit.frontpage", "Reddit", 0xFFFF4500, true, false),
    )

    val tikTok = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

    val reelsPackages = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.lite",
    )

    val browsers = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
    )

    val websiteSuggestions = listOf(
        "instagram.com",
        "youtube.com",
        "tiktok.com",
        "facebook.com",
        "x.com",
        "reddit.com",
    )

    const val QUESTION = "Is this important?"

    fun labelFor(packageName: String): String =
        apps.find { it.packageName == packageName }?.label ?: packageName.substringAfterLast('.')

    fun colorFor(packageName: String): Long =
        apps.find { it.packageName == packageName }?.color ?: 0xFFB8A6FF
}
