package com.roulette.tracker.data

sealed class TrackingState {
    object Initializing : TrackingState()
    object Running : TrackingState()
    object BallNotFound : TrackingState()
    object WheelNotFound : TrackingState()
    object Idle : TrackingState()
    object TrackingBall : TrackingState()
    object TrackingWheel : TrackingState()
    data class Predicting(val timeRemaining: Long) : TrackingState()
    data class Success(val result: PredictionResult) : TrackingState()
    data class Error(val message: String) : TrackingState()
    object ResultReady : TrackingState()

    var message: String? = null
} 