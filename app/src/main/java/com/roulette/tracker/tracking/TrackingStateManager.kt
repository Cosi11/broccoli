package com.roulette.tracker.tracking

import com.roulette.tracker.data.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingStateManager @Inject constructor() {
    private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.INITIALIZING)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _predictionResults = MutableStateFlow<List<PredictionResult>>(emptyList())
    val predictionResults: StateFlow<List<PredictionResult>> = _predictionResults.asStateFlow()

    fun updateState(newState: TrackingState, message: String? = null) {
        _trackingState.value = newState.apply { 
            this.message = message 
        }
    }

    fun addPredictionResult(result: PredictionResult) {
        _predictionResults.update { current ->
            current.take(9) + result
        }
    }

    fun reset() {
        _trackingState.value = TrackingState.INITIALIZING
        _predictionResults.value = emptyList()
    }

    fun reportError(message: String) {
        _trackingState.value = TrackingState.Error(message)
    }

    fun startTracking() {
        _trackingState.value = TrackingState.Running
    }

    fun ballLost() {
        _trackingState.value = TrackingState.BallNotFound
    }

    fun wheelLost() {
        _trackingState.value = TrackingState.WheelNotFound
    }

    fun startPrediction(timeRemaining: Long) {
        _trackingState.value = TrackingState.Predicting(timeRemaining)
    }

    fun predictionComplete(result: com.roulette.tracker.data.PredictionResult) {
        _trackingState.value = TrackingState.Success(result)
    }
} 