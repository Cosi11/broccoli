package com.roulette.tracker.camera

data class CameraError(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val exception: Exception? = null
) 