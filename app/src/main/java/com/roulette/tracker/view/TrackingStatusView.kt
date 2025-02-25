package com.roulette.tracker.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.roulette.tracker.R
import com.roulette.tracker.data.PredictionResult
import com.roulette.tracker.data.TrackingState
import com.roulette.tracker.databinding.TrackingStatusViewBinding

class TrackingStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var _binding: TrackingStatusViewBinding? = null
    private val binding get() = _binding!!

    init {
        _binding = TrackingStatusViewBinding.inflate(
            LayoutInflater.from(context),
            this  // parent
        ).also { binding ->
            addView(binding.root)
        }
    }

    fun updateState(state: TrackingState) {
        when (state) {
            is TrackingState.Initializing -> showInitializing()
            is TrackingState.Running -> showRunning()
            is TrackingState.Error -> showError(state.message)
            is TrackingState.BallNotFound -> showBallNotFound()
            is TrackingState.WheelNotFound -> showWheelNotFound()
            is TrackingState.Predicting -> showPredicting(state.timeRemaining)
            is TrackingState.Success -> showSuccess(state.prediction)
        }
    }

    private fun showInitializing() {
        binding.apply {
            statusText.text = context.getString(R.string.status_initializing)
            statusProgress.visibility = View.VISIBLE
            statusProgress.isIndeterminate = true
        }
    }

    private fun showRunning() {
        binding.apply {
            statusText.text = context.getString(R.string.status_running)
            statusProgress.visibility = View.VISIBLE
            statusProgress.isIndeterminate = false
            statusProgress.progress = 100
        }
    }

    private fun showError(message: String) {
        binding.apply {
            statusText.text = context.getString(R.string.status_error, message)
            statusProgress.visibility = View.GONE
        }
    }

    private fun showBallNotFound() {
        binding.apply {
            statusText.text = context.getString(R.string.status_ball_lost)
            statusProgress.visibility = View.GONE
        }
    }

    private fun showWheelNotFound() {
        binding.apply {
            statusText.text = context.getString(R.string.status_wheel_lost)
            statusProgress.visibility = View.GONE
        }
    }

    private fun showPredicting(timeRemaining: Long) {
        binding.apply {
            statusText.text = context.getString(
                R.string.status_predicting,
                timeRemaining / 1000
            )
            statusProgress.visibility = View.VISIBLE
            statusProgress.isIndeterminate = false
            statusProgress.progress = ((timeRemaining.toFloat() / 5000) * 100).toInt()
        }
    }

    private fun showSuccess(prediction: PredictionResult) {
        binding.apply {
            statusText.text = context.getString(
                R.string.status_prediction,
                prediction.predictedNumber,
                prediction.confidence * 100
            )
            statusProgress.visibility = View.GONE
        }
    }
} 