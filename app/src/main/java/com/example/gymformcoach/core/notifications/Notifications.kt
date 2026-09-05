package com.example.gymformcoach.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/**
 * §14: two opt-in notification types on their own channels, so the user can tune
 * each independently at the OS level rather than getting one all-or-nothing app
 * toggle.
 */
object Notifications {
    const val CHANNEL_REST_TIMER = "rest_timer"
    const val CHANNEL_WORKOUT_REMINDER = "workout_reminder"

    const val ID_REST_TIMER = 1001
    const val ID_WORKOUT_REMINDER = 1002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REST_TIMER,
                "Rest timer",
                // HIGH so it can surface while the phone is face-down between sets.
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Fires when your rest period is over." }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WORKOUT_REMINDER,
                "Workout reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminds you about a planned workout you haven't logged yet." }
        )
    }
}
