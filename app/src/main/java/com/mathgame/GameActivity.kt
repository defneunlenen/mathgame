package com.mathgame

import android.animation.ObjectAnimator
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.mathgame.databinding.ActivityGameBinding
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var game: MathGame
    private lateinit var db: GameDatabase

    private var currentIndex = 0
    private var correctCount = 0
    private var operation = MathGame.OPERATION_MIX
    private var answered = false

    private val handler = Handler(Looper.getMainLooper())

    // Track all confetti views for cleanup — prevents view accumulation ANR
    private val confettiViews = mutableListOf<View>()

    // Sound effects
    private var soundPool: SoundPool? = null
    private var soundCorrect: Int = 0
    private var soundWrong: Int = 0
    private var soundTick: Int = 0

    // Timer
    private val QUESTION_TIME_MS = 30_000L
    private val WARNING_THRESHOLD_MS = 10_000L
    private var timer: CountDownTimer? = null

    // Original button backgrounds for reset
    private val originalBackgrounds = intArrayOf(
        R.drawable.btn_answer_blue,
        R.drawable.btn_answer_purple,
        R.drawable.btn_answer_orange,
        R.drawable.btn_answer_green
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get extras
        operation = intent.getIntExtra(MainActivity.EXTRA_OPERATION, MathGame.OPERATION_MIX)
        val questionCount = intent.getIntExtra("questionCount", 10)

        binding.btnLeaderboard.setOnClickListener { finish() }


        // Show loading overlay while doing heavy init
        binding.loadingOverlay.visibility = View.VISIBLE

        // Heavy init on background thread
        Thread {
            try {
                // Init DB
                db = Room.databaseBuilder(
                    this@GameActivity, GameDatabase::class.java, "mathgame.db"
                ).build()

                // Init sounds
                soundPool = SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .build()
                soundCorrect = soundPool!!.load(this@GameActivity, R.raw.sound_correct, 1)
                soundWrong = soundPool!!.load(this@GameActivity, R.raw.sound_wrong, 1)
                soundTick = soundPool!!.load(this@GameActivity, R.raw.sound_tick, 1)

                // Generate questions (this is the heavy part for 30+ questions)
                game = MathGame(operation, questionCount)

                // Back to main thread
                runOnUiThread {
                    // Hide loading overlay
                    binding.loadingOverlay.visibility = View.GONE
                    currentIndex = 0
                    correctCount = 0
                    showQuestion()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    binding.loadingOverlay.visibility = View.GONE
                    finish()
                }
            }
        }.start()
    }

    private fun showQuestion() {
        answered = false
        if (currentIndex >= game.questions.size) {
            finishGame()
            return
        }

        val q = game.questions[currentIndex]

        binding.progressBar.progress =
            (currentIndex.toFloat() / game.questions.size * 100).toInt()
        binding.tvQuestionCounter.text = "${currentIndex + 1} / ${game.questions.size}"

        binding.tvQuestion.text = q.questionText
        binding.tvQuestion.alpha = 0f
        binding.tvQuestion.animate().alpha(1f).setDuration(300).start()

        binding.tvQuestionType.text = q.typeLabel
        when (q.typeLabel) {
            "Toplama" -> binding.tvQuestionType.setBackgroundResource(R.drawable.badge_green)
            "Çıkarma" -> binding.tvQuestionType.setBackgroundResource(R.drawable.badge_blue)
            "Çarpma" -> binding.tvQuestionType.setBackgroundResource(R.drawable.badge_purple)
            "Bölme" -> binding.tvQuestionType.setBackgroundResource(R.drawable.badge_orange)
            "Yuvarlama" -> binding.tvQuestionType.setBackgroundResource(R.drawable.badge_teal)
        }

        val answerTextViews = listOf(
            binding.tvAnswer1, binding.tvAnswer2,
            binding.tvAnswer3, binding.tvAnswer4
        )
        val answerButtons = listOf<FrameLayout>(
            binding.btnAnswer1, binding.btnAnswer2,
            binding.btnAnswer3, binding.btnAnswer4
        )

        // Reset & configure each button
        answerButtons.forEachIndexed { index, btn ->
            btn.setBackgroundResource(originalBackgrounds[index])
            btn.isEnabled = true
        }

        answerTextViews.forEachIndexed { index, tv ->
            tv.text = q.answers[index].toString()
            val isCorrect = q.answers[index] == q.correctAnswer

            tv.alpha = 0f
            tv.translationY = 30f
            tv.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay((150L * index).coerceAtMost(400L))
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            answerButtons[index].alpha = 0f
            answerButtons[index].animate()
                .alpha(1f).setStartDelay((150L * index).coerceAtMost(400L))
                .setDuration(300).start()

            answerButtons[index].setOnClickListener {
                if (answered) return@setOnClickListener
                answered = true
                handleAnswer(index, isCorrect)
            }
        }

        // Feedback
        binding.tvFeedback.visibility = View.GONE

        // Start countdown timer
        startTimer()
    }

    private fun handleAnswer(index: Int, isCorrect: Boolean) {
        val answerButtons = listOf<FrameLayout>(
            binding.btnAnswer1, binding.btnAnswer2,
            binding.btnAnswer3, binding.btnAnswer4
        )

        answerButtons.forEach { it.isEnabled = false }

        val correctColor = ContextCompat.getColor(this, R.color.correct)
        val wrongColor = ContextCompat.getColor(this, R.color.wrong)

        if (isCorrect) {
            correctCount++
            answerButtons[index].setBackgroundColor(correctColor)
            binding.tvFeedback.text = getRandomCorrectMessage()
            binding.tvFeedback.setTextColor(correctColor)
            playSound(soundCorrect)
            showConfetti()
        } else {
            answerButtons[index].setBackgroundColor(wrongColor)
            val correctIndex = game.questions[currentIndex].answers
                .indexOf(game.questions[currentIndex].correctAnswer)
            if (correctIndex >= 0 && correctIndex < answerButtons.size) {
                answerButtons[correctIndex].setBackgroundColor(correctColor)
            }
            binding.tvFeedback.text = getRandomWrongMessage() + " Cevap: ${game.questions[currentIndex].correctAnswer}"
            binding.tvFeedback.setTextColor(wrongColor)
            playSound(soundWrong)
            shakeButton(answerButtons[index])
        }

        binding.tvFeedback.visibility = View.VISIBLE
        scheduleNext()
    }

    private fun scheduleNext() {
        stopTimer()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            if (currentIndex + 1 >= game.questions.size) {
                finishGame()
            } else {
                currentIndex++
                showQuestion()
            }
        }, 1500)
    }

    private fun startTimer() {
        stopTimer()
        val totalSeconds = QUESTION_TIME_MS / 1000
        binding.tvTimer.text = "⏱️ $totalSeconds"
        binding.tvTimer.setTextColor(Color.WHITE)

        timer = object : CountDownTimer(QUESTION_TIME_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (answered) return
                val secondsLeft = millisUntilFinished / 1000
                binding.tvTimer.text = "⏱️ $secondsLeft"

                // Last 10 seconds: flash red + tick sound
                if (millisUntilFinished <= WARNING_THRESHOLD_MS) {
                    val redColor = ContextCompat.getColor(this@GameActivity, R.color.wrong)
                    binding.tvTimer.setTextColor(redColor)
                    playSound(soundTick)
                    // Pulse animation
                    binding.tvTimer.scaleX = 1.15f
                    binding.tvTimer.scaleY = 1.15f
                    binding.tvTimer.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(300)
                        .start()
                }
            }

            override fun onFinish() {
                if (answered) return
                answered = true
                binding.tvTimer.text = "⏰ Süre!"
                // Treat as wrong — show correct answer
                val q = game.questions[currentIndex]
                binding.tvFeedback.text = "⏰ Süre doldu! Cevap: ${q.correctAnswer}"
                binding.tvFeedback.setTextColor(
                    ContextCompat.getColor(this@GameActivity, R.color.wrong))
                binding.tvFeedback.visibility = View.VISIBLE
                playSound(soundWrong)

                // Highlight correct answer button
                val answerButtons = listOf<FrameLayout>(
                    binding.btnAnswer1, binding.btnAnswer2,
                    binding.btnAnswer3, binding.btnAnswer4
                )
                answerButtons.forEach { it.isEnabled = false }
                val correctIndex = q.answers.indexOf(q.correctAnswer)
                if (correctIndex >= 0 && correctIndex < answerButtons.size) {
                    answerButtons[correctIndex].setBackgroundColor(
                        ContextCompat.getColor(this@GameActivity, R.color.correct))
                }

                scheduleNext()
            }
        }.start()
    }

    private fun stopTimer() {
        try { timer?.cancel() } catch (_: Exception) {}
        timer = null
    }

    private fun showConfetti() {
        // If already finishing, skip confetti to avoid ANR
        if (isFinishing || isDestroyed) return

        val colors = listOf(
            ContextCompat.getColor(this, R.color.confetti1),
            ContextCompat.getColor(this, R.color.confetti2),
            ContextCompat.getColor(this, R.color.confetti3),
            ContextCompat.getColor(this, R.color.confetti4),
            ContextCompat.getColor(this, R.color.confetti5)
        )

        // Cap active confetti at 30 to prevent view inflation lag (was unbounded per question)
        val maxActive = 30
        val toRemove = confettiViews.size.coerceIn(0, confettiViews.size - maxActive + 20)
        if (toRemove > 0) {
            for (i in 0 until toRemove) {
                confettiViews[i].animate().cancel()
                try {
                    (confettiViews[i].parent as? ViewGroup)?.removeView(confettiViews[i])
                } catch (_: Exception) {}
            }
            confettiViews.subList(0, toRemove).clear()
        }

        for (i in 0 until 20) {
            val dot = View(this).apply {
                setBackgroundResource(R.drawable.dot)
                setBackgroundColor(colors[i % colors.size])
                alpha = 0f
                x = (java.lang.Math.random() * binding.gameRoot.width).toFloat()
                y = binding.gameRoot.height.toFloat()
            }
            try {
                (binding.gameRoot as ViewGroup).addView(dot, 60, 60)
                confettiViews.add(dot)
                ObjectAnimator.ofFloat(dot, "translationY",
                    -(binding.gameRoot.height + 100).toFloat()).apply {
                    duration = 1000L + (java.lang.Math.random() * 1000).toLong()
                    start()
                }
                ObjectAnimator.ofFloat(dot, "alpha", 1f, 0f).apply {
                    duration = 1500; start()
                }
                ObjectAnimator.ofFloat(dot, "scaleX", 1f, 0f).apply {
                    duration = 1500; start()
                }
                ObjectAnimator.ofFloat(dot, "scaleY", 1f, 0f).apply {
                    duration = 1500; start()
                }
                handler.postDelayed({
                    if (isFinishing || isDestroyed) return@postDelayed
                    dot.animate().cancel()
                    try { (dot.parent as? ViewGroup)?.removeView(dot) } catch (_: Exception) {}
                    confettiViews.remove(dot)
                }, 1800)
            } catch (e: Exception) {
                try { (dot.parent as? ViewGroup)?.removeView(dot) } catch (_: Exception) {}
                confettiViews.remove(dot)
            }
        }
    }

    private fun shakeButton(view: View) {
        try {
            ObjectAnimator.ofFloat(view, "translationX",
                -20f, 20f, -15f, 15f, -10f, 10f, 0f).apply {
                duration = 500; start()
            }
        } catch (@Suppress("unused") e: Exception) {}
    }

    private fun finishGame() {
        // Prevent re-entry — user taps fast or timer fires again
        if (isFinishing || isDestroyed) return

        // Stop all running animations immediately
        stopTimer()
        handler.removeCallbacksAndMessages(null)

        // Cancel & remove all confetti views — prevents ANR from stale callbacks
        for (dot in confettiViews) {
            dot.animate().cancel()
            try { (dot.parent as? ViewGroup)?.removeView(dot) } catch (_: Exception) {}
        }
        confettiViews.clear()

        // Show loading overlay so user knows something is happening
        binding.loadingOverlay.visibility = View.VISIBLE

        // Defer the actual finish to let the overlay render
        handler.post {
            saveScoreSync()
            val intent = android.content.Intent(this, ResultActivity::class.java).apply {
                putExtra("correctCount", correctCount)
                putExtra("totalQuestions", game.questions.size)
                putExtra("operation", operation)
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun saveScoreSync() {
        val score = correctCount * 100
        val accuracy = if (game.questions.size > 0)
            (correctCount.toFloat() / game.questions.size) * 100f else 0f
        val entry = ScoreEntry(
            score = score,
            correctCount = correctCount,
            totalQuestions = game.questions.size,
            accuracy = accuracy,
            operation = operationName(operation),
            timestamp = System.currentTimeMillis()
        )
        Thread {
            try {
                val bgDb = Room.databaseBuilder(
                    applicationContext, GameDatabase::class.java, "mathgame.db"
                ).build()
                bgDb.scoreDao().insert(entry)
                bgDb.close()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    private fun operationName(op: Int): String = when (op) {
        MathGame.OPERATION_ADD -> "Toplama"
        MathGame.OPERATION_SUBTRACT -> "Çıkarma"
        MathGame.OPERATION_MULTIPLY -> "Çarpma"
        MathGame.OPERATION_DIVIDE -> "Bölme"
        MathGame.OPERATION_ROUND -> "Yuvarlama"
        else -> "Karışık"
    }

    private fun getRandomCorrectMessage(): String = listOf(
        "🌟 Harika!", "🎉 Çok İyi!", "⭐ Mükemmel!",
        "🏆 Aferin!", "🌈 Süper!", "💪 Gayet Güzel!"
    ).random()

    private fun getRandomWrongMessage(): String = listOf(
        "😊 Bir dahakine!", "💪 Üzülme!",
        "📚 Çalış!", "🌟 Çok yakıştın!"
    ).random()

    private fun playSound(soundId: Int) {
        if (soundId != 0) {
            try {
                soundPool?.play(soundId, 0.7f, 0.7f, 1, 0, 1f)
            } catch (e: Exception) {}
        }
    }

    // Marker to detect "intentional" finish (by us) vs system-triggered pause
    private var isFinishingIntentionally = false

    override fun onPause() {
        super.onPause()
        // Only clean up if NOT intentionally finishing — preserves scheduleNext() callback
        if (!isFinishingIntentionally) {
            stopTimer()
            handler.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        handler.removeCallbacksAndMessages(null)
        // Cancel & remove any residual confetti views
        for (dot in confettiViews) {
            dot.animate().cancel()
            try { (dot.parent as? ViewGroup)?.removeView(dot) } catch (_: Exception) {}
        }
        confettiViews.clear()
        try { db.close() } catch (e: Exception) {}
        soundPool?.release()
    }
}
