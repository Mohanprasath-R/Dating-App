package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        val email = intent.getStringExtra("EMAIL") ?: ""
        val otpInput = findViewById<EditText>(R.id.otp_input)
        val verifyButton = findViewById<Button>(R.id.verify_button)
        val resendText = findViewById<TextView>(R.id.resend_text)

        verifyButton.setOnClickListener {
            val otp = otpInput.text.toString().trim()

            if (otp.length != 6) {
                Toast.makeText(this, "Please enter 6-digit code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                verifyButton.isEnabled = false
                val result = repository.verifyOtp(email, otp)
                
                result.onSuccess {
                    val intent = Intent(this@OtpVerificationActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("EMAIL", email)
                    startActivity(intent)
                    finish()
                }.onFailure { e ->
                    Toast.makeText(this@OtpVerificationActivity, e.message, Toast.LENGTH_SHORT).show()
                    verifyButton.isEnabled = true
                }
            }
        }

        resendText.setOnClickListener {
            lifecycleScope.launch {
                repository.sendOtp(email).onSuccess {
                    Toast.makeText(this@OtpVerificationActivity, "Code resent! (Demo OTP: $it)", Toast.LENGTH_LONG).show()
                }
            }
        }

        findViewById<android.view.View>(R.id.back_button).setOnClickListener {
            finish()
        }
    }
}
