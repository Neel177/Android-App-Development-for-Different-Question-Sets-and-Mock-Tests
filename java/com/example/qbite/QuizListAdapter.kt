package com.example.qbite
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.qbite.databinding.QuizItemRecyclerRowBinding

class QuizListAdapter(private val quizModelList: List<QuizModel>) :
    RecyclerView.Adapter<QuizListAdapter.MyViewHolder>() {

    class MyViewHolder(private val binding: QuizItemRecyclerRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: QuizModel) {
            binding.apply {
                testTitleText.text = model.title
                testSubtitleText.text = model.subtitle
                testTimeText.text = model.time + " min"


                root.setOnClickListener {
                     // Log the model ID
                    val intent = Intent(root.context, TestActivity::class.java).apply {
                        putExtra("MODEL_TITLE", model.title)
                        putExtra("MODEL_ID", model.id)
                        Log.d("QuizListAdapter", "Model ID: ${model.id}")// Pass the model ID
                    }
                    TestActivity.questionModelList = model.questionList
                    TestActivity.time = model.time // If time is required in TestActivity
                    root.context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = QuizItemRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return quizModelList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(quizModelList[position])
    }
}
