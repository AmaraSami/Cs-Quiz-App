package com.example.csmaster

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.csmaster.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences
    private val db = FirebaseFirestore.getInstance()
    private val quizModelList = mutableListOf<QuizModel>()

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Must be logged in AND have a role saved
        val role = prefs.getString("role", null)
        if (auth.currentUser == null || role.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // If somehow an admin hits this, bump them to their dashboard
        if (role == "admin") {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
            return
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = QuizListAdapter(quizModelList)

        fetchQuizzes()

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Override back button to prevent returning to login page
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Show a confirmation dialog instead of going back
            Toast.makeText(this, "Use the logout button to exit", Toast.LENGTH_SHORT).show()
            return true // Consume the back button event
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun fetchQuizzes() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("quizzes")
            .addSnapshotListener { snaps, err ->
                binding.progressBar.visibility = View.GONE
                if (err != null) {
                    Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                quizModelList.clear()
                snaps?.forEach { doc ->
                    quizModelList.add(doc.toObject(QuizModel::class.java).copy(id = doc.id))
                }
                binding.recyclerView.adapter?.notifyDataSetChanged()
            }
    }
}