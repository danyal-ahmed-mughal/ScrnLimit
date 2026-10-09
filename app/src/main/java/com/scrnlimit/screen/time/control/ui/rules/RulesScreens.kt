@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.scrnlimit.screen.time.control.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrnlimit.screen.time.control.data.Schedule
import com.scrnlimit.screen.time.control.data.TrackedApp
import com.scrnlimit.screen.time.control.domain.Catalog
import com.scrnlimit.screen.time.control.domain.Category
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.domain.GuardPolicy
import com.scrnlimit.screen.time.control.domain.ScheduleKind
import com.scrnlimit.screen.time.control.ui.ScrnLimitViewModel
import com.scrnlimit.screen.time.control.ui.components.ChoiceChip
import com.scrnlimit.screen.time.control.ui.components.Kicker
import com.scrnlimit.screen.time.control.ui.components.PackageIcon
import com.scrnlimit.screen.time.control.ui.components.SLButton
import com.scrnlimit.screen.time.control.ui.components.SectionTitle
import com.scrnlimit.screen.time.control.ui.components.SwitchRow
import com.scrnlimit.screen.time.control.ui.components.slCard
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.components.Headline
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL

@Composable
fun AppsScreen(vm: ScrnLimitViewModel, onBack: () -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val current = apps.find { it.packageName == editing }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 22.dp)) {
        when {
            adding -> AddApp(vm) { adding = false }
            current != null -> EditApp(current, settings.capMinutes, vm) { editing = null }
            else -> AppList(apps, settings.capMinutes, onBack, { adding = true }, { editing = it })
        }
    }
}

