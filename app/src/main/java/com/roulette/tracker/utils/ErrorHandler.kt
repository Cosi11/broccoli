package com.roulette.tracker.utils

import android.content.Context
import javax.inject.Inject

sealed class AppError : Exception() {
    data class CameraError(override val message: String) : AppError()
    data class TrackingError(override val message: String) : AppError()
    data class DatabaseError(override val message: String) : AppError()
    data class OpenCVError(override val message: String) : AppError()
}

class ErrorHandler @Inject constructor(
    private val context: Context
) {
    fun handleError(error: AppError) {
        val errorMessage = when (error) {
            is AppError.CameraError -> context.getString(R.string.error_camera_init, error.message)
            is AppError.TrackingError -> context.getString(R.string.status_error, error.message)
            is AppError.DatabaseError -> "Datenbankfehler: ${error.message}"
            is AppError.OpenCVError -> context.getString(R.string.error_opencv_init, error.message)
        }
        
        Log.e(TAG, errorMessage, error)
        showErrorToast(errorMessage)
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "ErrorHandler"
    }
} 