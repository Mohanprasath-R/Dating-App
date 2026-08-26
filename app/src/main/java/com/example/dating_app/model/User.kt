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
    val city_lowercase: String = "",
    val language: String = "",
    val timezone: String = "",
    val pin: String = "",
    val biometric_enabled: Boolean = false,
    val private_profile: Boolean = false,
    val show_active_status: Boolean = true,
    val read_receipts: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_enabled: Boolean = true,
    val is_online: Boolean = false,
    val last_seen: Long = 0L,
    val email_verified: Boolean = false,
    val phone_verified: Boolean = false,
    val account_status: String = "active",
    val occupation: String = "",
    val looking_for: String = "",
    val interests: List<String> = emptyList(),
    val is_premium: Boolean = false,
    val premium_expiry: Long = 0L,
    val likes: Int = 0,
    val matches: Int = 0,
    val photos_count: Int = 0,
    val push_enabled: Boolean = true,
    val email_enabled: Boolean = false,
    val matches_notif: Boolean = true,
    val messages_notif: Boolean = true,
    val pinned_chats: List<String> = emptyList(),
    val muted_chats: List<String> = emptyList(),
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
) {
    fun isPremiumActive(): Boolean {
        return is_premium && (premium_expiry == 0L || System.currentTimeMillis() < premium_expiry)
    }
}
