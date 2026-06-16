package com.example.moodflow.model

import java.util.Date

data class HabitStreak(
    val habitId: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val lastCompletedDate: Date
)

data class WaterIntakeRecord(
    val date: Date,
    val amount: Int,
    val target: Int,
    val beverage: String = "Water",
    val reminders: List<Date> = emptyList()
)

data class MoodStatistics(
    val averageWeeklyScore: Float,
    val mostCommonMood: String,
    val bestDayOfWeek: String,
    val moodTrends: Map<String, Int>
)

data class HabitAnalytics(
    val completionRate: Float,
    val bestPerformanceTime: String,
    val streakHistory: List<HabitStreak>,
    val weeklyCompletion: Map<String, Float>
)

data class UserSettings(
    var darkMode: Boolean = false,
    var hapticFeedback: Boolean = true,
    var language: String = "English",
    var waterUnit: String = "ml",
    var notificationPreferences: NotificationPreferences = NotificationPreferences(),
    var backupEnabled: Boolean = false,
    var lastBackupDate: Date? = null
)

data class NotificationPreferences(
    var hydrationEnabled: Boolean = true,
    var habitEnabled: Boolean = true,
    var dailySummaryEnabled: Boolean = true,
    var reminderSound: String = "default",
    var hydrationFrequency: Int = 2, // hours
    var hydrationStartTime: String = "08:00",
    var hydrationEndTime: String = "22:00",
    var moodReminderTime: String = "20:00"
)