package com.roulette.tracker

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModels
import com.roulette.tracker.camera.CameraManager
import com.roulette.tracker.camera.CameraPreviewManager
import com.roulette.tracker.camera.FrameAnalyzer
import com.roulette.tracker.data.BallTracker
import com.roulette.tracker.data.PredictionEngine
import com.roulette.tracker.data.WheelTracker
import com.roulette.tracker.data.BallData
import com.roulette.tracker.data.WheelData
import com.roulette.tracker.data.PredictionResult
import com.roulette.tracker.databinding.ActivityTrackingBinding
import com.roulette.tracker.tracking.TrackingStateManager
import kotlinx.coroutines.launch
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackingActivity : AppCompatActivity() {
    @Inject lateinit var ballTracker: BallTracker
    @Inject lateinit var wheelTracker: WheelTracker
    @Inject lateinit var predictionEngine: PredictionEngine
    private lateinit var binding: ActivityTrackingBinding
    private lateinit var systemCameraManager: android.hardware.camera2.CameraManager
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraPreviewManager: CameraPreviewManager
    private lateinit var trackingStateManager: TrackingStateManager
    private val viewModel: TrackingViewModel by viewModels()
    
    private var frameAnalyzer: FrameAnalyzer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeManagers()
        setupObservers()
        setupUI()
    }

    private fun initializeManagers() {
        systemCameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        cameraManager = CameraManager(this, this, systemCameraManager)
        cameraPreviewManager = CameraPreviewManager(this)
        trackingStateManager = TrackingStateManager()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.trackingState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun setupUI() {
        binding.btnStartStop.setOnClickListener {
            if (viewModel.isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }
    }

    private fun startTracking() {
        if (!checkPermissions()) return
        
        lifecycleScope.launch {
            when (val result = initializeCamera()) {
                is CameraResult.Success -> viewModel.startTracking()
                is CameraResult.Error -> showError(result.exception.message)
            }
        }
    }

    private fun stopTracking() {
        viewModel.stopTracking()
        cameraManager.stopCamera()
    }

    private fun initializeCamera(): CameraResult {
        try {
            frameAnalyzer = FrameAnalyzer { frame ->
                processFrame(frame)
            }
            lifecycleScope.launch {
                cameraManager.startCamera(
                    binding.previewView,
                    frameAnalyzer!!
                )
            }
            return CameraResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Kamera-Setup: ${e.message}")
            return CameraResult.Error(e)
        }
    }

    private fun processFrame(frame: Mat) {
        try {
            trackingStateManager.startTracking()
            
            val ballData = ballTracker.trackBall(frame)
            if (ballData.currentPosition == Point(0.0, 0.0)) {
                trackingStateManager.ballLost()
                return
            }
            
            val wheelData = wheelTracker.trackWheel(frame)
            if (wheelData.centerPoint == null) {
                trackingStateManager.wheelLost()
                return
            }
            
            showDebugFrame(frame, ballData, wheelData)
            binding.predictionOverlay.updateDebugInfo(ballData.velocity, wheelData.rotationSpeed)
            
            if (ballData.predictedLandingTime > 0) {
                trackingStateManager.startPrediction(ballData.predictedLandingTime - System.currentTimeMillis())
            }
            
            val prediction = predictionEngine.predict(ballData, wheelData)
            trackingStateManager.predictionComplete(prediction)
            updateUI(prediction)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
            trackingStateManager.reportError(e.message ?: "Unbekannter Fehler")
        }
    }

    private fun showDebugFrame(frame: Mat, ballData: BallData, wheelData: WheelData) {
        try {
            Imgproc.circle(
                frame, 
                ballData.currentPosition,
                5,
                Scalar(0.0, 255.0, 0.0),
                2
            )
            
            wheelData.visibleNumbers.forEachIndexed { index, number ->
                Imgproc.putText(
                    frame,
                    number.toString(),
                    Point(10.0, ((index + 1) * 30).toDouble()),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    1.0,
                    Scalar(255.0, 255.0, 255.0),
                    2
                )
            }
            
            cameraPreviewManager.showPreview(frame, binding.debugPreview)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing debug frame: ${e.message}")
        }
    }

    private fun updateUI(prediction: PredictionResult) {
        binding.predictionOverlay.updatePrediction(prediction)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun checkPermissions(): Boolean {
        // Implement the logic to check permissions
        return true // Placeholder return, actual implementation needed
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        binding.videoView.stopPlayback()
        try {
            cameraPreviewManager.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "TrackingActivity"
    }
} 