package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dating_app.model.User
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class PasskeyActivity : ComponentActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                var userPin by remember { mutableStateOf<String?>(null) }
                var currentUserData by remember { mutableStateOf<User?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    repository.getUser(currentUser.uid).onSuccess { user ->
                        if (user?.pin.isNullOrEmpty()) {
                            val intent = Intent(this@PasskeyActivity, SetPinActivity::class.java)
                            intent.putExtra("USER_ID", currentUser.uid)
                            startActivity(intent)
                            finish()
                        } else {
                            userPin = user?.pin
                            currentUserData = user
                        }
                    }.onFailure {
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this@PasskeyActivity, LoginActivity::class.java))
                        finish()
                    }
                    isLoading = false
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF050008))
                ) {
                    // Reuse Bokeh Background from MainActivity
                    val bokehs = remember {
                        List(10) {
                            PasskeyBokehData(
                                x = (0..1000).random().toFloat(),
                                y = (0..2000).random().toFloat(),
                                size = (100..250).random().dp,
                                color = if (it % 2 == 0) Color(0xFFFF1493).copy(alpha = 0.15f) else Color(0xFF9C27B0).copy(alpha = 0.1f)
                            )
                        }
                    }
                    bokehs.forEach { PasskeyAnimatedBokeh(it) }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFF1493))
                        }
                    } else if (userPin != null) {
                        PasskeyScreen(
                            correctPasscode = userPin!!,
                            onSuccess = {
                                // Initialization after successful PIN entry
                                currentUserData?.let { user ->
                                    (application as MyApplication).initZegoService(user.id, "${user.first_name} ${user.last_name}")
                                }
                                
                                startActivity(Intent(this@PasskeyActivity, HomeActivity::class.java))
                                finish()
                            },
                            onUsePasswordInstead = {
                                FirebaseAuth.getInstance().signOut()
                                startActivity(Intent(this@PasskeyActivity, LoginActivity::class.java))
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PasskeyScreen(
    correctPasscode: String,
    onSuccess: () -> Unit,
    onUsePasswordInstead: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    // Shake animation for error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            repeat(4) {
                shakeOffset.animateTo(20f, tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-20f, tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, tween(50, easing = LinearEasing))
            delay(1000)
            isError = false
            passcode = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color(0xFFFF1493),
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFFF1493).copy(alpha = 0.1f), CircleShape)
                .padding(16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome Back",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter your PIN to unlock",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Visual representation of the 4 digits
        Row(
            modifier = Modifier.offset(x = shakeOffset.value.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isFilled = index < passcode.length
                val dotScale by animateFloatAsState(
                    targetValue = if (isFilled) 1f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dot"
                )
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isError) Color.Red.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 2.dp,
                            color = when {
                                isError -> Color.Red
                                isFilled -> Color(0xFFFF1493)
                                else -> Color.White.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(if (isError) Color.Red else Color(0xFFFF1493))
                    )
                }
            }
        }

        if (isError) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Incorrect PIN, please try again",
                color = Color.Red.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        NumericKeypad(onNumberClick = { num ->
            if (passcode.length < 4 && !isError) {
                passcode += num
                if (passcode.length == 4) {
                    if (passcode == correctPasscode) {
                        onSuccess()
                    } else {
                        isError = true
                    }
                }
            }
        }, onDeleteClick = {
            if (passcode.isNotEmpty() && !isError) {
                passcode = passcode.dropLast(1)
            }
        })

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = { onUsePasswordInstead() }) {
            Text(
                text = "Use Password Instead",
                color = Color(0xFFFF1493),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NumericKeypad(onNumberClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        numbers.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                row.forEach { char ->
                    if (char.isEmpty()) {
                        Spacer(modifier = Modifier.size(72.dp))
                    } else {
                        KeypadButton(
                            text = char,
                            onClick = {
                                if (char == "⌫") onDeleteClick()
                                else onNumberClick(char)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.05f),
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun PasskeyAnimatedBokeh(data: PasskeyBokehData) {
    val infiniteTransition = rememberInfiniteTransition(label = "bokeh")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -150f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "move"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fade"
    )

    Box(
        modifier = Modifier
            .offset(x = data.x.dp, y = (data.y + offsetY).dp)
            .size(data.size)
            .alpha(alpha)
            .blur(60.dp)
            .background(data.color, CircleShape)
    )
}

data class PasskeyBokehData(val x: Float, val y: Float, val size: androidx.compose.ui.unit.Dp, val color: Color)
