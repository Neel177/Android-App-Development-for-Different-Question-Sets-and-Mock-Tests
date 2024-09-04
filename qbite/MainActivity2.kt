package com.example.qbite

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.qbite.fragments.CreateProfileFragment
import com.example.qbite.fragments.HomeFragment
import com.example.qbite.fragments.ProfileFragment
import com.example.qbite.fragments.QuizsplashFragment
import com.example.qbite.fragments.UpdateProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.qamar.curvedbottomnaviagtion.CurvedBottomNavigation

class MainActivity2 : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: CurvedBottomNavigation
    public lateinit var adapter: ViewPagerAdapter
    private var backPressedOnce = false
    private var selectedItemId: Int = 1 // Default to HomeFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        // Enable edge-to-edge display
        enableEdgeToEdge()
        // Restore the selected item ID from savedInstanceState
        savedInstanceState?.let {
            selectedItemId = it.getInt("selectedItemId", 1) // Default to HomeFragment
        }
        viewPager = findViewById(R.id.fragmentcontainer)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        // Set up the ViewPager with the adapter
        adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        // Set up bottom navigation
        bottomNavigation.add(CurvedBottomNavigation.Model(1, "Home", R.drawable.ic_home))
        bottomNavigation.add(CurvedBottomNavigation.Model(2, "Tests", R.drawable.ic_book))
        bottomNavigation.add(CurvedBottomNavigation.Model(3, "Profile", R.drawable.ic_profile))
        // Synchronize ViewPager with BottomNavigation
        bottomNavigation.setOnClickMenuListener { item ->
            when (item.id) {
                1 -> viewPager.currentItem = 0
                2 -> viewPager.currentItem = 1
                3 -> checkProfileDetailsAndReplaceFragment()
            }
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigation.show(position + 1)


            }
        })
        // Restore state if available
        if (savedInstanceState == null) {
            viewPager.currentItem = 0 // Default to HomeFragment
        } else {
            viewPager.currentItem = savedInstanceState.getInt("selectedItemId", 0)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current fragment position
        outState.putInt("selectedItemId", viewPager.currentItem)
    }
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentcontainer2)

        if (currentFragment is UpdateProfileFragment) {
            currentFragment.handleOnBackPressed() // Let the fragment handle the back press
        } else {
            if (viewPager.currentItem == 0) {
                if (backPressedOnce) {
                    finishAffinity() // Finish all activities in the task stack
                    return
                }
                backPressedOnce = true
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    backPressedOnce = false
                }, 2000)
            } else {
                viewPager.currentItem = 0
            }
        }
    }


    private fun checkProfileDetailsAndReplaceFragment() {
        // Fetch profile details in the background first
        checkProfileDetails { hasProfileDetails ->
            if (hasProfileDetails) {
                // If profile details exist, show the ProfileFragment
                runOnUiThread {
                    adapter.updateFragment(2, ProfileFragment())
                    viewPager.currentItem = 2
                }
            } else {
                // If profile details do not exist, show the CreateProfileFragment
                runOnUiThread {
                    adapter.updateFragment(2, CreateProfileFragment())
                    viewPager.currentItem = 2
                }
            }
        }
    }

    private fun checkProfileDetails(callback: (Boolean) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()
        // Check if currentUser is not null and has a UID
        currentUser?.uid?.let { uid ->
            // Reference to the document with the user's UID
            val userDocumentRef = db.collection("users").document(uid)
            // Check if the document exists
            userDocumentRef.get().addOnSuccessListener { documentSnapshot ->
                callback(documentSnapshot.exists())
            }.addOnFailureListener { exception ->
                // Error occurred while checking for profile details
                Log.e(TAG, "Error checking profile details: $exception")
                callback(false) // Return false in case of error
            }
        } ?: run {
            // Current user is null or UID is null
            callback(false)
        }
    }
    companion object {
        private const val TAG = "MainActivity2"
    }
}
