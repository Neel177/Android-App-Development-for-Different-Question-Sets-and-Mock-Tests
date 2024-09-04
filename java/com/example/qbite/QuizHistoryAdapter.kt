package com.example.qbite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuizHistoryAdapter(
    private var quizHistoryList: List<QuizHistory>,
    private val onDeleteClick: (QuizHistory) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_EMPTY = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.quiz_history_item, parent, false)
            QuizHistoryViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.empty_view, parent, false)
            EmptyViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is QuizHistoryViewHolder) {
            val quizHistory = quizHistoryList[position]
            holder.bind(quizHistory, onDeleteClick)
        }
    }

    override fun getItemCount(): Int {
        return if (quizHistoryList.isEmpty()) {
            1 // Return 1 to show the empty view
        } else {
            quizHistoryList.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (quizHistoryList.isEmpty()) {
            VIEW_TYPE_EMPTY
        } else {
            VIEW_TYPE_ITEM
        }
    }

    fun updateData(newQuizHistoryList: List<QuizHistory>) {
        quizHistoryList = newQuizHistoryList
        notifyDataSetChanged()
    }

    class QuizHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quizNameTextView: TextView = itemView.findViewById(R.id.quizNameTextView)
        private val scoreTextView: TextView = itemView.findViewById(R.id.scoreTextView)
        private val timeTakenTextView: TextView = itemView.findViewById(R.id.timeTakenTextView)
        private val quizDurationTextView: TextView = itemView.findViewById(R.id.quizDurationTextView)
        private val wrongAnswersPercentageTextView: TextView = itemView.findViewById(R.id.wrongAnswersPercentageTextView)
        private val totalQuestionsTextView: TextView = itemView.findViewById(R.id.totalQuestionsTextView)
        private val attemptedQuestionsTextView: TextView = itemView.findViewById(R.id.attemptedQuestionsTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(quizHistory: QuizHistory, onDeleteClick: (QuizHistory) -> Unit) {
            quizNameTextView.text = "Test: ${quizHistory.quizName}"
            scoreTextView.text = "Score: ${quizHistory.score}"

            val timeTakenMinutes = (quizHistory.timeTaken / 1000) / 60
            val timeTakenSeconds = (quizHistory.timeTaken / 1000) % 60
            timeTakenTextView.text = "Time Taken: ${timeTakenMinutes}m ${timeTakenSeconds}s"

            val quizDurationMinutes = quizHistory.quizDuration / 60
            quizDurationTextView.text = "Quiz Duration: ${quizDurationMinutes} minutes"
            wrongAnswersPercentageTextView.text = "Wrong Answers Percentage: ${quizHistory.wrongAnswersPercentage}%"
            totalQuestionsTextView.text = "Total Questions: ${quizHistory.totalQuestions}"
            attemptedQuestionsTextView.text = "Attempted Questions: ${quizHistory.attemptedQuestions}"
            dateTextView.text = "Date: ${quizHistory.date}"
            timeTextView.text = "Time: ${quizHistory.time}"
        }
    }

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emptyTextView: TextView = itemView.findViewById(R.id.emptyTextView)

        fun bind() {
            emptyTextView.text = "No history"
        }
    }
}
