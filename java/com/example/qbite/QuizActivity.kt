package com.example.qbite

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.qbite.databinding.ActivityQuizBinding
import com.example.qbite.fragments.AboutFragment
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.abs

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private lateinit var quizModelList: MutableList<QuizModel>
    private lateinit var filteredQuizModelList: MutableList<QuizModel>
    private lateinit var adapter: QuizListAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var overflowMenuIndicator: ImageButton
    private val categories = mutableSetOf<String>()
    private var selectedCategory: String = "All"
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var transparentView: View



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        binding.main.fitsSystemWindows = false
        setTheme(R.style.quizactivity)
        setContentView(binding.root)
        quizModelList = mutableListOf()
        filteredQuizModelList = mutableListOf()

        onBackPressedDispatcher.addCallback(this, callback)
        transparentView = findViewById<View>(R.id.transparentView)
        transparentView.setOnClickListener { /* Do nothing, just intercept clicks */ }


        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        overflowMenuIndicator = findViewById(R.id.overflow_menu_indicator)
        toolbar.overflowIcon = null
        toolbar.overflowIcon = overflowMenuIndicator.drawable

        // Initialize swipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            getDataFromFireBase()
        }

        // Call getDataFromFirebase after initializing swipeRefreshLayout
        getDataFromFireBase()
        setupRecyclerView()
        setupRecyclerViewGesture()
        overflowMenuIndicator.setOnClickListener {
            Log.d("Menu", "Overflow menu clicked")
            openOptionsMenu()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d("QuizActivity", "onCreateOptionsMenu called")
        menuInflater.inflate(R.menu.menu_overflow, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_about -> {
                navigateToAboutFragment()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToAboutFragment() {
        transparentView.visibility = View.VISIBLE

        supportFragmentManager.commit {
            replace(R.id.fragment_container, AboutFragment())
            addToBackStack(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = QuizListAdapter(filteredQuizModelList)
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.adapter = adapter
        binding.backbtn.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.nav_slide_out_right, R.anim.nav_slide_in_left)
        }
    }

    private fun getDataFromFireBase() {
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

        val buttonLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 8, 8, 8)
        }

        val allButton = Button(this).apply {
            text = "All"
            typeface = resources.getFont(R.font.myrobotobold)
            setOnClickListener { filterQuizzesByCategory("All") }
            setTextColor(resources.getColor(R.color.white, theme))
            setBackgroundResource(R.drawable.ripple_rounded_gradient_background)
            layoutParams = buttonLayoutParams
            tag = "All"
        }
        categoryLayout.addView(allButton)

        val sortedCategories = categories.sortedDescending()
        for (category in sortedCategories) {
            val button = Button(this).apply {
                text = category
                typeface = resources.getFont(R.font.myrobotobold)
                setOnClickListener { filterQuizzesByCategory(category) }
                setTextColor(resources.getColor(android.R.color.white, theme))
                setBackgroundResource(R.drawable.ripple_rounded_gradient_background)
                layoutParams = buttonLayoutParams
                tag = category
            }
            categoryLayout.addView(button)
        }

        val space = Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20.dpToPx()
            )
        }
        categoryLayout.addView(space)

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


    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val intent = Intent(this@QuizActivity, MainActivity2::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.nav_slide_out_right, R.anim.nav_slide_in_left)
        }
    }
    fun hideTransparentView() {
        transparentView.visibility = View.GONE
    }
}
