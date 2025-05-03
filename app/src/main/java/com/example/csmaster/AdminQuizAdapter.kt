package com.example.csmaster

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.csmaster.databinding.QuizItemRecyclerRowBinding
import com.google.firebase.firestore.FirebaseFirestore

class AdminQuizAdapter(
    private val quizList: List<QuizModel>,
    private val onEditClick: (QuizModel) -> Unit
) : RecyclerView.Adapter<AdminQuizAdapter.AdminViewHolder>() {

    inner class AdminViewHolder(private val binding: QuizItemRecyclerRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: QuizModel) {
            binding.quizTitleText.text = model.title
            binding.quizSubtitleText.text = model.subtitle
            binding.quizTimeText.text = "${model.time} min"

            binding.root.setOnClickListener {
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Manage Quiz")
                    .setMessage("What would you like to do with \"${model.title}\"?")
                    .setPositiveButton("Edit") { _, _ -> onEditClick(model) }
                    .setNegativeButton("Delete") { _, _ -> deleteQuiz(model) }
                    .setNeutralButton("Cancel", null)
                    .show()
            }
        }

        private fun deleteQuiz(model: QuizModel) {
            val db = FirebaseFirestore.getInstance()
            db.collection("quizzes").document(model.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(binding.root.context, "Quiz deleted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(binding.root.context, "Failed to delete quiz", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val binding = QuizItemRecyclerRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        holder.bind(quizList[position])
    }

    override fun getItemCount(): Int = quizList.size
}
