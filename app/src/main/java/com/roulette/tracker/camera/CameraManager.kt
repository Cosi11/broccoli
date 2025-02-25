package com.roulette.tracker.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

sealed class CameraResult {
    object Success : CameraResult()
    data class Error(val exception: Exception) : CameraResult()
}

class CameraManager @Inject constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val systemCameraManager: android.hardware.camera2.CameraManager,
    private val openCVLoader: OpenCVLoader
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    suspend fun startCamera(
        previewView: PreviewView,
        imageAnalyzer: ImageAnalysis.Analyzer
    ) = suspendCoroutine { continuation ->
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    this.imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, imageAnalyzer)
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        this.imageAnalyzer
                    )
                    continuation.resume(CameraResult.Success)
                } catch (e: Exception) {
                    Log.e(TAG, ERROR_USE_CASE_BINDING_FAILED, e)
                    continuation.resume(CameraResult.Error(e))
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, ERROR_USE_CASE_BINDING_FAILED, e)
            continuation.resume(CameraResult.Error(e))
        }
    }

    suspend fun startCamera(
        preview: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        resolutionSelector: ResolutionSelector,
        frameRate: Int,
        useHardwareAcceleration: Boolean
    ) = suspendCoroutine { continuation ->
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also {
                            it.setSurfaceProvider(preview.surfaceProvider)
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetFrameRate(frameRate)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setImageQueueDepth(1)
                        .build()
                        .also {
                            it.setAnalyzer(
                                if (useHardwareAcceleration) cameraExecutor else Dispatchers.Default.asExecutor(),
                                analyzer
                            )
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    continuation.resume(CameraResult.Success)
                } catch (e: Exception) {
                    Log.e(TAG, ERROR_USE_CASE_BINDING_FAILED, e)
                    continuation.resume(CameraResult.Error(e))
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, ERROR_USE_CASE_BINDING_FAILED, e)
            continuation.resume(CameraResult.Error(e))
        }
    }

    fun stopCamera() {
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraManager"
        private const val ERROR_USE_CASE_BINDING_FAILED = "Anwendungsfallbindung fehlgeschlagen"
    }
} 