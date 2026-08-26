package com.example.dating_app.repository

import com.example.dating_app.model.User
import com.example.dating_app.model.Message
import com.example.dating_app.model.Call
import android.net.Uri
import com.example.dating_app.util.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.dating_app.model.LoginRecord
import com.example.dating_app.model.UserDevice
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.example.dating_app.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatListItem(
    val partner: User,
    val lastMessage: Message?,
    val unreadCount: Int,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersCollection = firestore.collection("users")
    private val messagesCollection = firestore.collection("messages")

    suspend fun registerUser(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logLogin(userId: String, device: UserDevice): Result<Boolean> {
        return try {
            // 1. Check if device is new
            val deviceDoc = usersCollection.document(userId).collection("devices").document(device.deviceId).get().await()
            val isNewDevice = !deviceDoc.exists()

            // 2. Save device info
            usersCollection.document(userId).collection("devices").document(device.deviceId)
                .set(device, SetOptions.merge()).await()

            // 3. Log history
            val recordRef = firestore.collection("login_history").document()
            val record = LoginRecord(
                id = recordRef.id,
                userId = userId,
                deviceName = device.deviceName,
                deviceId = device.deviceId,
                osVersion = android.os.Build.VERSION.RELEASE
            )
            recordRef.set(record).await()
            
            Result.success(isNewDevice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLoginHistory(userId: String): Result<List<LoginRecord>> {
        return try {
            val snapshot = firestore.collection("login_history")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
            Result.success(snapshot.toObjects(LoginRecord::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            val user = snapshot.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPin(userId: String, pin: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("pin", pin).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, profileData: Map<String, Any>): Result<Unit> {
        return try {
            usersCollection.document(userId).update(profileData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return updateProfile(userId, mapOf("fcmToken" to token))
    }

    /**
     * Checks if premium has expired and updates Firestore if necessary.
     * This ensures the boolean flag is synced with the timestamp.
     */
    suspend fun syncPremiumStatus(userId: String): Result<Boolean> {
        return try {
            val user = getUser(userId).getOrNull()
            if (user != null && user.is_premium && user.premium_expiry > 0) {
                if (System.currentTimeMillis() > user.premium_expiry) {
                    // Expired!
                    updateProfile(userId, mapOf("is_premium" to false))
                    Result.success(false)
                } else {
                    Result.success(true)
                }
            } else {
                Result.success(user?.is_premium ?: false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLocation(userId: String, lat: Double, lon: Double): Result<Unit> {
        return updateProfile(userId, mapOf(
            "latitude" to lat,
            "longitude" to lon,
            "updated_at" to System.currentTimeMillis()
        ))
    }

    suspend fun uploadImage(userId: String, imageUri: Uri, folderName: String): Result<String> {
        return uploadMedia(userId, imageUri, folderName, "image")
    }

    suspend fun uploadImageToCloudinary(imageUri: Uri, folderName: String): Result<String> {
        return try {
            val uploadResult = CloudinaryHelper.uploadMedia(imageUri, "image", folderName)
            if (uploadResult != null) {
                Result.success(uploadResult.url)
            } else {
                Result.failure(Exception("Cloudinary upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadMedia(userId: String, uri: Uri, folderName: String, type: String): Result<String> {
        if (userId.isEmpty()) return Result.failure(Exception("User ID is empty"))
        
        val extension = when (type) {
            "video" -> "mp4"
            "audio" -> "m4a"
            else -> "jpg"
        }
        val fileName = "${System.currentTimeMillis()}.$extension"
        val mimeType = when (type) {
            "video" -> "video/mp4"
            "audio" -> "audio/m4a"
            else -> "image/jpeg"
        }
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        return try {
            val ref = storage.reference.child("users").child(userId).child(folderName).child(fileName)
            android.util.Log.d("FirebaseRepository", "Attempting upload to bucket: ${ref.bucket}, path: ${ref.path}")
            
            try {
                ref.putFile(uri, metadata).await()
                val url = ref.downloadUrl.await().toString()
                Result.success(url)
            } catch (e: Exception) {
                // Handle the specific "Object does not exist" error which often indicates a bucket mismatch
                if (e.message?.contains("Object does not exist at location", ignoreCase = true) == true) {
                    android.util.Log.w("FirebaseRepository", "Primary bucket failed, trying fallback bucket...")
                    
                    val fallbacks = listOf(
                        "dating-application-45fb8.appspot.com",
                        "dating-application-45fb8.firebasestorage.app"
                    ).filter { it != ref.bucket }
                    
                    var lastError: Exception = e
                    for (bucket in fallbacks) {
                        try {
                            android.util.Log.d("FirebaseRepository", "Trying fallback bucket: $bucket")
                            val fallbackRef = FirebaseStorage.getInstance("gs://$bucket")
                                .reference.child("users").child(userId).child(folderName).child(fileName)
                            fallbackRef.putFile(uri, metadata).await()
                            val url = fallbackRef.downloadUrl.await().toString()
                            android.util.Log.d("FirebaseRepository", "Upload successful with fallback bucket: $bucket")
                            return Result.success(url)
                        } catch (fallbackEx: Exception) {
                            lastError = fallbackEx
                        }
                    }
                    throw lastError
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Upload failed for $type: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(excludeUserId: String? = null): Result<List<User>> {
        return try {
            val snapshot = if (excludeUserId != null) {
                // Note: Not ideal for very large datasets, but better than fetching all and filtering in memory if we use pagination
                // For now, let's keep it simple but avoid the in-memory filter if possible
                usersCollection.whereNotEqualTo("id", excludeUserId).get().await()
            } else {
                usersCollection.get().await()
            }
            Result.success(snapshot.toObjects(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeProfile(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            val likeData = hashMapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("liked_profiles")
                .document("${fromUserId}_${toUserId}")
                .set(likeData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLikedProfiles(userId: String): Result<List<User>> {
        return try {
            // Fetch blocked users first to filter
            val blockedSnapshot = firestore.collection("blocked_users")
                .whereEqualTo("blockerId", userId)
                .get()
                .await()
            val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()

            val snapshot = firestore.collection("liked_profiles")
                .whereEqualTo("fromUserId", userId)
                .get()
                .await()
            
            val likedUserIds = snapshot.documents.mapNotNull { it.getString("toUserId") }
                .filter { it !in blockedIds } // Filter out blocked users
            
            if (likedUserIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val likedUsers = mutableListOf<User>()
            for (batch in likedUserIds.chunked(30)) {
                val usersSnapshot = usersCollection.whereIn("id", batch).get().await()
                likedUsers.addAll(usersSnapshot.toObjects(User::class.java))
            }
            
            Result.success(likedUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLikedByUsers(userId: String): Result<List<User>> {
        return try {
            // Fetch blocked users first to filter
            val blockedSnapshot = firestore.collection("blocked_users")
                .whereEqualTo("blockerId", userId)
                .get()
                .await()
            val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()

            // Fetch disliked users to filter
            val dislikedSnapshot = firestore.collection("disliked_profiles")
                .whereEqualTo("fromUserId", userId)
                .get()
                .await()
            val dislikedIds = dislikedSnapshot.documents.mapNotNull { it.getString("toUserId") }.toSet()

            // Fetch profiles I liked to filter out mutual matches
            val myLikesSnapshot = firestore.collection("liked_profiles")
                .whereEqualTo("fromUserId", userId)
                .get()
                .await()
            val myLikedIds = myLikesSnapshot.documents.mapNotNull { it.getString("toUserId") }.toSet()

            val snapshot = firestore.collection("liked_profiles")
                .whereEqualTo("toUserId", userId)
                .get()
                .await()
            
            val likerIds = snapshot.documents.mapNotNull { it.getString("fromUserId") }
                .filter { it !in blockedIds && it !in dislikedIds && it !in myLikedIds }
            
            if (likerIds.isEmpty()) return Result.success(emptyList())

            val likers = mutableListOf<User>()
            for (batch in likerIds.chunked(30)) {
                val usersSnapshot = usersCollection.whereIn("id", batch).get().await()
                likers.addAll(usersSnapshot.toObjects(User::class.java))
            }
            
            Result.success(likers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatches(userId: String): Result<List<User>> {
        return try {
            // 0. Get blocked users
            val blockedSnapshot = firestore.collection("blocked_users")
                .whereEqualTo("blockerId", userId)
                .get()
                .await()
            val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()

            // 1. Get profiles I liked
            val myLikesSnapshot = firestore.collection("liked_profiles")
                .whereEqualTo("fromUserId", userId)
                .get()
                .await()
            val myLikedIds = myLikesSnapshot.documents.mapNotNull { it.getString("toUserId") }
                .filter { it !in blockedIds }.toSet()

            // 2. Get profiles who liked me
            val theyLikedMeSnapshot = firestore.collection("liked_profiles")
                .whereEqualTo("toUserId", userId)
                .get()
                .await()
            val theyLikedMeIds = theyLikedMeSnapshot.documents.mapNotNull { it.getString("fromUserId") }
                .filter { it !in blockedIds }.toSet()

            // 3. Find mutual likes
            val mutualIds = myLikedIds.intersect(theyLikedMeIds)
            if (mutualIds.isEmpty()) return Result.success(emptyList())

            val matches = mutableListOf<User>()
            for (batch in mutualIds.chunked(30)) {
                val usersSnapshot = usersCollection.whereIn("id", batch.toList()).get().await()
                matches.addAll(usersSnapshot.toObjects(User::class.java))
            }
            
            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikeProfile(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            firestore.collection("liked_profiles")
                .document("${fromUserId}_${toUserId}")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dislikeProfile(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            val dislikeData = hashMapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("disliked_profiles")
                .document("${fromUserId}_${toUserId}")
                .set(dislikeData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDislikedProfileIds(userId: String): Result<Set<String>> {
        return try {
            val snapshot = firestore.collection("disliked_profiles")
                .whereEqualTo("fromUserId", userId)
                .get()
                .await()
            val ids = snapshot.documents.mapNotNull { it.getString("toUserId") }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDislikedProfiles(userId: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("disliked_profiles")
                .whereEqualTo("fromUserId", userId)
                .get()
                .await()
            
            val dislikedIds = snapshot.documents.mapNotNull { it.getString("toUserId") }
            if (dislikedIds.isEmpty()) return Result.success(emptyList())

            val usersSnapshot = usersCollection.get().await()
            val allUsers = usersSnapshot.toObjects(User::class.java)
            val dislikedUsers = allUsers.filter { it.id in dislikedIds }
            
            Result.success(dislikedUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun undislikeProfile(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            firestore.collection("disliked_profiles")
                .document("${fromUserId}_${toUserId}")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkMatch(userId1: String, userId2: String): Result<Boolean> {
        return try {
            val doc = firestore.collection("liked_profiles")
                .document("${userId2}_${userId1}")
                .get()
                .await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            val docRef = messagesCollection.document()
            val messageWithId = message.copy(id = docRef.id)
            docRef.set(messageWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForMe(messageId: String, userId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId).update("deletedForMe", FieldValue.arrayUnion(userId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMessage(messageId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            messagesCollection.document(messageId).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReaction(messageId: String, userId: String, emoji: String): Result<Unit> {
        return try {
            // Using set with merge is safer if the 'reactions' field doesn't exist yet
            val reactionData = mapOf("reactions" to mapOf(userId to emoji))
            messagesCollection.document(messageId).set(reactionData, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeReaction(messageId: String, userId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId).update("reactions.$userId", FieldValue.delete()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessages(userId1: String, userId2: String): Flow<List<Message>> = callbackFlow {
        // Optimized query to fetch only messages between these two users
        val messagesQuery = messagesCollection.where(
            com.google.firebase.firestore.Filter.or(
                com.google.firebase.firestore.Filter.and(
                    com.google.firebase.firestore.Filter.equalTo("senderId", userId1),
                    com.google.firebase.firestore.Filter.equalTo("receiverId", userId2)
                ),
                com.google.firebase.firestore.Filter.and(
                    com.google.firebase.firestore.Filter.equalTo("senderId", userId2),
                    com.google.firebase.firestore.Filter.equalTo("receiverId", userId1)
                )
            )
        )

        val subscription = messagesQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseRepository", "Firestore Error: ${error.message}")
                return@addSnapshotListener
            }
            
            val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
            trySend(messages.sortedBy { it.timestamp })
        }
        awaitClose { subscription.remove() }
    }

    suspend fun getPendingRequests(userId: String): Result<List<User>> {
        return try {
            // 1. Get blocked users
            val blockedSnapshot = firestore.collection("blocked_users")
                .whereEqualTo("blockerId", userId)
                .get()
                .await()
            val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()

            // 2. Get profiles I already liked (matched or pending my side)
            val myLikesSnapshot = firestore.collection("liked_profiles")
                .whereEqualTo("fromUserId", userId)
                .get()
                .await()
            val myLikedIds = myLikesSnapshot.documents.mapNotNull { it.getString("toUserId") }.toSet()

            // 3. Get profiles who liked me
            val theyLikedMeSnapshot = firestore.collection("liked_profiles")
                .whereEqualTo("toUserId", userId)
                .get()
                .await()
            
            // Pending Requests = People who liked me, BUT I haven't liked back AND I haven't blocked
            val pendingIds = theyLikedMeSnapshot.documents.mapNotNull { it.getString("fromUserId") }
                .filter { it !in blockedIds && it !in myLikedIds }
            
            if (pendingIds.isEmpty()) return Result.success(emptyList())

            val usersList = mutableListOf<User>()
            for (batch in pendingIds.chunked(30)) {
                val usersSnapshot = usersCollection.whereIn("id", batch).get().await()
                usersList.addAll(usersSnapshot.toObjects(User::class.java))
            }
            
            Result.success(usersList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatList(currentUserId: String): Result<List<ChatListItem>> {
        return try {
            val blockedSnapshot = firestore.collection("blocked_users")
                .whereEqualTo("blockerId", currentUserId)
                .get()
                .await()
            val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()

            // Fetch from server to avoid stale cache
            val sentSnapshot = messagesCollection.whereEqualTo("senderId", currentUserId)
                .get().await()
            val receivedSnapshot = messagesCollection.whereEqualTo("receiverId", currentUserId)
                .get().await()
            
            val allMessages = (sentSnapshot.toObjects(Message::class.java) + 
                              receivedSnapshot.toObjects(Message::class.java))
                              .sortedByDescending { it.timestamp }
            
            val chatPartners = allMessages.map { 
                if (it.senderId == currentUserId) it.receiverId else it.senderId 
            }.distinct().filter { it !in blockedIds }
            
            val resultList = mutableListOf<ChatListItem>()
            for (partnerId in chatPartners) {
                val partnerResult = getUser(partnerId)
                partnerResult.onSuccess { partner ->
                    if (partner != null) {
                        val partnerMessages = allMessages.filter { 
                            (it.senderId == partnerId || it.receiverId == partnerId) &&
                            !it.deletedForMe.contains(currentUserId) &&
                            !it.deletedForEveryone
                        }
                        val lastMessage = partnerMessages.firstOrNull()
                        val unreadCount = partnerMessages.count { it.receiverId == currentUserId && !it.isRead }
                        resultList.add(ChatListItem(partner, lastMessage, unreadCount))
                    }
                }
            }
            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeChatList(currentUserId: String): Flow<List<ChatListItem>> = callbackFlow {
        // Use a single listener for messages where the user is a participant
        val messagesQuery = messagesCollection.where(
            com.google.firebase.firestore.Filter.or(
                com.google.firebase.firestore.Filter.equalTo("senderId", currentUserId),
                com.google.firebase.firestore.Filter.equalTo("receiverId", currentUserId)
            )
        )

        val subscription = messagesQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseRepository", "Chat List Error: ${error.message}")
                return@addSnapshotListener
            }

            val allMessages = snapshot?.toObjects(Message::class.java) ?: emptyList()
            if (allMessages.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Group messages by partner ID
            val partnersWithMessages = allMessages.groupBy { 
                if (it.senderId == currentUserId) it.receiverId else it.senderId 
            }

            launch {
                try {
                    // Fetch blocked users and partner profiles
                    val blockedSnapshot = firestore.collection("blocked_users")
                        .whereEqualTo("blockerId", currentUserId)
                        .get().await()
                    val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()

                    val resultList = mutableListOf<ChatListItem>()
                    
                    for ((partnerId, partnerMessages) in partnersWithMessages) {
                        if (partnerId in blockedIds || partnerId.isEmpty()) continue
                        
                        // Use a cached user fetch if possible
                        val partner = getUser(partnerId).getOrNull()
                        val currentUserData = getUser(currentUserId).getOrNull()

                        if (partner != null) {
                            val validMessages = partnerMessages.filter { 
                                !it.deletedForMe.contains(currentUserId) && !it.deletedForEveryone 
                            }
                            val sortedMessages = validMessages.sortedByDescending { it.timestamp }
                            val lastMessage = sortedMessages.firstOrNull()
                            
                            // Calculate unread count specifically for messages received BY the current user
                            val unreadCount = validMessages.count { 
                                it.receiverId == currentUserId && !it.isRead
                            }
                            
                            val isPinned = currentUserData?.pinned_chats?.contains(partnerId) == true
                            val isMuted = currentUserData?.muted_chats?.contains(partnerId) == true

                            resultList.add(ChatListItem(partner, lastMessage, unreadCount, isPinned, isMuted))
                        }
                    }
                    
                    // Sort pinned first, then by timestamp
                    val sortedList = resultList.sortedWith(
                        compareByDescending<ChatListItem> { it.isPinned }
                            .thenByDescending { it.lastMessage?.timestamp ?: 0L }
                    )
                    
                    trySend(sortedList)
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseRepository", "Error processing chat list: ${e.message}")
                }
            }
        }
        
        awaitClose { subscription.remove() }
    }

    suspend fun blockUser(blockerId: String, blockedId: String): Result<Unit> {
        return try {
            val data = hashMapOf(
                "blockerId" to blockerId,
                "blockedId" to blockedId,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("blocked_users")
                .document("${blockerId}_${blockedId}")
                .set(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockUser(blockerId: String, blockedId: String): Result<Unit> {
        return try {
            firestore.collection("blocked_users")
                .document("${blockerId}_${blockedId}")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBlockedUsers(userId: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("blocked_users")
                .whereEqualTo("blockerId", userId)
                .get()
                .await()
            
            val blockedIds = snapshot.documents.mapNotNull { it.getString("blockedId") }
            if (blockedIds.isEmpty()) return Result.success(emptyList())

            val usersSnapshot = usersCollection.get().await()
            val allUsers = usersSnapshot.toObjects(User::class.java)
            val blockedUsers = allUsers.filter { it.id in blockedIds }
            
            Result.success(blockedUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportUser(reporterId: String, reportedUserId: String, reason: String): Result<Unit> {
        return try {
            val reportData = hashMapOf(
                "reporter_id" to reporterId,
                "reported_user" to reportedUserId,
                "reason" to reason,
                "status" to "pending",
                "created_at" to System.currentTimeMillis()
            )
            firestore.collection("reported_users")
                .document()
                .set(reportData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun initiateCall(call: Call): Result<String> {
        return try {
            val docRef = firestore.collection("calls").document()
            val callWithId = call.copy(id = docRef.id)
            docRef.set(callWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCallStatus(callId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("calls").document(callId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendOtp(email: String): Result<String> {
        return try {
            // Check if user exists
            val userQuery = usersCollection.whereEqualTo("email", email).get().await()
            if (userQuery.isEmpty) {
                return Result.failure(Exception("User not found with this email"))
            }

            val otp = (100000..999999).random().toString()
            val otpData = hashMapOf(
                "email" to email,
                "otp" to otp,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("password_resets").document(email).set(otpData).await()
            
            // In a real app, you'd trigger a cloud function here to send the email.
            // For now, we'll return the OTP for demonstration (normally you wouldn't).
            Result.success(otp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(email: String, otp: String): Result<Unit> {
        return try {
            val doc = firestore.collection("password_resets").document(email).get().await()
            if (doc.exists() && doc.getString("otp") == otp) {
                val timestamp = doc.getLong("timestamp") ?: 0L
                if (System.currentTimeMillis() - timestamp < 10 * 60 * 1000) { // 10 mins expiry
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("OTP expired"))
                }
            } else {
                Result.failure(Exception("Invalid OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        return try {
            val userQuery = usersCollection.whereEqualTo("email", email).get().await()
            if (userQuery.isEmpty) {
                return Result.failure(Exception("User not found"))
            }
            val userId = userQuery.documents[0].id
            usersCollection.document(userId).update("password", newPassword).await()
            
            // Also delete the used OTP
            firestore.collection("password_resets").document(email).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeIncomingCalls(receiverId: String): Flow<List<Call>> = callbackFlow {
        val subscription = firestore.collection("calls")
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val calls = snapshot?.toObjects(Call::class.java) ?: emptyList()
                trySend(calls)
            }
        awaitClose { subscription.remove() }
    }

    fun observeCall(callId: String): Flow<Call?> = callbackFlow {
        val subscription = firestore.collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObject(Call::class.java))
            }
        awaitClose { subscription.remove() }
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val subscription = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun markMessagesAsRead(currentUserId: String, chatPartnerId: String): Result<Unit> {
        return try {
            // Remove Source.SERVER to ensure we catch all messages regardless of sync status
            val unreadMessages = messagesCollection
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("senderId", chatPartnerId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (unreadMessages.isEmpty) {
                android.util.Log.d("FirebaseRepository", "No unread messages to mark for $chatPartnerId")
                return Result.success(Unit)
            }

            val batch = firestore.batch()
            for (doc in unreadMessages.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            android.util.Log.d("FirebaseRepository", "Successfully marked ${unreadMessages.size()} messages as read from $chatPartnerId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error marking messages as read: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsReadByIds(messageIds: List<String>): Result<Unit> {
        if (messageIds.isEmpty()) return Result.success(Unit)
        return try {
            val batch = firestore.batch()
            for (id in messageIds) {
                batch.update(messagesCollection.document(id), "isRead", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUnreadMessageCount(userId: String): Flow<Int> = callbackFlow {
        val subscription = messagesCollection
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val unreadMessages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                if (unreadMessages.isEmpty()) {
                    trySend(0)
                    return@addSnapshotListener
                }

                firestore.collection("blocked_users")
                    .whereEqualTo("blockerId", userId)
                    .get()
                    .addOnSuccessListener { blockedSnapshot ->
                        val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()
                        val filteredCount = unreadMessages.count { it.senderId !in blockedIds }
                        trySend(filteredCount)
                    }
                    .addOnFailureListener {
                        trySend(unreadMessages.size)
                    }
            }
        awaitClose { subscription.remove() }
    }

    fun observeLastReceivedMessage(userId: String): Flow<Message?> = callbackFlow {
        val subscription = messagesCollection
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(Message::class.java)?.firstOrNull())
            }
        awaitClose { subscription.remove() }
    }

    fun observeLastLike(userId: String): Flow<Map<String, Any>?> = callbackFlow {
        val subscription = firestore.collection("liked_profiles")
            .whereEqualTo("toUserId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.documents?.firstOrNull()?.data)
            }
        awaitClose { subscription.remove() }
    }

    fun observeLikedByCount(userId: String): Flow<Int> = callbackFlow {
        val subscription = firestore.collection("liked_profiles")
            .whereEqualTo("toUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val docs = snapshot?.documents ?: emptyList()
                if (docs.isEmpty()) {
                    trySend(0)
                    return@addSnapshotListener
                }

                firestore.collection("blocked_users")
                    .whereEqualTo("blockerId", userId)
                    .get()
                    .addOnSuccessListener { blockedSnapshot ->
                        val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()
                        val filteredCount = docs.count { it.getString("fromUserId") !in blockedIds }
                        trySend(filteredCount)
                    }
                    .addOnFailureListener {
                        trySend(docs.size)
                    }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun deleteAccount(userId: String): Result<Unit> {
        return try {
            // Delete user document
            usersCollection.document(userId).delete().await()
            // Delete from auth
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePinChat(userId: String, partnerId: String, pin: Boolean): Result<Unit> {
        return try {
            val update = if (pin) FieldValue.arrayUnion(partnerId) else FieldValue.arrayRemove(partnerId)
            usersCollection.document(userId).update("pinned_chats", update).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleMuteChat(userId: String, partnerId: String, mute: Boolean): Result<Unit> {
        return try {
            val update = if (mute) FieldValue.arrayUnion(partnerId) else FieldValue.arrayRemove(partnerId)
            usersCollection.document(userId).update("muted_chats", update).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(userId: String, partnerId: String): Result<Unit> {
        return try {
            val snapshot = messagesCollection.where(
                com.google.firebase.firestore.Filter.or(
                    com.google.firebase.firestore.Filter.and(
                        com.google.firebase.firestore.Filter.equalTo("senderId", userId),
                        com.google.firebase.firestore.Filter.equalTo("receiverId", partnerId)
                    ),
                    com.google.firebase.firestore.Filter.and(
                        com.google.firebase.firestore.Filter.equalTo("senderId", partnerId),
                        com.google.firebase.firestore.Filter.equalTo("receiverId", userId)
                    )
                )
            ).get().await()

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestSubscription(userId: String, planName: String, price: String): Result<Unit> {
        return try {
            val request = hashMapOf(
                "userId" to userId,
                "planName" to planName,
                "price" to price,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("subscription_requests").document(userId).set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifySubscriptionOTP(userId: String, otp: String): Result<Unit> {
        return try {
            val otpDoc = firestore.collection("subscription_otps").document(otp).get().await()
            if (otpDoc.exists()) {
                val otpUserId = otpDoc.getString("userId")
                val isUsed = otpDoc.getBoolean("isUsed") ?: false
                val expiryTimestamp = otpDoc.getLong("expiryTimestamp") ?: 0L
                val durationDays = otpDoc.getLong("durationDays") ?: 30L

                if (otpUserId == userId && !isUsed && System.currentTimeMillis() < expiryTimestamp) {
                    // Valid OTP
                    firestore.runTransaction { transaction ->
                        // 1. Mark OTP as used
                        transaction.update(otpDoc.reference, "isUsed", true)
                        
                        // 2. Update user's premium status
                        val userRef = usersCollection.document(userId)
                        transaction.update(userRef, "is_premium", true)
                        transaction.update(userRef, "premium_expiry", System.currentTimeMillis() + (durationDays * 24 * 60 * 60 * 1000))
                        
                        // 3. Delete the request
                        transaction.delete(firestore.collection("subscription_requests").document(userId))
                    }.await()
                    
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Invalid, used, or expired OTP"))
                }
            } else {
                Result.failure(Exception("OTP not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Optimized Discovery with Pagination & Filtering ---

    suspend fun getDiscoveryProfiles(
        currentUserId: String,
        limit: Long = 20,
        lastVisible: com.google.firebase.firestore.DocumentSnapshot? = null,
        genderFilter: String? = null,
        preferredCity: String? = null,
        minAge: Int = 18,
        maxAge: Int = 100
    ): Result<Pair<List<User>, com.google.firebase.firestore.DocumentSnapshot?>> {
        return try {
            // 1. Get IDs to exclude (already liked, disliked, or blocked)
            val likedSnapshot = firestore.collection("liked_profiles").whereEqualTo("fromUserId", currentUserId).get().await()
            val dislikedSnapshot = firestore.collection("disliked_profiles").whereEqualTo("fromUserId", currentUserId).get().await()
            val blockedSnapshot = firestore.collection("blocked_users").whereEqualTo("blockerId", currentUserId).get().await()
            
            val excludeIds = (likedSnapshot.documents.mapNotNull { it.getString("toUserId") } +
                              dislikedSnapshot.documents.mapNotNull { it.getString("toUserId") } +
                              blockedSnapshot.documents.mapNotNull { it.getString("blockedId") } +
                              currentUserId).toSet()

            // 2. Build Query
            var query: Query = usersCollection
            
            if (genderFilter != null && genderFilter != "All") {
                query = query.whereEqualTo("gender", genderFilter)
            }
            
            if (preferredCity != null && preferredCity.isNotEmpty()) {
                query = query.whereEqualTo("city_lowercase", preferredCity.lowercase().trim())
            }

            query = query.limit(limit)
            
            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            
            // 3. Filter out excluded IDs and age range locally
            val profiles = snapshot.toObjects(User::class.java).filter { user ->
                val age = DateUtils.getAgeFromDob(user.dob)
                user.id !in excludeIds && age in minAge..maxAge
            }
            val newLastVisible = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null
            
            Result.success(Pair(profiles, newLastVisible))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
