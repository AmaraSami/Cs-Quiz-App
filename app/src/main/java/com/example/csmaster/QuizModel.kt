package com.example.ThinkBinary

data class QuizModel(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val time: Int = 0,
    val questionList: List<QuestionModel> = emptyList()
)

data class QuestionModel(
    var question: String = "",
    var options: List<String> = emptyList(),
    var correct: String = ""
)

