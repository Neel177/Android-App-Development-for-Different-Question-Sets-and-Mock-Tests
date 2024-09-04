package com.example.qbite

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TestResultAdapter(
    private val questionModelList: List<QuestionModel>,
    private val score: Int,
    private val timeTakenSeconds: Long,
    private val quizTimeInSeconds: Long,
    private val wrongAnswersPercentage: Float,
    private val userAnswersBundle: Bundle?,
    private val correctAnswersBundle: Bundle?,
    private val modelId: String?,
    private val userId: String,
    private val databaseReference: DatabaseReference



    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SCORE = 0
        private const val VIEW_TYPE_QUESTION = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SCORE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_score_view, parent, false)
                ScoreViewHolder(view)
            }
            VIEW_TYPE_QUESTION -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_test_result, parent, false)
                TestResultViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ScoreViewHolder -> {
                holder.bind(score, timeTakenSeconds, wrongAnswersPercentage)
                Log.d("Adapter", "Quiz Time (seconds) in Adapter: $quizTimeInSeconds")
                Log.d("TestResultAdapter", "Model ID: $modelId") // Log the modelId here
            }
            is TestResultViewHolder -> {
                if (position > 0) {
                    val questionModel = questionModelList[position - 1] // Adjust position for the score view
                    holder.bind(questionModel, userAnswersBundle, position - 1)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        // Adjust count to include the score view
        return questionModelList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_SCORE
        } else {
            VIEW_TYPE_QUESTION
        }
    }

    inner class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scoreSubtitle: TextView = itemView.findViewById(R.id.score_subtitle)
        private val scoreSubtitletext: TextView = itemView.findViewById(R.id.score_subtitle_text)
        private val timeSubtitleText: TextView = itemView.findViewById(R.id.time_subtitle_text)
        private val scoreProgressText: TextView = itemView.findViewById(R.id.score_progress_text)
        private val timeProgressText: TextView = itemView.findViewById(R.id.time_progress_text)
        private val timeProgressIndicator: CircularProgressIndicator = itemView.findViewById(R.id.time_progress_indicator)
        private val wrongAnswersProgressText: TextView = itemView.findViewById(R.id.wrong_answers_progress_text)
        private val wrongAnswersSubtitleText: TextView = itemView.findViewById(R.id.wrong_answers_subtitle_text)
        private val congratsText: TextView = itemView.findViewById(R.id.congrats_text)
        private val attemptedQuestionsText: TextView = itemView.findViewById(R.id.attempted_questions_text)
        private val scoreProgressIndicator: CircularProgressIndicator = itemView.findViewById(R.id.score_progress_indicator)
        private val wrongAnswersProgressIndicator: CircularProgressIndicator = itemView.findViewById(R.id.wrong_answers_progress_indicator)

        fun bind(score: Int, timeTakenSeconds: Long, wrongAnswersPercentage: Float) {
            // Update score subtitle
            scoreSubtitle.text = "Your score is: $score"

            val scorePercentages = calculateScorePercentage(score)
            scoreSubtitletext.text = "You got $scorePercentages% "

            // Update score progress text
            val scorePercentage = calculateScorePercentage(score)
            animateTextProgress(scoreProgressText, scorePercentage)
            animateScoreProgress(scoreProgressIndicator, scorePercentage.toFloat())

            // Update time progress text
            val timeFormatted = formatTime(timeTakenSeconds)
            timeProgressText.text = timeFormatted

            // Update wrong answers progress text
            val questionsAttemptedd = if (userAnswersBundle is Bundle) {
                userAnswersBundle.keySet().count { key -> userAnswersBundle.getInt(key) != -1 }
            } else {
                0
            }
            val wrongAnswersPercentage = calculateWrongAnswersPercentage(score, questionsAttemptedd)
            animateWrongAnswersProgress(wrongAnswersProgressIndicator, wrongAnswersPercentage)

            // Update wrong answers subtitle text
            wrongAnswersProgressText.text = "${wrongAnswersPercentage.toInt()}%"
            wrongAnswersSubtitleText.text = "${wrongAnswersPercentage.toInt()}% Wrong answers"

            // Update time subtitle text
            val timetakenpercentage = if (quizTimeInSeconds.toInt() != 0) {
                ((timeTakenSeconds.toFloat() / (quizTimeInSeconds * 1000)) * 100).toInt()
            } else {
                0 // Handle the case where quizTimeInSeconds is zero
            }
            animateWrongAnswersProgress(timeProgressIndicator, timetakenpercentage.toFloat())
            timeSubtitleText.text = "You took ${formatTimeTaken(timeTakenSeconds)}"

            // Update congrats text based on score
            congratsText.text = when {
                scorePercentage >= 90 -> "Outstanding! You did exceptionally well!"
                scorePercentage >= 70 -> "Excellent! You did great!"
                scorePercentage >= 40 -> "Congratulations! You did good!"
                else -> "Better luck next time!"
            }

            // Update attempted questions text
            val questionsAttempted = if (userAnswersBundle is Bundle) {
                userAnswersBundle.keySet().count { key -> userAnswersBundle.getInt(key) != -1 }
            } else {
                0
            }
            attemptedQuestionsText.text = "You attempted $questionsAttempted/${questionModelList.size} questions."
            fetchQuizTitleAndSaveResult(userId, score, timeTakenSeconds, wrongAnswersPercentage)
        }

        private fun fetchQuizTitleAndSaveResult(userId: String?, score: Int, timeTakenSeconds: Long, wrongAnswersPercentage: Float) {
            modelId?.let { id ->
                databaseReference.child(id).child("title").get().addOnSuccessListener { dataSnapshot ->
                    val quizTitle = dataSnapshot.getValue(String::class.java) ?: "Unknown Quiz"
                    saveTestResultToFirebase(userId, quizTitle, score, timeTakenSeconds, wrongAnswersPercentage)
                }.addOnFailureListener { e ->
                    Log.e("TestResultAdapter", "Error fetching quiz title: ${e.message}")
                    saveTestResultToFirebase(userId, "Unknown Quiz", score, timeTakenSeconds, wrongAnswersPercentage)
                }
            } ?: run {
                saveTestResultToFirebase(userId, "Unknown Quiz", score, timeTakenSeconds, wrongAnswersPercentage)
            }
        }
        private fun saveTestResultToFirebase(userId: String?, quizTitle: String, score: Int, timeTakenSeconds: Long, wrongAnswersPercentage: Float) {
            userId?.let { uid ->
                val timestamp = Calendar.getInstance().timeInMillis

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val date = dateFormat.format(timestamp)
                val time = timeFormat.format(timestamp)
                val quizModelId=modelId.toString()

                val quizHistory = QuizHistory(
                    quizModelId,
                    quizTitle,
                    score,
                    timeTakenSeconds,
                    quizTimeInSeconds,
                    wrongAnswersPercentage,
                    questionModelList.size,
                    calculateAttemptedQuestions(),
                    date,
                    time
                )
                databaseReference.child("users").child(uid).child("quizHistory").child(timestamp.toString()).setValue(quizHistory)
                    .addOnSuccessListener {
                        Log.d("TestResultAdapter", "Quiz history saved to Firebase")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TestResultAdapter", "Error saving quiz history to Firebase: ${e.message}")
                    }
            }
        }
        private fun calculateAttemptedQuestions(): Int {
            return userAnswersBundle?.keySet()?.count { key -> userAnswersBundle.getInt(key) != -1 } ?: 0
        }
        private fun formatTimeTaken(timeTakenSeconds: Long): String {
            val seconds = (timeTakenSeconds / 1000).toInt()
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return if (minutes == 0) {
                "$remainingSeconds seconds"
            } else {
                "$minutes min ${remainingSeconds} seconds"
            }
        }
        private fun calculateWrongAnswersPercentage(score: Int, totalQuestionsAttempted: Int): Float {
            val wrongAnswersCount = totalQuestionsAttempted - score
            return if (totalQuestionsAttempted > 0) {
                (wrongAnswersCount.toFloat() / totalQuestionsAttempted) * 100
            } else {
                0f // Handle the case where no questions were attempted
            }
        }
        private fun calculateScorePercentage(score: Int): Int {
            val totalQuestions = questionModelList.size
            return if (totalQuestions > 0) {
                ((score.toFloat() / totalQuestions) * 100).toInt()
            } else {
                0 // Handle the case where there are no questions
            }
        }

        private fun formatTime(timeTakenSeconds: Long): String {
            val seconds = (timeTakenSeconds / 1000).toInt()
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%02d:%02d", minutes, remainingSeconds)
        }
        private fun animateTextProgress(textView: TextView, endValue: Int) {
            val animator = ValueAnimator.ofInt(0, endValue)
            animator.duration = 2000
            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                textView.text = "$animatedValue%"
            }
            animator.start()
        }
        private fun animateScoreProgress(progressIndicator: CircularProgressIndicator, percentage: Float) {
            // Color logic for score progress
            val color = when {
                percentage < 40 -> ContextCompat.getColor(itemView.context, R.color.red)
                percentage < 60 -> ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                else -> ContextCompat.getColor(itemView.context, R.color.colorPrimary)
            }
            progressIndicator.setIndicatorColor(color)

            val animator = ValueAnimator.ofFloat(0f, percentage)
            animator.duration = 1000
            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                progressIndicator.setProgressCompat(animatedValue.toInt(), true)
            }
            animator.start()
        }
        private fun animateWrongAnswersProgress(progressIndicator: CircularProgressIndicator, percentage: Float) {
            // Color logic for wrong answers progress (inverse logic)
            val color = when {
                percentage > 60 -> ContextCompat.getColor(itemView.context, R.color.red)
                percentage > 40 -> ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                else -> ContextCompat.getColor(itemView.context, R.color.green)
            }
            progressIndicator.setIndicatorColor(color)

            val animator = ValueAnimator.ofFloat(0f, percentage)
            animator.duration = 1000
            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                progressIndicator.setProgressCompat(animatedValue.toInt(), true)
            }
            animator.start()
        }
    }
    inner class TestResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionTextView: TextView = itemView.findViewById(R.id.question_textview)
        private val answerOptionButtons: List<Button> = listOf(
            itemView.findViewById(R.id.btn0),
            itemView.findViewById(R.id.btn1),
            itemView.findViewById(R.id.btn2),
            itemView.findViewById(R.id.btn3)
        )
        fun bind(questionModel: QuestionModel, userAnswersBundle: Bundle?, position: Int) {
            // Bind question text with question number
            questionTextView.text = "${position + 1}. ${questionModel.question}"

            Log.d("bind", "QuestionModel ID: ${questionModel.id}")

            // Reset background color for all buttons
            answerOptionButtons.forEach { button ->
                button.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.gray)
            }
            // Bind answer options
            questionModel.options.forEachIndexed { index, option ->
                // Set option label (A., B., C., D.)
                val optionLabel = "${('A'.toInt() + index).toChar()}. "
                answerOptionButtons[index].text = "$optionLabel$option"
                answerOptionButtons[index].isEnabled = false // Disables clickability
            }
            // Highlight the correct answer
            val correctAnswerIndex = questionModel.options.indexOf(questionModel.correct)
            val correctButton = answerOptionButtons[correctAnswerIndex]
            correctButton.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.green)
            // Highlight the user's answer if it was incorrect using userAnswersBundle
            correctAnswersBundle?.let { correctBundle ->
                userAnswersBundle?.let { userBundle ->
                    for (key in correctBundle.keySet()) {
                        val questionId = key.toInt()
                        val correctAnswerIndex = correctBundle.getInt(key)
                        val userAnswerIndex = userBundle.getInt(key, -1) // default value -1 if user answer is not provided
                        val currentQuestionIndex = adapterPosition - 1

                        if (questionId == currentQuestionIndex) {
                            val correctButton = answerOptionButtons[correctAnswerIndex]

                            if (userAnswerIndex != -1) { // checking if user has provided an answer
                                val userButton = answerOptionButtons[userAnswerIndex]

                                if (correctButton == userButton) {
                                    correctButton.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.lightgreen)
                                } else {
                                    userButton.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.red)
                                }
                            } else { // User hasn't provided an answer
                            }
                        }
                    }
                }
            }
        }
    }
}
