package com.example.moodflow

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.moodflow.model.Habit
import com.example.moodflow.model.UserProfile
import com.example.moodflow.HabitProgressWidget
import com.example.moodflow.views.FlameView
import com.example.moodflow.views.QuickEntryBottomSheet
import com.example.moodflow.views.ProgressRingView
import com.example.moodflow.views.WaveView
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : BaseFragment() {
    
    // Motivational quotes array
    private val motivationalQuotes = arrayOf(
        "\"The best time to plant a tree was 20 years ago. The second best time is now.\"",
        "\"Small daily improvements are the key to staggering long-term results.\"",
        "\"Don't wish it were easier. Wish you were better.\"",
        "\"The only bad workout is the one that didn't happen.\"",
        "\"Success is the sum of small efforts repeated day in and day out.\"",
        "\"The difference between ordinary and extraordinary is that little extra.\"",
        "\"Your future self is counting on you.\"",
        "\"Consistency is the key to success.\""
    )
    
    override fun getLayoutId(): Int {
        return R.layout.fragment_dashboard
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setupToolbar(view)
        return view
    }
    
    override fun setupViews(view: View) {
        setupHeader(view)
        setupProgressRing(view)
        setupQuickStats(view)
        setupActiveHabits(view)
        setupCompletedHabits(view)
        setupQuickActions(view)
        setupMotivationalQuote(view)
        updateHabitProgressWidget()
    }
    
    private fun setupToolbar(view: View?) {
        // In a real app, you would set up the toolbar here
        // For now, we'll just leave it as is
    }
    
    private fun setupHeader(view: View) {
        val userProfile = preferencesHelper.getUserProfile()
        if (userProfile.name == "User") {
            preferencesHelper.saveUserProfile(UserProfile("Sarah", "sarah@example.com"))
        }
        
        val greetingTextView = view.findViewById<TextView>(R.id.greeting_text)
        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        greetingTextView.text = "$greeting, ${userProfile.name}!"
        
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        view.findViewById<TextView>(R.id.date_text).text = dateFormat.format(Date())
    }
    
    private fun setupMotivationalQuote(view: View) {
        val quoteTextView = view.findViewById<TextView>(R.id.motivational_quote)
        val random = Random()
        val quote = motivationalQuotes[random.nextInt(motivationalQuotes.size)]
        quoteTextView.text = quote
    }
    
    private fun setupProgressRing(view: View) {
        val progressRing = view.findViewById<ProgressRingView>(R.id.progress_ring)
        val habits = preferencesHelper.getHabits()
        val dailyCompletion = calculateDailyCompletion(habits)
        preferencesHelper.saveDailyCompletion(dailyCompletion)
        
        progressRing.setProgress(dailyCompletion / 100f, true)
        view.findViewById<TextView>(R.id.progress_text).text = "$dailyCompletion%"
        
        // Update habits completed text
        val completedHabits = habits.count { it.completed && isSameDay(it.lastCompletedDate, Date()) }
        view.findViewById<TextView>(R.id.habits_completed_text).text = "$completedHabits of ${habits.size}"
    }
    
    private fun setupQuickStats(view: View) {
        // Water Stats
        val waterWave = view.findViewById<WaveView>(R.id.water_wave)
        val hydration = preferencesHelper.getHydrationProgress()
        val waterProgress = hydration.first.toFloat() / hydration.second.toFloat()
        waterWave.setFillLevel(waterProgress)
        view.findViewById<TextView>(R.id.water_text).text = "${hydration.first}/${hydration.second} ml"
        
        // Mood Stats
        val moodEntries = preferencesHelper.getMoodEntries()
        val latestMood = moodEntries.maxByOrNull { it.date }
        val moodTrend = view.findViewById<ImageView>(R.id.mood_trend)
        if (moodEntries.size >= 2) {
            val previousMood = moodEntries.sortedByDescending { it.date }[1]
            moodTrend.setImageResource(
                when {
                    latestMood?.moodScore ?: 3 > previousMood.moodScore -> R.drawable.ic_trend_up
                    latestMood?.moodScore ?: 3 < previousMood.moodScore -> R.drawable.ic_trend_down
                    else -> R.drawable.ic_trend_flat
                }
            )
        }
        view.findViewById<TextView>(R.id.mood_emoji).text = latestMood?.mood ?: "😊"
        view.findViewById<TextView>(R.id.mood_text).text = "Feeling ${latestMood?.mood?.lowercase() ?: "happy"}"
        
        // Streak Stats
        val streakFlame = view.findViewById<FlameView>(R.id.streak_flame)
        val currentStreak = preferencesHelper.getStreak()
        view.findViewById<TextView>(R.id.streak_text).text = "$currentStreak days"
        
        // Find best streak from habits
        val allHabits = preferencesHelper.getHabits()
        val bestStreak = allHabits.maxOfOrNull { it.streak } ?: 0
        view.findViewById<TextView>(R.id.streak_best).text = "$bestStreak days"
        
        // Set up card click listeners
        setupCardClickListeners(view)
    }
    
    private fun setupCardClickListeners(view: View) {
        val waterCard = view.findViewById<MaterialCardView>(R.id.water_card)
        val moodCard = view.findViewById<MaterialCardView>(R.id.mood_card)
        
        // Animate cards sequentially
        val slideInRight = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
        waterCard.startAnimation(slideInRight)
        
        val slideInRightDelayed = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
        slideInRightDelayed.startOffset = 100
        moodCard.startAnimation(slideInRightDelayed)
        
        waterCard.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.example.moodflow.HydrationFragmentConsolidated())
        }
        
        moodCard.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.example.moodflow.MoodJournalFragment())
        }
        
        // Set up view all buttons
        val viewAllHabitsButton = view.findViewById<MaterialButton>(R.id.view_all_habits_button)
        val viewAllCompletedButton = view.findViewById<MaterialButton>(R.id.view_all_completed_button)
        
        viewAllHabitsButton.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.example.moodflow.HabitsFragment())
        }
        
        viewAllCompletedButton.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.example.moodflow.HabitsFragment())
        }
    }
    
    private fun setupQuickActions(view: View) {
        val logWaterButton = view.findViewById<MaterialButton>(R.id.log_water_button)
        val addMoodButton = view.findViewById<MaterialButton>(R.id.add_mood_button)
        
        logWaterButton.setOnClickListener {
            // For water, we'll keep the existing behavior for now
            (activity as? MainActivity)?.loadFragment(com.example.moodflow.HydrationFragmentConsolidated())
        }
        
        addMoodButton.setOnClickListener {
            // Use the new quick entry bottom sheet for mood
            val bottomSheet = QuickEntryBottomSheet.newInstance("mood")
            bottomSheet.show(parentFragmentManager, "quick_mood_entry")
        }
    }

    private fun setupActiveHabits(view: View) {
        val card = view.findViewById<MaterialCardView>(R.id.active_habits_card)
        val chipGroup = view.findViewById<ChipGroup>(R.id.active_habits_group)
        val emptyView = view.findViewById<TextView>(R.id.active_habits_empty)
        val subtitle = view.findViewById<TextView>(R.id.active_habits_subtitle)

        if (card == null || chipGroup == null || emptyView == null) {
            return
        }

        val habits = preferencesHelper.getHabits()
        val today = Date()
        val activeHabits = habits.filterNot { habit ->
            habit.completed && isSameDay(habit.lastCompletedDate, today)
        }

        chipGroup.removeAllViews()

        if (activeHabits.isEmpty()) {
            chipGroup.isVisible = false
            emptyView.isVisible = true
            subtitle?.text = "Nothing queued—add something inspiring."
            card.isVisible = true
            return
        }

        chipGroup.isVisible = true
        emptyView.isVisible = false

        subtitle?.text = if (activeHabits.size == 1) {
            "One small win awaits."
        } else {
            "Stay consistent by tackling these next."
        }

        activeHabits.take(6).forEach { habit ->
            val baseColor = runCatching { Color.parseColor(habit.color) }
                .getOrElse { ContextCompat.getColor(requireContext(), R.color.primary) }
            val backgroundColor = ColorUtils.setAlphaComponent(baseColor, (255 * 0.18f).toInt())
            val chip = Chip(requireContext()).apply {
                text = if (habit.target > 0) {
                    "${habit.name}  •  ${habit.progress}/${habit.target}"
                } else {
                    habit.name
                }
                isCheckable = false
                isClickable = false
                setEnsureMinTouchTargetSize(false)
                chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
                chipStrokeColor = ColorStateList.valueOf(baseColor)
                chipStrokeWidth = 2f
                val textColor = if (ColorUtils.calculateLuminance(backgroundColor) < 0.65) {
                    Color.WHITE
                } else {
                    ContextCompat.getColor(requireContext(), R.color.text_primary)
                }
                setTextColor(textColor)
                alpha = 0.92f
            }
            chipGroup.addView(chip)
        }

        card.isVisible = true
    }

    private fun setupCompletedHabits(view: View) {
        val card = view.findViewById<MaterialCardView>(R.id.completed_habits_card)
        val chipGroup = view.findViewById<ChipGroup>(R.id.completed_habits_group)
        val emptyView = view.findViewById<TextView>(R.id.completed_habits_empty)

        if (card == null || chipGroup == null || emptyView == null) {
            return
        }

        val completedHabits = preferencesHelper.getHabits()
            .filter { it.completed && isSameDay(it.lastCompletedDate, Date()) }
            .sortedByDescending { it.lastCompletedDate }

        chipGroup.removeAllViews()

        if (completedHabits.isEmpty()) {
            chipGroup.isVisible = false
            emptyView.isVisible = true
            card.isVisible = true
            return
        }

        chipGroup.isVisible = true
        emptyView.isVisible = false

        completedHabits.forEach { habit ->
            val chip = Chip(requireContext()).apply {
                text = "✓ ${habit.name}"
                isCheckable = false
                isClickable = false
                setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2)

                val parsedColor = runCatching { Color.parseColor(habit.color) }.getOrElse {
                    ContextCompat.getColor(requireContext(), R.color.primary)
                }

                chipBackgroundColor = ColorStateList.valueOf(parsedColor)

                val textColor = if (ColorUtils.calculateLuminance(parsedColor) < 0.5) {
                    Color.WHITE
                } else {
                    ContextCompat.getColor(requireContext(), R.color.text_primary)
                }
                setTextColor(textColor)
                chipStrokeWidth = 0f
            }

            chipGroup.addView(chip)
        }

        card.isVisible = true
    }

    private fun isSameDay(date: Date?, reference: Date): Boolean {
        if (date == null) return false

        val calendar1 = Calendar.getInstance().apply { time = date }
        val calendar2 = Calendar.getInstance().apply { time = reference }

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
            calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun calculateDailyCompletion(habits: List<Habit>): Int {
        if (habits.isEmpty()) return 0
        
        val completed = habits.sumOf { habit ->
            when {
                habit.completed -> habit.target
                else -> habit.progress
            }
        }
        
        val totalTargets = habits.sumOf { it.target }
        return if (totalTargets == 0) 0 else (completed.toDouble() / totalTargets * 100).toInt()
    }
    
    private fun updateHabitProgressWidget() {
        val intent = Intent(context, HabitProgressWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context)
            ?.getAppWidgetIds(ComponentName(requireContext(), HabitProgressWidget::class.java))
        ids?.let {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, it)
            context?.sendBroadcast(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh the dashboard when returning to it
        view?.let { setupViews(it) }
    }
}