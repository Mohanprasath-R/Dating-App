package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datingapp.R
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class OnboardingActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScreen(
                onFinish = {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
        }
    }
}

enum class IllustrationType {
    CARD_STACK, HEART_ORBIT, SHIELD_ORBIT
}

data class OnboardingPage(
    val titlePart1: String,
    val titlePart2: String,
    val description: String,
    val type: IllustrationType,
    val mainImage: Int = R.drawable.girl,
    val features: List<OnboardingFeature> = emptyList()
)

data class OnboardingFeature(
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            "Find your",
            "perfect match",
            "Discover amazing people and build meaningful connections.",
            IllustrationType.CARD_STACK,
            R.drawable.girl,
            listOf(
                OnboardingFeature(Icons.Default.Favorite, "Smart Matches"),
                OnboardingFeature(Icons.Default.ChatBubble, "Easy Chat"),
                OnboardingFeature(Icons.Default.Security, "Safe & Secure")
            )
        ),
        OnboardingPage(
            "It's all about",
            "connections",
            "Like, match and chat with people who share your interests.",
            IllustrationType.HEART_ORBIT
        ),
        OnboardingPage(
            "Your safety is",
            "our priority",
            "We keep your data safe and create a sequential space for everyone.",
            IllustrationType.SHIELD_ORBIT
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF5F7), Color.White),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { position ->
                OnboardingPageContent(pages[position])
            }

            // Pager Indicator
            Row(
                Modifier
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFFFF2D6C) else Color.LightGray.copy(alpha = 0.5f)
                    val size by animateDpAsState(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(size)
                    )
                }
            }

            // Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5F6D), Color(0xFFFF2D6C))
                        )
                    )
                    .clickable {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage == pages.size - 1) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }

                    Text(
                        text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Decorative floral pattern at bottom
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with floral resource
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(60.dp).graphicsLayer { alpha = 0.3f },
                    contentScale = ContentScale.FillWidth
                )
                
                Row(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .clickable { onFinish() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Already have an account? ", color = Color.Gray, fontSize = 14.sp)
                    Text(text = "Log in", color = Color(0xFFFF2D6C), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Illustration Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            when (page.type) {
                IllustrationType.CARD_STACK -> CardStackIllustration(page.mainImage)
                IllustrationType.HEART_ORBIT -> FloatingOrbitIllustration(Icons.Default.Favorite, Color(0xFFFF2D6C))
                IllustrationType.SHIELD_ORBIT -> FloatingOrbitIllustration(Icons.Default.Shield, Color(0xFFFF2D6C), isShield = true)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title with Outline Heart icon
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF111827), fontWeight = FontWeight.Bold, fontSize = 36.sp)) {
                        append(page.titlePart1 + "\n")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFFFF2D6C), fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)) {
                        append(page.titlePart2)
                    }
                },
                textAlign = TextAlign.Center,
                lineHeight = 44.sp
            )
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = Color(0xFFFF2D6C),
                modifier = Modifier.size(32.dp).offset(x = 4.dp, y = 30.dp).rotate(15f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = page.description,
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 56.dp)
        )

        if (page.type == IllustrationType.CARD_STACK) {
            Spacer(modifier = Modifier.weight(1f))
            FeatureRow(page.features)
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CardStackIllustration(mainImage: Int) {
    Box(contentAlignment = Alignment.Center) {
        // Background Cards
        listOf(-10f to -60f, 10f to 60f).forEach { (rot, transX) ->
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .height(300.dp)
                    .graphicsLayer {
                        translationX = transX
                        rotationZ = rot
                    },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.girl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f }
                )
            }
        }

        // Main Card
        Card(
            modifier = Modifier
                .width(240.dp)
                .height(340.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box {
                Image(
                    painter = painterResource(id = mainImage),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Bottom info overlay... (omitted for brevity, assume same as before)
            }
        }
    }
}

@Composable
fun FloatingOrbitIllustration(mainIcon: ImageVector, color: Color, isShield: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "angle"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
        // Orbit Circle
        Box(
            modifier = Modifier
                .size(260.dp)
                .border(1.dp, Color.LightGray.copy(alpha = 0.3f), CircleShape)
        )

        // Main 3D-like Icon
        Surface(
            modifier = Modifier.size(160.dp).scale(1.1f),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 20.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(Color.White, color.copy(alpha = 0.05f)))
            )) {
                if (isShield) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = color, modifier = Modifier.size(100.dp))
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                } else {
                    Icon(mainIcon, contentDescription = null, tint = color, modifier = Modifier.size(100.dp))
                }
            }
        }

        // Floating Icons
        val floatingIcons = if (isShield) {
            listOf(Icons.Default.Person, Icons.Default.VerifiedUser, Icons.Default.Lock, Icons.Default.Chat)
        } else {
            listOf(Icons.Default.ChatBubble, Icons.Default.Favorite, Icons.Default.People)
        }

        floatingIcons.forEachIndexed { index, icon ->
            val currentAngle = (angle + (index * (360 / floatingIcons.size))) % 360
            val x = (130 * cos(Math.toRadians(currentAngle.toDouble()))).dp
            val y = (130 * sin(Math.toRadians(currentAngle.toDouble()))).dp

            Surface(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(48.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun FeatureRow(features: List<OnboardingFeature>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        features.forEach { feature ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(feature.icon, contentDescription = null, tint = Color(0xFFFF2D6C), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = feature.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
            }
        }
    }
}


