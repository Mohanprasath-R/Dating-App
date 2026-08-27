package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.itemsIndexed
import kotlin.math.roundToInt
import com.datingapp.R
import com.example.dating_app.model.User
import com.example.dating_app.model.Message
import com.example.dating_app.model.MessageType
import com.example.dating_app.model.Call
import com.example.dating_app.repository.FirebaseRepository
import com.example.dating_app.util.DateUtils
import com.example.dating_app.util.SecurityUtils
import com.example.dating_app.UserProfileScreen
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.*

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (Android 13)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Show an explanation to the user as to why your app needs the permission
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        askNotificationPermission()

        setContent {
            val scope = rememberCoroutineScope()
            val repository = remember { FirebaseRepository() }
            val auth = remember { FirebaseAuth.getInstance() }
            
            MaterialTheme {
                HomeScreen(
                    onChatClick = { name, id ->
                        // Mark as read immediately when clicking to update UI faster
                        scope.launch {
                            auth.currentUser?.uid?.let { uid ->
                                repository.markMessagesAsRead(uid, id)
                            }
                        }

                        val intent = Intent(this, ChatActivity::class.java)
                        intent.putExtra("CHAT_NAME", name)
                        intent.putExtra("RECEIVER_ID", id)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

enum class HomeTab(val label: String) {
    Discovery("Discover"),
    Matches("Matches"),
    Likes("Likes"),
    Message("Chats"),
    Chat("Astrology")
}

private fun getAge(dob: String): String {
    return try {
        val parts = dob.split("/")
        if (parts.size == 3) {
            val year = parts[2].toInt()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            (currentYear - year).toString()
        } else "20"
    } catch (e: Exception) { "20" }
}

@Composable
fun CustomDrawerItem(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFDFDFD), // Very light surface
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconBgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (label == "Upgrade to Premium") Color(0xFFDAA520) else Color(0xFF333333)
            )
            
            if (showBadge) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "NEW",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight, 
                contentDescription = null, 
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onChatClick: (String, String) -> Unit) {
    var selectedTab by remember { mutableStateOf(HomeTab.Discovery) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    var profileSubScreen by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    var currentUserProfile by remember { mutableStateOf<User?>(null) }
    var incomingCall by remember { mutableStateOf<Call?>(null) }
    var showAstrologyModal by remember { mutableStateOf(false) }
    var unreadMessageCount by remember { mutableIntStateOf(0) }
    var unreadLikesCount by remember { mutableIntStateOf(0) }
    var lastReceivedMessage by remember { mutableStateOf<Message?>(null) }
    var lastReceivedLike by remember { mutableStateOf<Map<String, Any>?>(null) }
    var bannerType by remember { mutableStateOf("message") } // "message" or "like"
    var showNotificationBanner by remember { mutableStateOf(false) }

    // Filter States
    var genderFilter by remember { mutableStateOf("All") }
    var preferredCity by remember { mutableStateOf("") }
    var ageRangeFilter by remember { mutableStateOf(18f..100f) }

    // Fetch Current User Data & Observe Unread Counts
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.syncPremiumStatus(uid) // Sync status on app launch
            
            // Update FCM Token
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                scope.launch {
                    repository.updateFcmToken(uid, token)
                }
            }

            repository.observeUser(uid).collectLatest { user ->
                currentUserProfile = user
                if (user != null) {
                    (context.applicationContext as MyApplication).initZegoService(uid, "${user.first_name} ${user.last_name}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            // Real-time Unread Count Observation
            repository.observeUnreadMessageCount(uid).collectLatest { count ->
                unreadMessageCount = count
                refreshTrigger++ // Refresh list when unread status changes anywhere
            }

            // Real-time Likes Count Observation
            repository.observeLikedByCount(uid).collectLatest { count ->
                unreadLikesCount = count
            }
        }
    }

    // Observe Incoming Messages for Notification Banner
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.observeLastReceivedMessage(uid).collectLatest { message ->
                // Only show banner for messages received while the app is open (recent timestamp)
                if (message != null && message.timestamp > System.currentTimeMillis() - 5000) {
                    lastReceivedMessage = message
                    bannerType = "message"
                    showNotificationBanner = true
                    refreshTrigger++ // Force list refresh
                    delay(3000)
                    showNotificationBanner = false
                }
            }
        }
    }

    // Observe Incoming Likes for Notification Banner
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.observeLastLike(uid).collectLatest { like ->
                val timestamp = like?.get("timestamp") as? Long ?: 0L
                if (like != null && timestamp > System.currentTimeMillis() - 5000) {
                    lastReceivedLike = like
                    bannerType = "like"
                    showNotificationBanner = true
                    refreshTrigger++ // Force list refresh
                    delay(3000)
                    showNotificationBanner = false
                }
            }
        }
    }

    // Real-time Online Status Observer for Self & Refresh Trigger
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val uid = auth.currentUser?.uid ?: return@LifecycleEventObserver
            scope.launch {
                when (event) {
                    Lifecycle.Event.ON_START -> repository.updateProfile(uid, mapOf("is_online" to true, "last_seen" to System.currentTimeMillis()))
                    Lifecycle.Event.ON_STOP -> repository.updateProfile(uid, mapOf("is_online" to false, "last_seen" to System.currentTimeMillis()))
                    Lifecycle.Event.ON_RESUME -> {
                        refreshTrigger++ // Refresh data when returning to home
                        // Force refresh user profile to ensure premium status is synced
                        scope.launch {
                            auth.currentUser?.uid?.let { uid ->
                                repository.syncPremiumStatus(uid)
                                repository.getUser(uid).onSuccess { user ->
                                    if (user != null) currentUserProfile = user
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val topBarTitle = when {
        currentSubScreen == "blocked" -> "Blocked List"
        currentSubScreen == "rejected" -> "Rejected List"
        currentSubScreen == "profile" -> "Profile"
        currentSubScreen == "settings" -> "Settings"
        currentSubScreen == "safety" -> "Safety Center"
        currentSubScreen == "filters" -> "Filters"
        currentSubScreen == "security" -> "Security & Privacy"
        currentSubScreen == "help" -> "Help & Support"
        currentSubScreen == "faq" -> "FAQ"
        currentSubScreen == "contact" -> "Contact Support"
        currentSubScreen == "report" -> "Report a Problem"
        currentSubScreen == "about" -> "About HeyDate"
        currentSubScreen == "terms" -> "Terms of Service"
        currentSubScreen == "privacy" -> "Privacy Policy"
        currentSubScreen == "safety_guidelines" -> "Safety Guidelines"
        currentSubScreen == "notifications" -> "Notifications"
        profileSubScreen == "details" -> "Profile Details"
        profileSubScreen == "edit" -> "Edit Profile"
        else -> when (selectedTab) {
            HomeTab.Discovery -> "Discover"
            HomeTab.Matches -> "Matches"
            HomeTab.Likes -> "Likes"
            HomeTab.Message -> "Chats"
            HomeTab.Chat -> "Astrology"
        }
    }

    val isSubScreen = currentSubScreen != null || profileSubScreen != null

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                modifier = Modifier.fillMaxHeight().width(320.dp)
            ) {
                // Header with decorative background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFEEF5), Color.White)
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Profile Image with ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(90.dp)
                                    .border(2.dp, Color(0xFFFF1493), CircleShape)
                                    .padding(4.dp)
                            ) {
                                AsyncImage(
                                    model = if (currentUserProfile?.profile_image?.isNotEmpty() == true) currentUserProfile?.profile_image else R.drawable.girl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = R.drawable.girl)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = currentUserProfile?.let { "${it.first_name} ${it.last_name}" } ?: "User",
                                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black
                                )
                                Text(
                                    text = auth.currentUser?.email ?: "",
                                    fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    onClick = { 
                                        currentSubScreen = "profile"
                                        scope.launch { drawerState.close() }
                                    },
                                    color = Color(0xFFFF1493).copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AccountCircle, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFFF1493)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "View Profile", 
                                            fontSize = 12.sp, 
                                            color = Color(0xFFFF1493),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Decorative Sparkles (Top Right)
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFF1493).copy(alpha = 0.2f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).size(24.dp)
                    )
                }

                // Drawer Items
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomDrawerItem(
                        label = "Settings",
                        icon = Icons.Default.Settings,
                        iconColor = Color(0xFFFF1493),
                        iconBgColor = Color(0xFFFFEBF5),
                        onClick = { currentSubScreen = "settings"; scope.launch { drawerState.close() } }
                    )
                    CustomDrawerItem(
                        label = if (currentUserProfile?.isPremiumActive() == true) "Premium Active" else "Upgrade to Premium",
                        icon = if (currentUserProfile?.isPremiumActive() == true) Icons.Default.VerifiedUser else Icons.Default.Diamond,
                        iconColor = Color(0xFFFFB300),
                        iconBgColor = Color(0xFFFFF8E1),
                        showBadge = currentUserProfile?.isPremiumActive() != true,
                        onClick = { 
                            if (currentUserProfile?.isPremiumActive() != true) {
                                context.startActivity(Intent(context, PremiumActivity::class.java))
                            }
                            scope.launch { drawerState.close() } 
                        }
                    )
                    CustomDrawerItem(
                        label = "Safety Center",
                        icon = Icons.Default.Shield,
                        iconColor = Color(0xFF7E57C2),
                        iconBgColor = Color(0xFFF3E5F5),
                        onClick = { currentSubScreen = "safety"; scope.launch { drawerState.close() } }
                    )
                    CustomDrawerItem(
                        label = "Blocked List",
                        icon = Icons.Default.Block,
                        iconColor = Color(0xFFEF5350),
                        iconBgColor = Color(0xFFFFEBEE),
                        onClick = { currentSubScreen = "blocked"; scope.launch { drawerState.close() } }
                    )
                    CustomDrawerItem(
                        label = "Rejected List",
                        icon = Icons.Default.DeleteSweep,
                        iconColor = Color(0xFFFF9800),
                        iconBgColor = Color(0xFFFFF3E0),
                        onClick = { currentSubScreen = "rejected"; scope.launch { drawerState.close() } }
                    )
                    CustomDrawerItem(
                        label = "Security & Privacy",
                        icon = Icons.Default.Security,
                        iconColor = Color(0xFF29B6F6),
                        iconBgColor = Color(0xFFE1F5FE),
                        onClick = { currentSubScreen = "security"; scope.launch { drawerState.close() } }
                    )
                    CustomDrawerItem(
                        label = "Help & Support",
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF66BB6A),
                        iconBgColor = Color(0xFFE8F5E9),
                        onClick = { currentSubScreen = "help"; scope.launch { drawerState.close() } }
                    )
                    CustomDrawerItem(
                        label = "Logout",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconColor = Color(0xFFD32F2F),
                        iconBgColor = Color(0xFFFFEBEE),
                        onClick = {
                            auth.signOut()
                            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Illustration (Safety/Security themed)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Decorative Botanical/Pink background elements
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFFFFEEF5).copy(alpha = 0.4f), Color.White)
                                )
                            )
                    ) {
                        // Mimicking the leaves with icons
                        Icon(
                            Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color(0xFFFF1493).copy(alpha = 0.15f),
                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 10.dp).size(40.dp)
                        )
                        Icon(
                            Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color(0xFFFF1493).copy(alpha = 0.15f),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 10.dp).size(40.dp)
                        )
                    }

                    // Shield Illustration
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFFEEF5),
                                border = BorderStroke(1.dp, Color(0xFFFF1493).copy(alpha = 0.1f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFFFF1493),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp).offset(y = (-2).dp)
                                    )
                                }
                            }
                            // Small check badge on the shield
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).size(18.dp).offset(x = 2.dp, y = (-2).dp),
                                shape = CircleShape,
                                color = Color(0xFFFF1493),
                                border = BorderStroke(2.dp, Color.White)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Color(0xFFF0F0F0),
                    thickness = 1.dp
                )

                // Delete Account Button
                Surface(
                    onClick = {
                        currentSubScreen = "security"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFEBEE),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Delete Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }

                // Version Info
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Version 1.0.4", fontSize = 11.sp, color = Color.LightGray)
                    Text("© 2024 Dating App Inc.", fontSize = 11.sp, color = Color.LightGray)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Hide main top bar for profile screen as it has its own immersive header
                if (currentSubScreen != "profile") {
                    TopAppBar(
                        title = { 
                            if (selectedTab == HomeTab.Discovery && currentSubScreen == null) {
                                Column {
                                    Text(
                                        text = "Discover",
                                        color = Color(0xFFFF1493), fontWeight = FontWeight.Bold, fontSize = 20.sp
                                    )
                                    Text(
                                        text = "${if(preferredCity.isEmpty()) "Anywhere" else preferredCity} • ${ageRangeFilter.start.toInt()}-${ageRangeFilter.endInclusive.toInt()}",
                                        color = Color.Gray, fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = topBarTitle, 
                                    color = Color(0xFFFF1493), fontWeight = FontWeight.Bold, fontSize = 24.sp
                                ) 
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                if (currentSubScreen != null) {
                                    currentSubScreen = null 
                                } else if (profileSubScreen != null) {
                                    profileSubScreen = null
                                } else {
                                    scope.launch { drawerState.open() } 
                                }
                            }) {
                                Icon(imageVector = if (isSubScreen) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu, contentDescription = "Navigation")
                            }
                        },
                        actions = {
                            if (!isSubScreen) {
                                IconButton(onClick = { currentSubScreen = "filters" }) {
                                    Icon(Icons.Default.Tune, contentDescription = "Filters", tint = Color.Gray)
                                }
                                IconButton(onClick = { currentSubScreen = "notifications" }) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadMessageCount + unreadLikesCount > 0) {
                                                Badge { Text((unreadMessageCount + unreadLikesCount).toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (unreadMessageCount + unreadLikesCount > 0) 
                                                Icons.Default.NotificationsActive else Icons.Default.NotificationsNone, 
                                            contentDescription = "Notifications", 
                                            tint = if (unreadMessageCount + unreadLikesCount > 0) Color(0xFFFF1493) else Color.Gray
                                        )
                                    }
                                }
                                // Added Profile Icon on the right side
                                IconButton(
                                    onClick = { 
                                        currentSubScreen = "profile"
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    AsyncImage(
                                        model = if (currentUserProfile?.profile_image?.isNotEmpty() == true) currentUserProfile?.profile_image else R.drawable.girl,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color(0xFFFF1493).copy(alpha = 0.2f), CircleShape),
                                        contentScale = ContentScale.Crop,
                                        placeholder = painterResource(id = R.drawable.girl)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                }
            },
            floatingActionButton = {
                if (currentSubScreen == null && selectedTab == HomeTab.Matches) {
                    FloatingActionButton(
                        onClick = { 
                            if (currentUserProfile?.isPremiumActive() == true) {
                                showAstrologyModal = true 
                            } else {
                                context.startActivity(Intent(context, PremiumActivity::class.java))
                            }
                        },
                        containerColor = Color(0xFFFF2D6C),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 80.dp, end = 8.dp)
                            .size(64.dp)
                            .shadow(12.dp, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                modifier = Modifier.size(32.dp)
                            )
                            if (currentUserProfile?.isPremiumActive() != true) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.White,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (currentSubScreen == null) {
                    key(currentUserProfile?.is_premium, currentUserProfile?.premium_expiry) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp, start = 12.dp, end = 12.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                            shape = RoundedCornerShape(32.dp),
                            color = Color.White,
                            shadowElevation = 12.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HomeTab.entries.forEach { tab ->
                                    val isSelected = selectedTab == tab
                                    val contentColor = if (isSelected) Color(0xFFFF2D6C) else Color(0xFF757575)
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { 
                                                    if (tab == HomeTab.Chat && currentUserProfile?.isPremiumActive() != true) {
                                                        context.startActivity(Intent(context, PremiumActivity::class.java))
                                                    } else {
                                                        selectedTab = tab 
                                                    }
                                                }
                                            )
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .blur(8.dp)
                                                        .background(Color(0xFFFF2D6C).copy(alpha = 0.15f), CircleShape)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(Color(0xFFFF2D6C).copy(alpha = 0.08f), CircleShape)
                                                )
                                            }
                                            
                                            BadgedBox(
                                                badge = {
                                                    when (tab) {
                                                        HomeTab.Likes -> {
                                                            if (unreadLikesCount > 0) {
                                                                Badge(
                                                                    containerColor = Color(0xFFFF2D6C),
                                                                    modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                                                ) { 
                                                                    Text(unreadLikesCount.toString(), color = Color.White, fontSize = 9.sp) 
                                                                }
                                                            }
                                                        }
                                                        HomeTab.Message -> {
                                                            if (unreadMessageCount > 0 && selectedTab != tab) {
                                                                Badge(containerColor = Color(0xFFFF2D6C)) { 
                                                                    Text(unreadMessageCount.toString(), color = Color.White, fontSize = 9.sp) 
                                                                }
                                                            }
                                                        }
                                                        HomeTab.Matches -> {
                                                            Icon(
                                                                imageVector = Icons.Default.Favorite, 
                                                                contentDescription = null, 
                                                                tint = Color(0xFFFF2D6C), 
                                                                modifier = Modifier.size(10.dp).offset(x = 6.dp, y = (-6).dp)
                                                            )
                                                        }
                                                        HomeTab.Chat -> {
                                                            if (currentUserProfile?.isPremiumActive() != true) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Lock,
                                                                    contentDescription = "Locked",
                                                                    tint = Color.Gray,
                                                                    modifier = Modifier.size(10.dp).offset(x = 4.dp, y = (-4).dp)
                                                                )
                                                            }
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = when (tab) {
                                                        HomeTab.Discovery -> Icons.Default.Favorite
                                                        HomeTab.Matches -> Icons.Default.People
                                                        HomeTab.Likes -> Icons.Default.Favorite
                                                        HomeTab.Message -> Icons.Default.Chat
                                                        HomeTab.Chat -> Icons.Default.AutoAwesome
                                                    },
                                                    contentDescription = tab.label,
                                                    tint = contentColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = tab.label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor
                                        )
                                        
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(12.dp)
                                                    .height(2.dp)
                                                    .blur(1.dp)
                                                    .background(Color(0xFFFF2D6C), RoundedCornerShape(1.dp))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(12.dp)
                                                    .height(2.dp)
                                                    .background(Color(0xFFFF2D6C), RoundedCornerShape(1.dp))
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
            val contentPadding = if (currentSubScreen == "profile") {
                PaddingValues(bottom = padding.calculateBottomPadding())
            } else {
                padding
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(contentPadding)) {
                    when {
                        currentSubScreen == "blocked" -> BlockedListScreen()
                        currentSubScreen == "rejected" -> RejectedListScreen()
                        currentSubScreen == "settings" -> SettingsScreen(onOptionClick = { currentSubScreen = it })
                        currentSubScreen == "safety" -> SafetyCenterScreen(onOptionClick = { currentSubScreen = it })
                        currentSubScreen == "filters" -> FiltersScreen(
                            currentGender = genderFilter,
                            onGenderChange = { genderFilter = it },
                            currentCity = preferredCity,
                            onCityChange = { preferredCity = it },
                            currentAgeRange = ageRangeFilter,
                            onAgeRangeChange = { ageRangeFilter = it },
                            onApply = { currentSubScreen = null }
                        )
                        currentSubScreen == "security" -> SecurityPrivacyScreen()
                        currentSubScreen == "help" -> HelpSupportScreen(onOptionClick = { currentSubScreen = it })
                        currentSubScreen == "faq" -> FAQScreen()
                        currentSubScreen == "contact" -> ContactSupportScreen()
                        currentSubScreen == "report" -> ReportProblemScreen(onBack = { currentSubScreen = "help" })
                        currentSubScreen == "about" -> AboutScreen()
                        currentSubScreen == "terms" -> TermsOfServiceScreen()
                        currentSubScreen == "privacy" -> PrivacyPolicyScreen()
                        currentSubScreen == "safety_guidelines" -> SafetyGuidelinesScreen()
                        currentSubScreen == "profile" -> UserProfileScreen(
                            onSubScreenChange = { profileSubScreen = it },
                            requestedSubScreen = profileSubScreen,
                            onBack = { currentSubScreen = null }
                        )
                        currentSubScreen == "notifications" -> NotificationsScreen(
                            currentUserProfile = currentUserProfile,
                            unreadMessageCount = unreadMessageCount,
                            unreadLikesCount = unreadLikesCount,
                            onActionClick = { action ->
                                when (action) {
                                    "premium" -> context.startActivity(Intent(context, PremiumActivity::class.java))
                                    "messages" -> { selectedTab = HomeTab.Message; currentSubScreen = null }
                                    "likes" -> { selectedTab = HomeTab.Likes; currentSubScreen = null }
                                }
                            }
                        )
                        else -> {
                            when (selectedTab) {
                                HomeTab.Discovery -> DiscoveryScreen(
                                    onChatClick = onChatClick,
                                    refreshTrigger = refreshTrigger,
                                    onRefresh = { refreshTrigger++ },
                                    genderFilter = genderFilter,
                                    preferredCity = preferredCity,
                                    minAge = ageRangeFilter.start.toInt(),
                                    maxAge = ageRangeFilter.endInclusive.toInt(),
                                    onOpenFilters = { currentSubScreen = "filters" }
                                )
                                HomeTab.Matches -> MatchesScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
                                HomeTab.Likes -> LikesScreen(
                                    onChatClick = onChatClick,
                                    isPremium = currentUserProfile?.isPremiumActive() == true,
                                    refreshTrigger = refreshTrigger
                                )
                                HomeTab.Message -> ModernChatListScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
                                HomeTab.Chat -> {
                                    if (currentUserProfile?.isPremiumActive() == true) {
                                        AstrologyChatView(
                                            user = currentUserProfile,
                                            onChatClick = onChatClick,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        // Fallback in case they somehow reach here
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text("Unlock AI Astrologer with Premium", fontWeight = FontWeight.Bold)
                                                Button(onClick = { context.startActivity(Intent(context, PremiumActivity::class.java)) }) {
                                                    Text("Go Premium")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAstrologyModal) {
                    AstrologyModal(
                        user = currentUserProfile,
                        onChatClick = onChatClick,
                        onDismiss = { showAstrologyModal = false }
                    )
                }

                // Notification Banner (Floats above everything)
                AnimatedVisibility(
                    visible = showNotificationBanner,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (bannerType == "message") {
                                    selectedTab = HomeTab.Message
                                } else {
                                    selectedTab = HomeTab.Likes
                                }
                                showNotificationBanner = false
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF1493).copy(0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (bannerType == "message") Icons.AutoMirrored.Filled.Chat else Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFFF1493),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (bannerType == "message") "New Message" else "New Like!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                val bannerText = remember(lastReceivedMessage, lastReceivedLike, bannerType) {
                                    if (bannerType == "message") {
                                        val msg = lastReceivedMessage
                                        if (msg == null) ""
                                        else if (msg.messageType != MessageType.TEXT) msg.messageType
                                        else if (msg.encrypted) {
                                            val key = SecurityUtils.generateChatKey(auth.currentUser?.uid ?: "", msg.senderId)
                                            SecurityUtils.decrypt(msg.messageText, key)
                                        } else msg.messageText
                                    } else {
                                        "Someone liked your profile! Check it out."
                                    }
                                }
                                Text(
                                    bannerText,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class AstrologyMessage {
    data class Text(val sender: String, val content: String) : AstrologyMessage()
    data class ProfileMatch(val matchedUsers: List<User>) : AstrologyMessage()
}

enum class MatchFlowState { 
    IDLE, 
    WAITING_FOR_CITY, 
    WAITING_FOR_AGE, 
    WAITING_FOR_GOAL, 
    WAITING_FOR_PREFERENCES 
}

@Composable
fun AstrologyModal(user: User?, onChatClick: (String, String) -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            AstrologyChatView(user = user, onChatClick = onChatClick, onDismiss = onDismiss)
        }
    }
}
@Composable
fun AstrologyChatView(
    user: User?,
    onChatClick: (String, String) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    var potentialMatches by remember { mutableStateOf<List<User>>(emptyList()) }
    
    var matchFlowState by remember { mutableStateOf(MatchFlowState.IDLE) }
    var preferredCity by remember { mutableStateOf("") }
    var preferredAgeRange by remember { mutableStateOf("") }
    var preferredGoal by remember { mutableStateOf("") }
    
    val userCity = user?.city?.takeIf { it.isNotBlank() } ?: "this realm"
    val suggestions = listOf("Find my match", "Daily horoscope", "Compatibility", "Relationship advice")
    
    val cityOptions = listOf("Chennai", "Mumbai", "Delhi", "Bangalore", "New York", "London", "Other")
    val ageOptions = listOf("18-24", "25-30", "31-40", "41-50", "50+")
    val goalOptions = listOf("Serious Relationship", "Casual Dating", "Friendship", "Marriage")
    val interestOptions = listOf("Travel", "Music", "Movies", "Sports", "Food", "Reading", "Fitness", "Art", "Dancing", "Photography")
    
    var messages by remember(user?.id, user?.is_premium) { 
        val name = user?.first_name ?: "Seeker"
        val initialMessages = mutableListOf<AstrologyMessage>(
            AstrologyMessage.Text("Astrologer", "Welcome, $name. I am your Celestial Guide. 🌙\n\nI see you are in $userCity, where the stars are currently aligned in your favor. Would you like to seek a new connection, or shall we explore the wisdom of the cosmos today?")
        )
        if (user?.isPremiumActive() != true) {
            initialMessages.add(AstrologyMessage.Text("Astrologer", "Note: Advanced features like chatting with your cosmic matches require a Premium subscription. ✨"))
        }
        mutableStateOf(initialMessages.toList())
    }
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val sendMessage: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            messages = messages + AstrologyMessage.Text("User", text)
            scope.launch {
                scrollState.animateScrollToItem(messages.size - 1)
                delay(1200)
                
                when (matchFlowState) {
                    MatchFlowState.IDLE -> {
                        val input = text.lowercase()
                        val userName = user?.first_name ?: "Seeker"
                        if (listOf("match", "girlfriend", "boyfriend", "someone", "love", "partner").any { input.contains(it) }) {
                            messages = messages + AstrologyMessage.Text("Astrologer", "I would be honored to help you find a companion. To begin our search, in which city shall we look for your match?")
                            matchFlowState = MatchFlowState.WAITING_FOR_CITY
                        } else if (input.contains("horoscope") || input.contains("today") || input.contains("energy")) {
                            messages = messages + AstrologyMessage.Text("Astrologer", "The cosmic currents are shifting for you, $userName. Let me consult the celestial charts...")
                            delay(1500)
                            val response = getAstrologyResponse(text, user, potentialMatches)
                            messages = messages + AstrologyMessage.Text("Astrologer", response)
                        } else {
                            val aiResponse = getAstrologyResponse(text, user, potentialMatches)
                            messages = messages + AstrologyMessage.Text("Astrologer", aiResponse)
                        }
                    }
                    MatchFlowState.WAITING_FOR_CITY -> {
                        preferredCity = text
                        messages = messages + AstrologyMessage.Text("Astrologer", "A wonderful choice. And what age range feels right for this connection?")
                        matchFlowState = MatchFlowState.WAITING_FOR_AGE
                    }
                    MatchFlowState.WAITING_FOR_AGE -> {
                        preferredAgeRange = text
                        if (user?.looking_for.isNullOrBlank()) {
                            messages = messages + AstrologyMessage.Text("Astrologer", "Understood. Tell me, what is your relationship goal? Are you seeking something serious, or perhaps a casual connection?")
                            matchFlowState = MatchFlowState.WAITING_FOR_GOAL
                        } else {
                            messages = messages + AstrologyMessage.Text("Astrologer", "Lastly, are there any other preferences? (Religion, education, hobbies, or simply type 'none' to proceed)")
                            matchFlowState = MatchFlowState.WAITING_FOR_PREFERENCES
                        }
                    }
                    MatchFlowState.WAITING_FOR_GOAL -> {
                        preferredGoal = text
                        messages = messages + AstrologyMessage.Text("Astrologer", "Thank you for sharing that. Are there any other specific preferences like hobbies or lifestyle choices I should consider?")
                        matchFlowState = MatchFlowState.WAITING_FOR_PREFERENCES
                    }
                    MatchFlowState.WAITING_FOR_PREFERENCES -> {
                        matchFlowState = MatchFlowState.IDLE
                        messages = messages + AstrologyMessage.Text("Astrologer", "The stars are shifting... I am scanning the horizons for your match. Please wait a moment.")
                        delay(2000)
                        
                        // Try strict filtering first (City + Age + Gender)
                        var filtered = potentialMatches.filter { 
                            val cityMatch = preferredCity.isEmpty() || it.city.contains(preferredCity, ignoreCase = true) || preferredCity.contains(it.city, ignoreCase = true)
                            val age = DateUtils.getAgeFromDob(it.dob)
                            val ageMatch = when (preferredAgeRange) {
                                "18-24" -> age in 18..24
                                "25-30" -> age in 25..30
                                "31-40" -> age in 31..40
                                "41-50" -> age in 41..50
                                "50+" -> age >= 50
                                else -> true
                            }
                            cityMatch && ageMatch
                        }
                        
                        // If no matches, relax the city constraint
                        if (filtered.isEmpty()) {
                            filtered = potentialMatches.filter { it.city.isNotEmpty() || preferredCity.isEmpty() }
                        }
                        
                        // If STILL no matches, just pick from potentialMatches (which handles gender/blocked)
                        if (filtered.isEmpty() && potentialMatches.isNotEmpty()) {
                            filtered = potentialMatches
                        }
                        
                        if (filtered.isNotEmpty()) {
                            // Show up to 3 matches if available
                            val matchedList = filtered.shuffled().take(3)
                            messages = messages + AstrologyMessage.Text("Astrologer", "The stars have revealed multiple paths that resonate with yours. 🌟")
                            messages = messages + AstrologyMessage.ProfileMatch(matchedList)
                        } else {
                            messages = messages + AstrologyMessage.Text("Astrologer", "I couldn't find someone matching all of your preferences right now. You could expand the age range or nearby cities, or I can notify you when someone compatible joins.")
                        }
                    }
                }
                scrollState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(user) {
        user?.let { currentUser ->
            val allUsersResult = repository.getAllUsers(currentUser.id)
            val allUsers = allUsersResult.getOrDefault(emptyList())
            val likedUsers = repository.getLikedProfiles(currentUser.id).getOrDefault(emptyList())
            val dislikedIds = repository.getDislikedProfileIds(currentUser.id).getOrDefault(emptySet())
            val likedIds = likedUsers.map { it.id }.toSet()
            
            val maleKeywords = listOf("male", "boy", "man", "m")
            val femaleKeywords = listOf("female", "girl", "woman", "w")
            
            val currentUserGender = currentUser.gender.lowercase().trim()
            val isCurrentUserMale = currentUserGender in maleKeywords
            val isCurrentUserFemale = currentUserGender in femaleKeywords
            
            potentialMatches = allUsers.filter { other ->
                val otherGender = other.gender.lowercase().trim()
                val isOtherOpposite = when {
                    isCurrentUserMale -> otherGender in femaleKeywords
                    isCurrentUserFemale -> otherGender in maleKeywords
                    else -> otherGender != currentUserGender && otherGender.isNotEmpty()
                }

                other.id != currentUser.id && 
                other.id !in likedIds && 
                other.id !in dislikedIds &&
                isOtherOpposite
            }
            
            // If still no matches and we have other users, try a more relaxed filter
            if (potentialMatches.isEmpty() && allUsers.isNotEmpty()) {
                potentialMatches = allUsers.filter { it.id != currentUser.id && it.id !in likedIds && it.id !in dislikedIds }
            }
        }
    }

    val context = LocalContext.current
    var showPremiumDialog by remember { mutableStateOf(false) }

    // Auto-dismiss dialog if user becomes premium
    LaunchedEffect(user?.is_premium, user?.premium_expiry) {
        if (user?.isPremiumActive() == true) {
            showPremiumDialog = false
        }
    }

    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text("Premium Feature") },
            text = { Text("Chatting with your cosmic matches is a premium feature. Upgrade to explore your destiny!") },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumDialog = false
                        context.startActivity(Intent(context, PremiumActivity::class.java))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493))
                ) {
                    Text("Get Premium")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text("Maybe later")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF1493).copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFF1493),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI Astrologer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Online • Celestial Guide",
                        fontSize = 12.sp,
                        color = Color.Green
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (user?.isPremiumActive() != true) {
                    TextButton(
                        onClick = { context.startActivity(Intent(context, PremiumActivity::class.java)) },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFF1493), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upgrade", color = Color(0xFFFF1493), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))

        // Context-aware Buttons based on Flow State
        val flowButtons = when (matchFlowState) {
            MatchFlowState.IDLE -> suggestions
            MatchFlowState.WAITING_FOR_CITY -> cityOptions
            MatchFlowState.WAITING_FOR_AGE -> ageOptions
            MatchFlowState.WAITING_FOR_GOAL -> goalOptions
            MatchFlowState.WAITING_FOR_PREFERENCES -> interestOptions
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(flowButtons) { buttonLabel ->
                Surface(
                    onClick = { sendMessage(buttonLabel) },
                    color = Color(0xFFFF1493).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF1493).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = buttonLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = Color(0xFFFF1493),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Chat Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages.size) { index ->
                when (val message = messages[index]) {
                    is AstrologyMessage.Text -> {
                        val isUser = message.sender == "User"
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                color = if (isUser) Color(0xFFFF1493) else Color(0xFFF0F0F0),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 16.dp
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = message.content,
                                    modifier = Modifier.padding(12.dp),
                                    color = if (isUser) Color.White else Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    is AstrologyMessage.ProfileMatch -> {
                        val matches = message.matchedUsers
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(matches) { match ->
                                    val score = (75..98).random()
                                    Card(
                                        modifier = Modifier.width(280.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                                        border = BorderStroke(1.dp, Color(0xFFFF1493).copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                AsyncImage(
                                                    model = if (match.profile_image.isNotEmpty()) match.profile_image else R.drawable.girl,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(60.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${match.first_name}, ${DateUtils.getAgeFromDob(match.dob)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp
                                                    )
                                                    Text(
                                                        text = "✨ Compatibility: $score%",
                                                        color = Color(0xFFFF1493),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { 
                                                        if (user?.isPremiumActive() == true) {
                                                            onChatClick(match.first_name, match.id)
                                                            onDismiss?.invoke()
                                                        } else {
                                                            showPremiumDialog = true
                                                        }
                                                    },
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = Color(0xFFFF1493),
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("✨ Why You Match", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("The stars suggest your personalities naturally balance one another. Your shared interest in ${match.interests.firstOrNull() ?: "connection"} creates a strong cosmic bond.", fontSize = 13.sp, color = Color.DarkGray)
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("💌 Icebreakers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("• \"I see you love ${match.interests.firstOrNull() ?: "exploring"}, what's your favorite spot?\"", fontSize = 13.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about your destiny...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF1493),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val userMsg = inputText
                        inputText = ""
                        sendMessage(userMsg)
                    }
                },
                containerColor = Color(0xFFFF1493),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}


fun getAstrologyResponse(userInput: String, user: User?, matches: List<User>): String {
    val input = userInput.lowercase()
    val name = user?.first_name ?: "Seeker"
    val zodiac = user?.dob?.let { getZodiacSign(it) } ?: "unknown sign"

    return when {
        input.contains("hello") || input.contains("hi") || input.contains("welcome") -> 
            "Greetings, $name. I am your Celestial Guide. How may I assist your heart's journey today?"
        
        input.contains("horoscope") || input.contains("today") || input.contains("energy") -> 
            "🌙 Celestial Insight\n\n✨ Today's Energy\n★★★★☆\n\n❤️ Love\nA meaningful conversation could strengthen an existing connection. Based on your $zodiac nature, today favors honest expression.\n\n🌟 Lucky Color\nEmerald Green\n\n💬 Cosmic Advice\nSometimes the smallest message starts the biggest story."

        input.contains("compatibility") ->
            "Compatibility is a dance of elements. As a $zodiac, you often find deep resonance with water and earth signs, who offer the stability or emotional depth you seek. Shall we look for such a match?"
        
        input.contains("zodiac") || input.contains("sign") ->
            "Your sign, $zodiac, speaks of unique strengths. Astrology suggests your communication style is intuitive and your love language often centers on quality time and deep connection. Would you like to know more about your personality traits?"
        
        input.contains("who am i") || input.contains("profile") ->
            "You are $name, a wise $zodiac from ${user?.city ?: "the stars"}. Your passion for ${user?.interests?.firstOrNull() ?: "life"} and your role as a ${user?.occupation ?: "seeker"} make you a truly unique soul in this cosmic dating pool."
        
        input.contains("advice") || input.contains("coach") ->
            "The stars suggest that when starting a conversation, curiosity is your greatest ally. Try asking about a dream they haven't shared yet, or their favorite way to find peace in a busy world."

        else -> "I hear your words, but the cosmic patterns are unclear. Would you like a 'daily horoscope', to 'find a match', or perhaps 'compatibility' advice?"
    }
}

fun getZodiacSign(dob: String): String {
    // Format: "day/month/year"
    val parts = dob.split("/")
    if (parts.size < 2) return "Celestial"
    
    val day = parts[0].toIntOrNull() ?: return "Celestial"
    val month = parts[1].toIntOrNull() ?: return "Celestial"
    
    return when (month) {
        1 -> if (day < 20) "Capricorn" else "Aquarius"
        2 -> if (day < 19) "Aquarius" else "Pisces"
        3 -> if (day < 21) "Pisces" else "Aries"
        4 -> if (day < 20) "Aries" else "Taurus"
        5 -> if (day < 21) "Taurus" else "Gemini"
        6 -> if (day < 21) "Gemini" else "Cancer"
        7 -> if (day < 23) "Cancer" else "Leo"
        8 -> if (day < 23) "Leo" else "Virgo"
        9 -> if (day < 23) "Virgo" else "Libra"
        10 -> if (day < 23) "Libra" else "Scorpio"
        11 -> if (day < 22) "Scorpio" else "Sagittarius"
        12 -> if (day < 22) "Sagittarius" else "Capricorn"
        else -> "Celestial"
    }
}
@Composable
private fun IncomingCallDialog(call: Call, onAccept: () -> Unit, onReject: () -> Unit) {
    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Incoming ${call.type.replaceFirstChar { it.uppercase() }} Call",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                AsyncImage(
                    model = if (call.callerImage.isNotEmpty()) call.callerImage else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, Color(0xFFFF1493), CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.girl)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = call.callerName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FloatingActionButton(
                        onClick = onReject,
                        containerColor = Color.Red,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Reject")
                    }
                    FloatingActionButton(
                        onClick = onAccept,
                        containerColor = Color.Green,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(if (call.type == "video") Icons.Default.VideoCall else Icons.Default.Call, contentDescription = "Accept")
                    }
                }
            }
        }
    }
}
@Composable
fun RejectedListScreen() {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    var rejectedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        currentUser?.let {
            repository.getDislikedProfiles(it.uid).onSuccess { list -> rejectedUsers = list }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFF1493)) }
    } else if (rejectedUsers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No rejected users", color = Color.Gray) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
            items(rejectedUsers) { user ->
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                        contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "${user.first_name} ${user.last_name}", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            scope.launch {
                                currentUser?.let { repository.undislikeProfile(it.uid, user.id).onSuccess { rejectedUsers = rejectedUsers.filter { b -> b.id != user.id } } }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) { Text("Revoke") }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(0.3f))
            }
        }
    }
}

@Composable
fun BlockedListScreen() {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    var blockedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        currentUser?.let {
            repository.getBlockedUsers(it.uid).onSuccess { list -> blockedUsers = list }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFF1493)) }
    } else if (blockedUsers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No blocked users", color = Color.Gray) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
            items(blockedUsers) { user ->
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                        contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "${user.first_name} ${user.last_name}", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            scope.launch {
                                currentUser?.let { repository.unblockUser(it.uid, user.id).onSuccess { blockedUsers = blockedUsers.filter { b -> b.id != user.id } } }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493))
                    ) { Text("Unblock") }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(0.3f))
            }
        }
    }
}
@Composable
fun LikesSummaryBanner(likedByUsers: List<User>, isPremium: Boolean) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFFFF0F5),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heart Icon Container
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF2D6C),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Text Column
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "${likedByUsers.size} people",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC2185B),
                    maxLines = 1
                )
                Text(
                    text = "liked your profile",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            
            // Avatars
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Row(horizontalArrangement = Arrangement.spacedBy((-16).dp)) {
                    likedByUsers.take(4).forEach { user ->
                            AsyncImage(
                                model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .then(if (!isPremium) Modifier.blur(10.dp) else Modifier),
                                contentScale = ContentScale.Crop
                            )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Button
            if (!isPremium) {
                Button(
                    onClick = { context.startActivity(Intent(context, PremiumActivity::class.java)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("See all", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
@Composable
fun LikesSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = Color(0xFFFF2D6C),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "People who like you",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Start a conversation and connect!",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
            Text(text = "Newest", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC2185B))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFFC2185B))
        }
    }
}
@Composable
fun UpgradeToPremiumBanner() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF0F5)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFFFF2D6C))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Upgrade to Premium", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "Unlock all likes and more features", fontSize = 12.sp, color = Color.Gray)
            }
            
            Button(
                onClick = { context.startActivity(Intent(context, PremiumActivity::class.java)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D6C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Upgrade", fontSize = 12.sp)
            }
        }
    }
}
@Composable
fun LikesScreen(onChatClick: (String, String) -> Unit, isPremium: Boolean, refreshTrigger: Int = 0) {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    var likedByUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(refreshTrigger) {
        if (currentUser != null) {
            isLoading = true
            repository.getLikedByUsers(currentUser.uid).onSuccess { users ->
                likedByUsers = users
                isLoading = false
            }.onFailure { isLoading = false }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // Summary Banner
        item(span = { GridItemSpan(2) }) {
            LikesSummaryBanner(likedByUsers, isPremium)
        }

        // Section Header
        item(span = { GridItemSpan(2) }) {
            LikesSectionHeader()
        }

        if (isLoading) {
            item(span = { GridItemSpan(2) }) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF2D6C))
                }
            }
        } else if (likedByUsers.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                EmptyLikesState()
            }
        } else {
            items(likedByUsers) { user ->
                LikedUserCard(
                    user = user,
                    isPremium = isPremium,
                    onChatClick = { onChatClick(user.first_name, user.id) },
                    onLikeBack = {
                        scope.launch {
                            currentUser?.let { repository.likeProfile(it.uid, user.id) }
                            onChatClick(user.first_name, user.id)
                        }
                    },
                    onReject = {
                        scope.launch {
                            currentUser?.let { repository.dislikeProfile(it.uid, user.id) }
                            // Force refresh by clearing the user from the list locally
                            likedByUsers = likedByUsers.filter { it.id != user.id }
                        }
                    }
                )
            }
        }

        // Bottom Premium Banner
        if (!isPremium) {
            item(span = { GridItemSpan(2) }) {
                UpgradeToPremiumBanner()
            }
        }
    }
}
@Composable
fun EmptyLikesState() {
    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = Color.White, shadowElevation = 8.dp) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color(0xFFFF1493).copy(alpha = 0.3f)) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "No Likes Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Start exploring to find matches!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}
