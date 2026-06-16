package com.example.moodflow

import android.view.View
import android.widget.CalendarView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodflow.model.Habit
import java.util.*

class HabitsCalendarFragment : BaseFragment() {
    
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var habits: MutableList<Habit>
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerView: RecyclerView
    private var selectedDate: Date = Date()
    
    override fun getLayoutId(): Int {
        return R.layout.fragment_habits_calendar
    }
    
    override fun setupViews(view: View) {
        habits = preferencesHelper.getHabits().toMutableList()
        
        calendarView = view.findViewById(R.id.habits_calendar_view)
        recyclerView = view.findViewById(R.id.calendar_habits_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        habitAdapter = HabitAdapter(
            getHabitsForSelectedDate(),
            onHabitToggle = { habit, position ->
                // Handle habit toggle for selected date
                toggleHabitCompletion(habit, position)
            },
            onHabitOptions = { _, habit, position ->
                // For calendar view, re-use toggle to quickly update completion
                toggleHabitCompletion(habit, position)
            }
        )
        
        recyclerView.adapter = habitAdapter
        
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            updateHabitsForSelectedDate()
        }
        
        updateHabitsForSelectedDate()
    }
    
    private fun getHabitsForSelectedDate(): MutableList<Habit> {
        // For now, we'll show all habits
        // In a more advanced implementation, we would filter by date
        return habits
    }
    
    private fun updateHabitsForSelectedDate() {
        // Update the adapter with habits for the selected date
        habitAdapter = HabitAdapter(
            getHabitsForSelectedDate(),
            onHabitToggle = { habit, position ->
                toggleHabitCompletion(habit, position)
            },
            onHabitOptions = { _, habit, position ->
                toggleHabitCompletion(habit, position)
            }
        )
        recyclerView.adapter = habitAdapter
    }
    
    private fun toggleHabitCompletion(habit: Habit, position: Int) {
        habit.completed = !habit.completed
        if (habit.completed) {
            habit.progress++
            habit.streak++
        } else {
            habit.progress = Math.max(0, habit.progress - 1)
            habit.streak = Math.max(0, habit.streak - 1)
        }
        
        // Update in preferences
        preferencesHelper.saveHabits(habits)
        
        // Notify adapter of change
        habitAdapter.notifyItemChanged(position)
        
        // Show toast or other feedback
        // Toast.makeText(context, "Habit updated", Toast.LENGTH_SHORT).show()
    }
}