package com.example.qbite

data class QuizHistory(
    val quizModelId: String = "",
    val quizName: String = "",
    val score: Int = 0,
    val timeTaken: Long = 0,
    val quizDuration: Long = 0,
    val wrongAnswersPercentage: Float = 0.0f,
    val totalQuestions: Int = 0,
    val attemptedQuestions: Int = 0,
    val date: String = "",
    val time: String = ""
) {
    // No-argument constructor
    constructor() : this("", "", 0, 0, 0, 0.0f, 0, 0, "", "")
}
