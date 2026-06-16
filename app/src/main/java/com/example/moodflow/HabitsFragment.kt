package com.example.moodflow

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodflow.model.Habit
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import com.example.moodflow.views.QuickEntryBottomSheet
import java.util.*
import kotlin.math.max

class HabitsFragment : BaseFragment() {
    
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var habits: MutableList<Habit>
    private var isBulkMode = false
    private val selectedHabits = mutableSetOf<Habit>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var calendarView: CalendarView
    private lateinit var calendarHabitsRecyclerView: RecyclerView
    private var isCalendarView = false
    private var selectedDate: Date = Date()
    
    // Two-pane layout components
    private var habitDetailPane: LinearLayout? = null
    private var habitDetailTitle: TextView? = null
    private var habitDetailDescription: TextView? = null
    private var habitDetailProgress: ProgressBar? = null
    private var habitDetailProgressText: TextView? = null
    private var habitDetailStreak: TextView? = null
    private var habitDetailToggleButton: Button? = null
    private var habitDetailEditButton: Button? = null
    private var habitDetailDeleteButton: Button? = null
    private var selectedHabit: Habit? = null
    
    // Daily summary and empty state
    private lateinit var dailyProgressBar: ProgressBar
    private lateinit var dailyProgressValue: TextView
    private lateinit var dailyProgressPercentage: TextView
    private lateinit var dailyProgressSubtitle: TextView
    private lateinit var habitsEmptyState: LinearLayout
    
    override fun getLayoutId(): Int {
        return R.layout.fragment_habits
    }
    
    override fun setupViews(view: View) {
        habits = preferencesHelper.getHabits().toMutableList()
        resetHabitsForNewDay()
        
        dailyProgressBar = view.findViewById(R.id.daily_progress_bar)
        dailyProgressValue = view.findViewById(R.id.daily_progress_value)
        dailyProgressPercentage = view.findViewById(R.id.daily_progress_percentage)
        dailyProgressSubtitle = view.findViewById(R.id.daily_progress_subtitle)
        habitsEmptyState = view.findViewById<LinearLayout>(R.id.habits_empty_state)
        
        recyclerView = view.findViewById(R.id.habits_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        habitAdapter = HabitAdapter(habits, { habit, position ->
            if (isBulkMode) {
                toggleHabitSelection(habit)
            } else {
                toggleHabitCompletion(habit, position)
                // On tablet, show habit details when clicked
                showHabitDetails(habit, view)
            }
        }) { anchor, habit, position ->
            showHabitOptions(anchor, habit, position)
        }
        
        recyclerView.adapter = habitAdapter
        
        // Calendar view components
        calendarView = view.findViewById(R.id.habits_calendar_view)
        calendarHabitsRecyclerView = view.findViewById(R.id.calendar_habits_recycler_view)
        calendarHabitsRecyclerView.layoutManager = LinearLayoutManager(context)
        
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            updateCalendarHabitsView()
        }
        
        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.add_habit_fab)
        fab.setOnClickListener {
            showHabitBottomSheet()
        }
        
        val bulkToggleButton = view.findViewById<Button>(R.id.bulk_toggle_button)
        bulkToggleButton.setOnClickListener {
            toggleBulkMode()
        }
        
        val viewToggleButton = view.findViewById<Button>(R.id.view_toggle_button)
        viewToggleButton.setOnClickListener {
            toggleView()
        }
        
        // Initialize two-pane components if they exist
        initializeTwoPaneComponents(view)
        
