package com.denzo.runners

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.denzo.runners.features.auth.LoginActivity
import com.denzo.runners.features.home.MainActivity
import com.denzo.runners.features.onboarding.OnboardingActivity
import com.denzo.runners.features.onboarding.OnboardingRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Pillar 1: On-Launch Interception
 * Reads the first-run flag on boot to route the user.
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var onboardingRepository: OnboardingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            val destination = when {
                !onboardingRepository.isFirstRunCompleted() -> OnboardingActivity::class.java
                currentUser == null -> LoginActivity::class.java
                else -> MainActivity::class.java
            }
            
            startActivity(Intent(this, destination))
            finish()
        }, 2000)
    }
}
