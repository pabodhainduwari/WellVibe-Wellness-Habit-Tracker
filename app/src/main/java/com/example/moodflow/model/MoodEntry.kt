package com.example.moodflow.model

import java.util.Date

data class MoodEntry(
    val id: String = System.currentTimeMillis().toString(),
    val date: Date,
    val mood: String,
    val note: String,
    val intensity: Int = 5, // 1-10 scale
    val activities: List<String> = emptyList(), // What user was doing
    val tags: List<String> = emptyList(), // Custom tags/categories
    val location: String? = null, // Optional location context
    val imageUri: String? = null, // Optional associated image
    val weather: String? = null // Optional weather context
) {
    val moodScore: Int
        get() = when (mood.lowercase()) {
            "😊", "😃", "😍" -> 5
            "😎", "🤗" -> 4
            "😐", "🤔" -> 3
            "😔", "😰" -> 2
            "😤", "😷" -> 1
            else -> 3
        }
        
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "date" to date.time,
            "mood" to mood,
            "note" to note,
            "intensity" to intensity,
            "activities" to activities,
            "tags" to tags,
            "location" to (location ?: ""),
            "imageUri" to (imageUri ?: ""),
            "weather" to (weather ?: "")
        )
    }
}