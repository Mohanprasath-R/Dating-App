package com.example.dating_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme

class PartnerProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val userId = intent.getStringExtra("USER_ID") ?: ""
        
        if (userId.isEmpty()) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                UserProfileScreen(
                    targetUserId = userId,
                    onBack = { finish() }
                )
            }
        }
    }
}
