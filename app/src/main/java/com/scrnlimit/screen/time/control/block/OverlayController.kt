package com.scrnlimit.screen.time.control.block

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.scrnlimit.screen.time.control.R
import com.scrnlimit.screen.time.control.ui.theme.ScrnLimitTheme

class OverlayController(private val context: Context) {
    private val window = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density

    private var pauseView: View? = null
    private var pauseOwner: ServiceLifecycleOwner? = null
    private var timerView: View? = null
    private var timerOwner: ServiceLifecycleOwner? = null
    private var coverView: View? = null
    private var coverOwner: ServiceLifecycleOwner? = null

    private val timerLabel = mutableStateOf("")
    private val timerRatio = mutableFloatStateOf(0f)
    private val coverTitle = mutableStateOf("")
    private val coverBody = mutableStateOf("")
    private val coverAction = mutableStateOf("Go back")
    private var coverClick: () -> Unit = {}

    val pauseVisible: Boolean get() = pauseView != null

    fun showPause(
        model: PauseModel,
        onOpen: () -> Unit,
        onLater: () -> Unit,
        onClose: () -> Unit,
        onEmergency: () -> Unit,
    ) {
        if (!canDraw()) return
        hidePause()
        val owner = ServiceLifecycleOwner()
        val view = composeView(owner) {
            ScrnLimitTheme {
                PauseOverlay(model, onOpen, onLater, onClose, onEmergency)
            }
        }
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.setOnKeyListener { _, key, event ->
            key == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
        }
        if (add(view, fullParams())) {
            view.post { view.requestFocus() }
            pauseView = view
            pauseOwner = owner
        } else {
            owner.destroy()
        }
    }

    fun hidePause() {
        remove(pauseView, pauseOwner)
        pauseView = null
        pauseOwner = null
    }

    fun showTimer(label: String, ratio: Float) {
        if (!canDraw()) return
        timerLabel.value = label
        timerRatio.floatValue = ratio
        if (timerView != null) return
        val owner = ServiceLifecycleOwner()
        val view = composeView(owner) {
            ScrnLimitTheme { TimerPill(timerLabel.value, timerRatio.floatValue) }
        }
        val width = (132 * density).toInt()
        val height = (48 * density).toInt()
        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * density).toInt()
            y = (64 * density).toInt()
        }
        if (add(view, params)) {
            timerView = view
            timerOwner = owner
        } else {
            owner.destroy()
        }
    }

    fun hideTimer() {
        remove(timerView, timerOwner)
        timerView = null
        timerOwner = null
    }

    fun showCover(title: String, body: String, action: String, onAction: () -> Unit) {
        if (!canDraw()) return
        coverTitle.value = title
        coverBody.value = body
        coverAction.value = action
        coverClick = onAction
        if (coverView != null) return
        val owner = ServiceLifecycleOwner()
        val view = composeView(owner) {
            ScrnLimitTheme {
                CoverCard(coverTitle.value, coverBody.value, coverAction.value) { coverClick() }
            }
        }
        if (add(view, fullParams())) {
            coverView = view
            coverOwner = owner
        } else {
            owner.destroy()
        }
    }

    fun hideCover() {
        remove(coverView, coverOwner)
        coverView = null
        coverOwner = null
    }

    fun hideAll() {
        hidePause()
        hideTimer()
        hideCover()
    }

    private fun canDraw() = android.provider.Settings.canDrawOverlays(context)

    private fun composeView(owner: ServiceLifecycleOwner, content: @androidx.compose.runtime.Composable () -> Unit): ComposeView {
        return ComposeView(android.view.ContextThemeWrapper(context, R.style.Theme_ScrnLimit)).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            setContent(content)
        }
    }

    private fun fullParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= 28) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun add(view: View, params: WindowManager.LayoutParams): Boolean = try {
        window.addView(view, params)
        true
    } catch (_: Exception) {
        false
    }

    private fun remove(view: View?, owner: ServiceLifecycleOwner?) {
        if (view != null) runCatching { window.removeView(view) }
        owner?.destroy()
    }
}

class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val saved = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    init {
        saved.performAttach()
        saved.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        if (registry.currentState != Lifecycle.State.DESTROYED) {
            registry.currentState = Lifecycle.State.DESTROYED
        }
        store.clear()
    }
}
