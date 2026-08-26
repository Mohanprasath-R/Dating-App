package com.example.dating_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dating_app.model.User
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class NotificationsSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                NotificationsSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()
    
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.getUser(uid).onSuccess { fetchedUser ->
                user = fetchedUser
            }
        }
        isLoading = false
    }

    val updateNotifications: (String, Boolean) -> Unit = { field, enabled ->
        scope.launch {
            auth.currentUser?.uid?.let { uid ->
                repository.updateProfile(uid, mapOf(field to enabled)).onSuccess {
                    user = when(field) {
                        "push_enabled" -> user?.copy(push_enabled = enabled)
                        "email_enabled" -> user?.copy(email_enabled = enabled)
                        "matches_notif" -> user?.copy(matches_notif = enabled)
                        "messages_notif" -> user?.copy(messages_notif = enabled)
                        else -> user
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = Color(0xFFFF1493), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF1493))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF9FAFB))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    "Push Notifications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    val push = user?.push_enabled ?: true
                    Column {
                        NotificationToggleItem("Allow Push Notifications", push) { updateNotifications("push_enabled", it) }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        NotificationToggleItem("New Matches", (user?.matches_notif ?: true) && push) { updateNotifications("matches_notif", it) }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        NotificationToggleItem("New Messages", (user?.messages_notif ?: true) && push) { updateNotifications("messages_notif", it) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Email Notifications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    NotificationToggleItem("Promotional Emails", user?.email_enabled ?: false) { updateNotifications("email_enabled", it) }
                }
            }
        }
    }
}

@Composable
fun NotificationToggleItem(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label, fontWeight = FontWeight.Medium) },
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFFF1493)
                )
            )
        },
        modifier = Modifier.background(Color.White)
    )
}
