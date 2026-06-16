package com.example.moodflow.model

import java.io.Serializable

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean = false,
    val unlockDate: Long? = null
) : Serializable