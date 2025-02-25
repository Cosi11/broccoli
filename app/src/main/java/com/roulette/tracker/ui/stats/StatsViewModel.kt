package com.roulette.tracker.ui.stats

import androidx.lifecycle.ViewModel
import com.roulette.tracker.data.repository.SimulationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: SimulationRepository
) : ViewModel() {
    val allResults = repository.allResults
    val accuracyRate = repository.accuracyRate
} 