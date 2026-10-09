package com.scrnlimit.screen.time.control.domain

import com.scrnlimit.screen.time.control.data.DayLog
import com.scrnlimit.screen.time.control.data.FocusSession
import com.scrnlimit.screen.time.control.data.Schedule
import com.scrnlimit.screen.time.control.data.TrackedApp
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GuardPolicy {
    fun pauseSeconds(base: Int, opensAlready: Int, grow: Boolean): Int {
        val start = base.coerceIn(5, 15)
        if (!grow) return start
        return (start + opensAlready.coerceAtLeast(0) * 3).coerceAtMost(15)
    }

    fun distractingPackages(apps: List<TrackedApp>): Set<String> =
        apps.filter { it.category == Category.DISTRACTING }.map { it.packageName }.toSet()

    fun totalFor(totals: Map<String, Long>, packages: Set<String>): Long =
        packages.sumOf { totals[it] ?: 0L }

    fun ratio(usedMs: Long, capMinutes: Int): Float {
        if (capMinutes <= 0) return 1f
        return usedMs.toFloat() / (capMinutes * 60_000f)
    }

    fun reason(
        packageName: String,
        apps: List<TrackedApp>,
        totals: Map<String, Long>,
        capMinutes: Int,
        schedules: List<Schedule>,
        now: ZonedDateTime,
        focusActive: Boolean,
    ): BlockReason {
        val rule = apps.find { it.packageName == packageName } ?: return BlockReason.NONE
        if (rule.category != Category.DISTRACTING) return BlockReason.NONE
        if (focusActive) return BlockReason.FOCUS
        if (schedules.any { it.enabled && scheduleActive(it, now) }) return BlockReason.SCHEDULE
        val used = totals[packageName] ?: 0L
        val perApp = rule.dailyLimitMinutes
        if (perApp != null && used >= perApp * 60_000L) return BlockReason.APP_CAP
        val group = totalFor(totals, distractingPackages(apps))
        if (group >= capMinutes * 60_000L) return BlockReason.CAP
        return BlockReason.NONE
    }

    fun scheduleActive(schedule: Schedule, now: ZonedDateTime): Boolean {
        if (!schedule.enabled) return false
        val minute = now.hour * 60 + now.minute
        val today = now.dayOfWeek.value - 1
        val wraps = schedule.endMinute <= schedule.startMinute
        if (!wraps) {
            if (!dayOn(schedule.daysMask, today)) return false
            return minute >= schedule.startMinute && minute < schedule.endMinute
        }
        return if (minute >= schedule.startMinute) {
            dayOn(schedule.daysMask, today)
        } else if (minute < schedule.endMinute) {
            dayOn(schedule.daysMask, (today + 6) % 7)
        } else {
            false
        }
    }

    fun dayOn(mask: Int, index: Int): Boolean = mask and (1 shl index) != 0

    fun mask(days: Set<Int>): Int = days.fold(0) { acc, day -> acc or (1 shl day) }

    fun days(mask: Int): Set<Int> = (0..6).filter { dayOn(mask, it) }.toSet()

    fun advanceSession(session: FocusSession, now: Long): FocusSession {
        if (session.endedAt != null) return session
        var current = session
        var guard = 0
        while (current.endedAt == null && now >= current.phaseEndsAt && guard < 48) {
            current = stepSession(current)
            guard++
        }
        return current
    }

    private fun stepSession(session: FocusSession): FocusSession {
        val slice = ((session.phaseEndsAt - session.phaseStartedAt) / 60_000L).toInt().coerceAtLeast(1)
        if (session.phase == Phase.BREAK) {
            val next = min(25, session.focusGoalMinutes - session.focusDoneMinutes).coerceAtLeast(1)
            return session.copy(
                phase = Phase.FOCUS,
                phaseStartedAt = session.phaseEndsAt,
                phaseEndsAt = session.phaseEndsAt + next * 60_000L,
            )
        }
        val done = session.focusDoneMinutes + slice
        if (!session.pomodoro || done >= session.focusGoalMinutes) {
            return session.copy(
                focusDoneMinutes = session.focusGoalMinutes,
                phase = Phase.FOCUS,
                endedAt = session.phaseEndsAt,
                completed = true,
            )
        }
        return session.copy(
            focusDoneMinutes = done,
            phase = Phase.BREAK,
            phaseStartedAt = session.phaseEndsAt,
            phaseEndsAt = session.phaseEndsAt + 5 * 60_000L,
        )
    }

    fun streak(
        today: LocalDate,
        todayMs: Long,
        capMinutes: Int,
        focusToday: Boolean,
        history: Map<LocalDate, Long>,
        logs: Map<Long, DayLog>,
    ): Int {
        val todayHeld = todayMs <= capMinutes * 60_000L || focusToday
        var date = if (todayHeld) today else today.minusDays(1)
        var count = 0
        while (count < 3650) {
            val epoch = date.toEpochDay()
            val log = logs[epoch]
            val hist = history[date]
            val isToday = date == today
            if (!isToday && log == null && (hist == null || hist == 0L)) break
            val ms = if (isToday) todayMs else max(log?.distractingMs ?: 0L, hist ?: 0L)
            val cap = if (isToday) capMinutes else log?.capMinutes ?: capMinutes
            val focus = if (isToday) focusToday || log?.focusFinished == true else log?.focusFinished == true
            if (ms > cap * 60_000L && !focus) break
            count++
            date = date.minusDays(1)
        }
        return count
    }

    fun hostBlocked(host: String, enabledHosts: List<String>): Boolean {
        val clean = host.lowercase().removePrefix("www.")
        return enabledHosts.any { rule ->
            val needle = rule.lowercase().removePrefix("www.")
            clean == needle || clean.endsWith(".$needle")
        }
    }

    fun normalizeHost(raw: String): String? {
        var value = raw.trim().lowercase()
        if (value.isEmpty()) return null
        value = value.removePrefix("https://").removePrefix("http://")
        value = value.substringBefore("/").substringBefore("?")
        value = value.removePrefix("www.")
        if (!value.contains('.') || value.contains(' ') || value.contains('@')) return null
        return value
    }
}

