package com.example.qbite.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.qbite.MainActivity
import com.example.qbite.R
import com.example.qbite.ViewPagerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private lateinit var textViewFullName: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewPhoneNumber: TextView
    private lateinit var textViewAddress: TextView
    private lateinit var textViewDepartment: TextView
    private lateinit var textViewCourseName: TextView
    private lateinit var textViewSemester: TextView
    private lateinit var logoutButton: Button
    private lateinit var imageViewProfilePhoto: ImageView
    private lateinit var updateProfileButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapter

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        textViewFullName = view.findViewById(R.id.textViewFullName)
        textViewEmail = view.findViewById(R.id.textViewEmail)
        textViewPhoneNumber = view.findViewById(R.id.textViewPhoneNumber)
        textViewAddress = view.findViewById(R.id.textViewAddress)
        textViewDepartment = view.findViewById(R.id.textViewDepartment)
        textViewCourseName = view.findViewById(R.id.textViewCourseName)
        textViewSemester = view.findViewById(R.id.textViewSemester)
        logoutButton = view.findViewById(R.id.logoutButton)
        imageViewProfilePhoto = view.findViewById(R.id.imageViewProfilePhoto)
        updateProfileButton = view.findViewById(R.id.updateProfileButton)
        progressBar = view.findViewById(R.id.progressBar)

        viewPager = requireActivity().findViewById(R.id.fragmentcontainer)
        adapter = viewPager.adapter as ViewPagerAdapter

        showLoading(true)  // Show loading initially
        loadProfileData()
        setupUpdateProfileButton()
        setupLogoutButton()

        return view
    }

    fun loadProfileData() {
        GlobalScope.launch(Dispatchers.Main) {
            currentUser?.uid?.let { uid ->
                val userDocumentRef = db.collection("users").document(uid)
                try {
                    val documentSnapshot = userDocumentRef.get().await()
                    if (documentSnapshot.exists()) {
                        val fullName = documentSnapshot.getString("fullName")
                        val email = currentUser.email
                        val phoneNumber = documentSnapshot.getString("phoneNumber")
                        val address = documentSnapshot.getString("address")
                        val department = documentSnapshot.getString("department")
                        val courseName = documentSnapshot.getString("courseName")
                        val semester = documentSnapshot.getString("semester")
                        val profileImageUrl = documentSnapshot.getString("profileImageUrl")

                        textViewFullName.text = fullName
                        textViewEmail.text = email
                        textViewPhoneNumber.text = phoneNumber
                        textViewAddress.text = address
                        textViewDepartment.text = department
                        textViewCourseName.text = courseName
                        textViewSemester.text = semester

                        if (!profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@ProfileFragment)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.baseline_error_24)
                                .into(imageViewProfilePhoto)
                        }
                    } else {
                    }
                    showLoading(false)  // Hide loading after data is loaded
                } catch (exception: Exception) {
                    showLoading(false)  // Hide loading if an error occurs
                    Toast.makeText(context, "Error loading profile data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        textViewFullName.visibility = if (isLoading) View.GONE else View.VISIBLE
        textViewEmail.visibility = if (isLoading) View.GONE else View.VISIBLE
        textViewPhoneNumber.visibility = if (isLoading) View.GONE else View.VISIBLE
        textViewAddress.visibility = if (isLoading) View.GONE else View.VISIBLE
        textViewDepartment.visibility = if (isLoading) View.GONE else View.VISIBLE
        textViewCourseName.visibility = if (isLoading) View.GONE else View.VISIBLE
        textViewSemester.visibility = if (isLoading) View.GONE else View.VISIBLE
        logoutButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        imageViewProfilePhoto.visibility = if (isLoading) View.GONE else View.VISIBLE
        updateProfileButton.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun setupUpdateProfileButton() {
        updateProfileButton.setOnClickListener {
            val updateProfileFragment = UpdateProfileFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentcontainer2, updateProfileFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupLogoutButton() {
        logoutButton.setOnClickListener {
            vibrateDevice()
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_dialog_layout, null)
            val alertDialogBuilder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
            val alertDialog = alertDialogBuilder.create()
            dialogView.alpha = 0f
            dialogView.animate().alpha(1f).setDuration(500).start()
            dialogView.findViewById<Button>(R.id.positiveButton).setOnClickListener {
                vibrateDevice()
                FirebaseAuth.getInstance().signOut()
                try {
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                alertDialog.dismiss()
            }
            dialogView.findViewById<Button>(R.id.negativeButton).setOnClickListener {
                vibrateDevice()
                alertDialog.dismiss()
            }
            alertDialog.show()
        }
    }

    private fun vibrateDevice() {
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }
}
