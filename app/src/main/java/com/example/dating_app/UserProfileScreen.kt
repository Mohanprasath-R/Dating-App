package com.example.dating_app

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.datingapp.R
import com.example.dating_app.model.User
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun UserProfileScreen(
    onSubScreenChange: (String?) -> Unit = {},
    requestedSubScreen: String? = null
) {
    var currentScreen by remember { mutableStateOf("profile") }
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val repository = remember { FirebaseRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    
    LaunchedEffect(requestedSubScreen) {
        if (requestedSubScreen == null) {
            currentScreen = "profile"
        }
    }

    LaunchedEffect(currentScreen) {
        onSubScreenChange(if (currentScreen == "profile") null else currentScreen)
    }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
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

    when (currentScreen) {
        "profile" -> MainProfileView(
            user = displayUser,
            onEditClick = { currentScreen = "edit" },
            onDetailsClick = { currentScreen = "details" }
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
                        "profile_image" to updatedUser.profile_image,
                        "cover_image" to updatedUser.cover_image,
                        "updated_at" to System.currentTimeMillis()
                    )
                    repository.updateProfile(uid, profileData).onSuccess {
                        user = updatedUser
                        currentScreen = "profile"
                        Toast.makeText(auth.app.applicationContext, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }.onFailure { e ->
                        Toast.makeText(auth.app.applicationContext, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBack = { currentScreen = "profile" }
        )
    }
}

@Composable
fun MainProfileView(user: User, onEditClick: () -> Unit, onDetailsClick: () -> Unit) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFFAFAFA))
    ) {
        // Header with Cover and Profile Image
        Box(modifier = Modifier.height(300.dp)) {
            AsyncImage(
                model = if (user.cover_image.isNotEmpty()) user.cover_image else R.drawable.girl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.girl)
            )
            
            // More button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDetailsClick, modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)) {
                    Icon(Icons.Default.List, contentDescription = "Details", tint = Color.White)
                }
            }

            // Profile Image
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                AsyncImage(
                    model = if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.girl)
                )
                // Edit icon on profile image
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color(0xFFFF1493), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // User Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${user.first_name} ${user.last_name}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                if (user.email_verified) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                }
            }
            Text(text = "@${user.username}", color = Color.Gray, fontSize = 14.sp)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(if(user.is_online) Color.Green else Color.Gray, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = if(user.is_online) "Online" else "Offline", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFFF1493)),
                border = BorderStroke(1.dp, Color(0xFFFF1493)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (user.bio.isNotEmpty()) user.bio else "Love traveling, good coffee and meaningful conversations. ✨",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info Tags
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileTag(icon = Icons.Default.Person, text = calculateAge(user.dob), subtext = "Age")
                ProfileTag(icon = Icons.Default.Face, text = if(user.gender.isNotEmpty()) user.gender else "Female", subtext = "Gender")
                ProfileTag(icon = Icons.Default.LocationOn, text = if(user.city.isNotEmpty()) user.city else "New York", subtext = if(user.country.isNotEmpty()) user.country else "USA")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(label = "Likes", count = user.likes.toString())
                StatItem(label = "Matches", count = user.matches.toString())
                StatItem(label = "Photos", count = user.photos_count.toString())
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Photos Grid placeholder
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val photos = listOf(R.drawable.girl, R.drawable.girl, R.drawable.girl)
                photos.forEach {
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Text(
                text = "View All Photos (${user.photos_count})",
                color = Color(0xFFFF1493),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // About Me
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "About Me", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Bio", fontSize = 12.sp, color = Color.Gray)
                Text(text = if (user.bio.isNotEmpty()) user.bio else "No bio available", fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Language", fontSize = 12.sp, color = Color.Gray)
                        Text(text = if(user.language.isNotEmpty()) user.language else "English", fontSize = 14.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Country", fontSize = 12.sp, color = Color.Gray)
                        Text(text = if(user.country.isNotEmpty()) user.country else "USA", fontSize = 14.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
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

@Composable
fun ProfileDetailsView(user: User, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DetailSection(title = "Personal Information", icon = Icons.Default.Person) {
            DetailItem(label = "Username", value = user.username)
            DetailItem(label = "First Name", value = user.first_name)
            DetailItem(label = "Last Name", value = user.last_name)
            DetailItem(label = "Email", value = user.email, isVerified = user.email_verified)
            DetailItem(label = "Phone", value = user.phone, isVerified = user.phone_verified)
            DetailItem(label = "Gender", value = user.gender)
            DetailItem(label = "Date of Birth", value = user.dob)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        DetailSection(title = "Location & Preferences", icon = Icons.Default.LocationOn) {
            DetailItem(label = "Country", value = user.country)
            DetailItem(label = "State", value = user.state)
            DetailItem(label = "City", value = user.city)
            DetailItem(label = "Language", value = user.language)
            DetailItem(label = "Timezone", value = user.timezone)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        DetailSection(title = "Account Status", icon = Icons.Default.Info) {
            DetailItem(label = "Is Online", value = if(user.is_online) "Online" else "Offline", showDot = true, dotColor = if(user.is_online) Color.Green else Color.Gray)
            DetailItem(label = "Last Seen", value = user.last_seen.toString())
            DetailItem(label = "Account Status", value = user.account_status, statusColor = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun EditProfileView(user: User, onSave: (User) -> Unit, onBack: () -> Unit) {
    var username by remember { mutableStateOf(user.username) }
    var firstName by remember { mutableStateOf(user.first_name) }
    var lastName by remember { mutableStateOf(user.last_name) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var bio by remember { mutableStateOf(user.bio) }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val context = androidx.compose.ui.platform.LocalContext.current

    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profileImageUri = uri
    }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        coverImageUri = uri
    }

    fun handleSave() {
        scope.launch {
            isUploading = true
            try {
                var finalProfileUrl = user.profile_image
                var finalCoverUrl = user.cover_image

                profileImageUri?.let { uri ->
                    repository.uploadImageToCloudinary(uri, "profile_images").onSuccess { url ->
                        finalProfileUrl = url
                    }.onFailure {
                        Toast.makeText(context, "Profile image upload failed", Toast.LENGTH_SHORT).show()
                    }
                }

                coverImageUri?.let { uri ->
                    repository.uploadImageToCloudinary(uri, "cover_images").onSuccess { url ->
                        finalCoverUrl = url
                    }.onFailure {
                        Toast.makeText(context, "Cover image upload failed", Toast.LENGTH_SHORT).show()
                    }
                }

                onSave(user.copy(
                    username = username,
                    first_name = firstName,
                    last_name = lastName,
                    email = email,
                    phone = phone,
                    bio = bio,
                    profile_image = finalProfileUrl,
                    cover_image = finalCoverUrl
                ))
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUploading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) { Text("Cancel", color = Color(0xFFFF1493)) }
                TextButton(onClick = { handleSave() }) {
                    Text("Save", color = Color(0xFFFF1493), fontWeight = FontWeight.Bold)
                }
            }

            // Cover Image Edit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray.copy(0.3f))
                    .clickable { coverPicker.launch("image/*") }
            ) {
                AsyncImage(
                    model = coverImageUri ?: if (user.cover_image.isNotEmpty()) user.cover_image else R.drawable.girl,
                    contentDescription = "Cover Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.girl)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Image Edit
            Box(modifier = Modifier.size(120.dp)) {
                AsyncImage(
                    model = profileImageUri ?: if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.girl)
                )
                IconButton(
                    onClick = { profilePicker.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                        .size(32.dp)
                        .border(1.dp, Color.LightGray, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            EditField(label = "Username", value = username, onValueChange = { username = it })
            EditField(label = "First Name", value = firstName, onValueChange = { firstName = it })
            EditField(label = "Last Name", value = lastName, onValueChange = { lastName = it })
            EditField(label = "Email", value = email, onValueChange = { email = it })
            EditField(label = "Phone", value = phone, onValueChange = { phone = it })
            EditField(label = "Bio", value = bio, onValueChange = { bio = it }, isLong = true)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { handleSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1493)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                    Text("Uploading images...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditField(label: String, value: String, onValueChange: (String) -> Unit, isLong: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFFFF1493),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            minLines = if(isLong) 3 else 1
        )
    }
}

@Composable
fun ProfileTag(icon: ImageVector, text: String, subtext: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF1493))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(text = subtext, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun StatItem(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
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
