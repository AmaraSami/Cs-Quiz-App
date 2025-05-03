package com.example.csmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.csmaster.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.registerButton.setOnClickListener {
            val email       = binding.emailEditText.text.toString().trim()
            val password    = binding.passwordEditText.text.toString().trim()
            val confirmPass = binding.confirmPasswordEditText.text.toString().trim()
            val isAdmin     = binding.adminRadioButton.isChecked

            // Basic validation
            if (email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create the Auth user
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

                    // Optionally send verification email
                    auth.currentUser?.sendEmailVerification()

                    // Write Firestore document with isAdmin flag
                    val userData = mapOf(
                        "email"   to email,
                        "isAdmin" to isAdmin,
                        "approved" to !isAdmin
                    )
                    db.collection("users")
                        .document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                if (isAdmin)
                                    "Admin registration submitted—awaiting approval."
                                else
                                    "Registered successfully! Please log in.",
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Registration failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        binding.loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
