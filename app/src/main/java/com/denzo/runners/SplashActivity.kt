package com.denzo.runners

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.denzo.runners.data.repository.AuthRepository
import com.denzo.runners.features.auth.LoginActivity
import com.denzo.runners.features.home.MainActivity
import com.denzo.runners.features.onboarding.OnboardingActivity
import com.denzo.runners.features.onboarding.OnboardingRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Pillar 1: On-Launch Interception
 * Modern implementation using Android SplashScreen API.
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var onboardingRepository: OnboardingRepository

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash screen on-screen until we decide where to route.
        // In a more complex app, we'd use splashScreen.setKeepOnScreenCondition { ... }
        
        routeUser()
    }

    private fun routeUser() {
        // If/Else Logic: Debug mode bypass
        if (BuildConfig.DEBUG) {
            // In Debug mode, skip onboarding automatically
            if (!onboardingRepository.isFirstRunCompleted()) {
                onboardingRepository.setFirstRunCompleted()
            }
            
            // Route based on auth, but consider direct skip in LoginActivity
            val destination = if (authRepository.isUserLoggedIn) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
            return
        }

        val destination = when {
            !onboardingRepository.isFirstRunCompleted() -> OnboardingActivity::class.java
            !authRepository.isUserLoggedIn -> LoginActivity::class.java
            else -> MainActivity::class.java
        }
        
        startActivity(Intent(this, destination))
        finish()
    }
}
