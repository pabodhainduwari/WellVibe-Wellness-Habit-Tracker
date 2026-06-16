package com.example.moodflow

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.moodflow.data.UserManager

class RegisterActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }
    
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var registerButton: MaterialButton
    private lateinit var signinLink: TextView
    private lateinit var termsCheckbox: MaterialCheckBox
    private lateinit var userManager: UserManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        // Initialize views
        initViews()
        
        // Initialize UserManager
        userManager = UserManager(this)
        
        // Set up listeners
        setupListeners()
        
        // Request notification permission
        requestNotificationPermission()
    }
    
    private fun initViews() {
        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        nameInputLayout = findViewById(R.id.name_input_layout)
        emailInputLayout = findViewById(R.id.email_input_layout)
        passwordInputLayout = findViewById(R.id.password_input_layout)
        confirmPasswordInputLayout = findViewById(R.id.confirm_password_input_layout)
        registerButton = findViewById(R.id.register_button)
        signinLink = findViewById(R.id.signin_link)
        termsCheckbox = findViewById(R.id.terms_checkbox)
        
        // Add text watchers for validation
        nameInput.addTextChangedListener(createTextWatcher(nameInputLayout))
        emailInput.addTextChangedListener(createTextWatcher(emailInputLayout))
        passwordInput.addTextChangedListener(createTextWatcher(passwordInputLayout))
        confirmPasswordInput.addTextChangedListener(createTextWatcher(confirmPasswordInputLayout))
    }
    
    private fun setupListeners() {
        registerButton.setOnClickListener {
            if (validateInputs()) {
                performRegistration()
            }
        }
        
        signinLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Validate name
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            nameInputLayout.error = "Name is required"
            isValid = false
        } else if (name.length < 2) {
            nameInputLayout.error = "Please enter a valid name"
            isValid = false
        } else {
            nameInputLayout.error = null
        }
        
        // Validate email
        val email = emailInput.text.toString().trim()
        if (email.isEmpty()) {
            emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Please enter a valid email"
            isValid = false
        } else {
            emailInputLayout.error = null
        }
        
        // Validate password
        val password = passwordInput.text.toString()
        if (password.isEmpty()) {
            passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordInputLayout.error = null
        }
        
        // Validate confirm password
        val confirmPassword = confirmPasswordInput.text.toString()
        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.error = "Please confirm your password"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordInputLayout.error = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordInputLayout.error = null
        }
        
        // Validate terms agreement
        if (!termsCheckbox.isChecked) {
            Toast.makeText(this, "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        return isValid
    }
    
    private fun performRegistration() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        
        // Show loading state
        registerButton.text = "Creating Account..."
        registerButton.isEnabled = false
        
        // Simulate network call
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val success = userManager.registerUser(name, email, password)
            if (success) {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                startMainActivity()
            } else {
                Toast.makeText(this, "An account with this email already exists", Toast.LENGTH_SHORT).show()
                registerButton.text = "Sign Up"
                registerButton.isEnabled = true
            }
        }, 1000)
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun createTextWatcher(layout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                layout.error = null
            }
        }
    }
    
    private fun requestNotificationPermission() {
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission denied
                    Toast.makeText(this, "Notification permission is required for reminders", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}