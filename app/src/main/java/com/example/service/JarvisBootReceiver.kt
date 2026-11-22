package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.JarvisPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles BOOT_COMPLETED cleanly in compliance with Android 14+ foreground service restrictions:
 * instead of attempting to start a microphone FGS from boot (which the OS blocks), it posts a
 * high-visibility launch notification when enabled by Boss so one tap resumes JARVIS.
 */
class JarvisBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = JarvisPreferencesRepository(context.applicationContext)
                    .settingsFlow
                    .first()
                if (settings.autoNotifyOnBoot) {
                    showResumeNotification(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showResumeNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                JarvisForegroundService.CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            nm.createNotificationChannel(channel)
        }

        val tapIntent = PendingIntent.getActivity(
            context,
            202,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, JarvisForegroundService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("JARVIS পুনরায় চালুর জন্য প্রস্তুত")
            .setContentText("Boss, ডিভাইস রিস্টার্ট হয়েছে। ব্যাকগ্রাউন্ড মাইক্রোফোন চালু করতে ট্যাপ করুন।")
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        nm.notify(4043, notification)
    }
}
