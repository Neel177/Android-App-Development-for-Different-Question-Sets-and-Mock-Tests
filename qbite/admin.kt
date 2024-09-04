package com.example.qbite

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.qbite.databinding.ActivityAdminBinding
import com.example.qbite.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.abs

class admin : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var quizModelList: MutableList<QuizModel>
    private lateinit var filteredQuizModelList: MutableList<QuizModel>
    private lateinit var adapter: QuizListAdapteradmin
    private lateinit var toolbar: Toolbar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val categories = mutableSetOf<String>()
    private var selectedCategory: String = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        binding.main.fitsSystemWindows = false
        setContentView(binding.root)

        // Initialize UI components
        quizModelList = mutableListOf()
        filteredQuizModelList = mutableListOf()

        // Setup the toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.overflowIcon = null

        toolbar.overflowIcon = getDrawable(R.drawable.menu)


        // Initialize swipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            getDataFromFirebase()
        }

        // Call getDataFromFirebase after initializing swipeRefreshLayout
        getDataFromFirebase()

        setupRecyclerView()
        setupRecyclerViewGesture()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_admin, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about_us -> {
                // Handle "About Us" menu click
                Toast.makeText(this, "About Us Clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_switch_account -> {
                startActivity(Intent(this, MainActivity2::class.java))
                true
            }

            R.id.logoutButton -> {
                showLogoutConfirmationDialog()
                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        val alertDialogBuilder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
        val alertDialog = alertDialogBuilder.create()
        dialogView.alpha = 0f
        dialogView.animate().alpha(1f).setDuration(500).start()
        dialogView.findViewById<Button>(R.id.positiveButton).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        dialogView.findViewById<Button>(R.id.negativeButton).setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    private fun setupRecyclerView() {
        adapter = QuizListAdapteradmin(this, filteredQuizModelList) // Pass context and filteredQuizModelList
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.adapter = adapter
        binding.backbtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun getDataFromFirebase() {
        swipeRefreshLayout.isRefreshing = true // Start the refresh spinner

        FirebaseDatabase.getInstance().reference
            .get()
            .addOnSuccessListener { dataSnapshot ->
                quizModelList.clear() // Clear the existing list to avoid duplication
                categories.clear() // Clear the existing categories to avoid duplication
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        // Check if the snapshot key is not "users"
                        if (snapshot.key != "users") {
                            val quizModel = snapshot.getValue(QuizModel::class.java)
                            if (quizModel != null) {
                                quizModelList.add(quizModel)
                                categories.add(quizModel.category)
                            }
                        }
                    }
                    setupCategoryButtons()
                    filterQuizzesByCategory(selectedCategory) // Use the currently selected category
                }
                swipeRefreshLayout.isRefreshing = false // Stop the refresh spinner
            }
            .addOnFailureListener {
                swipeRefreshLayout.isRefreshing = false // Stop the refresh spinner in case of failure
                Log.e("MainActivity", "Error fetching data from Firebase", it)
            }
    }


    private fun setupCategoryButtons() {
        val categoryLayout = findViewById<LinearLayout>(R.id.textView7)
        categoryLayout.removeAllViews()

        // Add "Categories:" TextView
        val categoriesTextView = TextView(this).apply {
            text = "Categories:"
            textSize = 18f
            setPadding(16, 0, 16, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        categoryLayout.addView(categoriesTextView)

        // Button layout parameters
        val buttonLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 8, 8, 8) // Adjust margins as needed
        }

        // Horizontal padding value in dp
        val horizontalPadding = 16.dpToPx()

        // Add "All" button
        val allButton = Button(this).apply {
            text = "All"
            setOnClickListener { filterQuizzesByCategory("All") }
            setTextColor(resources.getColor(R.color.white, theme))
            setBackgroundResource(R.drawable.rounded_corner_background) // Set background shape
            layoutParams = buttonLayoutParams
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            tag = "All"
        }
        categoryLayout.addView(allButton)

        // Add buttons for each category
        val sortedCategories = categories.sortedDescending()
        for (category in sortedCategories) {
            val button = Button(this).apply {
                text = category
                setOnClickListener { filterQuizzesByCategory(category) }
                setTextColor(resources.getColor(android.R.color.white, theme))
                setBackgroundResource(R.drawable.rounded_corner_background) // Set background shape
                layoutParams = buttonLayoutParams
                setPadding(horizontalPadding, 0, horizontalPadding, 0)
                tag = category
            }
            categoryLayout.addView(button)
        }

        // Add a 20dp gap at the end after the last button
        val space = Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20.dpToPx() // Extension function to convert dp to pixels
            )
        }
        categoryLayout.addView(space)

        // Highlight the selected category button if already set
        highlightSelectedCategory()
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun filterQuizzesByCategory(category: String) {
        filteredQuizModelList.clear()
        if (category == "All") {
            filteredQuizModelList.addAll(quizModelList)
        } else {
            filteredQuizModelList.addAll(quizModelList.filter { it.category == category })
        }
        adapter.notifyDataSetChanged()
        selectedCategory = category
        highlightSelectedCategory()
    }

    private fun highlightSelectedCategory() {
        val categoryLayout = findViewById<LinearLayout>(R.id.textView7)
        val scrollView = findViewById<HorizontalScrollView>(R.id.scrollView)

        for (i in 0 until categoryLayout.childCount) {
            val view = categoryLayout.getChildAt(i)
            if (view is Button) {
                if (view.tag == selectedCategory) {
                    view.setBackgroundResource(R.drawable.ripple_rounded_gradient_background_selected) // highlight selected category

                    // Scroll to the selected category button
                    view.post {
                        val scrollX = view.left - (scrollView.width / 2) + (view.width / 2)
                        scrollView.smoothScrollTo(scrollX, 0)
                    }
                } else {
                    view.setBackgroundResource(R.drawable.ripple_rounded_gradient_background) // normal background
                }
            }
        }
    }

    private fun setupRecyclerViewGesture() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) > abs(diffY) && abs(diffX) > 100 && abs(velocityX) > 100) {
                    if (diffX > 0) {
                        // Swiped right
                        navigateCategory(-1)
                    } else {
                        // Swiped left
                        navigateCategory(1)
                    }
                    return true
                }
                return false
            }
        })

        binding.recyclerview.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun navigateCategory(direction: Int) {
        val sortedCategories = listOf("All") + categories.sortedDescending()
        val currentIndex = sortedCategories.indexOf(selectedCategory)
        val newIndex = (currentIndex + direction).coerceIn(0, sortedCategories.size - 1)
        filterQuizzesByCategory(sortedCategories[newIndex])
    }
}
