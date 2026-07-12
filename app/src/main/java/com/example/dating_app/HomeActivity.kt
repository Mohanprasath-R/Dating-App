package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
import kotlin.math.roundToInt
import com.datingapp.R
import com.example.dating_app.model.User
import com.example.dating_app.model.Message
import com.example.dating_app.model.Call
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import com.example.dating_app.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.*

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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

enum class HomeTab(val label: String, val icon: ImageVector) {
    Discovery("Discover", Icons.Default.Explore),
    Likes("Likes", Icons.Default.FavoriteBorder),
    Matches("Matches", Icons.Default.FavoriteBorder),
    Message("Message", Icons.Default.ChatBubbleOutline),
    Chat("Chat", Icons.Default.AutoAwesome)
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

    // Fetch Current User Data
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.getUser(uid).onSuccess { user ->
                currentUserProfile = user
                if (user != null) {
                    (context.applicationContext as MyApplication).initZegoService(uid, "${user.first_name} ${user.last_name}")
                }
            }
        }
    }

    // Observe Incoming Calls
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.observeIncomingCalls(uid).collectLatest { calls ->
                incomingCall = calls.firstOrNull()
            }
        }
    }

    // Heartbeat: Update online status periodically
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            while (true) {
                repository.updateProfile(uid, mapOf("is_online" to true, "last_seen" to System.currentTimeMillis()))
                delay(60000) // Update every minute
            }
        }
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
            HomeTab.Discovery -> "Discovery"
            HomeTab.Likes -> "Likes"
            HomeTab.Matches -> "Matches"
            HomeTab.Message -> "Messages"
            HomeTab.Chat -> "Chat"
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
                                BadgedBox(
                                    badge = { Badge(containerColor = Color.Red) { Text("3") } },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = Color.Gray)
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
            },
            floatingActionButton = {
                if (currentSubScreen == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        var showText by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(2000)
                            showText = true
                            delay(3000) // Show for 3 seconds
                            showText = false
                        }
                        
                        AnimatedVisibility(
                            visible = showText,
                            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
                        ) {
                            Surface(
                                color = Color(0xFFFF1493),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "Discover Your Destiny ✨",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = { 
                                showAstrologyModal = true
                            },
                            containerColor = Color(0xFFFF1493),
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Astrology", modifier = Modifier.size(24.dp))
                        }
                    }
                }
            },
            bottomBar = {
                if (currentSubScreen == null) {
                    NavigationBar(containerColor = Color.White, contentColor = Color(0xFFFF1493)) {
                        HomeTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { 
                                    val iconModifier = Modifier.size(26.dp)
                                    when (tab) {
                                        HomeTab.Discovery -> Icon(Icons.Default.Explore, contentDescription = null, modifier = iconModifier)
                                        HomeTab.Likes -> Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = iconModifier)
                                        HomeTab.Matches -> Icon(painter = painterResource(id = R.drawable.ic_hearts), contentDescription = null, modifier = iconModifier)
                                        HomeTab.Message -> {
                                            BadgedBox(
                                                badge = { Badge(containerColor = Color(0xFFFF1493)) { Text("3", color = Color.White) } }
                                            ) {
                                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = iconModifier)
                                            }
                                        }
                                        HomeTab.Chat -> Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = iconModifier)
                                    }
                                },
                                label = { Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFFF1493), 
                                    selectedTextColor = Color(0xFFFF1493),
                                    unselectedIconColor = Color.Gray, 
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color(0xFFFF1493).copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when {
                    currentSubScreen == "blocked" -> BlockedListScreen()
                    currentSubScreen == "settings" -> SettingsScreen()
                    currentSubScreen == "safety" -> SafetyCenterScreen()
                    currentSubScreen == "filters" -> FiltersScreen()
                    currentSubScreen == "profile" -> UserProfileScreen(
                        onSubScreenChange = { profileSubScreen = it },
                        requestedSubScreen = profileSubScreen
                    )
                    else -> {
                        when (selectedTab) {
                            HomeTab.Discovery -> DiscoveryScreen(
                                currentUserProfile = currentUserProfile,
                                onChatClick = onChatClick, 
                                refreshTrigger = refreshTrigger
                            )
                            HomeTab.Likes -> LikesScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
                            HomeTab.Matches -> MatchesScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
                            HomeTab.Message -> ChatListScreen(onChatClick = onChatClick, refreshTrigger = refreshTrigger)
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Likes You", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = "${likedByUsers.size} people liked your profile", fontSize = 14.sp, color = Color.Gray)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFF2D6C)) }
        } else if (likedByUsers.isEmpty()) {
            EmptyLikesState()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(likedByUsers) { user -> 
                    LikedUserCard(
                        user = user, 
                        onUnlike = { /* No-op for 'liked by' screen usually, or 'pass' */ },
                        onMessage = { 
                            // Liking them back creates a match
                            scope.launch {
                                currentUser?.let { repository.likeProfile(it.uid, user.id) }
                                onChatClick(user.first_name, user.id)
                            }
                        }
                    ) 
                }
            }
        }
    }
}

