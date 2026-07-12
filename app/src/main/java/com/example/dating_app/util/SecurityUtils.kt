package com.example.dating_app.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_SIZE = 12
    private const val ITERATION_COUNT = 1000
    private const val KEY_LENGTH = 256
    
    // In a real E2EE system, keys would be generated per chat and exchanged via public key crypto.
    // For this implementation, we'll derive a consistent key for a conversation pair.
    fun generateChatKey(senderId: String, receiverId: String): SecretKeySpec {
        val pair = if (senderId < receiverId) senderId + receiverId else receiverId + senderId
        val salt = "HeyDateSecuritySalt".toByteArray()
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val spec = PBEKeySpec(pair.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encrypt(text: String, secretKey: SecretKeySpec): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val encryptedData = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String, secretKey: SecretKeySpec): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            val iv = combined.sliceArray(0 until IV_SIZE)
            val encryptedData = combined.sliceArray(IV_SIZE until combined.size)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            val decryptedData = cipher.doFinal(encryptedData)
            String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            "[Decryption Error]"
        }
    }

    fun isSpam(text: String): Boolean {
        val spamKeywords = listOf("win prize", "free money", "click here", "subscribe", "buy now", "bitcoin", "crypto")
        return spamKeywords.any { text.lowercase().contains(it) }
    }

    fun scanLinks(text: String): List<String> {
        val urlPattern = "(https?://[\\\\w-]+(\\\\.[\\\\w-]+)+(/[\\\\w- ./?%&=]*)?)".toRegex()
        return urlPattern.findAll(text).map { it.value }.toList()
    }
}
