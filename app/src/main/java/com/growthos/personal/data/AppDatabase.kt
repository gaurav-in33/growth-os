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
        WakeConfirmation::class,
        StudyLogEntity::class,
        WorkoutLogEntity::class,
        ProjectEntity::class,
        DayCheckin::class,
        FinanceEntryEntity::class,
        NoteEntity::class,
        RoutineCheck::class,
        CourseEntity::class,
        TopicEntity::class,
        SkillEntity::class,
        ProjectItemEntity::class,
        ExerciseLogEntity::class,
        MeasurementEntity::class,
        WorkoutPlanEntity::class,
        GoalMilestoneEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusDao(): FocusDao
    abstract fun waterDao(): WaterDao
    abstract fun routineDao(): RoutineDao
    abstract fun goalDao(): GoalDao
    abstract fun goalMilestoneDao(): GoalMilestoneDao
    abstract fun wakeDao(): WakeDao
    abstract fun studyDao(): StudyDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun projectDao(): ProjectDao
    abstract fun checkinDao(): CheckinDao
    abstract fun financeDao(): FinanceDao
    abstract fun noteDao(): NoteDao
    abstract fun learnDao(): LearnDao
    abstract fun projectItemDao(): ProjectItemDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun workoutPlanDao(): WorkoutPlanDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `study_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `kind` TEXT NOT NULL, `minutes` INTEGER NOT NULL, `topic` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `type` TEXT NOT NULL, `minutes` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `progress` INTEGER NOT NULL, `createdEpochDay` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `day_checkins` (`epochDay` INTEGER NOT NULL, `mood` INTEGER NOT NULL, `energy` INTEGER NOT NULL, `sleepMinutes` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `finance_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `kind` TEXT NOT NULL, `amountPaise` INTEGER NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `dueEpochDay` INTEGER")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `dueMinute` INTEGER")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `estimateMin` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `category` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `energy` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `repeat` TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `remind` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `completedEpochDay` INTEGER")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `routine_checks` (`routineId` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, PRIMARY KEY(`routineId`, `epochDay`))")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `courses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `deadlineEpochDay` INTEGER, `isArchived` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `topics` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `courseId` INTEGER NOT NULL, `name` TEXT NOT NULL, `status` INTEGER NOT NULL, `weak` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `skills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `status` INTEGER NOT NULL, `position` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `project_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `parentId` INTEGER NOT NULL DEFAULT 0, `title` TEXT NOT NULL, `isDone` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `exercise` TEXT NOT NULL, `sets` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `weightKg` REAL NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `kind` TEXT NOT NULL, `value` REAL NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_plan` (`dayOfWeek` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`dayOfWeek`))")
                db.execSQL("ALTER TABLE `day_checkins` ADD COLUMN `bedtimeMinute` INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE `day_checkins` ADD COLUMN `wakeMinute` INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE `day_checkins` ADD COLUMN `sleepQuality` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `goal_milestones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `goalId` INTEGER NOT NULL, `title` TEXT NOT NULL, `isDone` INTEGER NOT NULL)")
                db.execSQL("ALTER TABLE `goals` ADD COLUMN `deadlineEpochDay` INTEGER")
                db.execSQL("ALTER TABLE `goals` ADD COLUMN `completedEpochDay` INTEGER")
                // goals that were already at 100% count as completed (today, in UTC days)
                db.execSQL("UPDATE `goals` SET `completedEpochDay` = CAST(strftime('%s', 'now') / 86400 AS INTEGER) WHERE `progress` >= 100")
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "growth_os.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
