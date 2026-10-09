package com.scrnlimit.screen.time.control.ui.home

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrnlimit.screen.time.control.domain.Catalog
import com.scrnlimit.screen.time.control.domain.Category
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.domain.GuardPolicy
import com.scrnlimit.screen.time.control.domain.Phase
import com.scrnlimit.screen.time.control.domain.ScheduleKind
import com.scrnlimit.screen.time.control.ui.ScrnLimitViewModel
import com.scrnlimit.screen.time.control.ui.SystemIntents
import com.scrnlimit.screen.time.control.ui.components.ChecklistRow
import com.scrnlimit.screen.time.control.ui.components.Kicker
import com.scrnlimit.screen.time.control.ui.components.LimitDial
import com.scrnlimit.screen.time.control.ui.components.PackageIcon
import com.scrnlimit.screen.time.control.ui.components.SLButton
import com.scrnlimit.screen.time.control.ui.components.SectionTitle
import com.scrnlimit.screen.time.control.ui.components.StatTile
import com.scrnlimit.screen.time.control.ui.components.UsageBar
import com.scrnlimit.screen.time.control.ui.components.slCard
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.components.Headline
import com.scrnlimit.screen.time.control.ui.components.LanguageChip
import com.scrnlimit.screen.time.control.ui.components.LanguageDialog
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL
import com.scrnlimit.screen.time.control.ui.theme.meterColor
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    vm: ScrnLimitViewModel,
    onApps: () -> Unit,
    onSchedules: () -> Unit,
    onFocus: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val picture by vm.picture.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val sessionCount by vm.sessionCount.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()

    val distracting = apps.filter { it.category == Category.DISTRACTING }
    val packages = distracting.map { it.packageName }.toSet()
    val used = GuardPolicy.totalFor(picture.usage.totals, packages)
    val ratio = GuardPolicy.ratio(used, settings.capMinutes)
    val today = LocalDate.now()
    val log = picture.logs.find { it.epochDay == today.toEpochDay() }
    val yesterday = picture.history.find { it.date == today.minusDays(1) }?.distractingMs ?: 0L
    val streak = GuardPolicy.streak(
        today = today,
        todayMs = used,
        capMinutes = settings.capMinutes,
        focusToday = log?.focusFinished == true,
        history = picture.history.associate { it.date to it.distractingMs },
        logs = picture.logs.associateBy { it.epochDay },
    )
    val showStreak = streak > 0 && (used > 0 || log?.focusFinished == true || (log?.opensStopped ?: 0) > 0 || streak > 1)
    val passesLeft = (settings.passesPerDay - (log?.emergencyUsed ?: 0)).coerceAtLeast(0)
    val evening = LocalTime.now().hour >= 18
    val left = (settings.capMinutes * 60_000L - used).coerceAtLeast(0L)
    val hero = if (picture.usage.permitted) Formats.hero(used) else "—"
    val overMs = (used - settings.capMinutes * 60_000L).coerceAtLeast(0L)
    var pickingLanguage by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Kicker(if (evening) t("evening") else t("today"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showStreak) {
                    Text(
                        t("streak", streak),
                        color = SL.OnSignal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(CircleShape).background(SL.Signal).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                LanguageChip(settings.language) { pickingLanguage = true }
            }
        }
        Spacer(Modifier.height(14.dp))
        Headline(t("stay"))
        Spacer(Modifier.height(18.dp))
        if (picture.usage.permitted) {
            Text(
                t("limit_start"),
                color = SL.Faint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LimitDial(progress = if (picture.usage.permitted) ratio else 0f, size = 268.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val heroSize = when {
                        hero.length >= 7 -> 30.sp
                        hero.length >= 6 -> 34.sp
                        else -> 44.sp
                    }
                    Text(hero, color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = heroSize, letterSpacing = (-1).sp, maxLines = 1, softWrap = false)
                    Text(t("of_cap", Formats.duration(settings.capMinutes * 60_000L)), color = SL.Mute, fontSize = 14.sp)
                    if (picture.usage.permitted && ratio >= 1f) {
                        Text(
                            t("over_short", Formats.duration(overMs)),
                            color = SL.Coral,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clip(CircleShape)
                                .background(SL.Coral.copy(alpha = 0.14f))
                                .border(1.dp, SL.Coral.copy(alpha = 0.85f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
        if (picture.usage.permitted && ratio >= 1f) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(SL.Signal))
                Text(
                    t("your_limit", Formats.duration(settings.capMinutes * 60_000L)),
                    color = SL.Mute,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 6.dp, end = 16.dp),
                )
                Box(Modifier.size(8.dp).clip(CircleShape).background(SL.Coral))
                Text(
                    t("extra_time"),
                    color = SL.Mute,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        if (pickingLanguage) {
            val activity = context as Activity
            LanguageDialog(
                selected = settings.language,
                onSelect = {
                    pickingLanguage = false
                    vm.setLanguage(it, activity)
                },
                onDismiss = { pickingLanguage = false },
            )
        }
        if (picture.usage.permitted && ratio < 1f) {
            Text(
                t("left_line", Formats.duration(left), Phrases.versus(used, yesterday)),
                color = SL.Mute,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        if (!permissions.usage) {
            SLButton(t("allow_usage")) { SystemIntents.safeStart(context, SystemIntents.usage(context)) }
            Spacer(Modifier.height(8.dp))
        }
        if (permissions.usage && !permissions.overlay) {
            SLButton(t("allow_draw"), secondary = true) { SystemIntents.safeStart(context, SystemIntents.overlay(context)) }
            Spacer(Modifier.height(8.dp))
        }
        val focusLabel = when {
            session?.phase == Phase.BREAK -> t("on_break")
            session != null -> t("focus_on")
            else -> t("start_focus")
        }
        SLButton(focusLabel, onClick = onFocus, tint = if (session != null) SL.Focus else SL.Signal)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(t("stopped"), "${log?.opensStopped ?: 0}", Modifier.weight(1f))
            StatTile(t("passes"), t("n_left", passesLeft), Modifier.weight(1f), if (passesLeft == 0) SL.Coral else SL.Paper)
            StatTile(t("slips"), "${log?.slips ?: 0}", Modifier.weight(1f), if ((log?.slips ?: 0) > 0) SL.Amber else SL.Paper)
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle(t("distracting"), t("edit"), onApps)
        Spacer(Modifier.height(10.dp))
        if (distracting.isEmpty()) {
            Text(t("no_apps"), color = SL.Mute, fontSize = 14.sp)
        } else {
            distracting.sortedByDescending { picture.usage.totals[it.packageName] ?: 0L }.forEach { app ->
                val ms = picture.usage.totals[app.packageName] ?: 0L
                val capMs = (app.dailyLimitMinutes ?: settings.capMinutes) * 60_000L
                val fraction = if (capMs == 0L) 0f else ms.toFloat() / capMs
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).slCard().padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PackageIcon(app.packageName, app.label, Color(Catalog.colorFor(app.packageName)))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(app.label, color = SL.Paper, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(Formats.duration(ms), color = if (fraction >= 1f) SL.Coral else SL.Mute, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        UsageBar(fraction, meterColor(fraction))
                    }
                }
            }
        }
        val needSleep = schedules.none { it.kind == ScheduleKind.SLEEP && it.enabled }
        val needPin = !settings.hasPin
        val needFocus = sessionCount == 0
        if (needSleep || needPin || needFocus) {
            Spacer(Modifier.height(18.dp))
            SectionTitle(t("still_open"))
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needSleep) ChecklistRow(t("sleep_title"), t("sleep_body"), onSchedules)
                if (needPin) ChecklistRow(t("pin_title"), t("pin_body"), onSettings)
                if (needFocus) ChecklistRow(t("focus_row"), t("focus_row_body"), onFocus)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(t("trust"), color = SL.Faint, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
    }
}
