package com.scrnlimit.screen.time.control

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import com.scrnlimit.screen.time.control.i18n.UiPrefs
import com.scrnlimit.screen.time.control.ui.Root
import com.scrnlimit.screen.time.control.ui.ScrnLimitViewModel
import com.scrnlimit.screen.time.control.ui.theme.ScrnLimitTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(UiPrefs.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val dark = when (UiPrefs.theme(this)) {
            "light" -> false
            "dark" -> true
            else -> night
        }
        window.setBackgroundDrawable(ColorDrawable(if (dark) 0xFF08090C.toInt() else 0xFFF6F3EC.toInt()))
        val bars = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        val graph = (application as ScrnLimitApp).graph
        setContent {
            val model: ScrnLimitViewModel = viewModel(factory = ScrnLimitViewModel.factory(graph.repo, applicationContext))
            val settings by model.settings.collectAsStateWithLifecycle()
            val theme = if (settings.ready) settings.theme else UiPrefs.theme(this)
            val language = if (settings.ready) settings.language else UiPrefs.language(this)
            ScrnLimitTheme(theme, language) {
                Root(model)
            }
        }
    }
}
