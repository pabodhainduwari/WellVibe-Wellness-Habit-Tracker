package com.example.moodflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moodflow.model.MoodEntry
import java.text.SimpleDateFormat
import java.util.Locale

class MoodEntryAdapter(
    moodEntries: List<MoodEntry>,
    private val onDeleteClickListener: ((MoodEntry) -> Unit)? = null
) : RecyclerView.Adapter<MoodEntryAdapter.MoodEntryViewHolder>() {

    // Keep our own copy of the data to avoid inconsistencies
    private val _moodEntries = moodEntries.toMutableList()
    val moodEntries: List<MoodEntry>
        get() = _moodEntries

    class MoodEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.entry_date)
        val moodEmoji: TextView = view.findViewById(R.id.entry_mood)
        val noteText: TextView = view.findViewById(R.id.entry_note)
        val deleteButton: View = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_entry, parent, false)
        return MoodEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodEntryViewHolder, position: Int) {
        // Safety check to prevent IndexOutOfBoundsException
        if (position < 0 || position >= _moodEntries.size) {
            return
        }
        
        val entry = _moodEntries[position]
        
        // Format date as "MMM dd"
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        holder.dateText.text = dateFormat.format(entry.date)
        
        holder.moodEmoji.text = entry.mood
        holder.noteText.text = if (entry.note.isNotEmpty()) entry.note else "No note"
        
        // Set up delete button
        holder.deleteButton.setOnClickListener {
            onDeleteClickListener?.invoke(entry)
        }
    }

    override fun getItemCount() = _moodEntries.size
    
    fun removeEntry(entry: MoodEntry) {
        val position = _moodEntries.indexOf(entry)
        if (position != -1) {
            _moodEntries.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    
    fun updateEntries(newEntries: List<MoodEntry>) {
        _moodEntries.clear()
        _moodEntries.addAll(newEntries)
        notifyDataSetChanged()
    }
    
    fun addEntry(entry: MoodEntry) {
        _moodEntries.add(0, entry)
        notifyItemInserted(0)
    }
}