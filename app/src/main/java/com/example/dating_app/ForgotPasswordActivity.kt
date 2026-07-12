package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailInput = findViewById<EditText>(R.id.email_input)
        val sendCodeButton = findViewById<Button>(R.id.send_code_button)

        sendCodeButton.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                sendCodeButton.isEnabled = false
                val result = repository.sendOtp(email)
                
                result.onSuccess { otp ->
                    // For demonstration, show the OTP in a toast. 
                    // In production, this would be sent to the user's email.
                    Toast.makeText(this@ForgotPasswordActivity, "OTP sent! (Demo OTP: $otp)", Toast.LENGTH_LONG).show()
                    
                    val intent = Intent(this@ForgotPasswordActivity, OtpVerificationActivity::class.java)
                    intent.putExtra("EMAIL", email)
                    startActivity(intent)
                }.onFailure { e ->
                    Toast.makeText(this@ForgotPasswordActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    sendCodeButton.isEnabled = true
                }
            }
        }

        findViewById<android.view.View>(R.id.back_button).setOnClickListener {
            finish()
        }
    }
}