@Composable
private fun AppList(
    apps: List<TrackedApp>,
    cap: Int,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Header(t("apps_limits"), t("back"), onBack)
        Text(t("apps_shared", Formats.duration(cap * 60_000L)), color = SL.Mute, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { category ->
                val rows = apps.filter { it.category == category }
                if (rows.isNotEmpty()) {
                    item { SectionTitle(Phrases.category(category)) }
                    items(rows, key = { it.packageName }) { app ->
                        Row(
                            Modifier.fillMaxWidth().slCard().clickable { onEdit(app.packageName) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PackageIcon(app.packageName, app.label, Color(Catalog.colorFor(app.packageName)))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(app.label, color = SL.Paper, fontWeight = FontWeight.SemiBold)
                                Text(
                                    app.dailyLimitMinutes?.let { t("limit_of", Formats.duration(it * 60_000L)) } ?: t("uses_shared"),
                                    color = SL.Mute,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
        SLButton(t("add_app"), onClick = onAdd)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AddApp(vm: ScrnLimitViewModel, onDone: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val tracked = vm.apps.collectAsStateWithLifecycle().value.map { it.packageName }.toSet()
    val matches = vm.installed.filter { it.packageName !in tracked && it.label.contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        Header(t("add_app"), t("close"), onDone)
        SearchField(query) { query = it }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches, key = { it.packageName }) { app ->
                Row(
                    Modifier.fillMaxWidth().slCard().clickable {
                        vm.addDistracting(app)
                        onDone()
                    }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PackageIcon(app.packageName, app.label, Color(Catalog.colorFor(app.packageName)))
                    Text(app.label, color = SL.Paper, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun EditApp(app: TrackedApp, cap: Int, vm: ScrnLimitViewModel, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(app.label, t("done"), onDone)
        Text(t("category"), color = SL.Mute, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { category ->
                ChoiceChip(Phrases.category(category), app.category == category) {
                    vm.setCategory(app, category)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(t("daily_limit"), color = SL.Mute, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(t("shared_chip", Formats.duration(cap * 60_000L)), app.dailyLimitMinutes == null) { vm.setAppLimit(app, null) }
            listOf(15, 30, 45, 60).forEach { minutes ->
                ChoiceChip(Formats.duration(minutes * 60_000L), app.dailyLimitMinutes == minutes) { vm.setAppLimit(app, minutes) }
            }
        }
        Spacer(Modifier.height(22.dp))
        SLButton(t("remove"), onClick = { vm.removeApp(app); onDone() }, secondary = true, tint = SL.Coral)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun SchedulesScreen(vm: ScrnLimitViewModel, onBack: () -> Unit) {
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 22.dp)) {
        Header(t("schedules"), t("back"), onBack)
        Text(t("schedules_body"), color = SL.Mute, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        if (schedules.isEmpty()) {
            Text(t("no_windows"), color = SL.Paper, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
        }
        schedules.forEach { schedule ->
            ScheduleCard(schedule, vm)
            Spacer(Modifier.height(10.dp))
        }
        SLButton(t("add_sleep_btn", Phrases.time(22 * 60 + 30), Phrases.time(7 * 60)), onClick = vm::addSleep)
        Spacer(Modifier.height(8.dp))
        SLButton(t("add_work_btn", Phrases.time(9 * 60), Phrases.time(17 * 60)), onClick = vm::addWork, secondary = true)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ScheduleCard(schedule: Schedule, vm: ScrnLimitViewModel) {
    val days = Phrases.dayLetters()
    Column(Modifier.fillMaxWidth().slCard().padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(if (schedule.kind == ScheduleKind.SLEEP) t("sleep") else t("work"), color = SL.Paper, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text("${Phrases.time(schedule.startMinute)} – ${Phrases.time(schedule.endMinute)}", color = SL.Mute, fontSize = 13.sp)
                Text(Phrases.dayNames(schedule.daysMask), color = SL.Faint, fontSize = 12.sp)
            }
        }
        SwitchRow(t("on"), t("window_off"), schedule.enabled) { vm.setScheduleEnabled(schedule, it) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { index, label ->
                val on = GuardPolicy.dayOn(schedule.daysMask, index)
                ChoiceChip(label, on) {
                    val next = schedule.daysMask xor (1 shl index)
                    if (next != 0) vm.saveSchedule(schedule.copy(daysMask = next))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(t("start_minus"), false) { vm.saveSchedule(schedule.copy(startMinute = (schedule.startMinute - 15 + 1440) % 1440)) }
            ChoiceChip(t("start_plus"), false) { vm.saveSchedule(schedule.copy(startMinute = (schedule.startMinute + 15) % 1440)) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(t("end_minus"), false) { vm.saveSchedule(schedule.copy(endMinute = (schedule.endMinute - 15 + 1440) % 1440)) }
            ChoiceChip(t("end_plus"), false) { vm.saveSchedule(schedule.copy(endMinute = (schedule.endMinute + 15) % 1440)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(t("delete"), color = SL.Coral, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { vm.deleteSchedule(schedule) }.padding(vertical = 6.dp))
    }
}

@Composable
fun WebsitesScreen(vm: ScrnLimitViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val websites by vm.websites.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    var custom by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 22.dp)) {
        Header(t("sites_title"), t("back"), onBack)
        Text(t("sites_body"), color = SL.Mute, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        val reelsLine = when {
            !permissions.accessibility -> t("reels_need")
            settings.blockReels -> t("reels_on")
            else -> t("reels_off_line")
        }
        SwitchRow(t("block_reels"), reelsLine, settings.blockReels, vm::setReels)
        if (!permissions.accessibility) {
            Text(t("access_off"), color = SL.Amber, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle(t("websites"))
        Spacer(Modifier.height(4.dp))
        Text(t("websites_body"), color = SL.Faint, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        val known = (Catalog.websiteSuggestions + websites.map { it.host }).distinct()
        known.forEach { host ->
            val enabled = websites.find { it.host == host }?.enabled == true
            SwitchRow(host, if (enabled) t("blocked_browser") else t("allowed_browser"), enabled) { vm.setSite(host, it) }
        }
        Spacer(Modifier.height(8.dp))
        SearchField(custom) { custom = it }
        Spacer(Modifier.height(8.dp))
        SLButton(t("block_site"), onClick = {
            val host = GuardPolicy.normalizeHost(custom)
            if (host != null) {
                vm.setSite(host, true)
                custom = ""
            }
        })
        Spacer(Modifier.height(12.dp))
        Text(t("site_note"), color = SL.Faint, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Header(title: String, action: String, onAction: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text(action, color = SL.Accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAction))
    Spacer(Modifier.height(6.dp))
    Headline(title)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(t("site_hint")) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SL.Signal,
            unfocusedBorderColor = SL.Line,
            focusedTextColor = SL.Paper,
            unfocusedTextColor = SL.Paper,
            cursorColor = SL.Signal,
            focusedContainerColor = SL.Card,
            unfocusedContainerColor = SL.Card,
            focusedPlaceholderColor = SL.Faint,
            unfocusedPlaceholderColor = SL.Faint,
        ),
    )
}
