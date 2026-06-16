package com.example.moodflow.data

import android.content.Context
import android.content.SharedPreferences
import com.example.moodflow.model.User
import java.util.*

class UserManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_USERS = "users"
    }
    
    fun registerUser(name: String, email: String, password: String): Boolean {
        // Check if user already exists
        if (getUserByEmail(email) != null) {
            return false // User already exists
        }
        
        // Create new user
        val userId = UUID.randomUUID().toString()
        val user = User(userId, name, email, password)
        
        // For simplicity in this example, we'll just save the current user
        // In a real app, you would store all users in a database
        prefs.edit()
            .putString(KEY_CURRENT_USER_ID, userId)
            .apply()
            
        return true
    }
    
    fun loginUser(email: String, password: String): User? {
        // In a real implementation, this would check against stored users
        // For this example, we'll just simulate a successful login
        val userId = UUID.randomUUID().toString()
        val user = User(userId, "User", email, password)
        prefs.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
        return user
    }
    
    fun logout() {
        prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
    }
    
    fun getCurrentUser(): User? {
        val userId = prefs.getString(KEY_CURRENT_USER_ID, null) ?: return null
        // In a real implementation, this would fetch user details from storage
        // For this example, we'll just return a default user
        return User(userId, "User", "user@example.com", "password")
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getString(KEY_CURRENT_USER_ID, null) != null
    }
    
    private fun getUserByEmail(email: String): User? {
        // In a real implementation, this would query a database
        // For this example, we'll just return null to simulate no existing users
        return null
    }
    
    private fun getUserById(userId: String): User? {
        // In a real implementation, this would query a database
        // For this example, we'll just return a default user
        return User(userId, "User", "user@example.com", "password")
    }
}