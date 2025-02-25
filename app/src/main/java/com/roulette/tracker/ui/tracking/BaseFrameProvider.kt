package com.roulette.tracker.ui.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.opencv.core.Mat
import java.util.concurrent.atomic.AtomicLong
import timber.log.Timber

abstract class BaseFrameProvider : FrameProvider {
    protected var isActive = true
    private val frameCount = AtomicLong(0)
    private val droppedFrames = AtomicLong(0)
    private val _metrics = MutableStateFlow(FrameMetrics())
    val metrics: StateFlow<FrameMetrics> = _metrics
    
    protected fun updateMetrics(processed: Boolean = true) {
        if (processed) {
            frameCount.incrementAndGet()
        } else {
            droppedFrames.incrementAndGet()
        }
        _metrics.value = FrameMetrics(
            totalFrames = frameCount.get(),
            droppedFrames = droppedFrames.get(),
            fps = calculateFps()
        )
    }
    
    private var lastFpsCalculation = 0L
    private var framesSinceLastCalculation = 0
    private var currentFps = 0f
    
    private fun calculateFps(): Float {
        val now = System.currentTimeMillis()
        if (lastFpsCalculation == 0L) {
            lastFpsCalculation = now
            return 0f
        }
        
        framesSinceLastCalculation++
        val timeDiff = now - lastFpsCalculation
        
        if (timeDiff >= 1000) {  // Aktualisiere FPS jede Sekunde
            currentFps = (framesSinceLastCalculation * 1000f) / timeDiff
            framesSinceLastCalculation = 0
            lastFpsCalculation = now
        }
        
        return currentFps
    }
    
    protected fun handleError(e: Exception, message: String) {
        Timber.e(e, message)
        updateMetrics(false)
    }
    
    fun stop() {
        isActive = false
    }
    
    fun start() {
        isActive = true
        resetMetrics()
    }
    
    private fun resetMetrics() {
        frameCount.set(0)
        droppedFrames.set(0)
        lastFpsCalculation = 0
        framesSinceLastCalculation = 0
        currentFps = 0f
        _metrics.value = FrameMetrics()
    }
    
    protected fun Mat.safeRelease() {
        try {
            if (!this.empty()) {
                this.release()
            }
        } catch (e: Exception) {
            handleError(e, "Error releasing Mat")
        }
    }
}

data class FrameMetrics(
    val totalFrames: Long = 0,
    val droppedFrames: Long = 0,
    val fps: Float = 0f
) 