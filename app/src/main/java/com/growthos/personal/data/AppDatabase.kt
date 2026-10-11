package com.growthos.personal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitCheckin::class,
        FocusSessionEntity::class,
        WaterDay::class,
        RoutineItemEntity::class,
        GoalEntity::class,
        WakeConfirmation::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusDao(): FocusDao
    abstract fun waterDao(): WaterDao
    abstract fun routineDao(): RoutineDao
    abstract fun goalDao(): GoalDao
    abstract fun wakeDao(): WakeDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdEpochDay` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `habit_checkins` (`habitId` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `epochDay`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `endedAtMillis` INTEGER NOT NULL, `minutes` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `water_days` (`epochDay` INTEGER NOT NULL, `glasses` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `routine_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `startMinute` INTEGER NOT NULL, `remind` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `horizon` TEXT NOT NULL, `progress` INTEGER NOT NULL, `createdEpochDay` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `wake_confirmations` (`epochDay` INTEGER NOT NULL, `confirmedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))")
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "growth_os.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
