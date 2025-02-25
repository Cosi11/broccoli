package com.roulette.tracker.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import com.roulette.tracker.databinding.FragmentStatsBinding
import com.roulette.tracker.viewBinding

@AndroidEntryPoint
class StatsFragment : Fragment() {
    private val viewModel: StatsViewModel by viewModels()
    private val binding by viewBinding(FragmentStatsBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentStatsBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel.allResults.observe(viewLifecycleOwner) { results ->
            with(binding) {
                // Statistiken aktualisieren
                totalPredictionsText.text = getString(R.string.total_predictions, results.size)
                val successful = results.count { it.wasSuccessful }
                successfulPredictionsText.text = getString(R.string.successful_predictions, successful)
                
                val successRate = if (results.isNotEmpty()) {
                    (successful.toFloat() / results.size) * 100
                } else 0f
                successRateText.text = getString(R.string.success_rate, successRate)
                
                val avgConfidence = results.map { it.confidence }.average().toInt()
                averageConfidenceText.text = getString(R.string.average_confidence, avgConfidence)
                
                val avgTimeToLanding = results.map { it.timeToLanding }.average()
                averageTimeToLandingText.text = getString(
                    R.string.average_time_to_landing, 
                    avgTimeToLanding
                )

                // Charts aktualisieren
                updateConfidenceChart(results)
                updateTimeToLandingChart(results)
            }
        }
        
        viewModel.accuracyRate.observe(viewLifecycleOwner) { rate ->
            binding.trendText.text = getString(R.string.trend, rate)
        }
    }

    private fun updateConfidenceChart(results: List<PredictionResult>) {
        binding.confidenceChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            val entries = results.mapIndexed { index, result ->
                Entry(index.toFloat(), result.confidence)
            }
            
            val dataSet = LineDataSet(entries, getString(R.string.chart_confidence)).apply {
                setDrawValues(false)
                setDrawCircles(false)
                lineWidth = 2f
                color = ContextCompat.getColor(requireContext(), R.color.chart_line)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            
            data = LineData(dataSet)
            invalidate()
        }
    }

    private fun updateTimeToLandingChart(results: List<PredictionResult>) {
        binding.timeToLandingChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            val entries = results.mapIndexed { index, result ->
                Entry(index.toFloat(), result.timeToLanding)
            }
            
            val dataSet = LineDataSet(entries, getString(R.string.chart_time_to_landing)).apply {
                setDrawValues(false)
                setDrawCircles(false)
                lineWidth = 2f
                color = ContextCompat.getColor(requireContext(), R.color.chart_line)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            
            data = LineData(dataSet)
            invalidate()
        }
    }
} 