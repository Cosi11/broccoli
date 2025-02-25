package com.roulette.tracker.camera

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class CameraErrorHandler @Inject constructor() {
    private val _errors = MutableStateFlow<List<CameraError>>(emptyList())
    val errors: StateFlow<List<CameraError>> = _errors
    
    fun handleError(error: CameraError) {
        _errors.value = _errors.value + error
    }
    
    fun clearErrors() {
        _errors.value = emptyList()
    }
} 