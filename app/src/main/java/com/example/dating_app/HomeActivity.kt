package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.dating_app.util.SecurityUtils
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.*

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HomeScreen(
                    onChatClick = { name, id ->
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
    var lastReceivedMessage by remember { mutableStateOf<Message?>(null) }
    var showNotificationBanner by remember { mutableStateOf(false) }

    // Fetch Current User Data & Observe Unread Count
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.getUser(uid).onSuccess { user ->
                currentUserProfile = user
                if (user != null) {
                    (context.applicationContext as MyApplication).initZegoService(uid, "${user.first_name} ${user.last_name}")
                }
            }
            
            // Real-time Unread Count Observation
            repository.observeUnreadMessageCount(uid).collectLatest { count ->
                unreadMessageCount = count
                refreshTrigger++ // Refresh list when unread status changes anywhere
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
        currentSubScreen == "profile" -> "Profile"
        currentSubScreen == "settings" -> "Settings"
        currentSubScreen == "safety" -> "Safety Center"
        currentSubScreen == "filters" -> "Filters"
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
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    AsyncImage(
                        model = if (currentUserProfile?.profile_image?.isNotEmpty() == true) currentUserProfile?.profile_image else R.drawable.girl,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, Color(0xFFFF1493), CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.girl)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentUserProfile?.let { "${it.first_name} ${it.last_name}" } ?: "User",
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black
                    )
                    Text(
                        text = auth.currentUser?.email ?: "",
                        fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "View Profile", fontSize = 14.sp, color = Color(0xFFFF1493),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            currentSubScreen = "profile"
                            scope.launch { drawerState.close() }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text("Settings") }, selected = currentSubScreen == "settings",
                    onClick = { currentSubScreen = "settings"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }, modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Upgrade to Premium", color = Color(0xFFFFD700)) }, selected = false,
                    onClick = { context.startActivity(Intent(context, PremiumActivity::class.java)); scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700)) }, modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Safety Center") }, selected = currentSubScreen == "safety",
                    onClick = { currentSubScreen = "safety"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) }, modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Blocked List") }, selected = currentSubScreen == "blocked",
                    onClick = { currentSubScreen = "blocked"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Block, contentDescription = null) }, modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Security & Privacy") }, selected = false, onClick = { },
                    icon = { Icon(Icons.Default.Security, contentDescription = null) }, modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Help & Support") }, selected = false, onClick = { },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) }, modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                NavigationDrawerItem(
                    label = { Text("Logout", color = Color.Red) }, selected = false,
                    onClick = {
                        auth.signOut()
                        context.startActivity(Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Hide main top bar for profile screen as it has its own immersive header
                if (currentSubScreen != "profile") {
                    TopAppBar(
                        title = { 
                            Text(
                                text = topBarTitle, 
                                color = Color(0xFFFF1493), fontWeight = FontWeight.Bold, fontSize = 24.sp
                            ) 
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
                                IconButton(onClick = { refreshTrigger++ }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_heartbeat),
                                        contentDescription = "Heartbeat",
                                        tint = Color(0xFFFF1493),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { currentSubScreen = "filters" }) { 
                                    Icon(Icons.Default.Tune, contentDescription = "Filters", tint = Color.Gray)
                                }
                                IconButton(onClick = { }) { 
                                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = Color.Gray)
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
                        onClick = { showAstrologyModal = true },
                        containerColor = Color(0xFFFF2D6C),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 80.dp, end = 8.dp)
                            .size(64.dp)
                            .shadow(12.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            },
            bottomBar = {
                if (currentSubScreen == null) {
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
                                                onClick = { selectedTab = tab }
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
                                                            Badge(
                                                                containerColor = Color(0xFFFF2D6C),
                                                                modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                                            ) { 
                                                                Text("3", color = Color.White, fontSize = 9.sp) 
                                                            }
                                                        }
                                                        HomeTab.Message -> {
                                                            if (unreadMessageCount > 0) {
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
                                                        HomeTab.Chat -> Icons.Default.AccountCircle
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
                        currentSubScreen == "settings" -> SettingsScreen()
                        currentSubScreen == "safety" -> SafetyCenterScreen()
                        currentSubScreen == "filters" -> FiltersScreen()
                        currentSubScreen == "profile" -> UserProfileScreen(
                            onSubScreenChange = { profileSubScreen = it },
                            requestedSubScreen = profileSubScreen,
                            onBack = { currentSubScreen = null }
                        )
                        else -> {
                            when (selectedTab) {
                                HomeTab.Discovery -> DiscoveryScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
                                HomeTab.Matches -> MatchesScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
                                HomeTab.Likes -> LikesScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
                                HomeTab.Message -> ModernChatListScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
                                HomeTab.Chat -> AstrologyChatView(
                                    user = currentUserProfile,
                                    onChatClick = onChatClick,
                                    modifier = Modifier.fillMaxSize()
                                )
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
                                selectedTab = HomeTab.Message
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
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = Color(0xFFFF1493),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("New Message", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val bannerText = remember(lastReceivedMessage) {
                                    val msg = lastReceivedMessage
                                    if (msg == null) ""
                                    else if (msg.messageType != MessageType.TEXT) msg.messageType
                                    else if (msg.encrypted) {
                                        val key = SecurityUtils.generateChatKey(auth.currentUser?.uid ?: "", msg.senderId)
                                        SecurityUtils.decrypt(msg.messageText, key)
                                    } else msg.messageText
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
    data class ProfileMatch(val matchedUser: User) : AstrologyMessage()
}

enum class MatchFlowState { IDLE, WAITING_FOR_CITY, WAITING_FOR_AGE }

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
    var potentialMatches by remember { mutableStateOf<List<User>>(emptyList()) }
    
    var matchFlowState by remember { mutableStateOf(MatchFlowState.IDLE) }
    var preferredCity by remember { mutableStateOf("") }
    
    val userCity = user?.city?.takeIf { it.isNotBlank() } ?: "this realm"
    val suggestions = listOf("Find my match", "Daily horoscope", "My zodiac sign", "Who am I?")
    
    var messages by remember { 
        val name = user?.first_name ?: "Seeker"
        mutableStateOf(listOf<AstrologyMessage>(
            AstrologyMessage.Text("Astrologer", "Hi $name! I'm your AI guide. I see you're in $userCity. Want to find your perfect match or see your daily horoscope?")
        )) 
    }
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val sendMessage: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            messages = messages + AstrologyMessage.Text("User", text)
            scope.launch {
                scrollState.animateScrollToItem(messages.size - 1)
                delay(1000)
                
                when (matchFlowState) {
                    MatchFlowState.IDLE -> {
                        if (text.lowercase().contains("match") || text.lowercase().contains("someone")) {
                            messages = messages + AstrologyMessage.Text("Astrologer", "Which city are you looking for?")
                            matchFlowState = MatchFlowState.WAITING_FOR_CITY
                        } else {
                            val aiResponse = getAstrologyResponse(text, user, potentialMatches)
                            messages = messages + AstrologyMessage.Text("Astrologer", aiResponse)
                        }
                    }
                    MatchFlowState.WAITING_FOR_CITY -> {
                        preferredCity = text
                        messages = messages + AstrologyMessage.Text("Astrologer", "Got it! And what is the preferred age?")
                        matchFlowState = MatchFlowState.WAITING_FOR_AGE
                    }
                    MatchFlowState.WAITING_FOR_AGE -> {
                        val preferredAge = text.toIntOrNull()
                        matchFlowState = MatchFlowState.IDLE
                        
                        val filtered = potentialMatches.filter { 
                            (preferredCity.isEmpty() || it.city.contains(preferredCity, ignoreCase = true)) &&
                            (preferredAge == null || getAgeFromDob(it.dob) == preferredAge)
                        }
                        
                        if (filtered.isNotEmpty()) {
                            val matched = filtered.random()
                            messages = messages + AstrologyMessage.Text("Astrologer", "The stars have aligned! Here is your match:")
                            messages = messages + AstrologyMessage.ProfileMatch(matched)
                        } else {
                            messages = messages + AstrologyMessage.Text("Astrologer", "I couldn't find a perfect match for that city and age right now. Try another search!")
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))

        // Suggestion Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { suggestion ->
                Surface(
                    onClick = { sendMessage(suggestion) },
                    color = Color(0xFFFF1493).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF1493).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = suggestion,
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
                        val match = message.matchedUser
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                            border = BorderStroke(1.dp, Color(0xFFFF1493).copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = if (match.profile_image.isNotEmpty()) match.profile_image else R.drawable.girl,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${match.first_name}, ${getAgeFromDob(match.dob)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = match.city,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { 
                                        onChatClick(match.first_name, match.id)
                                        onDismiss?.invoke()
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

fun getAgeFromDob(dob: String): Int {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val birthYear = dob.split("/").lastOrNull()?.toIntOrNull() ?: 2000
    return currentYear - birthYear
}

fun getAstrologyResponse(userInput: String, user: User?, matches: List<User>): String {
    val input = userInput.lowercase()
    val name = user?.first_name ?: "Seeker"
    val city = user?.city?.takeIf { it.isNotBlank() } ?: "your city"
    val zodiac = user?.dob?.let { getZodiacSign(it) } ?: "unknown sign"

    return when {
        input.contains("hello") || input.contains("hi") -> 
            "Hi $name! How can I help you today? Ask about your match or horoscope."
        
        input.contains("love") || input.contains("match") || input.contains("find") || input.contains("someone") || input.contains("yes") || input.contains("another") -> {
            if (matches.isNotEmpty()) {
                val match = matches.random()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val birthYear = match.dob.split("/").lastOrNull()?.toIntOrNull() ?: 2000
                val age = currentYear - birthYear
                "I found ${match.first_name}, $age from ${match.city}. You both are a great match! Want to see another?"
            } else {
                "I couldn't find a match right now. Try updating your profile or checking back later!"
            }
        }

        input.contains("horoscope") || input.contains("today") || input.contains("day") -> 
            "Your stars look good today! It's a great day for new beginnings. Anything else?"
        
        input.contains("zodiac") || input.contains("sign") ->
            "You are a $zodiac. It's a very powerful sign! Want to know about your matches?"
        
        input.contains("who am i") ->
            "You are $name, a $zodiac from $city. What would you like to ask me?"

        else -> "I'm not sure I understand. Type 'match', 'horoscope' or 'zodiac'!"
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
fun LikesSummaryBanner(likedByUsers: List<User>) {
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
                                .border(2.dp, Color.White, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Button
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
fun LikesScreen(onChatClick: (String, String) -> Unit, refreshTrigger: Int = 0) {
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
            LikesSummaryBanner(likedByUsers)
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
                    onChatClick = { onChatClick(user.first_name, user.id) },
                    onLikeBack = {
                        scope.launch {
                            currentUser?.let { repository.likeProfile(it.uid, user.id) }
                            onChatClick(user.first_name, user.id)
                        }
                    }
                )
            }
        }

        // Bottom Premium Banner
        item(span = { GridItemSpan(2) }) {
            UpgradeToPremiumBanner()
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
fun LikedUserCard(user: User, onChatClick: () -> Unit, onLikeBack: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
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
            
            // "Chat" Badge (Clickable)
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onChatClick() },
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Chat", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF2D6C)))
                }
            }
            
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
            
            // Heart Button
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .align(Alignment.BottomEnd)
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
                        modifier = Modifier.size(20.dp)
                    )
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
fun NotificationsScreen() {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
        Text("Notifications", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No new notifications", color = Color.Gray)
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
fun DiscoveryScreen(onChatClick: (String, String) -> Unit, refreshTrigger: Int = 0) {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf<List<User>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var matchedUser by remember { mutableStateOf<User?>(null) }
    
    LaunchedEffect(refreshTrigger) {
        if (currentUser != null) {
            isLoading = true
            val allUsers = repository.getAllUsers(currentUser.uid).getOrDefault(emptyList())
            val likedUsers = repository.getLikedProfiles(currentUser.uid).getOrDefault(emptyList())
            val blockedUsers = repository.getBlockedUsers(currentUser.uid).getOrDefault(emptyList())
            val dislikedIds = repository.getDislikedProfileIds(currentUser.uid).getOrDefault(emptySet())
            val likedIds = likedUsers.map { it.id }.toSet()
            val blockedIds = blockedUsers.map { it.id }.toSet()
            profiles = allUsers.filter { it.id !in likedIds && it.id !in blockedIds && it.id !in dislikedIds }
            currentIndex = 0 
        }
        isLoading = false
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFF1493)) }
        } else if (currentIndex < profiles.size) {
            val profile = profiles[currentIndex]
            
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box {
                            AsyncImage(
                                model = if (profile.profile_image.isNotEmpty()) profile.profile_image else R.drawable.girl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
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
                                                Text(
                                                    text = "${profile.city} • 2km away",
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

                // Precise Swipe Actions matching requested UI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind - Yellow
                    ActionCircleButton(Icons.Default.Refresh, Color(0xFFFFB300), 50.dp) { /* Rewind */ }
                    
                    // Dislike - Red
                    ActionCircleButton(Icons.Default.Close, Color(0xFFF44336), 62.dp) { 
                        scope.launch { currentUser?.let { repository.dislikeProfile(it.uid, profile.id) } }
                        currentIndex++ 
                    }
                    
                    // Like - Pink Background, White Icon (Largest)
                    ActionCircleButton(Icons.Default.Favorite, Color.White, 82.dp, containerColor = Color(0xFFFF2D6C)) { 
                        scope.launch { 
                            if (currentUser != null) {
                                repository.likeProfile(currentUser.uid, profile.id)
                                val theyLikedMeSnapshot = repository.getLikedByUsers(currentUser.uid).getOrDefault(emptyList())
                                if (theyLikedMeSnapshot.any { it.id == profile.id }) {
                                    matchedUser = profile
                                } else {
                                    currentIndex++
                                }
                            }
                        }
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
                    Text("No more profiles", color = Color.Gray, fontSize = 18.sp)
                    TextButton(onClick = { refreshTrigger }) { Text("Refresh", color = Color(0xFFFF2D6C)) }
                }
            }
        }

        if (matchedUser != null) {
            MatchOverlay(
                matchedUser = matchedUser!!,
                onSendMessage = {
                    val user = matchedUser!!
                    matchedUser = null
                    currentIndex++
                    onChatClick(user.first_name, user.id)
                },
                onKeepSwiping = {
                    matchedUser = null
                    currentIndex++
                }
            )
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
fun SettingsScreen() {
    val items = listOf(
        "Account" to Icons.Default.Person,
        "Privacy" to Icons.Default.Lock,
        "Notifications" to Icons.Default.Notifications,
        "Subscription" to Icons.Default.Star,
        "Help & Support" to Icons.Default.Help,
        "About" to Icons.Default.Info
    )

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        items(items) { (title, icon) ->
            ListItem(
                headlineContent = { Text(title) },
                leadingContent = { Icon(icon, contentDescription = null, tint = Color.Gray) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable { }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
        }
    }
}
@Composable
fun SafetyCenterScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Your safety matters", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("We're here to help you", color = Color.Gray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SafetyItem("Safety Tips", Icons.Default.Lightbulb)
        SafetyItem("Block and Report", Icons.Default.Block)
        SafetyItem("No One", Icons.Default.PersonOff)
        SafetyItem("Verify Your Profile", Icons.Default.CheckCircle, isVerified = true)
    }
}
@Composable
fun SafetyItem(title: String, icon: ImageVector, isVerified: Boolean = false) {
    Surface(
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
fun FiltersScreen() {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        Text("Filters", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Location", fontWeight = FontWeight.Bold)
        Slider(value = 0.5f, onValueChange = {})
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Age Range", fontWeight = FontWeight.Bold)
        RangeSlider(value = 0.2f..0.6f, onValueChange = {})
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gender", fontWeight = FontWeight.Bold)
        Row {
            FilterChip(selected = true, onClick = {}, label = { Text("All") })
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(selected = false, onClick = {}, label = { Text("Women") })
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(selected = false, onClick = {}, label = { Text("Men") })
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D6C))
        ) {
            Text("Apply Filters")
        }
    }
}

