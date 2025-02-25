package com.roulette.tracker.data

data class BallData(
    val position: Float,
    val velocity: Float,
    val timestamp: Long
)

data class WheelData(
    val position: Float,
    val velocity: Float,
    val timestamp: Long
)

data class PredictionResult(
    val predictedNumber: Int,
    val confidence: Float,
    val timeToLanding: Float
)

data class AnalysisResult(
    val ballData: BallData?,
    val wheelData: WheelData?,
    val prediction: PredictionResult?
)

data class SimulationData(
    val predictedNumber: Int,
    val accuracy: Float
)

data class TrackingPosition(
    val currentPosition: Float,
    val rotationSpeed: Float,
    val centerPoint: Point? = null,
    val currentAngle: Float = 0f,
    val visibleNumbers: List<Int> = emptyList(),
    val predictedLandingTime: Long = 0L
) 