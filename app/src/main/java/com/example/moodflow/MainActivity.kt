package com.example.moodflow

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView

class MainActivity : AppCompatActivity() {
    
    private var bottomNavigation: BottomNavigationView? = null
    private var navigationRail: NavigationRailView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Handle window insets for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Update padding for the main content
            view.findViewById<View>(R.id.fragment_container).setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            
            // Handle navigation components padding
            bottomNavigation?.let { nav ->
                nav.updatePadding(bottom = systemBars.bottom)
            }
            
            navigationRail?.let { rail ->
                rail.updatePadding(left = systemBars.left)
            }
            
            insets
        }
        
        // Check if we're using navigation rail (tablet) or bottom navigation (phone)
        navigationRail = findViewById(R.id.navigation_rail)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        
        // Set up navigation listeners
        navigationRail?.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item)
        }
        
        bottomNavigation?.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item)
        }
        
        // Check if we need to load a specific fragment
        val fragmentToLoad = intent.getStringExtra("fragment")
        when (fragmentToLoad) {
            "habits" -> {
                loadFragment(HabitsFragment())
                setSelectedNavigationItem(R.id.nav_habits)
            }
            "mood" -> {
                loadFragment(MoodJournalFragment())
                setSelectedNavigationItem(R.id.nav_mood)
            }
            "settings" -> {
                loadFragment(SettingsFragment())
                setSelectedNavigationItem(R.id.nav_settings)
            }
            else -> {
                // Load default fragment
                if (savedInstanceState == null) {
                    loadFragment(DashboardFragment())
                    setSelectedNavigationItem(R.id.nav_home)
                }
            }
        }
    }
    
    private fun handleNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_home -> {
                loadFragment(DashboardFragment())
                true
            }
            R.id.nav_habits -> {
                loadFragment(HabitsFragment())
                true
            }
            R.id.nav_mood -> {
                loadFragment(MoodJournalFragment())
                true
            }
            R.id.nav_settings -> {
                loadFragment(SettingsFragment())
                true
            }
            else -> false
        }
    }
    
    private fun setSelectedNavigationItem(itemId: Int) {
        navigationRail?.selectedItemId = itemId
        bottomNavigation?.selectedItemId = itemId
    }
    
    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}