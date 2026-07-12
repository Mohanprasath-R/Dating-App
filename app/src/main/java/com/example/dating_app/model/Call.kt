package com.example.dating_app.model

data class Call(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerImage: String = "",
    val receiverId: String = "",
    val roomId: String = "",
    val type: String = "video", // "audio" or "video"
    val status: String = "ringing", // "ringing", "accepted", "rejected", "ended"
    val timestamp: Long = System.currentTimeMillis()
)
