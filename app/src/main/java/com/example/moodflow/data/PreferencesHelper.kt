package com.example.moodflow.data

import android.content.Context
import android.content.SharedPreferences
import com.example.moodflow.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mood_flow_prefs", Context.MODE_PRIVATE)
    private val analyticsPrefs: SharedPreferences = context.getSharedPreferences("mood_flow_analytics", Context.MODE_PRIVATE)
    private val settingsPrefs: SharedPreferences = context.getSharedPreferences("mood_flow_settings", Context.MODE_PRIVATE)
    
    // Achievements Management
    fun getAchievements(): List<Achievement> {
        val achievementsJsonString = prefs.getString("achievements", null)
        if (achievementsJsonString == null) {
            // Return default achievements if none exist
            return getDefaultAchievements()
        }
        
        val achievementsJson = JSONArray(achievementsJsonString)
        val achievements = mutableListOf<Achievement>()
        
        for (i in 0 until achievementsJson.length()) {
            val achievementJson = achievementsJson.getJSONObject(i)
            achievements.add(
                Achievement(
                    id = achievementJson.getString("id"),
                    name = achievementJson.getString("name"),
                    description = achievementJson.getString("description"),
                    icon = achievementJson.getString("icon"),
                    unlocked = achievementJson.getBoolean("unlocked"),
                    unlockDate = achievementJson.optLong("unlockDate", 0).let { if (it > 0) it else null }
                )
            )
        }
        return achievements
    }
    
    fun saveAchievements(achievements: List<Achievement>) {
        val achievementsJson = JSONArray()
        achievements.forEach { achievement ->
            val achievementJson = JSONObject().apply {
                put("id", achievement.id)
                put("name", achievement.name)
                put("description", achievement.description)
                put("icon", achievement.icon)
                put("unlocked", achievement.unlocked)
                put("unlockDate", achievement.unlockDate ?: 0)
            }
            achievementsJson.put(achievementJson)
        }
        prefs.edit().putString("achievements", achievementsJson.toString()).apply()
    }
    
    private fun getDefaultAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_habit",
                name = "First Habit",
                description = "Complete your first habit",
                icon = "trophy"
            ),
            Achievement(
                id = "seven_day_streak",
                name = "Week Warrior",
                description = "Maintain a 7-day streak",
                icon = "fire"
            ),
            Achievement(
                id = "hydration_champion",
                name = "Hydration Champion",
                description = "Drink 8 glasses of water for 3 consecutive days",
                icon = "water_drop"
            ),
            Achievement(
                id = "mood_tracker",
                name = "Mood Master",
                description = "Log your mood for 5 consecutive days",
                icon = "mood"
            ),
            Achievement(
                id = "perfect_day",
                name = "Perfect Day",
                description = "Complete all habits in a day",
                icon = "star"
            )
        )
    }
    
    fun unlockAchievement(achievementId: String): Boolean {
        val achievements = getAchievements().toMutableList()
        val achievementIndex = achievements.indexOfFirst { it.id == achievementId }
        
        if (achievementIndex != -1 && !achievements[achievementIndex].unlocked) {
            achievements[achievementIndex] = achievements[achievementIndex].copy(
                unlocked = true,
                unlockDate = System.currentTimeMillis()
            )
            saveAchievements(achievements)
            return true
        }
        return false
    }

    // User Profile
    fun saveUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_name", profile.name)
            .putString("user_email", profile.email)
            .apply()
    }

    fun getUserProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString("user_name", "User") ?: "User",
            email = prefs.getString("user_email", "") ?: ""
        )
    }

    // Enhanced Habits Methods
    // Daily Completion tracking
    fun saveDailyCompletion(completion: Int) {
        prefs.edit().putInt("daily_completion", completion).apply()
        
        // Save history
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val historyJson = JSONObject(prefs.getString("completion_history", "{}"))
        historyJson.put(date, completion)
        prefs.edit().putString("completion_history", historyJson.toString()).apply()
    }
    
    fun getDailyCompletion(): Int {
        return prefs.getInt("daily_completion", 0)
    }
    
    // Streak Management
    fun getStreak(): Int {
        return prefs.getInt("current_streak", 0)
    }
    
    fun updateStreak(completed: Boolean) {
        val currentStreak = getStreak()
        val lastCompletionDate = prefs.getLong("last_streak_date", 0)
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        
        val newStreak = when {
            completed && lastCompletionDate == 0L -> 1
            completed && isSameDay(lastCompletionDate, yesterday.timeInMillis) -> currentStreak + 1
            completed && !isSameDay(lastCompletionDate, today.timeInMillis) -> 1
            !completed -> 0
            else -> currentStreak
        }
        
        prefs.edit()
            .putInt("current_streak", newStreak)
            .putLong("last_streak_date", if (completed) today.timeInMillis else 0)
            .apply()
    }
    
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    fun saveHabits(habits: List<Habit>) {
        val habitsJson = JSONArray()
        habits.forEach { habit ->
            val habitJson = JSONObject().apply {
                put("id", habit.id)
                put("name", habit.name)
                put("category", habit.category)
                put("description", habit.description)
                put("completed", habit.completed)
                put("streak", habit.streak)
                put("progress", habit.progress)
                put("target", habit.target)
                put("frequency", habit.frequency)
                put("reminderTime", habit.reminderTime ?: "")
                put("reminderEnabled", habit.reminderEnabled)
                put("lastCompletedDate", habit.lastCompletedDate?.time ?: 0)
                put("createdDate", habit.createdDate.time)
                put("color", habit.color)
                put("icon", habit.icon)
            }
            habitsJson.put(habitJson)
        }
        prefs.edit().putString("habits", habitsJson.toString()).apply()
    }

    fun getHabits(): List<Habit> {
        val habitsJsonString = prefs.getString("habits", null) ?: return getDefaultHabits()
        val habitsJson = JSONArray(habitsJsonString)
        val habits = mutableListOf<Habit>()
        
        for (i in 0 until habitsJson.length()) {
            val habitJson = habitsJson.getJSONObject(i)
            habits.add(
                Habit(
                    id = habitJson.getInt("id"),
                    name = habitJson.getString("name"),
                    category = habitJson.optString("category", "General"),
                    description = habitJson.optString("description", ""),
                    completed = habitJson.getBoolean("completed"),
                    streak = habitJson.getInt("streak"),
                    progress = habitJson.getInt("progress"),
                    target = habitJson.getInt("target"),
                    frequency = habitJson.optString("frequency", "daily"),
                    reminderTime = habitJson.optString("reminderTime", null),
                    reminderEnabled = habitJson.optBoolean("reminderEnabled", false),
                    lastCompletedDate = habitJson.optLong("lastCompletedDate", 0).let { if (it > 0) Date(it) else null },
                    createdDate = Date(habitJson.optLong("createdDate", System.currentTimeMillis())),
                    color = habitJson.optString("color", "#3DDC84"),
                    icon = habitJson.optString("icon", "check")
                )
            )
        }
        return habits
    }

    private fun getDefaultHabits(): List<Habit> {
        val now = Date()
        return listOf(
            Habit(1, "Drink 8 glasses of water", "Health", "Stay hydrated throughout the day", false, 12, 6, 8, 
                  reminderEnabled = true, reminderTime = "10:00", lastCompletedDate = now),
            Habit(2, "Morning Meditation", "Mindfulness", "10 minutes of mindful breathing", true, 8, 1, 1,
                  frequency = "daily", reminderTime = "07:00", lastCompletedDate = now),
            Habit(3, "10k Steps", "Fitness", "Daily walking goal", false, 3, 3247, 10000,
                  icon = "directions_walk", color = "#FF4081")
        )
    }

    // Enhanced Mood Entries Methods
    fun saveMoodEntries(entries: List<MoodEntry>) {
        val entriesJson = JSONArray()
        entries.forEach { entry ->
            val entryJson = JSONObject().apply {
                put("id", entry.id)
                put("date", entry.date.time)
                put("mood", entry.mood)
                put("note", entry.note)
                put("intensity", entry.intensity)
                put("activities", JSONArray(entry.activities))
                put("tags", JSONArray(entry.tags))
                put("location", entry.location ?: "")
                put("imageUri", entry.imageUri ?: "")
                put("weather", entry.weather ?: "")
            }
            entriesJson.put(entryJson)
        }
        prefs.edit().putString("mood_entries", entriesJson.toString()).apply()
    }

    fun getMoodEntries(): List<MoodEntry> {
        val entriesJsonString = prefs.getString("mood_entries", null) ?: return emptyList()
        val entriesJson = JSONArray(entriesJsonString)
        val entries = mutableListOf<MoodEntry>()
        
        for (i in 0 until entriesJson.length()) {
            val entryJson = entriesJson.getJSONObject(i)
            entries.add(
                MoodEntry(
                    id = entryJson.optString("id", System.currentTimeMillis().toString()),
                    date = Date(entryJson.getLong("date")),
                    mood = entryJson.getString("mood"),
                    note = entryJson.getString("note"),
                    intensity = entryJson.optInt("intensity", 5),
                    activities = entryJson.optJSONArray("activities")?.let { array ->
                        List(array.length()) { array.getString(it) }
                    } ?: emptyList(),
                    tags = entryJson.optJSONArray("tags")?.let { array ->
                        List(array.length()) { array.getString(it) }
                    } ?: emptyList(),
                    location = entryJson.optString("location", null),
                    imageUri = entryJson.optString("imageUri", null),
                    weather = entryJson.optString("weather", null)
                )
            )
        }
        return entries.sortedByDescending { it.date }
    }
    
    // Mood Statistics Methods
    fun saveMoodStatistics(stats: MoodStatistics) {
        analyticsPrefs.edit().apply {
            putFloat("avg_weekly_mood", stats.averageWeeklyScore)
            putString("most_common_mood", stats.mostCommonMood)
            putString("best_day", stats.bestDayOfWeek)
            putString("mood_trends", JSONObject().apply {
                stats.moodTrends.forEach { (key, value) -> put(key, value) }
            }.toString())
        }.apply()
    }

    fun getMoodStatistics(): MoodStatistics {
        return MoodStatistics(
            averageWeeklyScore = analyticsPrefs.getFloat("avg_weekly_mood", 3.5f),
            mostCommonMood = analyticsPrefs.getString("most_common_mood", "😊") ?: "😊",
            bestDayOfWeek = analyticsPrefs.getString("best_day", "Monday") ?: "Monday",
            moodTrends = JSONObject(analyticsPrefs.getString("mood_trends", "{}")).let { json ->
                json.keys().asSequence().associateWith { json.getInt(it) }
            }
        )
    }

    // Enhanced Hydration Methods
    fun saveWaterIntakeRecord(record: WaterIntakeRecord) {
        val recordJson = JSONObject().apply {
            put("date", record.date.time)
            put("amount", record.amount)
            put("target", record.target)
            put("beverage", record.beverage)
            put("reminders", JSONArray(record.reminders.map { it.time }))
        }
        val existing = JSONArray(prefs.getString("water_records", "[]"))
        val recordsJson = JSONArray()
        // Keep only the most recent 50 records to avoid unbounded growth
        val startIndex = max(0, existing.length() - 49)
        for (i in startIndex until existing.length()) {
            recordsJson.put(existing.getJSONObject(i))
        }
        recordsJson.put(recordJson)
        prefs.edit().putString("water_records", recordsJson.toString()).apply()
    }

    fun getWaterIntakeRecords(days: Int = 7): List<WaterIntakeRecord> {
        val recordsJson = JSONArray(prefs.getString("water_records", "[]"))
        val records = mutableListOf<WaterIntakeRecord>()
        val cutoffDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }.time
        
        for (i in 0 until recordsJson.length()) {
            val recordJson = recordsJson.getJSONObject(i)
            val date = Date(recordJson.getLong("date"))
            if (date.after(cutoffDate)) {
                records.add(
                    WaterIntakeRecord(
                        date = date,
                        amount = recordJson.getInt("amount"),
                        target = recordJson.getInt("target"),
                        beverage = recordJson.optString("beverage", "Water"),
                        reminders = recordJson.optJSONArray("reminders")?.let { array ->
                            List(array.length()) { Date(array.getLong(it)) }
                        } ?: emptyList()
                    )
                )
            }
        }
        return records.sortedByDescending { it.date }
    }

    fun saveHydrationProgress(progress: Int, target: Int) {
        prefs.edit()
            .putInt("hydration_progress", progress)
            .putInt("hydration_target", target)
            .apply()
    }

    fun appendWaterIntake(amount: Int, target: Int, beverage: String) {
        val record = WaterIntakeRecord(
            date = Date(),
            amount = amount,
            target = target,
            beverage = beverage
        )
        saveWaterIntakeRecord(record)
    }

    fun getHydrationProgress(): Pair<Int, Int> {
        return Pair(
            prefs.getInt("hydration_progress", 1250),
            prefs.getInt("hydration_target", 2000)
        )
    }

    // Reminder Time Methods
    fun getReminderStartTime(): Date? {
        val timeMillis = prefs.getLong("reminder_start_time", -1)
        return if (timeMillis != -1L) Date(timeMillis) else null
    }

    fun saveReminderStartTime(time: Date) {
        prefs.edit().putLong("reminder_start_time", time.time).apply()
    }

    fun getLastResetDate(): Date? {
        val timeMillis = prefs.getLong("last_reset_date", -1)
        return if (timeMillis != -1L) Date(timeMillis) else null
    }

    fun saveLastResetDate(date: Date) {
        prefs.edit().putLong("last_reset_date", date.time).apply()
    }

    fun clearWaterIntakeRecords() {
        prefs.edit().putString("water_records", "[]").apply()
    }

    // Analytics Methods
    fun saveHabitAnalytics(habitId: Int, analytics: HabitAnalytics) {
        val key = "habit_analytics_$habitId"
        analyticsPrefs.edit().apply {
            putFloat("${key}_completion_rate", analytics.completionRate)
            putString("${key}_best_time", analytics.bestPerformanceTime)
            putString("${key}_streak_history", JSONArray(analytics.streakHistory.map { 
                JSONObject().apply {
                    put("habitId", it.habitId)
                    put("currentStreak", it.currentStreak)
                    put("bestStreak", it.bestStreak)
                    put("lastCompletedDate", it.lastCompletedDate.time)
                }
            }).toString())
            putString("${key}_weekly_completion", JSONObject(analytics.weeklyCompletion).toString())
        }.apply()
    }

    fun getHabitAnalytics(habitId: Int): HabitAnalytics {
        val key = "habit_analytics_$habitId"
        return HabitAnalytics(
            completionRate = analyticsPrefs.getFloat("${key}_completion_rate", 0f),
            bestPerformanceTime = analyticsPrefs.getString("${key}_best_time", "morning") ?: "morning",
            streakHistory = JSONArray(analyticsPrefs.getString("${key}_streak_history", "[]")).let { array ->
                List(array.length()) { i ->
                    val json = array.getJSONObject(i)
                    HabitStreak(
                        habitId = json.getInt("habitId"),
                        currentStreak = json.getInt("currentStreak"),
                        bestStreak = json.getInt("bestStreak"),
                        lastCompletedDate = Date(json.getLong("lastCompletedDate"))
                    )
                }
            },
            weeklyCompletion = JSONObject(analyticsPrefs.getString("${key}_weekly_completion", "{}")).let { json ->
                json.keys().asSequence().associateWith { json.getDouble(it).toFloat() }
            }
        )
    }

    // User Settings Management
    fun saveUserSettings(settings: UserSettings) {
        settingsPrefs.edit().apply {
            putBoolean("dark_mode", settings.darkMode)
            putBoolean("haptic_feedback", settings.hapticFeedback)
            putString("language", settings.language)
            putString("water_unit", settings.waterUnit)
            putBoolean("backup_enabled", settings.backupEnabled)
            settings.lastBackupDate?.let { putLong("last_backup_date", it.time) }
            
            // Notification preferences
            putBoolean("notification_hydration", settings.notificationPreferences.hydrationEnabled)
            putBoolean("notification_habit", settings.notificationPreferences.habitEnabled)
            putBoolean("notification_daily_summary", settings.notificationPreferences.dailySummaryEnabled)
            putString("notification_sound", settings.notificationPreferences.reminderSound)
            putInt("hydration_frequency", settings.notificationPreferences.hydrationFrequency)
            putString("hydration_start_time", settings.notificationPreferences.hydrationStartTime)
            putString("hydration_end_time", settings.notificationPreferences.hydrationEndTime)
            putString("mood_reminder_time", settings.notificationPreferences.moodReminderTime)
        }.apply()
    }

    fun getUserSettings(): UserSettings {
        return UserSettings(
            darkMode = settingsPrefs.getBoolean("dark_mode", false),
            hapticFeedback = settingsPrefs.getBoolean("haptic_feedback", true),
            language = settingsPrefs.getString("language", "English") ?: "English",
            waterUnit = settingsPrefs.getString("water_unit", "ml") ?: "ml",
            notificationPreferences = NotificationPreferences(
                hydrationEnabled = settingsPrefs.getBoolean("notification_hydration", true),
                habitEnabled = settingsPrefs.getBoolean("notification_habit", true),
                dailySummaryEnabled = settingsPrefs.getBoolean("notification_daily_summary", true),
                reminderSound = settingsPrefs.getString("notification_sound", "default") ?: "default",
                hydrationFrequency = settingsPrefs.getInt("hydration_frequency", 2),
                hydrationStartTime = settingsPrefs.getString("hydration_start_time", "08:00") ?: "08:00",
                hydrationEndTime = settingsPrefs.getString("hydration_end_time", "22:00") ?: "22:00",
                moodReminderTime = settingsPrefs.getString("mood_reminder_time", "20:00") ?: "20:00"
            ),
            backupEnabled = settingsPrefs.getBoolean("backup_enabled", false),
            lastBackupDate = settingsPrefs.getLong("last_backup_date", -1).let { if (it > 0) Date(it) else null }
        )
    }
}