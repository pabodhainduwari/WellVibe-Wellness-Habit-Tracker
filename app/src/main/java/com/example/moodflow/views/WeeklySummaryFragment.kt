package com.example.moodflow.views

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.FileProvider
import com.example.moodflow.BaseFragment
import com.example.moodflow.DashboardFragment
import com.example.moodflow.MainActivity
import com.example.moodflow.R
import com.example.moodflow.data.PreferencesHelper
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class WeeklySummaryFragment : BaseFragment() {
    
    
    private lateinit var chart: LineChart
    
    override fun getLayoutId(): Int {
        return R.layout.fragment_weekly_summary
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        
        return view
    }
    
    override fun setupViews(view: View) {
        setupHeader(view)
        setupHabitStats(view)
        setupHydrationStats(view)
        setupMoodStats(view)
        setupChart(view)
        setupShareButton(view)
        setupBackButton(view)
    }
    
    private fun setupHeader(view: View) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val startDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        val endDate = Calendar.getInstance()
        
        val dateRangeText = "${dateFormat.format(startDate.time)} - ${dateFormat.format(endDate.time)}"
        view.findViewById<TextView>(R.id.date_range_text).text = dateRangeText
    }
    
    private fun setupHabitStats(view: View) {
        val habits = preferencesHelper.getHabits()
        val completedHabits = habits.count { it.completed }
        val totalHabits = habits.size
        val completionRate = if (totalHabits > 0) (completedHabits.toFloat() / totalHabits * 100).roundToInt() else 0
        
        view.findViewById<TextView>(R.id.habits_completed_text).text = "$completedHabits/$totalHabits"
        view.findViewById<TextView>(R.id.habits_completion_rate).text = "$completionRate%"
    }
    
    private fun setupHydrationStats(view: View) {
        val hydrationProgress = preferencesHelper.getHydrationProgress()
        val progress = hydrationProgress.first
        val target = hydrationProgress.second
        val percentage = if (target > 0) (progress.toFloat() / target * 100).roundToInt() else 0
        
        view.findViewById<TextView>(R.id.hydrated_text).text = "$progress ml"
        view.findViewById<TextView>(R.id.hydrated_percentage).text = "$percentage%"
    }
    
    private fun setupMoodStats(view: View) {
        val moodEntries = preferencesHelper.getMoodEntries()
        if (moodEntries.isNotEmpty()) {
            val averageMood = moodEntries.take(7).map { it.moodScore }.average()
            val averageMoodText = String.format("%.1f", averageMood)
            
            view.findViewById<TextView>(R.id.mood_average_text).text = averageMoodText
            
            // Find most common mood
            val moodCounts = mutableMapOf<String, Int>()
            moodEntries.take(7).forEach { entry ->
                moodCounts[entry.mood] = moodCounts.getOrDefault(entry.mood, 0) + 1
            }
            
            val mostCommonMood = moodCounts.maxByOrNull { it.value }?.key ?: "😊"
            view.findViewById<TextView>(R.id.mood_emoji_text).text = mostCommonMood
        }
    }
    
    private fun setupChart(view: View) {
        chart = view.findViewById(R.id.progress_chart)
        
        // Sample data - in a real app, this would come from analytics
        val entries = listOf(
            Entry(0f, 40f),
            Entry(1f, 65f),
            Entry(2f, 50f),
            Entry(3f, 75f),
            Entry(4f, 80f),
            Entry(5f, 60f),
            Entry(6f, 90f)
        )
        
        val dataSet = LineDataSet(entries, "Weekly Progress").apply {
            color = Color.parseColor("#BE5985")
            setCircleColor(Color.parseColor("#BE5985"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 12f
        }
        
        val lineData = LineData(dataSet)
        chart.data = lineData
        
        // Format X axis
        val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(weekdays)
            granularity = 1f
        }
        
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
        }
        
        chart.invalidate()
    }
    
    private fun setupShareButton(view: View) {
        val shareButton = view.findViewById<Button>(R.id.share_button)
        shareButton.setOnClickListener {
            shareSummary()
        }
    }
    
    private fun setupBackButton(view: View) {
        val backButton = view.findViewById<Button>(R.id.back_button)
        backButton.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(DashboardFragment())
        }
    }
    
    private fun shareSummary() {
        // Create a bitmap of the summary view
        val view = view ?: return
        val bitmap = getBitmapFromView(view)
        
        // Save bitmap to file
        val file = saveBitmapToFile(bitmap)
        
        // Share the image
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share Weekly Summary"))
    }
    
    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
    
    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val imagesDir = File(requireContext().cacheDir, "images")
        imagesDir.mkdirs()
        val file = File(imagesDir, "weekly_summary.png")
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        return file
    }
}