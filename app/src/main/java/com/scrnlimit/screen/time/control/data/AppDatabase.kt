package com.scrnlimit.screen.time.control.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.scrnlimit.screen.time.control.domain.Category
import com.scrnlimit.screen.time.control.domain.Phase
import com.scrnlimit.screen.time.control.domain.ScheduleKind
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "apps")
data class TrackedApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val category: Category,
    val dailyLimitMinutes: Int?,
)

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: ScheduleKind,
    val daysMask: Int,
    val startMinute: Int,
    val endMinute: Int,
    val enabled: Boolean,
)

@Entity(tableName = "websites")
data class Website(
    @PrimaryKey val host: String,
    val enabled: Boolean,
)

@Entity(tableName = "sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val focusGoalMinutes: Int,
    val pomodoro: Boolean,
    val phase: Phase,
    val phaseStartedAt: Long,
    val phaseEndsAt: Long,
    val focusDoneMinutes: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val completed: Boolean,
)

@Entity(tableName = "days")
data class DayLog(
    @PrimaryKey val epochDay: Long,
    val distractingMs: Long,
    val productiveMs: Long,
    val neutralMs: Long,
    val opensStopped: Int,
    val emergencyUsed: Int,
    val slips: Int,
    val warned80: Boolean,
    val warnedCap: Boolean,
    val focusFinished: Boolean,
    val capMinutes: Int,
)

@Entity(tableName = "opens", primaryKeys = ["epochDay", "packageName"])
data class OpenTally(
    val epochDay: Long,
    val packageName: String,
    val count: Int,
)

class Converters {
    @TypeConverter fun categoryTo(value: Category): String = value.name
    @TypeConverter fun categoryFrom(value: String): Category = Category.valueOf(value)
    @TypeConverter fun kindTo(value: ScheduleKind): String = value.name
    @TypeConverter fun kindFrom(value: String): ScheduleKind = ScheduleKind.valueOf(value)
    @TypeConverter fun phaseTo(value: Phase): String = value.name
    @TypeConverter fun phaseFrom(value: String): Phase = Phase.valueOf(value)
}

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY label COLLATE NOCASE")
    fun observe(): Flow<List<TrackedApp>>

    @Query("SELECT * FROM apps")
    suspend fun all(): List<TrackedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: TrackedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<TrackedApp>)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY id")
    fun observe(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules")
    suspend fun all(): List<Schedule>

    @Insert
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface WebsiteDao {
    @Query("SELECT * FROM websites ORDER BY host")
    fun observe(): Flow<List<Website>>

    @Query("SELECT * FROM websites")
    suspend fun all(): List<Website>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(website: Website)

    @Query("DELETE FROM websites WHERE host = :host")
    suspend fun delete(host: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<FocusSession?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun active(): FocusSession?

    @Query("SELECT COUNT(*) FROM sessions")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(session: FocusSession): Long

    @Update
    suspend fun update(session: FocusSession)
}

@Dao
interface DayDao {
    @Query("SELECT * FROM days")
    suspend fun all(): List<DayLog>

    @Query("SELECT * FROM days WHERE epochDay = :day")
    suspend fun get(day: Long): DayLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DayLog)

    @Query("SELECT count FROM opens WHERE epochDay = :day AND packageName = :packageName")
    suspend fun opens(day: Long, packageName: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOpen(row: OpenTally)
}

@Database(
    entities = [
        TrackedApp::class,
        Schedule::class,
        Website::class,
        FocusSession::class,
        DayLog::class,
        OpenTally::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apps(): AppDao
    abstract fun schedules(): ScheduleDao
    abstract fun websites(): WebsiteDao
    abstract fun sessions(): SessionDao
    abstract fun days(): DayDao
}
