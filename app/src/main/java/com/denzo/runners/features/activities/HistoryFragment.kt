package com.denzo.runners.features.activities

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denzo.runners.R
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.databinding.ActivityHistoryBinding
import com.denzo.runners.databinding.ItemDataBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.activity_history) {

    private var _binding: ActivityHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityHistoryBinding.bind(view)

        observeHistory()
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyState.collect { runs ->
                    renderHistory(runs)
                }
            }
        }
    }

    private fun renderHistory(runs: List<RunEntity>) {
        binding.dataLayout.removeAllViews()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        runs.forEach { run ->
            val itemBinding = ItemDataBinding.inflate(layoutInflater, binding.dataLayout, false)
            itemBinding.dataId.text = run.id.toString()
            itemBinding.dataDistance.text = String.format("%.2f km", run.distanceMeters / 1000)
            itemBinding.dataStarttime.text = dateFormat.format(Date(run.timestamp))
            itemBinding.dataCalories.text = String.format("%.0f kcal", run.caloriesBurned)
            
            binding.dataLayout.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
