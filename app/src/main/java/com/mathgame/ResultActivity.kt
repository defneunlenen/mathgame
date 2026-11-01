package com.mathgame

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.mathgame.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val correct = intent.getIntExtra("correctCount", 0)
        val total = intent.getIntExtra("totalQuestions", 10)
        val operation = intent.getIntExtra("operation", MathGame.OPERATION_MIX)

        val score = correct * 100
        val accuracy = if (total > 0) (correct.toFloat() / total * 100).toInt() else 0

        val opName = when (operation) {
            MathGame.OPERATION_ADD -> "Toplama"
            MathGame.OPERATION_SUBTRACT -> "Çıkarma"
            MathGame.OPERATION_MULTIPLY -> "Çarpma"
            MathGame.OPERATION_DIVIDE -> "Bölme"
            MathGame.OPERATION_ROUND -> "Yuvarlama"
            else -> "Karışık"
        }

        // Determine star rating
        val stars = when {
            accuracy == 100 -> "⭐⭐⭐"
            accuracy >= 80 -> "⭐⭐"
            accuracy >= 60 -> "⭐"
            else -> "💪"
        }

        val message = when {
            accuracy == 100 -> "Harika! Hepsini doğru yaptın!"
            accuracy >= 80 -> "Çok iyi iş çıkardın!"
            accuracy >= 60 -> "İyi gidiyorsun, devam et!"
            else -> "Tekrar dene, daha da iyi olursun!"
        }

        binding.tvScoreValue.text = "$score"
        binding.tvStars.text = stars
        binding.tvMessage.text = message
        binding.tvDetails.text = "$correct / $total doğru ($accuracy%) | $opName"

        // Animate score
        binding.tvScoreValue.alpha = 0f
        binding.tvScoreValue.scaleX = 0.5f
        binding.tvScoreValue.scaleY = 0.5f
        binding.tvScoreValue.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.btnMainMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        binding.btnLeaderboard.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }
    }
}
