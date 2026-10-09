package com.scrnlimit.screen.time.control.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrnlimit.screen.time.control.domain.Category
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.domain.GuardPolicy
import com.scrnlimit.screen.time.control.domain.Segment
import com.scrnlimit.screen.time.control.ui.ScrnLimitViewModel
import com.scrnlimit.screen.time.control.ui.components.DayChart
import com.scrnlimit.screen.time.control.ui.components.HourStrip
import com.scrnlimit.screen.time.control.ui.components.Kicker
import com.scrnlimit.screen.time.control.ui.components.PackageIcon
import com.scrnlimit.screen.time.control.ui.components.SectionTitle
import com.scrnlimit.screen.time.control.ui.components.Segmented
import com.scrnlimit.screen.time.control.ui.components.StatTile
import com.scrnlimit.screen.time.control.ui.components.TimelineStrip
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.components.Headline
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.min

@Composable
fun InsightsScreen(vm: ScrnLimitViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val picture by vm.picture.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableIntStateOf(0) }
    val days = when (range) {
        1 -> 7
        2 -> 30
        else -> 1
    }
    val window = picture.history.takeLast(days)
    val distracting = apps.filter { it.category == Category.DISTRACTING }
    val packages = distracting.map { it.packageName }.toSet()
    val todayMs = GuardPolicy.totalFor(picture.usage.totals, packages)
    val rangeMs = if (range == 0) todayMs else window.sumOf { it.distractingMs }
    val productive = GuardPolicy.totalFor(picture.usage.totals, apps.filter { it.category == Category.PRODUCTIVE }.map { it.packageName }.toSet())
    val neutral = GuardPolicy.totalFor(picture.usage.totals, apps.filter { it.category == Category.NEUTRAL }.map { it.packageName }.toSet())
    val today = LocalDate.now()
    val log = picture.logs.find { it.epochDay == today.toEpochDay() }
    val hours = hourly(picture.usage.segments, packages)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Kicker(t("insights_kicker"))
        Spacer(Modifier.height(8.dp))
        Headline(t("where"))
        Spacer(Modifier.height(6.dp))
        Text(t("where_body"), color = SL.Mute, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Segmented(listOf(t("today"), t("days7"), t("days30")), range) { range = it }
        Spacer(Modifier.height(16.dp))
        Text(Formats.duration(rangeMs), color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 40.sp, maxLines = 1, softWrap = false)
        Text(if (range == 0) t("against", Formats.duration(settings.capMinutes * 60_000L)) else t("range_note"), color = SL.Mute, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        if (range == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(t("productive"), Formats.duration(productive), Modifier.weight(1f), SL.Productive)
                StatTile(t("neutral"), Formats.duration(neutral), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(t("productive_note"), color = SL.Faint, fontSize = 12.sp)
        } else {
            Spacer(Modifier.height(4.dp))
            DayChart(window)
        }
        Spacer(Modifier.height(20.dp))
        SectionTitle(t("by_app"))
        Spacer(Modifier.height(8.dp))
        val ranked = apps.filter { it.category != Category.PRODUCTIVE || (picture.usage.totals[it.packageName] ?: 0L) > 0 }
            .sortedByDescending { picture.usage.totals[it.packageName] ?: 0L }
        if (ranked.isEmpty() || ranked.all { (picture.usage.totals[it.packageName] ?: 0L) == 0L }) {
            Text(t("nothing"), color = SL.Mute, fontSize = 14.sp)
        } else {
            ranked.filter { (picture.usage.totals[it.packageName] ?: 0L) > 0L }.forEach { app ->
                val ms = picture.usage.totals[app.packageName] ?: 0L
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    PackageIcon(app.packageName, app.label, colorFor(app.category))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(app.label, color = SL.Paper, fontWeight = FontWeight.SemiBold)
                        Text("${Formats.duration(ms)} · ${Phrases.category(app.category)}", color = SL.Mute, fontSize = 12.sp)
                    }
                }
            }
        }
        if (range == 0) {
            Spacer(Modifier.height(12.dp))
            HourStrip(hours)
            Spacer(Modifier.height(16.dp))
            SectionTitle(t("timeline"))
            Spacer(Modifier.height(8.dp))
            val blocks = picture.usage.segments
                .filter { apps.any { app -> app.packageName == it.packageName } }
                .sortedBy { it.start }
                .takeLast(16)
                .map { segment ->
                    val category = apps.find { it.packageName == segment.packageName }?.category
                    val span = ((segment.end - segment.start) / 60_000f).coerceIn(0.2f, 8f)
                    colorFor(category) to span
                }
            TimelineStrip(blocks)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(t("stopped"), "${log?.opensStopped ?: 0}", Modifier.weight(1f))
            StatTile(t("passes"), "${log?.emergencyUsed ?: 0}", Modifier.weight(1f))
            StatTile(t("slips"), "${log?.slips ?: 0}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun colorFor(category: Category?): Color = when (category) {
    Category.DISTRACTING -> SL.Signal
    Category.PRODUCTIVE -> SL.Productive
    Category.NEUTRAL -> SL.Focus
    null -> SL.Mute
}

private fun hourly(segments: List<Segment>, packages: Set<String>): LongArray {
    val hours = LongArray(24)
    val zone = ZoneId.systemDefault()
    segments.filter { it.packageName in packages }.forEach { segment ->
        var cursor = segment.start
        while (cursor < segment.end) {
            val time = Instant.ofEpochMilli(cursor).atZone(zone)
            val boundary = time.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
            val next = min(segment.end, boundary)
            hours[time.hour] += (next - cursor).coerceAtLeast(0L)
            if (next <= cursor) break
            cursor = next
        }
    }
    return hours
}
