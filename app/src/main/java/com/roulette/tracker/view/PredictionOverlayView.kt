package com.roulette.tracker.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.roulette.tracker.data.PredictionResult
import com.roulette.tracker.databinding.PredictionOverlayBinding

class PredictionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var _binding: PredictionOverlayBinding? = null
    private val binding get() = _binding!!

    private var landingPosition: Point? = null
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 5f
    }

    init {
        _binding = PredictionOverlayBinding.inflate(
            LayoutInflater.from(context),
            this  // parent
        ).also { binding ->
            addView(binding.root)
        }
    }

    fun updatePrediction(prediction: PredictionResult) {
        binding.predictionNumberText.text = "Vorhersage: ${prediction.predictedNumber}"
        binding.confidenceBar.progress = (prediction.confidence * 100).toInt()
    }

    fun updateDebugInfo(ballVelocity: Double, wheelSpeed: Double) {
        binding.debugInfoText.text = """
            Ball: %.2f px/s
            Rad: %.2f °/s
        """.trimIndent().format(ballVelocity, wheelSpeed)
    }

    fun updateLandingPosition(position: Point) {
        landingPosition = position
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        landingPosition?.let { pos ->
            // Zeichne einen roten Punkt an der Landeposition
            canvas.drawCircle(pos.x.toFloat(), pos.y.toFloat(), 10f, paint)
        }
    }
} 