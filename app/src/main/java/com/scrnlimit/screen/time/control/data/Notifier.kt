package com.scrnlimit.screen.time.control.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.scrnlimit.screen.time.control.MainActivity
import com.scrnlimit.screen.time.control.R

object Notifier {
    const val ONGOING = 41
    const val EIGHTY = 42
    const val CAP = 43
    const val SLIP = 44
    const val FOCUS = 45

    private const val MONITOR = "monitor"
    private const val LIMITS = "limits"
    private var lastOngoing: String? = null

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(MONITOR, context.getString(R.string.channel_monitor), NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(LIMITS, context.getString(R.string.channel_limits), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    fun ongoing(context: Context, text: String): Notification {
        ensure(context)
        return NotificationCompat.Builder(context, MONITOR)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("ScrnLimit")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open(context))
            .build()
    }

    fun updateOngoing(context: Context, text: String) {
        if (text == lastOngoing) return
        lastOngoing = text
        runCatching { NotificationManagerCompat.from(context).notify(ONGOING, ongoing(context, text)) }
    }

    fun alert(context: Context, id: Int, title: String, body: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensure(context)
        val notification = NotificationCompat.Builder(context, LIMITS)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun open(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
