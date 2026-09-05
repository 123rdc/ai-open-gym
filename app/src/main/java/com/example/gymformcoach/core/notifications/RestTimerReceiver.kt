package com.example.gymformcoach.core.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.gymformcoach.R

/**
 * Fires when a scheduled rest period ends (§7.1/§7.2). Runs from AlarmManager, so
 * it works with the app backgrounded or closed — the point of the whole design.
 */
class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        vibrate(context)
        showNotification(context)
    }

    /** §7.1: vibration always; the notification's own sound respects silent mode. */
    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(400)
        }
    }

    private fun showNotification(context: Context) {
        Notifications.ensureChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Permission revoked since the alarm was scheduled — the vibration
            // above still fired, so the set isn't silently lost.
            return
        }

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_REST_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rest over")
            .setContentText("Time for your next set.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        context.getSystemService<NotificationManager>()
            ?.notify(Notifications.ID_REST_TIMER, notification)
    }
}
