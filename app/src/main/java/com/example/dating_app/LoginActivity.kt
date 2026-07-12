package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import android.provider.Settings
import com.example.dating_app.model.UserDevice
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val loginButton = findViewById<Button>(R.id.login_button)
        val passwordToggle = findViewById<ImageView>(R.id.password_toggle)
        var isPasswordVisible = false

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordInput.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                passwordToggle.setImageResource(android.R.drawable.ic_menu_view) // You can use a different icon for "hide"
            } else {
                passwordInput.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                passwordToggle.setImageResource(android.R.drawable.ic_menu_view)
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        findViewById<TextView>(R.id.signup_link).setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                loginButton.isEnabled = false
                val result = repository.loginUser(email, password)
                
                result.onSuccess { uid ->
                    val userResult = repository.getUser(uid)
                    userResult.onSuccess { user ->
                        if (user != null) {
                            // Security: Log Device and History
                            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                            val userDevice = UserDevice(deviceId, deviceName)
                            
                            val logResult = repository.logLogin(uid, userDevice)
                            logResult.onSuccess { isNewDevice ->
                                if (isNewDevice) {
                                    Toast.makeText(this@LoginActivity, "New device login detected!", Toast.LENGTH_LONG).show()
                                }
                            }

                            // Force JWT (ID Token) Refresh
                            FirebaseAuth.getInstance().currentUser?.getIdToken(true)

                            (application as MyApplication).initZegoService(uid, "${user.first_name} ${user.last_name}")
                        }
                        if (user?.pin.isNullOrEmpty()) {
                            val intent = Intent(this@LoginActivity, SetPinActivity::class.java)
                            intent.putExtra("USER_ID", uid)
                            startActivity(intent)
                        } else {
                            startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        }
                        finish()
                    }.onFailure { e ->
                        Toast.makeText(this@LoginActivity, "Error fetching profile: ${e.message}", Toast.LENGTH_LONG).show()
                        loginButton.isEnabled = true
                    }
                }.onFailure { e ->
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                    loginButton.isEnabled = true
                }
            }
        }

        findViewById<TextView>(R.id.forgot_password).setOnClickListener {
            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
        }

        findViewById<android.view.View>(R.id.back_button).setOnClickListener {
            finish()
        }
    }
}