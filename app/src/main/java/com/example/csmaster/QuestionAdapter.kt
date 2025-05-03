package com.example.csmaster

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.csmaster.databinding.ItemQuestionEditableBinding

class QuestionAdapter(
    private val questionList: MutableList<QuestionModel>
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(val binding: ItemQuestionEditableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return

            val question = questionList[position]
            val b = binding

            // Pre-fill all fields
            b.questionEditText.setText(question.question)
            b.optionA.setText(question.options.getOrNull(0) ?: "")
            b.optionB.setText(question.options.getOrNull(1) ?: "")
            b.optionC.setText(question.options.getOrNull(2) ?: "")
            b.optionD.setText(question.options.getOrNull(3) ?: "")
            b.correctAnswerEditText.setText(question.correct)

            b.questionEditText.addTextChangedListener(simpleWatcher {
                question.question = it
            })

            b.optionA.addTextChangedListener(simpleWatcher {
                updateOption(question, 0, it)
            })

            b.optionB.addTextChangedListener(simpleWatcher {
                updateOption(question, 1, it)
            })

            b.optionC.addTextChangedListener(simpleWatcher {
                updateOption(question, 2, it)
            })

            b.optionD.addTextChangedListener(simpleWatcher {
                updateOption(question, 3, it)
            })

            b.correctAnswerEditText.addTextChangedListener(simpleWatcher { text ->
                question.correct = text // The correct answer is now the full text (e.g., "Paris")
            })

            b.deleteButton.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    questionList.removeAt(pos)
                    notifyItemRemoved(pos)
                }
            }
        }

        private fun updateOption(question: QuestionModel, index: Int, value: String) {
            val updatedOptions = question.options.toMutableList()
            while (updatedOptions.size <= index) {
                updatedOptions.add("")
            }
            updatedOptions[index] = value
            question.options = updatedOptions
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionEditableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = questionList.size

    private fun simpleWatcher(onTextChanged: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s?.toString()?.trim() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }
}
