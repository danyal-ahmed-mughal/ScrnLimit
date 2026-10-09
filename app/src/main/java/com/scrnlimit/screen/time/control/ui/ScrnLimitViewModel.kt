package com.scrnlimit.screen.time.control.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scrnlimit.screen.time.control.block.AdminReceiver
import com.scrnlimit.screen.time.control.block.MonitorService
import com.scrnlimit.screen.time.control.data.FocusSession
import com.scrnlimit.screen.time.control.data.Schedule
import com.scrnlimit.screen.time.control.data.ScreenRepository
import com.scrnlimit.screen.time.control.data.TrackedApp
import android.app.Activity
import com.scrnlimit.screen.time.control.domain.Catalog
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.domain.Category
import com.scrnlimit.screen.time.control.domain.InstalledApp
import com.scrnlimit.screen.time.control.domain.Pin
import com.scrnlimit.screen.time.control.domain.ScheduleKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GateRequest(val title: String, val deferred: CompletableDeferred<Boolean>)

class ScrnLimitViewModel(
    private val app: Context,
    private val repo: ScreenRepository,
) : ViewModel() {
    val settings = repo.settings
    val apps = repo.apps
    val schedules = repo.schedules
    val websites = repo.websites
    val session = repo.session
    val sessionCount = repo.sessionCount
    val picture = repo.picture
    val permissions = repo.permissions

    var installed by mutableStateOf<List<InstalledApp>>(emptyList())
        private set
    var selected by mutableStateOf<Set<String>>(emptySet())
        private set
    var includeGames by mutableStateOf(true)
        private set
    var capDraft by mutableIntStateOf(60)
        private set

    private val _gate = MutableStateFlow<GateRequest?>(null)
    val gate: StateFlow<GateRequest?> = _gate
    private val _creatingPin = MutableStateFlow(false)
    val creatingPin: StateFlow<Boolean> = _creatingPin
    private var pinEnablesStrict = false
    private var seeded = false

    init {
        viewModelScope.launch { repo.refresh(false) }
        viewModelScope.launch {
            installed = withContext(Dispatchers.IO) { repo.launchableApps() }
            if (!seeded) {
                seeded = true
                val tiktok = Catalog.tikTok.filter { pkg -> installed.any { it.packageName == pkg } }
                    .ifEmpty { listOf(Catalog.tikTok.first()) }
                val known = Catalog.apps.filter { it.defaultOn && it.packageName !in Catalog.tikTok }.map { it.packageName }
                val games = if (includeGames) installed.filter { it.game }.map { it.packageName } else emptyList()
                selected = (known + tiktok + games).toSet()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { repo.refresh(MonitorService.running) }
    }

    fun toggle(packageName: String) {
        selected = if (packageName in selected) selected - packageName else selected + packageName
    }

    fun setGames(on: Boolean) {
        includeGames = on
        val games = installed.filter { it.game }.map { it.packageName }.toSet()
        selected = if (on) selected + games else selected - games
    }

    fun chooseCap(minutes: Int) {
        capDraft = minutes.coerceIn(15, 180)
    }

    fun dismissHow() {
        viewModelScope.launch { repo.updateSettings { it.copy(seenHow = true) } }
    }

    fun finishSetup() {
        viewModelScope.launch {
            val known = installed.associateBy { it.packageName }
            val rows = selected.map { pkg -> known[pkg] ?: InstalledApp(pkg, Catalog.labelFor(pkg), false) }
            repo.finishOnboarding(rows, capDraft)
            MonitorService.start(app)
        }
    }

    fun setTheme(mode: String, activity: Activity) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(theme = mode) }
            activity.recreate()
        }
    }

    fun setLanguage(code: String, activity: Activity) {
        viewModelScope.launch {
            repo.updateSettings { it.copy(language = code) }
            activity.recreate()
        }
    }

    fun setCap(minutes: Int) = edit(minutes > settings.value.capMinutes, Phrases.text("raise_cap")) {
        repo.updateSettings { it.copy(capMinutes = minutes.coerceIn(15, 180)) }
    }

    fun setPause(seconds: Int) = edit(seconds < settings.value.pauseSeconds, Phrases.text("shorten_pause")) {
        repo.updateSettings { it.copy(pauseSeconds = seconds.coerceIn(5, 15)) }
    }

    fun setBurst(minutes: Int) = edit(minutes > settings.value.burstMinutes, Phrases.text("lengthen_burst")) {
        repo.updateSettings { it.copy(burstMinutes = minutes.coerceIn(1, 15)) }
    }

    fun setGrow(on: Boolean) = edit(!on && settings.value.growPause, Phrases.text("stop_grow")) {
        repo.updateSettings { it.copy(growPause = on) }
    }

    fun setBreath(on: Boolean) = viewModelScope.launch { repo.updateSettings { it.copy(breath = on) } }

    fun setPasses(count: Int) = edit(count > settings.value.passesPerDay, Phrases.text("add_passes")) {
        repo.updateSettings { it.copy(passesPerDay = count.coerceIn(0, 10)) }
    }

    fun setReels(on: Boolean) = edit(!on && settings.value.blockReels, Phrases.text("reels_off")) {
        repo.updateSettings { it.copy(blockReels = on) }
    }

    fun setStrict(on: Boolean) {
        viewModelScope.launch {
            if (on) {
                if (!settings.value.hasPin) {
                    pinEnablesStrict = true
                    _creatingPin.value = true
                } else {
                    repo.updateSettings { it.copy(strict = true) }
                }
            } else {
                loosen(Phrases.text("strict_off")) { repo.updateSettings { it.copy(strict = false) } }
            }
        }
    }

    fun setBiometric(on: Boolean) {
        viewModelScope.launch {
            if (!on) {
                repo.updateSettings { it.copy(biometric = false) }
            } else if (confirm(Phrases.text("confirm_pin"))) {
                repo.updateSettings { it.copy(biometric = true) }
            }
        }
    }

    fun beginPinChange() {
        viewModelScope.launch {
            if (settings.value.hasPin && !confirm(Phrases.text("enter_pin"))) return@launch
            pinEnablesStrict = false
            _creatingPin.value = true
        }
    }

    fun cancelPinCreate() {
        pinEnablesStrict = false
        _creatingPin.value = false
    }

    fun saveCreatedPin(pin: String) {
        viewModelScope.launch {
            repo.setPin(pin)
            if (pinEnablesStrict) repo.updateSettings { it.copy(strict = true) }
            pinEnablesStrict = false
            _creatingPin.value = false
        }
    }

    fun verify(pin: String): Boolean {
        val current = settings.value
        if (!current.hasPin) return false
        return Pin.hash(pin, current.pinSalt) == current.pinHash
    }

    fun submitGate(ok: Boolean) {
        _gate.value?.deferred?.complete(ok)
    }

    fun addDistracting(app: InstalledApp) {
        viewModelScope.launch { repo.upsertApp(TrackedApp(app.packageName, app.label, Category.DISTRACTING, null)) }
    }

    fun setCategory(app: TrackedApp, category: Category) = edit(
        app.category == Category.DISTRACTING && category != Category.DISTRACTING,
        Phrases.text("off_list"),
    ) { repo.upsertApp(app.copy(category = category)) }

    fun setAppLimit(app: TrackedApp, minutes: Int?) {
        val looser = when {
            minutes == null && app.dailyLimitMinutes != null -> true
            minutes != null && app.dailyLimitMinutes != null && minutes > app.dailyLimitMinutes -> true
            else -> false
        }
        edit(looser, Phrases.text("loosen_limit")) { repo.upsertApp(app.copy(dailyLimitMinutes = minutes)) }
    }

    fun removeApp(app: TrackedApp) = edit(app.category == Category.DISTRACTING, Phrases.text("remove_app_gate")) {
        repo.deleteApp(app.packageName)
    }

    fun addSleep() = viewModelScope.launch {
        repo.addSchedule(Schedule(kind = ScheduleKind.SLEEP, daysMask = 127, startMinute = 22 * 60 + 30, endMinute = 7 * 60, enabled = true))
    }

    fun addWork() = viewModelScope.launch {
        repo.addSchedule(Schedule(kind = ScheduleKind.WORK, daysMask = 31, startMinute = 9 * 60, endMinute = 17 * 60, enabled = true))
    }

    fun saveSchedule(schedule: Schedule) = edit(true, Phrases.text("edit_schedule")) { repo.updateSchedule(schedule) }

    fun setScheduleEnabled(schedule: Schedule, enabled: Boolean) {
        if (enabled) viewModelScope.launch { repo.updateSchedule(schedule.copy(enabled = true)) }
        else edit(true, Phrases.text("turn_block_off")) { repo.updateSchedule(schedule.copy(enabled = false)) }
    }

    fun deleteSchedule(schedule: Schedule) = edit(true, Phrases.text("turn_block_off")) { repo.deleteSchedule(schedule.id) }

    fun setSite(host: String, enabled: Boolean) {
        if (enabled) viewModelScope.launch { repo.setWebsite(host, true) }
        else edit(true, Phrases.text("site_off")) { repo.setWebsite(host, false) }
    }

    fun removeSite(host: String) = edit(true, Phrases.text("remove_site")) { repo.deleteWebsite(host) }

    fun startFocus(minutes: Int, pomodoro: Boolean) {
        viewModelScope.launch { repo.startSession(minutes, pomodoro) }
    }

    fun endFocus() = edit(true, "End the session early") { repo.endSession() }

    fun removeProtection() {
        viewModelScope.launch {
            loosen("Remove uninstall protection") {
                val manager = app.getSystemService(DevicePolicyManager::class.java)
                val name = ComponentName(app, AdminReceiver::class.java)
                runCatching { manager?.removeActiveAdmin(name) }
                repo.updateSettings { it.copy(uninstallProtection = false) }
            }
        }
    }

    private fun edit(loose: Boolean, title: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (loose) loosen(title, block) else block()
        }
    }

    private suspend fun loosen(title: String, block: suspend () -> Unit = {}) {
        val current = settings.value
        if (current.strict && current.hasPin && !confirm(title)) return
        block()
    }

    private suspend fun confirm(title: String): Boolean {
        val request = GateRequest(title, CompletableDeferred())
        _gate.value = request
        return try {
            request.deferred.await()
        } finally {
            if (_gate.value == request) _gate.value = null
        }
    }

    companion object {
        fun factory(repo: ScreenRepository, app: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = ScrnLimitViewModel(app, repo) as T
            }
    }
}
