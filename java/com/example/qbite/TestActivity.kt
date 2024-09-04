package com.example.qbite

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.qbite.databinding.ActivityTestBinding
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.qbite.databinding.DialogConfirmFinishLayoutBinding
import com.google.android.material.progressindicator.CircularProgressIndicator



class TestActivity : AppCompatActivity(),View.OnClickListener {
    companion object {
        lateinit var questionModelList: List<QuestionModel>
        lateinit var time: String
    }

    private lateinit var binding: ActivityTestBinding
    private lateinit var progressIndicator: LinearProgressIndicator
    private var testStartTimeMillis: Long = 0 // Variable to store the start time of the test
    private var testEndTimeMillis: Long = 0 // Variable to store the end time of the test
    private lateinit var toolbarTitle: TextView
    private var currentQuestionIndex = 0
    private val selectedAnswers = mutableMapOf<Int, Int>() // Map to store selected answers (question number -> answer index)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, callback)
        binding.main.fitsSystemWindows = false


        // Retrieve the model ID from intent extras
        val modelId= intent.getStringExtra("MODEL_ID")

        Log.d("TestActivity", "Model ID: $modelId")
        val modeltitle= intent.getStringExtra("MODEL_TITLE")

        // Find the TextView in the toolbar
        val toolbarTitleTextView: TextView = findViewById(R.id.toolbarTitle)

        // Set the retrieved model title as the text for the TextView
        toolbarTitleTextView.text = modeltitle



        binding.finishButton.setOnClickListener {
            vibrateDevice()
            // Inflate the custom layout for the dialog box
            val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_finish_layout, null)
            val confirmTitle = dialogView.findViewById<TextView>(R.id.confirm_title)
            val confirmMessage = dialogView.findViewById<TextView>(R.id.confirm_message)



            // Create the AlertDialog instance
            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            // Set the background of the dialog window to be transparent
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Set the text for the TextViews
            confirmTitle.text = "Finish Test?"
            confirmMessage.text = "Are you sure you want to finish the test?"




            // Set click listeners for the positive and negative buttons
            dialogView.findViewById<Button>(R.id.confirm_negative_button)?.setOnClickListener {
                alertDialog.dismiss() // Dismiss the dialog when "No" button is clicked
            }

            dialogView.findViewById<Button>(R.id.confirm_positive_button)?.setOnClickListener {
                alertDialog.dismiss() // Dismiss the dialog
                finishQuiz() // Call finishQuiz() when "Yes" button is clicked
            }

