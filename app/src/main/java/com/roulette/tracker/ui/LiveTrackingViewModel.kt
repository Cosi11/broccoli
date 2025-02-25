package com.roulette.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.roulette.tracker.data.*
import com.roulette.tracker.data.entities.SimulationResult
import com.roulette.tracker.data.repository.SimulationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveTrackingViewModel @Inject constructor(
    private val repository: SimulationRepository
) : ViewModel() {
    
    val allResults = repository.allResults.asLiveData()
    val accuracyRate = repository.accuracyRate.asLiveData()

    fun saveResult(result: AnalysisResult) {
        viewModelScope.launch {
            result.prediction?.let { prediction ->
                val simulationResult = SimulationResult(
                    timestamp = System.currentTimeMillis(),
                    predictedNumber = prediction.predictedNumber,
                    actualNumber = null  // Wird später über updateActualNumber gesetzt
                )
                repository.insertResult(simulationResult)
            }
        }
    }

    fun updateActualNumber(resultId: Long, actualNumber: Int) {
        viewModelScope.launch {
            repository.updateActualNumber(resultId, actualNumber)
        }
    }
    
    fun clearAllResults() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
} 