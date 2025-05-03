package com.example.csmaster

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.csmaster.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences
    private val db = FirebaseFirestore.getInstance()
    private val quizList = mutableListOf<QuizModel>()
    private lateinit var adapter: AdminQuizAdapter
    private lateinit var internetReceiver: InternetConnectivityReceiver

    override fun onResume() {
        super.onResume()
        internetReceiver = InternetConnectivityReceiver()
        registerReceiver(
            internetReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    // 3) In onPause, unregister exactly that same receiver
    override fun onPause() {
        super.onPause()
        unregisterReceiver(internetReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Only allow admins here
        val role = prefs.getString("role", null)
        if (auth.currentUser == null || role != "admin") {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        adapter = AdminQuizAdapter(quizList) { quiz ->
            val i = Intent(this, EditQuizActivity::class.java).apply {
                putExtra("quizId", quiz.id)
                putExtra("title", quiz.title)
                putExtra("subtitle", quiz.subtitle)
                putExtra("time", quiz.time)
            }
            startActivity(i)
        }
        binding.adminQuizRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.adminQuizRecyclerView.adapter = adapter

        fetchQuizzes()

        binding.addQuizFab.setOnClickListener {
            startActivity(Intent(this, EditQuizActivity::class.java))
        }
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun fetchQuizzes() {
        db.collection("quizzes")
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    Toast.makeText(this, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                quizList.clear()
                snaps?.forEach { doc ->
                    quizList.add(doc.toObject(QuizModel::class.java).copy(id = doc.id))
                }
                adapter.notifyDataSetChanged()
            }
    }
}