@Composable
fun EmptyLikesState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
fun LikedUserCard(user: User, onUnlike: () -> Unit, onMessage: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(260.dp).clickable { }, shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 400f)))
            if (user.is_online) {
                Surface(modifier = Modifier.padding(12.dp).size(10.dp).align(Alignment.TopEnd), shape = CircleShape, color = Color.Green, border = BorderStroke(1.dp, Color.White)) {}
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
                Text(text = "${user.first_name}, ${user.dob.takeLast(4).let { 2024 - (it.toIntOrNull() ?: 2000) }}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = user.city, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onMessage, modifier = Modifier.weight(1f).height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493)), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(onClick = onUnlike, modifier = Modifier.size(36.dp), shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.2f), contentColor = Color.White) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, contentDescription = "Unlike", modifier = Modifier.size(18.dp)) }
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

    LaunchedEffect(refreshTrigger) {
        if (currentUser != null) {
            isLoading = true
            repository.getMatches(currentUser.uid).onSuccess { list -> 
                matches = list 
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF5D1049))) {
        // FRND Connect Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFF007F))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FRND Connect",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Rooms",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
                //卡通形象占位
                Row(verticalAlignment = Alignment.Bottom) {
                    AsyncImage(
                        model = R.drawable.girl,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp).clip(CircleShape)
                    )
                    AsyncImage(
                        model = R.drawable.ic_boy,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp).clip(CircleShape).offset(x = (-10).dp)
                    )
                }
            }
        }

        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        "LiveVideo",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(listOf("#FRND-ship 🤝", "#love ❤️", "#Hot 🔥", "#New ✨")) { tag ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        tag,
                        color = if(tag.contains("love")) Color.Red else Color.Black,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Main Area
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            // Background Hearts
            repeat(6) { i ->
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size((120 + i * 40).dp)
                        .offset(x = (i * 60 - 30).dp, y = (i * 120).dp)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator(color = Color.White) 
                }
            } else if (matches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                    Text("No active rooms", color = Color.White, fontSize = 18.sp) 
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(matches) { user ->
                        ConnectRoomItem(user = user) {
                            onChatClick(user.first_name, user.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectRoomItem(user: User, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
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
                    text = "हिन्दी",
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
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "It's a Match!",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You and ${matchedUser.first_name} liked each other",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = R.drawable.ic_boy, // currentUser (placeholder)
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape)
                )
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF2D6C), modifier = Modifier.size(40.dp).padding(horizontal = 8.dp))
                AsyncImage(
                    model = if(matchedUser.profile_image.isNotEmpty()) matchedUser.profile_image else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = onSendMessage,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D6C))
            ) {
                Text("Send a Message", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onKeepSwiping,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).height(56.dp),
                border = BorderStroke(1.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Keep Swiping")
            }
        }
    }
}

@Composable
fun DiscoveryScreen(
    currentUserProfile: User?,
    onChatClick: (String, String) -> Unit, 
    refreshTrigger: Int = 0
) {
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
                                            val distanceText = if (currentUserProfile != null && profile.latitude != null && profile.longitude != null && profile.location_enabled) {
                                                LocationHelper.calculateApproximateDistance(
                                                    currentUserProfile!!.latitude ?: 0.0,
                                                    currentUserProfile!!.longitude ?: 0.0,
                                                    profile.latitude!!,
                                                    profile.longitude!!
                                                )
                                            } else {
                                                "Location hidden"
                                            }
                                            Text(
                                                text = "${profile.city} • $distanceText",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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
    val repository = remember { FirebaseRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            repository.getUser(uid).onSuccess { user = it }
        }
    }

    val items = listOf(
        "Account" to Icons.Default.Person,
        "Privacy" to Icons.Default.Lock,
        "Notifications" to Icons.Default.Notifications,
        "Subscription" to Icons.Default.Star,
        "Help & Support" to Icons.Default.Help,
        "About" to Icons.Default.Info
    )

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        item {
            // Location Security Toggle
            ListItem(
                headlineContent = { Text("Share Location") },
                supportingContent = { Text("Enable to show approximate distance to others") },
                leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = user?.location_enabled ?: true,
                        onCheckedChange = { enabled ->
                            user = user?.copy(location_enabled = enabled)
                            scope.launch {
                                auth.currentUser?.uid?.let { uid ->
                                    repository.updateProfile(uid, mapOf("location_enabled" to enabled))
                                }
                            }
                        }
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
        }

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

@Composable
fun ChatListScreen(onChatClick: (String, String) -> Unit, refreshTrigger: Int = 0) {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    var chats by remember { mutableStateOf<List<Pair<User, Message?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        currentUser?.let { repository.getChatList(it.uid).onSuccess { list -> chats = list } }
        isLoading = false
    }

    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isEmpty()) chats
        else chats.filter { (partner, _) -> partner.first_name.contains(searchQuery, ignoreCase = true) || partner.last_name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search messages...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") } },
            shape = RoundedCornerShape(12.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFF5F5F5), unfocusedContainerColor = Color(0xFFF5F5F5), focusedIndicatorColor = Color(0xFFFF1493), unfocusedIndicatorColor = Color.Transparent),
            singleLine = true
        )
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFF1493)) }
        } else if (filteredChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = if (searchQuery.isEmpty()) "No messages" else "No results for '$searchQuery'", color = Color.Gray) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredChats) { (partner, lastMessage) ->
                    ChatItem(partner, lastMessage, onChatClick)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(0.5f))
                }
            }
        }
    }
}

@Composable
fun ChatItem(partner: User, lastMessage: Message?, onChatClick: (String, String) -> Unit) {
    val time = lastMessage?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it.timestamp)) } ?: ""
    Row(modifier = Modifier.fillMaxWidth().clickable { onChatClick("${partner.first_name} ${partner.last_name}", partner.id) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = if (partner.profile_image.isNotEmpty()) partner.profile_image else R.drawable.girl, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${partner.first_name} ${partner.last_name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(time, fontSize = 12.sp, color = Color.Gray)
            }
            Text(lastMessage?.messageText ?: "Start chatting...", fontSize = 14.sp, color = Color.Gray, maxLines = 1)
        }
    }
}
