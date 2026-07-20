package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.datingapp.R
import com.example.dating_app.model.UserDevice
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity

class LoginActivity : ComponentActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginClick = { email, password, onComplete ->
                        lifecycleScope.launch {
                            val result = repository.loginUser(email, password)
                            result.onSuccess { uid ->
                                val userResult = repository.getUser(uid)
                                userResult.onSuccess { user ->
                                    if (user != null) {
                                        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                                        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                                        val userDevice = UserDevice(deviceId, deviceName)
                                        repository.logLogin(uid, userDevice)
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
                                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    onComplete()
                                }
                            }.onFailure { e ->
                                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                                onComplete()
                            }
                        }
                    },
                    onSignupClick = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    },
                    onForgotPasswordClick = {
                        startActivity(Intent(this, ForgotPasswordActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginClick: (String, String, () -> Unit) -> Unit,
    onSignupClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val pinkGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF0F5), Color.White)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pinkGradient)
    ) {
        // Top Wave Blob
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.TopCenter)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width, 0f)
                    lineTo(width, height * 0.5f)
                    quadraticBezierTo(width * 0.75f, height * 0.8f, width * 0.5f, height * 0.5f)
                    quadraticBezierTo(width * 0.25f, height * 0.2f, 0f, height * 0.6f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF1493).copy(alpha = 0.15f), Color.Transparent)
                    )
                )
            }
        }

        // Floating Hearts Background
        LoginFloatingHeartsBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(30.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App Name
            Text(
                text = "HeyDate",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF2D6C), Color(0xFF8E24AA))
                    )
                ),
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
            )

            Text(
                text = "Welcome back! 👋",
                fontSize = 17.sp,
                color = Color.DarkGray.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(15.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Email Address", color = Color.Gray.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFFF2D6C))
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF2D6C).copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        isError = emailError,
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFDFDFD),
                            unfocusedContainerColor = Color(0xFFFDFDFD),
                            focusedBorderColor = Color(0xFFFF2D6C).copy(alpha = 0.1f),
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.2f),
                            errorBorderColor = Color.Red
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Password", color = Color.Gray.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF2D6C))
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray.copy(alpha = 0.6f)
                                )
                            }
                        },
                        isError = passwordError,
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFDFDFD),
                            unfocusedContainerColor = Color(0xFFFDFDFD),
                            focusedBorderColor = Color(0xFFFF2D6C).copy(alpha = 0.1f),
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.2f),
                            errorBorderColor = Color.Red
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Forgot Password
                    Text(
                        text = "Forgot Password?",
                        color = Color(0xFFFF2D6C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { onForgotPasswordClick() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    SwipeToLoginButton(
                        isLoading = isLoading,
                        onSwipeComplete = {
                            emailError = email.isEmpty()
                            passwordError = password.isEmpty()
                            
                            if (!emailError && !passwordError) {
                                isLoading = true
                                onLoginClick(email, password) { isLoading = false }
                            } else {
                                Toast.makeText(context, "Email and Password are required", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Sign Up Footer
            Row {
                Text(text = "Don't have an account? ", color = Color.Gray)
                Text(
                    text = "Sign Up",
                    color = Color(0xFFFF1493),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSignupClick() }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Bottom Pink Waves/Gradient Effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.15f)
                .align(Alignment.BottomCenter)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, height * 0.4f)
                    quadraticBezierTo(width * 0.25f, height * 0.1f, width * 0.5f, height * 0.4f)
                    quadraticBezierTo(width * 0.75f, height * 0.7f, width, height * 0.3f)
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF1493).copy(alpha = 0.1f), Color(0xFFFF1493).copy(alpha = 0.4f))
                    )
                )
            }
        }
    }
}

@Composable
fun LoginFloatingHeartsBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "hearts")
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Top left wave blob
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .alpha(0.1f)
                .background(Color(0xFFFF1493), CircleShape)
        )

        repeat(6) { i ->
            val duration = 6000 + (i * 800)
            val delay = i * 1000
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = duration
                        delayMillis = delay
                        0f at 0
                        0.2f at duration / 2
                        0f at duration
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha"
            )
            
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = -0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, delay, LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "yOffset"
            )

            val xPos = (i * 173) % 100 / 100f
            
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF1493).copy(alpha = alpha),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = size.width * xPos
                        translationY = size.height * yOffset
                        scaleX = 0.6f + (i % 2) * 0.3f
                        scaleY = 0.6f + (i % 2) * 0.3f
                    }
                    .size(24.dp)
            )
        }
    }
}

@Composable
fun SwipeToLoginButton(
    isLoading: Boolean,
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var width by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val thumbSize = 64.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offset"
    )

    // Calculate progress (0.0 to 1.0)
    val progress = if (width > 0) (offsetX / (width - thumbSizePx - 8f)).coerceIn(0f, 1f) else 0f

    // Dynamic background color based on progress
    val backgroundColor1 = lerp(Color(0xFFFA2E69), Color(0xFF8E24AA), progress)
    val backgroundColor2 = lerp(Color(0xFFC71585), Color(0xFFFF1493), progress)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFFFF2D6C))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(backgroundColor1, backgroundColor2)
                )
            )
            .onGloballyPositioned { width = it.size.width },
        contentAlignment = Alignment.CenterStart
    ) {
        // Background Text - Fades out as you swipe
        Text(
            text = if (isLoading) "" else "Swipe to Login",
            color = Color.White.copy(alpha = (0.8f - progress).coerceAtLeast(0f)),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
            )
        } else {
            // Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                    .size(64.dp)
                    .padding(4.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                val threshold = width - thumbSizePx - 40f
                                if (offsetX >= threshold) {
                                    onSwipeComplete()
                                }
                                offsetX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Slow down the drag by multiplying dragAmount.x (e.g., * 0.8f)
                                val maxOffset = width.toFloat() - thumbSizePx - 8f
                                offsetX = (offsetX + dragAmount.x * 0.9f).coerceIn(0f, maxOffset)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = lerp(Color(0xFFFF2D6C), Color(0xFF8E24AA), progress),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Helper function to interpolate colors
fun lerp(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
}
