package com.scrnlimit.screen.time.control.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.scrnlimit.screen.time.control.domain.DayPoint
import com.scrnlimit.screen.time.control.domain.Segment
import com.scrnlimit.screen.time.control.domain.UsageSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min

class UsageReader(context: Context) {
    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val zone: ZoneId = ZoneId.systemDefault()
    private var cursor = 0L
    private var foreground: String? = null
    private var historyCache: List<DayPoint> = emptyList()
    private var historyAt = 0L
    private var historyKey = 0

    fun pollForeground(): String? {
        val now = System.currentTimeMillis()
        val from = if (cursor == 0L) startOfDay(now) else cursor - 1_500L
        try {
            val events = usageStats.queryEvents(from, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND,
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    -> foreground = event.packageName
                    UsageEvents.Event.MOVE_TO_BACKGROUND,
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    -> if (foreground == event.packageName) foreground = null
                }
            }
            cursor = now
        } catch (_: SecurityException) {
            foreground = null
        }
        return foreground
    }

    fun readToday(): UsageSnapshot {
        val now = System.currentTimeMillis()
        val start = startOfDay(now)
        if (!Permissions.hasUsage(appContext)) {
            return UsageSnapshot(permitted = false, at = now)
        }
        return try {
            val segments = readSegments(start, now)
            val totals = totalsFrom(segments)
            UsageSnapshot(totals = totals, segments = segments, permitted = true, at = now)
        } catch (_: SecurityException) {
            UsageSnapshot(permitted = false, at = now)
        }
    }

    fun history(days: Int, distracting: Set<String>, todayMs: Long): List<DayPoint> {
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        if (!Permissions.hasUsage(appContext)) {
            return List(days) { index ->
                val date = today.minusDays((days - 1 - index).toLong())
                DayPoint(date, if (date == today) todayMs else 0L)
            }
        }
        val key = distracting.hashCode()
        if (now - historyAt > 120_000L || historyCache.size != days || historyKey != key) {
            historyKey = key
            val rangeStart = today.minusDays((days - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
            val byDay = HashMap<LocalDate, Long>()
            try {
                for (segment in readSegments(rangeStart, now)) {
                    if (segment.packageName !in distracting) continue
                    addToDays(segment, byDay)
                }
            } catch (_: SecurityException) {
                byDay.clear()
            }
            historyCache = (0 until days).map { index ->
                val date = today.minusDays((days - 1 - index).toLong())
                DayPoint(date, byDay[date] ?: 0L)
            }
            historyAt = now
        }
        return historyCache.map { point ->
            if (point.date == today) point.copy(distractingMs = todayMs) else point
        }
    }

    private fun readSegments(start: Long, end: Long): List<Segment> {
        val events = usageStats.queryEvents(start, end)
        val event = UsageEvents.Event()
        val open = HashMap<String, Long>()
        val segments = ArrayList<Segment>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED,
                -> if (pkg !in open) open[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                -> {
                    val began = open.remove(pkg) ?: continue
                    if (event.timeStamp - began >= 2_000L) {
                        segments.add(Segment(pkg, began, event.timeStamp))
                    }
                }
            }
        }
        open.forEach { (pkg, began) ->
            if (end - began >= 2_000L) segments.add(Segment(pkg, began, end))
        }
        return segments
    }

    private fun totalsFrom(segments: List<Segment>): Map<String, Long> =
        segments.groupBy { it.packageName }
            .mapValues { (_, items) -> items.sumOf { (it.end - it.start).coerceAtLeast(0L) } }

    private fun addToDays(segment: Segment, into: MutableMap<LocalDate, Long>) {
        var cursor = segment.start
        while (cursor < segment.end) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val boundary = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val sliceEnd = min(segment.end, boundary)
            if (sliceEnd > cursor) into[date] = (into[date] ?: 0L) + (sliceEnd - cursor)
            if (sliceEnd <= cursor) break
            cursor = sliceEnd
        }
    }

    private fun startOfDay(now: Long): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli().coerceAtMost(now)

    private val appContext = context.applicationContext
}
