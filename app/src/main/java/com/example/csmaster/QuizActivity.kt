package com.example.csmaster

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.*
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.csmaster.databinding.ActivityQuizBinding
import com.example.csmaster.databinding.ScoreDialogBinding

class QuizActivity : AppCompatActivity(), PhoneStateReceiver.InterruptionListener, View.OnClickListener {

    companion object {
        var questionModelList: List<QuestionModel> = listOf()
        var time: String = ""
    }
    private var interruptionCount = 0

    private lateinit var binding: ActivityQuizBinding
    private var countDownTimer: CountDownTimer? = null
    private lateinit var internetReceiver: InternetConnectivityReceiver
    private lateinit var phoneStateReceiver: PhoneStateReceiver
    private var currentQuestionIndex = 0
    private var selectedAnswer = ""
    private var score = 0
    private var questionAnswered = false
    private var interruptionHandled = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_PHONE_STATE] == true &&
                permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            registerPhoneReceiver()
        } else {
            Toast.makeText(this, "Permissions denied. Call/SMS detection won't work.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup click listeners
        binding.apply {
            btn0.setOnClickListener(this@QuizActivity)
            btn1.setOnClickListener(this@QuizActivity)
            btn2.setOnClickListener(this@QuizActivity)
            btn3.setOnClickListener(this@QuizActivity)
            nextBtn.setOnClickListener(this@QuizActivity)
            continueBtn.setOnClickListener {
                currentQuestionIndex++
                questionAnswered = false
                loadQuestions()
            }
        }

        // Ask for runtime permissions
        requestRuntimePermissions()

        loadQuestions()
        startTimer()
    }

    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            registerPhoneReceiver()
        }
    }

    private fun registerPhoneReceiver() {
        phoneStateReceiver = PhoneStateReceiver()
        PhoneStateReceiver.listener = this
        val intentFilter = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            addAction("android.provider.Telephony.SMS_RECEIVED")
        }
        registerReceiver(phoneStateReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()

        // Always update listener and re-register the phone receiver
        PhoneStateReceiver.listener = this

        phoneStateReceiver = PhoneStateReceiver()
        val phoneFilter = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            addAction("android.provider.Telephony.SMS_RECEIVED")
        }
        registerReceiver(phoneStateReceiver, phoneFilter)

        internetReceiver = InternetConnectivityReceiver()
        registerReceiver(internetReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        interruptionHandled = false
    }

    override fun onPause() {
        super.onPause()

        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (_: Exception) {}

        try {
            unregisterReceiver(internetReceiver)
        } catch (_: Exception) {}

        countDownTimer?.cancel()

        if (!interruptionHandled) {
            interruptionHandled = true
            autoFailCurrentQuestion("You left the app")
        }
    }

    private fun startTimer() {
        val totalTime = time.toInt() * 60 * 1000L
        countDownTimer = object : CountDownTimer(totalTime, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                binding.timerIndicatorTextview.text = String.format("%02d:%02d", minutes, remainingSeconds)
            }

            override fun onFinish() {
                countDownTimer = null
                showRestartDialog("Time's up! Please restart to try again.")
            }
        }.start()
    }

    private fun loadQuestions() {
        selectedAnswer = ""
        interruptionHandled = false
        binding.continueBtn.visibility = View.GONE
        binding.nextBtn.visibility = View.VISIBLE

        binding.apply {
            btn0.setBackgroundColor(getColor(R.color.gray))
            btn1.setBackgroundColor(getColor(R.color.gray))
            btn2.setBackgroundColor(getColor(R.color.gray))
            btn3.setBackgroundColor(getColor(R.color.gray))
            btn0.isEnabled = true
            btn1.isEnabled = true
            btn2.isEnabled = true
            btn3.isEnabled = true
        }

        if (currentQuestionIndex >= questionModelList.size) {
            finishQuiz()
            return
        }

        val q = questionModelList[currentQuestionIndex]
        binding.questionIndicatorTextview.text = "Question ${currentQuestionIndex + 1}/${questionModelList.size}"
        binding.questionProgressIndicator.progress = ((currentQuestionIndex.toFloat() / questionModelList.size) * 100).toInt()
        binding.questionTextview.text = q.question
        binding.btn0.text = q.options[0]
        binding.btn1.text = q.options[1]
        binding.btn2.text = q.options[2]
        binding.btn3.text = q.options[3]


    }

    override fun onClick(view: View?) {
        if (questionAnswered) return
        val clickedBtn = view as Button

        if (clickedBtn.id == R.id.next_btn) {
            if (selectedAnswer.isEmpty()) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
                return
            }

            questionAnswered = true
            val correct = questionModelList[currentQuestionIndex].correct
            highlightCorrectAnswer(correct)

            if (selectedAnswer == correct) score++

            binding.apply {
                btn0.isEnabled = false
                btn1.isEnabled = false
                btn2.isEnabled = false
                btn3.isEnabled = false
                nextBtn.visibility = View.GONE
                continueBtn.visibility = View.VISIBLE
            }
        } else {
            selectedAnswer = clickedBtn.text.toString()
            binding.apply {
                btn0.setBackgroundColor(getColor(R.color.gray))
                btn1.setBackgroundColor(getColor(R.color.gray))
                btn2.setBackgroundColor(getColor(R.color.gray))
                btn3.setBackgroundColor(getColor(R.color.gray))
            }
            clickedBtn.setBackgroundColor(getColor(R.color.orange))
        }
    }

    private fun highlightCorrectAnswer(correct: String) {
        binding.apply {
            when (correct) {
                btn0.text -> btn0.setBackgroundColor(getColor(R.color.green))
                btn1.text -> btn1.setBackgroundColor(getColor(R.color.green))
                btn2.text -> btn2.setBackgroundColor(getColor(R.color.green))
                btn3.text -> btn3.setBackgroundColor(getColor(R.color.green))
            }
        }
    }

    private fun autoFailCurrentQuestion(reason: String) {
        if (!questionAnswered && currentQuestionIndex < questionModelList.size) {
            questionAnswered = true
            val correct = questionModelList[currentQuestionIndex].correct
            highlightCorrectAnswer(correct)
            Log.d("QuizActivity", "$reason — Question marked wrong.")

            binding.apply {
                btn0.isEnabled = false
                btn1.isEnabled = false
                btn2.isEnabled = false
                btn3.isEnabled = false
                nextBtn.visibility = View.GONE
                continueBtn.visibility = View.VISIBLE
            }

            binding.continueBtn.setOnClickListener {
                goToNextQuestionOrScore()
            }
        }
    }

    private fun goToNextQuestionOrScore() {
        if (currentQuestionIndex + 1 < questionModelList.size) {
            currentQuestionIndex++
            questionAnswered = false
            loadQuestions()
        } else {
            finishQuiz()
        }
    }

    private fun finishQuiz() {
        val total = questionModelList.size
        val percentage = ((score.toFloat() / total) * 100).toInt()

        val dialogBinding = ScoreDialogBinding.inflate(layoutInflater)
        dialogBinding.apply {
            scoreProgressIndicator.progress = percentage
            scoreProgressText.text = "$percentage%"
            scoreTitle.text = if (percentage > 60) "Congrats! You passed" else "Oops! You failed"
            scoreTitle.setTextColor(if (percentage > 60) Color.BLUE else Color.RED)
            scoreSubtitle.text = "$score out of $total correct"
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
            AlertDialog.Builder(this)
                .setTitle("Quiz Ended")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Restart") { _, _ -> recreate() }
                .setNegativeButton("Exit") { _, _ -> finish() }
                .show()
        }
    }

    override fun onInterruptionDetected() {
        if (!interruptionHandled) {
            interruptionHandled = true
            interruptionCount++

            if (interruptionCount == 1) {
                // First interruption → auto-fail question, show warning
                autoFailCurrentQuestion("Call/SMS interruption")
                Toast.makeText(
                    this,
                    "Warning: You received a call or SMS.\nUse Do Not Disturb to avoid penalties.",
                    Toast.LENGTH_LONG
                ).show()
            } else if (interruptionCount >= 2) {
                // Second interruption → show exit dialog
                AlertDialog.Builder(this)
                    .setTitle("Too Many Interruptions")
                    .setMessage("You've received multiple calls or SMS.\nPlease retry the quiz later with Do Not Disturb enabled.")
                    .setCancelable(false)
                    .setPositiveButton("Exit") { _, _ -> finish() }
                    .show()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && questionAnswered) {
            Toast.makeText(this, "Cannot go back after answering!", Toast.LENGTH_SHORT).show()
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder(this)
                .setTitle("Exit Quiz")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes") { _, _ -> finish() }
                .setNegativeButton("No", null)
                .show()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }
}
