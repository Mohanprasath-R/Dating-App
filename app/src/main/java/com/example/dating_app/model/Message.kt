package com.example.dating_app.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

object MessageType {
    const val TEXT = "TEXT"
    const val AUDIO = "AUDIO"
    const val VIDEO = "VIDEO"
    const val IMAGE = "IMAGE"
}

@IgnoreExtraProperties
data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val messageType: String = MessageType.TEXT,
    val mediaUrl: String? = null,
    val mediaPublicId: String? = null,
    val duration: String? = null,
    val encrypted: Boolean = false,
    val selfDestructAt: Long? = null,
    val deletedForEveryone: Boolean = false,
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val replyToId: String? = null,
    val edited: Boolean = false
)
