package com.roulette.tracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface Repository<T> {
    fun getAll(): Flow<List<T>>
    fun getById(id: Long): Flow<T?>
    suspend fun insert(item: T): Long
    suspend fun delete(item: T)
    suspend fun deleteAll()
    suspend fun update(item: T)
    suspend fun deleteOlderThan(timestamp: Long)
    
    // Optionale Methoden mit Default-Implementierung
    suspend fun insertAll(items: List<T>) {
        items.forEach { insert(it) }
    }
    
    suspend fun exists(id: Long): Boolean {
        return getById(id).first() != null
    }
}

@Singleton
class SimulationRepositoryImpl @Inject constructor(
    private val simulationDao: SimulationDao
) : Repository<SimulationResult> {
    override fun getAll(): Flow<List<SimulationResult>> = simulationDao.getAllResults()
    
    override fun getById(id: Long): Flow<SimulationResult?> = simulationDao.getById(id)
    
    override suspend fun insert(item: SimulationResult): Long = simulationDao.insert(item)
    
    override suspend fun update(item: SimulationResult) {
        simulationDao.updateActualNumber(item.id, item.actualNumber ?: return)
    }
    
    override suspend fun delete(item: SimulationResult) {
        // Implement if needed
    }
    
    override suspend fun deleteAll() = simulationDao.deleteAll()
    
    override suspend fun deleteOlderThan(timestamp: Long) {
        // Implement if needed
    }
} 