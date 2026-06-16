package com.example.moodflow

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.example.moodflow.model.MoodEntry
import com.example.moodflow.views.QuickEntryBottomSheet
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.flexbox.FlexboxLayout
import java.text.SimpleDateFormat
import java.util.*

class MoodJournalFragment : BaseFragment() {
    
    private lateinit var moodEntries: MutableList<MoodEntry>
    private lateinit var moodEntriesAdapter: MoodEntryAdapter
    private lateinit var filteredMoodEntries: MutableList<MoodEntry>
    private var selectedMood: String = ""
    private var isCalendarView = false
    private var selectedDate: Date = Date()
    private lateinit var calendarView: CalendarView
    private lateinit var calendarMoodEntriesRecyclerView: RecyclerView
    private var view: View? = null
    
    override fun getLayoutId(): Int {
        // Check if we're on a tablet (sw600dp qualifier exists)
        val isTablet = resources?.getBoolean(R.bool.is_tablet) ?: false
        return if (isTablet) {
            R.layout.fragment_mood_journal_tablet
        } else {
            R.layout.fragment_mood_journal
        }
    }
    
    override fun setupViews(view: View) {
        this.view = view
        moodEntries = preferencesHelper.getMoodEntries().toMutableList()
        filteredMoodEntries = moodEntries.toMutableList()
        
        // Set today's date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        view.findViewById<TextView>(R.id.date_text).text = dateFormat.format(Date())
        
        // Set up emoji grid
        setupEmojiGrid(view)
        
        // Set up tags input
        val tagsInput = view.findViewById<TextInputEditText>(R.id.tags_input)
        
        // Set up note input
        val noteInput = view.findViewById<EditText>(R.id.note_input)
        noteInput.hint = "How are you feeling today?"
        
        // Set up save button
        val saveButton = view.findViewById<Button>(R.id.save_entry_button)
        saveButton.text = "Save Mood"
        saveButton.setOnClickListener {
            if (selectedMood.isEmpty()) {
                Toast.makeText(context, "Please select a mood", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val note = noteInput.text.toString()
            val tagsText = tagsInput?.text.toString()
            val tags = if (tagsText.isNotEmpty()) {
                tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            
            val newEntry = MoodEntry(
                date = Date(),
                mood = selectedMood,
                note = note,
                tags = tags
            )
            
            moodEntries.add(0, newEntry) // Add to the beginning of the list
            filteredMoodEntries.add(0, newEntry)
            moodEntriesAdapter.addEntry(newEntry) // Use adapter's method
            
            // Save to preferences
            preferencesHelper.saveMoodEntries(moodEntries)
            
            // Clear input
            noteInput.text.clear()
            tagsInput?.text?.clear()
            selectedMood = ""
            
            // Reset emoji selection
            resetEmojiSelection(view)
            
            Toast.makeText(context, "Mood saved successfully!", Toast.LENGTH_SHORT).show()
            
            // Update mood chart
            updateMoodChart(view)
            
            // Update calendar view if needed
            if (isCalendarView) {
                updateCalendarMoodEntries()
            }
        }
        
        // Set up mood entries list
        val recyclerView = view.findViewById<RecyclerView>(R.id.mood_entries_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        moodEntriesAdapter = MoodEntryAdapter(filteredMoodEntries) { entry ->
            // Handle delete click
            showDeleteConfirmationDialog(entry)
        }
        recyclerView.adapter = moodEntriesAdapter
        
        // Calendar view components
        calendarView = view.findViewById(R.id.mood_calendar_view)
        calendarMoodEntriesRecyclerView = view.findViewById(R.id.calendar_mood_entries_recycler_view)
        calendarMoodEntriesRecyclerView.layoutManager = LinearLayoutManager(context)
        
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            updateCalendarMoodEntries()
        }
        
        // Set up view toggle button
        val viewToggleButton = view.findViewById<Button>(R.id.view_toggle_button)
        viewToggleButton.setOnClickListener {
            toggleView(view)
        }
        
        // Set up export button
        val exportButton = view.findViewById<Button>(R.id.export_button)
        exportButton.setOnClickListener {
            exportMoodSummary()
        }
        
        // Set up floating action button
        val fabAddMood = view.findViewById<ExtendedFloatingActionButton>(R.id.fab_add_mood)
        fabAddMood.setOnClickListener {
            // Use the new quick entry bottom sheet
            val bottomSheet = QuickEntryBottomSheet.newInstance("mood")
            bottomSheet.show(parentFragmentManager, "quick_mood_entry")
        }
        
        // Add mood chart
        addMoodChart(view)
        
        updateCalendarMoodEntries()
    }
    
    private fun exportMoodSummary() {
        // Generate a summary of mood entries
        val summary = generateMoodSummary()
        
        // Create share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, summary)
            putExtra(Intent.EXTRA_TITLE, "Mood Summary")
        }
        
        // Show chooser
        val chooser = Intent.createChooser(shareIntent, "Share Mood Summary")
        startActivity(chooser)
    }
    
    private fun generateMoodSummary(): String {
        if (moodEntries.isEmpty()) {
            return "No mood entries to export."
        }
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        
        val summary = StringBuilder()
        summary.append("Mood Summary\n")
        summary.append("============\n\n")
        summary.append("Total Entries: ${moodEntries.size}\n\n")
        
        // Mood distribution
        val moodCount = mutableMapOf<String, Int>()
        for (entry in moodEntries) {
            moodCount[entry.mood] = moodCount.getOrDefault(entry.mood, 0) + 1
        }
        
        summary.append("Mood Distribution:\n")
        for ((mood, count) in moodCount) {
            val percentage = (count.toDouble() / moodEntries.size * 100).toInt()
            summary.append("$mood: $count ($percentage%)\n")
        }
        
        summary.append("\nRecent Entries:\n")
        for (i in 0 until Math.min(moodEntries.size, 10)) {
            val entry = moodEntries[i]
            summary.append("\n${dateFormat.format(entry.date)}: ${entry.mood}\n")
            if (entry.note.isNotEmpty()) {
                summary.append("  Note: ${entry.note}\n")
            }
            if (entry.tags.isNotEmpty()) {
                summary.append("  Tags: ${entry.tags.joinToString(", ")}\n")
            }
        }
        
        return summary.toString()
    }
    
    private fun toggleView(view: View) {
        isCalendarView = !isCalendarView
        
        val listViewContainer = view.findViewById<LinearLayout>(R.id.list_view_container)
        val calendarViewContainer = view.findViewById<LinearLayout>(R.id.calendar_view_container)
        val viewToggleButton = view.findViewById<Button>(R.id.view_toggle_button)
        
        if (isCalendarView) {
            listViewContainer.visibility = View.GONE
            calendarViewContainer.visibility = View.VISIBLE
            viewToggleButton.text = "List View"
        } else {
            listViewContainer.visibility = View.VISIBLE
            calendarViewContainer.visibility = View.GONE
            viewToggleButton.text = "Calendar View"
        }
    }
    
    private fun updateCalendarMoodEntries() {
        // Filter entries for the selected date
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        
        val selectedEntries = moodEntries.filter { entry ->
            val entryCalendar = Calendar.getInstance()
            entryCalendar.time = entry.date
            
            calendar.get(Calendar.YEAR) == entryCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == entryCalendar.get(Calendar.DAY_OF_YEAR)
        }
        
        val calendarAdapter = MoodEntryAdapter(selectedEntries.toMutableList())
        calendarMoodEntriesRecyclerView.adapter = calendarAdapter
    }
    
    private fun resetEmojiSelection(view: View) {
        val emojiGrid = view.findViewById<FlexboxLayout>(R.id.emoji_grid)
        for (i in 0 until emojiGrid.childCount) {
            val child = emojiGrid.getChildAt(i)
            child.animate().cancel()
            child.scaleX = 1f
            child.scaleY = 1f
            child.isSelected = false
        }
    }
    
    private fun setupEmojiGrid(view: View) {
        val emojiGrid = view.findViewById<FlexboxLayout>(R.id.emoji_grid)
        // Expanded list of moods for better selection
        val emojis = arrayOf(
            "😊" to "Happy",
            "😃" to "Excited",
            "😍" to "Loved",
            "😎" to "Cool",
            "🤩" to "Starstruck",
            "🥳" to "Partying",
            "😌" to "Relieved",
            "😐" to "Neutral",
            "😔" to "Sad",
            "😢" to "Crying",
            "😰" to "Anxious",
            "😨" to "Fearful",
            "😤" to "Frustrated",
            "😡" to "Angry",
            "😴" to "Tired",
            "😪" to "Sleepy",
            "🤗" to "Hugged",
            "🤔" to "Thoughtful",
            "😷" to "Sick",
            "🤒" to "Fever",
            "🤕" to "Hurt",
            "🤢" to "Nauseous",
            "🤮" to "Vomiting",
            "🥵" to "Hot",
            "🥶" to "Cold",

        )
        
    emojiGrid.removeAllViews()

    val density = resources?.displayMetrics?.density ?: 1f
    val margin = (8 * density).toInt()
    val itemPadding = (12 * density).toInt()

        for ((emoji, description) in emojis) {
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setPadding(itemPadding, itemPadding, itemPadding, itemPadding)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_emoji_selector)
                isSelected = false
            }
            
            val emojiView = TextView(context).apply {
                text = emoji
                textSize = 30f
                setPadding(4, 4, 4, 4)
            }
            
            container.addView(emojiView)
            TooltipCompat.setTooltipText(container, description)
            
            container.setOnClickListener {
                // Reset all emoji backgrounds
                resetEmojiSelection(view)
                
                // Highlight selected emoji
                container.isSelected = true
                selectedMood = emoji
                
                // Add animation to selected emoji
                container.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                    container.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                }.start()
            }
            
            emojiGrid.addView(container)
        }
    }
    
    private fun addMoodChart(view: View) {
    val chart = view.findViewById<LineChart>(R.id.mood_chart)
    chart.setNoDataText("Log a mood to see your trend")
    chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
        
        // Get last 7 days of mood entries
        val recentEntries = moodEntries.take(7).sortedBy { it.date }
        
        // Create data entries for the chart
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        
        // Map moods to numerical values
        val moodValues = mapOf(
            "😊" to 5f, "😃" to 5f, "😍" to 5f, // Happy
            "😎" to 4f, "🤗" to 4f, // Good
            "😐" to 3f, "🤔" to 3f, // Neutral
            "😔" to 2f, "😰" to 2f, // Sad/Anxious
            "😤" to 1f, "😴" to 1f, "😷" to 1f // Frustrated/Tired/Sick
        )
        
        for ((index, entry) in recentEntries.withIndex()) {
            val moodValue = moodValues[entry.mood] ?: 3f
            entries.add(Entry(index.toFloat(), moodValue))
            
            // Format date for label
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            labels.add(dateFormat.format(entry.date))
        }
        
        // Create data set
        val accentColor = ContextCompat.getColor(requireContext(), R.color.purple)
        val textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary)

        val dataSet = LineDataSet(entries, "Weekly Mood Trend").apply {
            color = accentColor
            setCircleColor(accentColor)
            lineWidth = 2f
            circleRadius = 6f
            setDrawCircleHole(true)
            valueTextSize = 10f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient_fill)
            valueTextColor = textPrimary
        }
        
        // Configure chart
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.legend.textColor = textPrimary
        chart.legend.textSize = 12f
        
        // Configure X axis
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(labels)
            textColor = textPrimary
            textSize = 10f
            granularity = 1f
            setAvoidFirstLastClipping(true)
        }
        
        // Configure Y axis
        chart.axisLeft.apply {
            textColor = textPrimary
            textSize = 10f
            axisMinimum = 1f
            axisMaximum = 5f
            setLabelCount(5, true)
            granularity = 1f
        }
        
        chart.axisRight.isEnabled = false
        
        // Enable touch gestures
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setDoubleTapToZoomEnabled(true)
        chart.setExtraOffsets(16f, 16f, 16f, 16f)
        chart.invalidate()
        
        chart.animateY(1000)
    }
    
    private fun updateMoodChart(view: View) {
        addMoodChart(view)
    }
    
    private fun showDeleteConfirmationDialog(entry: MoodEntry) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Mood Entry")
        builder.setMessage("Are you sure you want to delete this mood entry?")
        
        builder.setPositiveButton("Delete") { _, _ ->
            // Remove from both lists
            moodEntries.remove(entry)
            filteredMoodEntries.remove(entry)
            
            // Update adapter using its own method
            moodEntriesAdapter.removeEntry(entry)
            
            // Save to preferences
            preferencesHelper.saveMoodEntries(moodEntries)
            
            // Update mood chart
            updateMoodChart(view!!)
            
            Toast.makeText(context, "Mood entry deleted", Toast.LENGTH_SHORT).show()
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        
        builder.show()
    }
}