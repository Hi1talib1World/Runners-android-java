package com.denzo.runners.features.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.denzo.runners.data.local.dao.RunningDAO
import com.denzo.runners.databinding.ActivityHistoryBinding
import com.denzo.runners.databinding.ItemDataBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: ActivityHistoryBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var runningDao: RunningDAO

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHistory()
    }

    private fun loadHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                runningDao.getAllRuningdata()
            }
            
            binding.dataLayout.removeAllViews()
            data.forEach { run ->
                val itemBinding = ItemDataBinding.inflate(layoutInflater, binding.dataLayout, false)
                itemBinding.dataId.text = run.id.toString()
                itemBinding.dataDistance.text = String.format("%.2f km", run.distance)
                itemBinding.dataStarttime.text = run.starttime
                
                binding.dataLayout.addView(itemBinding.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
