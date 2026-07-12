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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
            val snapshot = usersCollection.get().await()
            val users = snapshot.toObjects(User::class.java)
            val filteredUsers = if (excludeUserId != null) {
                users.filter { it.id != excludeUserId }
            } else {
                users
            }
            Result.success(filteredUsers)
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

            val usersSnapshot = usersCollection.get().await()
            val allUsers = usersSnapshot.toObjects(User::class.java)
            val likedUsers = allUsers.filter { it.id in likedUserIds }
            
            Result.success(likedUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLikedByUsers(userId: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("liked_profiles")
                .whereEqualTo("toUserId", userId)
                .get()
                .await()
            
            val likerIds = snapshot.documents.mapNotNull { it.getString("fromUserId") }
            if (likerIds.isEmpty()) return Result.success(emptyList())

            val usersSnapshot = usersCollection.get().await()
            val allUsers = usersSnapshot.toObjects(User::class.java)
            val likers = allUsers.filter { it.id in likerIds }
            
            Result.success(likers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatches(userId: String): Result<List<User>> {
        return try {
            // 1. Get profiles I liked
            val myLikesSnapshot = firestore.collection("liked_profiles")
                .whereEqualTo("fromUserId", userId)
                .get()
                .await()
            val myLikedIds = myLikesSnapshot.documents.mapNotNull { it.getString("toUserId") }.toSet()

            // 2. Get profiles who liked me
            val theyLikedMeSnapshot = firestore.collection("liked_profiles")
                .whereEqualTo("toUserId", userId)
                .get()
                .await()
            val theyLikedMeIds = theyLikedMeSnapshot.documents.mapNotNull { it.getString("fromUserId") }.toSet()

            // 3. Find mutual likes
            val mutualIds = myLikedIds.intersect(theyLikedMeIds)
            if (mutualIds.isEmpty()) return Result.success(emptyList())

            val usersSnapshot = usersCollection.get().await()
            val allUsers = usersSnapshot.toObjects(User::class.java)
            val matches = allUsers.filter { it.id in mutualIds }
            
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
            messagesCollection.document(messageId).update("deletedForEveryone", true).await()
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

    fun getMessages(userId1: String, userId2: String): Flow<List<Message>> = callbackFlow {
        // Use a simpler query to avoid complex index requirements initially
        // We fetch messages where the sender is one of the two participants
        val subscription = messagesCollection
            .whereIn("senderId", listOf(userId1, userId2))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log error instead of closing with exception to prevent crash
                    android.util.Log.e("FirebaseRepository", "Firestore Error: ${error.message}")
                    return@addSnapshotListener
                }
                
                val allMessages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                // Filter specifically for the conversation between these two and sort in memory
                val filteredMessages = allMessages.filter { 
                    (it.senderId == userId1 && it.receiverId == userId2) ||
                    (it.senderId == userId2 && it.receiverId == userId1)
                }.sortedBy { it.timestamp }

                trySend(filteredMessages)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getChatList(currentUserId: String): Result<List<Pair<User, Message?>>> {
        return try {
            // Fetch blocked users first to filter the chat list
            val blockedSnapshot = firestore.collection("blocked_users")
                .whereEqualTo("blockerId", currentUserId)
                .get()
                .await()
            val blockedIds = blockedSnapshot.documents.mapNotNull { it.getString("blockedId") }.toSet()

            val sentSnapshot = messagesCollection.whereEqualTo("senderId", currentUserId).get().await()
            val receivedSnapshot = messagesCollection.whereEqualTo("receiverId", currentUserId).get().await()
            
            val allMessages = (sentSnapshot.toObjects(Message::class.java) + 
                              receivedSnapshot.toObjects(Message::class.java))
                              .sortedByDescending { it.timestamp }
            
            val chatPartners = allMessages.map { 
                if (it.senderId == currentUserId) it.receiverId else it.senderId 
            }.distinct().filter { it !in blockedIds } // Filter out blocked users
            
            val resultList = mutableListOf<Pair<User, Message?>>()
            for (partnerId in chatPartners) {
                val partnerResult = getUser(partnerId)
                partnerResult.onSuccess { partner ->
                    if (partner != null) {
                        val lastMessage = allMessages.firstOrNull { 
                            it.senderId == partnerId || it.receiverId == partnerId 
                        }
                        resultList.add(partner to lastMessage)
                    }
                }
            }
            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
}
