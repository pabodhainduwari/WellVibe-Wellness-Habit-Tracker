package com.example.moodflow

import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun testMainActivityLaunchesWithoutCrash() {
        // Launch the MainActivity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Verify that the activity was created successfully
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun testAddHabitsToViewDoesNotCrash() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        scenario.onActivity { activity ->
            // Get the main layout
            val mainLayout = activity.findViewById<LinearLayout>(R.id.main)
            assertNotNull(mainLayout)
            
            // Verify that we have at least one child
            assert(mainLayout.childCount > 0)
            
            // Get the first child and verify it's a ScrollView
            val firstChild = mainLayout.getChildAt(0)
            assert(firstChild is ScrollView)
            
            val scrollView = firstChild as ScrollView
            
            // Verify that the ScrollView has at least one child
            assert(scrollView.childCount > 0)
            
            // Get the content layout and verify it's a LinearLayout
            val contentLayout = scrollView.getChildAt(0)
            assert(contentLayout is LinearLayout)
        }
    }
}