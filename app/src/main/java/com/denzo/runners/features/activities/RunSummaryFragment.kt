package com.denzo.runners.features.activities

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.denzo.runners.R
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.data.repository.RunRepository
import com.denzo.runners.databinding.FragmentRunSummaryBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class RunSummaryFragment : Fragment(R.layout.fragment_run_summary) {

    @Inject
    lateinit var repository: RunRepository

    private var _binding: FragmentRunSummaryBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRunSummaryBinding.bind(view)

        val runId = arguments?.getInt("runId") ?: -1

        binding.buttonBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupMap()
        loadRunData(runId)
    }

    private fun setupMap() {
        binding.mapSummary.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapSummary.setMultiTouchControls(true)
    }

    private fun loadRunData(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val run = repository.getRunById(id)
            run?.let { displayRun(it) }
        }
    }

    private fun displayRun(run: RunEntity) {
        binding.textDistanceValue.text = String.format(Locale.getDefault(), "%.2f km", run.distanceMeters / 1000)
        binding.textDurationValue.text = formatTime(run.durationSeconds.toInt())
        binding.textPaceValue.text = String.format(Locale.getDefault(), "%.2f", run.avgPace)
        binding.textCaloriesValue.text = String.format(Locale.getDefault(), "%.0f kcal", run.caloriesBurned)

        if (run.pathPoints.isNotEmpty()) {
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

        setupChart(run)
    }

    private fun setupChart(run: RunEntity) {
        // Mocking pace points for the chart based on average pace
        // In a real app, we would store pace per km/min in the database
        val entries = mutableListOf<Entry>()
        for (i in 0 until 10) {
            val variation = (Math.random() * 0.5) - 0.25
            entries.add(Entry(i.toFloat(), (run.avgPace + variation).toFloat()))
        }

        val dataSet = LineDataSet(entries, "Pace (min/km)").apply {
            color = android.graphics.Color.parseColor("#C6FF00") // Volt
            setCircleColor(android.graphics.Color.parseColor("#C6FF00"))
            lineWidth = 2f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = android.graphics.Color.parseColor("#33C6FF00")
        }

        binding.paceChart.data = LineData(dataSet)
        binding.paceChart.description.isEnabled = false
        binding.paceChart.xAxis.textColor = android.graphics.Color.WHITE
        binding.paceChart.axisLeft.textColor = android.graphics.Color.WHITE
        binding.paceChart.axisRight.isEnabled = false
        binding.paceChart.legend.textColor = android.graphics.Color.WHITE
        binding.paceChart.invalidate()
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
