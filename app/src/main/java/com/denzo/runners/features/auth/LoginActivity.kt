package com.denzo.runners.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.denzo.runners.R
import com.denzo.runners.databinding.ActivityLoginBinding
import com.denzo.runners.features.home.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            updateUi(isLoading = false, error = "Google sign in failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            loginWithEmail()
        }

        binding.googleButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.signupLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun loginWithEmail() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showSnackbar("Please fill all fields", isError = true)
            return
        }

        updateUi(isLoading = true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    updateUi(isLoading = false, error = task.exception?.message ?: "Login Failed")
                }
            }
    }

    private fun signInWithGoogle() {
        updateUi(isLoading = true)
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    updateUi(isLoading = false, error = "Authentication Failed")
                }
            }
    }

    private fun navigateToMain() {
        lifecycleScope.launch {
            delay(500)
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun updateUi(isLoading: Boolean, error: String? = null) {
        binding.loginButton.isEnabled = !isLoading
        binding.googleButton.isEnabled = !isLoading
        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        error?.let {
            showSnackbar(it, isError = true)
        }
    }

    private fun showSnackbar(message: String, isError: Boolean) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
            if (isError) {
                setBackgroundTint(ContextCompat.getColor(this@LoginActivity, R.color.runners_accent_red))
                setTextColor(ContextCompat.getColor(this@LoginActivity, android.R.color.white))
            }
        }.show()
    }
}
