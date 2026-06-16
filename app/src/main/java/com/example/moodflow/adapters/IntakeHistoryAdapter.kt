package com.example.moodflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moodflow.R
import com.example.moodflow.model.WaterIntakeRecord
import java.text.SimpleDateFormat
import java.util.*

class IntakeHistoryAdapter(
    private var records: List<WaterIntakeRecord>
) : RecyclerView.Adapter<IntakeHistoryAdapter.IntakeHistoryViewHolder>() {

    class IntakeHistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.history_time_text)
        val amountText: TextView = view.findViewById(R.id.history_amount_text)
        val percentageText: TextView = view.findViewById(R.id.history_percentage_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntakeHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_intake_history, parent, false)
        return IntakeHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: IntakeHistoryViewHolder, position: Int) {
        val record = records[position]
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.timeText.text = timeFormat.format(record.date)
        
        holder.amountText.text = "${record.amount}ml"
        
        val percentage = if (record.target > 0) {
            (record.amount.toDouble() / record.target.toDouble() * 100).toInt()
        } else {
            0
        }
        holder.percentageText.text = "$percentage%"
    }

    override fun getItemCount() = records.size
    
    // Add the updateData method
    fun updateData(newRecords: List<WaterIntakeRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}