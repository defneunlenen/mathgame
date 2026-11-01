package com.mathgame

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mathgame.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val operationButtons = listOf(
        Triple("🧮 Toplama", R.color.op_add, MathGame.OPERATION_ADD),
        Triple("📐 Çıkarma", R.color.op_sub, MathGame.OPERATION_SUBTRACT),
        Triple("✖️ Çarpma", R.color.op_mul, MathGame.OPERATION_MULTIPLY),
        Triple("➗ Bölme", R.color.op_div, MathGame.OPERATION_DIVIDE),
        Triple("🎯 Yuvarlama", R.color.op_round, MathGame.OPERATION_ROUND),
        Triple("🎲 Karışık", R.color.op_mix, MathGame.OPERATION_MIX)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up operation buttons
        val buttonIds = listOf(
            binding.btnAdd, binding.btnSub, binding.btnMul,
            binding.btnDiv, binding.btnRound, binding.btnMix
        )

        operationButtons.forEachIndexed { index, (text, bgColor, operation) ->
            val btn = buttonIds[index]
            btn.text = text
            val color = ContextCompat.getColor(this, bgColor)
            btn.setBackgroundColor(color)
            btn.setOnClickListener {
                startGame(operation)
            }
        }

        binding.btnLeaderboard.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        // Animate buttons appearing
        buttonIds.forEachIndexed { index, btn ->
            btn.alpha = 0f
            btn.translationY = 50f
            btn.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 100L)
                .setDuration(400)
                .start()
        }
    }

    private fun startGame(operation: Int) {
        val questionsDialog = android.app.AlertDialog.Builder(this)
        questionsDialog.setTitle("Kaç soru çözmek istersin?")
            .setItems(ARRAY_QTY) { _, which ->
                val qty = listOf(5, 10, 15, 20, 30)[which]
                val intent = Intent(this, GameActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_OPERATION, operation)
                    putExtra("questionCount", qty)
                }
                startActivity(intent)
            }
            .setCancelable(false)
            .create()
        questionsDialog.show()
    }

    companion object {
        const val EXTRA_OPERATION = "extra_operation"
        private val ARRAY_QTY = arrayOf("5 soru", "10 soru", "15 soru", "20 soru", "30 soru")
    }
}
