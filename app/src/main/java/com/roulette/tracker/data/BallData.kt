package com.roulette.tracker.data

data class BallData(
    val currentPosition: Point,
    val velocity: Double,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
) 