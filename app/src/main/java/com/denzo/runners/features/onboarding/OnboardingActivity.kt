package com.denzo.runners.features.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.denzo.runners.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, OnboardingFragment())
                .commit()
        }
    }
}
