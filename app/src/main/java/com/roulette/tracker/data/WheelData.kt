package com.roulette.tracker.data

data class WheelData(
    val currentAngle: Double,
    val rotationSpeed: Double,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean = confidence > 0.0 && rotationSpeed >= 0.0
} 