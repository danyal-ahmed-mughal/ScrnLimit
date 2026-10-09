package com.scrnlimit.screen.time.control

import android.app.Application
import android.content.Context
import com.scrnlimit.screen.time.control.i18n.Phrases
import com.scrnlimit.screen.time.control.i18n.UiPrefs
import androidx.room.Room
import com.scrnlimit.screen.time.control.data.AppDatabase
import com.scrnlimit.screen.time.control.data.ScreenRepository
import com.scrnlimit.screen.time.control.data.SettingsStore
import com.scrnlimit.screen.time.control.data.UsageReader

class ScrnLimitApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun attachBaseContext(base: Context) {
        UiPrefs.prepare(base)
        super.attachBaseContext(UiPrefs.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        Phrases.apply(UiPrefs.language(this))
        graph = AppGraph(this)
        instance = this
    }

    companion object {
        lateinit var instance: ScrnLimitApp
            private set
    }
}

class AppGraph(context: Application) {
    val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "screenlimit.db",
    ).fallbackToDestructiveMigration().build()
    val settings = SettingsStore(context)
    val usage = UsageReader(context)
    val repo = ScreenRepository(context, database, settings, usage)
}
