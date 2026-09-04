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

@Database(
    entities = [ExerciseSession::class, Routine::class, RoutineExercise::class, UserProfile::class, SetAnalysis::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun routineDao(): RoutineDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun setAnalysisDao(): SetAnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_form_coach.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
        }
    }
}
