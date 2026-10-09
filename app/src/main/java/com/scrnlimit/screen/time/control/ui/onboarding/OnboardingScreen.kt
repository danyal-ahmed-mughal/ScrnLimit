@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.scrnlimit.screen.time.control.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import com.scrnlimit.screen.time.control.domain.Catalog
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.ui.ScrnLimitViewModel
import com.scrnlimit.screen.time.control.ui.SystemIntents
import com.scrnlimit.screen.time.control.ui.components.AccessibilityConsent
import com.scrnlimit.screen.time.control.ui.components.ChoiceChip
import com.scrnlimit.screen.time.control.ui.components.Kicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.scrnlimit.screen.time.control.R
import com.scrnlimit.screen.time.control.ui.components.Monogram
import com.scrnlimit.screen.time.control.ui.components.PermissionLine
import com.scrnlimit.screen.time.control.ui.components.SLButton
import com.scrnlimit.screen.time.control.ui.components.Stepper
import com.scrnlimit.screen.time.control.ui.components.slCard
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.components.Headline
import com.scrnlimit.screen.time.control.ui.theme.Outfit
import com.scrnlimit.screen.time.control.ui.theme.SL
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(vm: ScrnLimitViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = page > 0) { page-- }
    Column(
        Modifier
            .fillMaxSize()
            .background(SL.Ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = page,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it / 5 * dir } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 5 * dir } + fadeOut())
            },
            label = "setup",
        ) { step ->
            when (step) {
                0 -> Promise(onNext = { page = 1 })
                1 -> ChooseApps(vm, onNext = { page = 2 })
                2 -> ChooseCap(vm, onNext = { page = 3 })
                else -> PermissionsStep(vm)
            }
        }
    }
}

@Composable
private fun Promise(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_brand),
            contentDescription = null,
            modifier = Modifier.size(168.dp),
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "ScrnLimit",
            modifier = Modifier.fillMaxWidth(),
            color = SL.Paper,
            fontFamily = Outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            letterSpacing = (-0.8).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            t("promise_body"),
            color = SL.Mute,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        SLButton(t("set_up"), onClick = onNext)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ChooseApps(vm: ScrnLimitViewModel, onNext: () -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    if (adding) {
        Column(Modifier.fillMaxSize()) {
            Headline(t("add_app"))
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(t("search")) },
                singleLine = true,
                colors = fieldColors(),
            )
            Spacer(Modifier.height(12.dp))
            val matches = vm.installed.filter { it.label.contains(query, true) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(matches, key = { it.packageName }) { app ->
                    AppPickRow(
                        label = app.label,
                        packageName = app.packageName,
                        color = Color(Catalog.colorFor(app.packageName)),
                        checked = app.packageName in vm.selected,
                        note = if (app.game) t("game") else null,
                        onClick = { vm.toggle(app.packageName) },
                    )
                }
            }
            SLButton(t("done"), onClick = { adding = false })
            Spacer(Modifier.height(12.dp))
        }
        return
    }
    val games = vm.installed.filter { it.game }
    val tiktok = Catalog.tikTok.filter { pkg -> vm.installed.any { it.packageName == pkg } }.ifEmpty { listOf(Catalog.tikTok.first()) }
    Column(Modifier.fillMaxSize()) {
        Kicker(t("step", 2, 4))
        Spacer(Modifier.height(8.dp))
        Headline(t("which_apps"))
        Spacer(Modifier.height(6.dp))
        Text(t("ignored"), color = SL.Mute, fontSize = 15.sp)
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SuggestionRow("Instagram", "com.instagram.android", vm)
            }
            item { SuggestionRow("YouTube", "com.google.android.youtube", vm) }
            item {
                val on = tiktok.any { it in vm.selected }
                AppPickRow(
                    label = "TikTok",
                    packageName = tiktok.first(),
                    color = Color(0xFFF6F3EC),
                    checked = on,
                    note = if (vm.installed.none { it.packageName in Catalog.tikTok }) t("not_installed") else null,
                    onClick = {
                        if (on) tiktok.filter { it in vm.selected }.forEach(vm::toggle)
                        else tiktok.filter { it !in vm.selected }.forEach(vm::toggle)
                    },
                )
            }
            item {
                AppPickRow(
                    label = t("games"),
                    packageName = "games",
                    color = Color(0xFF5B4DFF),
                    checked = vm.includeGames,
                    note = if (games.isEmpty()) t("none_installed") else t("n_on_phone", games.size),
                    onClick = { vm.setGames(!vm.includeGames) },
                )
            }
            item { SuggestionRow("X", "com.twitter.android", vm) }
            item { SuggestionRow("Facebook", "com.facebook.katana", vm) }
            item { SuggestionRow("Reddit", "com.reddit.frontpage", vm) }
            val extras = vm.selected.filter { pkg ->
                pkg !in Catalog.apps.map { it.packageName } && pkg !in Catalog.tikTok && games.none { it.packageName == pkg }
            }
            items(extras) { pkg ->
                val app = vm.installed.find { it.packageName == pkg }
                AppPickRow(
                    label = app?.label ?: Catalog.labelFor(pkg),
                    packageName = pkg,
                    color = Color(Catalog.colorFor(pkg)),
                    checked = true,
                    note = null,
                    onClick = { vm.toggle(pkg) },
                )
            }
            item {
                Text(
                    t("add_another"),
                    color = SL.Accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp).clickable { adding = true },
                )
            }
        }
        SLButton(t("continue_"), onClick = onNext, enabled = vm.selected.isNotEmpty())
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SuggestionRow(label: String, packageName: String, vm: ScrnLimitViewModel) {
    val known = Catalog.apps.find { it.packageName == packageName }
    AppPickRow(
        label = label,
        packageName = packageName,
        color = Color(known?.color ?: 0xFFF6F3EC),
        checked = packageName in vm.selected,
        note = if (vm.installed.none { it.packageName == packageName }) t("not_installed") else null,
        onClick = { vm.toggle(packageName) },
    )
}

