package com.roulette.tracker.data.repository

import com.roulette.tracker.data.dao.SimulationDao
import com.roulette.tracker.data.entities.SimulationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class SimulationRepositoryImpl @Inject constructor(
    private val simulationDao: SimulationDao
) : Repository<SimulationResult> {
    
    override fun getAll(): Flow<List<SimulationResult>> = 
        simulationDao.getAllResults()
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e(e, "Fehler beim Laden aller Ergebnisse")
                emit(emptyList())
            }
    
    override fun getById(id: Long): Flow<SimulationResult?> =
        simulationDao.getResultById(id)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e(e, "Fehler beim Laden des Ergebnisses mit ID: $id")
                emit(null)
            }
    
    override suspend fun insert(item: SimulationResult): Long {
        return try {
            simulationDao.insert(item)
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Einfügen des Ergebnisses")
            -1L
        }
    }
    
    override suspend fun update(item: SimulationResult) {
        try {
            item.actualNumber?.let { number ->
                simulationDao.updateActualNumber(item.id, number)
            }
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Aktualisieren des Ergebnisses: ${item.id}")
        }
    }
    
    override suspend fun delete(item: SimulationResult) {
        try {
            simulationDao.delete(item)
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Löschen des Ergebnisses: ${item.id}")
        }
    }
    
    override suspend fun deleteAll() {
        try {
            simulationDao.deleteAll()
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Löschen aller Ergebnisse")
        }
    }

    override suspend fun deleteOlderThan(timestamp: Long) {
        simulationDao.deleteOlderThan(timestamp)
    }

    // Zusätzliche spezifische Methoden
    suspend fun getStatisticsForTimeRange(range: TimeRange): Flow<List<SimulationResult>> = flow {
        withContext(Dispatchers.IO) {
            val startTime = range.getMillis()
            simulationDao.getSimulationResultsAfter(startTime)
                .catch { e -> 
                    Timber.e(e, "Fehler beim Laden der Statistiken für Zeitraum: $range")
                    emit(emptyList())
                }
                .collect { results -> emit(results) }
        }
    }
} 