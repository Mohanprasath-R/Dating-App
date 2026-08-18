package com.example.dating_app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Compresses an image from a Uri and returns a new Uri to the compressed file.
     * Reduces quality to 70% and resizes if larger than 1200px.
     */
    fun compressImage(context: Context, uri: Uri): Uri {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        
        // Resize logic (keep aspect ratio, max 1200px)
        val maxSize = 1200
        val width = originalBitmap.width
        val height = originalBitmap.height
        
        val (scaledWidth, scaledHeight) = if (width > height) {
            if (width > maxSize) {
                val ratio = maxSize.toFloat() / width
                (maxSize to (height * ratio).toInt())
            } else (width to height)
        } else {
            if (height > maxSize) {
                val ratio = maxSize.toFloat() / height
                ((width * ratio).toInt() to maxSize)
            } else (width to height)
        }

        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
        
        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        
        // Save to temp file
        val tempFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
        val fileOutputStream = FileOutputStream(tempFile)
        fileOutputStream.write(outputStream.toByteArray())
        fileOutputStream.flush()
        fileOutputStream.close()
        
        return Uri.fromFile(tempFile)
    }
}
