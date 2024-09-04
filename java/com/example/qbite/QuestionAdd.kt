package com.example.qbite

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.*

class QuestionAdd : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextSubtitle: EditText
    private lateinit var editTextTime: EditText
    private lateinit var editTextCategory: EditText
    private lateinit var questionsContainer: LinearLayout
    private lateinit var buttonAddQuestion: Button
    private lateinit var buttonSubmit: Button

    private var newQuizId: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_add) // Move setContentView to the top

        // Initialize views after setContentView
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextSubtitle = findViewById(R.id.editTextSubtitle)
        editTextTime = findViewById(R.id.editTextTime)
        editTextCategory = findViewById(R.id.editTextCategory)
        questionsContainer = findViewById(R.id.questionsContainer)
        buttonAddQuestion = findViewById(R.id.buttonAddQuestion)
        buttonSubmit = findViewById(R.id.buttonSubmit)

        fetchHighestQuizId()

        buttonAddQuestion.setOnClickListener {
            addQuestionField()
        }

        buttonSubmit.setOnClickListener {
            submitQuiz()
        }
    }

    private fun fetchHighestQuizId() {
        val database = FirebaseDatabase.getInstance().reference
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var highestId = 0
                for (quizSnapshot in snapshot.children) {
                    val quizId = quizSnapshot.key?.toIntOrNull() ?: 0
                    if (quizId > highestId) {
                        highestId = quizId
                    }
                }
                newQuizId = highestId + 1
                Log.d("QuestionAdd", "New Quiz ID: $newQuizId")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("QuestionAdd", "Failed to fetch highest quiz ID", error.toException())
            }
        })
    }

    private fun addQuestionField() {
        val questionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 0) // Add some padding between questions
        }

        fun createEditText(hint: String): EditText {
            val editText = EditText(this)
            editText.apply {
                this.hint = hint
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.edit_text_width) // Set height to 50dp
                ).apply {
                    setMargins(0, resources.getDimensionPixelSize(R.dimen.edit_text_gap), 0, 0) // Add margin at the top
                }
                setBackgroundResource(R.drawable.edittextbck)
                setPadding(16, 16, 16, 16) // Add padding to make it look better
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@QuestionAdd, android.R.color.black)) // Set text color to black
                setHintTextColor(ContextCompat.getColor(this@QuestionAdd, android.R.color.white)) // Set hint text color to white

            }
            return editText
        }
        val editTextQuestion = createEditText("Question")
        val editTextOption1 = createEditText("Option 1")
        val editTextOption2 = createEditText("Option 2")
        val editTextOption3 = createEditText("Option 3")
        val editTextOption4 = createEditText("Option 4")
        val editTextCorrectAnswer = createEditText("Correct Answer")

        questionLayout.addView(editTextQuestion)
        questionLayout.addView(editTextOption1)
        questionLayout.addView(editTextOption2)
        questionLayout.addView(editTextOption3)
        questionLayout.addView(editTextOption4)
        questionLayout.addView(editTextCorrectAnswer)


        val deleteButton = Button(this).apply {
            text = "Delete Question"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, resources.getDimensionPixelSize(R.dimen.edit_text_gap), 0, 0) // Add margin at the top
            }
            setBackgroundResource(R.drawable.rounded_corner_background) // Use the same background as other buttons
            setTextColor(ContextCompat.getColor(this@QuestionAdd, android.R.color.white)) // Set text color to white
            textSize = 16f // Set text size
            setPadding(16, 16, 16, 16) // Add padding to make it look better
            setOnClickListener {
                questionsContainer.removeView(questionLayout) // Remove the question layout when clicked
            }
        }
        questionLayout.addView(deleteButton)

        questionsContainer.addView(questionLayout)
    }

    private fun submitQuiz() {
        val title = editTextTitle.text.toString().trim()
        val subtitle = editTextSubtitle.text.toString().trim()
        val time = editTextTime.text.toString().trim()
        val category = editTextCategory.text.toString().trim()

        if (title.isEmpty() || subtitle.isEmpty() || time.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val questions = mutableListOf<QuestionModel>()

        for (i in 0 until questionsContainer.childCount) {
            val questionLayout = questionsContainer.getChildAt(i) as LinearLayout
            val questionText = (questionLayout.getChildAt(0) as EditText).text.toString().trim()
            val option1 = (questionLayout.getChildAt(1) as EditText).text.toString().trim()
            val option2 = (questionLayout.getChildAt(2) as EditText).text.toString().trim()
            val option3 = (questionLayout.getChildAt(3) as EditText).text.toString().trim()
            val option4 = (questionLayout.getChildAt(4) as EditText).text.toString().trim()
            val correctAnswer = (questionLayout.getChildAt(5) as EditText).text.toString().trim()

            // Check if any question field is empty
            if (questionText.isEmpty()) {
                Toast.makeText(this, "Question ${i + 1} is empty", Toast.LENGTH_SHORT).show()
                return
            }

            // Check if any option field is empty
            if (option1.isEmpty() || option2.isEmpty() || option3.isEmpty() || option4.isEmpty() || correctAnswer.isEmpty()) {
                Toast.makeText(this, "Please fill out all options for question ${i + 1}", Toast.LENGTH_SHORT).show()
                return
            }

            // Check if options are unique and correct answer is one of the options
            val options = listOf(option1, option2, option3, option4)
            val distinctOptions = options.distinct()
            if (distinctOptions.size != options.size) {
                Toast.makeText(this, "Options for question ${i + 1} must be unique", Toast.LENGTH_SHORT).show()
                return
            }
            if (!options.any { it.toLowerCase() == correctAnswer.toLowerCase() }) {
                Toast.makeText(this, "Correct answer for question ${i + 1} must be one of the options", Toast.LENGTH_SHORT).show()
                return
            }

            val question = QuestionModel(
                id = "", // ID will be set by Firebase or can be left empty
                question = questionText,
                options = options,
                correct = correctAnswer
            )
            questions.add(question)
        }

        val quizModel = QuizModel(
            id = newQuizId.toString(),
            title = title,
            subtitle = subtitle,
            time = time,
            questionList = questions,
            category = category
        )

        uploadToFirebase(quizModel)
    }




    private fun uploadToFirebase(quizModel: QuizModel) {
        val database = FirebaseDatabase.getInstance().reference
        val quizRef = database.child(quizModel.id) // Save directly under the root or another specified node
        quizRef.setValue(quizModel)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Test added successfully", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity and return to the previous one
                } else {
                    Log.e("Upload", "Failed to add test", task.exception)
                    Toast.makeText(this, "Failed to add test", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
