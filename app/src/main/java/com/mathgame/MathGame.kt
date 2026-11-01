package com.mathgame

import kotlin.math.max
import kotlin.random.Random

class MathGame(private val operationMode: Int, questionCount: Int = 20) {

    val questions: List<Question>

    init {
        questions = generateQuestions(questionCount)
    }

    private fun generateQuestions(count: Int): List<Question> {
        val questions = mutableListOf<Question>()

        repeat(count) {
            val op = when (operationMode) {
                OPERATION_ADD -> OPERATION_ADD
                OPERATION_SUBTRACT -> OPERATION_SUBTRACT
                OPERATION_MULTIPLY -> OPERATION_MULTIPLY
                OPERATION_DIVIDE -> OPERATION_DIVIDE
                OPERATION_ROUND -> OPERATION_ROUND
                else -> listOf(
                    OPERATION_ADD, OPERATION_SUBTRACT,
                    OPERATION_MULTIPLY, OPERATION_DIVIDE, OPERATION_ROUND
                ).random()
            }

            questions += when (op) {
                OPERATION_ADD -> generateAddition()
                OPERATION_SUBTRACT -> generateSubtraction()
                OPERATION_MULTIPLY -> generateMultiplication()
                OPERATION_DIVIDE -> generateDivision()
                OPERATION_ROUND -> generateRounding()
                else -> generateAddition()
            }
        }

        return questions
    }

    // 1-100 arası toplama
    private fun generateAddition(): Question {
        val a = Random.nextInt(1, 101)
        val b = Random.nextInt(1, 101)
        val answer = a + b
        val wrongAnswers = generateWrongAnswers(answer, answer / 2, answer * 2)
        val maxDigits = max(a.toString().length, b.toString().length)
        val questionText = buildVerticalText(a, b, "+", maxDigits)
        return Question(
            questionText = questionText,
            answers = shuffle(listOf(answer) + wrongAnswers),
            correctAnswer = answer,
            typeLabel = "Toplama"
        )
    }

    // 1-100 arası çıkarma (sonuç negatif olmasın)
    private fun generateSubtraction(): Question {
        val a = Random.nextInt(2, 101)
        val b = Random.nextInt(1, a)
        val answer = a - b
        val wrongAnswers = generateWrongAnswers(answer, max(0, answer - 20), answer + 20)
        val maxDigits = max(a.toString().length, b.toString().length)
        val questionText = buildVerticalText(a, b, "−", maxDigits)
        return Question(
            questionText = questionText,
            answers = shuffle(listOf(answer) + wrongAnswers),
            correctAnswer = answer,
            typeLabel = "Çıkarma"
        )
    }

    // Build vertical text:
    //    54
    // + 61
    // ----------
    private fun buildVerticalText(a: Int, b: Int, op: String, width: Int): String {
        val numA = a.toString().padStart(width + 1, ' ')
        val numB = b.toString().padStart(width, ' ')
        val line = "─".repeat(width + 1)
        return "$numA\n$op $numB\n$line"
    }

    // 2-10 arası çarpma (1 yok, çok kolay)
    private fun generateMultiplication(): Question {
        val a = Random.nextInt(2, 11)
        val b = Random.nextInt(2, 11)
        val answer = a * b
        val wrongAnswers = generateWrongAnswers(answer, max(1, answer - 20), answer + 20)
        return Question(
            questionText = "$a × $b = ?",
            answers = shuffle(listOf(answer) + wrongAnswers),
            correctAnswer = answer,
            typeLabel = "Çarpma"
        )
    }

    // 2-10 arası bölme (tam bölünen, 1 yok)
    private fun generateDivision(): Question {
        val b = Random.nextInt(2, 11)
        val answer = Random.nextInt(2, 11)
        val a = b * answer // a / b = answer
        val wrongAnswers = generateWrongAnswers(answer, 1, 10)
        return Question(
            questionText = "$a / $b = ?",
            answers = shuffle(listOf(answer) + wrongAnswers),
            correctAnswer = answer,
            typeLabel = "Bölme"
        )
    }

    // 1-1000 arası yuvarlama (birlik, onluk, yüzlük)
    private fun generateRounding(): Question {
        val number = Random.nextInt(1, 1001)
        val roundTo = listOf(10, 100).random()
        val rounded = round(number, roundTo)
        val wrongAnswers = generateWrongRoundingAnswers(number, roundTo, rounded)

        val unitName = when (roundTo) {
            10 -> "onluk"
            100 -> "yüzlük"
            else -> ""
        }

        return Question(
            questionText = "$number sayısını en yakın $unitName'e yuvarla",
            answers = shuffle(listOf(rounded) + wrongAnswers),
            correctAnswer = rounded,
            typeLabel = "Yuvarlama"
        )
    }

    private fun round(number: Int, to: Int): Int {
        return (number + to / 2) / to * to
    }

    private fun generateWrongRoundingAnswers(
        number: Int, roundTo: Int, correct: Int
    ): List<Int> {
        val wrongs = mutableSetOf<Int>()
        while (wrongs.size < 3) {
            val offset = when (roundTo) {
                1 -> Random.nextInt(1, 5) * Random.nextInt(1, 3) * if (Random.nextBoolean()) 1 else -1
                10 -> Random.nextInt(1, 3) * 10
                100 -> Random.nextInt(1, 3) * 100
                else -> 10
            }
            val wrong = correct + offset
            if (wrong != correct && wrong >= 0 && wrong <= 1000) {
                wrongs.add(wrong)
            }
            // Prevent infinite loop
            if (wrongs.size < 3 && wrongs.size >= 2) {
                val alt = correct + (roundTo * if (Random.nextBoolean()) 1 else -1)
                if (alt != correct && alt >= 0) wrongs.add(alt)
            }
        }
        return wrongs.toList()
    }

    private fun generateWrongAnswers(
        correct: Int, minWrong: Int, maxWrong: Int
    ): List<Int> {
        val wrongs = mutableSetOf<Int>()
        var attempts = 0
        while (wrongs.size < 3 && attempts < 100) {
            val wrong = Random.nextInt(max(minWrong, 0), maxWrong + 1)
            if (wrong != correct) {
                wrongs.add(wrong)
            }
            attempts++
        }
        // If still not enough, add offsets
        while (wrongs.size < 3) {
            val offset = wrongs.size + 1
            val wrong = correct + (if (Random.nextBoolean()) offset else -offset)
            if (wrong != correct && wrong >= 0) wrongs.add(wrong)
        }
        return wrongs.toList()
    }

    private fun <T> shuffle(list: List<T>): List<T> {
        return list.shuffled()
    }

    companion object {
        const val OPERATION_ADD = 1
        const val OPERATION_SUBTRACT = 2
        const val OPERATION_MULTIPLY = 3
        const val OPERATION_DIVIDE = 4
        const val OPERATION_ROUND = 5
        const val OPERATION_MIX = 6
    }
}

data class Question(
    val questionText: String,
    val answers: List<Int>,
    val correctAnswer: Int,
    val typeLabel: String
)
