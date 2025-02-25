package com.roulette.tracker.data.repository

import com.roulette.tracker.data.dao.SimulationDao
import com.roulette.tracker.data.entities.SimulationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class SimulationRepository @Inject constructor(
    private val simulationDao: SimulationDao
) : Repository<SimulationResult> {
    val allResults: Flow<List<SimulationResult>> = simulationDao.getAllResults()
    val accuracyRate: Flow<Float> = simulationDao.getAccuracyRate()

    override fun getAll(): Flow<List<SimulationResult>> = 
        simulationDao.getAllResults()
    
    override fun getById(id: Long): Flow<SimulationResult?> = 
        simulationDao.getResultById(id)
    
    override suspend fun insert(item: SimulationResult): Long {
        simulationDao.insert(item)
        return item.id
    }
    
    override suspend fun update(item: SimulationResult) {
        simulationDao.updateActualNumber(item.id, item.actualNumber ?: return)
    }
    
    override suspend fun delete(item: SimulationResult) {
        simulationDao.delete(item)
    }
    
    override suspend fun deleteAll() = 
        simulationDao.deleteAll()
    
    override suspend fun deleteOlderThan(timestamp: Long) {
        simulationDao.deleteOlderThan(timestamp)
    }

    fun getResultsSince(timestamp: Long): Flow<List<SimulationResult>> {
        return simulationDao.getResultsSince(timestamp)
    }

    suspend fun getStatisticsForTimeRange(range: TimeRange): Flow<List<SimulationResult>> = flow {
        withContext(Dispatchers.IO) {
            val startTime = when (range) {
                TimeRange.LAST_WEEK -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                TimeRange.LAST_MONTH -> System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000
                TimeRange.ALL_TIME -> 0
            }
            
            simulationDao.getSimulationResultsAfter(startTime)
                .catch { e -> 
                    Timber.e(e, "Fehler beim Laden der Simulationsergebnisse")
                    emit(emptyList())
                }
                .collect { results ->
                    emit(results)
                }
        }
    }
} 