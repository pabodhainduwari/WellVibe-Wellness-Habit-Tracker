package com.example.moodflow.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.moodflow.R
import com.example.moodflow.data.AchievementManager
import com.example.moodflow.model.Habit
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.*

class QuickEntryBottomSheet : BottomSheetDialogFragment() {
    
    private var entryType: String = "habit"
    private lateinit var preferencesHelper: com.example.moodflow.data.PreferencesHelper
    private lateinit var achievementManager: AchievementManager
    
    companion object {
        const val ARG_ENTRY_TYPE = "entry_type"
        
        fun newInstance(entryType: String): QuickEntryBottomSheet {
            val fragment = QuickEntryBottomSheet()
            val args = Bundle()
            args.putString(ARG_ENTRY_TYPE, entryType)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entryType = arguments?.getString(ARG_ENTRY_TYPE) ?: "habit"
        preferencesHelper = com.example.moodflow.data.PreferencesHelper(requireContext())
        achievementManager = AchievementManager(requireContext())
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_quick_entry, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
    }
    
    private fun setupViews(view: View) {
        val titleText = view.findViewById<TextView>(R.id.title_text)
        val habitNameInput = view.findViewById<EditText>(R.id.habit_name_input)
        val habitTargetInput = view.findViewById<EditText>(R.id.habit_target_input)
        val moodChipGroup = view.findViewById<ChipGroup>(R.id.mood_chip_group)
        val noteInput = view.findViewById<EditText>(R.id.note_input)
        val saveButton = view.findViewById<Button>(R.id.save_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)
        
        when (entryType) {
            "habit" -> {
                titleText.text = "Add New Habit"
                habitNameInput.visibility = View.VISIBLE
                habitTargetInput.visibility = View.VISIBLE
                moodChipGroup.visibility = View.GONE
                noteInput.hint = "Add a description (optional)"
            }
            "mood" -> {
                titleText.text = "How are you feeling?"
                habitNameInput.visibility = View.GONE
                habitTargetInput.visibility = View.GONE
                moodChipGroup.visibility = View.VISIBLE
                noteInput.hint = "What's on your mind? (optional)"
                
                setupMoodChips(moodChipGroup)
            }
        }
        
        saveButton.setOnClickListener {
            when (entryType) {
                "habit" -> saveHabit(habitNameInput, habitTargetInput)
                "mood" -> saveMood(moodChipGroup, noteInput)
            }
        }
        
        cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupMoodChips(chipGroup: ChipGroup) {
        val moods = mapOf(
            "😊" to "Happy",
            "😢" to "Sad",
            "😠" to "Angry",
            "😴" to "Tired",
            "🤩" to "Excited",
            "😐" to "Neutral",
            "😰" to "Anxious",
            "😌" to "Calm"
        )
        
        moods.forEach { (emoji, name) ->
            val chip = Chip(requireContext()).apply {
                text = "$emoji $name"
                isCheckable = true
                isClickable = true
            }
            chipGroup.addView(chip)
        }
    }
    
    private fun saveHabit(nameInput: EditText, targetInput: EditText) {
        val name = nameInput.text.toString().trim()
        val targetText = targetInput.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(context, "Please enter a habit name", Toast.LENGTH_SHORT).show()
            return
        }
        
        val target = if (targetText.isNotEmpty()) targetText.toIntOrNull() ?: 1 else 1
        
        // Get existing habits
        val habits = preferencesHelper.getHabits().toMutableList()
        
        // Create new habit
        val newHabit = Habit(
            id = (habits.maxOfOrNull { it.id } ?: 0) + 1,
            name = name,
            category = "General",
            description = "",
            completed = false,
            streak = 0,
            progress = 0,
            target = target,
            frequency = "daily",
            reminderTime = null,
            reminderEnabled = false,
            lastCompletedDate = null,
            createdDate = Date(),
            color = "#3DDC84",
            icon = "check"
        )
        
        // Add to list and save
        habits.add(newHabit)
        preferencesHelper.saveHabits(habits)
        
        // Check for achievements
        achievementManager.checkAndUnlockAchievements(preferencesHelper)
        
        Toast.makeText(context, "Habit added successfully!", Toast.LENGTH_SHORT).show()
        dismiss()
    }
    
    private fun saveMood(chipGroup: ChipGroup, noteInput: EditText) {
        val selectedChipId = chipGroup.checkedChipId
        if (selectedChipId == View.NO_ID) {
            Toast.makeText(context, "Please select a mood", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedChip = chipGroup.findViewById<Chip>(selectedChipId)
        val moodText = selectedChip.text.toString()
        val emoji = moodText.substring(0, 2) // Extract emoji
        val note = noteInput.text.toString().trim()
        
        // Get existing mood entries
        val moodEntries = preferencesHelper.getMoodEntries().toMutableList()
        
        // Create new mood entry
        val newEntry = com.example.moodflow.model.MoodEntry(
            id = System.currentTimeMillis().toString(),
            date = Date(),
            mood = emoji,
            note = note,
            intensity = 5, // Default intensity
            activities = emptyList(),
            tags = emptyList(),
            location = null,
            imageUri = null,
            weather = null
        )
        
        // Add to list and save
        moodEntries.add(0, newEntry) // Add to beginning
        preferencesHelper.saveMoodEntries(moodEntries)
        
        // Check for achievements
        achievementManager.checkAndUnlockAchievements(preferencesHelper)
        
        Toast.makeText(context, "Mood entry saved!", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}