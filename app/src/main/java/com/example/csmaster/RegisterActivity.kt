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
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
            val role = if (binding.adminRadioButton.isChecked) "admin" else "user"

            if (email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword) {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val currentUser = firebaseAuth.currentUser

                            currentUser?.sendEmailVerification()
                                ?.addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this, "Email verification failed: ${it.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }

                            // Save user info in Firestore
                            val uid = currentUser?.uid
                            val userData = hashMapOf(
                                "email" to email,
                                "isAdmin" to (role == "admin"),
                                "approved" to (role != "admin") // admins need manual approval
                            )

                            uid?.let {
                                firestore.collection("users").document(it)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Error saving user role: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Fill all fields and make sure passwords match", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
