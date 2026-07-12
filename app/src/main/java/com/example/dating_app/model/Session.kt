package com.example.dating_app.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class LoginRecord(
    val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val deviceId: String = "",
    val osVersion: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val ipAddress: String = "unknown"
)

@IgnoreExtraProperties
data class UserDevice(
    val deviceId: String = "",
    val deviceName: String = "",
    val lastLogin: Long = System.currentTimeMillis(),
    val isAuthorized: Boolean = true
)