        updateCalendarHabitsView()
        updateEmptyState()
        updateDailySummary()
    }
    
    private fun initializeTwoPaneComponents(view: View) {
        habitDetailPane = view.findViewById(R.id.habit_detail_pane)
        habitDetailTitle = view.findViewById(R.id.habit_detail_title)
        habitDetailDescription = view.findViewById(R.id.habit_detail_description)
        habitDetailProgress = view.findViewById(R.id.habit_detail_progress)
        habitDetailProgressText = view.findViewById(R.id.habit_detail_progress_text)
        habitDetailStreak = view.findViewById(R.id.habit_detail_streak)
        habitDetailToggleButton = view.findViewById(R.id.habit_detail_toggle_button)
        habitDetailEditButton = view.findViewById(R.id.habit_detail_edit_button)
        habitDetailDeleteButton = view.findViewById(R.id.habit_detail_delete_button)
        
        // Set up detail pane button listeners
        habitDetailToggleButton?.setOnClickListener {
            selectedHabit?.let { habit ->
                val position = habits.indexOf(habit)
                if (position != -1) {
                    toggleHabitCompletion(habit, position)
                    updateHabitDetails(habit)
                }
            }
        }
        
        habitDetailEditButton?.setOnClickListener {
            selectedHabit?.let { habit ->
                val position = habits.indexOf(habit)
                if (position != -1) {
                    editHabit(habit, position)
                }
            }
        }
        
        habitDetailDeleteButton?.setOnClickListener {
            selectedHabit?.let { habit ->
                val position = habits.indexOf(habit)
                if (position != -1) {
                    deleteHabit(habit, position)
                    hideHabitDetails()
                }
            }
        }
    }
    
    private fun showHabitDetails(habit: Habit, view: View) {
        habitDetailPane?.visibility = View.VISIBLE
        selectedHabit = habit
        updateHabitDetails(habit)
    }
    
    private fun hideHabitDetails() {
        habitDetailPane?.visibility = View.GONE
        selectedHabit = null
    }
    
    private fun updateHabitDetails(habit: Habit) {
        habitDetailTitle?.text = habit.name
        habitDetailDescription?.text = habit.description.ifEmpty { "No description" }
        habitDetailProgress?.progress = habit.completionPercentage
        habitDetailProgressText?.text = "${habit.progress}/${habit.target} completed"
        habitDetailStreak?.text = "Current streak: ${habit.streak} days"
        habitDetailToggleButton?.text = if (habit.completed) "Mark as Incomplete" else "Mark as Completed"
    }
    
    private fun toggleView() {
        isCalendarView = !isCalendarView
        
        val listViewContainer = view?.findViewById<LinearLayout>(R.id.list_view_container)
        val calendarViewContainer = view?.findViewById<LinearLayout>(R.id.calendar_view_container)
        val viewToggleButton = view?.findViewById<Button>(R.id.view_toggle_button)
        
        if (isCalendarView) {
            listViewContainer?.visibility = View.GONE
            calendarViewContainer?.visibility = View.VISIBLE
            viewToggleButton?.text = "List View"
        } else {
            listViewContainer?.visibility = View.VISIBLE
            calendarViewContainer?.visibility = View.GONE
            viewToggleButton?.text = "Calendar View"
        }
    }
    
    private fun updateCalendarHabitsView() {
        // For now, we'll show all habits in the calendar view
        // In a more advanced implementation, we would filter by date
        val calendarHabitAdapter = HabitAdapter(habits, { habit, position ->
            toggleHabitCompletion(habit, position)
        }) { anchor, habit, position ->
            showHabitOptions(anchor, habit, position)
        }
        calendarHabitAdapter.updateSelectionState(isBulkMode, selectedHabits)
        calendarHabitsRecyclerView.adapter = calendarHabitAdapter
    }

    private fun showHabitOptions(anchor: View, habit: Habit, position: Int) {
        if (!isAdded) return
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_habit_options, popup.menu)
        val markItem = popup.menu.findItem(R.id.action_mark_complete)
        markItem.title = if (habit.completed) "Mark as Incomplete" else "Mark as Complete"
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_mark_complete -> {
                    toggleHabitCompletion(habit, position)
                    true
                }
                R.id.action_edit_habit -> {
                    editHabit(habit, position)
                    true
                }
                R.id.action_delete_habit -> {
                    deleteHabit(habit, position)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun resetHabitsForNewDay() {
        val today = Date()
        var updated = false
        habits.forEach { habit ->
            val completedToday = habit.lastCompletedDate?.let { isSameDay(it, today) } ?: false
            if (!completedToday && habit.completed) {
                habit.completed = false
                updated = true
            }
        }
        if (updated) {
            preferencesHelper.saveHabits(habits)
        }
    }

    private fun updateDailySummary() {
        val total = habits.size
        val today = Date()
        val completed = habits.count { it.completed && it.lastCompletedDate?.let { date -> isSameDay(date, today) } == true }
        val percentage = if (total > 0) (completed.toDouble() / total.toDouble() * 100).toInt() else 0
        dailyProgressBar.progress = percentage
        dailyProgressValue.text = "$completed of $total habits complete"
        dailyProgressPercentage.text = "$percentage%"
        dailyProgressSubtitle.text = when {
            total == 0 -> "Create habits to start tracking"
            percentage == 100 -> "You're on fire! Keep the streak alive."
            percentage >= 50 -> "Great momentum—just a little more to go."
            else -> "Tap a habit to mark it done."
        }
    }

    private fun updateEmptyState() {
        habitsEmptyState.visibility = if (habits.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun isSameDay(date: Date, comparison: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance().apply { time = comparison }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun pickColorForCategory(category: String): String {
        return when (category.lowercase(Locale.getDefault())) {
            "health" -> "#4CAF50"
            "wellness" -> "#26A69A"
            "fitness" -> "#0c3d39"
            "mindfulness" -> "#36205c"
            "productivity" -> "#4f1c22"
            "nutrition" -> "#FFA000"
            "learning" -> "#5C6BC0"
            "personal growth" -> "#AB47BC"
            "relationships" -> "#F06292"
            "finance" -> "#66BB6A"
            "home" -> "#8D6E63"
            "lifestyle" -> "#FFCA28"
            "general" -> "#3DDC84"
            else -> "#3DDC84"
        }
    }
    
    private fun toggleBulkMode() {
        isBulkMode = !isBulkMode
        selectedHabits.clear()
        val bulkToggleButton = view?.findViewById<Button>(R.id.bulk_toggle_button)
        bulkToggleButton?.text = if (isBulkMode) "Complete Selected" else "Bulk Toggle"
        habitAdapter.updateSelectionState(isBulkMode, selectedHabits)
        
        if (isBulkMode) {
            // Enter bulk mode
            Toast.makeText(context, "Select habits to toggle, then tap 'Complete Selected'", Toast.LENGTH_LONG).show()
        } else {
            // Exit bulk mode and apply changes
            if (selectedHabits.isNotEmpty()) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Complete Selected Habits")
                builder.setMessage("Mark ${selectedHabits.size} selected habits as completed?")
                
                builder.setPositiveButton("Complete") { _, _ ->
                    completeSelectedHabits()
                }
                
                builder.setNegativeButton("Cancel") { _, _ ->
                    selectedHabits.clear()
                    habitAdapter.updateSelectionState(isBulkMode, selectedHabits)
                }
                
                builder.show()
            }
        }
        habitAdapter.updateSelectionState(isBulkMode, selectedHabits)
    }
    
    private fun toggleHabitSelection(habit: Habit) {
        if (selectedHabits.contains(habit)) {
            selectedHabits.remove(habit)
        } else {
            selectedHabits.add(habit)
        }
        habitAdapter.updateSelectionState(isBulkMode, selectedHabits)
    }
    
    private fun completeSelectedHabits() {
        var completedCount = 0
        for (habit in selectedHabits) {
            if (!habit.completed) {
                habit.completed = true
                habit.progress++
                habit.streak++
                habit.lastCompletedDate = Date()
                completedCount++
            }
        }
        
        if (completedCount > 0) {
            // Update in preferences
            preferencesHelper.saveHabits(habits)
            
            // Update daily completion
            val dailyCompletion = calculateDailyCompletion(habits)
            preferencesHelper.saveDailyCompletion(dailyCompletion)
            updateDailySummary()
            
            // Update widget
            updateHabitProgressWidget()
            
            // Show toast
            Toast.makeText(context, "$completedCount habits completed!", Toast.LENGTH_SHORT).show()
            
            // Update detail pane if showing a selected habit
            selectedHabit?.let { updateHabitDetails(it) }
        }
        
        // Clear selection and exit bulk mode
        selectedHabits.clear()
        isBulkMode = false
        val bulkToggleButton = view?.findViewById<Button>(R.id.bulk_toggle_button)
        bulkToggleButton?.text = "Bulk Toggle"
        habitAdapter.updateSelectionState(isBulkMode, selectedHabits)
    }
    
    private fun toggleHabitCompletion(habit: Habit, position: Int) {
        habit.completed = !habit.completed
        if (habit.completed) {
            habit.progress++
            habit.streak++
            habit.lastCompletedDate = Date()
        } else {
            habit.progress = max(0, habit.progress - 1)
            habit.streak = max(0, habit.streak - 1)
            habit.lastCompletedDate = null
        }
        
        // Update in preferences
        preferencesHelper.saveHabits(habits)
        
        // Notify adapter of change
        habitAdapter.notifyItemChanged(position)
        
        // Update daily completion
        val dailyCompletion = calculateDailyCompletion(habits)
        preferencesHelper.saveDailyCompletion(dailyCompletion)
        updateDailySummary()
        
        // Update widget
        updateHabitProgressWidget()
        
        // Show toast
        val message = if (habit.completed) {
            "Habit completed! 🔥 ${habit.streak} day streak"
        } else {
            "Habit marked as incomplete"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showHabitBottomSheet(habitToEdit: Habit? = null, position: Int? = null) {
        // Use the full bottom sheet for both new and existing habits
        
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_habit, null)
        dialog.setContentView(sheetView)

        val title = sheetView.findViewById<TextView>(R.id.habit_sheet_title)
        val subtitle = sheetView.findViewById<TextView>(R.id.habit_sheet_subtitle)
        val badgeText = sheetView.findViewById<TextView>(R.id.habit_sheet_badge)
        val iconCard = sheetView.findViewById<MaterialCardView>(R.id.habit_icon_card)
        val iconView = sheetView.findViewById<ImageView>(R.id.habit_sheet_icon)
        val quickCategoryGroup = sheetView.findViewById<ChipGroup>(R.id.habit_quick_category_group)
        val quickFrequencyGroup = sheetView.findViewById<ChipGroup>(R.id.habit_quick_frequency_group)
        val quickTargetGroup = sheetView.findViewById<ChipGroup>(R.id.habit_quick_target_group)
        val nameLayout = sheetView.findViewById<TextInputLayout>(R.id.habit_name_layout)
        val nameInput = sheetView.findViewById<TextInputEditText>(R.id.habit_name_input)
        val descriptionInput = sheetView.findViewById<TextInputEditText>(R.id.habit_description_input)
        val targetInput = sheetView.findViewById<TextInputEditText>(R.id.habit_target_input)
        val categoryInput = sheetView.findViewById<AutoCompleteTextView>(R.id.habit_category_input)
        val frequencyInput = sheetView.findViewById<AutoCompleteTextView>(R.id.habit_frequency_input)
        val saveButton = sheetView.findViewById<MaterialButton>(R.id.habit_save_button)
        val cancelButton = sheetView.findViewById<MaterialButton>(R.id.habit_cancel_button)

        val categories = resources.getStringArray(R.array.habit_categories)
        val frequencies = resources.getStringArray(R.array.habit_frequencies)
        categoryInput.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories))
        frequencyInput.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, frequencies))

        val defaultAccentColor = ContextCompat.getColor(requireContext(), R.color.primary)

        fun resolveAccentColor(categoryName: String?): Int {
            val hex = when {
                habitToEdit != null && !habitToEdit.color.isNullOrBlank() && categoryName != null &&
                    categoryName.equals(habitToEdit.category, ignoreCase = true) -> habitToEdit.color
                !categoryName.isNullOrBlank() -> pickColorForCategory(categoryName)
                habitToEdit != null && !habitToEdit.color.isNullOrBlank() -> habitToEdit.color
                else -> pickColorForCategory("General")
            }
            return runCatching { Color.parseColor(hex) }.getOrElse { defaultAccentColor }
        }

        fun applyAccent(categoryName: String?) {
            iconCard?.setCardBackgroundColor(resolveAccentColor(categoryName))
            iconView?.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }

        fun styleSelectionChip(chip: Chip, checkedColorRes: Int, checkedTextColorRes: Int = R.color.white) {
            val checkedColor = ContextCompat.getColor(requireContext(), checkedColorRes)
            val checkedTextColor = ContextCompat.getColor(requireContext(), checkedTextColorRes)
            val defaultColor = ContextCompat.getColor(requireContext(), R.color.bg_secondary)
            val uncheckedTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            fun apply(isChecked: Boolean) {
                chip.chipBackgroundColor = ColorStateList.valueOf(if (isChecked) checkedColor else defaultColor)
                chip.setTextColor(if (isChecked) checkedTextColor else uncheckedTextColor)
            }
            chip.isCheckedIconVisible = false
            chip.setEnsureMinTouchTargetSize(false)
            apply(chip.isChecked)
            chip.setOnCheckedChangeListener { button, isChecked ->
                apply(isChecked)
            }
        }

        quickCategoryGroup?.let { group ->
            group.removeAllViews()
            categories.take(6).forEach { categoryName ->
                val colorInt = resolveAccentColor(categoryName)
                val chip = Chip(requireContext()).apply {
                    id = View.generateViewId()
                    text = categoryName
                    isCheckable = true
                    isClickable = true
                    isCheckedIconVisible = false
                    tag = categoryName
                    chipBackgroundColor = ColorStateList.valueOf(colorInt)
                    val textColor = if (ColorUtils.calculateLuminance(colorInt) < 0.55) {
                        ContextCompat.getColor(requireContext(), R.color.white)
                    } else {
                        ContextCompat.getColor(requireContext(), R.color.text_primary)
                    }
                    setTextColor(textColor)
                    alpha = 0.72f
                    setEnsureMinTouchTargetSize(false)
                }
                chip.setOnCheckedChangeListener { button, isChecked ->
                    (button as Chip).alpha = if (isChecked) 1f else 0.72f
                }
                chip.setOnClickListener {
                    categoryInput.setText(categoryName, false)
                    categoryInput.dismissDropDown()
                    group.check(chip.id)
                }
                group.addView(chip)
            }
        }

        quickTargetGroup?.let { group ->
            group.removeAllViews()
            listOf(1, 2, 3, 5, 10).forEach { value ->
                val chip = Chip(requireContext()).apply {
                    id = View.generateViewId()
                    text = if (value == 1) "Once" else "$value times"
                    isCheckable = true
                    isClickable = true
                    tag = value
                }
                chip.setOnClickListener {
                    targetInput.setText(value.toString())
                    targetInput.clearFocus()
                    group.check(chip.id)
                }
                styleSelectionChip(chip, R.color.primary)
                group.addView(chip)
            }
        }

        quickFrequencyGroup?.let { group ->
            group.removeAllViews()
            listOf("Daily", "Weekdays", "Weekends", "Weekly", "Monthly").forEach { option ->
                val chip = Chip(requireContext()).apply {
                    id = View.generateViewId()
                    text = option
                    isCheckable = true
                    isClickable = true
                    tag = option
                }
                chip.setOnClickListener {
                    frequencyInput.setText(option, false)
                    frequencyInput.dismissDropDown()
                    group.check(chip.id)
                }
                styleSelectionChip(chip, R.color.coral)
                group.addView(chip)
            }
        }

        val completedToday = habits.count { it.completed && it.lastCompletedDate?.let { date -> isSameDay(date, Date()) } == true }
        badgeText?.text = if (completedToday > 0) {
            "$completedToday completed today"
        } else {
            "Let's build your streak"
        }

        categoryInput.doOnTextChanged { text, _, _, _ ->
            val selected = text?.toString()?.trim()
            quickCategoryGroup?.children?.filterIsInstance<Chip>()?.forEach { chip ->
                val matches = chip.text.toString().equals(selected, ignoreCase = true)
                if (chip.isChecked != matches) {
                    chip.isChecked = matches
                }
            }
            applyAccent(selected)
        }

        targetInput.doOnTextChanged { text, _, _, _ ->
            val value = text?.toString()?.trim()?.toIntOrNull()
            quickTargetGroup?.children?.filterIsInstance<Chip>()?.forEach { chip ->
                val matches = (chip.tag as? Int) == value
                if (chip.isChecked != matches) {
                    chip.isChecked = matches
                }
            }
        }

        frequencyInput.doOnTextChanged { text, _, _, _ ->
            val value = text?.toString()?.trim()
            quickFrequencyGroup?.children?.filterIsInstance<Chip>()?.forEach { chip ->
                val matches = (chip.tag as? String)?.equals(value, ignoreCase = true) == true
                if (chip.isChecked != matches) {
                    chip.isChecked = matches
                }
            }
        }

        if (habitToEdit != null) {
            title.text = "Edit habit"
            subtitle?.text = "Tweak the details and keep your momentum strong."
            nameInput.setText(habitToEdit.name)
            descriptionInput.setText(habitToEdit.description)
            targetInput.setText(habitToEdit.target.toString())
            categoryInput.setText(habitToEdit.category, false)
            val frequencyLabel = habitToEdit.frequency.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            frequencyInput.setText(frequencyLabel, false)
            saveButton.text = "Save changes"
        } else {
            title.text = "Create new habit"
            subtitle?.text = "Design a routine you'll love to complete."
            frequencyInput.setText(frequencies.firstOrNull() ?: "Daily", false)
            targetInput.setText("1")
            saveButton.text = "Add habit"
            applyAccent(null)
        }

        applyAccent(categoryInput.text?.toString())

        cancelButton.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val name = nameInput.text?.toString()?.trim().orEmpty()
            val description = descriptionInput.text?.toString()?.trim().orEmpty()
            val target = targetInput.text?.toString()?.trim()?.toIntOrNull()?.takeIf { it > 0 } ?: 1
            val category = categoryInput.text?.toString()?.takeIf { it.isNotBlank() } ?: "General"
            val frequency = frequencyInput.text?.toString()?.takeIf { it.isNotBlank() } ?: "Daily"

            if (name.isEmpty()) {
                nameLayout.error = "Please enter a habit"
                return@setOnClickListener
            } else {
                nameLayout.error = null
            }

            if (habitToEdit != null && position != null) {
                habitToEdit.name = name
                habitToEdit.description = description
                habitToEdit.target = target
                habitToEdit.category = category
                habitToEdit.frequency = frequency.lowercase(Locale.getDefault())
                habitToEdit.color = pickColorForCategory(category)
                preferencesHelper.saveHabits(habits)
                habitAdapter.notifyItemChanged(position)
                if (selectedHabit?.id == habitToEdit.id) {
                    updateHabitDetails(habitToEdit)
                }
                Toast.makeText(context, "Habit updated", Toast.LENGTH_SHORT).show()
            } else {
                val newHabit = Habit(
                    id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                    name = name,
                    category = category,
                    description = description,
                    completed = false,
                    streak = 0,
                    progress = 0,
                    target = target,
                    frequency = frequency.lowercase(Locale.getDefault()),
                    reminderEnabled = false,
                    createdDate = Date(),
                    color = pickColorForCategory(category),
                    icon = "check"
                )
                habits.add(0, newHabit)
                preferencesHelper.saveHabits(habits)
                habitAdapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
                Toast.makeText(context, "Habit added", Toast.LENGTH_SHORT).show()
            }

            if (isCalendarView) {
                updateCalendarHabitsView()
            }

            updateEmptyState()
            val dailyCompletion = calculateDailyCompletion(habits)
            preferencesHelper.saveDailyCompletion(dailyCompletion)
            updateDailySummary()
            updateHabitProgressWidget()
            dialog.dismiss()
        }

        dialog.behavior.skipCollapsed = true
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        dialog.show()
    }

    private fun calculateDailyCompletion(habits: List<Habit>): Int {
        if (habits.isEmpty()) return 0
        val today = Date()
        val completedHabits = habits.count { it.completed && it.lastCompletedDate?.let { date -> isSameDay(date, today) } == true }
        return (completedHabits.toDouble() / habits.size.toDouble() * 100).toInt()
    }

    // Method to edit a habit (called from context menu)
    fun editHabit(habit: Habit, position: Int) {
        showHabitBottomSheet(habit, position)
    }

    // Method to delete a habit (called from context menu)
    fun deleteHabit(habit: Habit, position: Int) {
        if (position < 0 || position >= habits.size) {
            return  // Invalid position, exit safely
        }
        val deletedHabit = habit  // Use the passed habit instead of accessing by position
        habits.remove(habit)  // Remove by object instead of by position
        preferencesHelper.saveHabits(habits)
        habitAdapter.notifyItemRemoved(position)

        val dailyCompletion = calculateDailyCompletion(habits)
        preferencesHelper.saveDailyCompletion(dailyCompletion)
        updateDailySummary()
        updateEmptyState()
        updateHabitProgressWidget()
        if (isCalendarView) {
            updateCalendarHabitsView()
        }

        // Show undo snackbar
        val snackbar = Snackbar.make(
            if (isCalendarView) calendarHabitsRecyclerView else recyclerView,
            "Habit deleted",
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction("Undo") {
            habits.add(position, deletedHabit)
            preferencesHelper.saveHabits(habits)
            habitAdapter.notifyItemInserted(position)
            val newDailyCompletion = calculateDailyCompletion(habits)
            preferencesHelper.saveDailyCompletion(newDailyCompletion)
            updateDailySummary()
            updateEmptyState()
            updateHabitProgressWidget()
            if (isCalendarView) {
                updateCalendarHabitsView()
            }
        }
        snackbar.show()
    }
    
    // Update the habit progress widget
    private fun updateHabitProgressWidget() {
        val intent = Intent(context, HabitProgressWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(requireContext(), HabitProgressWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context?.sendBroadcast(intent)
    }
    
    // Getter for bulk mode status
    fun isBulkModeActive(): Boolean = isBulkMode
    
    // Getter for selected habits
    fun getSelectedHabits(): Set<Habit> = selectedHabits
}