@Composable
private fun AppPickRow(
    label: String,
    packageName: String,
    color: Color,
    checked: Boolean,
    note: String?,
    onClick: () -> Unit,
    monogram: String? = null,
) {
    Row(
        Modifier.fillMaxWidth().slCard().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (monogram != null) Monogram(monogram, color) else com.scrnlimit.screen.time.control.ui.components.PackageIcon(packageName, label, color)
        Spacer(Modifier.padding(6.dp))
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(label, color = SL.Paper, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (note != null) Text(note, color = SL.Faint, fontSize = 12.sp)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) SL.Signal else SL.CardHigh)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(if (checked) t("on") else t("off"), color = if (checked) SL.OnSignal else SL.Mute, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ChooseCap(vm: ScrnLimitViewModel, onNext: () -> Unit) {
    val caps = listOf(15, 30, 45, 60, 90, 120, 180)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Kicker(t("step", 3, 4), modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        Headline(t("one_limit"))
        Spacer(Modifier.height(8.dp))
        Text(
            t("one_limit_body"),
            color = SL.Mute,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        Stepper(Formats.duration(vm.capDraft * 60_000L), { vm.chooseCap(vm.capDraft - 15) }, { vm.chooseCap(vm.capDraft + 15) })
        Spacer(Modifier.height(18.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            caps.forEach { minutes ->
                ChoiceChip(Formats.duration(minutes * 60_000L), minutes == vm.capDraft) { vm.chooseCap(minutes) }
            }
        }
        Spacer(Modifier.weight(1f))
        SLButton(t("continue_"), onClick = onNext)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PermissionsStep(vm: ScrnLimitViewModel) {
    val context = LocalContext.current
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    var accessAsk by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Kicker(t("step", 4, 4))
        Spacer(Modifier.height(8.dp))
        Headline(t("allow_pause"))
        Spacer(Modifier.height(14.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                PermissionLine(t("usage"), t("usage_body"), permissions.usage, t("allow")) {
                    SystemIntents.safeStart(context, SystemIntents.usage(context))
                }
            }
            item {
                PermissionLine(t("overlay"), t("overlay_body"), permissions.overlay, t("allow")) {
                    SystemIntents.safeStart(context, SystemIntents.overlay(context))
                }
            }
            item {
                PermissionLine(t("notifications"), t("notifications_body"), permissions.notifications, t("allow")) {
                    if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.refresh()
                }
            }
            item {
                PermissionLine(
                    t("reels"),
                    t("reels_body"),
                    permissions.accessibility,
                    t("turn_on"),
                ) {
                    accessAsk = true
                }
                if (accessAsk) {
                    AccessibilityConsent(
                        onAgree = {
                            accessAsk = false
                            vm.setReels(true)
                            SystemIntents.safeStart(context, SystemIntents.accessibility())
                        },
                        onDismiss = { accessAsk = false },
                    )
                }
            }
            item {
                Text(
                    t("slip_note"),
                    color = SL.Faint,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        val next = when {
            !permissions.usage -> t("allow_usage") to { SystemIntents.safeStart(context, SystemIntents.usage(context)) }
            !permissions.overlay -> t("allow_overlay") to { SystemIntents.safeStart(context, SystemIntents.overlay(context)) }
            !permissions.notifications && Build.VERSION.SDK_INT >= 33 -> t("allow_notifications") to { notify.launch(Manifest.permission.POST_NOTIFICATIONS) }
            else -> t("start") to { vm.finishSetup() }
        }
        SLButton(next.first, onClick = next.second)
        Spacer(Modifier.height(8.dp))
        Text(
            t("not_now"),
            color = SL.Mute,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally).clickable { vm.finishSetup() }.padding(8.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun fieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SL.Signal,
    unfocusedBorderColor = SL.Line,
    focusedTextColor = SL.Paper,
    unfocusedTextColor = SL.Paper,
    cursorColor = SL.Signal,
    focusedContainerColor = SL.Card,
    unfocusedContainerColor = SL.Card,
    focusedPlaceholderColor = SL.Faint,
    unfocusedPlaceholderColor = SL.Faint,
)
