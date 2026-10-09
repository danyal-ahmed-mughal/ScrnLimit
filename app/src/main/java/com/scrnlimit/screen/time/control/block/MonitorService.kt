package com.scrnlimit.screen.time.control.block

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.scrnlimit.screen.time.control.ScrnLimitApp
import com.scrnlimit.screen.time.control.data.Notifier
import com.scrnlimit.screen.time.control.domain.BlockReason
import com.scrnlimit.screen.time.control.domain.Catalog
import com.scrnlimit.screen.time.control.domain.Category
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.domain.GuardPolicy
import com.scrnlimit.screen.time.control.domain.Phase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZonedDateTime

class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loop: Job? = null
    private lateinit var overlay: OverlayController
    private var tracking: String? = null
    private var sawLiveBurst = false
    private var shown: String? = null
    private var slipMarked = false
    private var lastHeavy = 0L
    private var siteHold: String? = null
    private var siteAllowedHost: String? = null
    private var siteAllowedUntil = 0L

    private val graph get() = (application as ScrnLimitApp).graph

    override fun onCreate() {
        super.onCreate()
        instance = this
        running = true
        GuardMemory.init(this)
        Notifier.ensure(this)
        overlay = OverlayController(this)
        try {
            val note = Notifier.ongoing(this, com.scrnlimit.screen.time.control.i18n.Phrases.text("watcher_note"))
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(Notifier.ONGOING, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(Notifier.ONGOING, note)
            }
        } catch (_: Exception) {
            running = false
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) return START_NOT_STICKY
        if (loop == null) loop = scope.launch { watch() }
        return START_STICKY
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        running = false
        loop?.cancel()
        scope.cancel()
        if (::overlay.isInitialized) overlay.hideAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun onReels(show: Boolean) {
        if (!::overlay.isInitialized) return
        val settings = graph.repo.settings.value
        if (!show || !settings.blockReels || overlay.pauseVisible) {
            overlay.hideCover()
            return
        }
        overlay.showCover(
            title = com.scrnlimit.screen.time.control.i18n.Phrases.text("reels_blocked"),
            body = com.scrnlimit.screen.time.control.i18n.Phrases.text("reels_blocked_body"),
            action = com.scrnlimit.screen.time.control.i18n.Phrases.text("go_back"),
            onAction = {
                FeedAccessibilityService.pressBack()
                overlay.hideCover()
            },
        )
    }

    fun onSite(host: String) {
        if (!::overlay.isInitialized) return
        val now = System.currentTimeMillis()
        if (siteAllowedHost == host && now < siteAllowedUntil) return
        val enabled = graph.repo.websites.value.filter { it.enabled }.map { it.host }
        if (!GuardPolicy.hostBlocked(host, enabled)) {
            onSiteClear()
            return
        }
        if (overlay.pauseVisible && shown == "STOP") return
        siteHold = host
        val browser = tracking?.takeIf { it in Catalog.browsers } ?: tracking ?: host
        scope.launch { showStop(browser, host, BlockReason.SITE, host) }
    }

    fun onSiteClear() {
        if (siteHold == null) return
        siteHold = null
        if (shown == "STOP") {
            shown = null
            overlay.hidePause()
        }
    }

    private suspend fun watch() {
        val ready = withTimeoutOrNull(3_000) { graph.repo.settings.first { it.onboarded } }
        if (ready == null && !graph.repo.settings.value.onboarded) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        while (scope.isActive) {
            val foreground = withContext(Dispatchers.IO) { graph.usage.pollForeground() }
            val now = System.currentTimeMillis()
            if (now - lastHeavy > 5_000L) {
                lastHeavy = now
                withContext(Dispatchers.IO) { runCatching { graph.repo.refresh(true) } }
            }
            handle(foreground)
            delay(1_000)
        }
    }

    private suspend fun handle(foreground: String?) {
        if (foreground == "com.android.systemui") return
        val fg = foreground
        if (fg != tracking) {
            tracking = fg
            slipMarked = false
            shown = null
            val armed = GuardMemory.armed()
            sawLiveBurst = armed != null && armed == fg
            if (armed != fg) GuardMemory.clearArmed()
            siteHold = null
            overlay.hideAll()
        }
        if (fg == null || fg == packageName) return
        if (siteHold != null && fg in Catalog.browsers) return

        val apps = graph.repo.apps.value
        val rule = apps.find { it.packageName == fg }
        if (rule?.category != Category.DISTRACTING) {
            overlay.hideTimer()
            if (siteHold == null) overlay.hidePause()
            return
        }

        val settings = graph.repo.settings.value
        val now = System.currentTimeMillis()
        val burstUntil = GuardMemory.burstUntil(fg)
        if (burstUntil > now) {
            sawLiveBurst = true
            shown = "BURST"
            overlay.hidePause()
            val burstLeft = (burstUntil - now).coerceAtLeast(0L)
            val window = settings.burstMinutes.coerceAtLeast(1) * 60_000f
            val spent = (1f - burstLeft / window).coerceIn(0f, 1f)
            overlay.showTimer(Formats.clock(burstLeft), spent)
            return
        }

        val session = graph.repo.session.value
        val focusOn = session != null && session.endedAt == null && session.phase == Phase.FOCUS
        val policy = GuardPolicy.reason(
            packageName = fg,
            apps = apps,
            totals = graph.repo.picture.value.usage.totals,
            capMinutes = settings.capMinutes,
            schedules = graph.repo.schedules.value,
            now = ZonedDateTime.now(),
            focusActive = focusOn,
        )
        val reason = when {
            policy != BlockReason.NONE -> policy
            sawLiveBurst -> BlockReason.BURST_END
            else -> BlockReason.NONE
        }

        if (!android.provider.Settings.canDrawOverlays(this)) {
            if (!slipMarked) {
                slipMarked = true
                withContext(Dispatchers.IO) { runCatching { graph.repo.recordSlip() } }
                Notifier.alert(
                    this,
                    Notifier.SLIP,
                    com.scrnlimit.screen.time.control.i18n.Phrases.text("slip_title"),
                    com.scrnlimit.screen.time.control.i18n.Phrases.text("slip_body"),
                )
            }
            return
        }

        if (reason == BlockReason.NONE) {
            overlay.hideTimer()
            if (shown != "RITUAL") {
                shown = "RITUAL"
                showRitual(fg)
            }
        } else {
            overlay.hideTimer()
            if (shown != "STOP") {
                shown = "STOP"
                if (reason == BlockReason.BURST_END) GuardMemory.arm(fg)
                showStop(fg, labelOf(fg), reason, Formats.reasonBody(reason))
            }
        }
    }

    private suspend fun showRitual(packageName: String) {
        val settings = graph.repo.settings.value
        val count = withContext(Dispatchers.IO) { graph.repo.incrementOpen(packageName) }
        val wait = GuardPolicy.pauseSeconds(settings.pauseSeconds, count - 1, settings.growPause)
        val model = PauseModel(
            packageName = packageName,
            label = labelOf(packageName),
            mode = PauseMode.RITUAL,
            waitSeconds = wait,
            burstMinutes = settings.burstMinutes,
            breath = settings.breath,
            reason = BlockReason.NONE,
            detail = "",
            passesLeft = 0,
        )
        overlay.showPause(
            model = model,
            onOpen = {
                GuardMemory.setBurst(packageName, System.currentTimeMillis() + settings.burstMinutes * 60_000L)
                sawLiveBurst = true
                shown = "BURST"
                overlay.hidePause()
            },
            onLater = {
                scope.launch(Dispatchers.IO) { runCatching { graph.repo.recordStopped() } }
                shown = "HOME"
                overlay.hidePause()
                goHome()
            },
            onClose = {},
            onEmergency = {},
        )
        if (!overlay.pauseVisible) {
            slipMarked = true
            withContext(Dispatchers.IO) { runCatching { graph.repo.recordSlip() } }
        }
    }

    private suspend fun showStop(packageName: String, label: String, reason: BlockReason, detail: String) {
        shown = "STOP"
        val settings = graph.repo.settings.value
        val passes = withContext(Dispatchers.IO) { graph.repo.passesLeft() }
        val model = PauseModel(
            packageName = packageName,
            label = label,
            mode = PauseMode.STOP,
            waitSeconds = 0,
            burstMinutes = settings.burstMinutes,
            breath = false,
            reason = reason,
            detail = detail,
            passesLeft = passes,
        )
        overlay.showPause(
            model = model,
            onOpen = {},
            onLater = {},
            onClose = {
                scope.launch(Dispatchers.IO) { runCatching { graph.repo.recordStopped() } }
                shown = "HOME"
                siteHold = null
                overlay.hidePause()
                goHome()
            },
            onEmergency = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { graph.repo.useEmergency() }
                    if (!ok) return@launch
                    val until = System.currentTimeMillis() + settings.burstMinutes * 60_000L
                    if (reason == BlockReason.SITE) {
                        siteAllowedHost = label
                        siteAllowedUntil = until
                        siteHold = null
                    } else {
                        GuardMemory.setBurst(packageName, until)
                        GuardMemory.clearArmed()
                        sawLiveBurst = true
                    }
                    shown = "BURST"
                    overlay.hidePause()
                }
            },
        )
    }

    private fun labelOf(packageName: String): String {
        graph.repo.apps.value.find { it.packageName == packageName }?.let { return it.label }
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(Catalog.labelFor(packageName))
    }

    private fun goHome() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    companion object {
        @Volatile var instance: MonitorService? = null
        @Volatile var running: Boolean = false

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
            }
        }
    }
}
