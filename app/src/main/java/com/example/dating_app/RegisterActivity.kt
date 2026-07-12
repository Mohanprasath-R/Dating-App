package com.example.dating_app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datingapp.R
import com.example.dating_app.model.User
import com.example.dating_app.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.util.*

class RegisterActivity : AppCompatActivity() {
    private val repository = FirebaseRepository()
    private var selectedGender = "Female"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameInput = findViewById<EditText>(R.id.name_input)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirm_password_input)
        val dobInput = findViewById<EditText>(R.id.dob_input)
        val genderFemale = findViewById<View>(R.id.gender_female)
        val genderMale = findViewById<View>(R.id.gender_male)
        val createAccountButton = findViewById<Button>(R.id.create_account_button)
        val passwordToggle = findViewById<ImageView>(R.id.password_toggle)
        val confirmPasswordToggle = findViewById<ImageView>(R.id.confirm_password_toggle)

        var isPasswordVisible = false
        var isConfirmPasswordVisible = false

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordInput.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
            } else {
                passwordInput.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        confirmPasswordToggle.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                confirmPasswordInput.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
            } else {
                confirmPasswordInput.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            }
            confirmPasswordInput.setSelection(confirmPasswordInput.text.length)
        }

        dobInput.setOnClickListener {
            showDatePicker(dobInput)
        }

        genderFemale.setOnClickListener {
            selectedGender = "Female"
            genderFemale.setBackgroundResource(R.drawable.edit_text_background) // Should have a selected style
            genderMale.setBackgroundResource(R.drawable.edit_text_background)
            // Update UI to show selection (simple way for now)
            genderFemale.alpha = 1.0f
            genderMale.alpha = 0.5f
        }

        genderMale.setOnClickListener {
            selectedGender = "Male"
            genderFemale.alpha = 0.5f
            genderMale.alpha = 1.0f
        }

        findViewById<TextView>(R.id.login_link).setOnClickListener {
            finish()
        }

        createAccountButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val dob = dobInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || dob.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Strong Password Policy
            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!password.any { it.isDigit() } || !password.any { !it.isLetterOrDigit() }) {
                Toast.makeText(this, "Password must contain a number and special character", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                createAccountButton.isEnabled = false
                val authResult = repository.registerUser(email, password)
                
                authResult.onSuccess { uid ->
                    val user = User(
                        id = uid,
                        username = email.substringBefore("@"),
                        first_name = name,
                        email = email,
                        gender = selectedGender,
                        dob = dob,
                        created_at = System.currentTimeMillis()
                    )
                    
                    val dbResult = repository.createUser(user)
                    dbResult.onSuccess {
                        Toast.makeText(this@RegisterActivity, "Registration Successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, CompleteProfileActivity::class.java)
                        intent.putExtra("USER_ID", uid)
                        startActivity(intent)
                        finish()
                    }.onFailure { e ->
                        // LOG THE ERROR
                        android.util.Log.e("RegisterActivity", "Firestore Error: ${e.message}", e)
                        Toast.makeText(this@RegisterActivity, "Firestore Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        createAccountButton.isEnabled = true
                    }
                }.onFailure { e ->
                    android.util.Log.e("RegisterActivity", "Auth Error: ${e.message}", e)
                    Toast.makeText(this@RegisterActivity, "Registration failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    createAccountButton.isEnabled = true
                }
            }
        }

        findViewById<android.view.View>(R.id.back_button).setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            editText.setText("$dayOfMonth/${month + 1}/$year")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
}