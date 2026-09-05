package com.example.gymformcoach.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.gymformcoach.R
import com.example.gymformcoach.core.data.AppDatabase
import com.example.gymformcoach.core.data.ExerciseSessionRepository
import com.example.gymformcoach.core.data.PlanRepository
import com.example.gymformcoach.core.data.PlannedDay
import com.example.gymformcoach.core.utils.PreferenceManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * §14 workout reminder: fires only on days that actually have a planned workout
 * (§2) with nothing logged yet. Periodic + inexact via WorkManager, per spec —
 * a reminder is not worth an exact alarm.
 */
class WorkoutReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferenceManager(applicationContext)
        if (!prefs.workoutReminderEnabled) return Result.success()

        val database = AppDatabase.getInstance(applicationContext)
        val today = LocalDate.now()

        // Nothing planned today -> nothing to nag about.
        val planned = PlanRepository(database).resolveDay(today)
        if (planned !is PlannedDay.Workout) return Result.success()

        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val loggedToday = ExerciseSessionRepository(database).getAllSessions().first()
            .any { it.performedAt >= startOfDay }
        if (loggedToday) return Result.success()

        showReminderNotification()
        return Result.success()
    }

    private fun showReminderNotification() {
        Notifications.ensureChannels(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_WORKOUT_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Workout planned today")
            .setContentText("You haven't logged today's session yet.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        applicationContext.getSystemService<NotificationManager>()
            ?.notify(Notifications.ID_WORKOUT_REMINDER, notification)
    }

    companion object {
        private const val WORK_NAME = "workout_reminder"

        fun schedule(context: Context, atHour: Int, atMinute: Int) {
            val now = LocalTime.now()
            val target = LocalTime.of(atHour, atMinute)
            val delayMinutes = if (target.isAfter(now)) {
                java.time.Duration.between(now, target).toMinutes()
            } else {
                java.time.Duration.between(now, target).toMinutes() + TimeUnit.DAYS.toMinutes(1)
            }

            val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
