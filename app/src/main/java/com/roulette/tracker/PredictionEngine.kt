package com.roulette.tracker

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*
import com.roulette.tracker.data.*
import android.util.Log
import kotlin.math.abs
import kotlin.math.PI
import com.roulette.tracker.ml.MLModel
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class PredictionEngine @Inject constructor(
    private val context: Context
) {
    private var mlModel: MLModel? = null
    private var tts: TextToSpeech? = null
    
    init {
        initializeTTS()
    }
    
    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            } else {
                Timber.e("TTS Initialisierung fehlgeschlagen mit Status: $status")
            }
        }
    }
    
    private val rouletteNumbers = listOf(
        0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30,
        8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7,
        28, 12, 35, 3, 26
    )

    fun predict(ballData: BallData, wheelData: WheelData): PredictionResult {
        if (!isValidInput(ballData, wheelData)) {
            return PredictionResult(-1, 0.0)
        }

        val predictedNumber = calculatePredictedNumber(ballData, wheelData)
        val confidence = calculateConfidence(ballData, wheelData).toDouble()
        
        if (confidence > 0.8) {
            playAudioPrediction(predictedNumber)
        }
        
        return PredictionResult(
            predictedNumber = predictedNumber,
            confidence = confidence
        )
    }

    private fun isValidInput(ballData: BallData, wheelData: WheelData): Boolean {
        return ballData.velocity >= MIN_BALL_VELOCITY && 
               abs(wheelData.rotationSpeed) >= MIN_WHEEL_SPEED
    }

    private fun calculateTimeToLanding(ballData: BallData): Double {
        return (WHEEL_RADIUS * PI) / ballData.velocity
    }

    private fun predictLandingPosition(ballData: BallData, timeToLanding: Double): Point {
        val deceleratedVelocity = ballData.velocity * FRICTION_FACTOR
        val x = (ballData.currentPosition.x + deceleratedVelocity * timeToLanding) % (2 * PI)
        val y = ballData.currentPosition.y
        return Point(x, y)
    }

    private fun predictWheelPosition(
        currentAngle: Double, 
        rotationSpeed: Double, 
        timeToLanding: Double
    ): Double {
        return (currentAngle + rotationSpeed * timeToLanding) % 360.0
    }

    private fun determineNumberAtPosition(angle: Double): Int {
        val normalizedAngle = (angle + 360.0) % 360.0
        val index = ((normalizedAngle / 360.0) * rouletteNumbers.size).toInt()
        return rouletteNumbers[index]
    }

    private fun calculateConfidence(ballData: BallData, wheelData: WheelData): Float {
        var confidence = 100f
        
        confidence *= when {
            ballData.velocity > MAX_RELIABLE_BALL_VELOCITY -> 0.8f
            abs(wheelData.rotationSpeed) > MAX_RELIABLE_WHEEL_SPEED -> 0.8f
            calculateTimeToLanding(ballData) > MAX_RELIABLE_PREDICTION_TIME -> 0.7f
            else -> 1.0f
        }

        return confidence.coerceIn(0f, 100f)
    }

    private fun playAudioPrediction(number: Int) {
        tts?.speak(
            "Vorhergesagte Nummer: $number", 
            TextToSpeech.QUEUE_FLUSH, 
            null, 
            null
        )
    }
    
    fun cleanup() {
        tts?.apply {
            stop()
            shutdown()
        }
        mlModel = null
    }

    private fun calculatePredictedNumber(ballData: BallData, wheelData: WheelData): Int {
        val timeToLanding = calculateTimeToLanding(ballData)
        val landingPosition = predictLandingPosition(ballData, timeToLanding)
        val wheelPositionAtLanding = predictWheelPosition(
            wheelData.currentAngle,
            wheelData.rotationSpeed,
            timeToLanding
        )

        announcePosition(landingPosition)
        return determineNumberAtPosition(wheelPositionAtLanding)
    }

    private fun announcePosition(position: Point) {
        tts?.speak(
            "Ball landet bei Position ${position.x.toInt()}, ${position.y.toInt()}", 
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }

    companion object {
        private const val WHEEL_RADIUS = 400.0
        private const val FRICTION_FACTOR = 0.95
        private const val MIN_BALL_VELOCITY = 1.0
        private const val MIN_WHEEL_SPEED = 0.1
        private const val MAX_RELIABLE_BALL_VELOCITY = 100f
        private const val MAX_RELIABLE_WHEEL_SPEED = 50.0
        private const val MAX_RELIABLE_PREDICTION_TIME = 5.0
    }
} 