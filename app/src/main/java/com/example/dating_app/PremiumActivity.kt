package com.example.dating_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class PremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PremiumScreen(onClose = { finish() })
        }
    }
}

@Composable
fun PremiumScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Icon(
            Icons.Default.Favorite,
            contentDescription = null,
            tint = Color(0xFFFFD700), // Gold
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Upgrade to Premium",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Get more matches and features",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        PremiumBenefit("Unlimited Likes", "Like as many people as you want")
        PremiumBenefit("See who likes you", "See everyone who likes you")
        PremiumBenefit("Boost your profile", "Get more visibility")
        PremiumBenefit("No ads", "Enjoy ad-free experience")

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumPlan("1 Month", "$9.99", "Save 20%", modifier = Modifier.weight(1f))
            PremiumPlan("6 Months", "$7.49", "Save 30%", isPopular = true, modifier = Modifier.weight(1f))
            PremiumPlan("12 Months", "$4.99", "Save 50%", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D6C)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PremiumBenefit(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF2D6C), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = subtitle, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun PremiumPlan(duration: String, price: String, save: String, isPopular: Boolean = false, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, if (isPopular) Color(0xFFFF2D6C) else Color.LightGray.copy(alpha = 0.5f)),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = duration, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "$price / mo", fontSize = 12.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = if (isPopular) Color(0xFFFF2D6C) else Color.LightGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = save,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = if (isPopular) Color.White else Color.Black
                )
            }
        }
    }
}
