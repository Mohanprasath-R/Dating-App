package com.example.dating_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class TwoFactorScreen {
    Main,
    ResetPin
}

class TwoFactorAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(TwoFactorScreen.Main) }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            TwoFactorScreen.Main -> TwoFactorMainScreen(
                                onBack = { finish() },
                                onResetPinClick = { currentScreen = TwoFactorScreen.ResetPin }
                            )
                            TwoFactorScreen.ResetPin -> ResetPinScreen(
                                onBack = { currentScreen = TwoFactorScreen.Main }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorMainScreen(onBack: () -> Unit, onResetPinClick: () -> Unit) {
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()
    var biometricEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.getUser(uid).onSuccess { user ->
                biometricEnabled = user?.biometric_enabled ?: false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Two-Factor Authentication", color = Color(0xFFFF1493), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Shield Icon with Glow
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF1493).copy(alpha = 0.15f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFEEF5)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFFF1493),
                            modifier = Modifier.size(40.dp)
                        )
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp).offset(y = (-2).dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Add an extra layer of security",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose how you want to protect your account",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // List Items
            TwoFactorOptionItem(
                title = "Reset PIN",
                subtitle = "Set a PIN to verify your identity",
                icon = Icons.Default.Lock,
                onClick = onResetPinClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TwoFactorOptionItem(
                title = "Biometric Authentication",
                subtitle = "Use fingerprint or face to login",
                icon = Icons.Default.Fingerprint,
                showSwitch = true,
                switchChecked = biometricEnabled,
                onSwitchChange = { enabled ->
                    biometricEnabled = enabled
                    scope.launch {
                        auth.currentUser?.uid?.let { uid ->
                            repository.updateProfile(uid, mapOf("biometric_enabled" to enabled))
                                .onSuccess {
                                    Toast.makeText(context, "Biometric ${if(enabled) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(context, "Failed to update biometric setting", Toast.LENGTH_SHORT).show()
                                    biometricEnabled = !enabled // Rollback on failure
                                }
                        }
                    }
                },
                onClick = { /* Already handled by switch */ }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Why info box
            Surface(
                color = Color(0xFFF3E5F5).copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF7E57C2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Why use two-factor authentication?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Two-factor authentication helps keep your account secure by requiring an additional verification step.",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(40.dp))
            
            // Bottom priority text
            Surface(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your security is our priority",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun TwoFactorOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    showSwitch: Boolean = false,
    switchChecked: Boolean = false,
    onSwitchChange: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFEEF5)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color(0xFFFF1493), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            
            if (showSwitch) {
                Switch(
                    checked = switchChecked,
                    onCheckedChange = onSwitchChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF1493),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPinScreen(onBack: () -> Unit) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isNewPinVisible by remember { mutableStateOf(false) }
    var isConfirmPinVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset PIN", color = Color(0xFFFF1493), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Pin Icon with Glow
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF1493).copy(alpha = 0.15f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFEEF5)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF1493),
                                modifier = Modifier.size(32.dp)
                            )
                            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(top = 2.dp)) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .padding(1.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF1493))
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Create a new PIN",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose a PIN you'll remember",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // PIN Input Fields
            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 6) newPin = it },
                placeholder = { Text("New PIN") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray) },
                trailingIcon = {
                    IconButton(onClick = { isNewPinVisible = !isNewPinVisible }) {
                        Icon(
                            if (isNewPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.LightGray
                        )
                    }
                },
                visualTransformation = if (isNewPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF1493),
                    unfocusedBorderColor = Color(0xFFF3F4F6)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 6) confirmPin = it },
                placeholder = { Text("Confirm PIN") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray) },
                trailingIcon = {
                    IconButton(onClick = { isConfirmPinVisible = !isConfirmPinVisible }) {
                        Icon(
                            if (isConfirmPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.LightGray
                        )
                    }
                },
                visualTransformation = if (isConfirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF1493),
                    unfocusedBorderColor = Color(0xFFF3F4F6)
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Guidelines info box
            Surface(
                color = Color(0xFFF3E5F5).copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF7E57C2),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "PIN Guidelines",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GuidelineItem("PIN must be at least 4 digits")
                    GuidelineItem("Avoid using easy number combinations")
                    GuidelineItem("Don't use your birth date or phone number")
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(40.dp))
            
            // Save Button
            Button(
                onClick = {
                    if (newPin.length < 4) {
                        Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPin != confirmPin) {
                        Toast.makeText(context, "PINs do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            repository.updateUserPin(userId, newPin)
                                .onSuccess {
                                    Toast.makeText(context, "PIN saved successfully", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .onFailure {
                                    Toast.makeText(context, "Failed to save PIN: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save PIN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GuidelineItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF7E57C2),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 13.sp, color = Color.DarkGray)
    }
}
