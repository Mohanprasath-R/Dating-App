package com.example.dating_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.datingapp.R
import com.example.dating_app.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileDetailsActivity : AppCompatActivity() {
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_details)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        


        findViewById<ImageView>(R.id.btn_edit).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        
        lifecycleScope.launch {
            val result = repository.getUser(uid)
            result.onSuccess { user ->
                user?.let {
                    // Personal Information
                    setRowData(R.id.row_username, "Username", "@${it.username}", iconRes = android.R.drawable.ic_menu_myplaces)
                    setRowData(R.id.row_first_name, "First Name", it.first_name, iconRes = android.R.drawable.ic_menu_myplaces)
                    setRowData(R.id.row_last_name, "Last Name", it.last_name, iconRes = android.R.drawable.ic_menu_myplaces)
                    setRowData(R.id.row_email, "Email", it.email, isVerified = it.email_verified, iconRes = android.R.drawable.ic_dialog_email)
                    setRowData(R.id.row_phone, "Phone", it.phone, isVerified = it.phone_verified, iconRes = android.R.drawable.ic_menu_call)
                    setRowData(R.id.row_gender, "Gender", it.gender, iconRes = android.R.drawable.ic_menu_myplaces)
                    setRowData(R.id.row_dob, "Date of Birth", it.dob, iconRes = android.R.drawable.ic_menu_today)
                    
                    setRowData(R.id.row_profile_image, "Profile Image", "", iconRes = android.R.drawable.ic_menu_gallery, imageUrl = it.profile_image, showAction = true)
                    setRowData(R.id.row_cover_image, "Cover Image", "", iconRes = android.R.drawable.ic_menu_gallery, imageUrl = it.cover_image, showAction = true)

                    // Location & Preferences
                    setRowData(R.id.row_country, "Country", it.country, iconRes = android.R.drawable.ic_menu_mapmode)
                    setRowData(R.id.row_state, "State", it.state, iconRes = android.R.drawable.ic_menu_mapmode)
                    setRowData(R.id.row_city, "City", it.city, iconRes = android.R.drawable.ic_menu_mapmode)
                    setRowData(R.id.row_language, "Language", it.language, iconRes = android.R.drawable.ic_menu_manage)
                    setRowData(R.id.row_timezone, "Timezone", it.timezone, iconRes = android.R.drawable.ic_menu_recent_history)

                    // Account Status
                    setRowData(R.id.row_is_online, "Is Online", if (it.is_online) "Online" else "Offline", showDot = it.is_online, iconRes = android.R.drawable.ic_menu_info_details)
                    setRowData(R.id.row_last_seen, "Last Seen", formatTimestamp(it.last_seen), iconRes = android.R.drawable.ic_menu_recent_history)
                    setRowData(R.id.row_email_verified_status, "Email Verified", if (it.email_verified) "Verified" else "Not Verified", isVerified = it.email_verified, iconRes = android.R.drawable.checkbox_on_background)
                    setRowData(R.id.row_phone_verified_status, "Phone Verified", if (it.phone_verified) "Verified" else "Not Verified", isVerified = it.phone_verified, iconRes = android.R.drawable.checkbox_on_background)
                    setRowData(R.id.row_status, "Account Status", it.account_status, iconRes = android.R.drawable.ic_menu_info_details)
                }
            }
        }
    }

    private fun setRowData(rowId: Int, label: String, value: String, isVerified: Boolean = false, showDot: Boolean = false, iconRes: Int? = null, imageUrl: String? = null, showAction: Boolean = false) {
        val row = findViewById<View>(rowId) ?: return
        row.findViewById<TextView>(R.id.tv_label).text = label
        val tvValue = row.findViewById<TextView>(R.id.tv_value)
        tvValue.text = value
        
        iconRes?.let {
            row.findViewById<ImageView>(R.id.iv_icon).setImageResource(it)
        }

        row.findViewById<View>(R.id.iv_verified).visibility = if (isVerified) View.VISIBLE else View.GONE
        row.findViewById<View>(R.id.v_status_dot).visibility = if (showDot) View.VISIBLE else View.GONE

        val ivThumbnail = row.findViewById<ImageView>(R.id.iv_thumbnail)
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            ivThumbnail.visibility = View.VISIBLE
            ivThumbnail.load(imageUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.girl)
                error(R.drawable.girl)
            }
        } else {
            ivThumbnail.visibility = View.GONE
        }

        row.findViewById<View>(R.id.tv_action).visibility = if (showAction) View.VISIBLE else View.GONE
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} mins ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
