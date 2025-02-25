package com.roulette.tracker.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.roulette.tracker.databinding.ActivityStatisticsBinding
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color
import java.text.DecimalFormat
import com.google.android.material.snackbar.Snackbar
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.view.View

@AndroidEntryPoint
class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private val viewModel: StatisticsViewModel by viewModels()
    private val decimalFormat = DecimalFormat("#.##")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCharts()
        setupSwipeRefresh()
        setupTimeRangeSpinner()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Statistiken"
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshStatistics()
        }
    }

    private fun setupCharts() {
        with(binding.accuracyChart) {
            setHardwareAccelerationEnabled(true)
            setDrawGridBackground(false)
            setDrawBorders(false)
            
            isAutoScaleMinMaxEnabled = true
            setViewPortOffsets(50f, 20f, 20f, 50f)
            
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                textSize = 12f
                formSize = 12f
            }
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawZeroLine(true)
            }
            
            axisRight.isEnabled = false
        }

        with(binding.predictionChart) {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 36f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun setupTimeRangeSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            StatisticsViewModel.TimeRange.values().map { 
                when(it) {
                    StatisticsViewModel.TimeRange.LAST_24H -> "Letzte 24 Stunden"
                    StatisticsViewModel.TimeRange.LAST_WEEK -> "Letzte Woche"
                    StatisticsViewModel.TimeRange.LAST_MONTH -> "Letzter Monat"
                    StatisticsViewModel.TimeRange.ALL -> "Alle Zeiten"
                }
            }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.timeRangeSpinner.apply {
            this.adapter = adapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.setTimeRange(StatisticsViewModel.TimeRange.values()[position])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Wenn nichts ausgewählt ist, Standard-Zeitraum verwenden
                    viewModel.setTimeRange(StatisticsViewModel.TimeRange.LAST_WEEK)
                    
                    // UI-Feedback
                    binding.swipeRefresh.isRefreshing = true
                    viewModel.refreshStatistics()
                }
            }
        }
    }

    private fun observeData() {
        viewModel.statisticsData.observe(this) { stats ->
            updateAccuracyChart(stats.accuracyData)
            updatePredictionChart(stats.predictionData)
            updateStatsSummary(stats)
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        viewModel.error.observe(this) { error ->
            error?.let { showError(it) }
        }
    }

    private fun showError(error: String) {
        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
            .setAction("Wiederholen") {
                viewModel.refreshStatistics()
            }
            .show()
    }

    private fun updateAccuracyChart(accuracyData: List<Float>) {
        val entries = accuracyData.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Genauigkeit").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        binding.accuracyChart.apply {
            data = LineData(dataSet)
            animateX(500)
            setVisibleXRangeMaximum(50f)
            moveViewToX(accuracyData.size.toFloat())
            invalidate()
        }
    }

    private fun updatePredictionChart(predictionData: List<Pair<Int, Int>>) {
        val predictedEntries = predictionData.mapIndexed { index, (predicted, _) ->
            Entry(index.toFloat(), predicted.toFloat())
        }
        val actualEntries = predictionData.mapIndexed { index, (_, actual) ->
            Entry(index.toFloat(), actual.toFloat())
        }

        val predictedDataSet = LineDataSet(predictedEntries, "Vorhergesagt").apply {
            color = Color.RED
            setCircleColor(Color.RED)
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
        }
        
        val actualDataSet = LineDataSet(actualEntries, "Tatsächlich").apply {
            color = Color.GREEN
            setCircleColor(Color.GREEN)
            setDrawValues(false)
            lineWidth = 2f
            circleRadius = 4f
        }

        binding.predictionChart.data = LineData(predictedDataSet, actualDataSet)
        binding.predictionChart.invalidate()

        binding.predictionChart.apply {
            animateX(1000)
            setVisibleXRangeMaximum(20f)
            moveViewToX(predictionData.size.toFloat())
        }
    }

    private fun updateStatsSummary(stats: StatisticsData) {
        binding.stats100.text = """
            Letzte 100 Vorhersagen:
            • Korrekte Vorhersagen: ${stats.last100Stats.correctPredictions}/${stats.last100Stats.totalPredictions}
            • Genauigkeit: ${decimalFormat.format(stats.last100Stats.accuracy)}%
            • Durchschnittlicher Fehler: ${decimalFormat.format(stats.last100Stats.averageError)}
        """.trimIndent()

        binding.stats1000.text = """
            Letzte 1000 Vorhersagen:
            • Korrekte Vorhersagen: ${stats.last1000Stats.correctPredictions}/${stats.last1000Stats.totalPredictions}
            • Genauigkeit: ${decimalFormat.format(stats.last1000Stats.accuracy)}%
            • Durchschnittlicher Fehler: ${decimalFormat.format(stats.last1000Stats.averageError)}
        """.trimIndent()

        // Erweiterte Statistiken
        binding.extendedStats.text = """
            Erweiterte Statistiken:
            • Häufigste Zahl: ${stats.extendedStats.mostCommonNumber}
            • Seltenste Zahl: ${stats.extendedStats.leastCommonNumber}
            • Längste korrekte Serie: ${stats.extendedStats.maxConsecutiveCorrect}
            • Gesamtanzahl Vorhersagen: ${stats.extendedStats.totalPredictions}
        """.trimIndent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 