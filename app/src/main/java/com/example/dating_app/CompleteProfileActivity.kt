package com.example.dating_app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
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

class CompleteProfileActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etGender: TextInputEditText
    private lateinit var etDob: TextInputEditText
    private lateinit var etCountry: TextInputEditText
    private lateinit var etState: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etLanguage: TextInputEditText
    private lateinit var ivProfile: ImageView
    private lateinit var ivCover: ImageView
    private lateinit var progressBar: ProgressBar

    private val repository = FirebaseRepository()
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
        setContentView(R.layout.activity_setup_profile)

        initViews()

        // Pre-fill some data if available from Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            etEmail.setText(it.email)
            etUsername.setText(it.email?.substringBefore("@"))
        }

        findViewById<Button>(R.id.btn_finish).setOnClickListener {
            submitForm()
        }

        findViewById<ImageView>(R.id.btn_pick_image).setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        findViewById<ImageView>(R.id.btn_pick_cover_image).setOnClickListener {
            pickCoverLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                etDob.setText("$dayOfMonth/${month + 1}/$year")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun initViews() {
        etUsername = findViewById(R.id.et_username)
        etFirstName = findViewById(R.id.et_first_name)
        etLastName = findViewById(R.id.et_last_name)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        etGender = findViewById(R.id.et_gender)
        etDob = findViewById(R.id.et_dob)
        etCountry = findViewById(R.id.et_country)
        etState = findViewById(R.id.et_state)
        etCity = findViewById(R.id.et_city)
        etLanguage = findViewById(R.id.et_language)
        ivProfile = findViewById(R.id.setup_profile_image)
        ivCover = findViewById(R.id.setup_cover_image)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun submitForm() {
        val username = etUsername.text.toString().trim()
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val gender = etGender.text.toString().trim()
        val dob = etDob.text.toString().trim()
        val country = etCountry.text.toString().trim()
        val state = etState.text.toString().trim()
        val city = etCity.text.toString().trim()
        val language = etLanguage.text.toString().trim()

        if (username.isEmpty() || firstName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            findViewById<Button>(R.id.btn_finish).isEnabled = false
            progressBar.visibility = android.view.View.VISIBLE
            
            val profileData = hashMapOf<String, Any>(
                "username" to username,
                "first_name" to firstName,
                "last_name" to lastName,
                "email" to email,
                "phone" to phone,
                "gender" to gender,
                "dob" to dob,
                "country" to country,
                "state" to state,
                "city" to city,
                "language" to language,
                "updated_at" to System.currentTimeMillis()
            )

            // Upload profile image if selected
            var uploadSuccess = true
            selectedImageUri?.let { uri ->
                val uploadResult = repository.uploadImageToCloudinary(uri, "profile_images")
                uploadResult.onSuccess { url ->
                    profileData["profile_image"] = url
                }.onFailure { e ->
                    uploadSuccess = false
                    Toast.makeText(this@CompleteProfileActivity, "Profile image upload failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.btn_finish).isEnabled = true
                    progressBar.visibility = android.view.View.GONE
                }
            }

            // Upload cover/banner image if selected
            if (uploadSuccess) {
                selectedCoverUri?.let { uri ->
                    val uploadResult = repository.uploadImageToCloudinary(uri, "cover_images")
                    uploadResult.onSuccess { url ->
                        profileData["cover_image"] = url
                    }.onFailure { e ->
                        uploadSuccess = false
                        Toast.makeText(this@CompleteProfileActivity, "Banner upload failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        findViewById<Button>(R.id.btn_finish).isEnabled = true
                        progressBar.visibility = android.view.View.GONE
                    }
                }
            }

            if (!uploadSuccess) return@launch

            val result = repository.updateProfile(uid, profileData)
            progressBar.visibility = android.view.View.GONE
            
            result.onSuccess {
                Toast.makeText(this@CompleteProfileActivity, "Profile completed!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@CompleteProfileActivity, SetPinActivity::class.java)
                intent.putExtra("USER_ID", uid)
                startActivity(intent)
                finish()
            }.onFailure { e ->
                Toast.makeText(this@CompleteProfileActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.btn_finish).isEnabled = true
            }
        }
    }
}
