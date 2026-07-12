package com.example.dating_app

import com.datingapp.R

data class UserProfile(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val profileImage: Int,
    val coverImage: Int,
    val bio: String,
    val gender: String,
    val dob: String,
    val country: String,
    val state: String,
    val city: String,
    val language: String,
    val timezone: String,
    val isOnline: Boolean,
    val lastSeen: String,
    val emailVerified: Boolean,
    val phoneVerified: Boolean,
    val accountStatus: String,
    val likes: Int,
    val matches: Int,
    val photosCount: Int
)

object ProfileRepository {
    var userProfile: UserProfile = UserProfile(
        id = "1",
        username = "@oliviamartin",
        firstName = "Olivia",
        lastName = "Martin",
        email = "olivia.martin@email.com",
        phone = "+1 (212) 555-0148",
        profileImage = R.drawable.girl,
        coverImage = R.drawable.girl,
        bio = "Love traveling, good coffee and meaningful conversations. ✨",
        gender = "Female",
        dob = "May 16, 1998",
        country = "USA",
        state = "New York",
        city = "New York",
        language = "English",
        timezone = "America/New_York",
        isOnline = true,
        lastSeen = "2 mins ago",
        emailVerified = true,
        phoneVerified = true,
        accountStatus = "Active",
        likes = 128,
        matches = 96,
        photosCount = 32
    )
}