            alertDialog.show() // Show the dialog
        }



        // Enable edge-to-edge display
        //enableEdgeToEdge()


        binding.btn0.setOnClickListener(this)
        binding.btn1.setOnClickListener(this)
        binding.btn2.setOnClickListener(this)
        binding.btn3.setOnClickListener(this)

        // Initialize RecyclerView and QuizAdapter

        progressIndicator = findViewById(R.id.question_progress_indicator)

        // Load questions
        loadQuestion()

        // Start timer
        startTimer()


        // Set up click listeners for next and previous buttons
        binding.nextButton.setOnClickListener { onNextClicked() }
        binding.previousButton.setOnClickListener { onPreviousClicked() }
        binding.backbtn.setOnClickListener { showQuitConfirmationDialog() }


        // Dynamically add buttons for each question
        addQuestionButtons()
    }

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Show quit confirmation dialog when back button is pressed
            showQuitConfirmationDialog()
        }
    }

    private fun showQuitConfirmationDialog() {
        // Inflate the quit confirmation dialog layout
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.quit_confirm_dialogbox, null)


        // Create the AlertDialog instance
        val alertDialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        // Set the background of the dialog window to be transparent
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Find views in the inflated layout
        val confirmTitle = view.findViewById<TextView>(R.id.confirm_title)
        val confirmMessage = view.findViewById<TextView>(R.id.confirm_message)
        val positiveButton = view.findViewById<Button>(R.id.confirm_positive_button)
        val negativeButton = view.findViewById<Button>(R.id.confirm_negative_button)


        // Set the text for the TextViews
        confirmTitle.text = "Quit Test?"
        confirmMessage.text = "Are you sure you want to quit the test?"

        // Set click listeners for the positive and negative buttons
        positiveButton.setOnClickListener {
            alertDialog.dismiss() // Dismiss the dialog when "Yes" button is clicked
            // Navigate to QuizActivity
            val intent = Intent(this, QuizActivity::class.java)
            startActivity(intent)

            // Set exit animation for the current activity
            overridePendingTransition(R.anim.nav_slide_out_right, R.anim.nav_slide_in_left)        }

        negativeButton.setOnClickListener {
            alertDialog.dismiss() // Dismiss the dialog when "No" button is clicked
        }


        // Show the dialog
        alertDialog.show()
    }








    private fun startTimer() {

        // Record the start time of the test
        testStartTimeMillis = System.currentTimeMillis()



        val totalTimeInMillis = time.toInt() * 60 * 1000L
        object : CountDownTimer(totalTimeInMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                binding.timerIndicatorTextview.text = String.format("%02d:%02d", minutes, remainingSeconds)
            }

            override fun onFinish() {
                finishQuiz()// Finish the quiz
            }
        }.start()
    }

    private fun loadQuestion() {

        val currentQuestion = questionModelList[currentQuestionIndex]
        binding.apply {




            questionTextview.text = currentQuestion.question
            btn0.text = currentQuestion.options[0]
            btn1.text = currentQuestion.options[1]
            btn2.text = currentQuestion.options[2]
            btn3.text = currentQuestion.options[3]


            // Reset button backgrounds to default state
            btn0.setBackgroundColor(getColor(R.color.gray))
            btn1.setBackgroundColor(getColor(R.color.gray))
            btn2.setBackgroundColor(getColor(R.color.gray))
            btn3.setBackgroundColor(getColor(R.color.gray))

            // Update indicators
            updateQuestionIndicator()
            updateProgressIndicator()
            // Check if a selected answer exists for this question
            selectedAnswers[currentQuestionIndex]?.let { answerIndex ->
                // Highlight the selected answer if it exists
                when (answerIndex) {
                    0 -> btn0.setBackgroundColor(getColor(R.color.button_text_color))
                    1 -> btn1.setBackgroundColor(getColor(R.color.button_text_color))
                    2 -> btn2.setBackgroundColor(getColor(R.color.button_text_color))
                    3 -> btn3.setBackgroundColor(getColor(R.color.button_text_color))
                }
            }
        }
    }



    private fun onNextClicked() {
        if (currentQuestionIndex < questionModelList.size - 1) {
            currentQuestionIndex++
            loadQuestion()
            highlightCurrentButton(currentQuestionIndex + 1)

        }
    }

    private fun onPreviousClicked() {

        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            loadQuestion()
            highlightCurrentButton(currentQuestionIndex+1)
        }
    }

    private fun updateQuestionIndicator() {
        binding.questionIndicatorTextview.text = "Question ${currentQuestionIndex + 1}/${questionModelList.size}"
    }
    private fun onQuestionButtonClicked(questionNumber: Int, answerIndex: Int) {
        currentQuestionIndex = questionNumber - 1
        selectedAnswers[currentQuestionIndex] = answerIndex // Store the selected answer
        loadQuestion()
    }

    private fun addQuestionButtons() {
        val questionButtonLayout = binding.questionButtonLayout

        val buttonCount = questionModelList.size
        val buttonWidth = resources.getDimensionPixelSize(R.dimen.button_width)
        val buttonMargin = resources.getDimensionPixelSize(R.dimen.button_margin)

        val rowLayout = LinearLayout(this)
        rowLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowLayout.orientation = LinearLayout.HORIZONTAL

        for (i in 0 until buttonCount) {
            val button = Button(this)
            val buttonNumber = i + 1
            button.text = buttonNumber.toString()
            button.id = View.generateViewId()

            // Set rounded circle background drawable to the button
            button.background = ContextCompat.getDrawable(this, R.drawable.rounded_circle)
            button.foreground = ContextCompat.getDrawable(this, R.drawable.buble)

            // Set text color of the button numbers
            button.setTextColor(ContextCompat.getColor(this, R.color.button_text_color))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                button.elevation = resources.getDimensionPixelSize(R.dimen.button_elevation).toFloat() // Set elevation for shadow effect
            }

            // Set a fixed width for the button
            val layoutParams = LinearLayout.LayoutParams(
                buttonWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (i != 0) {
                layoutParams.leftMargin = buttonMargin
            }
            button.layoutParams = layoutParams

            // Add button to the layout
            rowLayout.addView(button)

            // Set click listener
            button.setOnClickListener {
                // Execute the click logic
                onQuestionButtonClicked(buttonNumber)
                highlightCurrentButton(buttonNumber)

                // Animate the button text
                animateButtonText(button)
            }
        }

        // Add the row layout to the parent layout
        questionButtonLayout.addView(rowLayout)

        highlightCurrentButton(currentQuestionIndex + 1)

    }
    private fun highlightCurrentButton(currentButtonNumber: Int) {
        val rowLayout = binding.questionButtonLayout.getChildAt(0) as LinearLayout // Assuming the row layout is the first child

        for (i in 0 until rowLayout.childCount) {
            val button = rowLayout.getChildAt(i) as Button
            val buttonNumber = i + 1

            if (buttonNumber == currentButtonNumber) {
                // Apply highlighting effect to the current button
                val highlightDrawable = ContextCompat.getDrawable(this, R.drawable.highlighted_button_overlay)
                button.background = highlightDrawable



                // Check if the button is fully visible on the screen
                val isButtonVisible = isButtonFullyVisible(button, binding.horizontalScrollView)

                if (!isButtonVisible) {
                    // Button is not fully visible, so scroll to make it fully visible
                    button.post {
                        button.requestFocus()
                        val scrollAmount = calculateScrollAmount(button, binding.horizontalScrollView)
                        binding.horizontalScrollView.smoothScrollBy(scrollAmount, 0)
                    }
                }
            } else {
                // Reset other buttons to the original rounded circle background drawable
                val originalDrawable = ContextCompat.getDrawable(this, R.drawable.rounded_circle)
                button.background = originalDrawable
            }
        }
    }

    private fun isButtonFullyVisible(button: Button, scrollView: HorizontalScrollView): Boolean {
        val scrollViewRect = Rect()
        scrollView.getDrawingRect(scrollViewRect)

        val buttonRect = Rect()
        button.getDrawingRect(buttonRect)
        val buttonParent = button.parent as ViewGroup
        buttonParent.offsetDescendantRectToMyCoords(button, buttonRect)

        return scrollViewRect.contains(buttonRect)
    }

    private fun calculateScrollAmount(button: Button, scrollView: HorizontalScrollView): Int {
        val scrollViewRect = Rect()
        scrollView.getDrawingRect(scrollViewRect)

        val buttonRect = Rect()
        button.getDrawingRect(buttonRect)
        val buttonParent = button.parent as ViewGroup
        buttonParent.offsetDescendantRectToMyCoords(button, buttonRect)

        val extraScrollAmount = 150 // Adjust this value as needed

        return if (buttonRect.left < scrollViewRect.left) {
            // Button is to the left of the visible area, scroll left
            buttonRect.left - scrollViewRect.left - extraScrollAmount
        } else {
            // Button is to the right of the visible area, scroll right
            buttonRect.right - scrollViewRect.right + extraScrollAmount
        }
    }







    private var isAnimating = false // Flag to indicate if animation is in progress

    private fun animateButtonText(button: Button) {
        if (!isAnimating) { // Check if animation is not already in progress
            isAnimating = true // Set the flag to indicate animation is starting

            val startSize = button.textSize // Get the initial text size
            val endSize = startSize * 1.5f // Define the end text size

            // Define the value animator for text size animation
            val animator = ValueAnimator.ofFloat(startSize, endSize, startSize)
            animator.duration = 500 // Set the duration of the animation
            animator.interpolator = AccelerateDecelerateInterpolator() // Set the interpolator

            // Add update listener to update the text size during animation
            animator.addUpdateListener { valueAnimator ->
                val animatedValue = valueAnimator.animatedValue as Float
                if (animatedValue <= endSize) {
                    button.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue) // Update the text size
                }

            }

            // Add listener to reset the flag when animation ends
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false // Reset the flag when animation ends
                }
            })

            animator.start() // Start the animation
        }
    }


    private fun updateProgressIndicator() {
        val totalQuestions = questionModelList.size
        val progress = ((currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat()) * 100

        // Create an ObjectAnimator to smoothly animate the progress changes
        val animator = ObjectAnimator.ofInt(progressIndicator, "progress", progress.toInt())
        animator.interpolator = FastOutSlowInInterpolator() // Use FastOutSlowInInterpolator for smoother animation
        animator.duration = 300 // Set a shorter animation duration (in milliseconds)
        animator.addUpdateListener {
            // Update the progressIndicator during animation
            progressIndicator.progress = it.animatedValue as Int
        }
        animator.start() // Start the animation
    }

    private fun onQuestionButtonClicked(questionNumber: Int) {
        currentQuestionIndex = questionNumber - 1
        loadQuestion()

    }
    // Method to calculate the time taken for the test
    private fun calculateTimeTaken(): Long {
        // Calculate the end time of the test when the finish quiz button is clicked
        testEndTimeMillis = System.currentTimeMillis()
        // Calculate the time taken for the test by subtracting the start time from the end time
        return testEndTimeMillis - testStartTimeMillis
    }
    private fun finishQuiz() {
        // Calculate the time taken for the test
        val timeTakenSeconds: Long = calculateTimeTaken()

        var score = 0 // Initialize score variable

        // Iterate through each question
        for ((index, question) in questionModelList.withIndex()) {
            // Retrieve the correct answer for the current question
            val correctAnswerIndex = question.options.indexOf(question.correct)

            // Retrieve the user's selected answer for the current question
            val selectedAnswerIndex = selectedAnswers[index]

            // Compare the user's selected answer with the correct answer
            if (selectedAnswerIndex != null && selectedAnswerIndex == correctAnswerIndex) {
                // Increment score if the answer matches
                score++
            }
        }

        // Inflate the custom layout for the dialog box
        val dialogView = layoutInflater.inflate(R.layout.custom_score_dialog_layout, null)

        // Update the score information in the dialog layout
        val scoreTitle = dialogView.findViewById<TextView>(R.id.score_title)
        scoreTitle.text = "Test Finished..."
        scoreTitle.setTextColor(ContextCompat.getColor(this, R.color.white))

        val scoreSubtitle = dialogView.findViewById<TextView>(R.id.score_subtitle)
        scoreSubtitle.text = "Your score is: $score"

        val scoreProgressIndicator = dialogView.findViewById<CircularProgressIndicator>(R.id.score_progress_indicator)
        // Calculate the percentage of correct answers


        // Update score progress text
        val scorePercentag = (score * 100) / questionModelList.size
        animateProgress(scoreProgressIndicator, scorePercentag.toFloat())





        val scoreProgressText = dialogView.findViewById<TextView>(R.id.score_progress_text)
        scoreProgressText.text = "${(score * 100) / questionModelList.size}%"

        // Create and show the custom dialog box
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set the background of the dialog window to be transparent
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Find the buttons within the dialog view
        val finishButton = dialogView.findViewById<Button>(R.id.finish_btn)
        val watchAnswersButton = dialogView.findViewById<Button>(R.id.watch_answers_btn)

        // Set click listener for the Finish button
        finishButton.setOnClickListener {
            // Dismiss the dialog
            alertDialog.dismiss()

            // Navigate to QuizActivity
            val intent = Intent(this, QuizActivity::class.java)
            startActivity(intent)

            // Set exit animation for the current activity
            overridePendingTransition(R.anim.nav_slide_out_right, R.anim.nav_slide_in_left)
        }






        watchAnswersButton.setOnClickListener {
            // Dismiss the dialog
            alertDialog.dismiss()
            // Initialize variables to count total attempted answers and wrong answers
            var totalAttemptedAnswers = 0
            var wrongAnswers = 0

           // Iterate through each question
            for ((index, question) in questionModelList.withIndex()) {
                // Check if the user attempted this question
                if (selectedAnswers.containsKey(index)) {
                    totalAttemptedAnswers++

                    // Retrieve the correct answer index for the current question
                    val correctAnswerIndex = question.options.indexOf(question.correct)

                    // Retrieve the user's selected answer index for the current question
                    val userAnswerIndex = selectedAnswers[index]

                    // Check if the user's answer matches the correct answer
                    if (userAnswerIndex != null && userAnswerIndex != correctAnswerIndex) {
                        // Increment the count of wrong answers
                        wrongAnswers++
                    }
                }
            }

            // Calculate the percentage of wrong answers
            val wrongAnswersPercentage = if (totalAttemptedAnswers > 0) {
                (wrongAnswers.toDouble() / totalAttemptedAnswers.toDouble()) * 100.0
            } else {
                0.0 // Avoid division by zero if the user hasn't attempted any questions
            }


            val quizTimeInSeconds: Long = time.toInt() * 60L
            Log.d("TestActivity", "Quiz Time in Seconds: $quizTimeInSeconds")


            val modelId = intent.getStringExtra("MODEL_ID")
            Log.d("TestActivity2", "Model ID: $modelId")


            // Navigate to TestResultViewActivity with all data
            val intent = Intent(this, testresult_view::class.java).apply {

                // Add extras to the Intent
                putExtra("SCORE", score)
                putExtra("TIME_TAKEN", timeTakenSeconds)
                putExtra("QUIZ_TIME", quizTimeInSeconds)
                putExtra("MODEL_ID", modelId)
                val bundle = Bundle().apply {
                    selectedAnswers.forEach { (questionNumber, answerIndex) ->
                        putInt(questionNumber.toString(), answerIndex)
                    }
                }
                putExtra("USER_ANSWERS", bundle)
                putExtra("WRONG_ANSWERS_PERCENTAGE", wrongAnswersPercentage) // Add wrong answers percentage
                putExtra("QUESTION_MODEL_LIST", questionModelList.toTypedArray())

                // Include correct answers
                val correctAnswersMap = mutableMapOf<Int, Int>()
                for ((index, question) in questionModelList.withIndex()) {
                    val correctAnswerIndex = question.options.indexOf(question.correct)
                    correctAnswersMap[index] = correctAnswerIndex
                }
                val correctAnswersBundle = Bundle().apply {
                    correctAnswersMap.forEach { (questionNumber, answerIndex) ->
                        putInt(questionNumber.toString(), answerIndex)
                        Log.d("TestActivity", "Question $questionNumber - Correct Answer Index: $answerIndex")

                    }
                }
                putExtra("CORRECT_ANSWERS", correctAnswersBundle)
            }

            startActivity(intent)
            overridePendingTransition(R.anim.nav_slide_out_left, R.anim.nav_slide_in_right) // Animation for transitioning to TestResultViewActivity

        }


        alertDialog.show()
    }

    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Check if the device supports vibration
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Vibrate for 90 milliseconds using amplitude pattern
                vibrator.vibrate(VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Vibrate for 60 milliseconds
                vibrator.vibrate(60)
            }
        }
    }


    private fun animateProgress(progressIndicator: CircularProgressIndicator, percentage: Float) {
        val animator = ValueAnimator.ofFloat(0f, percentage)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            progressIndicator.setProgressCompat(animatedValue.toInt(), true)
        }
        animator.start()
    }




    override fun onClick(view: View?) {
        binding.apply {
            btn0.setBackgroundColor(getColor(R.color.gray))
            btn1.setBackgroundColor(getColor(R.color.gray))
            btn2.setBackgroundColor(getColor(R.color.gray))
            btn3.setBackgroundColor(getColor(R.color.gray))
        }
        val clickedBtn = view as Button
        val selectedAnswerIndex = when (clickedBtn) {
            binding.btn0 -> 0
            binding.btn1 -> 1
            binding.btn2 -> 2
            binding.btn3 -> 3
            else -> -1
        }

        clickedBtn.setBackgroundColor(getColor(R.color.button_text_color))
        onQuestionButtonClicked(currentQuestionIndex + 1, selectedAnswerIndex)    }
}
