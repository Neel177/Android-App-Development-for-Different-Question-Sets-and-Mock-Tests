package com.example.qbite


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.qbite.databinding.QuizItemRecyclerRowAdminBinding
import com.google.firebase.database.FirebaseDatabase

class QuizListAdapteradmin(
    private val context: Context,
    private val quizModelList: MutableList<QuizModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ADD_BUTTON = 0
    private val VIEW_TYPE_ITEM = 1

    inner class ItemViewHolder(private val binding: QuizItemRecyclerRowAdminBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: QuizModel) {
            binding.apply {
                testTitleText.text = model.title
                testSubtitleText.text = model.subtitle
                testTimeText.text = "${model.time} min"

                deletebutton.setOnClickListener {
                    showDeleteConfirmationDialog(model)
                }
            }
        }

        private fun showDeleteConfirmationDialog(quiz: QuizModel) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_delete, null)
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogView.findViewById<TextView>(R.id.dialog_message).text =
                "Do you really want to delete ${quiz.title} test set from the database?"

            dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                deleteQuiz(quiz)
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun deleteQuiz(quiz: QuizModel) {
            val database = FirebaseDatabase.getInstance().reference.child(quiz.id)
            database.removeValue()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Test deleted successfully", Toast.LENGTH_SHORT).show()
                        quizModelList.remove(quiz)
                        notifyDataSetChanged()
                    } else {
                        Toast.makeText(context, "Failed to delete Test", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    inner class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.findViewById<Button>(R.id.btn_add_quiz).setOnClickListener {
                val intent = Intent(context, QuestionAdd::class.java)
                context.startActivity(intent)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ADD_BUTTON) {
            val buttonLayout = LayoutInflater.from(parent.context).inflate(R.layout.item_add_button, parent, false)
            ButtonViewHolder(buttonLayout)
        } else {
            val binding = QuizItemRecyclerRowAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun getItemCount(): Int {
        return quizModelList.size + 1 // +1 for the add button
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(quizModelList[position - 1]) // Adjust for the add button at position 0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_ADD_BUTTON
        } else {
            VIEW_TYPE_ITEM
        }
    }
}
