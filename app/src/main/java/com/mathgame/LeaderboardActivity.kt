package com.mathgame

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.mathgame.databinding.ActivityLeaderboardBinding
import com.mathgame.databinding.ItemScoreBinding
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private lateinit var db: GameDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            this, GameDatabase::class.java, "mathgame.db"
        ).build()

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadScores()
    }

    private fun loadScores() {
        lifecycleScope.launch {
            val scores = db.scoreDao().getTopScores(10)
            binding.recyclerView.layoutManager = LinearLayoutManager(this@LeaderboardActivity)

            if (scores.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.recyclerView.adapter = ScoreAdapter(scores)
            }
        }
    }
}

class ScoreAdapter(private val scores: List<ScoreEntry>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<ScoreAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemScoreBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemScoreBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val score = scores[position]

        holder.binding.tvRank.text = "#${position + 1}"
        when (position) {
            0 -> holder.binding.tvRank.setBackgroundColor(Color.parseColor("#FFD700"))
            1 -> holder.binding.tvRank.setBackgroundColor(Color.parseColor("#C0C0C0"))
            2 -> holder.binding.tvRank.setBackgroundColor(Color.parseColor("#CD7F32"))
            else -> holder.binding.tvRank.setBackgroundColor(Color.parseColor("#9E9E9E"))
        }

        holder.binding.tvScore.text = "${score.score} Puan"
        holder.binding.tvDetails.text =
            "${score.correctCount}/${score.totalQuestions} doğru (${score.accuracy.toInt()}%)"
        holder.binding.tvOperation.text = score.operation

        val diff = System.currentTimeMillis() - score.timestamp
        val timeAgo = when {
            diff < 60000 -> "Az önce"
            diff < 3600000 -> "${diff / 60000} dk önce"
            diff < 86400000 -> "${diff / 3600000} saat önce"
            else -> "${diff / 86400000} gün önce"
        }
        holder.binding.tvTime.text = timeAgo
    }

    override fun getItemCount() = scores.size
}
