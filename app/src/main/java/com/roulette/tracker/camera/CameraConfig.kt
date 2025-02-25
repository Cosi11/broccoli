package com.roulette.tracker.camera

import android.content.Context
import android.util.Log
import android.util.Size
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class CameraConfig @Inject constructor(
    private val context: Context
) {
    fun getOptimalPreviewSize(
        targetWidth: Int,
        targetHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
        aspectRatio: Size
    ): Size {
        // Berechne das Ziel-Seitenverhältnis direkt in der Höhenberechnung
        val targetArea = targetWidth * targetHeight
        
        // Finde die beste Größe basierend auf Zielbereich
        val width = if (targetArea > maxWidth * maxHeight) {
            maxWidth
        } else {
            min(targetWidth, maxWidth)
        }
        
        // Berechne Höhe direkt mit dem Seitenverhältnis von aspectRatio
        val height = (width / (aspectRatio.width.toFloat() / aspectRatio.height))
            .toInt()
            .coerceAtMost(maxHeight)
        
        return Size(width, height).also { size ->
            Log.d(TAG, "Selected preview size: $size (area: $targetArea)")
        }
    }

    fun getOptimalImageAnalysisSize(): Size {
        return Size(1280, 720) // HD für gute Performance/Qualität-Balance
    }

    companion object {
        private const val TAG = "CameraConfig"
    }
} 