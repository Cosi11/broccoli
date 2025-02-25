package com.roulette.tracker.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.opencv.core.Point
import timber.log.Timber

class MLModel private constructor(private val interpreter: Interpreter) {
    private val modelFile = "roulette_model.tflite"
    private val inputSize = 5  // Ball x,y, velocity, wheel angle, wheel speed
    
    fun predict(
        ballPosition: Point,
        ballVelocity: Float,
        wheelAngle: Double,
        wheelSpeed: Double
    ): Pair<Int, Float> {
        val inputBuffer = createInputBuffer(
            ballPosition,
            ballVelocity,
            wheelAngle,
            wheelSpeed
        )

        val outputArray = FloatArray(OUTPUT_SIZE)
        val outputBuffer = ByteBuffer.allocateDirect(OUTPUT_SIZE * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer[outputArray]  // Indexed accessor statt get()

        // Finde Maximum mit indexbasiertem Zugriff
        var maxProb = outputArray[0]
        var predictedNumber = 0
        
        for (i in 1..MAX_NUMBER) {
            if (outputArray[i] > maxProb) {
                maxProb = outputArray[i]
                predictedNumber = i
            }
        }

        return Pair(predictedNumber, maxProb * CONFIDENCE_SCALE)
    }

    private fun createInputBuffer(
        ballPosition: Point,
        ballVelocity: Float,
        wheelAngle: Double,
        wheelSpeed: Double
    ): FloatBuffer {
        return ByteBuffer.allocateDirect(inputSize * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(0, ballPosition.x.toFloat())
                put(1, ballPosition.y.toFloat())
                put(2, ballVelocity)
                put(3, wheelAngle.toFloat())
                put(4, wheelSpeed.toFloat())
            }
    }

    fun release() {
        try {
            interpreter.close()
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Schließen des Interpreters")
        }
    }

    companion object {
        private const val FLOAT_BYTES = 4
        private const val OUTPUT_SIZE = 38  // 37 numbers + 1 confidence
        private const val MAX_NUMBER = 36
        private const val CONFIDENCE_SCALE = 100f

        fun create(modelFile: File): MLModel? {
            return try {
                val interpreter = Interpreter(modelFile)
                MLModel(interpreter)
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Erstellen des MLModels")
                null
            }
        }
    }
} 