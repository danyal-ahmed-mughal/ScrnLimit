package com.scrnlimit.screen.time.control.block

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scrnlimit.screen.time.control.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsStore(context).flow.first()
                if (settings.onboarded) {
                    withContext(Dispatchers.Main) { MonitorService.start(context) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
