package com.example.qbite

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qbite.QuestionModel
import com.example.qbite.QuizActivity
import com.example.qbite.R
import com.example.qbite.TestResultAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class testresult_view : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testresult_view)

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().reference

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Set back button click listener
        val backButton = findViewById<ImageButton>(R.id.backbtn)
        backButton.setOnClickListener {
            // Navigate back to QuizActivity
            val intent = Intent(this, QuizActivity::class.java)
            startActivity(intent)
            finish() // Finish the current activity
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.resultsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Retrieve data from intent extras
        val score = intent.getIntExtra("SCORE", 0)
        val timeTakenSeconds = intent.getLongExtra("TIME_TAKEN", 0)
        val quizTimeInSeconds = intent.getLongExtra("QUIZ_TIME", 0)
        val wrongAnswersPercentage = intent.getFloatExtra("WRONG_ANSWERS_PERCENTAGE", 0f)
        val questionModelList = intent.getSerializableExtra("QUESTION_MODEL_LIST") as? Array<QuestionModel>
        val userAnswersBundle = intent.getBundleExtra("USER_ANSWERS")
        val correctAnswersBundle = intent.getBundleExtra("CORRECT_ANSWERS")
        val modelId = intent.getStringExtra("MODEL_ID")

        // Log to check if correct answers are retrieved correctly
        correctAnswersBundle?.let { bundle ->
            for (key in bundle.keySet()) {
                val questionNumber = key.toInt()
                val correctAnswerIndex = bundle.getInt(key)
                Log.d("TestResultActivity", "Question $questionNumber - Correct Answer Index: $correctAnswerIndex")
            }
        }

        // Check for nullability of questionModelList
        if (questionModelList != null) {
            // Pass data to TestResultAdapter along with database reference
            val userId = FirebaseAuth.getInstance().currentUser?.uid // Assuming you're using Firebase Authentication

            val adapter = TestResultAdapter(
                questionModelList.toList(),
                score,
                timeTakenSeconds,
                quizTimeInSeconds,
                wrongAnswersPercentage,
                userAnswersBundle,
                correctAnswersBundle,
                modelId,
                userId.toString(),
                databaseReference // Pass the database reference
            )
            recyclerView.adapter = adapter
        } else {
            // Handle the case when questionModelList is null (data not properly sent)
            // Display a toast or log an error message
            // For now, let's just finish the activity
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Start QuizActivity when back button is pressed
        startActivity(Intent(this, QuizActivity::class.java))
        finish()
    }
}
