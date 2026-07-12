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

class SetPinActivity : AppCompatActivity() {
    private val repository = FirebaseRepository()
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_pin)

        userId = intent.getStringExtra("USER_ID") ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val pinInput = findViewById<EditText>(R.id.pin_input)
        val confirmPinInput = findViewById<EditText>(R.id.confirm_pin_input)
        val savePinButton = findViewById<Button>(R.id.save_pin_button)

        savePinButton.setOnClickListener {
            val pin = pinInput.text.toString()
            val confirmPin = confirmPinInput.text.toString()

            if (pin.length != 4) {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin != confirmPin) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                savePinButton.isEnabled = false
                val result = repository.updateUserPin(userId, pin)
                
                result.onSuccess {
                    Toast.makeText(this@SetPinActivity, "PIN saved successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@SetPinActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }.onFailure { e ->
                    Toast.makeText(this@SetPinActivity, "Failed to save PIN: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    savePinButton.isEnabled = true
                }
            }
        }

        findViewById<android.view.View>(R.id.back_button).setOnClickListener {
            finish()
        }
    }
}