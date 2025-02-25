package com.roulette.tracker.data.models

data class ExtendedStats(
    val mostCommonNumber: Int = 0,
    val leastCommonNumber: Int = 0,
    val maxConsecutiveCorrect: Int = 0,
    val totalPredictions: Int = 0,
    val numberFrequencies: Map<Int, Int> = emptyMap(),
    val averageAccuracy: Float = 0f,
    val standardDeviation: Float = 0f,
    val winRate: Float = 0f,
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
) {
    fun getHotNumbers(): List<Int> = 
        numberFrequencies.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

    fun getColdNumbers(): List<Int> = 
        numberFrequencies.entries
            .sortedBy { it.value }
            .take(5)
            .map { it.key }
} 