package com.roulette.tracker.ui.tracking

import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import javax.inject.Inject
import com.roulette.tracker.camera.CameraManager
import com.roulette.tracker.utils.OpenCVManager
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import android.util.Log
import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import androidx.camera.core.ResolutionSelector
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.roulette.tracker.camera.FrameAnalyzerFactory
import com.roulette.tracker.camera.FrameAnalyzerConfig

class CameraFrameProvider @Inject constructor(
    private val cameraManager: CameraManager,
    private val openCVManager: OpenCVManager,
    private val config: FrameProviderConfig,
    private val frameAnalyzerFactory: FrameAnalyzerFactory
) : BaseFrameProvider(), DefaultLifecycleObserver {
    
    private val frameChannel = Channel<Mat>(BUFFERED)
    private var previewView: PreviewView? = null
    private val frameAnalyzer = frameAnalyzerFactory.create(
        onFrameAnalyzed = { mat ->
            if (isActive) {
                val frameCopy = mat.clone()
                val sent = frameChannel.trySend(frameCopy).isSuccess
                if (!sent) {
                    frameCopy.safeRelease()
                } else {
                    _cameraMetrics.value = _cameraMetrics.value.copy(
                        isRunning = true,
                        lastError = null
                    )
                }
            }
        },
        config = FrameAnalyzerConfig(
            targetFps = config.frameRate,
            colorConversion = FrameAnalyzerConfig.ColorConversion.YUV2BGR
        )
    )

    private val _cameraMetrics = MutableStateFlow(CameraMetrics())
    val cameraMetrics: StateFlow<CameraMetrics> = _cameraMetrics
    
    fun setPreviewView(view: PreviewView) {
        previewView = view
    }
    
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isActive = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        stopCamera()
    }
    
    override fun onError(error: Throwable) {
        handleError(error as? Exception ?: Exception(error), "Camera error")
    }
    
    override suspend operator fun invoke(): Mat? = withContext(Dispatchers.Default) {
        if (!isActive) return@withContext null
        
        try {
            frameChannel.receive()
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving frame", e)
            null
        }
    }

    fun getFrameFlow(): Flow<Mat> = flow {
        while (isActive) {
            getNextFrame()?.let { frame ->
                emit(frame)
            }
        }
    }.flowOn(Dispatchers.Default)
    
    fun stopCamera() {
        isActive = false
        cameraManager.stopCamera()
        frameChannel.close()
    }
    
    companion object {
        private const val TAG = "CameraFrameProvider"
    }
}

data class CameraMetrics(
    val isRunning: Boolean = false,
    val lastError: String? = null,
    val errorCount: Int = 0
) 