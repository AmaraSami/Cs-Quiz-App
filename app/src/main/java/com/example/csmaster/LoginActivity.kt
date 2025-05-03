package com.example.csmaster

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.csmaster.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences
    private val db = FirebaseFirestore.getInstance()
    private lateinit var internetReceiver: InternetConnectivityReceiver


    override fun onResume() {
        super.onResume()
        internetReceiver = InternetConnectivityReceiver()
        registerReceiver(
            internetReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(internetReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // ─── If they didn’t check “Remember Me” last time, clear any saved session ───
        val wasRemembered = prefs.getBoolean("rememberMe", false)
        if (!wasRemembered) {
            auth.signOut()                            // clear Firebase’s cached user
            prefs.edit().clear().apply()              // clear saved flags & email & role
        }

        // ─── Auto‑login if they _did_ check “Remember Me” and Firebase still has a user ───
        val savedRemember = prefs.getBoolean("rememberMe", false)
        val savedUid      = auth.currentUser?.uid
        if (savedRemember && savedUid != null) {
            fetchRoleAndRedirect(savedUid)
            return
        }

        // ——— Normal login flow below ———
        binding.loginButton.setOnClickListener {
            val emailInput = binding.emailEditText.text.toString().trim()
            val password   = binding.passwordEditText.text.toString().trim()
            if (emailInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailInput, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser!!.uid
                    // Save "remember me" choice
                    prefs.edit().putBoolean("rememberMe", binding.rememberMeCheckBox.isChecked)
                        .apply()
                    // Now fetch the user’s role and send them off
                    fetchRoleAndRedirect(uid)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        binding.registerRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.forgotPasswordText.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim().takeIf { it.isNotEmpty() }
            if (email == null) {
                Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { Toast.makeText(this, "Reset link sent", Toast.LENGTH_LONG).show() }
                    .addOnFailureListener { Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun fetchRoleAndRedirect(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val isAdmin = doc.getBoolean("isAdmin") == true
                prefs.edit().putString("role", if (isAdmin) "admin" else "user").apply()

                val dest = if (isAdmin) AdminDashboardActivity::class.java
                else MainActivity::class.java
                startActivity(Intent(this, dest))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to get role: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
