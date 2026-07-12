package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class UserProfileActivity : AppCompatActivity() {

    private lateinit var ivProfileImage: ImageView
    private lateinit var ivCoverImage: ImageView
    private lateinit var tvFullName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvBioShort: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var ivVerifiedBadge: ImageView
    private lateinit var btnEditProfile: Button
    
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        initViews()
        loadData()

        findViewById<View>(R.id.btn_menu).setOnClickListener { 
            // Handle menu click
        }
        
        findViewById<View>(R.id.btn_notifications).setOnClickListener {
            // Handle notifications click
        }

        findViewById<View>(R.id.btn_edit_image).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }

    private fun initViews() {
        ivProfileImage = findViewById(R.id.profile_image)
        ivCoverImage = findViewById(R.id.cover_image)
        tvFullName = findViewById(R.id.tv_full_name)
        tvUsername = findViewById(R.id.tv_username)
        tvBioShort = findViewById(R.id.tv_bio_short)
        tvOnlineStatus = findViewById(R.id.tv_online_status)
        ivVerifiedBadge = findViewById(R.id.iv_verified_badge)
        btnEditProfile = findViewById(R.id.btn_edit_profile)
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        
        lifecycleScope.launch {
            val result = repository.getUser(uid)
            result.onSuccess { user ->
                if (user != null) {
                    // Header Info
                    tvFullName.text = "${user.first_name} ${user.last_name}"
                    tvUsername.text = "@${user.username}"
                    tvOnlineStatus.text = if (user.is_online) "Online" else "Offline"
                    
                    // Verified Badge (Use email_verified as a proxy for verification)
                    ivVerifiedBadge.visibility = if (user.email_verified) View.VISIBLE else View.GONE
                    
                    // Bio
                    tvBioShort.text = if (user.bio.isNotEmpty()) user.bio else "Love traveling, good coffee and meaningful conversations. ✨"
                    
                    // Images
                    ivProfileImage.load(if (user.profile_image.isNotEmpty()) user.profile_image else R.drawable.girl) {
                        crossfade(true)
                        placeholder(R.drawable.girl)
                        transformations(CircleCropTransformation())
                    }

                    ivCoverImage.load(if (user.cover_image.isNotEmpty()) user.cover_image else R.drawable.girl) {
                        crossfade(true)
                        placeholder(R.drawable.girl)
                    }

                    // Tags Section
                    updateTag(R.id.tag_age, calculateAge(user.dob), "Age")
                    updateTag(R.id.tag_gender, if(user.gender.isNotEmpty()) user.gender else "Female", "Gender")
                    updateTag(R.id.tag_location, if(user.city.isNotEmpty()) user.city else "New York", if(user.country.isNotEmpty()) user.country else "USA")

                    // Stats Section
                    updateStat(R.id.stat_likes, user.likes.toString(), "Likes")
                    updateStat(R.id.stat_matches, user.matches.toString(), "Matches")
                    updateStat(R.id.stat_photos, user.photos_count.toString(), "Photos")
                }
            }
        }
    }

    private fun updateTag(containerId: Int, value: String, label: String) {
        val container = findViewById<View>(containerId) ?: return
        container.findViewById<TextView>(R.id.tv_tag_value).text = value
        container.findViewById<TextView>(R.id.tv_tag_label).text = label
    }

    private fun updateStat(containerId: Int, count: String, label: String) {
        val container = findViewById<View>(containerId) ?: return
        container.findViewById<TextView>(R.id.tv_stat_count).text = count
        container.findViewById<TextView>(R.id.tv_stat_label).text = label
    }

    private fun calculateAge(dob: String): String {
        return try {
            val parts = dob.split("/")
            if (parts.size == 3) {
                val year = parts[2].toInt()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                (currentYear - year).toString()
            } else "26"
        } catch (e: Exception) { "26" }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}
