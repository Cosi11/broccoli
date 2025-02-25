package com.roulette.tracker.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "simulation_results")
data class SimulationResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val predictedNumber: Int,
    val actualNumber: Int? = null,
    val confidence: Double
) 