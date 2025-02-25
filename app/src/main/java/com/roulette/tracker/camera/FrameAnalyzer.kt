@file:OptIn(ExperimentalUnsignedTypes::class)
package com.roulette.tracker.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.core.Mat
import org.opencv.core.CvType
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import javax.inject.Inject
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FrameAnalyzer @Inject constructor(
    private val onFrameAnalyzed: (Mat) -> Unit,
    private val config: FrameAnalyzerConfig
) : ImageAnalysis.Analyzer {

    private var lastProcessingTimeMs: Long = 0
    private val _analyzerMetrics = MutableStateFlow(AnalyzerMetrics())
    val analyzerMetrics: StateFlow<AnalyzerMetrics> = _analyzerMetrics

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessingTimeMs < config.processingInterval) {
            image.close()
            updateMetrics(skipped = true)
            return
        }

        var mat: Mat? = null
        try {
            mat = convertImageToMat(image)
            performColorConversion(mat)
            
            onFrameAnalyzed(mat)
            lastProcessingTimeMs = currentTime
            updateMetrics(processed = true)
        } catch (e: Exception) {
            Timber.e(e, "Frame analysis failed")
            updateMetrics(error = true)
            mat?.safeRelease()
        } finally {
            image.close()
        }
    }

    private fun convertImageToMat(image: ImageProxy): Mat {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        return Mat(image.height, image.width, CvType.CV_8UC1).apply {
            put(0, 0, data)
        }
    }

    private fun performColorConversion(mat: Mat) {
        when (config.colorConversion) {
            ColorConversion.YUV2BGR -> Imgproc.cvtColor(mat, mat, Imgproc.COLOR_YUV2BGR_NV21)
            ColorConversion.YUV2RGB -> Imgproc.cvtColor(mat, mat, Imgproc.COLOR_YUV2RGB_NV21)
            ColorConversion.NONE -> { /* Keine Konvertierung */ }
        }
    }

    private fun Mat.safeRelease() {
        try {
            if (!empty()) {
                release()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error releasing Mat")
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    private fun updateMetrics(
        processed: Boolean = false,
        skipped: Boolean = false,
        error: Boolean = false
    ) {
        val current = _analyzerMetrics.value
        _analyzerMetrics.value = current.copy(
            processedFrames = current.processedFrames + if (processed) 1 else 0,
            skippedFrames = current.skippedFrames + if (skipped) 1 else 0,
            errorFrames = current.errorFrames + if (error) 1 else 0,
            lastProcessingTime = if (processed) System.currentTimeMillis() - lastProcessingTimeMs else current.lastProcessingTime
        )
    }

    companion object {
        private const val TAG = "FrameAnalyzer"
    }
}

data class AnalyzerMetrics(
    val processedFrames: Long = 0,
    val skippedFrames: Long = 0,
    val errorFrames: Long = 0,
    val lastProcessingTime: Long = 0
) 