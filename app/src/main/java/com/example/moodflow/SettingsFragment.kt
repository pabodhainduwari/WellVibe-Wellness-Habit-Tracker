package com.example.moodflow

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.Button
import com.google.android.material.materialswitch.MaterialSwitch
import android.widget.Toast
import com.example.moodflow.data.AchievementManager
import com.example.moodflow.data.ReminderManager
import com.example.moodflow.model.UserProfile
import com.example.moodflow.views.WeeklySummaryFragment
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class SettingsFragment : BaseFragment() {
    
    private lateinit var achievementManager: AchievementManager
    private lateinit var reminderManager: ReminderManager
    
    override fun getLayoutId(): Int {
        return R.layout.fragment_settings
    }
    
    override fun setupViews(view: View) {
        achievementManager = AchievementManager(requireContext())
        reminderManager = ReminderManager(requireContext())
        
        // Load current user profile
        val userProfile = preferencesHelper.getUserProfile()
        
        // Set up profile settings
        val nameEditText = view.findViewById<android.widget.EditText>(R.id.name_edit_text)
        val emailEditText = view.findViewById<android.widget.EditText>(R.id.email_edit_text)
        
        nameEditText.setText(userProfile.name)
        emailEditText.setText(userProfile.email)
        
        // Set up save button
        val saveButton = view.findViewById<Button>(R.id.save_profile_button)
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            
            if (name.isNotEmpty() && email.isNotEmpty()) {
                val newProfile = UserProfile(name, email)
                preferencesHelper.saveUserProfile(newProfile)
                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up notification settings
        val notificationsSwitch = view.findViewById<MaterialSwitch>(R.id.notifications_switch)
        notificationsSwitch.isChecked = true // Default to enabled
        
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up dark mode switch
        val darkModeSwitch = view.findViewById<MaterialSwitch>(R.id.dark_mode_switch)
        darkModeSwitch.isChecked = false // Default to light mode
        
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(context, "Dark mode enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Light mode enabled", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up weekly summary button
        val weeklySummaryButton = view.findViewById<Button>(R.id.weekly_summary_button)
        weeklySummaryButton.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(WeeklySummaryFragment())
        }
        
        // Set up achievements button
        val achievementsButton = view.findViewById<Button>(R.id.achievements_button)
        achievementsButton.setOnClickListener {
            showAchievementsDialog()
        }
        
        // Set up export button
        val exportButton = view.findViewById<Button>(R.id.export_settings_button)
        exportButton.setOnClickListener {
            exportSettings()
        }
        
        // Set up import button
        val importButton = view.findViewById<Button>(R.id.import_settings_button)
        importButton.setOnClickListener {
            importSettings(view)
        }
    }
    
    private fun showAchievementsDialog() {
        val achievements = achievementManager.getAchievements()
        val unlockedCount = achievements.count { it.unlocked }
        
        val message = "You've unlocked $unlockedCount out of ${achievements.size} achievements!\n\n" +
                achievements.joinToString("\n") { achievement ->
                    if (achievement.unlocked) {
                        "✓ ${achievement.name} - ${achievement.description}"
                    } else {
                        "○ ${achievement.name} - ${achievement.description}"
                    }
                }
        
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Achievements")
        builder.setMessage(message)
        builder.setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }
    
    private fun exportSettings() {
        try {
            // Create a JSON object with all settings
            val settingsJson = JSONObject()
            
            // Add user profile
            val userProfile = preferencesHelper.getUserProfile()
            settingsJson.put("userProfile", JSONObject().apply {
                put("name", userProfile.name)
                put("email", userProfile.email)
            })
            
            // Add habits
            val habits = preferencesHelper.getHabits()
            val habitsArray = org.json.JSONArray()
            habits.forEach { habit ->
                habitsArray.put(JSONObject().apply {
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
                })
            }
            settingsJson.put("habits", habitsArray)
            
            // Add mood entries
            val moodEntries = preferencesHelper.getMoodEntries()
            val moodEntriesArray = org.json.JSONArray()
            moodEntries.forEach { entry ->
                moodEntriesArray.put(JSONObject().apply {
                    put("id", entry.id)
                    put("date", entry.date.time)
                    put("mood", entry.mood)
                    put("note", entry.note)
                    put("intensity", entry.intensity)
                    put("activities", org.json.JSONArray(entry.activities))
                    put("tags", org.json.JSONArray(entry.tags))
                    put("location", entry.location ?: "")
                    put("imageUri", entry.imageUri ?: "")
                    put("weather", entry.weather ?: "")
                })
            }
            settingsJson.put("moodEntries", moodEntriesArray)
            
            // Add hydration progress
            val (progress, target) = preferencesHelper.getHydrationProgress()
            settingsJson.put("hydrationProgress", JSONObject().apply {
                put("progress", progress)
                put("target", target)
            })
            
            // Add achievements
            val achievements = achievementManager.getAchievements()
            val achievementsArray = org.json.JSONArray()
            achievements.forEach { achievement ->
                achievementsArray.put(JSONObject().apply {
                    put("id", achievement.id)
                    put("name", achievement.name)
                    put("description", achievement.description)
                    put("icon", achievement.icon)
                    put("unlocked", achievement.unlocked)
                    put("unlockDate", achievement.unlockDate ?: 0)
                })
            }
            settingsJson.put("achievements", achievementsArray)
            
            // Show the JSON in a dialog for sharing
            showExportDialog(settingsJson.toString(2))
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting settings: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showExportDialog(jsonString: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Export Settings")
        builder.setMessage("Copy the JSON below and save it to backup your data:")
        
        val input = android.widget.EditText(context)
        input.setText(jsonString)
        input.setTextIsSelectable(true)
        input.setLines(10)
        input.isSingleLine = false
        
        val container = android.widget.ScrollView(context)
        container.addView(input)
        builder.setView(container)
        
        builder.setPositiveButton("Share") { _, _ ->
            // Create share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, jsonString)
                putExtra(Intent.EXTRA_TITLE, "Routinely Settings Backup")
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Settings")
            startActivity(chooser)
        }
        
        builder.setNegativeButton("Close") { dialog, _ ->
            dialog.cancel()
        }
        
        builder.show()
    }
    
    private fun importSettings(view: View) {
        val importJsonInput = view.findViewById<TextInputEditText>(R.id.import_json_input)
        val jsonString = importJsonInput.text.toString().trim()
        
        if (jsonString.isEmpty()) {
            Toast.makeText(context, "Please paste JSON data to import", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val settingsJson = JSONObject(jsonString)
            
            // Confirm import
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Import Settings")
            builder.setMessage("This will overwrite your current settings. Continue?")
            
            builder.setPositiveButton("Import") { _, _ ->
                try {
                    // Import user profile
                    if (settingsJson.has("userProfile")) {
                        val profileJson = settingsJson.getJSONObject("userProfile")
                        val userProfile = UserProfile(
                            name = profileJson.getString("name"),
                            email = profileJson.getString("email")
                        )
                        preferencesHelper.saveUserProfile(userProfile)
                    }
                    
                    // Import habits
                    if (settingsJson.has("habits")) {
                        val habitsArray = settingsJson.getJSONArray("habits")
                        val habits = mutableListOf<com.example.moodflow.model.Habit>()
                        for (i in 0 until habitsArray.length()) {
                            val habitJson = habitsArray.getJSONObject(i)
                            habits.add(
                                com.example.moodflow.model.Habit(
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
                                    lastCompletedDate = habitJson.optLong("lastCompletedDate", 0).let { if (it > 0) java.util.Date(it) else null },
                                    createdDate = java.util.Date(habitJson.optLong("createdDate", System.currentTimeMillis())),
                                    color = habitJson.optString("color", "#3DDC84"),
                                    icon = habitJson.optString("icon", "check")
                                )
                            )
                        }
                        preferencesHelper.saveHabits(habits)
                    }
                    
                    // Import mood entries
                    if (settingsJson.has("moodEntries")) {
                        val moodEntriesArray = settingsJson.getJSONArray("moodEntries")
                        val moodEntries = mutableListOf<com.example.moodflow.model.MoodEntry>()
                        for (i in 0 until moodEntriesArray.length()) {
                            val entryJson = moodEntriesArray.getJSONObject(i)
                            moodEntries.add(
                                com.example.moodflow.model.MoodEntry(
                                    id = entryJson.optString("id", System.currentTimeMillis().toString()),
                                    date = java.util.Date(entryJson.getLong("date")),
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
                        preferencesHelper.saveMoodEntries(moodEntries)
                    }
                    
                    // Import hydration progress
                    if (settingsJson.has("hydrationProgress")) {
                        val hydrationJson = settingsJson.getJSONObject("hydrationProgress")
                        val progress = hydrationJson.getInt("progress")
                        val target = hydrationJson.getInt("target")
                        preferencesHelper.saveHydrationProgress(progress, target)
                    }
                    
                    // Import achievements
                    if (settingsJson.has("achievements")) {
                        val achievementsArray = settingsJson.getJSONArray("achievements")
                        val achievements = mutableListOf<com.example.moodflow.model.Achievement>()
                        for (i in 0 until achievementsArray.length()) {
                            val achievementJson = achievementsArray.getJSONObject(i)
                            achievements.add(
                                com.example.moodflow.model.Achievement(
                                    id = achievementJson.getString("id"),
                                    name = achievementJson.getString("name"),
                                    description = achievementJson.getString("description"),
                                    icon = achievementJson.getString("icon"),
                                    unlocked = achievementJson.getBoolean("unlocked"),
                                    unlockDate = achievementJson.optLong("unlockDate", 0).let { if (it > 0) it else null }
                                )
                            )
                        }
                        achievementManager.saveAchievements(achievements)
                    }
                    
                    Toast.makeText(context, "Settings imported successfully!", Toast.LENGTH_LONG).show()
                    
                    // Clear the input field
                    importJsonInput.setText("")
                } catch (e: Exception) {
                    Toast.makeText(context, "Error importing settings: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            
            builder.show()
        } catch (e: Exception) {
            Toast.makeText(context, "Invalid JSON format: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}