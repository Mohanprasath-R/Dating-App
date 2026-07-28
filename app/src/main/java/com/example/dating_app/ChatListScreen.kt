@file:Suppress("PreviewAnnotationInFunctionWithParameters")

package com.example.dating_app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil.compose.AsyncImage
import com.datingapp.R
import com.example.dating_app.model.Message
import com.example.dating_app.model.MessageType
import com.example.dating_app.model.User
import com.example.dating_app.repository.ChatListItem
import com.example.dating_app.repository.FirebaseRepository
import com.example.dating_app.util.DateUtils
import com.example.dating_app.util.SecurityUtils
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernChatListScreen(onChatClick: (String, String) -> Unit, refreshTrigger: Int = 0) {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    // Real-time Chat List
    val chats by remember(currentUser?.uid) {
        currentUser?.let { repository.observeChatList(it.uid) } ?: flowOf(emptyList<ChatListItem>())
    }.collectAsState(initial = emptyList())

    var requests by remember { mutableStateOf<List<User>>(emptyList()) }
    var currentUserProfile by remember { mutableStateOf<User?>(null) }
    var onlineUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Messages, 1 for Requests
    var localRefreshTrigger by remember { mutableIntStateOf(0) }
    
    // Bottom Sheet State
    var selectedChatItemForMenu by remember { mutableStateOf<ChatListItem?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger, localRefreshTrigger) {
        currentUser?.let { user ->
            repository.getMatches(user.uid).onSuccess { matches -> 
                onlineUsers = matches.filter { u -> u.is_online }
            }
            repository.getUser(user.uid).onSuccess { profile ->
                currentUserProfile = profile
            }
            repository.getPendingRequests(user.uid).onSuccess { list ->
                requests = list
            }
        }
        isLoading = false
    }

    val visibleRequests = remember(requests, currentUserProfile) {
        if (currentUserProfile?.is_premium == true) requests
        else requests.take(6)
    }

    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isEmpty()) chats
        else chats.filter { item -> 
            item.partner.first_name.contains(searchQuery, ignoreCase = true) || 
            item.partner.last_name.contains(searchQuery, ignoreCase = true) 
        }
    }

    val totalUnreadCount = remember(chats) { chats.sumOf { it.unreadCount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF7F9))
    ) {
        // Search Bar Row - Show only on Messages tab
        if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text("Search messages or people...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFFF1493)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Horizontal Stories/Matches Bar - Show only on Messages tab
            if (onlineUsers.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(onlineUsers) { user ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFFFF1493), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(user.first_name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Tab Switcher (Pill Style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp)
                .background(Color(0xFFF9F9F9), RoundedCornerShape(28.dp))
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Messages Tab
                Surface(
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    color = if (selectedTab == 0) Color.White else Color.Transparent,
                    shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.Gray else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Messages",
                            color = if (selectedTab == 0) Color.Gray else Color.LightGray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }

                // Requests Tab
                Surface(
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    color = if (selectedTab == 1) Color.White else Color.Transparent,
                    shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color(0xFFFF1493) else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Requests",
                            color = if (selectedTab == 1) Color(0xFFFF1493) else Color.LightGray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        if (requests.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier.size(22.dp),
                                shape = CircleShape,
                                color = Color(0xFFFF1493)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = requests.size.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading && (if (selectedTab == 0) chats.isEmpty() else requests.isEmpty())) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF1493))
                }
            } else {
                if (selectedTab == 0) {
                    // Messages Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredChats) { chatItem ->
                            ModernChatItem(
                                chatItem = chatItem, 
                                currentUserId = currentUser?.uid, 
                                onChatClick = onChatClick,
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedChatItemForMenu = chatItem
                                    showBottomSheet = true
                                }
                            )
                        }
                    }
                } else {
                    // Requests Tab (Redesigned based on Image)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // New Request Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("New Request", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF1493)))
                                }
                                Text("1 new", color = Color(0xFFFF1493), fontSize = 14.sp)
                            }
                        }

                        // Request Card
                        if (visibleRequests.isNotEmpty()) {
                            item {
                                ModernRequestItem(
                                    user = visibleRequests.first(),
                                    onAccept = {
                                        scope.launch {
                                            currentUser?.let { me ->
                                                repository.likeProfile(me.uid, visibleRequests.first().id).onSuccess {
                                                    localRefreshTrigger++
                                                }
                                            }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            currentUser?.let { me ->
                                                repository.blockUser(me.uid, visibleRequests.first().id).onSuccess {
                                                    localRefreshTrigger++
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Suggestions Header
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFF1493), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Suggestions", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                                }
                                Text("See all", color = Color(0xFFFF1493), fontSize = 14.sp)
                            }
                        }

                        // Suggestions Row
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(onlineUsers.take(5)) { user ->
                                    SuggestionCard(user)
                                }
                            }
                        }

                        // Safety Banner
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SafetyBanner()
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet && selectedChatItemForMenu != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1C1C1E), // Dark professional background
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ChatListMenuContent(
                chatItem = selectedChatItemForMenu!!,
                onAction = { action ->
                    showBottomSheet = false
                    currentUser?.let { user ->
                        val partnerId = selectedChatItemForMenu!!.partner.id
                        scope.launch {
                            when (action) {
                                "pin" -> repository.togglePinChat(user.uid, partnerId, !selectedChatItemForMenu!!.isPinned)
                                "delete" -> repository.deleteConversation(user.uid, partnerId)
                                "mute_messages" -> repository.toggleMuteChat(user.uid, partnerId, !selectedChatItemForMenu!!.isMuted)
                                "mute_calls" -> { /* Logic for muting calls */ }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ChatListMenuContent(chatItem: ChatListItem, onAction: (String) -> Unit) {
    val partner = chatItem.partner
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Professional Header
        Text(
            text = "${partner.first_name} ${partner.last_name} 🦋".uppercase(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
        )
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

        Spacer(modifier = Modifier.height(8.dp))

        MenuItem(
            title = if (chatItem.isPinned) "Unpin" else "Pin", 
            icon = if (chatItem.isPinned) Icons.Default.PushPin else Icons.Default.PushPin, 
            onClick = { onAction("pin") }
        )
        
        MenuItem(
            title = "Delete", 
            icon = Icons.Default.DeleteOutline, 
            color = Color(0xFFFF453A), // System Red
            onClick = { onAction("delete") }
        )
        
        MenuItem(
            title = if (chatItem.isMuted) "Unmute messages" else "Mute messages", 
            icon = if (chatItem.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, 
            onClick = { onAction("mute_messages") }
        )
        
        MenuItem(
            title = "Mute calls", 
            icon = Icons.Default.PhonePaused, 
            onClick = { onAction("mute_calls") }
        )
    }
}

@Composable
fun MenuItem(
    title: String,
    icon: ImageVector,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TabItem(
    title: String,
    badgeCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) Color(0xFFFF1493) else Color.White,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (title == "Messages") Icons.AutoMirrored.Filled.Chat else Icons.Default.Person,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFFFF69B4) else Color(0xFFFF1493)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernRequestItem(
    user: User,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Circle Around Profile Image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, Color(0xFFFF1493), CircleShape)
                )
                AsyncImage(
                    model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.first_name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFFFEEF5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "New",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color(0xFFFF1493),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Interested in you!",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, tint = Color(0xFFFF1493).copy(0.5f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("2 min ago", fontSize = 12.sp, color = Color.Gray)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFF5F5F5), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFF2D6C), Color(0xFFC71585))), 
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Accept",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionCard(user: User) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, Color.White, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(user.first_name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${DateUtils.getAgeFromDob(user.dob)}  •  ${user.city}", color = Color.Gray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = Color(0xFFFFEEF5),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "3 mutual interests", 
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color(0xFFFF1493),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF5F5F5), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFF2D6C), Color(0xFFC71585))), 
                            CircleShape
                        )
                ) {
                    Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SafetyBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAFB)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFEEF5)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Shield, null, tint = Color(0xFFFF1493), modifier = Modifier.size(28.dp))
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text("Your safety is our priority", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "We keep your data safe and ensure a secure dating experience.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFFF1493))
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernChatItem(
    chatItem: ChatListItem, 
    currentUserId: String?, 
    onChatClick: (String, String) -> Unit,
    onLongClick: () -> Unit
) {
    val partner = chatItem.partner
    val lastMessage = chatItem.lastMessage
    val unreadCount = chatItem.unreadCount

    val displayMessage = remember(lastMessage, currentUserId) {
        if (lastMessage == null) return@remember "Start chatting..."
        
        val content = if (lastMessage.encrypted && currentUserId != null) {
            try {
                val key = SecurityUtils.generateChatKey(currentUserId, partner.id)
                SecurityUtils.decrypt(lastMessage.messageText, key)
            } catch (e: Exception) {
                lastMessage.messageText
            }
        } else {
            lastMessage.messageText
        }
        
        when (lastMessage.messageType) {
            MessageType.IMAGE -> "📷 Image"
            MessageType.VIDEO -> "🎥 Video"
            MessageType.AUDIO -> "🎤 Voice Note"
            else -> content
        }
    }

    val time = lastMessage?.let { 
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(it.timestamp))
    } ?: ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onChatClick("${partner.first_name} ${partner.last_name}", partner.id) },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = if (partner.profile_image.isNotEmpty()) partner.profile_image else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                if (partner.is_online) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${partner.first_name} ${partner.last_name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    if (chatItem.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = time,
                        fontSize = 11.sp,
                        color = if (unreadCount > 0) Color(0xFFFF1493) else Color.Gray,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayMessage,
                        fontSize = 13.sp,
                        color = if (unreadCount > 0) Color.Black else Color.Gray,
                        fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (unreadCount > 0) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = Color(0xFFFF1493)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_10_pro_xl"
)
@Composable
fun ModernChatItemPreview() {
    val dummyUser = User(
        id = "user1",
        first_name = "Jane",
        last_name = "Doe",
        is_online = true
    )
    val dummyMessage = Message(
        messageText = "Hello! How are you?",
        timestamp = System.currentTimeMillis()
    )
    val dummyChatItem = ChatListItem(
        partner = dummyUser,
        lastMessage = dummyMessage,
        unreadCount = 2
    )

    ModernChatItem(
        chatItem = dummyChatItem,
        currentUserId = "current_user_id",
        onChatClick = { _, _ -> },
        onLongClick = {}
    )
}


@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_10_pro_xl"
)
@Composable
fun ModernChatListScreenPreview() {
    ModernChatListScreen(onChatClick = { _, _ -> })
}
