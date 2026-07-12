package com.example.dating_app.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val id: String = "",
    val username: String = "",
    val first_name: String = "",
    val last_name: String = "",
    val email: String = "",
    val phone: String = "",
    val profile_image: String = "",
    val cover_image: String = "",
    val bio: String = "",
    val gender: String = "",
    val dob: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val language: String = "",
    val timezone: String = "",
    val pin: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_enabled: Boolean = true,
    val is_online: Boolean = false,
    val last_seen: Long = 0L,
    val email_verified: Boolean = false,
    val phone_verified: Boolean = false,
    val account_status: String = "active",
    val likes: Int = 0,
    val matches: Int = 0,
    val photos_count: Int = 0,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)