object Formats {
    fun duration(ms: Long): String {
        val minutes = (ms / 60_000L).toInt().coerceAtLeast(0)
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours > 0 && rest > 0 -> "${hours}h ${rest}m"
            hours > 0 -> "${hours}h"
            else -> "${rest}m"
        }
    }

    fun hero(ms: Long): String = duration(ms)

    fun clock(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0)
        val minutes = total / 60
        val seconds = total % 60
        return if (minutes >= 60) duration(ms) else "%d:%02d".format(minutes, seconds)
    }

    fun minuteLabel(minute: Int): String {
        val normalized = ((minute % (24 * 60)) + (24 * 60)) % (24 * 60)
        val hour24 = normalized / 60
        val min = normalized % 60
        val suffix = if (hour24 < 12) "AM" else "PM"
        val hour = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }
        return "%d:%02d %s".format(hour, min, suffix)
    }

    fun versus(today: Long, yesterday: Long): String {
        val delta = ((today - yesterday) / 60_000L).toInt()
        return when {
            kotlin.math.abs(delta) < 1 -> "Same as yesterday"
            delta < 0 -> "${-delta}m under yesterday"
            else -> "${delta}m over yesterday"
        }
    }

    fun status(ratio: Float, permitted: Boolean): String = when {
        !permitted -> "Not counting"
        ratio >= 1f -> "Over the cap"
        ratio >= 0.8f -> "Close to the cap"
        else -> "In control"
    }

    fun reasonTitle(reason: BlockReason): String = when (reason) {
        BlockReason.CAP -> "You've hit the cap"
        BlockReason.APP_CAP -> "This app hit its limit"
        BlockReason.SCHEDULE -> "This time is blocked"
        BlockReason.FOCUS -> "Focus is on"
        BlockReason.BURST_END -> "That's the burst"
        BlockReason.SITE -> "This site is blocked"
        BlockReason.NONE -> ""
    }

    fun reasonBody(reason: BlockReason): String = when (reason) {
        BlockReason.CAP -> "The distracting set is at its daily limit."
        BlockReason.APP_CAP -> "The others stay open until the shared cap."
        BlockReason.SCHEDULE -> "A work or sleep window is on."
        BlockReason.FOCUS -> "These apps stay closed until the session ends."
        BlockReason.BURST_END -> "Close it, or spend an emergency pass."
        BlockReason.SITE -> "You blocked this site."
        BlockReason.NONE -> ""
    }

    fun shortDay(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))

    fun hourLabel(hour: Int): String {
        val suffix = if (hour < 12) "a" else "p"
        val h = when (val value = hour % 12) {
            0 -> 12
            else -> value
        }
        return "$h$suffix"
    }

    fun dayNames(mask: Int): String {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val on = labels.filterIndexed { index, _ -> GuardPolicy.dayOn(mask, index) }
        if (on.size == 7) return "Every day"
        if (on == listOf("Mon", "Tue", "Wed", "Thu", "Fri")) return "Weekdays"
        if (on == listOf("Sat", "Sun")) return "Weekends"
        return on.joinToString(" ")
    }

    fun passesLine(left: Int): String = when (left) {
        0 -> "No passes left today"
        1 -> "1 left today"
        else -> "$left left today"
    }

    fun ratioPercent(ratio: Float): Int = (ratio * 100).roundToInt().coerceAtLeast(0)
}
