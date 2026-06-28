package com.denzo.runners.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.denzo.runners.R
import com.denzo.runners.databinding.ActivitySignupBinding
import com.denzo.runners.features.home.MainActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.signupButton.setOnClickListener {
            createAccount()
        }

        binding.loginLink.setOnClickListener {
            finish()
        }
    }

    private fun createAccount() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showSnackbar("Please fill all fields", isError = true)
            return
        }

        if (password != confirmPassword) {
            showSnackbar("Passwords do not match", isError = true)
            return
        }

        if (password.length < 6) {
            showSnackbar("Password should be at least 6 characters", isError = true)
            return
        }

        updateUi(isLoading = true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    updateUi(isLoading = false, error = task.exception?.message ?: "Account Creation Failed")
                }
            }
    }

    private fun navigateToMain() {
        lifecycleScope.launch {
            delay(500)
            startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
            finishAffinity() // Clear activity stack
        }
    }

    private fun updateUi(isLoading: Boolean, error: String? = null) {
        binding.signupButton.isEnabled = !isLoading
        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        error?.let {
            showSnackbar(it, isError = true)
        }
    }

    private fun showSnackbar(message: String, isError: Boolean) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
            if (isError) {
                setBackgroundTint(ContextCompat.getColor(this@SignUpActivity, R.color.runners_accent_red))
                setTextColor(ContextCompat.getColor(this@SignUpActivity, android.R.color.white))
            }
        }.show()
    }
}
