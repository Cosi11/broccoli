package com.roulette.tracker.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "simulations")
data class SimulationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val settings: String  // JSON der Simulationseinstellungen
) 