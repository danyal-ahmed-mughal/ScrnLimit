@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.scrnlimit.screen.time.control.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.domain.Phase
import com.scrnlimit.screen.time.control.ui.ScrnLimitViewModel
import com.scrnlimit.screen.time.control.ui.components.ChoiceChip
import com.scrnlimit.screen.time.control.ui.components.Kicker
import com.scrnlimit.screen.time.control.ui.components.MeterRing
import com.scrnlimit.screen.time.control.ui.components.SLButton
import com.scrnlimit.screen.time.control.ui.components.SwitchRow
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.components.Headline
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(vm: ScrnLimitViewModel) {
    val session by vm.session.collectAsStateWithLifecycle()
    val active = session?.takeIf { it.endedAt == null }
    if (active != null) {
        ActiveFocus(
            goal = active.focusGoalMinutes,
            pomodoro = active.pomodoro,
            phase = active.phase,
            phaseStarted = active.phaseStartedAt,
            phaseEnds = active.phaseEndsAt,
            doneMinutes = active.focusDoneMinutes,
            onEnd = vm::endFocus,
            onElapsed = vm::refresh,
        )
    } else {
        Picker(onStart = vm::startFocus)
    }
}

@Composable
private fun Picker(onStart: (Int, Boolean) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var pomodoro by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Kicker(t("nav_focus"), color = SL.Focus)
        Spacer(Modifier.height(8.dp))
        Headline(t("focus_title"))
        Spacer(Modifier.height(8.dp))
        Text(t("focus_body"), color = SL.Mute, fontSize = 15.sp)
        Spacer(Modifier.height(22.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(25, 50, 90).forEach { option ->
                ChoiceChip(t("min_chip", option), minutes == option) { minutes = option }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(t("custom"), color = SL.Mute, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 40, 60, 120).forEach { option ->
                ChoiceChip("$option", minutes == option) { minutes = option }
            }
        }
        Spacer(Modifier.height(8.dp))
        SwitchRow(
            t("pomodoro"),
            t("pomodoro_body"),
            pomodoro,
        ) { pomodoro = it }
        Spacer(Modifier.height(22.dp))
        SLButton(t("focus_for", minutes), onClick = { onStart(minutes, pomodoro) }, tint = SL.Focus)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActiveFocus(
    goal: Int,
    pomodoro: Boolean,
    phase: Phase,
    phaseStarted: Long,
    phaseEnds: Long,
    doneMinutes: Int,
    onEnd: () -> Unit,
    onElapsed: () -> Unit,
) {
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var pulsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(phaseEnds) {
        pulsed = false
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val left = (phaseEnds - now).coerceAtLeast(0L)
    LaunchedEffect(left, phaseEnds) {
        if (left == 0L && !pulsed) {
            pulsed = true
            onElapsed()
        }
    }
    val span = (phaseEnds - phaseStarted).coerceAtLeast(1L)
    val progress = 1f - left.toFloat() / span.toFloat()
    val breakTime = phase == Phase.BREAK
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Kicker(if (breakTime) t("break_label") else t("nav_focus"), color = SL.Focus)
        Spacer(Modifier.height(28.dp))
        MeterRing(progress = progress.coerceIn(0f, 1f), color = if (breakTime) SL.Productive else SL.Focus) {
            Text(Formats.clock(left), color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 44.sp, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (breakTime) t("apps_open") else t("apps_closed"),
            color = SL.Mute,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (pomodoro) t("focused_of", doneMinutes, goal) else t("session_min", goal),
            color = SL.Paper,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        SLButton(t("end_session"), onClick = onEnd, secondary = true, tint = SL.Coral)
        Spacer(Modifier.height(22.dp))
    }
}
