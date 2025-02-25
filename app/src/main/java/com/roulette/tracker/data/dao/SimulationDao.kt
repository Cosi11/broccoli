package com.roulette.tracker.data.dao

import androidx.room.*
import com.roulette.tracker.data.entities.SimulationEntity
import com.roulette.tracker.data.entities.SimulationResult
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulationDao {
    @Query("SELECT * FROM simulations")
    fun getAllSimulations(): Flow<List<SimulationEntity>>

    @Query("SELECT * FROM simulation_results")
    fun getAllResults(): Flow<List<SimulationResult>>

    @Query("SELECT * FROM simulation_results WHERE id = :id")
    fun getResultById(id: Long): Flow<SimulationResult?>

    @Query("SELECT AVG(CASE WHEN actualNumber = predictedNumber THEN 1.0 ELSE 0.0 END) FROM simulation_results WHERE actualNumber IS NOT NULL")
    fun getAccuracyRate(): Flow<Float>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: SimulationResult)

    @Delete
    suspend fun delete(result: SimulationResult)

    @Query("DELETE FROM simulation_results")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM simulation_results 
        WHERE timestamp >= :since 
        AND timestamp <= :now
        ORDER BY timestamp DESC
    """)
    fun getResultsSince(since: Long, now: Long = System.currentTimeMillis()): Flow<List<SimulationResult>>

    @Insert
    suspend fun insertSimulation(simulation: SimulationEntity)

    @Query("UPDATE simulation_results SET actualNumber = :actualNumber WHERE id = :resultId")
    suspend fun updateActualNumber(resultId: Long, actualNumber: Int)

    @Query("DELETE FROM simulation_results WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
} 