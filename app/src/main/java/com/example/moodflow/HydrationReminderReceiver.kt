package com.example.moodflow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.moodflow.data.PreferencesHelper

class HydrationReminderReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_DRINK_NOW = "com.example.moodflow.ACTION_DRINK_NOW"
        const val ACTION_SNOOZE = "com.example.moodflow.ACTION_SNOOZE"
        const val ACTION_SNOOZE_15M = "com.example.moodflow.ACTION_SNOOZE_15M"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DRINK_NOW -> {
                // Add water intake and show confirmation
                addWaterIntake(context, 250) // Default 250ml
                showConfirmationNotification(context)
            }
            ACTION_SNOOZE_15M -> {
                // Schedule a new reminder for 15 minutes from now
                scheduleSnoozeReminder(context, 15)
            }
            else -> {
                showNotification(context)
            }
        }
    }
    
    private fun addWaterIntake(context: Context, amount: Int) {
        val preferencesHelper = PreferencesHelper(context)
        val progressData = preferencesHelper.getHydrationProgress()
        val currentProgress = progressData.first
        val target = progressData.second
        val newProgress = Math.min(currentProgress + amount, target)
        preferencesHelper.saveHydrationProgress(newProgress, target)
    }
    
    private fun showConfirmationNotification(context: Context) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        val channelId = "hydration_confirmation_channel"
        createNotificationChannel(context, channelId)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "hydration")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle("Great job!")
            .setContentText("You've logged 250ml of water. Keep hydrated!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds
            .build()
        
        NotificationManagerCompat.from(context).notify(3, notification)
    }
    
    private fun scheduleSnoozeReminder(context: Context, minutes: Int) {
        val intent = Intent(context, HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
    
    private fun showNotification(context: Context) {
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        val channelId = "hydration_reminder_channel"
        createNotificationChannel(context, channelId)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "hydration")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create "Drink now" action
        val drinkNowIntent = Intent(context, HydrationReminderReceiver::class.java).apply {
            action = ACTION_DRINK_NOW
        }
        val drinkNowPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            drinkNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val drinkNowAction = NotificationCompat.Action.Builder(
            R.drawable.ic_check,
            "Drink now",
            drinkNowPendingIntent
        ).build()
        
        // Create "Snooze 15m" action
        val snoozeIntent = Intent(context, HydrationReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_15M
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_trend_flat,
            "Snooze 15m",
            snoozePendingIntent
        ).build()
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle(context.getString(R.string.water_reminder_title))
            .setContentText(context.getString(R.string.water_reminder_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(drinkNowAction)
            .addAction(snoozeAction)
            .build()
        
        NotificationManagerCompat.from(context).notify(2, notification)
    }
    
    private fun createNotificationChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = when (channelId) {
                "hydration_reminder_channel" -> "Hydration Reminder"
                "hydration_confirmation_channel" -> "Hydration Confirmation"
                else -> "Hydration Notifications"
            }
            val descriptionText = when (channelId) {
                "hydration_reminder_channel" -> "Reminders to drink water"
                "hydration_confirmation_channel" -> "Confirmation of water intake"
                else -> "Hydration related notifications"
            }
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}