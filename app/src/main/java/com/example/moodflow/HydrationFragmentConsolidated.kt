package com.example.moodflow

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.moodflow.adapters.IntakeHistoryAdapter
import com.example.moodflow.data.PreferencesHelper
import com.example.moodflow.views.WaterProgressRingView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HydrationFragmentConsolidated : Fragment() {
    
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1
    }
    
    private lateinit var preferencesHelper: PreferencesHelper
    private var currentProgress: Int = 0
    private var target: Int = 2000
    private var reminderInterval: Int = 2 // hours
    private var reminderTime: Calendar = Calendar.getInstance()
    private lateinit var waterProgressRing: WaterProgressRingView
    private var intakeHistoryAdapter: IntakeHistoryAdapter? = null
    private lateinit var rootView: View
    private var selectedFrequencyIndex: Int = 1 // Default to 2 hours (index 1)
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_hydration_consolidated, container, false)
        return rootView
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesHelper = PreferencesHelper(requireContext())
        val progressData = preferencesHelper.getHydrationProgress()
        currentProgress = progressData.first
        target = progressData.second
        reminderInterval = preferencesHelper.getUserSettings().notificationPreferences.hydrationFrequency
        
        // Initialize reminder time to 9 AM if not set
        val savedStartTime = preferencesHelper.getReminderStartTime()
        if (savedStartTime != null) {
            reminderTime.timeInMillis = savedStartTime.time
        } else {
            reminderTime.set(Calendar.HOUR_OF_DAY, 9)
            reminderTime.set(Calendar.MINUTE, 0)
            preferencesHelper.saveReminderStartTime(reminderTime.time)
        }
        
        // Initialize views first before calling any UI updates
        initViews()
        
        // Initialize reminder settings
        initReminderSettings()
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
        
        // Check for daily reset (now safe to call after initViews)
        checkAndResetDaily()
        
        scheduleHydrationReminder()
        updateIntakeHistory()
    }
    
    private fun initViews() {
        waterProgressRing = rootView.findViewById(R.id.water_progress_ring)
        val progressText = rootView.findViewById<TextView>(R.id.water_progress_text)
        val percentageText = rootView.findViewById<TextView>(R.id.percentage_text)
        
        updateProgressUI()
        
        // Initialize progress bar
        rootView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.daily_progress_bar)?.apply {
            max = target
            progress = currentProgress
            setProgressCompat(currentProgress, true)
        }
        
        // Set up quick add buttons
        val add250mlButton = rootView.findViewById<MaterialCardView>(R.id.add_250ml_button)
        add250mlButton.setOnClickListener {
            addWater(250)
        }
        
        val add500mlButton = rootView.findViewById<MaterialCardView>(R.id.add_500ml_button)
        add500mlButton.setOnClickListener {
            addWater(500)
        }
        
        val customAddButton = rootView.findViewById<MaterialCardView>(R.id.custom_add_button)
        customAddButton.setOnClickListener {
            showCustomWaterDialog()
        }
        
        // Add button press animations
        val cardButtons = listOf(add250mlButton, add500mlButton, customAddButton)
        cardButtons.forEach { card ->
            card.setOnTouchListener(View.OnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val pressAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.button_press)
                        view.startAnimation(pressAnim)
                    }
                    MotionEvent.ACTION_UP -> {
                        val releaseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.button_release)
                        view.startAnimation(releaseAnim)
                    }
                }
                false
            })
        }
        
        // Set target and reminder info
        val dailyGoalText = rootView.findViewById<TextView>(R.id.daily_goal_text)
        dailyGoalText.text = getString(R.string.water_goal, target)
        
        // Set up edit goal button
        val editGoalButton = rootView.findViewById<MaterialButton>(R.id.edit_goal_button)
        editGoalButton.setOnClickListener {
            showGoalSettingDialog()
        }
        
        // Set up reset button
        val resetButton = rootView.findViewById<MaterialButton>(R.id.reset_button)
        resetButton.setOnClickListener {
            showResetConfirmationDialog()
        }
        
        rootView.findViewById<TextView>(R.id.reminder_frequency_text).text = getString(R.string.reminder_frequency, reminderInterval)
        
        // Set up reminder time button
        val reminderTimeButton = rootView.findViewById<MaterialButton>(R.id.reminder_time_button)
        reminderTimeButton.setOnClickListener {
            showTimePicker()
        }
        
        // Set up change frequency button
        val changeFrequencyButton = rootView.findViewById<MaterialButton>(R.id.change_frequency_button)
        changeFrequencyButton.setOnClickListener {
            showFrequencyDialog()
        }
        
        // Set up reminder switch
        val reminderSwitch = rootView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.reminder_switch)
        reminderSwitch.isChecked = preferencesHelper.getUserSettings().notificationPreferences.hydrationEnabled
        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            val settings = preferencesHelper.getUserSettings()
            settings.notificationPreferences.hydrationEnabled = isChecked
            preferencesHelper.saveUserSettings(settings)
            if (isChecked) {
                scheduleHydrationReminder()
            } else {
                cancelExistingReminder()
            }
        }
        
        updateTimeDisplay()
    }
    
    private fun updateProgressUI() {
        val progressText = rootView.findViewById<TextView>(R.id.water_progress_text)
        val percentageText = rootView.findViewById<TextView>(R.id.percentage_text)
        val dailyGoalText = rootView.findViewById<TextView>(R.id.daily_goal_text)
        val progressBar = rootView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.daily_progress_bar)
        
        progressText.text = getString(R.string.water_progress, currentProgress, target)
        
        val percentage = if (target > 0) (currentProgress.toDouble() / target.toDouble() * 100).toInt() else 0
        percentageText.text = getString(R.string.water_percentage, percentage)
        
        // Update progress bar
        progressBar?.apply {
            max = target
            setProgressCompat(currentProgress, true)
        }
        
        // Update the water progress ring with animation - check if initialized
        if (::waterProgressRing.isInitialized) {
            waterProgressRing.setProgress(currentProgress.toFloat(), target.toFloat())
        }
        
        // Add animation to progress text
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        progressText.startAnimation(fadeIn)
        percentageText.startAnimation(fadeIn)
        
        // Update goal text and style based on progress
        dailyGoalText.text = getString(R.string.water_goal, target)
        if (currentProgress >= target) {
            dailyGoalText.setTextColor(resources.getColor(R.color.success, null))
            if (::waterProgressRing.isInitialized) {
                val pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
                waterProgressRing.startAnimation(pulseAnim)
            }
        } else {
            dailyGoalText.setTextColor(resources.getColor(R.color.text_primary, null))
        }
        
        // Update intake history
        updateIntakeHistory()
    }
    
    private fun updateIntakeHistory() {
        val records = preferencesHelper.getWaterIntakeRecords(7) // Last 7 days
        if (intakeHistoryAdapter == null) {
            intakeHistoryAdapter = IntakeHistoryAdapter(records)
            val recyclerView = rootView.findViewById<RecyclerView>(R.id.intake_history_recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = intakeHistoryAdapter
        } else {
            // Update existing adapter data
            intakeHistoryAdapter?.updateData(records)
        }
    }
    
    private fun addWater(amount: Int) {
        val wasBeforeGoal = currentProgress < target
        currentProgress += amount
        if (currentProgress > target) {
            currentProgress = target
        }
        
        // Save to preferences
        preferencesHelper.saveHydrationProgress(currentProgress, target)
        preferencesHelper.appendWaterIntake(amount, target, "Water")
        
        // Update UI
        updateProgressUI()
        
        // Show toast
        Toast.makeText(requireContext(), getString(R.string.water_added, amount), Toast.LENGTH_SHORT).show()
        
        // Show goal completion notification if we just reached the goal
        if (wasBeforeGoal && currentProgress >= target) {
            showGoalCompletionNotification()
        }
    }
    
    private fun resetProgress() {
        currentProgress = 0
        preferencesHelper.saveHydrationProgress(currentProgress, target)
        preferencesHelper.saveLastResetDate(Date())
        preferencesHelper.clearWaterIntakeRecords()
        updateProgressUI()
        updateIntakeHistory()
        Toast.makeText(requireContext(), "Progress and intake history reset for today", Toast.LENGTH_SHORT).show()
    }
    
    private fun showResetConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Progress")
            .setMessage("Are you sure you want to reset your water intake progress and clear all recent intake history?")
            .setPositiveButton("Reset") { _, _ ->
                resetProgress()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showGoalCompletionNotification() {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        // Create notification channel for Android O and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "hydration_goal",
                "Hydration Goal",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for hydration goal completion"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Build the notification
        val builder = android.app.Notification.Builder(
            requireContext(),
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) "hydration_goal" else ""
        ).apply {
            setSmallIcon(R.drawable.ic_water_drop)
            setContentTitle("Daily Water Goal Achieved!")
            setContentText("Congratulations! You've reached your daily water intake goal of ${target}ml.")
            setAutoCancel(true)
            
            // Add intent to open the hydration fragment when notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setContentIntent(pendingIntent)
            
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                setPriority(android.app.Notification.PRIORITY_DEFAULT)
            }
        }
        
        // Show the notification
        notificationManager.notify(1001, builder.build())
    }
    
    private fun showCustomWaterDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_water_intake)
        
        val waterInput = dialog.findViewById<TextInputEditText>(R.id.water_input)
        val chipGroup = dialog.findViewById<ChipGroup>(R.id.quick_amount_chips)
        
        // Set up quick amount chips
        chipGroup.setOnCheckedChangeListener { _: ChipGroup, checkedId: Int ->
            when (checkedId) {
                R.id.amount_100 -> waterInput.setText("100")
                R.id.amount_200 -> waterInput.setText("200")
                R.id.amount_300 -> waterInput.setText("300")
            }
        }
        
        dialog.findViewById<MaterialButton>(R.id.add_button).setOnClickListener {
            val amountStr = waterInput.text.toString().trim()
            if (amountStr.isNotEmpty()) {
                try {
                    val amount = amountStr.toInt()
                    if (amount > 0) {
                        addWater(amount)
                        dialog.dismiss()
                    } else {
                        waterInput.error = "Please enter a valid amount"
                    }
                } catch (e: NumberFormatException) {
                    waterInput.error = "Please enter a valid number"
                }
            }
        }
        
        dialog.findViewById<MaterialButton>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
    
    private fun checkAndResetDaily() {
        val lastResetDate = preferencesHelper.getLastResetDate()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        if (lastResetDate == null || lastResetDate.before(today)) {
            currentProgress = 0
            preferencesHelper.saveHydrationProgress(currentProgress, target)
            preferencesHelper.saveLastResetDate(today)
            updateProgressUI()
        }
    }
    
    private fun showTimePicker() {
        val currentTime = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            val newReminderTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // If the selected time is before current time, add a day
            if (newReminderTime.before(currentTime)) {
                newReminderTime.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            reminderTime = newReminderTime
            updateTimeDisplay()
            scheduleHydrationReminder() // Reschedule with new time
        }
        
        TimePickerDialog(
            requireContext(),
            
            timeSetListener,
            reminderTime.get(Calendar.HOUR_OF_DAY),
            reminderTime.get(Calendar.MINUTE),
            true
        ).apply {
            setTitle("Set Reminder Time")
            show()
        }
    }
    
    private fun cancelExistingReminder() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        rootView.findViewById<TextView>(R.id.next_reminder_text).text = getString(
            R.string.next_reminder,
            timeFormat.format(reminderTime.time)
        )
        
        // Update frequency text
        rootView.findViewById<TextView>(R.id.reminder_frequency_text).text = 
            "Every $reminderInterval " + if (reminderInterval == 1) "hour" else "hours"
    }
    
    private fun showFrequencyDialog() {
        val frequencies = arrayOf("1 hour", "2 hours", "3 hours", "4 hours", "6 hours")
        val values = arrayOf(1, 2, 3, 4, 6)
        selectedFrequencyIndex = values.indexOf(reminderInterval).takeIf { it != -1 } ?: 1

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reminder Frequency")
            .setSingleChoiceItems(frequencies, selectedFrequencyIndex) { dialogInterface, which ->
                reminderInterval = values[which]
                val settings = preferencesHelper.getUserSettings()
                settings.notificationPreferences.hydrationFrequency = reminderInterval
                preferencesHelper.saveUserSettings(settings)
                updateTimeDisplay()
                scheduleHydrationReminder()
                dialogInterface.dismiss()
                Toast.makeText(requireContext(), "Reminder frequency updated", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun initReminderSettings() {
        // These listeners are already set up in initViews, so we don't need to duplicate them here
    }
    
    private fun scheduleHydrationReminder() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get current time and set the reminder time for today
        val now = Calendar.getInstance()
        val scheduledTime = Calendar.getInstance()
        scheduledTime.set(Calendar.HOUR_OF_DAY, reminderTime.get(Calendar.HOUR_OF_DAY))
        scheduledTime.set(Calendar.MINUTE, reminderTime.get(Calendar.MINUTE))
        scheduledTime.set(Calendar.SECOND, 0)
        scheduledTime.set(Calendar.MILLISECOND, 0)
        
        // If the scheduled time is already past for today, set it for tomorrow
        if (scheduledTime.before(now)) {
            scheduledTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Convert reminder interval to milliseconds
        val intervalMillis = reminderInterval * 60 * 60 * 1000L // hours to milliseconds
        
        try {
            // Cancel any existing alarms first
            alarmManager.cancel(pendingIntent)
            
            // For Android 12 (API 31) and above, we need to check for exact alarm permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduledTime.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact repeating alarm
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        scheduledTime.timeInMillis,
                        intervalMillis,
                        pendingIntent
                    )
                }
            } else {
                // For older Android versions, use setAlarmClock for high-priority exact alarms
                val showIntent = Intent(requireContext(), MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    requireContext(),
                    0,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(scheduledTime.timeInMillis, showPendingIntent),
                    pendingIntent
                )
            }
            
            // Save the scheduled time
            preferencesHelper.saveReminderStartTime(scheduledTime.time)
            
            // Show confirmation toast
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            Toast.makeText(
                requireContext(),
                "Reminder set for ${timeFormat.format(scheduledTime.time)}",
                Toast.LENGTH_SHORT
            ).show()
            
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Could not schedule alarm. Please check app permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun showGoalSettingDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_goal_setting)
        
        val goalInput = dialog.findViewById<TextInputEditText>(R.id.goal_input)
        val chipGroup = dialog.findViewById<ChipGroup>(R.id.quick_goal_chips)
        val saveButton = dialog.findViewById<MaterialButton>(R.id.save_button)
        val cancelButton = dialog.findViewById<MaterialButton>(R.id.cancel_button)
        
        // Set current goal
        goalInput.setText(target.toString())
        
        // Set up quick goal chips
        chipGroup.setOnCheckedChangeListener { _: ChipGroup, checkedId: Int ->
            when (checkedId) {
                R.id.goal_2000 -> goalInput.setText("2000")
                R.id.goal_2500 -> goalInput.setText("2500")
                R.id.goal_3000 -> goalInput.setText("3000")
            }
        }
        
        saveButton.setOnClickListener {
            val goalStr = goalInput.text.toString().trim()
            if (goalStr.isNotEmpty()) {
                try {
                    val newGoal = goalStr.toInt()
                    if (newGoal > 0) {
                        updateDailyGoal(newGoal)
                        dialog.dismiss()
                    } else {
                        goalInput.error = "Please enter a valid goal"
                    }
                } catch (e: NumberFormatException) {
                    goalInput.error = "Please enter a valid number"
                }
            }
        }
        
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        
        dialog.show()
    }
    
    private fun updateDailyGoal(newGoal: Int) {
        target = newGoal
        preferencesHelper.saveHydrationProgress(currentProgress, target)
        updateProgressUI()
        rootView.findViewById<TextView>(R.id.daily_goal_text).text = getString(R.string.water_goal, target)
        Toast.makeText(requireContext(), "Daily goal updated to ${target}ml", Toast.LENGTH_SHORT).show()
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, reschedule reminder
                    scheduleHydrationReminder()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Notification permission is required for reminders",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}