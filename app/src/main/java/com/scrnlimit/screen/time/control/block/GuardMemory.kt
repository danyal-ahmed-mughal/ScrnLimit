package com.scrnlimit.screen.time.control.block

import android.content.Context

object GuardMemory {
    private const val FILE = "guard"
    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun burstUntil(packageName: String): Long = prefs.getLong(burstKey(packageName), 0L)

    fun setBurst(packageName: String, until: Long) {
        prefs.edit().putLong(burstKey(packageName), until).apply()
    }

    fun arm(packageName: String) {
        prefs.edit().putString("armed", packageName).apply()
    }

    fun armed(): String? = prefs.getString("armed", null)

    fun clearArmed() {
        prefs.edit().remove("armed").apply()
    }

    private fun burstKey(packageName: String) = "burst_$packageName"
}
