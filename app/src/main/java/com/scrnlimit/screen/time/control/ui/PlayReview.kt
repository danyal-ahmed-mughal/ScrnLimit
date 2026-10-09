package com.scrnlimit.screen.time.control.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

private const val StoreId = "com.scrnlimit.screen.time.control"

object ReviewPrefs {
    private const val File = "review"
    private const val Opens = "opens"
    private const val Asked = "asked"

    fun shouldAsk(context: Context): Boolean {
        val prefs = context.getSharedPreferences(File, Context.MODE_PRIVATE)
        if (prefs.getBoolean(Asked, false)) return false
        val opens = prefs.getInt(Opens, 0) + 1
        prefs.edit().putInt(Opens, opens).apply()
        return opens >= 3
    }

    fun markAsked(context: Context) {
        context.getSharedPreferences(File, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(Asked, true)
            .apply()
    }
}

object PlayReview {
    fun request(activity: Activity, onFinished: () -> Unit = {}) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                openStore(activity)
                onFinished()
                return@addOnCompleteListener
            }
            manager.launchReviewFlow(activity, task.result).addOnCompleteListener {
                onFinished()
            }
        }
    }

    fun openStore(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$StoreId"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$StoreId"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(market) }.onFailure {
            runCatching { context.startActivity(web) }
        }
    }
}
