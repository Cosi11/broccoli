package com.roulette.tracker.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.roulette.tracker.data.repository.Repository
import com.roulette.tracker.data.entities.SimulationResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: Repository<SimulationResult>
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Lösche alte Simulationsergebnisse
            val cutoffTime = System.currentTimeMillis() - RETENTION_PERIOD
            repository.deleteOlderThan(cutoffTime)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val RETENTION_PERIOD = 30L * 24 * 60 * 60 * 1000 // 30 Tage
    }
} 