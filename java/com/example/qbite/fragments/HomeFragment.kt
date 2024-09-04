package com.example.qbite.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qbite.CustomQuizAdapter
import com.example.qbite.QuizActivity
import com.example.qbite.QuizHistory
import com.example.qbite.QuizHistoryAdapter
import com.example.qbite.QuizModel
import com.example.qbite.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFragment : Fragment() {

    private lateinit var greetingTextView: TextView
    private lateinit var startNewQuizButton: Button
    private lateinit var quizHistoryRecyclerView: RecyclerView
    private lateinit var quizHistoryAdapter: QuizHistoryAdapter
    private lateinit var category1RecyclerView: RecyclerView
    private lateinit var category2RecyclerView: RecyclerView
    private lateinit var category3RecyclerView: RecyclerView
    private lateinit var category1Adapter: CustomQuizAdapter
    private lateinit var category2Adapter: CustomQuizAdapter
    private lateinit var category3Adapter: CustomQuizAdapter
    private lateinit var category1TextView: TextView
    private lateinit var category2TextView: TextView
    private lateinit var category3TextView: TextView

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        initializeViews(view)
        setupRecyclerViews(view)
        checkUserProfile()
        fetchQuizData()
        fetchCustomQuizData()
        setupButtonClickListeners()
        return view
    }

    private fun initializeViews(view: View) {
        greetingTextView = view.findViewById(R.id.greetingTextView)
        startNewQuizButton = view.findViewById(R.id.startNewQuizButton)
        category1TextView = view.findViewById(R.id.category1TextView)
        category2TextView = view.findViewById(R.id.category2TextView)
        category3TextView = view.findViewById(R.id.category3TextView)
    }

    private fun setupRecyclerViews(view: View) {
        // Setup Quiz History RecyclerView
        quizHistoryRecyclerView = view.findViewById(R.id.quizHistoryRecyclerView)
        quizHistoryRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        quizHistoryAdapter = QuizHistoryAdapter(emptyList(), ::deleteQuizHistory)
        quizHistoryRecyclerView.adapter = quizHistoryAdapter

        // Setup Category 1 RecyclerView
        category1RecyclerView = view.findViewById(R.id.category1RecyclerView)
        category1RecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        category1Adapter = CustomQuizAdapter(emptyList())
        category1RecyclerView.adapter = category1Adapter

        // Setup Category 2 RecyclerView
        category2RecyclerView = view.findViewById(R.id.category2RecyclerView)
        category2RecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        category2Adapter = CustomQuizAdapter(emptyList())
        category2RecyclerView.adapter = category2Adapter

        // Setup Category 3 RecyclerView
        category3RecyclerView = view.findViewById(R.id.category3RecyclerView)
        category3RecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        category3Adapter = CustomQuizAdapter(emptyList())
        category3RecyclerView.adapter = category3Adapter
    }

    private fun checkUserProfile() {
        GlobalScope.launch(Dispatchers.Main) {
            currentUser?.uid?.let { uid ->
                val userDocumentRef = db.collection("users").document(uid)
                try {
                    val documentSnapshot = userDocumentRef.get().await()
                    if (documentSnapshot.exists()) {
                        val fullName = documentSnapshot.getString("fullName")
                        val firstName = fullName?.split(" ")?.getOrNull(0)
                        greetingTextView.text = "Hello, $firstName!"
                    } else {
                        // Handle profile not found
                    }
                } catch (exception: Exception) {
                    // Handle exceptions
                }
            }
        }
    }

    private fun fetchQuizData() {
        GlobalScope.launch(Dispatchers.Main) {
            currentUser?.uid?.let { uid ->
                val quizHistoryRef = database.getReference("users/$uid/quizHistory")
                quizHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val quizHistoryList = mutableListOf<QuizHistory>()
                        for (quizSnapshot in dataSnapshot.children) {
                            val quizHistory = quizSnapshot.getValue(QuizHistory::class.java)
                            if (quizHistory != null) {
                                quizHistoryList.add(quizHistory)
                            }
                        }
                        if (quizHistoryList.isNotEmpty()) {
                            quizHistoryAdapter.updateData(quizHistoryList)
                        } else {
                            Log.d("HomeFragment", "No quiz history found for user.")
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("HomeFragment", "Error fetching quiz history: ${databaseError.message}")
                    }
                })
            }
        }
    }

    private fun fetchCustomQuizData() {
        FirebaseDatabase.getInstance().reference
            .get()
            .addOnSuccessListener { dataSnapshot ->
                val quizModelList = mutableListOf<QuizModel>()
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        // Check if the snapshot key is not "users"
                        if (snapshot.key != "users") {
                            val quizModel = snapshot.getValue(QuizModel::class.java)
                            quizModel?.let { quizModelList.add(it) }
                        }
                    }
                    // Categorize quizzes and update RecyclerViews
                    categorizeAndDisplayQuizzes(quizModelList)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error fetching data from Firebase", exception)
                // Handle failure, show error message, etc.
            }
    }


    private fun categorizeAndDisplayQuizzes(quizModelList: List<QuizModel>) {
        // Extract unique categories
        val uniqueCategories = quizModelList.map { it.category }.distinct()

        // Randomly select three unique categories
        val selectedCategories = uniqueCategories.shuffled().take(3)

        // Filter quizzes based on the selected categories
        val category1Quizzes = quizModelList.filter { it.category == selectedCategories[0] }
        val category2Quizzes = quizModelList.filter { it.category == selectedCategories[1] }
        val category3Quizzes = quizModelList.filter { it.category == selectedCategories[2] }

        category1TextView.text = selectedCategories[0]
        category2TextView.text = selectedCategories[1]
        category3TextView.text = selectedCategories[2]
        // Update adapters with the filtered data
        category1Adapter.updateData(category1Quizzes)
        category2Adapter.updateData(category2Quizzes)
        category3Adapter.updateData(category3Quizzes)
    }

    private fun deleteQuizHistory(quizHistory: QuizHistory) {
        currentUser?.uid?.let { uid ->
            val quizHistoryRef = database.getReference("users/$uid/quizHistory/datetime/${quizHistory.quizModelId}")
            quizHistoryRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Refresh the data after deletion
                    fetchQuizData()
                } else {
                    Log.e("HomeFragment", "Error deleting quiz history: ${task.exception?.message}")
                }
            }
        }
    }
    private fun setupButtonClickListeners() {
        startNewQuizButton.setOnClickListener {
            val intent = Intent(requireContext(), QuizActivity::class.java)
            startActivity(intent)
        }
    }
}
