package com.example.moodflow

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.moodflow.model.Habit
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.util.Locale

class HabitAdapter(
    private val habits: MutableList<Habit>,
    private val onHabitToggle: (Habit, Int) -> Unit,
    private val onHabitOptions: (View, Habit, Int) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    private var bulkMode: Boolean = false
    private var selectedHabitIds: Set<Int> = emptySet()

    class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.habit_checkbox)
        val habitName: TextView = view.findViewById(R.id.habit_name)
        val streakText: TextView = view.findViewById(R.id.streak_text)
        val progressBar: ProgressBar = view.findViewById(R.id.habit_progress_bar)
        val progressText: TextView = view.findViewById(R.id.progress_text)
        val descriptionText: TextView = view.findViewById(R.id.habit_description)
        val optionsButton: View = view.findViewById(R.id.habit_options_button)
        val iconCard: MaterialCardView = view.findViewById(R.id.habit_color_card)
        val iconView: ImageView = view.findViewById(R.id.habit_icon)
        val frequencyChip: Chip = view.findViewById(R.id.habit_frequency_chip)
        val categoryChip: Chip = view.findViewById(R.id.habit_category_chip)
        val targetText: TextView = view.findViewById(R.id.habit_target_text)
        val itemView: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        val context = holder.itemView.context

        holder.checkBox.isChecked = habit.completed
        holder.habitName.text = habit.name

        holder.streakText.apply {
            if (habit.streak > 0) {
                visibility = View.VISIBLE
                text = "\uD83D\uDD25 ${habit.streak} day" + if (habit.streak == 1) "" else "s"
            } else {
                visibility = View.VISIBLE
                text = "✨ Fresh start"
            }
        }

        val accentColor = runCatching { Color.parseColor(habit.color) }.getOrElse {
            ContextCompat.getColor(context, R.color.primary)
        }
        holder.iconCard.setCardBackgroundColor(accentColor)
        holder.iconView.imageTintList = ColorStateList.valueOf(Color.WHITE)

        val chipFillColor = ColorUtils.setAlphaComponent(accentColor, (255 * 0.2f).toInt())
        val chipTextColor = if (ColorUtils.calculateLuminance(chipFillColor) < 0.5) {
            Color.WHITE
        } else {
            ContextCompat.getColor(context, R.color.text_primary)
        }

        holder.categoryChip.apply {
            text = habit.category.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            chipBackgroundColor = ColorStateList.valueOf(chipFillColor)
            setTextColor(chipTextColor)
            chipIconTint = ColorStateList.valueOf(chipTextColor)
        }

        val frequencyLabel = habit.frequency.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        holder.frequencyChip.apply {
            text = frequencyLabel
            chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.bg_secondary))
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
        }

        val unit = when {
            habit.name.contains("water", ignoreCase = true) -> "glasses"
            habit.name.contains("step", ignoreCase = true) -> "steps"
            habit.target == 1 -> "time"
            else -> "times"
        }

        holder.targetText.text = if (habit.target > 0) {
            "Target: ${habit.target} $unit • $frequencyLabel"
        } else {
            "Target: flexible • $frequencyLabel"
        }

        holder.progressBar.progress = habit.completionPercentage
        holder.progressText.text = if (habit.target > 0) {
            "${habit.progress}/${habit.target} $unit"
        } else {
            "Tap to log"
        }

        if (habit.description.isNotBlank()) {
            holder.descriptionText.visibility = View.VISIBLE
            holder.descriptionText.text = habit.description
        } else {
            holder.descriptionText.visibility = View.GONE
        }

        holder.checkBox.setOnClickListener {
            onHabitToggle(habit, position)
        }

        holder.optionsButton.setOnClickListener {
            onHabitOptions(it, habit, position)
        }

        holder.itemView.setOnLongClickListener {
            onHabitOptions(it, habit, position)
            true
        }

        when {
            bulkMode && selectedHabitIds.contains(habit.id) -> holder.itemView.setBackgroundResource(R.drawable.habit_completed_background)
            habit.completed -> holder.itemView.setBackgroundResource(R.drawable.habit_completed_background)
            else -> holder.itemView.setBackgroundResource(R.drawable.habit_default_background)
        }

        holder.checkBox.isEnabled = !bulkMode
    }

    override fun getItemCount(): Int = habits.size

    fun updateSelectionState(isBulkMode: Boolean, selectedHabits: Set<Habit>) {
        bulkMode = isBulkMode
        selectedHabitIds = selectedHabits.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                habits[i] = habits[i + 1].also { habits[i + 1] = habits[i] }
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                habits[i] = habits[i - 1].also { habits[i - 1] = habits[i] }
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getHabitAt(position: Int): Habit = habits[position]

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val temp = habits[fromPosition]
        habits[fromPosition] = habits[toPosition]
        habits[toPosition] = temp
        notifyItemChanged(fromPosition)
        notifyItemChanged(toPosition)
    }
}