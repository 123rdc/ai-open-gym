package com.example.gymformcoach.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- routines: Long autoinc id -> String UUID id, add syncStatus/serverId/updatedAt ---
        db.execSQL(
            """
            CREATE TABLE routines_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        val routineIdMap = HashMap<Long, String>()
        db.query("SELECT id, name, description, createdAt FROM routines").use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            val descIdx = cursor.getColumnIndexOrThrow("description")
            val createdIdx = cursor.getColumnIndexOrThrow("createdAt")
            while (cursor.moveToNext()) {
                val oldId = cursor.getLong(idIdx)
                val newId = UUID.randomUUID().toString()
                routineIdMap[oldId] = newId
                val createdAt = cursor.getLong(createdIdx)
                db.execSQL(
                    "INSERT INTO routines_new (id, name, description, syncStatus, serverId, createdAt, updatedAt) VALUES (?, ?, ?, ?, NULL, ?, ?)",
                    arrayOf(newId, cursor.getString(nameIdx), cursor.getString(descIdx), SyncStatus.SYNCED, createdAt, createdAt)
                )
            }
        }
        db.execSQL("DROP TABLE routines")
        db.execSQL("ALTER TABLE routines_new RENAME TO routines")

        // --- routine_exercises: Long id -> String UUID id, Long routineId -> String routineId ---
        db.execSQL(
            """
            CREATE TABLE routine_exercises_new (
                id TEXT NOT NULL PRIMARY KEY,
                routineId TEXT NOT NULL,
                exerciseId TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                targetWeightKg REAL NOT NULL,
                targetReps INTEGER NOT NULL,
                targetSets INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("DROP INDEX IF EXISTS index_routine_exercises_routineId")
        db.execSQL("CREATE INDEX index_routine_exercises_routineId ON routine_exercises_new(routineId)")
        val now = System.currentTimeMillis()
        db.query(
            "SELECT id, routineId, exerciseId, orderIndex, targetWeightKg, targetReps, targetSets FROM routine_exercises"
        ).use { cursor ->
            val routineIdIdx = cursor.getColumnIndexOrThrow("routineId")
            val exerciseIdIdx = cursor.getColumnIndexOrThrow("exerciseId")
            val orderIdx = cursor.getColumnIndexOrThrow("orderIndex")
            val weightIdx = cursor.getColumnIndexOrThrow("targetWeightKg")
            val repsIdx = cursor.getColumnIndexOrThrow("targetReps")
            val setsIdx = cursor.getColumnIndexOrThrow("targetSets")
            while (cursor.moveToNext()) {
                val newId = UUID.randomUUID().toString()
                val oldRoutineId = cursor.getLong(routineIdIdx)
                val newRoutineId = routineIdMap[oldRoutineId] ?: continue
                db.execSQL(
                    "INSERT INTO routine_exercises_new (id, routineId, exerciseId, orderIndex, targetWeightKg, targetReps, targetSets, syncStatus, serverId, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)",
                    arrayOf(
                        newId, newRoutineId, cursor.getString(exerciseIdIdx), cursor.getInt(orderIdx),
                        cursor.getFloat(weightIdx), cursor.getInt(repsIdx), cursor.getInt(setsIdx),
                        SyncStatus.SYNCED, now, now
                    )
                )
            }
        }
        db.execSQL("DROP TABLE routine_exercises")
        db.execSQL("ALTER TABLE routine_exercises_new RENAME TO routine_exercises")

        // --- exercise_sessions: Long id -> String UUID id, add sync fields + createdAt/updatedAt ---
        db.execSQL(
            """
            CREATE TABLE exercise_sessions_new (
                id TEXT NOT NULL PRIMARY KEY,
                exerciseId TEXT NOT NULL,
                date INTEGER NOT NULL,
                weightKg REAL NOT NULL,
                reps INTEGER NOT NULL,
                sets INTEGER NOT NULL,
                volumeScore REAL NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.query(
            "SELECT id, exerciseId, date, weightKg, reps, sets, volumeScore FROM exercise_sessions"
        ).use { cursor ->
            val exerciseIdIdx = cursor.getColumnIndexOrThrow("exerciseId")
            val dateIdx = cursor.getColumnIndexOrThrow("date")
            val weightIdx = cursor.getColumnIndexOrThrow("weightKg")
            val repsIdx = cursor.getColumnIndexOrThrow("reps")
            val setsIdx = cursor.getColumnIndexOrThrow("sets")
            val volumeIdx = cursor.getColumnIndexOrThrow("volumeScore")
            while (cursor.moveToNext()) {
                val newId = UUID.randomUUID().toString()
                val date = cursor.getLong(dateIdx)
                db.execSQL(
                    "INSERT INTO exercise_sessions_new (id, exerciseId, date, weightKg, reps, sets, volumeScore, syncStatus, serverId, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)",
                    arrayOf(
                        newId, cursor.getString(exerciseIdIdx), date, cursor.getFloat(weightIdx),
                        cursor.getInt(repsIdx), cursor.getInt(setsIdx), cursor.getFloat(volumeIdx),
                        SyncStatus.SYNCED, date, date
                    )
                )
            }
        }
        db.execSQL("DROP TABLE exercise_sessions")
        db.execSQL("ALTER TABLE exercise_sessions_new RENAME TO exercise_sessions")

        // --- user_profile: brand-new table, no prior data to migrate ---
        db.execSQL(
            """
            CREATE TABLE user_profile (
                id TEXT NOT NULL PRIMARY KEY,
                gender TEXT NOT NULL,
                age INTEGER NOT NULL,
                weightKg REAL NOT NULL,
                heightCm REAL NOT NULL,
                fitnessGoal TEXT NOT NULL,
                activityLevel TEXT NOT NULL,
                trainingExperience TEXT NOT NULL,
                equipment TEXT NOT NULL,
                trainingSplit TEXT NOT NULL,
                focusAreas TEXT NOT NULL,
                sessionLengthPreference TEXT NOT NULL,
                trainingLimitations TEXT NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE set_analyses (
                id TEXT NOT NULL PRIMARY KEY,
                exerciseSessionId TEXT NOT NULL,
                mainIssue TEXT NOT NULL,
                trendSummary TEXT NOT NULL,
                correctives TEXT NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(exerciseSessionId) REFERENCES exercise_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_set_analyses_exerciseSessionId ON set_analyses(exerciseSessionId)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // exercise_sessions.date -> performedAt (§0.1: performedAt is the
        // user-facing "when the workout happened" field, distinct from
        // createdAt which is "when the row was written").
        // Table rebuild rather than ALTER TABLE ... RENAME COLUMN: that syntax
        // needs SQLite 3.25+ (Android 10 / API 29+), but minSdk here is 24.
        db.execSQL(
            """
            CREATE TABLE exercise_sessions_new (
                id TEXT NOT NULL PRIMARY KEY,
                exerciseId TEXT NOT NULL,
                performedAt INTEGER NOT NULL,
                weightKg REAL NOT NULL,
                reps INTEGER NOT NULL,
                sets INTEGER NOT NULL,
                volumeScore REAL NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO exercise_sessions_new
                (id, exerciseId, performedAt, weightKg, reps, sets, volumeScore, syncStatus, serverId, createdAt, updatedAt)
            SELECT id, exerciseId, date, weightKg, reps, sets, volumeScore, syncStatus, serverId, createdAt, updatedAt
            FROM exercise_sessions
            """.trimIndent()
        )
        db.execSQL("DROP TABLE exercise_sessions")
        db.execSQL("ALTER TABLE exercise_sessions_new RENAME TO exercise_sessions")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // §1 exercise type system (§20.3): promote the previously-hardcoded
        // ExerciseCatalog (WorkoutListScreen.kt) into a real, classified table,
        // and add SetLog for per-set data that the four exercise shapes need
        // (TIMED/CARDIO can't be represented by ExerciseSession's single
        // aggregate weightKg/reps/sets columns).
        db.execSQL(
            """
            CREATE TABLE exercises (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                bodyPart TEXT NOT NULL,
                muscle TEXT NOT NULL,
                category TEXT NOT NULL,
                duration TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                imageUrl TEXT NOT NULL,
                about TEXT NOT NULL,
                exerciseType TEXT NOT NULL,
                loadType TEXT NOT NULL,
                isUnilateral INTEGER NOT NULL,
                isCustom INTEGER NOT NULL,
                muscleGroups TEXT NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_exercises_name ON exercises(name)")

        val now = System.currentTimeMillis()
        for (exercise in ExerciseSeedData.all) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO exercises
                    (id, name, bodyPart, muscle, category, duration, difficulty, imageUrl, about,
                     exerciseType, loadType, isUnilateral, isCustom, muscleGroups,
                     syncStatus, serverId, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)
                """.trimIndent(),
                arrayOf(
                    exercise.id, exercise.name, exercise.bodyPart, exercise.muscle, exercise.category,
                    exercise.duration, exercise.difficulty, exercise.imageUrl, exercise.about,
                    exercise.exerciseType.name, exercise.loadType.name,
                    if (exercise.isUnilateral) 1 else 0, if (exercise.isCustom) 1 else 0,
                    exercise.muscleGroups.joinToString("||"),
                    SyncStatus.SYNCED, now, now
                )
            )
        }

        db.execSQL(
            """
            CREATE TABLE set_logs (
                id TEXT NOT NULL PRIMARY KEY,
                exerciseSessionId TEXT NOT NULL,
                setIndex INTEGER NOT NULL,
                weightKg REAL,
                addedWeightKg REAL,
                reps INTEGER,
                durationSeconds INTEGER,
                distanceMeters REAL,
                effortValue INTEGER,
                effortScale TEXT,
                isPr INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(exerciseSessionId) REFERENCES exercise_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_set_logs_exerciseSessionId ON set_logs(exerciseSessionId)")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // §2 weekly plan + overrides, §3.3 active session persistence,
        // §4.1 superset grouping.
        db.execSQL(
            """
            CREATE TABLE weekly_plan_entries (
                id TEXT NOT NULL PRIMARY KEY,
                dayOfWeek INTEGER NOT NULL,
                routineId TEXT,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_weekly_plan_entries_dayOfWeek ON weekly_plan_entries(dayOfWeek)")
        db.execSQL("CREATE INDEX index_weekly_plan_entries_routineId ON weekly_plan_entries(routineId)")

        db.execSQL(
            """
            CREATE TABLE plan_overrides (
                id TEXT NOT NULL PRIMARY KEY,
                epochDay INTEGER NOT NULL,
                routineId TEXT,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_plan_overrides_epochDay ON plan_overrides(epochDay)")
        db.execSQL("CREATE INDEX index_plan_overrides_routineId ON plan_overrides(routineId)")

        db.execSQL(
            """
            CREATE TABLE active_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                routineId TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                currentExerciseIndex INTEGER NOT NULL,
                completedSetIds TEXT NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Additive column — no table rebuild needed, and ADD COLUMN is supported
        // on every SQLite version this app targets.
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN supersetGroupId TEXT")
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // §17A body-weight tracking with a goal.
        db.execSQL(
            """
            CREATE TABLE body_weight_entries (
                id TEXT NOT NULL PRIMARY KEY,
                weightKg REAL NOT NULL,
                epochDay INTEGER NOT NULL,
                recordedAt INTEGER NOT NULL,
                source TEXT NOT NULL,
                syncStatus TEXT NOT NULL,
                serverId TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_body_weight_entries_epochDay ON body_weight_entries(epochDay)")
        db.execSQL("ALTER TABLE user_profile ADD COLUMN goalWeightKg REAL")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // §5.1 progression rule assignment + per-exercise config. All additive,
        // all with defaults matching the entity, so existing routines keep
        // behaving exactly as they did (NONE = pre-fill last session).
        db.execSQL("ALTER TABLE routines ADD COLUMN progressionRule TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN progressionRuleOverride TEXT")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN incrementKg REAL NOT NULL DEFAULT 2.5")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN repRangeMin INTEGER NOT NULL DEFAULT 8")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN repRangeMax INTEGER NOT NULL DEFAULT 12")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN targetDurationSeconds INTEGER NOT NULL DEFAULT 30")
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Coach Chat: a free-form Q&A surface, separate from the §17D structured Coach.
        db.execSQL(
            """
            CREATE TABLE chat_messages (
                id TEXT NOT NULL PRIMARY KEY,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [
        ExerciseSession::class, Routine::class, RoutineExercise::class, UserProfile::class,
        SetAnalysis::class, Exercise::class, SetLog::class,
        WeeklyPlanEntry::class, PlanOverride::class, ActiveSession::class,
        BodyWeightEntry::class, ChatMessage::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun routineDao(): RoutineDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun setAnalysisDao(): SetAnalysisDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun setLogDao(): SetLogDao
    abstract fun planDao(): PlanDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_form_coach.db"
                ).addMigrations(
                    MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
                ).build().also { INSTANCE = it }
            }
        }
    }
}
