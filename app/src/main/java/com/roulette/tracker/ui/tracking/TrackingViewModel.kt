package com.roulette.tracker.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roulette.tracker.data.BallTracker
import com.roulette.tracker.data.WheelTracker
import com.roulette.tracker.PredictionEngine
import com.roulette.tracker.data.repository.SimulationRepository
import com.roulette.tracker.data.TrackingState
import com.roulette.tracker.data.BallData
import com.roulette.tracker.data.WheelData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import org.opencv.core.Mat
import com.roulette.tracker.data.entities.SimulationResult
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.BufferOverflow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.TimeSource

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val ballTracker: BallTracker,
    private val wheelTracker: WheelTracker,
    private val predictionEngine: PredictionEngine,
    private val simulationRepository: SimulationRepository
) : ViewModel() {
    private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.INITIALIZING)
    val trackingState = _trackingState.asStateFlow()
        .distinctUntilChanged()
        .onEach { state -> logStateTransition(state) }
        .debounce(STATE_UPDATE_DEBOUNCE)
        .sample(STATE_SAMPLING_INTERVAL)
    
    private val errorChannel = Channel<String>(Channel.BUFFERED)
    private val frameMetrics = ConcurrentHashMap<String, Long>()
    private val _processingStats = MutableStateFlow(ProcessingStats())
    val processingStats = _processingStats.asStateFlow()
        .distinctUntilChanged()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)
    
    private val frameCount = AtomicLong(0)
    private val lastFrameTime = AtomicLong(0)
    private val startTime = AtomicReference(TimeSource.Monotonic.markNow())
    private val lastMetricsUpdate = AtomicLong(0)
    
    private var trackingJob: Job? = null
    private var frameProcessor: FrameProcessor? = null
    private var lastPredictionTime = 0L
    private val predictionMutex = Mutex()
    private val isTracking = AtomicBoolean(false)
    
    init {
        handleErrors()
    }

    private fun handleErrors() {
        viewModelScope.launch {
            errorChannel.consumeEach { errorMessage ->
                Log.e(TAG, errorMessage)
                _trackingState.value = TrackingState.Error(errorMessage)
            }
        }
    }
    
    fun startTracking(frameProvider: FrameProvider) {
        if (!isTracking.compareAndSet(false, true)) {
            return // Verhindere mehrfaches Starten
        }

        trackingJob?.cancel()
        frameProcessor = FrameProcessor(frameProvider)
        
        trackingJob = viewModelScope.launch {
            try {
                initializeTracking()
                processFrames()
            } catch (e: Exception) {
                handleError("Tracking-Fehler: ${e.message}")
            } finally {
                isTracking.set(false)
            }
        }
    }

    fun stopTracking() {
        if (isTracking.compareAndSet(true, false)) {
            cleanup()
            _trackingState.value = TrackingState.INITIALIZING
        }
    }

    private suspend fun initializeTracking() {
        _trackingState.value = TrackingState.Running
        lastPredictionTime = 0L
    }

    private suspend fun processFrames() {
        frameProcessor?.processFrames()
            ?.buffer(UNLIMITED)
            ?.filterNotNull()
            ?.onEach { updateFrameMetrics() }
            ?.transformWhile { frame ->
                if (!isTracking.get()) {
                    false // Stoppt die Transformation
                } else {
                    emit(frame)
                    true // Fortsetzung
                }
            }
            ?.map { frame -> 
                measureFrameProcessing {
                    processFrame(frame)
                }
            }
            ?.flowOn(Dispatchers.Default)
            ?.retry(FRAME_RETRY_COUNT) { e ->
                val shouldRetry = handleRetry(e)
                if (shouldRetry) delay(calculateBackoffDelay())
                shouldRetry && isTracking.get()
            }
            ?.catch { e -> handleError("Frame-Processing-Fehler: ${e.message}") }
            ?.collect()
    }

    private fun calculateBackoffDelay(): Long {
        val retryCount = (frameMetrics["retryCount"] ?: 0) + 1
        frameMetrics["retryCount"] = retryCount
        return minOf(
            FRAME_RETRY_DELAY * (1L shl (retryCount - 1)), // Exponentielles Backoff
            MAX_RETRY_DELAY
        )
    }

    private fun updateFrameMetrics() {
        frameCount.incrementAndGet()
        val currentTime = System.nanoTime()
        lastFrameTime.set(currentTime)
        
        // Periodische Metrik-Updates
        if (shouldUpdateMetrics(currentTime)) {
            updateDetailedMetrics()
        }
    }

    private fun shouldUpdateMetrics(currentTime: Long): Boolean {
        val lastUpdate = lastMetricsUpdate.get()
        return (currentTime - lastUpdate) >= METRICS_UPDATE_INTERVAL * 1_000_000 && 
               lastMetricsUpdate.compareAndSet(lastUpdate, currentTime)
    }

    private fun updateDetailedMetrics() {
        val currentStats = ProcessingStats(
            frameRate = calculateCurrentFrameRate(),
            processingTime = measureProcessingTime(),
            successRate = calculateSuccessRate(),
            errorRate = calculateErrorRate(),
            averageProcessingTime = calculateAverageProcessingTime(),
            totalFrames = frameCount.get(),
            droppedFrames = calculateDroppedFrames(),
            memoryUsage = calculateMemoryUsage(),
            cpuUsage = calculateCpuUsage()
        )
        
        _processingStats.value = currentStats
        
        // Log wichtige Metriken
        if (currentStats.errorRate > ERROR_RATE_THRESHOLD || 
            currentStats.droppedFrames > DROPPED_FRAMES_THRESHOLD) {
            Log.w(TAG, "Performance-Warnung: $currentStats")
        }
    }

    private fun calculateMemoryUsage(): Float {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        return used.toFloat() / runtime.maxMemory()
    }

    private fun calculateCpuUsage(): Float {
        // Vereinfachte CPU-Auslastung basierend auf Verarbeitungszeit
        return (frameMetrics["totalProcessingTime"] ?: 0).toFloat() / 
               (System.nanoTime() - startTime.get().elapsedNow().inWholeNanoseconds)
    }

    private fun handleRetry(error: Throwable): Boolean {
        handleError("Frame-Verarbeitung wird neu versucht: ${error.message}")
        return when (error) {
            is OutOfMemoryError -> false
            is IllegalStateException -> false
            else -> true
        }
    }

    private suspend fun <T> measureFrameProcessing(block: suspend () -> T): T {
        val startTime = System.nanoTime()
        return try {
            block()
        } catch (e: Exception) {
            frameMetrics["processingErrors"] = (frameMetrics["processingErrors"] ?: 0) + 1
            throw e
        } finally {
            val duration = (System.nanoTime() - startTime) / 1_000_000
            updateProcessingMetrics(duration)
        }
    }

    private fun updateProcessingMetrics(duration: Long) {
        frameMetrics["lastFrameTime"] = duration
        frameMetrics["totalProcessingTime"] = 
            (frameMetrics["totalProcessingTime"] ?: 0) + duration
        frameMetrics["frameCount"] = frameCount.get()
        updateProcessingStats()
    }

    private fun calculateCurrentFrameRate(): Float {
        val currentTime = System.nanoTime()
        val lastTime = lastFrameTime.get()
        if (lastTime == 0L) return 0f
        
        val timeDiff = (currentTime - lastTime) / 1_000_000 // ms
        return if (timeDiff > 0) 1000f / timeDiff else 0f
    }

    private fun calculateAverageProcessingTime(): Float {
        val total = frameMetrics["totalProcessingTime"] ?: 0
        val count = frameMetrics["frameCount"] ?: 1
        return total.toFloat() / count
    }

    private fun updateProcessingStats() {
        _processingStats.value = ProcessingStats(
            frameRate = calculateCurrentFrameRate(),
            processingTime = measureProcessingTime(),
            successRate = calculateSuccessRate(),
            errorRate = calculateErrorRate(),
            averageProcessingTime = calculateAverageProcessingTime(),
            totalFrames = frameCount.get(),
            droppedFrames = calculateDroppedFrames()
        )
    }

    private fun calculateDroppedFrames(): Long {
        val expectedFrames = ((System.nanoTime() - startTime.get().elapsedNow().inWholeNanoseconds) / 1_000_000 / FRAME_PROCESSING_DELAY)
        return maxOf(0, expectedFrames - frameCount.get())
    }

    private suspend fun processFrame(frame: Mat) = withContext(Dispatchers.Default) {
        try {
            // Ball tracken
            val ballData = ballTracker.trackBall(frame)
                ?: return@withContext handleTrackingError(TrackingState.BallNotFound)
            
            // Rad tracken
            val wheelData = wheelTracker.trackWheel(frame).also { wheelData ->
                if (!wheelData.isValid()) {
                    return@withContext handleTrackingError(TrackingState.WheelNotFound)
                }
            }
            
            // Vorhersage wenn nötig
            checkAndMakePrediction(ballData, wheelData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Frame-Verarbeitung", e)
            handleTrackingError(TrackingState.Error("Frame-Verarbeitung fehlgeschlagen: ${e.message}"))
        } finally {
            frame.release() // OpenCV Ressourcen freigeben
        }
    }
    
    private suspend fun checkAndMakePrediction(ballData: BallData, wheelData: WheelData) {
        if (!isReadyForPrediction(ballData, wheelData)) return
        
        predictionMutex.withLock {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPredictionTime <= PREDICTION_INTERVAL) return
            
            makePrediction(ballData, wheelData)
            lastPredictionTime = currentTime
        }
    }

    private fun isReadyForPrediction(ballData: BallData, wheelData: WheelData): Boolean {
        return shouldMakePrediction(ballData, wheelData) && 
               isTrackingStable(ballData, wheelData)
    }

    private fun isTrackingStable(ballData: BallData, wheelData: WheelData): Boolean {
        return ballData.confidence >= MIN_CONFIDENCE &&
               ballData.velocity <= MAX_BALL_VELOCITY &&
               wheelData.rotationSpeed <= MAX_WHEEL_SPEED
    }

    private suspend fun makePrediction(ballData: BallData, wheelData: WheelData) = 
        withContext(Dispatchers.IO) {
            try {
                val prediction = predictionEngine.predict(ballData, wheelData)
                savePrediction(prediction, ballData, wheelData)
                updateTrackingState(prediction)
            } catch (e: Exception) {
                handleError("Vorhersage fehlgeschlagen: ${e.message}")
            }
        }

    private suspend fun savePrediction(
        prediction: PredictionResult,
        ballData: BallData,
        wheelData: WheelData
    ) = withContext(Dispatchers.IO) {
        predictionMutex.withLock {
            val stats = _processingStats.value
            val result = SimulationResult(
                predictedNumber = prediction.predictedNumber,
                confidence = prediction.confidence,
                timestamp = System.currentTimeMillis(),
                ballVelocity = ballData.velocity,
                wheelSpeed = wheelData.rotationSpeed,
                ballConfidence = ballData.confidence,
                wheelConfidence = wheelData.confidence,
                frameRate = stats.frameRate,
                processingTime = stats.processingTime,
                successRate = stats.successRate,
                errorRate = stats.errorRate,
                averageProcessingTime = stats.averageProcessingTime,
                droppedFrames = stats.droppedFrames
            )
            simulationRepository.insertResult(result)
        }
    }

    private suspend fun updateTrackingState(prediction: PredictionResult) {
        withContext(Dispatchers.Main) {
            _trackingState.value = TrackingState.Success(prediction)
        }
    }

    private suspend fun handleError(message: String) {
        errorChannel.send(message)
    }

    private fun shouldMakePrediction(ballData: BallData, wheelData: WheelData): Boolean {
        return ballData.velocity >= MIN_BALL_VELOCITY && 
               wheelData.rotationSpeed >= MIN_WHEEL_SPEED &&
               ballData.confidence >= MIN_CONFIDENCE
    }

    private fun handleTrackingError(error: TrackingState) {
        viewModelScope.launch(Dispatchers.Main) {
            _trackingState.value = error
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
    
    private fun cleanup() {
        trackingJob?.cancel()
        frameProcessor?.release()
        wheelTracker.release()
        predictionEngine.cleanup()
        errorChannel.close()
        frameMetrics.clear()
    }
    
    private fun calculateCurrentFrameRate(): Float {
        // Implementierung der Framerate-Berechnung
        return 1000f / FRAME_PROCESSING_DELAY
    }

    private var lastProcessingTime = 0L
    private fun measureProcessingTime(): Long {
        val current = System.nanoTime()
        val processing = if (lastProcessingTime > 0) {
            (current - lastProcessingTime) / 1_000_000 // Konvertiere zu Millisekunden
        } else 0
        lastProcessingTime = current
        return processing
    }

    private fun logStateTransition(state: TrackingState) {
        Log.d(TAG, "State transition: $state")
        updateMetrics(state)
    }

    private fun updateMetrics(state: TrackingState) {
        when (state) {
            is TrackingState.Success -> {
                frameMetrics["successfulPredictions"] = 
                    (frameMetrics["successfulPredictions"] ?: 0) + 1
            }
            is TrackingState.Error -> {
                frameMetrics["errors"] = (frameMetrics["errors"] ?: 0) + 1
            }
            else -> { /* andere Zustände */ }
        }
        updateProcessingStats()
    }

    private fun calculateSuccessRate(): Float {
        val total = frameMetrics.values.sum()
        return if (total > 0) {
            (frameMetrics["successfulPredictions"] ?: 0).toFloat() / total
        } else 0f
    }

    private fun calculateErrorRate(): Float {
        val total = frameMetrics.values.sum()
        return if (total > 0) {
            (frameMetrics["errors"] ?: 0).toFloat() / total
        } else 0f
    }

    companion object {
        private const val TAG = "TrackingViewModel"
        private const val FRAME_PROCESSING_DELAY = 33L // ~30 FPS
        private const val PREDICTION_INTERVAL = 500L
        private const val MIN_BALL_VELOCITY = 1.0
        private const val MAX_BALL_VELOCITY = 100.0
        private const val MIN_WHEEL_SPEED = 0.1
        private const val MAX_WHEEL_SPEED = 50.0
        private const val MIN_CONFIDENCE = 0.7
        private const val FRAME_RETRY_COUNT = 3L
        private const val FRAME_RETRY_DELAY = 100L
        private const val STATE_UPDATE_DEBOUNCE = 100L
        private const val STATE_SAMPLING_INTERVAL = 250L
        private const val METRICS_UPDATE_INTERVAL = 1000L // 1 Sekunde
        private const val MAX_RETRY_DELAY = 5000L // 5 Sekunden
        private const val ERROR_RATE_THRESHOLD = 0.1f // 10%
        private const val DROPPED_FRAMES_THRESHOLD = 30L // pro Sekunde
    }
}

data class ProcessingStats(
    val frameRate: Float = 0f,
    val processingTime: Long = 0L,
    val successRate: Float = 0f,
    val errorRate: Float = 0f,
    val averageProcessingTime: Float = 0f,
    val totalFrames: Long = 0L,
    val droppedFrames: Long = 0L,
    val memoryUsage: Float = 0f,
    val cpuUsage: Float = 0f
)

class FrameProcessor(private val frameProvider: FrameProvider) {
    private var isProcessing = true
    
    fun processFrames() = flow {
        while (isProcessing) {
            frameProvider.getNextFrame()?.let { emit(it) }
        }
    }
    
    fun release() {
        isProcessing = false
    }
} 