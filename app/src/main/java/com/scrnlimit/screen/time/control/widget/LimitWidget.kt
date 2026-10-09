package com.scrnlimit.screen.time.control.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.scrnlimit.screen.time.control.MainActivity
import com.scrnlimit.screen.time.control.R

class LimitWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.refresh(context, appWidgetManager, appWidgetIds)
    }
}

object WidgetUpdater {
    private const val FILE = "widget"

    fun publish(context: Context, time: String, session: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("time", time)
            .putString("session", session)
            .apply()
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, LimitWidget::class.java))
        refresh(context, manager, ids)
    }

    fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val time = prefs.getString("time", "0m")
        val session = prefs.getString("session", null) ?: context.getString(R.string.widget_session_empty)
        val pending = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_limit)
            views.setTextViewText(R.id.widget_time, time)
            views.setTextViewText(R.id.widget_session, session)
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}
