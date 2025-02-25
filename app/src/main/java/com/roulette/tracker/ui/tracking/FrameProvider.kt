package com.roulette.tracker.ui.tracking

import org.opencv.core.Mat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Funktionale Schnittstelle für die Bereitstellung von Frames
 */
fun interface FrameProvider {
    /**
     * Liefert den nächsten Frame oder null, wenn kein Frame verfügbar ist
     * @return Mat? Der nächste Frame oder null
     */
    suspend fun getNextFrame(): Mat?
}

/**
 * Basis-Implementierung des FrameProviders
 */
abstract class BaseFrameProvider : FrameProvider {
    protected var isActive = true
    
    /**
     * Beendet die Frame-Bereitstellung
     */
    fun stop() {
        isActive = false
    }
    
    /**
     * Startet die Frame-Bereitstellung neu
     */
    fun start() {
        isActive = true
    }
}

/**
 * Erweiterte Version des FrameProcessors mit besserer Fehlerbehandlung
 */
class FrameProcessor(private val frameProvider: FrameProvider) {
    private var isProcessing = true
    private var errorHandler: (Throwable) -> Unit = { }
    
    fun processFrames(): Flow<Mat?> = flow {
        while (isProcessing) {
            try {
                frameProvider.getNextFrame()?.let { frame ->
                    if (!frame.empty()) {
                        emit(frame)
                    } else {
                        frame.release()
                    }
                }
            } catch (e: Exception) {
                errorHandler(e)
            }
        }
    }
    
    fun setErrorHandler(handler: (Throwable) -> Unit) {
        errorHandler = handler
    }
    
    fun release() {
        isProcessing = false
    }
} 