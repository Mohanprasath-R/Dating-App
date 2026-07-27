package com.example.dating_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationService
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.plugin.invitation.ZegoInvitationType
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import android.app.Activity
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.datingapp.R
import com.example.dating_app.model.Message
import com.example.dating_app.model.MessageType
import com.example.dating_app.model.User
import com.example.dating_app.model.Call
import com.example.dating_app.repository.FirebaseRepository
import com.example.dating_app.util.CloudinaryHelper
import com.example.dating_app.util.MediaDownloader
import com.example.dating_app.util.SecurityUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.spec.SecretKeySpec

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chatName = intent.getStringExtra("CHAT_NAME") ?: "User"
        val receiverId = intent.getStringExtra("RECEIVER_ID") ?: ""
        
        if (receiverId.isEmpty()) {
            android.util.Log.e("ChatActivity", "Error: RECEIVER_ID is missing")
            finish()
            return
        }

        setContent {
            MaterialTheme {
                ChatScreen(
                    chatName = chatName, 
                    receiverId = receiverId,
                    onBack = { finish() }
                )
            }
        }
    }
}

enum class CallType { NONE, AUDIO, VIDEO }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(chatName: String, receiverId: String, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val messages = remember { mutableStateListOf<Message>() }
    var receiverUser by remember { mutableStateOf<User?>(null) }
    var currentUserProfile by remember { mutableStateOf<User?>(null) }
    var incomingCall by remember { mutableStateOf<Call?>(null) }

    // Security States
    var isE2EEEnabled by remember { mutableStateOf(true) }
    var selfDestructSeconds by remember { mutableIntStateOf(0) }
    val chatKey = remember(currentUser?.uid, receiverId) {
        if (currentUser != null && receiverId.isNotEmpty()) {
            SecurityUtils.generateChatKey(currentUser.uid, receiverId)
        } else null
    }

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                if (uri?.toString()?.contains("screenshots") == true) {
                    Toast.makeText(context, "Screenshot detected!", Toast.LENGTH_LONG).show()
                }
            }
        }
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    LaunchedEffect(receiverId) {
        if (receiverId.isNotEmpty()) {
            repository.getUser(receiverId).onSuccess { user ->
                receiverUser = user
            }
        }
        if (currentUser != null && receiverId.isNotEmpty()) {
            // Aggressive mark as read on entry
            repository.markMessagesAsRead(currentUser.uid, receiverId)
            
            repository.getMessages(currentUser.uid, receiverId).collectLatest { updatedMessages ->
                messages.clear()
                messages.addAll(updatedMessages)
                
                // Mark incoming messages as read while chat is open
                val unreadIds = updatedMessages.filter { 
                    it.receiverId == currentUser.uid && !it.isRead 
                }.map { it.id }
                
                if (unreadIds.isNotEmpty()) {
                    repository.markMessagesAsReadByIds(unreadIds)
                }
            }
        }
    }

    // Secondary safety check to mark as read when messages list changes
    LaunchedEffect(messages.toList()) {
        if (currentUser != null && receiverId.isNotEmpty()) {
            val unreadIds = messages.filter { 
                it.receiverId == currentUser.uid && !it.isRead 
            }.map { it.id }
            
            if (unreadIds.isNotEmpty()) {
                repository.markMessagesAsReadByIds(unreadIds)
            }
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            repository.getUser(uid).onSuccess { user ->
                currentUserProfile = user
            }
            repository.observeIncomingCalls(uid).collectLatest { calls ->
                incomingCall = calls.firstOrNull()
            }
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var activeCall by remember { mutableStateOf(CallType.NONE) }
    var currentCallId by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    
    // Message Options State
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var forwardingMessage by remember { mutableStateOf<Message?>(null) }
    var showForwardDialog by remember { mutableStateOf(false) }
    
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    var recordingStartTime by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    if (showReportDialog) {
        var selectedReason by remember { mutableStateOf("") }
        val reasons = listOf("Harassment", "Fake Profile", "Spam", "Inappropriate Content", "Other")
        
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report User") },
            text = {
                Column {
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                            Text(text = reason, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedReason.isNotEmpty() && currentUser != null && receiverId.isNotEmpty()) {
                            scope.launch {
                                repository.reportUser(currentUser.uid, receiverId, selectedReason)
                                showReportDialog = false
                                Toast.makeText(context, "User reported successfully", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    },
                    enabled = selectedReason.isNotEmpty()
                ) {
                    Text("Report", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
        audioFile = file
        recordingStartTime = System.currentTimeMillis()
        
        try {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording(send: Boolean) {
        val duration = if (recordingStartTime > 0) System.currentTimeMillis() - recordingStartTime else 0L
        recordingStartTime = 0L

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        isRecording = false

        if (send && audioFile != null) {
            if (currentUser != null && receiverId.isNotEmpty()) {
                scope.launch {
                    val uri = Uri.fromFile(audioFile)
                    val result = CloudinaryHelper.uploadMedia(uri, "audio")
                    if (result != null) {
                        repository.sendMessage(Message(
                            senderId = currentUser.uid,
                            receiverId = receiverId,
                            messageText = "Voice Note",
                            messageType = MessageType.AUDIO,
                            mediaUrl = result.url,
                            mediaPublicId = result.publicId,
                            duration = formatDuration(duration),
                            timestamp = System.currentTimeMillis()
                        ))
                    } else {
                        Toast.makeText(context, "Voice note upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var activeVideoUri by remember { mutableStateOf<Uri?>(null) }
    var activeImageUri by remember { mutableStateOf<Uri?>(null) }
    val audioPlayer = remember { ExoPlayer.Builder(context).build() }
    var currentlyPlayingAudioId by remember { mutableStateOf<String?>(null) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var audioTotalDuration by remember { mutableLongStateOf(0L) }

    DisposableEffect(audioPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    currentlyPlayingAudioId = null
                    playbackPosition = 0L
                }
            }
        }
        audioPlayer.addListener(listener)
        onDispose {
            audioPlayer.removeListener(listener)
            audioPlayer.release()
        }
    }

    LaunchedEffect(currentlyPlayingAudioId) {
        if (currentlyPlayingAudioId != null) {
            while (currentlyPlayingAudioId != null) {
                playbackPosition = audioPlayer.currentPosition
                audioTotalDuration = if (audioPlayer.duration > 0) audioPlayer.duration else 0L
                delay(100)
            }
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (currentUser != null && receiverId.isNotEmpty()) {
                scope.launch {
                    val mimeType = if (it.authority != null) context.contentResolver.getType(it) else null
                    val type = when {
                        mimeType?.startsWith("video") == true -> MessageType.VIDEO
                        mimeType?.startsWith("audio") == true -> MessageType.AUDIO
                        mimeType?.startsWith("image") == true -> MessageType.IMAGE
                        else -> MessageType.IMAGE
                    }
                    
                    val uploadType = when (type) {
                        MessageType.VIDEO -> "video"
                        MessageType.AUDIO -> "audio"
                        else -> "image"
                    }
                    
                    Toast.makeText(context, "Uploading ${uploadType}...", Toast.LENGTH_SHORT).show()
                    
                    val result = CloudinaryHelper.uploadMedia(it, uploadType)
                    if (result != null) {
                        val duration = if (type == MessageType.AUDIO) {
                            getAudioDuration(context, it)
                        } else null

                        repository.sendMessage(Message(
                            senderId = currentUser.uid,
                            receiverId = receiverId,
                            messageText = uploadType.replaceFirstChar { it.uppercase() },
                            messageType = type,
                            mediaUrl = result.url,
                            mediaPublicId = result.publicId,
                            duration = duration,
                            timestamp = System.currentTimeMillis()
                        ))
                    } else {
                        Toast.makeText(context, "Media upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (receiverUser != null) "${receiverUser?.first_name} ${receiverUser?.last_name}" else chatName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (receiverUser?.is_online == true) Color(0xFF4CAF50) else Color.Gray))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (receiverUser?.is_online == true) "Online" else "Offline", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }
                            AsyncImage(
                                model = if (receiverUser?.profile_image?.isNotEmpty() == true) receiverUser?.profile_image else R.drawable.girl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.girl)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val invitees = Collections.singletonList(com.zegocloud.uikit.service.defines.ZegoUIKitUser(receiverId, chatName))
                            com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService.sendInvitationWithUIChange(
                                context as Activity, invitees, ZegoInvitationType.VOICE_CALL, null
                            )
                        }) {
                            Icon(painter = painterResource(id = R.drawable.ic_call_pink), contentDescription = "Voice Call", modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = {
                            val invitees = Collections.singletonList(com.zegocloud.uikit.service.defines.ZegoUIKitUser(receiverId, chatName))
                            com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService.sendInvitationWithUIChange(
                                context as Activity, invitees, ZegoInvitationType.VIDEO_CALL, null
                            )
                        }) {
                            Icon(painter = painterResource(id = R.drawable.ic_video_pink), contentDescription = "Video Call", modifier = Modifier.size(24.dp))
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                            DropdownMenu(
                                expanded = showMenu, 
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View Profile") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = { 
                                        showMenu = false
                                        val intent = Intent(context, PartnerProfileActivity::class.java)
                                        intent.putExtra("USER_ID", receiverId)
                                        context.startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isE2EEEnabled) "Disable Encryption" else "Enable Encryption") },
                                    leadingIcon = { 
                                        Icon(
                                            imageVector = if (isE2EEEnabled) Icons.Default.Https else Icons.Default.LockOpen, 
                                            contentDescription = null,
                                            tint = if (isE2EEEnabled) Color(0xFF4CAF50) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        ) 
                                    },
                                    onClick = { 
                                        isE2EEEnabled = !isE2EEEnabled
                                        showMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Self-destruct Timer: ${if(selfDestructSeconds > 0) "${selfDestructSeconds}s" else "Off"}") },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Timer, 
                                            contentDescription = null, 
                                            tint = if (selfDestructSeconds > 0) Color(0xFFFF9800) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        ) 
                                    },
                                    onClick = { 
                                        selfDestructSeconds = when(selfDestructSeconds) {
                                            0 -> 10
                                            10 -> 60
                                            60 -> 3600
                                            else -> 0
                                        }
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report User") },
                                    leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMenu = false; showReportDialog = true }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.2f))
                                DropdownMenuItem(
                                    text = { Text("Block User", color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp)) },
                                    onClick = { 
                                        showMenu = false 
                                        scope.launch { currentUser?.let { repository.blockUser(it.uid, receiverId) }; onBack() }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Column {
                    if (replyingToMessage != null || editingMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = Color(0xFFFC2C5A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (editingMessage != null) "Editing Message" else "Replying to ${if (replyingToMessage?.senderId == currentUser?.uid) "yourself" else receiverUser?.first_name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFFC2C5A)
                                    )
                                    val previewText = remember(replyingToMessage, editingMessage, chatKey) {
                                        val msg = editingMessage ?: replyingToMessage
                                        if (msg == null) ""
                                        else if (msg.encrypted && chatKey != null) {
                                            try { SecurityUtils.decrypt(msg.messageText, chatKey) } catch (e: Exception) { msg.messageText }
                                        } else msg.messageText
                                    }
                                    Text(
                                        text = previewText,
                                        maxLines = 1,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = {
                                    replyingToMessage = null
                                    editingMessage = null
                                    if (editingMessage != null) messageText = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    ChatBottomBar(
                        messageText = messageText,
                        isRecording = isRecording,
                        onValueChange = { messageText = it },
                        onAttachClick = { mediaPickerLauncher.launch("*/*") },
                        onSendClick = {
                            if (messageText.isNotBlank()) {
                                if (SecurityUtils.isSpam(messageText)) {
                                    Toast.makeText(context, "Spam detected!", Toast.LENGTH_SHORT).show()
                                    return@ChatBottomBar
                                }
                                val textToSend = if (isE2EEEnabled && chatKey != null) {
                                    SecurityUtils.encrypt(messageText, chatKey)
                                } else messageText

                                scope.launch {
                                    currentUser?.let {
                                        if (editingMessage != null) {
                                            repository.updateMessage(editingMessage!!.id, mapOf(
                                                "messageText" to textToSend,
                                                "edited" to true
                                            ))
                                            editingMessage = null
                                        } else {
                                            repository.sendMessage(Message(
                                                senderId = it.uid,
                                                receiverId = receiverId,
                                                messageText = textToSend,
                                                encrypted = isE2EEEnabled,
                                                replyToId = replyingToMessage?.id,
                                                selfDestructAt = if (selfDestructSeconds > 0) System.currentTimeMillis() + (selfDestructSeconds * 1000) else null,
                                                timestamp = System.currentTimeMillis()
                                            ))
                                            replyingToMessage = null
                                        }
                                    }
                                }
                                messageText = ""
                            }
                        },
                        onMicClick = { startRecording() },
                        onSendVoice = { stopRecording(true) },
                        onCancelVoice = { stopRecording(false) }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val filteredMessages = messages.filter { !it.deletedForMe.contains(currentUser?.uid ?: "") }
                items(filteredMessages) { message ->
                    ChatBubble(
                        message = message,
                        currentUserId = currentUser?.uid ?: "",
                        receiverUser = receiverUser,
                        currentUserProfile = currentUserProfile,
                        isPlaying = currentlyPlayingAudioId == message.id,
                        playbackProgress = if (currentlyPlayingAudioId == message.id && audioTotalDuration > 0) playbackPosition.toFloat() / audioTotalDuration else 0f,
                        currentPositionText = if (currentlyPlayingAudioId == message.id) formatDuration(playbackPosition) else (message.duration ?: "0:00"),
                        chatKey = chatKey,
                        onMediaClick = {
                            if (message.messageType == MessageType.VIDEO) activeVideoUri = Uri.parse(message.mediaUrl)
                            else if (message.messageType == MessageType.IMAGE) activeImageUri = Uri.parse(message.mediaUrl)
                            else if (message.messageType == MessageType.AUDIO) {
                                if (currentlyPlayingAudioId == message.id) { if (audioPlayer.isPlaying) audioPlayer.pause() else audioPlayer.play() }
                                else {
                                    currentlyPlayingAudioId = message.id
                                    audioPlayer.stop(); audioPlayer.setMediaItem(MediaItem.fromUri(message.mediaUrl ?: "")); audioPlayer.prepare(); audioPlayer.play()
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedMessageForMenu = message
                        }
                    )
                }
            }
            LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
        }

        if (selectedMessageForMenu != null) {
            MessageOptionsMenu(
                message = selectedMessageForMenu!!,
                isMe = selectedMessageForMenu!!.senderId == currentUser?.uid,
                onReaction = { emoji ->
                    scope.launch {
                        currentUser?.let { repository.addReaction(selectedMessageForMenu!!.id, it.uid, emoji) }
                    }
                },
                onReply = { replyingToMessage = selectedMessageForMenu },
                onEdit = { 
                    editingMessage = selectedMessageForMenu
                    messageText = if (selectedMessageForMenu!!.encrypted && chatKey != null) {
                        try { SecurityUtils.decrypt(selectedMessageForMenu!!.messageText, chatKey) } catch (e: Exception) { selectedMessageForMenu!!.messageText }
                    } else selectedMessageForMenu!!.messageText
                },
                onForward = { 
                    forwardingMessage = selectedMessageForMenu
                    showForwardDialog = true
                },
                onCopy = {
                    val content = if (selectedMessageForMenu!!.encrypted && chatKey != null) {
                        SecurityUtils.decrypt(selectedMessageForMenu!!.messageText, chatKey)
                    } else selectedMessageForMenu!!.messageText
                    clipboardManager.setText(AnnotatedString(content))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                onUnsend = {
                    scope.launch { repository.deleteMessage(selectedMessageForMenu!!.id) }
                },
                onDeleteForMe = {
                    scope.launch { currentUser?.let { repository.deleteMessageForMe(selectedMessageForMenu!!.id, it.uid) } }
                },
                onDismiss = { selectedMessageForMenu = null }
            )
        }

        if (activeVideoUri != null) VideoPlayerDialog(uri = activeVideoUri!!) { activeVideoUri = null }
        if (activeImageUri != null) ImagePreviewDialog(uri = activeImageUri!!) { activeImageUri = null }

        if (showForwardDialog && forwardingMessage != null) {
            ForwardMessageDialog(
                message = forwardingMessage!!,
                onDismiss = { 
                    showForwardDialog = false
                    forwardingMessage = null
                },
                onForwardTo = { partnerId ->
                    scope.launch {
                        val forwardedMsg = forwardingMessage!!.copy(
                            id = "",
                            senderId = currentUser?.uid ?: "",
                            receiverId = partnerId,
                            timestamp = System.currentTimeMillis(),
                            isRead = false,
                            reactions = emptyMap()
                        )
                        repository.sendMessage(forwardedMsg)
                        Toast.makeText(context, "Message forwarded", Toast.LENGTH_SHORT).show()
                        showForwardDialog = false
                        forwardingMessage = null
                    }
                }
            )
        }
    }
}

@Composable
fun ForwardMessageDialog(
    message: Message,
    onDismiss: () -> Unit,
    onForwardTo: (String) -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    var matches by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var forwardedUserIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        currentUser?.let {
            repository.getMatches(it.uid).onSuccess { list ->
                matches = list
                isLoading = false
            }
        }
    }

    val filteredMatches = remember(matches, searchQuery) {
        if (searchQuery.isEmpty()) matches
        else matches.filter { 
            it.first_name.contains(searchQuery, ignoreCase = true) || 
            it.last_name.contains(searchQuery, ignoreCase = true) 
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Forward to",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape).size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp), tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Search Bar
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        placeholder = { Text("Search friends...", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFFC2C5A)) },
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8F8F8),
                            unfocusedContainerColor = Color(0xFFF8F8F8),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // List
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFC2C5A))
                        }
                    } else if (filteredMatches.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No connections found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredMatches) { user ->
                                val isForwarded = forwardedUserIds.contains(user.id)
                                
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(enabled = !isForwarded) { 
                                            forwardedUserIds = forwardedUserIds + user.id
                                            onForwardTo(user.id)
                                        },
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "${user.first_name} ${user.last_name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.Black
                                            )
                                            Text(
                                                if (user.is_online) "Online" else "Recently active",
                                                fontSize = 12.sp,
                                                color = if (user.is_online) Color(0xFF4CAF50) else Color.Gray
                                            )
                                        }

                                        Button(
                                            onClick = { 
                                                forwardedUserIds = forwardedUserIds + user.id
                                                onForwardTo(user.id)
                                            },
                                            enabled = !isForwarded,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isForwarded) Color(0xFFEEEEEE) else Color(0xFFFC2C5A),
                                                contentColor = if (isForwarded) Color.Gray else Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text(
                                                if (isForwarded) "Sent" else "Send",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8F8F8)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                    ) {
                        Text("Done", color = Color(0xFFFC2C5A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageOptionsMenu(
    message: Message,
    isMe: Boolean,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onUnsend: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Reactions Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val reactions = listOf("❤️", "😂", "😮", "😢", "😡", "👍")
                    reactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clickable { onReaction(emoji); onDismiss() }
                                .padding(4.dp)
                        )
                    }
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                    )
                }

                HorizontalDivider(color = Color.DarkGray)

                // Menu Options
                MenuOptionItem("Reply", Icons.AutoMirrored.Filled.Reply, Color.White) { onReply(); onDismiss() }
                if (isMe && message.messageType == MessageType.TEXT) {
                    MenuOptionItem("Edit", Icons.Default.Edit, Color.White) { onEdit(); onDismiss() }
                }
                MenuOptionItem("Forward", Icons.Default.Send, Color.White) { onForward(); onDismiss() }
                if (message.messageType == MessageType.TEXT) {
                    MenuOptionItem("Copy", Icons.Default.ContentCopy, Color.White) { onCopy(); onDismiss() }
                }
                
                if (isMe) {
                    MenuOptionItem("Delete for both", Icons.Default.Undo, Color.Red) { onUnsend(); onDismiss() }
                }
                
                MenuOptionItem("Delete for me", Icons.Default.DeleteOutline, Color.White) { onDeleteForMe(); onDismiss() }
            }
        }
    }
}

@Composable
fun MenuOptionItem(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: Message,
    currentUserId: String,
    receiverUser: User?,
    currentUserProfile: User?,
    isPlaying: Boolean,
    playbackProgress: Float,
    currentPositionText: String,
    chatKey: SecretKeySpec?,
    onMediaClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isMe = message.senderId == currentUserId
    val bubbleColor = if (isMe) Color(0xFFFC2C5A) else Color.White
    val textColor = if (isMe) Color.White else Color.Black
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) RoundedCornerShape(18.dp, 18.dp, 2.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 2.dp)
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        if (message.selfDestructAt != null && !message.deletedForEveryone) {
            val currentTime = System.currentTimeMillis()
            val timeLeft = (message.selfDestructAt - currentTime) / 1000
            
            if (timeLeft <= 0) {
                // Trigger deletion
                LaunchedEffect(message.id) {
                    onLongClick() // This was mapped to delete in the caller
                }
            } else {
                Text(text = "Expires in ${timeLeft}s", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
            }
        }

        Surface(
            color = if (message.deletedForEveryone) Color.LightGray.copy(alpha = 0.2f) else bubbleColor,
            shape = shape,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = if (message.messageType == MessageType.TEXT) 300.dp else 260.dp)
                .combinedClickable(onClick = onMediaClick, onLongClick = onLongClick)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (message.deletedForEveryone) {
                    Text(
                        text = "This message was deleted",
                        modifier = Modifier.padding(12.dp),
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    Column {
                        if (message.replyToId != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "Replying to message",
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                        }

                        when (message.messageType) {
                            MessageType.TEXT -> {
                                val displayContent = if (message.encrypted) {
                                    if (chatKey != null) {
                                        SecurityUtils.decrypt(message.messageText, chatKey)
                                    } else {
                                        "[Encrypted Message]"
                                    }
                                } else message.messageText

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayContent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        color = textColor,
                                        fontSize = 14.sp
                                    )
                                    if (message.encrypted) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = textColor.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .size(10.dp)
                                                .padding(end = 8.dp)
                                        )
                                    }
                                }
                            }
                            MessageType.AUDIO -> {
                                val senderProfileImage =
                                    if (isMe) currentUserProfile?.profile_image else receiverUser?.profile_image
                                AudioMessageItem(
                                    senderProfileImage,
                                    isMe,
                                    isPlaying,
                                    playbackProgress,
                                    currentPositionText,
                                    message.id.hashCode(),
                                    onMediaClick
                                )
                            }
                            MessageType.VIDEO -> VideoMessageItem(message, onMediaClick)
                            MessageType.IMAGE -> ImageMessageItem(message, onMediaClick)
                        }
                    }
                }

                // Reactions Display
                if (message.reactions.isNotEmpty() && !message.deletedForEveryone) {
                    Row(
                        modifier = Modifier
                            .padding(end = 4.dp, bottom = 4.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val reactionList = message.reactions.values.distinct().take(3)
                        reactionList.forEach { emoji ->
                            Text(text = emoji, fontSize = 10.sp)
                        }
                        if (message.reactions.size > 1) {
                            Text(
                                text = message.reactions.size.toString(),
                                fontSize = 8.sp,
                                modifier = Modifier.padding(start = 2.dp),
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = time, fontSize = 10.sp, color = Color.Gray)
            if (message.edited) {
                Text(text = " • Edited", fontSize = 10.sp, color = Color.Gray)
            }
            if (isMe) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = if (message.isRead) Color(0xFFFC2C5A) else Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun AudioMessageItem(senderProfileImage: String?, isMe: Boolean, isPlaying: Boolean, playbackProgress: Float, currentPositionText: String, seed: Int, onPlayClick: () -> Unit) {
    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(model = if (senderProfileImage?.isNotEmpty() == true) senderProfileImage else R.drawable.girl, contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop, placeholder = painterResource(id = R.drawable.girl))
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White).padding(2.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = if (isMe) Color(0xFFFC2C5A) else Color.Gray, modifier = Modifier.size(10.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlayClick, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = if (isMe) Color.White else Color(0xFFFC2C5A), modifier = Modifier.size(28.dp))
                }
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f).height(24.dp)) {
                    AudioWaveform(playbackProgress, if (isMe) Color.White else Color(0xFFFC2C5A), seed)
                }
            }
            Text(text = currentPositionText, fontSize = 11.sp, color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray, modifier = Modifier.padding(start = 36.dp))
        }
    }
}

@Composable
fun AudioWaveform(progress: Float, color: Color, seed: Int = 0) {
    val barCount = 35
    val heights = remember(seed) { List(barCount) { (6..18).random().dp } }
    Row(modifier = Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(barCount) { index ->
            val isPlayed = (index.toFloat() / barCount) <= progress
            Box(modifier = Modifier.width(2.dp).height(heights[index]).clip(CircleShape).background(if (isPlayed) color else color.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun ImageMessageItem(message: Message, onClick: () -> Unit) {
    AsyncImage(model = message.mediaUrl, contentDescription = "Image", modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clickable { onClick() }, contentScale = ContentScale.Crop)
}

@Composable
fun VideoMessageItem(message: Message, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        AsyncImage(model = message.mediaUrl, contentDescription = "Video", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

fun getAudioDuration(context: android.content.Context, uri: Uri): String {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        formatDuration(durationMs)
    } catch (e: Exception) { "0:00" } finally { retriever.release() }
}

@Composable
fun ChatBottomBar(messageText: String, isRecording: Boolean, onValueChange: (String) -> Unit, onAttachClick: () -> Unit, onSendClick: () -> Unit, onMicClick: () -> Unit, onSendVoice: () -> Unit, onCancelVoice: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
        Row(modifier = Modifier.padding(8.dp).navigationBarsPadding().imePadding().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!isRecording) IconButton(onClick = onAttachClick) { Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color.Gray) }
            Box(modifier = Modifier.weight(1f)) {
                if (isRecording) RecordingUI(onCancel = onCancelVoice)
                else {
                    TextField(
                        value = messageText, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type a message...", fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFF0F0F0), unfocusedContainerColor = Color(0xFFF0F0F0), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        shape = RoundedCornerShape(24.dp), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        leadingIcon = { Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Emoji", tint = Color.Gray) }
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = if (isRecording) onSendVoice else if (messageText.isNotBlank()) onSendClick else onMicClick, modifier = Modifier.size(42.dp).background(Color(0xFFFC2C5A), CircleShape)) {
                Icon(imageVector = if (isRecording || messageText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic, contentDescription = "Action", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}



@Composable
fun RecordingUI(onCancel: () -> Unit) {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while(true) { delay(1000); seconds++ } }
    Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFFC2C5A), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60), fontSize = 14.sp, color = Color.Black, modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel) { Text("Cancel", color = Color.Gray) }
    }
}

@Composable
private fun VideoPlayerDialog(uri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); prepare(); playWhenReady = true } }
        DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { PlayerView(context).apply { player = exoPlayer } }, modifier = Modifier.fillMaxSize())
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
        }
    }
}

@Composable
private fun ImagePreviewDialog(uri: Uri, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
        }
    }
}
