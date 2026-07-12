package com.example.dating_app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var etGender: TextInputEditText
    private lateinit var etDob: TextInputEditText
    private lateinit var etCountry: TextInputEditText
    private lateinit var etState: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etLanguage: TextInputEditText
    private lateinit var etTimezone: TextInputEditText
    private lateinit var ivProfile: ImageView
    private lateinit var ivCover: ImageView
    private lateinit var progressBar: ProgressBar
    
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private var selectedCoverUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivProfile.load(uri) {
                transformations(CircleCropTransformation())
            }
        }
    }

    private val pickCoverLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedCoverUri = uri
            ivCover.load(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        initViews()
        loadData()

        findViewById<TextView>(R.id.btn_cancel).setOnClickListener { finish() }
        
        findViewById<TextView>(R.id.btn_save_top).setOnClickListener { saveData() }
        findViewById<Button>(R.id.btn_save_bottom).setOnClickListener { saveData() }

        findViewById<ImageView>(R.id.btn_pick_image).setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        findViewById<ImageView>(R.id.btn_pick_cover_image).setOnClickListener {
            pickCoverLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                etDob.setText(date)
            }, year, month, day).show()
        }
    }

    private fun initViews() {
        etUsername = findViewById(R.id.et_username)
        etFirstName = findViewById(R.id.et_first_name)
        etLastName = findViewById(R.id.et_last_name)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        etBio = findViewById(R.id.et_bio)
        etGender = findViewById(R.id.et_gender)
        etDob = findViewById(R.id.et_dob)
        etCountry = findViewById(R.id.et_country)
        etState = findViewById(R.id.et_state)
        etCity = findViewById(R.id.et_city)
        etLanguage = findViewById(R.id.et_language)
        etTimezone = findViewById(R.id.et_timezone)
        ivProfile = findViewById(R.id.edit_profile_image)
        ivCover = findViewById(R.id.edit_cover_image)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.getUser(uid)
            progressBar.visibility = View.GONE
            result.onSuccess { user ->
                user?.let {
                    etUsername.setText(it.username)
                    etFirstName.setText(it.first_name)
                    etLastName.setText(it.last_name)
                    etEmail.setText(it.email)
                    etPhone.setText(it.phone)
                    etBio.setText(it.bio)
                    etGender.setText(it.gender)
                    etDob.setText(it.dob)
                    etCountry.setText(it.country)
                    etState.setText(it.state)
                    etCity.setText(it.city)
                    etLanguage.setText(it.language)
                    etTimezone.setText(it.timezone)
                    
                    if (it.profile_image.isNotEmpty()) {
                        ivProfile.load(it.profile_image) {
                            transformations(CircleCropTransformation())
                        }
                    }
                    if (it.cover_image.isNotEmpty()) {
                        ivCover.load(it.cover_image)
                    }
                }
            }.onFailure {
                Toast.makeText(this@EditProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveData() {
        val uid = auth.currentUser?.uid ?: return
        
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val profileData = hashMapOf<String, Any>(
                    "username" to etUsername.text.toString(),
                    "first_name" to etFirstName.text.toString(),
                    "last_name" to etLastName.text.toString(),
                    "email" to etEmail.text.toString(),
                    "phone" to etPhone.text.toString(),
                    "bio" to etBio.text.toString(),
                    "gender" to etGender.text.toString(),
                    "dob" to etDob.text.toString(),
                    "country" to etCountry.text.toString(),
                    "state" to etState.text.toString(),
                    "city" to etCity.text.toString(),
                    "language" to etLanguage.text.toString(),
                    "timezone" to etTimezone.text.toString(),
                    "updated_at" to System.currentTimeMillis()
                )

                // Upload profile image if changed
                selectedImageUri?.let { uri ->
                    val uploadResult = repository.uploadImageToCloudinary(uri, "profile_images")
                    if (uploadResult.isSuccess) {
                        profileData["profile_image"] = uploadResult.getOrThrow()
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@EditProfileActivity, "Profile image upload failed", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                // Upload cover image if changed
                selectedCoverUri?.let { uri ->
                    val uploadResult = repository.uploadImageToCloudinary(uri, "cover_images")
                    if (uploadResult.isSuccess) {
                        profileData["cover_image"] = uploadResult.getOrThrow()
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@EditProfileActivity, "Cover image upload failed", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                val updateResult = repository.updateProfile(uid, profileData)
                progressBar.visibility = View.GONE
                
                if (updateResult.isSuccess) {
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
