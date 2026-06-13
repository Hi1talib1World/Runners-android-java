package com.denzo.runners.features.home

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.denzo.runners.R
import com.denzo.runners.databinding.ActivityMainBinding
import com.denzo.runners.features.settings.SettingsRepository
import com.denzo.runners.features.subscription.BillingManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "runners_prefs"
        const val PREF_DARK_THEME = "dark_theme"
    }

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pillar 1: Hydrate the initial state directly from persistent cache
        // to eliminate visual flickering.
        lifecycleScope.launch {
            val initialConfig = settingsRepository.settingsFlow.first()
            applyTheme(initialConfig.isDarkMode)
            
            // Observe SSOT for global application of state changes
            settingsRepository.settingsFlow.collect { config ->
                applyTheme(config.isDarkMode)
            }
        }

        // Load osmdroid configuration for offline caching
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        billingManager.startConnection()

        setupNavigation()
    }

    private fun applyTheme(isDarkMode: Boolean) {
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, 
                R.id.my_journey, 
                R.id.nav_star, 
                R.id.navigation_profile,
                R.id.navigation_settings
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}
