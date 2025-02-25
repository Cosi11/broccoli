package com.roulette.tracker.network

import android.util.Log
import android.view.SurfaceView
import org.opencv.core.Mat
import com.roulette.tracker.utils.FrameExtractor
import kotlinx.coroutines.*

class RtspClient(
    private val url: String,
    private val surfaceView: SurfaceView,
    private val onFrameReceived: (Mat) -> Unit
) {
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val frameExtractor = FrameExtractor()
    
    fun start() {
        isRunning = true
        scope.launch {
            try {
                // Für den Test: Extrahiere einfach Frames vom SurfaceView
                while (isRunning) {
                    frameExtractor.extractFrame(surfaceView)?.let { frame ->
                        onFrameReceived(frame)
                    }
                    delay(33) // ca. 30 FPS
                }
            } catch (e: Exception) {
                Log.e("RtspClient", "Streaming-Fehler: ${e.message}")
            }
        }
    }
    
    fun stop() {
        isRunning = false
        scope.cancel()
    }
} 