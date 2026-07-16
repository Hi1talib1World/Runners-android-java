package com.denzo.runners.features.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.github.mikephil.charting.data.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

@AndroidEntryPoint
class RunSummaryFragment : Fragment() {

    private var _binding: FragmentRunSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RunSummaryViewModel by viewModels()
    private var currentRun: RunEntity? = null
    private var isMetricPref: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRunSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        setupMap()
        observeUiState()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.btnShare.setOnClickListener {
            if (currentRun != null) {
                shareRun()
            } else {
                Toast.makeText(requireContext(), "Activity data loading...", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Activity")
                .setMessage("This action cannot be undone. Permanent deletion from cloud and local storage.")
                .setPositiveButton("DELETE PERMANENTLY") { _, _ ->
                    currentRun?.let { viewModel.deleteRun(it) }
                    findNavController().popBackStack()
                }
                .setNegativeButton("KEEP", null)
                .show()
        }

        binding.chipGroupMetrics.setOnCheckedStateChangeListener { _, checkedIds ->
            val metric = when (checkedIds.firstOrNull()) {
                R.id.chip_hr -> "HR"
                R.id.chip_cadence -> "CADENCE"
                else -> "PACE"
            }
            currentRun?.let { updateChart(it, metric) }
        }
    }

    private fun setupMap() {
        binding.mapSummary.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapSummary.setMultiTouchControls(false)
        binding.mapSummary.controller.setZoom(17.0)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.run != null) {
                        displayRun(state.run, state.isMetric)
                    } else if (!state.isLoading) {
                        Toast.makeText(requireContext(), "Error: Activity not found", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
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

        if (run.temperature != null) { 
            binding.weatherBadge.visibility = View.VISIBLE
            binding.tvWeatherTemp.text = String.format(Locale.getDefault(), "%.0f°C", run.temperature)
            binding.tvEnvironmentLabel.text = run.environment ?: "ROAD"
        } else {
            binding.weatherBadge.visibility = View.GONE
        }

        if (run.pathPoints.isNotEmpty()) {
            val polyline = Polyline(binding.mapSummary)
            polyline.outlinePaint.color = Color.RED
            polyline.outlinePaint.strokeWidth = 10f
            polyline.setPoints(run.pathPoints)
            binding.mapSummary.overlays.add(polyline)
            binding.mapSummary.controller.setCenter(run.pathPoints.last())
        }

        updateChart(run, "PACE")
        setupZoneChart(run)
    }

    private fun setupZoneChart(run: RunEntity) {
        val entries = mutableListOf<BarEntry>()
        
        if (run.zoneBreakdown.isNotEmpty()) {
            run.zoneBreakdown.forEachIndexed { index, seconds ->
                entries.add(BarEntry(index.toFloat(), (seconds / 60).toFloat()))
            }
        } else {
            for (i in 0..4) entries.add(BarEntry(i.toFloat(), 0f))
        }

        val dataSet = BarDataSet(entries, "Minutes in Zone")
        dataSet.colors = listOf(Color.GRAY, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED)
        dataSet.valueTextColor = Color.WHITE
        
        binding.zoneChart.data = BarData(dataSet)
        binding.zoneChart.description.isEnabled = false
        binding.zoneChart.xAxis.textColor = Color.WHITE
        binding.zoneChart.axisLeft.textColor = Color.WHITE
        binding.zoneChart.axisRight.isEnabled = false
        binding.zoneChart.legend.isEnabled = false
        binding.zoneChart.invalidate()
    }

    private fun updateChart(run: RunEntity, metric: String) {
        val entries = mutableListOf<Entry>()
        val points = when(metric) {
            "HR" -> if (run.heartRatePoints.isNotEmpty()) run.heartRatePoints else simulatePoints(140, 180)
            "CADENCE" -> if (run.cadencePoints.isNotEmpty()) run.cadencePoints else simulatePoints(160, 190)
            else -> simulatePoints(4, 7)
        }

        points.forEachIndexed { index, value ->
            entries.add(Entry(index.toFloat(), value.toFloat()))
        }

        val dataSet = LineDataSet(entries, metric)
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.runners_volt)
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2f
        dataSet.setDrawFilled(true)
        
        val fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.fade_volt)
        if (fillDrawable != null) {
            dataSet.fillDrawable = fillDrawable
        } else {
            dataSet.fillColor = Color.GREEN
        }

        binding.summaryChart.data = LineData(dataSet)
        binding.summaryChart.description.isEnabled = false
        binding.summaryChart.xAxis.textColor = Color.WHITE
        binding.summaryChart.axisLeft.textColor = Color.WHITE
        binding.summaryChart.axisRight.isEnabled = false
        binding.summaryChart.invalidate()
    }

    private fun simulatePoints(min: Int, max: Int): List<Int> {
        return (1..50).map { (min..max).random() }
    }

    private fun shareRun() {
        val run = currentRun ?: return
        val dist = UnitConverter.formatDistance(run.distanceMeters, isMetricPref)
        val pace = UnitConverter.formatPace(run.avgPace, isMetricPref)
        
        val shareText = if (run.distanceMeters > 10000) {
            "Big session! Just crushed $dist at $pace pace with Runners. #Performance"
        } else {
            "Just finished a $dist run at $pace pace! #RunnersApp"
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share your activity"))
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s) 
               else String.format(Locale.getDefault(), "%02d:%02d", m, s)
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