@Composable
fun LikedUserCard(user: User, isPremium: Boolean, onChatClick: () -> Unit, onLikeBack: () -> Unit, onReject: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable { 
                if (!isPremium) {
                    context.startActivity(Intent(context, PremiumActivity::class.java))
                }
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().then(if (!isPremium) Modifier.blur(20.dp) else Modifier),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 350f
                        )
                    )
            )
            
            // User Details
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${user.first_name}, ${user.dob.takeLast(4).let { 2024 - (it.toIntOrNull() ?: 2000) }}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFFFF2D6C),
                        modifier = Modifier.size(14.dp)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = user.city, color = Color.White, fontSize = 11.sp)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Work, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if(user.occupation.isNotEmpty()) user.occupation else "Student", color = Color.White, fontSize = 11.sp)
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reject Button (X)
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onReject() },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Reject",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Heart Button (Like Back)
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onLikeBack() },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Like Back",
                            tint = Color(0xFFFF2D6C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun MatchesScreen(onChatClick: (String, String) -> Unit, refreshTrigger: Int = 0) {
    val repository = remember { FirebaseRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    var matches by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scrollState = rememberLazyListState()

    LaunchedEffect(refreshTrigger) {
        if (currentUser != null) {
            isLoading = true
            repository.getMatches(currentUser.uid).onSuccess { list -> 
                matches = list 
            }
        }
        isLoading = false
    }

    // Automatic Continuous Scrolling (Right to Left)
    LaunchedEffect(matches) {
        if (matches.isNotEmpty()) {
            while (true) {
                scrollState.animateScrollBy(
                    value = 150f, 
                    animationSpec = tween<Float>(
                        durationMillis = 3000,
                        easing = LinearEasing
                    )
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF4A0033))) {
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            // Animated Floating Background Hearts (Zig-Zag motion)
            FloatingHeartsBackground()

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator(color = Color.White) 
                }
            } else if (matches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                    Text("No active rooms", color = Color.White, fontSize = 18.sp) 
                }
            } else {
                // Horizontal Staggered List (Zig-Zag)
                LazyRow(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(matches) { index, user ->
                        // Alternate positions for Zig-Zag
                        val yOffset = if (index % 2 == 0) (-100).dp else 100.dp
                        
                        ConnectRoomItem(
                            user = user,
                            modifier = Modifier.offset(y = yOffset)
                        ) {
                            onChatClick(user.first_name, user.id)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ConnectRoomItem(user: User, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Avatar with Gradient Border
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(Color(0xFF00E676), Color(0xFF29B6F6), Color(0xFF00E676))
                        )
                    )
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Video Icon
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(28.dp),
                shape = CircleShape,
                color = Color(0xFF00C853),
                border = BorderStroke(2.dp, Color.White)
            ) {
                Icon(
                    Icons.Default.VideoCall, 
                    contentDescription = null, 
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
            
            // Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Text(
                    text = if (user.language.isNotBlank()) user.language else "English",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = user.first_name + " ✨",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
fun FloatingHeartsBackground() {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    val hearts = remember {
        List(50) {
            HeartData(
                xPosition = (0..100).random().toFloat() / 100f * screenWidth.value,
                size = (6..14).random().dp,
                duration = (5000..10000).random(),
                delay = (0..8000).random()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        hearts.forEach { heart ->
            FloatingHeart(heart, screenHeight)
        }
    }
}

data class HeartData(
    val xPosition: Float,
    val size: androidx.compose.ui.unit.Dp,
    val duration: Int,
    val delay: Int
)

@Composable
fun FloatingHeart(data: HeartData, screenHeight: androidx.compose.ui.unit.Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartTransition")
    
    val yProgress by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = -0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(data.duration, data.delay, LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yPosition"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = data.duration
                0f at 0
                0.8f at (data.duration * 0.2).toInt()
                0.8f at (data.duration * 0.8).toInt()
                0f at data.duration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = null,
        tint = Color.White.copy(alpha = alpha),
        modifier = Modifier
            .offset(
                x = data.xPosition.dp,
                y = screenHeight * yProgress
            )
            .size(data.size)
    )
}
@Composable
fun NotificationsScreen(
    currentUserProfile: User?,
    unreadMessageCount: Int,
    unreadLikesCount: Int,
    onActionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF7F9))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Premium Notification
        if (currentUserProfile?.is_premium != true) {
            NotificationCard(
                title = "Upgrade to Premium! 💎",
                description = "See who liked you, use AI Astrologer and get unlimited swipes with our Premium plan.",
                icon = Icons.Default.Diamond,
                iconColor = Color(0xFFFFD700),
                backgroundColor = Color(0xFFFFF8E1),
                onClick = { onActionClick("premium") }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Likes Notification
        if (unreadLikesCount > 0) {
            NotificationCard(
                title = "New Likes! ❤️",
                description = "You have $unreadLikesCount new people interested in you. Check them out!",
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFFF2D6C),
                backgroundColor = Color(0xFFFFEBEE),
                onClick = { onActionClick("likes") }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Messages Notification
        if (unreadMessageCount > 0) {
            NotificationCard(
                title = "New Messages 💬",
                description = "You have $unreadMessageCount unread messages waiting for you.",
                icon = Icons.Default.Chat,
                iconColor = Color(0xFF2196F3),
                backgroundColor = Color(0xFFE3F2FD),
                onClick = { onActionClick("messages") }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // App Updates / Tips
        NotificationCard(
            title = "Profile Tip 💡",
            description = "Users with at least 3 photos get 5x more matches! Add more photos to your profile.",
            icon = Icons.Default.Lightbulb,
            iconColor = Color(0xFF4CAF50),
            backgroundColor = Color(0xFFE8F5E9),
            onClick = { /* Could navigate to profile edit */ }
        )
        
        if (unreadMessageCount == 0 && unreadLikesCount == 0 && currentUserProfile?.is_premium == true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You're all caught up!", color = Color.Gray, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = backgroundColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(text = description, fontSize = 14.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}
@Composable
fun MatchOverlay(matchedUser: User, onSendMessage: () -> Unit, onKeepSwiping: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C29), Color(0xFF1E1E1E), Color(0xFF0F0C29))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background elements (Confetti simulation)
        Box(modifier = Modifier.fillMaxSize()) {
            val dotColors = listOf(Color(0xFFFF2D6C), Color.White, Color(0xFFFFC0CB))
            listOf(
                Offset(50f, 100f), Offset(300f, 150f), Offset(100f, 400f),
                Offset(350f, 500f), Offset(50f, 700f), Offset(300f, 800f)
            ).forEachIndexed { index, offset ->
                Box(
                    modifier = Modifier
                        .offset(offset.x.dp, offset.y.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColors[index % dotColors.size].copy(alpha = 0.4f))
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Glowing Hearts Illustration
            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(160.dp)) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color(0xFFFF2D6C).copy(alpha = 0.1f), CircleShape)
                )
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF2D6C),
                        modifier = Modifier.size(90.dp).graphicsLayer { rotationZ = -15f }
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF2D6C),
                        modifier = Modifier.size(110.dp).graphicsLayer { rotationZ = 15f; translationX = -25f; translationY = -15f }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // It's a Match! Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "It's a Match!",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Cursive
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF2D6C),
                    modifier = Modifier.size(24.dp).offset(y = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description with highlighted name
            Text(
                text = buildAnnotatedString {
                    append("You and ")
                    withStyle(style = SpanStyle(color = Color(0xFFFF2D6C), fontWeight = FontWeight.Bold)) {
                        append(matchedUser.first_name)
                    }
                    append(" liked each other")
                },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Profile Images with Borders
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color(0xFFFF2D6C)),
                    color = Color.Transparent
                ) {
                    AsyncImage(
                        model = R.drawable.ic_boy, // currentUser placeholder
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(4.dp).clip(CircleShape)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF2D6C),
                    modifier = Modifier.size(32.dp).padding(horizontal = 12.dp)
                )

                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color(0xFFFF2D6C)),
                    color = Color.Transparent
                ) {
                    AsyncImage(
                        model = if (matchedUser.profile_image.isNotEmpty()) matchedUser.profile_image else R.drawable.girl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(4.dp).clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Send a Message Button (Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFA2E69), Color(0xFFC71585))
                        )
                    )
                    .clickable { onSendMessage() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Send a Message",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keep Swiping Button (Outline)
            OutlinedButton(
                onClick = onKeepSwiping,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(2.dp, Color(0xFFFF2D6C)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(
                    text = "Keep Swiping",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Pager-like indicator at the very bottom
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Gray))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF2D6C)))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Gray))
            }
        }
    }
}
@Composable
fun DiscoveryHeader(
    preferredCity: String,
    minAge: Int,
    maxAge: Int,
    gender: String,
    onOpenFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable { onOpenFilters() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF2D6C), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (preferredCity.isEmpty()) "Anywhere" else preferredCity,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        VerticalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.LightGray.copy(alpha = 0.5f))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFFF2D6C), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$minAge - $maxAge",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        VerticalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.LightGray.copy(alpha = 0.5f))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFFF2D6C), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun DiscoveryScreen(
    onChatClick: (String, String) -> Unit,
    refreshTrigger: Int = 0,
    onRefresh: () -> Unit,
    genderFilter: String = "All",
    preferredCity: String = "",
    minAge: Int = 18,
    maxAge: Int = 100,
    onOpenFilters: () -> Unit = {}
) {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf<List<User>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var matchedUser by remember { mutableStateOf<User?>(null) }
    
    var lastVisibleDocument by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    var isEndReached by remember { mutableStateOf(false) }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    fun loadMoreProfiles() {
        if (currentUser == null || isEndReached) return
        
        scope.launch {
            repository.getDiscoveryProfiles(
                currentUserId = currentUser.uid,
                limit = 10,
                lastVisible = lastVisibleDocument,
                genderFilter = genderFilter,
                preferredCity = preferredCity,
                minAge = minAge,
                maxAge = maxAge
            ).onSuccess { (newProfiles, lastDoc) ->
                if (newProfiles.isEmpty()) {
                    isEndReached = true
                } else {
                    profiles = profiles + newProfiles
                    lastVisibleDocument = lastDoc
                }
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }

    val onNext = {
        if (profiles.isNotEmpty()) {
            currentIndex++
            if (currentIndex >= profiles.size - 2) {
                loadMoreProfiles()
            }
        }
        scope.launch {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
        }
    }

    val onPrevious = {
        if (currentIndex > 0) {
            currentIndex--
        }
        scope.launch {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
        }
    }

    val onLike = {
        scope.launch {
            if (currentUser != null && currentIndex < profiles.size) {
                val profile = profiles[currentIndex]
                repository.likeProfile(currentUser.uid, profile.id)
                repository.checkMatch(currentUser.uid, profile.id).onSuccess { isMatch ->
                    if (isMatch) {
                        matchedUser = profile
                    } else {
                        onNext()
                    }
                }.onFailure {
                    onNext()
                }
            }
        }
    }

    val onDislike = {
        scope.launch {
            if (currentUser != null && currentIndex < profiles.size) {
                val profile = profiles[currentIndex]
                repository.dislikeProfile(currentUser.uid, profile.id)
                onNext()
            }
        }
    }
    
    LaunchedEffect(refreshTrigger, genderFilter, preferredCity, minAge, maxAge) {
        profiles = emptyList()
        currentIndex = 0
        lastVisibleDocument = null
        isEndReached = false
        isLoading = true
        loadMoreProfiles()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {
        if (isLoading && profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFF1493)) }
        } else if (currentIndex < profiles.size) {
            Column(modifier = Modifier.fillMaxSize()) {
                DiscoveryHeader(
                    preferredCity = preferredCity,
                    minAge = minAge,
                    maxAge = maxAge,
                    gender = genderFilter,
                    onOpenFilters = onOpenFilters
                )

                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    val swipeProgress = (kotlin.math.abs(offsetX.value) / 500f).coerceIn(0f, 1f)
                    
                    // Card 1 (Middle/Next)
                    if (currentIndex + 1 < profiles.size) {
                        val middleIndex = currentIndex + 1
                        DiscoveryCard(
                            profile = profiles[middleIndex],
                            modifier = Modifier
                                .graphicsLayer {
                                    val scale = 0.95f + (0.05f * swipeProgress)
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = (15.dp.toPx()) * (1f - swipeProgress)
                                    alpha = 0.5f + (0.5f * swipeProgress)
                                },
                            getAge = { getAge(it) }
                        )
                    }

                    // Card 0 (Top)
                    val profile = profiles[currentIndex]
                    DiscoveryCard(
                        profile = profile,
                        modifier = Modifier
                            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                            .graphicsLayer {
                                rotationZ = offsetX.value / 20
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                            }
                            .pointerInput(profile.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        scope.launch {
                                            if (offsetX.value > 300) {
                                                offsetX.animateTo(1000f, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                                                onLike()
                                            } else if (offsetX.value < -300) {
                                                offsetX.animateTo(-1000f, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                                                onDislike()
                                            } else {
                                                launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                                launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            offsetX.snapTo(offsetX.value + dragAmount.x)
                                            offsetY.snapTo(offsetY.value + dragAmount.y)
                                        }
                                    }
                                )
                            },
                        isTopCard = true,
                        offsetX = offsetX.value,
                        onChatClick = onChatClick,
                        getAge = { getAge(it) }
                    )
                }

                // Precise Swipe Actions matching requested UI
                val profile = profiles[currentIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind - Yellow
                    ActionCircleButton(Icons.Default.Refresh, Color(0xFFFFB300), 50.dp) { 
                        onPrevious()
                    }
                    
                    // Dislike - Red
                    ActionCircleButton(Icons.Default.Close, Color(0xFFF44336), 62.dp) { 
                        onDislike()
                    }
                    
                    // Like - Pink Background, White Icon (Largest)
                    ActionCircleButton(Icons.Default.Favorite, Color.White, 82.dp, containerColor = Color(0xFFFF2D6C)) { 
                        onLike()
                    }
                    
                    // Super Like - Cyan
                    ActionCircleButton(Icons.Default.Star, Color(0xFF29B6F6), 62.dp) { /* Super Like */ }
                    
                    // Boost/Sparkles - Pink Background, White Icon
                    ActionCircleButton(Icons.Default.AutoAwesome, Color.White, 50.dp, containerColor = Color(0xFFFF2D6C)) { /* Boost */ }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFFF1493).copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No more profiles", color = Color.Gray, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh")
                    }
                }
            }
        }

        if (matchedUser != null) {
            MatchOverlay(
                matchedUser = matchedUser!!,
                onSendMessage = {
                    val user = matchedUser!!
                    matchedUser = null
                    if (profiles.isNotEmpty()) {
                        currentIndex = (currentIndex + 1) % profiles.size
                    }
                    scope.launch {
                        offsetX.snapTo(0f)
                        offsetY.snapTo(0f)
                    }
                    onChatClick(user.first_name, user.id)
                },
                onKeepSwiping = {
                    matchedUser = null
                    if (profiles.isNotEmpty()) {
                        currentIndex = (currentIndex + 1) % profiles.size
                    }
                    scope.launch {
                        offsetX.snapTo(0f)
                        offsetY.snapTo(0f)
                    }
                }
            )
        }
    }
}

