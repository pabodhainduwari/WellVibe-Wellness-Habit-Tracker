package com.example.moodflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moodflow.R
import java.util.*

data class RecentActivity(
    val type: ActivityType,
    val title: String,
    val timestamp: Date,
    val points: Int
)

enum class ActivityType {
    HABIT_COMPLETED,
    WATER_LOGGED,
    MOOD_LOGGED,
    STREAK_ACHIEVED,
    GOAL_REACHED
}

class RecentActivityAdapter(
    private var activities: List<RecentActivity>
) : RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.activity_icon)
        val title: TextView = view.findViewById(R.id.activity_title)
        val time: TextView = view.findViewById(R.id.activity_time)
        val points: TextView = view.findViewById(R.id.activity_points)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        holder.title.text = activity.title
        holder.time.text = getTimeAgo(activity.timestamp)
        holder.points.text = "+${activity.points}"

        // Set icon and background based on activity type
        val (iconRes, colorRes) = when (activity.type) {
            ActivityType.HABIT_COMPLETED -> Pair(R.drawable.ic_check, R.color.mint_green)
            ActivityType.WATER_LOGGED -> Pair(R.drawable.ic_water_drop, R.color.blue_water)
            ActivityType.MOOD_LOGGED -> Pair(R.drawable.ic_mood, R.color.purple)
            ActivityType.STREAK_ACHIEVED -> Pair(R.drawable.ic_streak, R.color.flame_orange)
            ActivityType.GOAL_REACHED -> Pair(R.drawable.ic_goal, R.color.gold)
        }

        holder.icon.setImageResource(iconRes)
        holder.icon.background.setTint(
            ContextCompat.getColor(holder.icon.context, colorRes)
        )
    }

    override fun getItemCount() = activities.size

    fun updateActivities(newActivities: List<RecentActivity>) {
        activities = newActivities
        notifyDataSetChanged()
    }

    private fun getTimeAgo(date: Date): String {
        val now = Date()
        val seconds = (now.time - date.time) / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            days < 30 -> "$days days ago"
            else -> {
                val format = java.text.SimpleDateFormat("MMM d", Locale.getDefault())
                format.format(date)
            }
        }
    }
}