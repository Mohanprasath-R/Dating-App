package com.example.dating_app

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import com.example.dating_app.util.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*

class CompleteProfileActivity : ComponentActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompleteProfileScreen(
                    onComplete = { profileData, imageUri ->
                        saveProfile(profileData, imageUri)
                    },
                    onSkip = {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun saveProfile(profileData: Map<String, Any>, imageUri: Uri?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            val finalData = profileData.toMutableMap()
            
            imageUri?.let { uri ->
                val compressedUri = ImageUtils.compressImage(this@CompleteProfileActivity, uri)
                val uploadResult = repository.uploadImageToCloudinary(compressedUri, "profile_images")
                uploadResult.onSuccess { url ->
                    finalData["profile_image"] = url
                }
            }

            repository.updateProfile(uid, finalData).onSuccess {
                Toast.makeText(this@CompleteProfileActivity, "Profile updated!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@CompleteProfileActivity, SetPinActivity::class.java).apply {
                    putExtra("USER_ID", uid)
                })
                finish()
            }.onFailure { e ->
                Toast.makeText(this@CompleteProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompleteProfileScreen(
    onComplete: (Map<String, Any>, Uri?) -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    
    // State for all slides
    var fullName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var email by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.email ?: "") }

    LaunchedEffect(uid) {
        uid?.let {
            repository.getUser(it).onSuccess { user ->
                user?.let {
                    if (fullName.isEmpty()) fullName = it.first_name
                    if (dob.isEmpty()) dob = it.dob
                    if (gender == "Female" && it.gender.isNotEmpty()) gender = it.gender
                    if (email.isEmpty()) email = it.email
                }
            }
        }
    }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var languages by remember { mutableStateOf("") }
    
    val interestsList = listOf(
        InterestItem("Travel", Icons.Default.AirplanemodeActive),
        InterestItem("Music", Icons.Default.MusicNote),
        InterestItem("Movies", Icons.Default.Movie),
        InterestItem("Sports", Icons.Default.SportsBasketball),
        InterestItem("Food", Icons.Default.Restaurant),
        InterestItem("Reading", Icons.Default.MenuBook),
        InterestItem("Photography", Icons.Default.CameraAlt),
        InterestItem("Fitness", Icons.Default.FitnessCenter),
        InterestItem("Art", Icons.Default.Palette),
        InterestItem("Dancing", Icons.Default.SelfImprovement)
    )
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var otherInterest by remember { mutableStateOf("") }

    val pinkColor = Color(0xFFFE3C72)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                
                // Progress Indicator with Lines
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { index ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(1.dp)
                                    .background(if (pagerState.currentPage >= index) pinkColor else Color.LightGray)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 36.dp else 24.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == index) pinkColor else if (pagerState.currentPage > index) pinkColor.copy(alpha = 0.2f) else Color(0xFFF5F5F5))
                                .border(1.dp, if (pagerState.currentPage >= index) pinkColor else Color.LightGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (pagerState.currentPage == index) "${index + 1}/3" else "${index + 1}",
                                color = if (pagerState.currentPage == index) Color.White else if (pagerState.currentPage > index) pinkColor else Color.Gray,
                                fontSize = if (pagerState.currentPage == index) 12.sp else 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                TextButton(onClick = onSkip) {
                    Text("Skip", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) { padding ->
        val backgroundGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF5F8), Color.White)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundGradient)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> BasicsSlide(
                        fullName = fullName, onFullNameChange = { fullName = it },
                        dob = dob, onDobChange = { dob = it },
                        gender = gender, onGenderChange = { gender = it },
                        imageUri = profileImageUri, onImagePick = { profileImageUri = it }
                    )
                    1 -> DetailsSlide(
                        email = email, onEmailChange = { email = it },
                        phone = phone, onPhoneChange = { phone = it },
                        country = country, onCountryChange = { country = it },
                        state = state, onStateChange = { state = it },
                        city = city, onCityChange = { city = it },
                        languages = languages, onLanguagesChange = { languages = it }
                    )
                    2 -> InterestsSlide(
                        interests = interestsList,
                        selectedInterests = selectedInterests,
                        onInterestToggle = { interest ->
                            selectedInterests = if (selectedInterests.contains(interest)) {
                                selectedInterests - interest
                            } else {
                                selectedInterests + interest
                            }
                        },
                        otherInterest = otherInterest,
                        onOtherInterestChange = { otherInterest = it }
                    )
                }
            }

            // Bottom Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            val data = mapOf(
                                "first_name" to fullName,
                                "dob" to dob,
                                "gender" to gender,
                                "email" to email,
                                "phone" to phone,
                                "country" to country,
                                "state" to state,
                                "city" to city,
                                "city_lowercase" to city.lowercase().trim(),
                                "language" to languages,
                                "interests" to selectedInterests.toList(),
                                "other_interest" to otherInterest
                            )
                            onComplete(data, profileImageUri)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFE3C72), Color(0xFFFF597B)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (pagerState.currentPage < 2) "Continue" else "Complete Profile 🎉",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun BasicsSlide(
    fullName: String, onFullNameChange: (String) -> Unit,
    dob: String, onDobChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    imageUri: Uri?, onImagePick: (Uri?) -> Unit
) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        onImagePick(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Let's start with your basics 💖",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "This helps people find you.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Image Picker
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0F5))
                    .border(2.dp, Color(0xFFFE3C72).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.LightGray
                    )
                }
            }
            
            FloatingActionButton(
                onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                containerColor = Color.White,
                contentColor = Color(0xFFFE3C72),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Add Photo", modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFE3C72)) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFE3C72),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dob,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Date of Birth") },
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFFE3C72)) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFE3C72),
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, year, month, day ->
                            onDobChange("$day/${month + 1}/$year")
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "I am",
            modifier = Modifier.align(Alignment.Start),
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            ProfileGenderButton(
                text = "Female",
                isSelected = gender == "Female",
                icon = Icons.Default.Female,
                onClick = { onGenderChange("Female") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            ProfileGenderButton(
                text = "Male",
                isSelected = gender == "Male",
                icon = Icons.Default.Male,
                onClick = { onGenderChange("Male") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DetailsSlide(
    email: String, onEmailChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    country: String, onCountryChange: (String) -> Unit,
    state: String, onStateChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    languages: String, onLanguagesChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Almost there! ✨",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Tell us more about you.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        DetailField(value = email, onValueChange = onEmailChange, label = "Email Address", icon = Icons.Default.Email)
        DetailField(value = phone, onValueChange = onPhoneChange, label = "Phone Number", icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)
        
        val countryList = listOf("USA", "UK", "India", "Canada", "Australia", "Germany", "France")
        val languageList = listOf("English", "Spanish", "Hindi", "French", "German", "Chinese")
        val stateList = listOf("California", "New York", "Texas", "Maharashtra", "Delhi", "London", "Ontario")
        val cityList = listOf("New York", "London", "Mumbai", "Paris", "Tokyo", "Los Angeles", "Chicago")

        DetailField(
            value = country,
            onValueChange = onCountryChange,
            label = "Country",
            icon = Icons.Default.Public,
            isDropdown = true,
            options = countryList
        )
        DetailField(
            value = state,
            onValueChange = onStateChange,
            label = "State / Province",
            icon = Icons.Default.LocationCity,
            isDropdown = true,
            options = stateList
        )
        DetailField(
            value = city,
            onValueChange = onCityChange,
            label = "City",
            icon = Icons.Default.LocationOn,
            isDropdown = true,
            options = cityList
        )
        DetailField(
            value = languages,
            onValueChange = onLanguagesChange,
            label = "Languages",
            icon = Icons.Default.Translate,
            isDropdown = true,
            options = languageList
        )
    }
}

@Composable
fun InterestsSlide(
    interests: List<InterestItem>,
    selectedInterests: Set<String>,
    onInterestToggle: (String) -> Unit,
    otherInterest: String,
    onOtherInterestChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Your interests 💖",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Select a few things you love.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(interests) { item ->
                InterestChip(
                    item = item,
                    isSelected = selectedInterests.contains(item.name),
                    onClick = { onInterestToggle(item.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = otherInterest,
            onValueChange = onOtherInterestChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add anything else you love...") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFE3C72),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DetailField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isDropdown: Boolean = false,
    options: List<String> = emptyList(),
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog && options.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select $label") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { option ->
                        Text(
                            text = option,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(option)
                                    showDialog = false
                                }
                                .padding(16.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (!isDropdown) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFFFE3C72)) },
            trailingIcon = { if (isDropdown) Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            readOnly = isDropdown,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFE3C72),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )
        if (isDropdown) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDialog = true }
            )
        }
    }
}

@Composable
fun ProfileGenderButton(
    text: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinkColor = Color(0xFFFE3C72)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) pinkColor else Color.LightGray.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) pinkColor.copy(alpha = 0.05f) else Color.Transparent
        )
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) pinkColor else Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = if (isSelected) pinkColor else Color.Gray)
    }
}

@Composable
fun InterestChip(
    item: InterestItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pinkColor = Color(0xFFFE3C72)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSelected) pinkColor else Color.LightGray.copy(alpha = 0.3f)),
        color = if (isSelected) pinkColor.copy(alpha = 0.05f) else Color.Transparent,
        modifier = Modifier.height(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = null, tint = if (isSelected) pinkColor else Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                item.name,
                color = if (isSelected) pinkColor else Color.Gray,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = pinkColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}


data class InterestItem(val name: String, val icon: ImageVector)
