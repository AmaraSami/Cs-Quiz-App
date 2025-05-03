package com.example.csmaster

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.csmaster.databinding.ActivityEditQuizBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditQuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditQuizBinding
    private val db = FirebaseFirestore.getInstance()
    private var quizId: String? = null
    private lateinit var questionList: ArrayList<QuestionModel>
    private lateinit var questionAdapter: QuestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quizId = intent.getStringExtra("quizId")
        binding.quizTitleEditText.setText(intent.getStringExtra("title") ?: "")
        binding.quizSubtitleEditText.setText(intent.getStringExtra("subtitle") ?: "")
        binding.quizTimeEditText.setText(intent.getStringExtra("time") ?: "")

        questionList = ArrayList()
        questionAdapter = QuestionAdapter(questionList)

        binding.questionRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.questionRecyclerView.adapter = questionAdapter

        if (quizId != null) {
            loadQuizData()
        }

        binding.addQuestionButton.setOnClickListener {
            questionList.add(QuestionModel("", listOf("", "", "", ""), ""))
            questionAdapter.notifyItemInserted(questionList.size - 1)
        }

        binding.saveQuizButton.setOnClickListener {
            currentFocus?.clearFocus()
            saveQuiz()
        }
    }

    private fun loadQuizData() {
        db.collection("quizzes").document(quizId!!).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val questions = document["questionList"] as? List<Map<String, Any>> ?: emptyList()
                    questionList.clear()
                    for (q in questions) {
                        val question = q["question"] as? String ?: ""
                        val correct = q["correct"] as? String ?: ""
                        val options = q["options"] as? List<String> ?: listOf("", "", "", "")
                        questionList.add(QuestionModel(question, options, correct))
                    }
                    questionAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load quiz", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveQuiz() {
        val title = binding.quizTitleEditText.text.toString().trim()
        val subtitle = binding.quizSubtitleEditText.text.toString().trim()
        val time = binding.quizTimeEditText.text.toString().trim().toIntOrNull()

        if (title.isEmpty() || subtitle.isEmpty() || time == null || time <= 0) {
            Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val quizData = hashMapOf(
            "title" to title,
            "subtitle" to subtitle,
            "time" to time,
            "questionList" to questionList.map {
                mapOf(
                    "question" to it.question,
                    "options" to it.options,
                    "correct" to it.correct
                )
            }
        )

        if (quizId == null) {
            db.collection("quizzes")
                .add(quizData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Quiz created", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to create quiz", Toast.LENGTH_SHORT).show()
                }
        } else {
            db.collection("quizzes").document(quizId!!)
                .set(quizData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Quiz updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update quiz", Toast.LENGTH_SHORT).show()
                }
        }
    }
}