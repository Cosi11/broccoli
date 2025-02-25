package com.roulette.tracker.data

import com.roulette.tracker.data.entities.SimulationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

object StatisticsProcessor {
    suspend fun process(data: List<SimulationResult>) = coroutineScope {
        withContext(Dispatchers.Default) {
            // Parallele Berechnung der verschiedenen Statistiken
            val accuracyDataDeferred = async { calculateAccuracyData(data) }
            val predictionDataDeferred = async { extractPredictionData(data) }
            val last100StatsDeferred = async { calculateStats(data.takeLast(100)) }
            val last1000StatsDeferred = async { calculateStats(data.takeLast(1000)) }
            val extendedStatsDeferred = async { calculateExtendedStats(data) }

            StatisticsData(
                accuracyData = accuracyDataDeferred.await(),
                predictionData = predictionDataDeferred.await(),
                last100Stats = last100StatsDeferred.await(),
                last1000Stats = last1000StatsDeferred.await(),
                extendedStats = extendedStatsDeferred.await()
            )
        }
    }

    private fun calculateAccuracyData(data: List<SimulationResult>): List<Float> {
        return data.asSequence()
            .map { it.accuracy.toFloat() }
            .toList()
    }

    private fun extractPredictionData(data: List<SimulationResult>): List<Pair<Int, Int>> {
        return data.asSequence()
            .map { Pair(it.predictedNumber, it.actualNumber) }
            .toList()
    }

    private fun calculateStats(data: List<SimulationResult>): StatsSummary {
        if (data.isEmpty()) return StatsSummary()

        val totalPredictions = data.size
        val correctPredictions = data.count { it.predictedNumber == it.actualNumber }
        val accuracy = (correctPredictions.toFloat() / totalPredictions) * 100
        val averageError = data.asSequence()
            .map { abs(it.predictedNumber - it.actualNumber) }
            .average()
            .toFloat()

        return StatsSummary(
            totalPredictions = totalPredictions,
            correctPredictions = correctPredictions,
            accuracy = accuracy,
            averageError = averageError
        )
    }

    private fun calculateExtendedStats(data: List<SimulationResult>): ExtendedStats {
        if (data.isEmpty()) return ExtendedStats()

        val numberFrequency = data.asSequence()
            .groupBy { it.actualNumber }
            .mapValues { it.value.size }
            .toMap()
        
        return ExtendedStats(
            mostCommonNumber = numberFrequency.maxByOrNull { it.value }?.key ?: 0,
            leastCommonNumber = numberFrequency.minByOrNull { it.value }?.key ?: 0,
            maxConsecutiveCorrect = calculateMaxConsecutiveCorrect(data),
            totalPredictions = data.size,
            numberFrequencies = numberFrequency
        )
    }

    private fun calculateMaxConsecutiveCorrect(data: List<SimulationResult>): Int {
        return data.asSequence()
            .windowed(2)
            .fold(Triple(0, 0, 0)) { (max, current, _), window ->
                val (result1, result2) = window
                when {
                    result1.predictedNumber == result1.actualNumber && 
                    result2.predictedNumber == result2.actualNumber -> 
                        Triple(maxOf(max, current + 1), current + 1, 1)
                    result2.predictedNumber == result2.actualNumber -> 
                        Triple(max, 1, 1)
                    else -> 
                        Triple(max, 0, 0)
                }
            }.first
    }

    // Cache für häufig verwendete Berechnungen
    private val statsCache = mutableMapOf<String, Any>()
    
    private fun getCachedValue(key: String, calculator: () -> Any): Any {
        return statsCache.getOrPut(key, calculator)
    }
    
    fun clearCache() {
        statsCache.clear()
    }
} 