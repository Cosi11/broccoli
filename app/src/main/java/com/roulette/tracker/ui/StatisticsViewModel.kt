package com.roulette.tracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roulette.tracker.data.repository.SimulationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

data class StatisticsData(
    val accuracyData: List<Float> = emptyList(),
    val predictionData: List<Pair<Int, Int>> = emptyList(),
    val last100Stats: StatsSummary = StatsSummary(),
    val last1000Stats: StatsSummary = StatsSummary(),
    val extendedStats: ExtendedStats = ExtendedStats()
)

data class StatsSummary(
    val totalPredictions: Int = 0,
    val correctPredictions: Int = 0,
    val accuracy: Float = 0f,
    val averageError: Float = 0f
)

data class ExtendedStats(
    val mostCommonNumber: Int = 0,
    val leastCommonNumber: Int = 0,
    val maxConsecutiveCorrect: Int = 0,
    val totalPredictions: Int = 0
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: SimulationRepository
) : ViewModel() {

    private val _statisticsData = MutableLiveData<StatisticsData>()
    val statisticsData: LiveData<StatisticsData> = _statisticsData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _timeRange = MutableLiveData(TimeRange.ALL)
    val timeRange: LiveData<TimeRange> = _timeRange

    enum class TimeRange {
        LAST_24H, LAST_WEEK, LAST_MONTH, ALL
    }

    init {
        loadStatistics()
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val allSimulations = repository.getAllSimulations()
                if (allSimulations.isEmpty()) {
                    _error.value = "Keine Daten verfügbar"
                    return@launch
                }

                val filteredSimulations = filterSimulationsByTimeRange(allSimulations)
                
                val accuracyList = filteredSimulations.map { it.accuracy.toFloat() }
                val predictionsList = filteredSimulations.map { Pair(it.predictedNumber, it.actualNumber) }

                val stats = calculateExtendedStats(filteredSimulations)
                val last100Stats = calculateStats(filteredSimulations.takeLast(100))
                val last1000Stats = calculateStats(filteredSimulations.takeLast(1000))

                _statisticsData.value = StatisticsData(
                    accuracyData = accuracyList,
                    predictionData = predictionsList,
                    last100Stats = last100Stats,
                    last1000Stats = last1000Stats,
                    extendedStats = stats
                )
            } catch (e: Exception) {
                _error.value = "Fehler beim Laden der Statistiken: ${e.localizedMessage}"
                Timber.e(e, "Fehler beim Laden der Statistiken")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun filterSimulationsByTimeRange(simulations: List<SimulationEntity>): List<SimulationEntity> {
        val currentTime = System.currentTimeMillis()
        return when (_timeRange.value) {
            TimeRange.LAST_24H -> simulations.filter { 
                (currentTime - it.timestamp) < TimeUnit.DAYS.toMillis(1) 
            }
            TimeRange.LAST_WEEK -> simulations.filter { 
                (currentTime - it.timestamp) < TimeUnit.DAYS.toMillis(7) 
            }
            TimeRange.LAST_MONTH -> simulations.filter { 
                (currentTime - it.timestamp) < TimeUnit.DAYS.toMillis(30) 
            }
            TimeRange.ALL -> simulations
            else -> simulations
        }
    }

    private fun calculateExtendedStats(simulations: List<SimulationEntity>): ExtendedStats {
        if (simulations.isEmpty()) return ExtendedStats()

        val numberFrequency = simulations.groupBy { it.actualNumber }
            .mapValues { it.value.size }
        
        val mostCommonNumber = numberFrequency.maxByOrNull { it.value }?.key ?: 0
        val leastCommonNumber = numberFrequency.minByOrNull { it.value }?.key ?: 0

        val consecutiveCorrect = simulations
            .asSequence()
            .windowed(size = 2, step = 1)
            .map { it[0].predictedNumber == it[0].actualNumber && it[1].predictedNumber == it[1].actualNumber }
            .fold(Pair(0, 0)) { (max, current), isCorrect ->
                when {
                    isCorrect -> Pair(maxOf(max, current + 1), current + 1)
                    else -> Pair(max, 0)
                }
            }.first

        return ExtendedStats(
            mostCommonNumber = mostCommonNumber,
            leastCommonNumber = leastCommonNumber,
            maxConsecutiveCorrect = consecutiveCorrect,
            totalPredictions = simulations.size
        )
    }

    private fun calculateStats(simulations: List<SimulationEntity>): StatsSummary {
        if (simulations.isEmpty()) return StatsSummary()

        val totalPredictions = simulations.size
        val correctPredictions = simulations.count { it.predictedNumber == it.actualNumber }
        val accuracy = (correctPredictions.toFloat() / totalPredictions) * 100
        val averageError = simulations
            .map { Math.abs(it.predictedNumber - it.actualNumber) }
            .average()
            .toFloat()

        return StatsSummary(
            totalPredictions = totalPredictions,
            correctPredictions = correctPredictions,
            accuracy = accuracy,
            averageError = averageError
        )
    }

    fun refreshStatistics() {
        loadStatistics()
    }
} 