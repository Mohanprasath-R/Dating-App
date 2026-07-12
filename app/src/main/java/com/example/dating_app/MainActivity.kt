package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.dating_app.util.CloudinaryHelper
import com.datingapp.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)
        
        try {
            CloudinaryHelper.init(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Cloudinary Init Failed", e)
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                HeartSplashScreen(onAnimationFinished = {
                    val auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser
                    
                    if (currentUser != null) {
                        // User is logged in, always show PIN screen on app open
                        startActivity(Intent(this, PasskeyActivity::class.java))
                    } else {
                        // No session, show Onboarding
                        startActivity(Intent(this, OnboardingActivity::class.java))
                    }
                    finish()
                })
            }
        }
    }
}

@Composable
fun HeartSplashScreen(onAnimationFinished: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val heyDateAlpha = remember { Animatable(0f) }
    
    // Bokeh particles state
    val bokehs = remember {
        List(15) {
            BokehData(
                x = (0..1000).random().toFloat(),
                y = (0..2000).random().toFloat(),
                size = (100..250).random().dp,
                color = if (it % 2 == 0) Color(0xFFFF1493).copy(alpha = 0.15f) else Color(0xFF9C27B0).copy(alpha = 0.1f)
            )
        }
    }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(1f, tween(1000))
            delay(2500)
            alpha.animateTo(0f, tween(800))
        }

        launch {
            // Smooth reveal
            scale.animateTo(1.1f, tween(1200, easing = FastOutSlowInEasing))
            
            // Subtle "Breathing" Pulse
            launch {
                while(true) {
                    scale.animateTo(1.05f, tween(2000, easing = LinearOutSlowInEasing))
                    scale.animateTo(1.1f, tween(2000, easing = LinearOutSlowInEasing))
                }
            }

            delay(2800)
            // Elegant "Zoom Through" into the app with Name reveal
            launch {
                heyDateAlpha.animateTo(1f, tween(400))
            }
            scale.animateTo(25f, tween(1200, easing = FastOutLinearInEasing))
            onAnimationFinished()
        }

        launch {
            // Elegant slow rotation
            rotationY.animateTo(360f, tween(4000, easing = LinearEasing))
        }
        
        launch {
            delay(500)
            contentAlpha.animateTo(1f, tween(1000))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050008)),
        contentAlignment = Alignment.Center
    ) {
        // Soft Bokeh Background
        bokehs.forEach { bokeh ->
            AnimatedBokeh(bokeh)
        }

        // Central Composed Heart
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = contentAlpha.value
                    this.scaleX = scale.value
                    this.scaleY = scale.value
                    this.rotationY = rotationY.value
                    cameraDistance = 12f * density
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Massive Soft Glow
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF1493).copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )

            // 2. The Core Heart Layers
            Icon(
                painter = painterResource(id = R.drawable.ic_heart),
                contentDescription = null,
                tint = Color(0xFFFF1493).copy(alpha = 0.2f),
                modifier = Modifier.size(160.dp).scale(1.2f)
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_heart),
                contentDescription = null,
                tint = Color(0xFFFF1493),
                modifier = Modifier
                    .size(130.dp)
                    .shadow(20.dp, CircleShape, spotColor = Color(0xFFFF1493))
            )

            // 3. Glassmorphic Highlight
            Icon(
                painter = painterResource(id = R.drawable.ic_heart),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        this.rotationY = 35f
                        this.translationX = 8f
                        this.translationY = -4f
                        this.alpha = 0.6f
                    }
            )

            // 4. App Name Revealed Inside
            Text(
                text = "HeyDate",
                color = Color.White,
                fontSize = 6.sp, // Scales up with the heart
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .alpha(heyDateAlpha.value)
                    .graphicsLayer {
                        // Keep text flat even when heart rotates slightly
                        this.rotationY = -rotationY.value
                    }
            )
        }

        // Elegant Minimalist Branding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "D A T I N G",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFFFF1493), Color.Transparent)
                        )
                    )
            )
        }
    }
}

data class BokehData(val x: Float, val y: Float, val size: androidx.compose.ui.unit.Dp, val color: Color)

@Composable
fun AnimatedBokeh(data: BokehData) {
    val infiniteTransition = rememberInfiniteTransition(label = "bokeh")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "move"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fade"
    )

    Box(
        modifier = Modifier
            .offset(x = data.x.dp, y = (data.y + offsetY).dp)
            .size(data.size)
            .alpha(alpha)
            .blur(50.dp)
            .background(data.color, CircleShape)
    )
}
