package com.scrnlimit.screen.time.control.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scrnlimit.screen.time.control.block.MonitorService
import com.scrnlimit.screen.time.control.ui.focus.FocusScreen
import com.scrnlimit.screen.time.control.ui.gate.CreatePin
import com.scrnlimit.screen.time.control.ui.gate.PinGate
import com.scrnlimit.screen.time.control.ui.home.HomeScreen
import com.scrnlimit.screen.time.control.ui.home.HowItWorks
import com.scrnlimit.screen.time.control.ui.insights.InsightsScreen
import com.scrnlimit.screen.time.control.ui.onboarding.OnboardingScreen
import com.scrnlimit.screen.time.control.ui.rules.AppsScreen
import com.scrnlimit.screen.time.control.ui.rules.SchedulesScreen
import com.scrnlimit.screen.time.control.ui.rules.WebsitesScreen
import com.scrnlimit.screen.time.control.ui.settings.SettingsScreen
import com.scrnlimit.screen.time.control.i18n.t
import com.scrnlimit.screen.time.control.ui.components.RateDialog
import com.scrnlimit.screen.time.control.ui.theme.SL

private object Routes {
    const val Home = "home"
    const val Insights = "insights"
    const val Focus = "focus"
    const val Settings = "settings"
    const val Apps = "apps"
    const val Schedules = "schedules"
    const val Websites = "websites"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun Root(vm: ScrnLimitViewModel) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val gate by vm.gate.collectAsStateWithLifecycle()
    val creating by vm.creatingPin.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var askRate by remember { mutableStateOf(false) }
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded) MonitorService.start(context)
    }
    LaunchedEffect(settings.onboarded, settings.seenHow) {
        if (settings.onboarded && settings.seenHow && ReviewPrefs.shouldAsk(context)) askRate = true
    }
    Box(Modifier.fillMaxSize().background(SL.Ink)) {
        when {
            !settings.ready -> Box(Modifier.fillMaxSize().background(SL.Ink))
            !settings.onboarded -> OnboardingScreen(vm)
            !settings.seenHow -> HowItWorks(vm::dismissHow)
            else -> Shell(vm)
        }
        if (creating) {
            CreatePin(onCancel = vm::cancelPinCreate, onSave = vm::saveCreatedPin)
        } else if (gate != null) {
            PinGate(
                request = gate!!,
                biometric = settings.biometric,
                verify = vm::verify,
                onSubmit = vm::submitGate,
            )
        } else if (askRate && settings.onboarded) {
            val activity = context as? Activity
            RateDialog(
                onRate = {
                    ReviewPrefs.markAsked(context)
                    askRate = false
                    if (activity != null) PlayReview.request(activity) else PlayReview.openStore(context)
                },
                onLater = {
                    ReviewPrefs.markAsked(context)
                    askRate = false
                },
            )
        }
    }
}

@Composable
private fun Shell(vm: ScrnLimitViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        containerColor = SL.Ink,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val tabs = listOf(
                Tab(Routes.Home, t("nav_home"), Icons.Filled.Home),
                Tab(Routes.Insights, t("nav_insights"), Icons.Filled.BarChart),
                Tab(Routes.Focus, t("nav_focus"), Icons.Filled.Timer),
                Tab(Routes.Settings, t("nav_settings"), Icons.Filled.Settings),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(SL.Ink)
                    .navigationBarsPadding()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val selected = route == tab.route
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(tab.icon, contentDescription = tab.label, tint = if (selected) SL.Accent else SL.Faint)
                        Spacer(Modifier.height(2.dp))
                        Text(tab.label, color = if (selected) SL.Accent else SL.Faint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.Home, modifier = Modifier.padding(padding)) {
            composable(Routes.Home) {
                HomeScreen(
                    vm = vm,
                    onApps = { nav.navigate(Routes.Apps) },
                    onSchedules = { nav.navigate(Routes.Schedules) },
                    onFocus = { nav.navigate(Routes.Focus) },
                    onSettings = { nav.navigate(Routes.Settings) },
                )
            }
            composable(Routes.Insights) { InsightsScreen(vm) }
            composable(Routes.Focus) { FocusScreen(vm) }
            composable(Routes.Settings) {
                SettingsScreen(
                    vm,
                    onApps = { nav.navigate(Routes.Apps) },
                    onSchedules = { nav.navigate(Routes.Schedules) },
                    onWebsites = { nav.navigate(Routes.Websites) },
                )
            }
            composable(Routes.Apps) { AppsScreen(vm) { nav.popBackStack() } }
            composable(Routes.Schedules) { SchedulesScreen(vm) { nav.popBackStack() } }
            composable(Routes.Websites) { WebsitesScreen(vm) { nav.popBackStack() } }
        }
    }
}
