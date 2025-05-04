package com.example.csmaster

import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import com.example.csmaster.databinding.ActivityQuizBinding
import com.example.csmaster.databinding.ScoreDialogBinding

class QuizActivity : AppCompatActivity(), View.OnClickListener, PhoneStateReceiver.PhoneStateListener {

    companion object {
        var questionModelList: List<QuestionModel> = listOf()
        var time: String = ""
    }

    private lateinit var binding: ActivityQuizBinding
    private var countDownTimer: CountDownTimer? = null
    private lateinit var internetReceiver: InternetConnectivityReceiver
    private lateinit var phoneStateReceiver: PhoneStateReceiver

    private var currentQuestionIndex = 0
    private var selectedAnswer = ""
    private var score = 0
    private var questionAnswered = false

    private var interruptionHandled = false

    override fun onResume() {
        super.onResume()

        // Reset interruption flag
        interruptionHandled = false

        // Re-register the internet receiver
        internetReceiver = InternetConnectivityReceiver()
        registerReceiver(
            internetReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }


    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (e: Exception) { }

        unregisterReceiver(internetReceiver)
        countDownTimer?.cancel()

        if (!interruptionHandled) {
            interruptionHandled = true
            autoFailCurrentQuestion("You left the app")
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set click listeners
        binding.apply {
            btn0.setOnClickListener(this@QuizActivity)
            btn1.setOnClickListener(this@QuizActivity)
            btn2.setOnClickListener(this@QuizActivity)
            btn3.setOnClickListener(this@QuizActivity)
            nextBtn.setOnClickListener(this@QuizActivity)
            continueBtn.setOnClickListener {
                // Move to next question
                currentQuestionIndex++
                questionAnswered = false
                loadQuestions()
            }
        }

        // Register phone state receiver
        phoneStateReceiver = PhoneStateReceiver()
        PhoneStateReceiver.listener = this
        val intentFilter = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            addAction("android.provider.Telephony.SMS_RECEIVED")
        }
        registerReceiver(phoneStateReceiver, intentFilter)

        loadQuestions()
        startTimer()
    }

    override fun onCallOrSmsReceived() {
        runOnUiThread {
            autoFailCurrentQuestion("You received a call or SMS")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Prevent back button from working when a question is answered
        // or during the quiz
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (questionAnswered) {
                Toast.makeText(this, "Cannot go back after answering!", Toast.LENGTH_SHORT).show()
                return true // consume the back button
            }

            // Show dialog to confirm exit
            AlertDialog.Builder(this)
                .setTitle("Exit Quiz")
                .setMessage("Are you sure you want to exit? Your progress will be lost.")
                .setPositiveButton("Yes") { _, _ -> finish() }
                .setNegativeButton("No", null)
                .show()
            return true
        }
        return super.onKeyDown(keyCode, event)
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
                // Time's up — cancel further ticks
                countDownTimer = null
                showRestartDialog("Time's up! Please restart to try again.")
            }
        }.start()
    }

    private fun loadQuestions() {
        selectedAnswer = ""
        binding.continueBtn.visibility = View.GONE
        binding.nextBtn.visibility = View.VISIBLE

        // Reset button colors
        binding.apply {
            btn0.setBackgroundColor(getColor(R.color.gray))
            btn1.setBackgroundColor(getColor(R.color.gray))
            btn2.setBackgroundColor(getColor(R.color.gray))
            btn3.setBackgroundColor(getColor(R.color.gray))

            // Enable all option buttons
            btn0.isEnabled = true
            btn1.isEnabled = true
            btn2.isEnabled = true
            btn3.isEnabled = true
        }

        if (currentQuestionIndex >= questionModelList.size) {
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
        if (questionAnswered) {
            // If question is already answered, don't allow option changes
            return
        }

        val clickedBtn = view as Button
        if (clickedBtn.id == R.id.next_btn) {
            // Next button
            if (selectedAnswer.isEmpty()) {
                Toast.makeText(applicationContext, "Please select an answer", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            // Set question as answered
            questionAnswered = true

            // Process the answer
            val currentQuestion = questionModelList[currentQuestionIndex]
            val correctAnswer = currentQuestion.correct

            // Highlight correct answer
            highlightCorrectAnswer(correctAnswer)

            // Check if answer is correct
            if (selectedAnswer == correctAnswer) {
                score++
                Log.i("Score of quiz", score.toString())
            }

            // Disable all option buttons
            binding.apply {
                btn0.isEnabled = false
                btn1.isEnabled = false
                btn2.isEnabled = false
                btn3.isEnabled = false

                // Hide next button, show continue button
                nextBtn.visibility = View.GONE
                continueBtn.visibility = View.VISIBLE
            }

        } else {
            // An option was clicked
            // Reset all buttons to gray
            binding.apply {
                btn0.setBackgroundColor(getColor(R.color.gray))
                btn1.setBackgroundColor(getColor(R.color.gray))
                btn2.setBackgroundColor(getColor(R.color.gray))
                btn3.setBackgroundColor(getColor(R.color.gray))
            }

            selectedAnswer = clickedBtn.text.toString()
            clickedBtn.setBackgroundColor(getColor(R.color.orange))
        }
    }

    private fun highlightCorrectAnswer(correctAnswer: String) {
        // Find and highlight the correct answer button
        binding.apply {
            when {
                btn0.text.toString() == correctAnswer ->
                    btn0.setBackgroundColor(getColor(R.color.green))
                btn1.text.toString() == correctAnswer ->
                    btn1.setBackgroundColor(getColor(R.color.green))
                btn2.text.toString() == correctAnswer ->
                    btn2.setBackgroundColor(getColor(R.color.green))
                btn3.text.toString() == correctAnswer ->
                    btn3.setBackgroundColor(getColor(R.color.green))
            }
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

    private fun autoFailCurrentQuestion(reason: String) {
        if (!questionAnswered && currentQuestionIndex < questionModelList.size) {
            questionAnswered = true

            // Show the correct answer
            val currentQuestion = questionModelList[currentQuestionIndex]
            val correctAnswer = currentQuestion.correct
            highlightCorrectAnswer(correctAnswer)

            // Log the interruption reason
            Log.d("QuizActivity", "$reason — Question marked as incorrect.")

            // Disable all buttons so no further changes can be made
            binding.apply {
                btn0.isEnabled = false
                btn1.isEnabled = false
                btn2.isEnabled = false
                btn3.isEnabled = false

                // Hide next button, show continue button after interruption
                nextBtn.visibility = View.GONE
                continueBtn.visibility = View.VISIBLE
            }

            // Set the "Continue" button click listener to move to the next question or show the score
            binding.continueBtn.setOnClickListener {
                goToNextQuestionOrScore()
            }
        }
    }



    private fun goToNextQuestionOrScore() {
        if (currentQuestionIndex + 1 < questionModelList.size) {
            // Move to the next question
            currentQuestionIndex++
            questionAnswered = false
            loadQuestions()  // Load next question
        } else {
            // Show score dialog if it's the last question
            finishQuiz()
        }
    }


}