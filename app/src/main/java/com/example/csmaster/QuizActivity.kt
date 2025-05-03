package com.example.csmaster

import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.csmaster.databinding.ActivityQuizBinding
import com.example.csmaster.databinding.ScoreDialogBinding

class QuizActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        var questionModelList: List<QuestionModel> = listOf()
        var time: String = ""
    }

    private lateinit var binding: ActivityQuizBinding
    private var countDownTimer: CountDownTimer? = null
    private lateinit var internetReceiver: InternetConnectivityReceiver

    private var currentQuestionIndex = 0
    private var selectedAnswer = ""
    private var score = 0

    override fun onResume() {
        super.onResume()
        // register for connectivity changes
        internetReceiver = InternetConnectivityReceiver()
        registerReceiver(
            internetReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onPause() {
        super.onPause()
        // unregister and cancel quiz
        unregisterReceiver(internetReceiver)
        countDownTimer?.cancel()
        showRestartDialog("Quiz paused. Please restart to continue.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set click listeners
        binding.apply {
            btn0.setOnClickListener(this@QuizActivity)
            btn1.setOnClickListener(this@QuizActivity)
            btn2.setOnClickListener(this@QuizActivity)
            btn3.setOnClickListener(this@QuizActivity)
            nextBtn.setOnClickListener(this@QuizActivity)
        }

        loadQuestions()
        startTimer()
    }

    private fun startTimer() {
        val totalTimeInMillis = time.toInt() * 60 * 1000L
        countDownTimer = object : CountDownTimer(totalTimeInMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                binding.timerIndicatorTextview.text =
                    String.format("%02d:%02d", minutes, remainingSeconds)
            }

            override fun onFinish() {
                // Time’s up — cancel further ticks
                countDownTimer = null
                showRestartDialog("Time’s up! Please restart to try again.")
            }
        }.start()
    }

    private fun loadQuestions() {
        selectedAnswer = ""
        if (currentQuestionIndex == questionModelList.size) {
            finishQuiz()
            return
        }

        binding.apply {
            questionIndicatorTextview.text =
                "Question ${currentQuestionIndex + 1}/${questionModelList.size}"
            questionProgressIndicator.progress =
                ((currentQuestionIndex.toFloat() / questionModelList.size) * 100).toInt()
            questionTextview.text = questionModelList[currentQuestionIndex].question
            btn0.text = questionModelList[currentQuestionIndex].options[0]
            btn1.text = questionModelList[currentQuestionIndex].options[1]
            btn2.text = questionModelList[currentQuestionIndex].options[2]
            btn3.text = questionModelList[currentQuestionIndex].options[3]
        }
    }

    override fun onClick(view: View?) {
        // reset all buttons to gray
        binding.apply {
            btn0.setBackgroundColor(getColor(R.color.gray))
            btn1.setBackgroundColor(getColor(R.color.gray))
            btn2.setBackgroundColor(getColor(R.color.gray))
            btn3.setBackgroundColor(getColor(R.color.gray))
        }

        val clickedBtn = view as Button
        if (clickedBtn.id == R.id.next_btn) {
            // Next button
            if (selectedAnswer.isEmpty()) {
                Toast.makeText(applicationContext, "Please select an answer", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            if (selectedAnswer == questionModelList[currentQuestionIndex].correct) {
                score++
                Log.i("Score of quiz", score.toString())
            }
            currentQuestionIndex++
            loadQuestions()
        } else {
            // An option was clicked
            selectedAnswer = clickedBtn.text.toString()
            clickedBtn.setBackgroundColor(getColor(R.color.orange))
        }
    }


    private fun finishQuiz() {
        val totalQuestions = questionModelList.size
        val percentage = ((score.toFloat() / totalQuestions) * 100).toInt()

        val dialogBinding = ScoreDialogBinding.inflate(layoutInflater)
        dialogBinding.apply {
            scoreProgressIndicator.progress = percentage
            scoreProgressText.text = "$percentage%"
            if (percentage > 60) {
                scoreTitle.text = "Congrats! You have passed"
                scoreTitle.setTextColor(Color.BLUE)
            } else {
                scoreTitle.text = "Oops! You have failed"
                scoreTitle.setTextColor(Color.RED)
            }
            scoreSubtitle.text = "$score out of $totalQuestions correct"
            finishBtn.setOnClickListener { finish() }
        }

        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                AlertDialog.Builder(this)
                    .setView(dialogBinding.root)
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun showRestartDialog(message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                AlertDialog.Builder(this)
                    .setTitle("Quiz Ended")
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Restart") { _, _ ->
                        // restart this activity
                        recreate()
                    }
                    .setNegativeButton("Exit") { _, _ ->
                        finish()
                    }
                    .show()
            }
        }
    }
}
