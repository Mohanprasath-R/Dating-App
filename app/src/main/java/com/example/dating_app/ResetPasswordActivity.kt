package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val email = intent.getStringExtra("EMAIL") ?: ""
        val newPasswordInput = findViewById<EditText>(R.id.new_password_input)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirm_password_input)
        val resetButton = findViewById<Button>(R.id.reset_button)
        val passwordToggle = findViewById<ImageView>(R.id.password_toggle)
        val confirmPasswordToggle = findViewById<ImageView>(R.id.confirm_password_toggle)

        var isPasswordVisible = false
        var isConfirmPasswordVisible = false

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                newPasswordInput.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
            } else {
                newPasswordInput.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            }
            newPasswordInput.setSelection(newPasswordInput.text.length)
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

        resetButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (newPassword.isEmpty()) {
                Toast.makeText(this, "Please enter new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                resetButton.isEnabled = false
                val result = repository.resetPassword(email, newPassword)
                
                result.onSuccess {
                    Toast.makeText(this@ResetPasswordActivity, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }.onFailure { e ->
                    Toast.makeText(this@ResetPasswordActivity, e.message, Toast.LENGTH_SHORT).show()
                    resetButton.isEnabled = true
                }
            }
        }

        findViewById<android.view.View>(R.id.back_button).setOnClickListener {
            finish()
        }
    }
}
