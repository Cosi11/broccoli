package com.roulette.tracker.utils

import android.content.Context
import android.util.Log
import com.roulette.tracker.opencv.OpenCVInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenCVManager(private val context: Context) {
    private val initializer = OpenCVInitializer()
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            initializer.initializeOpenCV(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OpenCV: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "OpenCVManager"
    }
} 