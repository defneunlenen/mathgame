package com.mathgame

import androidx.room.*

@Entity(tableName = "scores")
data class ScoreEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val correctCount: Int,
    val totalQuestions: Int,
    val accuracy: Float,
    val operation: String,
    val timestamp: Long
)

@Dao
interface ScoreDao {
    @Insert
    fun insert(entry: ScoreEntry)

    @Query("SELECT * FROM scores ORDER BY score DESC LIMIT :limit")
    suspend fun getTopScores(limit: Int): List<ScoreEntry>

    @Query("DELETE FROM scores WHERE score < (SELECT MIN(score) FROM (SELECT * FROM scores ORDER BY score DESC LIMIT 100))")
    suspend fun pruneScores()
}

@Database(entities = [ScoreEntry::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
}
