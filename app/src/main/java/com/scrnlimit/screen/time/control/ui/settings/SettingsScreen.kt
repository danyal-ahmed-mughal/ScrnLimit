@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.scrnlimit.screen.time.control.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrnlimit.screen.time.control.block.MonitorService
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.ui.PlayReview
import com.scrnlimit.screen.time.control.ui.ScrnLimitViewModel
import com.scrnlimit.screen.time.control.ui.SystemIntents
import com.scrnlimit.screen.time.control.ui.components.AccessibilityConsent
import com.scrnlimit.screen.time.control.ui.components.ChoiceChip
import com.scrnlimit.screen.time.control.ui.components.Kicker
import com.scrnlimit.screen.time.control.ui.components.PermissionLine
import com.scrnlimit.screen.time.control.ui.components.SLButton
import com.scrnlimit.screen.time.control.ui.components.SwitchRow
import com.scrnlimit.screen.time.control.ui.components.slCard
import android.app.Activity
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.components.Headline
import com.scrnlimit.screen.time.control.ui.components.LanguageDialog
import com.scrnlimit.screen.time.control.ui.components.LanguageField
import com.scrnlimit.screen.time.control.ui.theme.SL

@Composable
fun SettingsScreen(
    vm: ScrnLimitViewModel,
    onApps: () -> Unit,
    onSchedules: () -> Unit,
    onWebsites: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    var accessAsk by remember { mutableStateOf(false) }
    val biometric = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Kicker(t("kicker_control"))
        Spacer(Modifier.height(8.dp))
        Headline(t("settings"))
        Spacer(Modifier.height(16.dp))
        val activity = context as Activity
        var pickingLanguage by remember { mutableStateOf(false) }
        Group(t("appearance")) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(t("theme_system"), settings.theme == "system") { vm.setTheme("system", activity) }
                ChoiceChip(t("theme_light"), settings.theme == "light") { vm.setTheme("light", activity) }
                ChoiceChip(t("theme_dark"), settings.theme == "dark") { vm.setTheme("dark", activity) }
            }
        }
        Group(t("language")) {
            LanguageField(settings.language) { pickingLanguage = true }
        }
        if (pickingLanguage) {
            LanguageDialog(
                selected = settings.language,
                onSelect = {
                    pickingLanguage = false
                    vm.setLanguage(it, activity)
                },
                onDismiss = { pickingLanguage = false },
            )
        }
        Group(t("the_cap")) {
            Text(t("shared_cap"), color = SL.Paper, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60, 90, 120, 180).forEach { minutes ->
                    ChoiceChip(Formats.duration(minutes * 60_000L), settings.capMinutes == minutes) { vm.setCap(minutes) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(t("apps_limits"), color = SL.Accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onApps))
        }
        Group(t("the_pause")) {
            Text(t("wait_open"), color = SL.Mute, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 8, 10, 12, 15).forEach { seconds ->
                    ChoiceChip("${seconds}s", settings.pauseSeconds == seconds) { vm.setPause(seconds) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(t("then_allow"), color = SL.Mute, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10, 15).forEach { minutes ->
                    ChoiceChip("${minutes}m", settings.burstMinutes == minutes) { vm.setBurst(minutes) }
                }
            }
            SwitchRow(t("wait_grows"), t("wait_grows_body"), settings.growPause, vm::setGrow)
            SwitchRow(t("breath"), t("breath_body"), settings.breath, vm::setBreath)
        }
        Group(t("strict")) {
            Text(t("passes_day"), color = SL.Mute, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..6).forEach { count ->
                    ChoiceChip("$count", settings.passesPerDay == count) { vm.setPasses(count) }
                }
            }
            SwitchRow(t("strict"), t("strict_body"), settings.strict, vm::setStrict)
            Text(
                if (settings.hasPin) t("change_pin") else t("set_pin"),
                color = SL.Accent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { vm.beginPinChange() }.padding(vertical = 6.dp),
            )
            if (biometric && settings.hasPin) {
                SwitchRow(t("fingerprint"), t("fingerprint_body"), settings.biometric, vm::setBiometric)
            }
        }
        Group(t("blocks")) {
            Text(t("schedules"), color = SL.Accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onSchedules).padding(vertical = 6.dp))
            Text(t("sites_reels"), color = SL.Accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onWebsites).padding(vertical = 6.dp))
        }
        Group(t("permissions")) {
            PermissionLine(t("usage"), t("usage_short"), permissions.usage, t("allow")) {
                SystemIntents.safeStart(context, SystemIntents.usage(context))
            }
            Spacer(Modifier.height(8.dp))
            PermissionLine(t("overlay"), t("overlay_short"), permissions.overlay, t("allow")) {
                SystemIntents.safeStart(context, SystemIntents.overlay(context))
            }
            Spacer(Modifier.height(8.dp))
            PermissionLine(t("notifications"), t("notes_short"), permissions.notifications, t("allow")) {
                if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            Spacer(Modifier.height(8.dp))
            PermissionLine(t("reels"), t("access_short"), permissions.accessibility, t("allow")) {
                accessAsk = true
            }
            if (accessAsk) {
                AccessibilityConsent(
                    onAgree = {
                        accessAsk = false
                        SystemIntents.safeStart(context, SystemIntents.accessibility())
                    },
                    onDismiss = { accessAsk = false },
                )
            }
            Spacer(Modifier.height(8.dp))
            PermissionLine(t("battery"), t("battery_body"), permissions.batteryUnrestricted, t("allow")) {
                SystemIntents.safeStart(context, SystemIntents.battery(context))
            }
        }
        Group(t("harder")) {
            Text(
                if (MonitorService.running) t("watcher_on") else t("watcher_off"),
                color = if (MonitorService.running) SL.Signal else SL.Amber,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            if (permissions.admin) {
                SLButton(t("remove_admin"), onClick = vm::removeProtection, secondary = true, tint = SL.Coral)
            } else {
                SLButton(t("ask_uninstall"), secondary = true) {
                    SystemIntents.safeStart(context, SystemIntents.admin(context))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(t("force_stop"), color = SL.Faint, fontSize = 13.sp)
        }
        Group(t("rate_title")) {
            Text(t("rate_row_body"), color = SL.Mute, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            SLButton(t("rate_now")) {
                val host = context as? Activity
                if (host != null) PlayReview.request(host) else PlayReview.openStore(context)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            t("about"),
            color = SL.Mute,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Kicker(title)
    Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxWidth().slCard().padding(14.dp)) { content() }
    Spacer(Modifier.height(10.dp))
}
