package com.example.moodflow.model

import java.util.Date

data class Habit(
    val id: Int,
    var name: String,
    var category: String = "General",
    var description: String = "",
    var completed: Boolean,
    var streak: Int,
    var progress: Int,
    var target: Int,
    var frequency: String = "daily", // daily, weekly, monthly
    var reminderTime: String? = null,
    var reminderEnabled: Boolean = false,
    var lastCompletedDate: Date? = null,
    var createdDate: Date = Date(),
    var color: String = "#3DDC84", // Default Material Green
    var icon: String = "check" // Default icon
) {
    val completionPercentage: Int
        get() = if (target > 0) (progress.toDouble() / target.toDouble() * 100).toInt() else 0
        
    val isOverdue: Boolean
        get() = lastCompletedDate?.let { lastDate ->
            when (frequency) {
                "daily" -> Date().time - lastDate.time > 24 * 60 * 60 * 1000
                "weekly" -> Date().time - lastDate.time > 7 * 24 * 60 * 60 * 1000
                "monthly" -> Date().time - lastDate.time > 30 * 24 * 60 * 60 * 1000
                else -> false
            }
        } ?: true
}