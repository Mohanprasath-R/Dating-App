package com.example.dating_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dating_app.model.User
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AccountSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AccountSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(onBack: () -> Unit) {
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Settings", color = Color(0xFFFF1493), fontWeight = FontWeight.Bold) },
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
                AccountSection("Personal Information") {
                    AccountItem("Username", user?.username ?: "Not set", Icons.Default.Person)
                    AccountItem("Email", user?.email ?: "", Icons.Default.Email)
                    AccountItem("Phone", user?.phone ?: "Not set", Icons.Default.Phone)
                }

                Spacer(modifier = Modifier.height(24.dp))

                AccountSection("Security") {
                    AccountItem("Password", "••••••••", Icons.Default.Lock, onClick = {
                        // Navigate to Change Password
                    })
                    AccountItem("Two-Factor Auth", if (user?.pin?.isNotEmpty() == true) "Enabled" else "Disabled", Icons.Default.VpnKey)
                }

                Spacer(modifier = Modifier.height(24.dp))

                AccountSection("Account Actions") {
                    ListItem(
                        headlineContent = { Text("Deactivate Account", color = Color.Gray) },
                        leadingContent = { Icon(Icons.Default.PauseCircle, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.background(Color.White)
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    ListItem(
                        headlineContent = { Text("Delete Account", color = Color.Red, fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier.background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
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
            Column {
                content()
            }
        }
    }
}

@Composable
fun AccountItem(label: String, value: String, icon: ImageVector, onClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(label, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(value, color = Color.Gray) },
        leadingContent = { 
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = Color(0xFFFFEEF5)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color(0xFFFF1493), modifier = Modifier.size(18.dp))
                }
            }
        },
        trailingContent = { if (onClick != null) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
        modifier = Modifier.background(Color.White).let { if (onClick != null) it.clickable(onClick = onClick) else it }
    )
}
