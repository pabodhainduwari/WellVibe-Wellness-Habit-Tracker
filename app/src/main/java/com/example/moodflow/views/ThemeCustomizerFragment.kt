package com.example.moodflow.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.moodflow.BaseFragment
import com.example.moodflow.DashboardFragment
import com.example.moodflow.MainActivity
import com.example.moodflow.R
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView

class ThemeCustomizerFragment : BaseFragment() {
    
    override fun getLayoutId(): Int {
        return R.layout.fragment_theme_customizer
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        return view
    }
    
    override fun setupViews(view: View) {
        setupThemeOptions(view)
        setupAccentColorOptions(view)
        setupBackButton(view)
    }
    
    private fun setupThemeOptions(view: View) {
        val themeToggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.theme_toggle_group)
        
        // Set default selection based on current theme
        // In a real app, you would check the current theme from SharedPreferences
        themeToggleGroup.check(R.id.light_theme_button)
        
        themeToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.light_theme_button -> {
                        // Apply light theme
                        // In a real app, you would save this preference and apply the theme
                    }
                    R.id.dark_theme_button -> {
                        // Apply dark theme
                        // In a real app, you would save this preference and apply the theme
                    }
                }
            }
        }
    }
    
    private fun setupAccentColorOptions(view: View) {
        val colorCards = listOf(
            R.id.color_card_1 to R.color.primary,
            R.id.color_card_2 to R.color.accent,
            R.id.color_card_3 to R.color.water_primary,
            R.id.color_card_4 to R.color.mood_happy,
            R.id.color_card_5 to R.color.purple
        )
        
        colorCards.forEach { (cardId, colorRes) ->
            val card = view.findViewById<MaterialCardView>(cardId)
            card.setOnClickListener {
                // Apply selected color as accent color
                // In a real app, you would save this preference and apply the color
                selectColorCard(view, cardId)
            }
        }
        
        // Set default selection
        selectColorCard(view, R.id.color_card_1)
    }
    
    private fun selectColorCard(view: View, selectedCardId: Int) {
        val colorCards = listOf(
            R.id.color_card_1,
            R.id.color_card_2,
            R.id.color_card_3,
            R.id.color_card_4,
            R.id.color_card_5
        )
        
        colorCards.forEach { cardId ->
            val card = view.findViewById<MaterialCardView>(cardId)
            if (cardId == selectedCardId) {
                card.strokeWidth = 4
            } else {
                card.strokeWidth = 0
            }
        }
    }
    
    private fun setupBackButton(view: View) {
        val backButton = view.findViewById<Button>(R.id.back_button)
        backButton.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(DashboardFragment())
        }
    }
}