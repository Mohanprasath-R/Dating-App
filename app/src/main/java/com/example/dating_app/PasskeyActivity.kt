package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import com.datingapp.R
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
import androidx.compose.ui.tooling.preview.Preview
import com.example.dating_app.model.User
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class PasskeyActivity : ComponentActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
                        .background(Color(0xFF060008))
                ) {
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
    
    // Floating Hearts Background
    val heartParticles = remember {
        List(40) {
            HeartParticleData(
                x = (0..1000).random().toFloat(),
                y = (0..2000).random().toFloat(),
                size = (10..40).random().dp,
                speed = (6000..12000).random(),
                alpha = (0.05f + (0..30).random() / 100f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        heartParticles.forEach { HeartParticle(it) }

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
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {

                // Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "HeyDate",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        painter = painterResource(R.drawable.ic_heart),
                        contentDescription = null,
                        tint = Color(0xFFFF1493),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                HeartLock()

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Welcome Back",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Enter your PIN to unlock",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                PinIndicator(
                    passcode = passcode,
                    isError = isError,
                    shakeOffset = shakeOffset.value
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            NumericKeypad(
                onNumberClick = { number ->
                    if (passcode.length < 4 && !isError) {
                        passcode += number

                        if (passcode.length == 4) {
                            if (passcode == correctPasscode) {
                                onSuccess()
                            } else {
                                isError = true
                            }
                        }
                    }
                },
                onDeleteClick = {
                    if (passcode.isNotEmpty() && !isError) {
                        passcode = passcode.dropLast(1)
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            PasswordButton(
                onClick = onUsePasswordInstead
            )
        }
    }
}

@Composable
fun HeartLock() {
    Box(
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Icon(
            painter = painterResource(id = R.drawable.ic_heart),
            contentDescription = null,
            tint = Color(0xFFFF1493).copy(alpha = 0.15f),
            modifier = Modifier.size(160.dp)
        )

        // Middle glow
        Icon(
            painter = painterResource(id = R.drawable.ic_heart),
            contentDescription = null,
            tint = Color(0xFFFF1493).copy(alpha = 0.3f),
            modifier = Modifier.size(140.dp)
        )

        // Main heart
        Icon(
            painter = painterResource(id = R.drawable.ic_heart),
            contentDescription = null,
            tint = Color(0xFFFF1493),
            modifier = Modifier.size(100.dp)
        )

        // Keyhole
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )

            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 8.dp)
                    .background(Color.Black)
            )
        }
    }
}

@Composable
fun PinIndicator(
    passcode: String,
    isError: Boolean,
    shakeOffset: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeOffset.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(4) { index ->
            val filled = index < passcode.length
            val current = index == passcode.length

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.5.dp,
                        color = when {
                            isError -> Color.Red
                            filled || current -> Color(0xFFFF1493)
                            else -> Color.White.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (filled) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                Color.White,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordButton(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Color(0xFFFF1493).copy(alpha = .6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Fingerprint,
                null,
                tint = Color(0xFFFF1493)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "Use Password Instead",
                color = Color(0xFFFF1493),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        rows.forEach { row ->

            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                row.forEach { item ->

                    when (item) {

                        "" -> Spacer(modifier = Modifier.size(78.dp))

                        "⌫" ->
                            KeypadButton(
                                text = item,
                                onClick = onDeleteClick
                            )

                        else ->
                            KeypadButton(
                                text = item,
                                onClick = { onNumberClick(item) }
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        modifier = Modifier.size(76.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.07f)
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            if (text == "⌫") {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

            } else {

                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
fun HeartParticle(data: HeartParticleData) {
    val infiniteTransition = rememberInfiniteTransition(label = "heart")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -300f,
        animationSpec = infiniteRepeatable(
            animation = tween(data.speed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "move"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = data.alpha,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(data.speed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fade"
    )

    Icon(
        painter = painterResource(id = R.drawable.ic_heart),
        contentDescription = null,
        tint = Color(0xFFFF1493),
        modifier = Modifier
            .offset(x = data.x.dp, y = (data.y + offsetY).dp)
            .size(data.size)
            .alpha(alpha)
            .graphicsLayer {
                rotationZ = (data.x % 45)
            }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF050008)
@Composable
fun PasskeyScreenPreview() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050008))) {
        PasskeyScreen(correctPasscode = "1234", onSuccess = {}, onUsePasswordInstead = {})
    }
}

data class HeartParticleData(val x: Float, val y: Float, val size: androidx.compose.ui.unit.Dp, val speed: Int, val alpha: Float)
