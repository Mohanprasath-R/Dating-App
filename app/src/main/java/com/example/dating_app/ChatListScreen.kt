@file:Suppress("PreviewAnnotationInFunctionWithParameters")

package com.example.dating_app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.datingapp.R
import com.example.dating_app.model.Message
import com.example.dating_app.model.MessageType
import com.example.dating_app.model.User
import com.example.dating_app.repository.ChatListItem
import com.example.dating_app.repository.FirebaseRepository
import com.example.dating_app.util.SecurityUtils
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ModernChatListScreen(onChatClick: (String, String) -> Unit, refreshTrigger: Int = 0) {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()
    var chats by remember { mutableStateOf<List<ChatListItem>>(emptyList()) }
    var requests by remember { mutableStateOf<List<User>>(emptyList()) }
    var currentUserProfile by remember { mutableStateOf<User?>(null) }
    var onlineUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Messages, 1 for Requests
    var localRefreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger, localRefreshTrigger) {
        isLoading = true
        currentUser?.let { user ->
            repository.getChatList(user.uid).onSuccess { list -> chats = list }
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
        // Search Bar Row
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

        // Horizontal Stories/Matches Bar
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

        // Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabItem(
                title = "Messages",
                badgeCount = totalUnreadCount,
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabItem(
                title = "Requests",
                badgeCount = requests.size,
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                            ModernChatItem(chatItem, currentUser?.uid, onChatClick)
                        }
                    }
                } else {
                    // Requests Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibleRequests) { user ->
                            ModernRequestItem(
                                user = user,
                                onAccept = {
                                    scope.launch {
                                        currentUser?.let { me ->
                                            repository.likeProfile(me.uid, user.id).onSuccess {
                                                localRefreshTrigger++
                                            }
                                        }
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        currentUser?.let { me ->
                                            repository.blockUser(me.uid, user.id).onSuccess {
                                                localRefreshTrigger++
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
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
        modifier = Modifier.fillMaxWidth(),
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
                    model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.first_name} ${user.last_name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    text = "Interested in you!",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF5F5F5), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFF1493), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Accept",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernChatItem(chatItem: ChatListItem, currentUserId: String?, onChatClick: (String, String) -> Unit) {
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
            .clickable { onChatClick("${partner.first_name} ${partner.last_name}", partner.id) },
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
        onChatClick = { _, _ -> }
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
