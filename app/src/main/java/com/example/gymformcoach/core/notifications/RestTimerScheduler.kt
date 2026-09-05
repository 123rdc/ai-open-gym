package com.example.gymformcoach.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService

/**
 * §7.2: the rest timer's completion is scheduled with AlarmManager rather than a
 * Compose-side coroutine, so it still fires when the app is backgrounded or the
 * screen is off. The visible countdown is driven separately from the wall-clock
 * end timestamp this returns.
 */
object RestTimerScheduler {

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, RestTimerReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, Notifications.ID_REST_TIMER, intent, flags)
    }

    /** Returns the wall-clock epoch millis at which rest ends. */
    fun schedule(context: Context, durationSeconds: Int): Long {
        Notifications.ensureChannels(context)
        val endAt = System.currentTimeMillis() + durationSeconds * 1000L
        val alarmManager = context.getSystemService<AlarmManager>() ?: return endAt

        // setExactAndAllowWhileIdle survives Doze; on S+ exact alarms need a
        // permission the user can revoke, so fall back to an inexact alarm rather
        // than crashing with SecurityException.
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pendingIntent(context))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pendingIntent(context))
        }
        return endAt
    }

    fun cancel(context: Context) {
        context.getSystemService<AlarmManager>()?.cancel(pendingIntent(context))
    }
}
