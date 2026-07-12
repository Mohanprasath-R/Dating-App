package com.example.dating_app.util

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.Cloudinary
import com.cloudinary.Transformation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * CloudinaryHelper provides a bridge between the Android SDK and your application.
 * Using the standard Android SDK (Java based) to avoid dependency conflicts with the new Kotlin SDK.
 */
object CloudinaryHelper {
    private var isInitialized = false
    
    // Configuration credentials
    private const val CLOUD_NAME = "kwsldsqn"
    private const val API_KEY = "573997728744597"
    private const val API_SECRET = "p9N8XmP_PZoRfRJLqfFuf0P4LtU"

    // Cloudinary instance for transformation/URL generation
    private val cloudinary = Cloudinary("cloudinary://$API_KEY:$API_SECRET@$CLOUD_NAME")

    fun init(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to CLOUD_NAME,
                "api_key" to API_KEY,
                "api_secret" to API_SECRET,
                "secure" to true
            )
            try {
                MediaManager.init(context, config)
                isInitialized = true
            } catch (e: Exception) {
                android.util.Log.e("CloudinaryHelper", "Init failed: ${e.message}")
            }
        }
    }

    data class UploadResult(val url: String, val publicId: String)

    /**
     * Uploads media using the Android SDK's MediaManager.
     * Returns the UploadResult on success.
     */
    suspend fun uploadMedia(uri: Uri, type: String, folder: String? = null): UploadResult? = suspendCancellableCoroutine { continuation ->
        // Use 'auto' to let Cloudinary detect the resource type (image, video, audio, etc.)
        // This prevents "Invalid image file" errors when uploading audio/video.
        val resourceType = "auto"

        android.util.Log.d("CloudinaryHelper", "Starting upload: $uri as type: $type (resourceType: $resourceType)")

        val request = MediaManager.get().upload(uri)
            .option("upload_preset", "ml_default")
            .option("resource_type", resourceType)
            .also { if (folder != null) it.option("folder", folder) }
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    android.util.Log.d("CloudinaryHelper", "Upload started: $requestId")
                }
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    android.util.Log.v("CloudinaryHelper", "Upload progress: $bytes/$totalBytes")
                }
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    val publicId = resultData["public_id"] as? String
                    android.util.Log.d("CloudinaryHelper", "Upload success: $url")
                    if (url != null && publicId != null) {
                        continuation.resume(UploadResult(url, publicId))
                    } else {
                        continuation.resume(null)
                    }
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    android.util.Log.e("CloudinaryHelper", "Upload error for request $requestId: ${error.description}")
                    continuation.resume(null)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    android.util.Log.w("CloudinaryHelper", "Upload rescheduled: ${error.description}")
                    continuation.resume(null)
                }
            })
        
        request.dispatch()
    }

    /**
     * Generates an optimized URL with auto format and quality.
     */
    fun getOptimizedUrl(publicId: String): String? {
        return cloudinary.url()
            .transformation(Transformation<Transformation<*>>().fetchFormat("auto").quality("auto"))
            .generate(publicId)
    }

    /**
     * Generates a circular thumbnail for profile pictures.
     */
    fun getCircularProfileUrl(publicId: String, size: Int = 200): String? {
        return cloudinary.url()
            .transformation(Transformation<Transformation<*>>()
                .width(size)
                .height(size)
                .crop("fill")
                .gravity("face")
                .radius("max")
                .fetchFormat("auto")
                .quality("auto"))
            .generate(publicId)
    }

    /**
     * Generates a thumbnail for a video.
     */
    fun getVideoThumbnailUrl(publicId: String, width: Int = 400): String? {
        return cloudinary.url()
            .resourceType("video")
            .transformation(Transformation<Transformation<*>>()
                .width(width)
                .crop("scale")
                .fetchFormat("auto")
                .quality("auto"))
            .generate(publicId)
    }
}
