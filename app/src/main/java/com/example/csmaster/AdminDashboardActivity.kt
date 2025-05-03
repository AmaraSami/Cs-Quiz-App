package com.example.csmaster

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.csmaster.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val db = FirebaseFirestore.getInstance()
    private val quizList = mutableListOf<QuizModel>()
    private lateinit var adapter: AdminQuizAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        setupRecyclerView()
        fetchQuizzes()

        binding.addQuizFab.setOnClickListener {
            startActivity(Intent(this, EditQuizActivity::class.java))
        }

        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            sharedPreferences.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminQuizAdapter(quizList) { quiz ->
            val intent = Intent(this, EditQuizActivity::class.java).apply {
                putExtra("quizId", quiz.id)
                putExtra("title", quiz.title)
                putExtra("subtitle", quiz.subtitle)
                putExtra("time", quiz.time)
            }
            startActivity(intent)
        }
        binding.adminQuizRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.adminQuizRecyclerView.adapter = adapter
    }

    private fun fetchQuizzes() {
        db.collection("quizzes")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    quizList.clear()
                    for (doc in snapshots) {
                        val quiz = doc.toObject(QuizModel::class.java).copy(id = doc.id)
                        quizList.add(quiz)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        fetchQuizzes()
    }
}
