package com.scrnlimit.screen.time.control.data

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.scrnlimit.screen.time.control.block.AdminReceiver
import com.scrnlimit.screen.time.control.block.FeedAccessibilityService
import com.scrnlimit.screen.time.control.domain.PermissionSnapshot

object Permissions {
    fun snapshot(context: Context): PermissionSnapshot = PermissionSnapshot(
        usage = hasUsage(context),
        overlay = Settings.canDrawOverlays(context),
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        accessibility = hasAccessibility(context),
        batteryUnrestricted = isIgnoringBattery(context),
        admin = isAdmin(context),
    )

    fun hasUsage(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasAccessibility(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val flat = android.content.ComponentName(context, FeedAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(flat, ignoreCase = true) }
    }

    fun isIgnoringBattery(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isAdmin(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        val name = android.content.ComponentName(context, AdminReceiver::class.java)
        return dpm.isAdminActive(name)
    }
}
