package com.denzo.runners.features.activities

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.denzo.runners.R
import com.denzo.runners.core.utils.UnitConverter
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.databinding.FragmentRunSummaryBinding
import com.google.android.material.snackbar.Snackbar
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

@AndroidEntryPoint
class RunSummaryFragment : Fragment(R.layout.fragment_run_summary) {

    private var _binding: FragmentRunSummaryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RunSummaryViewModel by viewModels()
    private var currentRun: RunEntity? = null
    private var isMetricPref: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRunSummaryBinding.bind(view)

        val runId = arguments?.getInt("runId") ?: -1

        setupInteractions()
        setupMap()
        observeUiState()
        viewModel.loadRun(runId)
    }

    private fun setupInteractions() {
        binding.buttonBack.setOnClickListener { findNavController().popBackStack() }
        binding.buttonShare.setOnClickListener { shareRun() }
        
        binding.buttonSaveRoute.setOnClickListener {
            // Branded dialog for route naming would go here
            viewModel.saveAsRoute("Saved Route ${System.currentTimeMillis()}")
        }

        binding.chipGroupMetrics.setOnCheckedStateChangeListener { _, checkedIds ->
            currentRun?.let { run ->
                when (checkedIds.firstOrNull()) {
                    R.id.chip_hr -> updateChart(run, "HR")
                    R.id.chip_cadence -> updateChart(run, "CAD")
                    else -> updateChart(run, "PACE")
                }
            }
        }
    }

    private fun setupMap() {
        binding.mapSummary.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapSummary.setMultiTouchControls(true)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.run?.let { displayRun(it, state.isMetric) }
                    
                    state.successMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }

    private fun displayRun(run: RunEntity, isMetric: Boolean) {
        currentRun = run
        isMetricPref = isMetric
        
        binding.textDistanceValue.text = UnitConverter.formatDistance(run.distanceMeters, isMetric)
        binding.textDurationValue.text = formatTime(run.durationSeconds.toInt())
        binding.textPaceValue.text = UnitConverter.formatPace(run.avgPace, isMetric).split(" ")[0]
        binding.textCaloriesValue.text = String.format(Locale.getDefault(), "%.0f kcal", run.caloriesBurned)

        if (run.pathPoints.isNotEmpty()) {
            binding.mapSummary.overlays.clear()
            val polyline = Polyline(binding.mapSummary).apply {
                setPoints(run.pathPoints)
                outlinePaint.color = android.graphics.Color.RED
                outlinePaint.strokeWidth = 10f
            }
            binding.mapSummary.overlays.add(polyline)
            binding.mapSummary.controller.setZoom(17.0)
            binding.mapSummary.controller.setCenter(run.pathPoints.first())
            binding.mapSummary.invalidate()
        }

        updateChart(run, "PACE")
        setupZoneChart(run)
    }

    private fun setupZoneChart(run: RunEntity) {
        val breakdown = if (run.zoneBreakdown.isNotEmpty()) run.zoneBreakdown else listOf(120L, 450L, 800L, 300L, 50L)
        val entries = breakdown.mapIndexed { index, seconds ->
            BarEntry(index.toFloat(), (seconds / 60).toFloat())
        }

        val dataSet = BarDataSet(entries, "Minutes per Zone").apply {
            colors = listOf(
                android.graphics.Color.GRAY,
                android.graphics.Color.BLUE,
                android.graphics.Color.GREEN,
                android.graphics.Color.YELLOW,
                android.graphics.Color.RED
            )
            setDrawValues(true)
            valueTextColor = android.graphics.Color.WHITE
            valueTextSize = 10f
        }

        binding.zoneChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(listOf("Z1", "Z2", "Z3", "Z4", "Z5"))
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                textColor = android.graphics.Color.WHITE
                setDrawGridLines(false)
            }
            axisLeft.apply {
                textColor = android.graphics.Color.WHITE
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            invalidate()
        }
    }

    private fun updateChart(run: RunEntity, type: String) {
        val entries = mutableListOf<Entry>()
        val label: String
        val color: Int
        
        when (type) {
            "HR" -> {
                label = "Heart Rate (BPM)"
                color = android.graphics.Color.RED
                // Use stored points or simulate based on avg
                val points = if (run.heartRatePoints.isNotEmpty()) run.heartRatePoints else simulatePoints(run.avgHeartRate, 10)
                points.forEachIndexed { i, p -> entries.add(Entry(i.toFloat(), p.toFloat())) }
            }
            "CAD" -> {
                label = "Cadence (RPM)"
                color = android.graphics.Color.CYAN
                val points = if (run.cadencePoints.isNotEmpty()) run.cadencePoints else simulatePoints(run.cadence, 10)
                points.forEachIndexed { i, p -> entries.add(Entry(i.toFloat(), p.toFloat())) }
            }
            else -> {
                label = if (isMetricPref) "Pace (min/km)" else "Pace (min/mi)"
                color = android.graphics.Color.parseColor("#C6FF00")
                val basePace = if (isMetricPref) run.avgPace else run.avgPace * (1609.34 / 1000.0)
                val points = simulatePoints(basePace.toInt(), 10)
                points.forEachIndexed { i, p -> entries.add(Entry(i.toFloat(), p.toFloat())) }
            }
        }

        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            setCircleColor(color)
            lineWidth = 2f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 50
        }

        binding.paceChart.data = LineData(dataSet)
        binding.paceChart.description.isEnabled = false
        binding.paceChart.xAxis.textColor = android.graphics.Color.WHITE
        binding.paceChart.axisLeft.textColor = android.graphics.Color.WHITE
        binding.paceChart.axisRight.isEnabled = false
        binding.paceChart.legend.textColor = android.graphics.Color.WHITE
        binding.paceChart.invalidate()
    }

    private fun simulatePoints(base: Int, count: Int): List<Int> {
        return (0 until count).map { (base + (Math.random() * 10 - 5)).toInt() }
    }

    private fun shareRun() {
        val run = currentRun ?: return
        val shareMessage = """
            🏃 New Run Logged!
            📏 Distance: ${UnitConverter.formatDistance(run.distanceMeters, isMetricPref)}
            ⏱️ Time: ${formatTime(run.durationSeconds.toInt())}
            ⚡ Pace: ${UnitConverter.formatPace(run.avgPace, isMetricPref)}
            🔥 Calories: ${String.format(Locale.getDefault(), "%.0f kcal", run.caloriesBurned)}
            
            Check it out on Runners! #RunnersApp #Running
        """.trimIndent()

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
        }
        startActivity(android.content.Intent.createChooser(intent, "Share your run"))
    }

    private fun formatTime(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapSummary.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapSummary.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
