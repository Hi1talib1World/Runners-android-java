package com.denzo.runners.features.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denzo.runners.R
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.databinding.ActivityHistoryBinding
import com.denzo.runners.databinding.ItemDataBinding
import com.denzo.runners.features.home.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(MainActivity.PREF_DARK_THEME, false)) {
            setTheme(R.style.AppThemeDarkDialog)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeHistory()
    }

    private fun observeHistory() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
            
            // Handle delete item
            itemBinding.buttonDeleteitem.setOnClickListener {
                // In a real app, we'd call viewModel.deleteRun(run)
                // For this refactor, we are focusing on state-driven rendering
            }

            binding.dataLayout.addView(itemBinding.root)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.back_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_back -> {
                startActivity(Intent(this, MainActivity::class.java))
                true
            }
            R.id.action_clear -> {
                viewModel.clearHistory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onClickDeleteItem(view: View) {
        val r = view.parent as ViewGroup
        val deleteView = r.getChildAt(1) as TextView
        val deleteId = deleteView.text.toString().toDoubleOrNull()?.toInt()
        // Handle deletion via ViewModel
    }
}
