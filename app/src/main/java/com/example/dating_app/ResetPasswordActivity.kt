package com.example.dating_app

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import kotlinx.coroutines.launch

class ResetPasswordActivity : ComponentActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val email = intent.getStringExtra("EMAIL") ?: ""
        
        setContent {
            MaterialTheme {
                var isSuccess by remember { mutableStateOf(false) }
                
                Crossfade(targetState = isSuccess, label = "ScreenTransition") { success ->
                    if (success) {
                        PasswordResetSuccessScreen(
                            onGoToLogin = {
                                val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        )
                    } else {
                        CreateNewPasswordScreen(
                            onBack = { finish() },
                            onReset = { newPassword ->
                                lifecycleScope.launch {
                                    repository.resetPassword(email, newPassword).onSuccess {
                                        isSuccess = true
                                    }.onFailure { e ->
                                        Toast.makeText(this@ResetPasswordActivity, e.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewPasswordScreen(onBack: () -> Unit, onReset: (String) -> Unit) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val hasMinLength = newPassword.length >= 8
    val hasUpperLower = newPassword.any { it.isUpperCase() } && newPassword.any { it.isLowerCase() }
    val hasNumberSpecial = newPassword.any { it.isDigit() } && newPassword.any { !it.isLetterOrDigit() }

    val strengthProgress = listOf(hasMinLength, hasUpperLower, hasNumberSpecial).count { it }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFFFFF5F8), Color.White)))
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(16.dp).size(40.dp).background(Color.White, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Illustration
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(180.dp).background(Brush.radialGradient(colors = listOf(Color(0xFFFF1493).copy(alpha = 0.1f), Color.Transparent)), CircleShape))
                Box(contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.size(120.dp), color = Color.White.copy(0.1f), shape = CircleShape) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Shield, null, tint = Color(0xFFFF1493).copy(0.1f), modifier = Modifier.size(100.dp))
                        }
                    }
                    Surface(modifier = Modifier.size(70.dp), color = Color(0xFFFF1493), shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                    Surface(modifier = Modifier.size(28.dp).offset(x = 35.dp, y = 20.dp), color = Color(0xFFFF1493), shape = CircleShape, border = BorderStroke(2.dp, Color.White)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Create ", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A1A1A))
                Text(text = "New Password", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF1493))
            }
            Text(text = "Your new password must be different\nfrom previous passwords.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(32.dp))

            // Inputs Card
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFF3F4F6))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PasswordInputField(label = "New Password", value = newPassword, onValueChange = { newPassword = it }, isVisible = isNewPasswordVisible, onToggle = { isNewPasswordVisible = !isNewPasswordVisible })
                    
                    // Strength Bars
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { index ->
                            Box(modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (index < strengthProgress) Color(0xFF4CAF50) else Color(0xFFE0E0E0)))
                        }
                        Text(text = when(strengthProgress) { 1 -> "Weak"; 2 -> "Medium"; 3 -> "Strong"; else -> "" }, fontSize = 10.sp, color = if(strengthProgress == 3) Color(0xFF4CAF50) else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    
                    PasswordInputField(label = "Confirm Password", value = confirmPassword, onValueChange = { confirmPassword = it }, isVisible = isConfirmPasswordVisible, onToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Requirements
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RequirementItem(text = "At least 8 characters", isMet = hasMinLength)
                RequirementItem(text = "Include uppercase & lowercase letters", isMet = hasUpperLower)
                RequirementItem(text = "Include number and special character", isMet = hasNumberSpecial)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { if (newPassword == confirmPassword && strengthProgress == 3) onReset(newPassword) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(Color(0xFFFF2D6C), Color(0xFFFF1493)))), contentAlignment = Alignment.Center) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Text("Reset Password", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PasswordInputField(label: String, value: String, onValueChange: (String) -> Unit, isVisible: Boolean, onToggle: () -> Unit) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Color(0xFFFF1493), fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = Color(0xFFFF1493).copy(0.4f), modifier = Modifier.size(20.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = onToggle) { Icon(if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color(0xFFFF1493).copy(0.4f)) } },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent),
                singleLine = true
            )
        }
    }
}

@Composable
fun RequirementItem(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = if (isMet) Color(0xFF4CAF50) else Color(0xFFE0E0E0), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 13.sp, color = if (isMet) Color.Black else Color.Gray)
    }
}

@Composable
fun PasswordResetSuccessScreen(onGoToLogin: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    
    // Checkmark animation
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(1f, tween(500))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFFFFF5F8), Color.White))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success Illustration with Animation
            Box(contentAlignment = Alignment.Center) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(scale.value)
                        .alpha(alpha.value * 0.1f)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                
                // Animated Confetti
                repeat(15) { i ->
                    val angle = (i * 24).toDouble()
                    val drift by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "drift"
                    )
                    
                    val distance = (85 + (i % 3) * 10).dp
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (Math.cos(Math.toRadians(angle)) * distance.value).dp + drift.dp,
                                y = (Math.sin(Math.toRadians(angle)) * distance.value).dp - drift.dp
                            )
                            .size((4..8).random().dp)
                            .clip(CircleShape)
                            .background(
                                when(i % 4) {
                                    0 -> Color(0xFFFF1493)
                                    1 -> Color(0xFF4CAF50)
                                    2 -> Color(0xFF2196F3)
                                    else -> Color(0xFFFFB300)
                                }
                            )
                            .alpha(alpha.value)
                    )
                }

                // The Tick Icon with Scale/Bounce
                Surface(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(scale.value)
                        .alpha(alpha.value),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(4.dp, Color(0xFFF3F4F6))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Password Reset",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = "Successful!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF1493)
            )
            Text(
                text = "Your password has been reset successfully.\nYou can now login with your new password.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color(0xFFFFEEF5)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = Color(0xFFFF1493), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Your account is now more secure", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "We recommend keeping your password\nprivate and updated.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onGoToLogin,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(Color(0xFFFF2D6C), Color(0xFFFF1493)))), contentAlignment = Alignment.Center) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Text("Go to Login", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
                    }
                }
            }
        }
    }
}
