package com.example.dating_app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.datingapp.R
import com.example.dating_app.model.User
import com.example.dating_app.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.util.*

class RegisterActivity : ComponentActivity() {
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RegisterScreen(
                    onBackClick = { finish() },
                    onRegisterClick = { name, email, password, dob, gender, onComplete ->
                        lifecycleScope.launch {
                            val authResult = repository.registerUser(email, password)
                            authResult.onSuccess { uid ->
                                val user = User(
                                    id = uid,
                                    username = email.substringBefore("@"),
                                    first_name = name,
                                    email = email,
                                    gender = gender,
                                    dob = dob,
                                    created_at = System.currentTimeMillis()
                                )
                                val dbResult = repository.createUser(user)
                                dbResult.onSuccess {
                                    Toast.makeText(this@RegisterActivity, "Registration Successful", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@RegisterActivity, CompleteProfileActivity::class.java)
                                    intent.putExtra("USER_ID", uid)
                                    startActivity(intent)
                                    finish()
                                }.onFailure { e ->
                                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    onComplete()
                                }
                            }.onFailure { e ->
                                Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                                onComplete()
                            }
                        }
                    },
                    onLoginClick = { finish() }
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterClick: (String, String, String, String, String, () -> Unit) -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }
    
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val pinkGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF0F5), Color.White)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pinkGradient)
    ) {
        // Top Wave Blob
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width, 0f)
                    lineTo(width, height * 0.4f)
                    quadraticBezierTo(width * 0.75f, height * 0.7f, width * 0.5f, height * 0.4f)
                    quadraticBezierTo(width * 0.25f, height * 0.1f, 0f, height * 0.5f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF1493).copy(alpha = 0.15f), Color.Transparent)
                    )
                )
            }
        }

        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .background(Color.White, CircleShape)
                .shadow(2.dp, CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(20.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App Name
            Text(
                text = "HeyDate",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF2D6C), Color(0xFF8E24AA))
                    )
                )
            )

            Text(
                text = "Find your perfect match \uD83D\uDC9E",
                fontSize = 16.sp,
                color = Color.DarkGray.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            RegisterTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Full Name",
                leadingIcon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(12.dp))

            RegisterTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email Address",
                leadingIcon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            RegisterTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
            )

            Spacer(modifier = Modifier.height(12.dp))

            RegisterTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Confirm Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                isPasswordVisible = isConfirmPasswordVisible,
                onPasswordToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date of Birth Field
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Date of Birth", color = Color.Gray.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFFF2D6C))
                    },
                    trailingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.LightGray.copy(alpha = 0.6f))
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFFF2D6C).copy(alpha = 0.1f),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.2f),
                        cursorColor = Color.Transparent
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                val year = calendar.get(Calendar.YEAR)
                                val month = calendar.get(Calendar.MONTH)
                                val day = calendar.get(Calendar.DAY_OF_MONTH)
                                
                                DatePickerDialog(
                                    context,
                                    { _, selectedYear, selectedMonth, selectedDay ->
                                        dob = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                                    },
                                    year,
                                    month,
                                    day
                                ).show()
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gender Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "I am",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GenderButton(
                        text = "Female",
                        icon = Icons.Default.Female,
                        isSelected = gender == "Female",
                        onClick = { gender = "Female" },
                        modifier = Modifier.weight(1f)
                    )
                    GenderButton(
                        text = "Male",
                        icon = Icons.Default.Male,
                        isSelected = gender == "Male",
                        onClick = { gender = "Male" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account Button
            Button(
                onClick = {
                    if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && dob.isNotEmpty()) {
                        if (password == confirmPassword) {
                            isLoading = true
                            onRegisterClick(name, email, password, dob, gender) { isLoading = false }
                        } else {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(12.dp, RoundedCornerShape(30.dp), spotColor = Color(0xFFFF2D6C)),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFA2E69), Color(0xFFC71585))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Create Account", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Heart Divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.3f))
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF2D6C), modifier = Modifier.size(12.dp).padding(horizontal = 4.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Row {
                Text(text = "Already have an account? ", color = Color.Gray, fontSize = 14.sp)
                Text(
                    text = "Login",
                    color = Color(0xFFFF1493),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Pink Waves
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.12f)
                .align(Alignment.BottomCenter)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, height * 0.5f)
                    quadraticBezierTo(width * 0.25f, height * 0.2f, width * 0.5f, height * 0.5f)
                    quadraticBezierTo(width * 0.75f, height * 0.8f, width, height * 0.4f)
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF1493).copy(alpha = 0.05f), Color(0xFFFF1493).copy(alpha = 0.2f))
                    )
                )
            }
        }
    }
}

@Composable
fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color.Gray.copy(alpha = 0.5f)) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = Color(0xFFFF2D6C))
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.6f)
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFFFF2D6C).copy(alpha = 0.1f),
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun GenderButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFFFF2D6C) else Color.LightGray.copy(alpha = 0.3f)
    val bgColor = if (isSelected) Color(0xFFFF2D6C).copy(alpha = 0.05f) else Color.White
    val textColor = if (isSelected) Color(0xFFFF2D6C) else Color.Gray

    Box(
        modifier = modifier
            .height(58.dp)
            .shadow(
                elevation = if (isSelected) 8.dp else 0.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0xFFFF2D6C).copy(alpha = 0.5f)
            )
            .background(bgColor, RoundedCornerShape(22.dp))
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (isSelected) Color(0xFFFF2D6C) else Color.LightGray.copy(alpha = 0.2f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
