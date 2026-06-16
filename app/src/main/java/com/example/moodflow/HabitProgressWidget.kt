package com.example.moodflow

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.moodflow.data.PreferencesHelper

/**
 * Implementation of App Widget functionality.
 */
class HabitProgressWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Get the current habit completion percentage
            val preferencesHelper = PreferencesHelper(context)
            val habits = preferencesHelper.getHabits()
            val dailyCompletion = if (habits.isEmpty()) 0 else {
                val completedHabits = habits.count { it.completed }
                (completedHabits.toDouble() / habits.size.toDouble() * 100).toInt()
            }

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_habit_progress)
            views.setTextViewText(R.id.widget_progress_text, "$dailyCompletion%")

            // Create an Intent to launch MainActivity when the widget is clicked
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_progress_text, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}