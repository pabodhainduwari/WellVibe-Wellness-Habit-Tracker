package com.example.moodflow

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodflow.adapters.IntakeHistoryAdapter
import com.example.moodflow.data.PreferencesHelper
import com.example.moodflow.views.WaterProgressRingView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HydrationActivity : AppCompatActivity() {
    
    private lateinit var preferencesHelper: PreferencesHelper
    private var currentProgress: Int = 0
    private var target: Int = 2000
    private var reminderInterval: Int = 2 // hours
    private var reminderTime: Calendar = Calendar.getInstance()
    private lateinit var waterProgressRing: WaterProgressRingView
    private lateinit var intakeHistoryAdapter: IntakeHistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_hydration_consolidated)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        preferencesHelper = PreferencesHelper(this)
        val progressData = preferencesHelper.getHydrationProgress()
        currentProgress = progressData.first
        target = progressData.second
        
        // Initialize reminder time to 9 AM
        reminderTime.set(Calendar.HOUR_OF_DAY, 9)
        reminderTime.set(Calendar.MINUTE, 0)
        
        initViews()
        setupBottomNavigation()
        scheduleHydrationReminder()
        updateIntakeHistory()
    }
    
    private fun initViews() {
        waterProgressRing = findViewById(R.id.water_progress_ring)
        val progressText = findViewById<TextView>(R.id.water_progress_text)
        val percentageText = findViewById<TextView>(R.id.percentage_text)
        
        updateProgressUI()
        
        // Set up quick add buttons
        val add250mlButton = findViewById<Button>(R.id.add_250ml_button)
        add250mlButton.text = "+ 250ml"
        add250mlButton.setOnClickListener {
            addWater(250)
        }
        
        val add500mlButton = findViewById<Button>(R.id.add_500ml_button)
        add500mlButton.text = "+ 500ml"
        add500mlButton.setOnClickListener {
            addWater(500)
        }
        
        val customAddButton = findViewById<Button>(R.id.custom_add_button)
        customAddButton.text = "Custom"
        customAddButton.setOnClickListener {
            showCustomWaterDialog()
        }
        
        // Add button press animations
        val buttons = listOf(add250mlButton, add500mlButton, customAddButton)
        buttons.forEach { button ->
            button.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val pressAnim = AnimationUtils.loadAnimation(this, R.anim.button_press)
                        view.startAnimation(pressAnim)
                    }
                    MotionEvent.ACTION_UP -> {
                        val releaseAnim = AnimationUtils.loadAnimation(this, R.anim.button_release)
                        view.startAnimation(releaseAnim)
                    }
                }
                false
            }
        }
        
        // Set target and reminder info
        findViewById<TextView>(R.id.daily_goal_text).text = getString(R.string.water_goal, target)
        findViewById<TextView>(R.id.reminder_frequency_text).text = getString(R.string.reminder_frequency, reminderInterval)
        
        // Set up reminder time button
        val reminderTimeButton = findViewById<Button>(R.id.reminder_time_button)
        reminderTimeButton.setOnClickListener {
            showTimePicker()
        }
        
        updateTimeDisplay()
        
        // Add weekly progress chart
        // addWeeklyProgressChart() - Removed as per user request
        
        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        // Clear the back stack to avoid returning to this activity
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.nav_habits -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        // Pass an extra to indicate which fragment to load
                        putExtra("fragment", "habits")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.nav_mood -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        // Pass an extra to indicate which fragment to load
                        putExtra("fragment", "mood")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        // Pass an extra to indicate which fragment to load
                        putExtra("fragment", "settings")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                else -> false
            }
        }
        // Set hydration as selected (since we're in the hydration activity)
        bottomNavigation.selectedItemId = R.id.nav_habits
    }
    
    private fun updateProgressUI() {
        val progressText = findViewById<TextView>(R.id.water_progress_text)
        val percentageText = findViewById<TextView>(R.id.percentage_text)
        
        progressText.text = getString(R.string.water_progress, currentProgress, target)
        
        val percentage = if (target > 0) (currentProgress.toDouble() / target.toDouble() * 100).toInt() else 0
        percentageText.text = getString(R.string.water_percentage, percentage)
        
        // Update the water progress ring
        waterProgressRing.setProgress(currentProgress.toFloat(), target.toFloat())
        
        // Add animation to progress text
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        progressText.startAnimation(fadeIn)
        percentageText.startAnimation(fadeIn)
        
        // Update intake history
        updateIntakeHistory()
    }
    
    private fun updateIntakeHistory() {
        val records = preferencesHelper.getWaterIntakeRecords(7) // Last 7 days
        intakeHistoryAdapter = IntakeHistoryAdapter(records)
        
        val recyclerView = findViewById<RecyclerView>(R.id.intake_history_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = intakeHistoryAdapter
    }
    
    private fun addWater(amount: Int) {
        currentProgress += amount
        if (currentProgress > target) {
            currentProgress = target
        }
        
        // Save to preferences
        preferencesHelper.saveHydrationProgress(currentProgress, target)
        
        // Update UI
        updateProgressUI()
        
        // Show toast
        Toast.makeText(this, getString(R.string.water_added, amount), Toast.LENGTH_SHORT).show()
    }
    
    private fun showCustomWaterDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Add Custom Water Intake")
        
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter amount in ml"
        builder.setView(input)
        
        builder.setPositiveButton("Add") { _, _ ->
            val amountStr = input.text.toString().trim()
            if (amountStr.isNotEmpty()) {
                try {
                    val amount = amountStr.toInt()
                    if (amount > 0) {
                        addWater(amount)
                    } else {
                        Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        
        builder.show()
    }
    
    private fun showTimePicker() {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            reminderTime.set(Calendar.HOUR_OF_DAY, hour)
            reminderTime.set(Calendar.MINUTE, minute)
            updateTimeDisplay()
            scheduleHydrationReminder() // Reschedule with new time
        }
        
        TimePickerDialog(
            this,
            timeSetListener,
            reminderTime.get(Calendar.HOUR_OF_DAY),
            reminderTime.get(Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        findViewById<TextView>(R.id.next_reminder_text).text = getString(
            R.string.next_reminder,
            timeFormat.format(reminderTime.time)
        )
    }
    
    private fun scheduleHydrationReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule repeating alarm
        val intervalMillis = reminderInterval * 60 * 60 * 1000L // Convert hours to milliseconds
        
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            reminderTime.timeInMillis,
            intervalMillis,
            pendingIntent
        )
    }
}