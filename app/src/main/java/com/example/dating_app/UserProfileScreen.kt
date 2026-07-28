package com.example.dating_app

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.datingapp.R
import com.example.dating_app.model.User
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun UserProfileScreen(
    targetUserId: String? = null,
    onSubScreenChange: (String?) -> Unit = {},
    requestedSubScreen: String? = null,
    onBack: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf("profile") }
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    
    LaunchedEffect(requestedSubScreen) {
        if (requestedSubScreen != null) {
            currentScreen = requestedSubScreen
        } else {
            currentScreen = "profile"
        }
    }

    LaunchedEffect(currentScreen) {
        onSubScreenChange(if (currentScreen == "profile") null else currentScreen)
    }

    LaunchedEffect(targetUserId) {
        val uid = targetUserId ?: auth.currentUser?.uid
        if (uid != null) {
            repository.getUser(uid).onSuccess { fetchedUser ->
                user = fetchedUser
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF1493))
        }
        return
    }

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("User not found")
        }
        return
    }

    val displayUser = user!!
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    when (currentScreen) {
        "profile" -> MainProfileView(
            user = displayUser,
            onEditClick = { currentScreen = "edit" },
            onDetailsClick = { currentScreen = "details" },
            onBack = onBack
        )
        "details" -> ProfileDetailsView(
            user = displayUser,
            onBack = { currentScreen = "profile" }
        )
        "edit" -> EditProfileView(
            user = displayUser,
            onSave = { updatedUser ->
                scope.launch {
                    val uid = auth.currentUser?.uid ?: return@launch
                    val profileData = mapOf(
                        "username" to updatedUser.username,
                        "first_name" to updatedUser.first_name,
                        "last_name" to updatedUser.last_name,
                        "email" to updatedUser.email,
                        "phone" to updatedUser.phone,
                        "bio" to updatedUser.bio,
                        "occupation" to updatedUser.occupation,
                        "looking_for" to updatedUser.looking_for,
                        "interests" to updatedUser.interests,
                        "language" to updatedUser.language,
                        "country" to updatedUser.country,
                        "state" to updatedUser.state,
                        "city" to updatedUser.city,
                        "profile_image" to updatedUser.profile_image,
                        "updated_at" to System.currentTimeMillis()
                    )
                    repository.updateProfile(uid, profileData).onSuccess {
                        user = updatedUser
                        currentScreen = "profile"
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }.onFailure { e ->
                        Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBack = { currentScreen = "profile" }
        )
    }
}

@Composable
fun MainProfileView(user: User, onEditClick: () -> Unit, onDetailsClick: () -> Unit, onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(Color.White)
        ) {
            // Large Top Image
            Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                AsyncImage(
                    model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.girl)
                )
                
                // Overlay for Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
                    }
                    
                    Row {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.background(Color.Black.copy(0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDetailsClick,
                            modifier = Modifier.background(Color.Black.copy(0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White)
                        }
                    }
                }
            }

            // User Info Card (Overlapping the image slightly)
            Column(
                modifier = Modifier
                    .offset(y = (-40).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                // Name and Age
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${user.first_name}, ${calculateAge(user.dob)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Occupation
                Text(
                    text = if(user.occupation.isNotEmpty()) user.occupation else "Photographer",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFFFF1493),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if(user.city.isNotEmpty()) user.city else "New York"} \u2022 2 km away",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // About Me
                Text(
                    text = "About me",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = if (user.bio.isNotEmpty()) user.bio else "Love capturing beautiful moments and exploring new places.",
                    fontSize = 16.sp,
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Looking For
                Text(
                    text = "Looking for",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val preferences = if(user.looking_for.isNotEmpty()) user.looking_for.split(",") else listOf("Serious Relationship", "Open to Chat")
                    preferences.forEach { pref ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFF1493).copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color(0xFFFF1493).copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = pref.trim(),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = Color(0xFFFF1493),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interests
                Text(
                    text = "Interests",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val userInterests = user.interests.takeIf { it.isNotEmpty() } ?: listOf("Travel", "Photography", "Music", "Food")
                    userInterests.take(4).forEach { name ->
                        val icon = when(name.lowercase().trim()) {
                            "travel" -> Icons.Default.Flight
                            "photography" -> Icons.Default.CameraAlt
                            "music" -> Icons.Default.MusicNote
                            "food" -> Icons.Default.Favorite
                            else -> Icons.Default.AutoAwesome
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = name, tint = Color(0xFF9C27B0), modifier = Modifier.size(24.dp))
                                }
                            }
                            Text(text = name, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Space for FAB
            }
        }

        // Floating Heart Button
        FloatingActionButton(
            onClick = { /* Handle Like */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp),
            containerColor = Color(0xFFFF1493),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Favorite, contentDescription = "Like", modifier = Modifier.size(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileView(user: User, onSave: (User) -> Unit, onBack: () -> Unit) {
    var firstName by remember { mutableStateOf(user.first_name) }
    var lastName by remember { mutableStateOf(user.last_name) }
    var occupation by remember { mutableStateOf(user.occupation) }
    var bio by remember { mutableStateOf(user.bio) }
    var lookingFor by remember { mutableStateOf(user.looking_for) }
    var interests by remember { mutableStateOf(user.interests.joinToString(", ")) }
    var phone by remember { mutableStateOf(user.phone) }
    var language by remember { mutableStateOf(user.language) }
    var country by remember { mutableStateOf(user.country) }
    var state by remember { mutableStateOf(user.state) }
    var city by remember { mutableStateOf(user.city) }
    var dob by remember { mutableStateOf(user.dob) }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val context = LocalContext.current

    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profileImageUri = uri
    }

    fun handleSave() {
        scope.launch {
            isUploading = true
            try {
                var finalProfileUrl = user.profile_image

                profileImageUri?.let { uri ->
                    repository.uploadImageToCloudinary(uri, "profile_images").onSuccess { url ->
                        finalProfileUrl = url
                    }.onFailure {
                        Toast.makeText(context, "Profile image upload failed", Toast.LENGTH_SHORT).show()
                    }
                }

                onSave(user.copy(
                    first_name = firstName,
                    last_name = lastName,
                    occupation = occupation,
                    bio = bio,
                    looking_for = lookingFor,
                    interests = interests.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    profile_image = finalProfileUrl,
                    phone = phone,
                    language = language,
                    country = country,
                    state = state,
                    city = city,
                    dob = dob,
                    updated_at = System.currentTimeMillis()
                ))
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    TextButton(onClick = { handleSave() }) {
                        Text("Save", color = Color(0xFFFF1493), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Profile Image
            Box(modifier = Modifier.size(150.dp)) {
                AsyncImage(
                    model = profileImageUri ?: if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.girl)
                )
                IconButton(
                    onClick = { profilePicker.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Change Photo", modifier = Modifier.size(20.dp), tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Name Field
            EditProfileField(label = "First Name", value = firstName, onValueChange = { firstName = it }, placeholder = "Enter your first name")

            Spacer(modifier = Modifier.height(24.dp))

            // Last Name Field
            EditProfileField(label = "Last Name", value = lastName, onValueChange = { lastName = it }, placeholder = "Enter your last name")

            Spacer(modifier = Modifier.height(24.dp))

            // Bio Field
            EditProfileField(label = "About me", value = bio, onValueChange = { bio = it }, minHeight = 100.dp, placeholder = "Tell something about yourself...")

            Spacer(modifier = Modifier.height(24.dp))

            // Phone Field
            EditProfileField(label = "Phone", value = phone, onValueChange = { phone = it }, placeholder = "+1 234 567 890")

            Spacer(modifier = Modifier.height(24.dp))

            // Occupation Field
            EditProfileField(label = "Occupation", value = occupation, onValueChange = { occupation = it }, placeholder = "Photographer")

            Spacer(modifier = Modifier.height(24.dp))

            // Looking For Field
            EditProfileField(
                label = "Looking for",
                value = lookingFor,
                onValueChange = { lookingFor = it },
                placeholder = "Select your goal"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Language Field
            EditProfileField(
                label = "Language",
                value = language,
                onValueChange = { language = it },
                placeholder = "Select Language"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Country Field
            EditProfileField(
                label = "Country",
                value = country,
                onValueChange = { country = it },
                placeholder = "Select Country"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // State Field
            EditProfileField(
                label = "State",
                value = state,
                onValueChange = { state = it },
                placeholder = "Select State"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // City Field
            EditProfileField(
                label = "City",
                value = city,
                onValueChange = { city = it },
                placeholder = "Select City"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Date of Birth Field
            EditProfileField(
                label = "Date of Birth",
                value = dob,
                onValueChange = { dob = it },
                placeholder = "DD/MM/YYYY"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Interests Field
            EditProfileField(
                label = "Interests",
                value = interests,
                onValueChange = { interests = it },
                placeholder = "Select Interests"
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.3f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFF1493))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saving changes...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    isSelection: Boolean = false,
    isMultiSelect: Boolean = false,
    options: List<String> = emptyList()
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedOptions = remember(value) { 
        if (isMultiSelect) value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableStateList()
        else mutableStateListOf<String>()
    }

    if (showDialog && options.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select $label") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isMultiSelect) {
                                        if (selectedOptions.contains(option)) selectedOptions.remove(option)
                                        else selectedOptions.add(option)
                                    } else {
                                        onValueChange(option)
                                        showDialog = false
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isMultiSelect) {
                                Checkbox(
                                    checked = selectedOptions.contains(option),
                                    onCheckedChange = null // Handled by row click
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = option, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (isMultiSelect) {
                        onValueChange(selectedOptions.joinToString(", "))
                    }
                    showDialog = false 
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                if (!isMultiSelect) {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        if (isSelection) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .clickable { showDialog = true }
                    .padding(16.dp)
            ) {
                Text(
                    text = if (value.isNotEmpty()) value else placeholder,
                    color = if (value.isNotEmpty()) Color.Black else Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
                placeholder = { Text(placeholder, color = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFFFF1493)
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
            )
        }
    }
}

fun calculateAge(dob: String): String {
    return try {
        val parts = dob.split("/")
        if (parts.size == 3) {
            val year = parts[2].toInt()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            (currentYear - year).toString()
        } else "26"
    } catch (e: Exception) { "26" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsView(user: User, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            DetailSection(title = "Personal Information", icon = Icons.Default.Person) {
                DetailItem(label = "Username", value = user.username)
                DetailItem(label = "Email", value = user.email, isVerified = user.email_verified)
                DetailItem(label = "Phone", value = user.phone, isVerified = user.phone_verified)
                DetailItem(label = "Gender", value = user.gender)
                DetailItem(label = "Date of Birth", value = user.dob)
                DetailItem(label = "Occupation", value = user.occupation)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailSection(title = "Location & Preferences", icon = Icons.Default.LocationOn) {
                DetailItem(label = "Country", value = user.country)
                DetailItem(label = "State", value = user.state)
                DetailItem(label = "City", value = user.city)
                DetailItem(label = "Language", value = user.language)
                DetailItem(label = "Looking for", value = user.looking_for)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailSection(title = "Account Status", icon = Icons.Default.Info) {
                DetailItem(label = "Is Online", value = if(user.is_online) "Online" else "Offline", showDot = true, dotColor = if(user.is_online) Color.Green else Color.Gray)
                DetailItem(label = "Last Seen", value = user.last_seen.toString())
                DetailItem(label = "Account Status", value = user.account_status, statusColor = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun DetailSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFFFF1493), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    isVerified: Boolean = false,
    showDot: Boolean = false,
    dotColor: Color = Color.Transparent,
    statusColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (statusColor != Color.Unspecified) statusColor else Color.Black
            )
            if (isVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
