package com.example.csmaster

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.csmaster.databinding.ActivityAdminEditQuizBinding
import com.google.firebase.firestore.FirebaseFirestore

class AdminEditQuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminEditQuizBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var quizId: String
    private lateinit var questionList: ArrayList<QuestionModel>
    private lateinit var questionAdapter: QuestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminEditQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // Get quizId from Intent
        quizId = intent.getStringExtra("quizId") ?: return

        // Initialize the question list and adapter
        questionList = ArrayList()
        questionAdapter = QuestionAdapter(questionList)

        // Setup RecyclerView
        binding.questionRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.questionRecyclerView.adapter = questionAdapter

        // Load quiz data
        loadQuizData()

        // Add new question button click listener
        binding.addQuestionButton.setOnClickListener {
            val newQuestion = QuestionModel("", listOf("") ,"" ) // Add default values
            questionList.add(newQuestion)
            questionAdapter.notifyItemInserted(questionList.size - 1)
        }

        // Save button click listener
        binding.saveButton.setOnClickListener {
            saveQuizData()
        }
    }

    private fun loadQuizData() {
        db.collection("quizzes").document(quizId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val title = document.getString("title") ?: ""
                    val subtitle = document.getString("subtitle") ?: ""
                    val time = document.getLong("time") ?: 0L

                    // Populate the title, subtitle, and time fields
                    binding.titleEditText.setText(title)
                    binding.subtitleEditText.setText(subtitle)
                    binding.timeEditText.setText(time.toString())

                    // Load the questions
                    val questions = document.get("questions") as? List<Map<String, Any>> ?: emptyList()

                    questionList.clear()
                    for (question in questions) {
                        val questionText = question["question"] as? String ?: ""
                        val correctAnswer = question["correctAnswer"] as? String ?: ""
                        val options = question["options"] as? List<String> ?: listOf()

                        questionList.add(QuestionModel(questionText, options,correctAnswer))
                    }
                    questionAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Quiz not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading quiz data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveQuizData() {
        val title = binding.titleEditText.text.toString().trim()
        val subtitle = binding.subtitleEditText.text.toString().trim()
        val time = binding.timeEditText.text.toString().toLongOrNull() ?: 0L

        val updatedQuiz = hashMapOf(
            "title" to title,
            "subtitle" to subtitle,
            "time" to time.toString(),
            "questions" to questionList.map { question ->
                mapOf(
                    "question" to question.question,
                    "correctAnswer" to question.correct,
                    "options" to question.options
                )
            }
        )

        db.collection("quizzes").document(quizId).set(updatedQuiz)
            .addOnSuccessListener {
                Toast.makeText(this, "Quiz updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error saving quiz data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