@Composable
fun DiscoveryCard(
    profile: User,
    modifier: Modifier = Modifier,
    isTopCard: Boolean = false,
    offsetX: Float = 0f,
    onChatClick: (String, String) -> Unit = { _, _ -> },
    getAge: (String) -> String
) {
    val repository = remember { FirebaseRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val dragProgress = (kotlin.math.abs(offsetX) / 100f).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopCard) (8.dp + (4.dp * dragProgress)) else 2.dp)
    ) {
        Box {
            AsyncImage(
                model = if (profile.profile_image.isNotEmpty()) profile.profile_image else R.drawable.girl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Tinder-style Overlays
            if (isTopCard) {
                // NEXT (Left Swipe) Indicator
                if (offsetX < -100) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .align(Alignment.TopEnd)
                            .graphicsLayer { 
                                alpha = ((-offsetX - 100) / 200f).coerceIn(0f, 1f)
                                rotationZ = 15f
                            }
                            .border(4.dp, Color.Red, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("NEXT", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
                
                // PREV (Right Swipe) Indicator
                if (offsetX > 100) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .align(Alignment.TopStart)
                            .graphicsLayer { 
                                alpha = ((offsetX - 100) / 200f).coerceIn(0f, 1f)
                                rotationZ = -15f
                            }
                            .border(4.dp, Color(0xFFFFB300), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("PREV", color = Color(0xFFFFB300), fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            // Top Overlay
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Online", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 500f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${profile.first_name}, ${getAge(profile.dob)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFFFF1493),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = profile.bio.take(100) + if(profile.bio.length > 100) "..." else "",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                val distanceText = remember(profile) {
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser != null) {
                                        // Since we don't have current user's lat/lon here easily, 
                                        // let's assume a default or use a placeholder that calls LocationHelper if available
                                        // In a real app, you'd pass the current user's location into this composable.
                                        "Nearby"
                                    } else "Location hidden"
                                }

                                Text(
                                    text = "${profile.city} • $distanceText",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        var isMutualLike by remember(profile.id) { mutableStateOf(false) }
                        LaunchedEffect(profile.id) {
                            if (currentUser != null) {
                                val theyLikedMeSnapshot = repository.getLikedByUsers(currentUser.uid).getOrDefault(emptyList())
                                isMutualLike = theyLikedMeSnapshot.any { it.id == profile.id }
                            }
                        }

                        if (isMutualLike) {
                            Spacer(modifier = Modifier.height(8.dp))
                            IconButton(
                                onClick = { onChatClick(profile.first_name, profile.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFFF1493), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Message",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        InterestChip("Travel ✈️")
                        Spacer(modifier = Modifier.height(8.dp))
                        InterestChip("Music 🎵")
                        Spacer(modifier = Modifier.height(8.dp))
                        InterestChip("Photography 📷")
                    }
                }
            }
        }
    }
}
@Composable
fun InterestChip(text: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
fun ActionCircleButton(
    icon: ImageVector,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    containerColor: Color = Color.White,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "scale"
    )

    val shadowColor = if (containerColor == Color.White) Color.Black.copy(alpha = 0.15f) else containerColor.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 10.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(containerColor, CircleShape)
            .then(
                if (containerColor == Color.White) 
                    Modifier.border(0.5.dp, Color.LightGray.copy(alpha = 0.2f), CircleShape)
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}
@Composable
fun SettingsScreen(onOptionClick: (String) -> Unit) {
    val settingsItems = listOf(
        Triple("Account", "Personal info, email, and phone", Icons.Default.Person),
        Triple("Privacy", "Visibility and blocked users", Icons.Default.Lock),
        Triple("Notifications", "Push alerts and messages", Icons.Default.Notifications),
        Triple("Subscription", "Manage your premium plan", Icons.Default.Star),
        Triple("Help & Support", "FAQ and contact details", Icons.Default.Help),
        Triple("About", "Version and legal info", Icons.Default.Info)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // Decorative Header Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFEEF5), Color.White)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFFF1493),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        items(settingsItems) { (title, subtitle, icon) ->
            val context = LocalContext.current
            ListItem(
                headlineContent = { 
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) 
                },
                supportingContent = { 
                    Text(subtitle, fontSize = 12.sp, color = Color.Gray) 
                },
                leadingContent = { 
                    Surface(
                        color = Color(0xFFF8F9FE), 
                        shape = RoundedCornerShape(12.dp), 
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon, 
                                contentDescription = null, 
                                tint = Color(0xFFFF1493), 
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD1D1D1)) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { 
                        when (title) {
                            "Account" -> context.startActivity(Intent(context, AccountSettingsActivity::class.java))
                            "Privacy" -> onOptionClick("security")
                            "Notifications" -> context.startActivity(Intent(context, NotificationsSettingsActivity::class.java))
                            "Subscription" -> context.startActivity(Intent(context, SubscriptionSettingsActivity::class.java))
                            "Help & Support" -> onOptionClick("help")
                            "About" -> onOptionClick("about")
                        }
                    }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6), thickness = 0.5.dp)
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "App Version 1.0.4",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Dating App Inc.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFEEF5),
            border = BorderStroke(2.dp, Color(0xFFFF1493).copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Favorite, null, tint = Color(0xFFFF1493), modifier = Modifier.size(64.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("HeyDate", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        Text("Version 1.0.4 (Build 124)", color = Color.Gray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            "HeyDate is a modern dating platform designed to foster meaningful connections through shared interests and astrological insights. Our mission is to make dating safer, more intuitive, and fun for everyone.",
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            color = Color.DarkGray,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text("Made with ❤️ for the world", fontSize = 12.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("© 2024 Dating App Inc. All rights reserved.", fontSize = 11.sp, color = Color.LightGray)
    }
}

@Composable
fun SafetyCenterScreen(onOptionClick: (String) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Your safety matters", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("We're here to help you", color = Color.Gray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SafetyItem("Safety Tips", Icons.Default.Lightbulb, onClick = { onOptionClick("safety_guidelines") })
        SafetyItem("Block and Report", Icons.Default.Block, onClick = { onOptionClick("help") })
        SafetyItem("Report Behavior", Icons.Default.Security, onClick = { onOptionClick("report") })
        SafetyItem("Verify Profile", Icons.Default.CheckCircle, isVerified = true, onClick = { 
            Toast.makeText(context, "Verification feature coming soon!", Toast.LENGTH_SHORT).show()
        })
    }
}
@Composable
fun SafetyItem(title: String, icon: ImageVector, isVerified: Boolean = false, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF9FAFB)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if(isVerified) Color(0xFF2196F3) else Color.Black)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (isVerified) {
                Text("Verified", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}
@Composable
fun FiltersScreen(
    currentGender: String,
    onGenderChange: (String) -> Unit,
    currentCity: String,
    onCityChange: (String) -> Unit,
    currentAgeRange: ClosedFloatingPointRange<Float>,
    onAgeRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onApply: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        Text("Filters", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Location (City)", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = currentCity,
            onValueChange = onCityChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter city name") },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Age Range: ${currentAgeRange.start.toInt()} - ${currentAgeRange.endInclusive.toInt()}", fontWeight = FontWeight.Bold)
        RangeSlider(
            value = currentAgeRange,
            onValueChange = onAgeRangeChange,
            valueRange = 18f..100f
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gender", fontWeight = FontWeight.Bold)
        Row {
            listOf("All", "Female", "Male").forEach { gender ->
                FilterChip(
                    selected = currentGender == gender,
                    onClick = { onGenderChange(gender) },
                    label = { Text(gender) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D6C))
        ) {
            Text("Apply Filters")
        }
    }
}

@Composable
fun SecurityPrivacyScreen() {
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var user by remember { mutableStateOf<User?>(null) }
    
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.getUser(uid).onSuccess { fetchedUser ->
                user = fetchedUser
            }
        }
    }

    val securityItems = listOf(
        Triple("Two-Factor Authentication", "Add an extra layer of security", Icons.Default.VpnKey),
        Triple("Biometric App Lock", "Unlock with fingerprint or face (Premium)", Icons.Default.Fingerprint),
        Triple("Login Activity", "Check where you're logged in", Icons.Default.Devices),
        Triple("Security Checkup", "Keep your account safe", Icons.Default.VerifiedUser)
    )

    val privacyItems = listOf(
        Triple("Private Profile", "Only your matches can see your photos", Icons.Default.VisibilityOff),
        Triple("Active Status", "Show when you're online", Icons.Default.ToggleOn),
        Triple("Read Receipts", "Let others know you've read messages", Icons.Default.DoneAll)
    )

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        item {
            Text(
                "Security",
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFF1493),
                fontWeight = FontWeight.Bold
            )
        }
        items(securityItems) { (title, subtitle, icon) ->
            ListItem(
                headlineContent = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
                supportingContent = { Text(subtitle, fontSize = 12.sp, color = Color.Gray) },
                leadingContent = { 
                    Surface(
                        color = Color(0xFFF8F9FE), 
                        shape = CircleShape, 
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon, 
                                contentDescription = null, 
                                tint = Color(0xFF29B6F6), 
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                trailingContent = {
                    if (title == "Biometric App Lock") {
                        Switch(
                            checked = user?.biometric_enabled ?: false,
                            onCheckedChange = { enabled ->
                                if (user?.isPremiumActive() == true) {
                                    scope.launch {
                                        auth.currentUser?.uid?.let { uid ->
                                            repository.updateProfile(uid, mapOf("biometric_enabled" to enabled))
                                                .onSuccess {
                                                    user = user?.copy(biometric_enabled = enabled)
                                                    Toast.makeText(context, "Biometric lock ${if(enabled) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                } else {
                                    context.startActivity(Intent(context, PremiumActivity::class.java))
                                    Toast.makeText(context, "Upgrade to Premium to enable Biometric Lock", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF1493)
                            )
                        )
                    } else {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD1D1D1))
                    }
                },
                modifier = Modifier.clickable { 
                    when (title) {
                        "Two-Factor Authentication" -> {
                            context.startActivity(Intent(context, TwoFactorAuthActivity::class.java))
                        }
                        "Login Activity" -> {
                            context.startActivity(Intent(context, LoginActivityHistoryActivity::class.java))
                        }
                        "Security Checkup" -> {
                            context.startActivity(Intent(context, SecurityCheckupActivity::class.java))
                        }
                        "Biometric App Lock" -> {
                            if (user?.isPremiumActive() != true) {
                                context.startActivity(Intent(context, PremiumActivity::class.java))
                            }
                        }
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6), thickness = 0.5.dp)
        }

        item {
            Text(
                "Privacy",
                modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFF1493),
                fontWeight = FontWeight.Bold
            )
        }
        items(privacyItems) { (title, subtitle, icon) ->
            val isChecked = when (title) {
                "Private Profile" -> user?.private_profile ?: false
                "Active Status" -> user?.show_active_status ?: true
                "Read Receipts" -> user?.read_receipts ?: true
                else -> false
            }

            ListItem(
                headlineContent = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
                supportingContent = { Text(subtitle, fontSize = 12.sp, color = Color.Gray) },
                leadingContent = { 
                    Surface(
                        color = Color(0xFFF8F9FE), 
                        shape = CircleShape, 
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon, 
                                contentDescription = null, 
                                tint = Color(0xFF7E57C2), 
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                trailingContent = {
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { enabled ->
                            val field = when (title) {
                                "Private Profile" -> "private_profile"
                                "Active Status" -> "show_active_status"
                                "Read Receipts" -> "read_receipts"
                                else -> ""
                            }
                            if (field.isNotEmpty()) {
                                scope.launch {
                                    auth.currentUser?.uid?.let { uid ->
                                        repository.updateProfile(uid, mapOf(field to enabled))
                                            .onSuccess {
                                                user = when (field) {
                                                    "private_profile" -> user?.copy(private_profile = enabled)
                                                    "show_active_status" -> user?.copy(show_active_status = enabled)
                                                    "read_receipts" -> user?.copy(read_receipts = enabled)
                                                    else -> user
                                                }
                                                Toast.makeText(context, "$title updated", Toast.LENGTH_SHORT).show()
                                            }
                                            .onFailure {
                                                Toast.makeText(context, "Failed to update $title", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF1493)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6), thickness = 0.5.dp)
        }

        item {
            var showDeleteDialog by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val repository = remember { FirebaseRepository() }
            val auth = FirebaseAuth.getInstance()
            val context = LocalContext.current

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Account") },
                    text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    auth.currentUser?.uid?.let { uid ->
                                        repository.deleteAccount(uid).onSuccess {
                                            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            })
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) { Text("Delete", color = Color.White) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(64.dp)
                    .clickable { showDeleteDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFEBEE),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Delete Account",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HelpSupportScreen(onOptionClick: (String) -> Unit) {
    val supportOptions = listOf(
        Triple("FAQ", "Frequently asked questions", Icons.Default.QuestionAnswer),
        Triple("Contact Support", "Our team is here to help", Icons.Default.Email),
        Triple("Report a Problem", "Let us know if something is wrong", Icons.Default.BugReport)
    )

    val legalOptions = listOf(
        Triple("Terms of Service", "The rules of our community", Icons.Default.Gavel),
        Triple("Privacy Policy", "How we handle your data", Icons.Default.Policy),
        Triple("Safety Guidelines", "Stay safe while dating", Icons.Default.HealthAndSafety)
    )

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp).background(
                    Brush.verticalGradient(listOf(Color(0xFFFFEEF5), Color.White))
                ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color(0xFFFF1493), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("How can we help you?", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }

        item {
            Text(
                "Support",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(supportOptions) { (title, subtitle, icon) ->
            ListItem(
                headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(subtitle, fontSize = 12.sp, color = Color.Gray) },
                leadingContent = { 
                    Icon(icon, contentDescription = null, tint = Color(0xFF66BB6A))
                },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
                modifier = Modifier.clickable { 
                    when(title) {
                        "FAQ" -> onOptionClick("faq")
                        "Contact Support" -> onOptionClick("contact")
                        "Report a Problem" -> onOptionClick("report")
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
        }

        item {
            Text(
                "Legal & Safety",
                modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(legalOptions) { (title, subtitle, icon) ->
            ListItem(
                headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(subtitle, fontSize = 12.sp, color = Color.Gray) },
                leadingContent = { 
                    Icon(icon, contentDescription = null, tint = Color(0xFF5C6BC0))
                },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
                modifier = Modifier.clickable { 
                    when(title) {
                        "Terms of Service" -> onOptionClick("terms")
                        "Privacy Policy" -> onOptionClick("privacy")
                        "Safety Guidelines" -> onOptionClick("safety_guidelines")
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Version 1.0.4", fontSize = 12.sp, color = Color.LightGray)
                Text("© 2024 Dating App Inc.", fontSize = 12.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun FAQScreen() {
    val faqs = listOf(
        "How do I create a profile?" to "Go to the profile tab and tap on 'Edit Profile' to add your photos and bio. Make sure to use clear, high-quality photos to attract more matches!",
        "How does matching work?" to "When two people like each other by swiping right, it's a match! You'll be notified immediately and can start a conversation from the 'Chats' tab.",
        "Is my data safe?" to "Absolutely. We use industry-standard end-to-end encryption to protect your messages and personal information. Your privacy is our top priority.",
        "How do I report someone?" to "Your safety is important. Tap the '...' menu on any user's profile or inside a chat window to report inappropriate behavior or block a user.",
        "Can I change my location?" to "Yes! You can update your city in your profile settings or use filters to find people in different areas."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFDFDFD)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Frequently Asked Questions",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(faqs) { (question, answer) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFF3F4F6))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFFEEF5),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QuestionAnswer, null, tint = Color(0xFFFF1493), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = question, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = answer, 
                        fontSize = 14.sp, 
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ContactSupportScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFEEF5),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SupportAgent, null, tint = Color(0xFFFF1493), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("We're here to help!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Our support team is available 24/7 to ensure you have the best experience.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Support Channels", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        SupportChannelItem(
            icon = Icons.Default.Email, 
            title = "Email Support", 
            value = "support@datingapp.com", 
            bgColor = Color(0xFFE8F5E9), 
            iconColor = Color(0xFF4CAF50),
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:support@datingapp.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Support Request")
                }
                context.startActivity(Intent.createChooser(intent, "Send Email"))
            }
        )
        SupportChannelItem(
            icon = Icons.Default.Phone, 
            title = "Hotline", 
            value = "+1 (800) 123-4567", 
            bgColor = Color(0xFFE3F2FD), 
            iconColor = Color(0xFF2196F3),
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:+18001234567")
                }
                context.startActivity(intent)
            }
        )
        SupportChannelItem(
            icon = Icons.Default.Chat, 
            title = "Live Chat", 
            value = "Typical response: 5 mins", 
            bgColor = Color(0xFFFFF3E0), 
            iconColor = Color(0xFFFF9800),
            onClick = {
                Toast.makeText(context, "Live Chat is currently offline. Please use email.", Toast.LENGTH_LONG).show()
            }
        )

        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { 
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:support@datingapp.com")
                }
                context.startActivity(Intent.createChooser(intent, "Contact Us"))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493))
        ) {
            Text("Send us a Message", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SupportChannelItem(icon: ImageVector, title: String, value: String, bgColor: Color, iconColor: Color, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = bgColor, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = value, fontSize = 13.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun ReportProblemScreen(onBack: () -> Unit) {
    var problemText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        Text("Report a Problem", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Spotted a bug? Help us improve your experience by describing it below.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Description", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = problemText,
            onValueChange = { problemText = it },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            placeholder = { Text("What happened? When did you notice it?", color = Color.LightGray) },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF1493),
                unfocusedBorderColor = Color(0xFFF3F4F6),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedContainerColor = Color(0xFFF9FAFB)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            color = Color(0xFFF1F1F1),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().clickable { 
                Toast.makeText(context, "Image picker coming soon", Toast.LENGTH_SHORT).show()
            }
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Add Screenshots (Optional)", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { 
                isSubmitting = true
                scope.launch {
                    delay(1500)
                    Toast.makeText(context, "Report submitted successfully. Thank you!", Toast.LENGTH_LONG).show()
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493)),
            enabled = problemText.isNotBlank() && !isSubmitting
        ) {
            if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Submit Report", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TermsOfServiceScreen() {
    val terms = listOf(
        "1. Acceptance of Terms" to "By accessing or using our application, you agree to be bound by these Terms of Service. If you do not agree to all of the terms and conditions, then you may not access the service.",
        "2. Eligibility" to "You must be at least 18 years of age to create an account on this platform. By using the service, you represent and warrant that you have the right, authority, and capacity to enter into this agreement.",
        "3. User Conduct" to "You are solely responsible for your interactions with other users. You agree to treat all members with respect and refrain from any form of harassment, hate speech, or illegal activities.",
        "4. Content" to "You retain ownership of the content you post, but you grant us a worldwide license to use it. We reserve the right to remove any content that violates our community guidelines.",
        "5. Safety and Security" to "While we strive to provide a safe environment, we are not responsible for the conduct of any user on or off the service. Always use caution when meeting new people."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                "Terms of Service",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                "Last Updated: June 2024",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        items(terms) { (title, content) ->
            Column {
                Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFFFF1493))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content, 
                    fontSize = 14.sp, 
                    color = Color(0xFF424242),
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen() {
    val policies = listOf(
        "Data Collection" to "We collect personal information that you provide to us, including your name, photos, bio, and preferences. We also collect usage data and device information automatically.",
        "How We Use Data" to "Your data is used to provide and improve the service, facilitate matching, ensure safety through moderation, and personalize your experience.",
        "Data Sharing" to "We do not sell your personal data. We only share information with trusted third-party service providers who help us operate our app, or when required by law.",
        "Your Privacy Rights" to "You have the right to access, update, or delete your information at any time. You can manage your privacy settings directly within the app's account settings.",
        "Security Measures" to "We implement robust security measures to protect your data from unauthorized access, including encryption and secure server infrastructure."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                "Privacy Policy",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                "Your privacy matters to us.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        items(policies) { (title, content) ->
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content, 
                    fontSize = 14.sp, 
                    color = Color.DarkGray,
                    lineHeight = 22.sp
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = Color(0xFFF3F4F6))
        }
    }
}

@Composable
fun SafetyGuidelinesScreen() {
    val guidelines = listOf(
        "Meet in Public" to "For the first few dates, always meet in a well-lit, busy public place like a popular cafe, restaurant, or park. Avoid private locations.",
        "Tell a Friend" to "Share your date's name, location, and time with a trusted friend or family member. Let them know when you've arrived and when you're back home safely.",
        "Trust Your Instincts" to "If something feels off or you feel uncomfortable, don't hesitate to leave immediately. Your safety and comfort are more important than being polite.",
        "Guard Financial Info" to "Never send money or share bank details, credit card numbers, or social security information with anyone you meet online, regardless of their story.",
        "Stay on the App" to "Keep your conversations within our platform for as long as possible. Our moderation tools are here to protect you while you're getting to know someone."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB)),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Safety Guidelines",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(guidelines) { (title, content) ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.HealthAndSafety, 
                        contentDescription = null, 
                        tint = Color(0xFF4CAF50), 
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = content, 
                            fontSize = 14.sp, 
                            color = Color(0xFF616161),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

