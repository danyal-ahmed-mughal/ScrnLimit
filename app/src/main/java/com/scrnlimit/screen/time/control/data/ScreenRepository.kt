package com.scrnlimit.screen.time.control.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.scrnlimit.screen.time.control.domain.AppSettings
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.domain.Catalog
import com.scrnlimit.screen.time.control.domain.Category
import com.scrnlimit.screen.time.control.domain.Formats
import com.scrnlimit.screen.time.control.domain.GuardPolicy
import com.scrnlimit.screen.time.control.domain.InstalledApp
import com.scrnlimit.screen.time.control.domain.PermissionSnapshot
import com.scrnlimit.screen.time.control.domain.Phase
import com.scrnlimit.screen.time.control.domain.Picture
import com.scrnlimit.screen.time.control.domain.Pin
import com.scrnlimit.screen.time.control.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.math.min

class ScreenRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settingsStore: SettingsStore,
    private val usage: UsageReader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()
    private var widgetMinute = -1
    private var widgetSession = ""

    val settings: StateFlow<AppSettings> =
        settingsStore.flow.stateIn(scope, SharingStarted.Eagerly, AppSettings())
    val apps: StateFlow<List<TrackedApp>> =
        db.apps().observe().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val schedules: StateFlow<List<Schedule>> =
        db.schedules().observe().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val websites: StateFlow<List<Website>> =
        db.websites().observe().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val session: StateFlow<FocusSession?> =
        db.sessions().observeActive().stateIn(scope, SharingStarted.Eagerly, null)
    val sessionCount: StateFlow<Int> =
        db.sessions().observeCount().stateIn(scope, SharingStarted.Eagerly, 0)
    val picture = MutableStateFlow(Picture())
    val permissions = MutableStateFlow(Permissions.snapshot(context))

    init {
        scope.launch {
            settingsStore.adoptEnglishDefault()
            dedupeTikTok()
        }
    }

    suspend fun refresh(updateOngoing: Boolean) = gate.withLock {
        advanceSession()
        val current = settingsStore.flow.first()
        val appList = db.apps().all()
        val distracting = GuardPolicy.distractingPackages(appList)
        val productive = appList.filter { it.category == Category.PRODUCTIVE }.map { it.packageName }.toSet()
        val neutral = appList.filter { it.category == Category.NEUTRAL }.map { it.packageName }.toSet()
        val snapshot = usage.readToday()
        val distractingMs = GuardPolicy.totalFor(snapshot.totals, distracting)
        val history = usage.history(30, distracting, distractingMs)
        val today = LocalDate.now().toEpochDay()
        val existing = db.days().get(today) ?: blank(today, current.capMinutes)
        var log = existing.copy(
            distractingMs = distractingMs,
            productiveMs = GuardPolicy.totalFor(snapshot.totals, productive),
            neutralMs = GuardPolicy.totalFor(snapshot.totals, neutral),
            capMinutes = current.capMinutes,
        )
        db.days().upsert(log)
        picture.value = Picture(snapshot, history, db.days().all())
        permissions.value = Permissions.snapshot(context)

        if (current.onboarded && snapshot.permitted && current.capMinutes > 0) {
            val ratio = GuardPolicy.ratio(distractingMs, current.capMinutes)
            if (ratio >= 1f && !log.warnedCap) {
                Notifier.alert(
                    context,
                    Notifier.CAP,
                    Phrases.text("cap_up"),
                    Phrases.text("cap_up_body", Formats.duration(distractingMs)),
                )
                log = log.copy(warnedCap = true, warned80 = true)
            } else if (ratio >= 0.8f && !log.warned80) {
                Notifier.alert(
                    context,
                    Notifier.EIGHTY,
                    Phrases.text("eighty"),
                    Phrases.text("eighty_body", Formats.duration(distractingMs), Formats.duration(current.capMinutes * 60_000L)),
                )
                log = log.copy(warned80 = true)
            }
            if (log.warned80 != existing.warned80 || log.warnedCap != existing.warnedCap || existing.warned80 != log.warned80) {
                db.days().upsert(log)
                picture.value = picture.value.copy(logs = db.days().all())
            }
        }

        val active = db.sessions().active()
        val sessionLabel = sessionLabel(active)
        val minute = (distractingMs / 60_000L).toInt()
        if (minute != widgetMinute || sessionLabel != widgetSession) {
            widgetMinute = minute
            widgetSession = sessionLabel
            WidgetUpdater.publish(context, Formats.duration(distractingMs), sessionLabel)
        }
        if (updateOngoing && current.onboarded) {
            val ratio = GuardPolicy.ratio(distractingMs, current.capMinutes)
            Notifier.updateOngoing(
                context,
                "${Formats.duration(distractingMs)} · ${Phrases.status(ratio, snapshot.permitted)}",
            )
        }
    }

    suspend fun finishOnboarding(selected: List<InstalledApp>, capMinutes: Int) {
        db.apps().upsertAll(
            selected.distinctBy { it.packageName }.map {
                TrackedApp(it.packageName, it.label, Category.DISTRACTING, null)
            },
        )
        settingsStore.update { it.copy(onboarded = true, capMinutes = capMinutes.coerceIn(15, 180)) }
        refresh(false)
    }

    suspend fun updateSettings(block: (AppSettings) -> AppSettings) {
        settingsStore.update(block)
        refresh(false)
    }

    suspend fun upsertApp(app: TrackedApp) {
        db.apps().upsert(app)
        refresh(false)
    }

    suspend fun deleteApp(packageName: String) {
        db.apps().delete(packageName)
        refresh(false)
    }

    suspend fun addSchedule(schedule: Schedule) {
        db.schedules().insert(schedule)
        refresh(false)
    }

    suspend fun updateSchedule(schedule: Schedule) {
        db.schedules().update(schedule)
        refresh(false)
    }

    suspend fun deleteSchedule(id: Long) {
        db.schedules().delete(id)
        refresh(false)
    }

    suspend fun setWebsite(host: String, enabled: Boolean) {
        db.websites().upsert(Website(host, enabled))
        refresh(false)
    }

    suspend fun deleteWebsite(host: String) {
        db.websites().delete(host)
        refresh(false)
    }

    suspend fun startSession(minutes: Int, pomodoro: Boolean) {
        val now = System.currentTimeMillis()
        db.sessions().active()?.let { db.sessions().update(it.copy(endedAt = now, completed = false)) }
        val slice = if (pomodoro) min(25, minutes) else minutes
        db.sessions().insert(
            FocusSession(
                focusGoalMinutes = minutes.coerceIn(5, 180),
                pomodoro = pomodoro,
                phase = Phase.FOCUS,
                phaseStartedAt = now,
                phaseEndsAt = now + slice * 60_000L,
                focusDoneMinutes = 0,
                startedAt = now,
                endedAt = null,
                completed = false,
            ),
        )
        refresh(false)
    }

    suspend fun endSession() {
        val now = System.currentTimeMillis()
        db.sessions().active()?.let { db.sessions().update(it.copy(endedAt = now, completed = false)) }
        refresh(false)
    }

    suspend fun incrementOpen(packageName: String): Int {
        val day = LocalDate.now().toEpochDay()
        val next = (db.days().opens(day, packageName) ?: 0) + 1
        db.days().upsertOpen(OpenTally(day, packageName, next))
        return next
    }

    suspend fun recordStopped() = bump { it.copy(opensStopped = it.opensStopped + 1) }

    suspend fun recordSlip() = bump { it.copy(slips = it.slips + 1) }

    suspend fun useEmergency(): Boolean {
        val current = settingsStore.flow.first()
        val day = LocalDate.now().toEpochDay()
        val log = db.days().get(day) ?: blank(day, current.capMinutes)
        if (log.emergencyUsed >= current.passesPerDay) return false
        db.days().upsert(log.copy(emergencyUsed = log.emergencyUsed + 1))
        refresh(false)
        return true
    }

    suspend fun passesLeft(): Int {
        val current = settingsStore.flow.first()
        val used = db.days().get(LocalDate.now().toEpochDay())?.emergencyUsed ?: 0
        return (current.passesPerDay - used).coerceAtLeast(0)
    }

    suspend fun setPin(pin: String) {
        val salt = Pin.salt()
        settingsStore.update { it.copy(pinSalt = salt, pinHash = Pin.hash(pin, salt)) }
    }

    fun launchableApps(): List<InstalledApp> {
        val manager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            manager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            manager.queryIntentActivities(intent, 0)
        }
        return resolved.mapNotNull { info ->
            val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
            if (appInfo.packageName == context.packageName) return@mapNotNull null
            InstalledApp(
                packageName = appInfo.packageName,
                label = info.loadLabel(manager)?.toString().orEmpty().ifBlank { Catalog.labelFor(appInfo.packageName) },
                game = appInfo.category == ApplicationInfo.CATEGORY_GAME,
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    private suspend fun bump(block: (DayLog) -> DayLog) {
        val current = settingsStore.flow.first()
        val day = LocalDate.now().toEpochDay()
        val log = db.days().get(day) ?: blank(day, current.capMinutes)
        db.days().upsert(block(log))
        refresh(false)
    }

    private suspend fun advanceSession() {
        val active = db.sessions().active() ?: return
        val stepped = GuardPolicy.advanceSession(active, System.currentTimeMillis())
        if (stepped == active) return
        db.sessions().update(stepped)
        if (stepped.completed) {
            val current = settings.value
            val day = LocalDate.now().toEpochDay()
            val log = db.days().get(day) ?: blank(day, current.capMinutes)
            db.days().upsert(log.copy(focusFinished = true))
            Notifier.alert(context, Notifier.FOCUS, Phrases.text("focus_done"), Phrases.text("focus_done_body"))
        }
    }

    private fun sessionLabel(session: FocusSession?): String = when {
        session == null -> Phrases.text("no_focus")
        session.phase == Phase.BREAK -> Phrases.text("focus_break")
        else -> Phrases.text("focus_on")
    }

    private suspend fun dedupeTikTok() {
        val rows = db.apps().all().filter { it.packageName in Catalog.tikTok }
        if (rows.size < 2) return
        val installed = rows.filter { installed(it.packageName) }
        val keep = if (installed.isNotEmpty()) installed.map { it.packageName }.toSet() else setOf(rows.first().packageName)
        rows.filter { it.packageName !in keep }.forEach { db.apps().delete(it.packageName) }
    }

    private fun installed(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)

    private fun blank(day: Long, cap: Int) = DayLog(
        epochDay = day,
        distractingMs = 0,
        productiveMs = 0,
        neutralMs = 0,
        opensStopped = 0,
        emergencyUsed = 0,
        slips = 0,
        warned80 = false,
        warnedCap = false,
        focusFinished = false,
        capMinutes = cap,
    )
}
