package com.example.qbite

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qbite.QuizModel
import com.example.qbite.R

class CustomQuizAdapter(private var quizList: List<QuizModel>) :
    RecyclerView.Adapter<CustomQuizAdapter.CustomQuizViewHolder>() {

    inner class CustomQuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quizTitleTextView: TextView = itemView.findViewById(R.id.quizTitleTextView)

        fun bind(quizModel: QuizModel) {
            // Set quiz title
            quizTitleTextView.text = quizModel.title

            // Set click listener
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, TestActivity::class.java).apply {
                    putExtra("MODEL_TITLE", quizModel.title)
                    putExtra("MODEL_ID", quizModel.id)
                    Log.d("CustomQuizAdapter", "Model ID: ${quizModel.id}") // Pass the model ID
                }
                TestActivity.questionModelList = quizModel.questionList
                TestActivity.time = quizModel.time // If time is required in TestActivity
                itemView.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomQuizViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.recycler_view_item,
            parent, false
        )
        return CustomQuizViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CustomQuizViewHolder, position: Int) {
        val currentItem = quizList[position]
        holder.bind(currentItem)
    }

    override fun getItemCount(): Int {
        return quizList.size
    }

    fun updateData(newList: List<QuizModel>) {
        quizList = newList
        notifyDataSetChanged()
    }
}
