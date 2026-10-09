package com.scrnlimit.screen.time.control.block

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.scrnlimit.screen.time.control.R

class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.admin_disable)
    }
}
