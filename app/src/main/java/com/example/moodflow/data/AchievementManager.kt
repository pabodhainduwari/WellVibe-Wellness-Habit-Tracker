package com.example.moodflow.data

import android.content.Context
import android.content.SharedPreferences
import com.example.moodflow.model.Achievement
import org.json.JSONArray
import org.json.JSONObject

class AchievementManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("routinely_achievements", Context.MODE_PRIVATE)
    
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
    
    fun checkAndUnlockAchievements(preferencesHelper: PreferencesHelper): List<Achievement> {
        val unlockedAchievements = mutableListOf<Achievement>()
        
        // Check for "First Habit" achievement
        val habits = preferencesHelper.getHabits()
        if (habits.any { it.completed } && unlockAchievement("first_habit")) {
            unlockedAchievements.add(getAchievements().first { it.id == "first_habit" })
        }
        
        // Check for "Week Warrior" achievement
        val streak = preferencesHelper.getStreak()
        if (streak >= 7 && unlockAchievement("seven_day_streak")) {
            unlockedAchievements.add(getAchievements().first { it.id == "seven_day_streak" })
        }
        
        // Check for "Perfect Day" achievement
        if (habits.isNotEmpty() && habits.all { it.completed } && unlockAchievement("perfect_day")) {
            unlockedAchievements.add(getAchievements().first { it.id == "perfect_day" })
        }
        
        return unlockedAchievements
    }
}