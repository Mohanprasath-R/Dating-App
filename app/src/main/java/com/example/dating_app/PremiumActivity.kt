package com.example.dating_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class PremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PremiumScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
fun PremiumScreen(onClose: () -> Unit) {
    var selectedPlan by remember { mutableIntStateOf(1) } // 0: 1 Month, 1: 6 Months, 2: 12 Months

    // No scrolling: everything is laid out in normal flow so the whole
    // screen (header, hero, benefits, pricing, CTA) always fits on screen.
    // The middle block gets the remaining space and centers its content,
    // so it compresses gracefully on shorter screens instead of clipping.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header: handle + close
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 20.dp)

        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
            }
        }
        // Middle content: takes up remaining space, centered vertically
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with Crown
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF2D6C).copy(alpha = 0.1f), Color.Transparent)
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFF2D6C),
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopStart)
                        .offset(x = (-3).dp, y = 3.dp)
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFF2D6C),
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-1).dp)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF69B4), Color(0xFFFF2D6C))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(35.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Upgrade to",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Premium",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                style = LocalTextStyle.current.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFC2C5A), Color(0xFFC71585))
                    )
                )
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Get more matches and unlock\nexciting features",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(35.dp))

            // Benefits Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, Color(0xFFF3F4F6))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    BenefitItem("Unlimited Likes", "Like as many people as you want", Icons.Default.Favorite)
                    BenefitItem("See who likes you", "See everyone who likes you", Icons.Default.Visibility)
                    BenefitItem("Boost your profile", "Get more visibility & more matches", Icons.Default.RocketLaunch)
                    BenefitItem("No ads", "Enjoy ad-free experience", Icons.Default.Block)
                }
            }

            Spacer(modifier = Modifier.height(35.dp))

            // Pricing Plans — height(IntrinsicSize.Min) makes the row exactly
            // as tall as its tallest child (the "Most Popular" column with its
            // badge), and each card fillMaxHeight()s to match. This auto-sizes
            // to whatever the text actually needs, so nothing gets clipped.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PricingPlan(
                    duration = "1 Month",
                    price = "\u20b9599",
                    savings = "Save 21%",
                    isSelected = selectedPlan == 0,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { selectedPlan = 0 }
                )
                PricingPlan(
                    duration = "6 Months",
                    price = "\u20b9379",
                    savings = "Save 40%",
                    isSelected = selectedPlan == 1,
                    isPopular = true,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { selectedPlan = 1 }
                )
                PricingPlan(
                    duration = "12 Months",
                    price = "\u20b9459",
                    savings = "Save 50%",
                    isSelected = selectedPlan == 2,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { selectedPlan = 2 }
                )
            }
        }

        // Bottom Action Bar — normal flow, not an overlay, so it can never
        // cover content above it and never needs a scroll to be reached.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFA2E69), Color(0xFFC71585))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFFFA2E69),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Continue",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFC71585),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Secure payments \u2022 Cancel anytime \u2022 100% safe",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitItem(title: String, subtitle: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = Color(0xFFFF2D6C).copy(alpha = 0.08f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFF2D6C),
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun PricingPlan(
    duration: String,
    price: String,
    savings: String,
    isSelected: Boolean,
    isPopular: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Reserve the same vertical space above every card, whether or not
        // it shows the badge, so all three card bodies stay aligned.
        if (isPopular) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFF2D6C)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Most Popular", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            Spacer(modifier = Modifier.height(27.dp))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFFF2D6C) else Color.LightGray.copy(alpha = 0.3f)
            ),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = duration, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = price, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFFC71585) else Color.Black)
                    Text(text = "/mo", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFFFF2D6C).copy(alpha = 0.1f) else Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = savings,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFFFF2D6C) else Color.Gray
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_8"
)
@Composable
fun PremiumScreenPreview() {
    MaterialTheme {
        PremiumScreen(onClose = {})
    }
}
