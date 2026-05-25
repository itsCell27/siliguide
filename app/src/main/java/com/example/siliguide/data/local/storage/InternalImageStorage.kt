package com.example.siliguide.data.local.storage

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class InternalImageStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun saveImagePermanently(tempUriString: String, sessionId: String): String? {
        return try {
            val tempUri = tempUriString.toUri()
            val inputStream = context.contentResolver.openInputStream(tempUri) ?: return null

            // Create a directory inside internal storage
            val directory = File(context.filesDir, "analysis_images")
            if (!directory.exists()) directory.mkdirs()

            // Create the permanent file
            val permanentFile = File(directory, "analysis_$sessionId.jpg")
            val outputStream = FileOutputStream(permanentFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            permanentFile.absolutePath
        } catch (e: Exception) {
            Log.e("InternalImageStorage", "Failed to save image permanently", e)
            null
        }
    }
}