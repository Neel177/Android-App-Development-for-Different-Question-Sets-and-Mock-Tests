package com.example.qbite.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.qbite.R
import com.example.qbite.ViewPagerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File

class UpdateProfileFragment : Fragment() {

    private lateinit var editTextFullName: EditText
    private lateinit var editTextPhoneNumber: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextDepartment: EditText
    private lateinit var editTextCourseName: EditText
    private lateinit var editTextSemester: EditText
    private lateinit var buttonSaveProfile: Button
    private lateinit var imageViewProfilePhoto: ImageView
    private lateinit var progressBarProfile: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val storageRef = FirebaseStorage.getInstance().reference

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_update_profile, container, false)

        editTextFullName = view.findViewById(R.id.editTextFullName)
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber)
        editTextAddress = view.findViewById(R.id.editTextAddress)
        editTextDepartment = view.findViewById(R.id.editTextDepartment)
        editTextCourseName = view.findViewById(R.id.editTextCourseName)
        editTextSemester = view.findViewById(R.id.editTextSemester)
        buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile)
        imageViewProfilePhoto = view.findViewById(R.id.imageViewProfilePhoto)
        progressBarProfile = view.findViewById(R.id.progressBarProfile)

        // Set the listeners to navigate between EditTexts
        setEditTextNavigation()

        // Fetch and display current user details
        fetchAndDisplayCurrentUserDetails()

        buttonSaveProfile.setOnClickListener {
            updateProfile()
        }

        imageViewProfilePhoto.setOnClickListener {
            dispatchTakePictureIntent()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        Log.d("UpdateProfileFragment", "onCreateView executed")

        return view
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            requireActivity().supportFragmentManager.beginTransaction().remove(this@UpdateProfileFragment).commit()
        }
    }
    fun handleOnBackPressed() {
        callback.handleOnBackPressed()
    }



    private fun setEditTextNavigation() {
        editTextFullName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                true
            } else {
                false
            }
        }

        editTextPhoneNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                editTextAddress.requestFocus()
                true
            } else {
                false
            }
        }
        editTextAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                editTextDepartment.requestFocus()
                true
            } else {
                false
            }
        }
        editTextDepartment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                editTextCourseName.requestFocus()
                true
            } else {
                false
            }
        }
        editTextCourseName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                editTextSemester.requestFocus()
                true
            } else {
                false
            }
        }
        editTextSemester.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateProfile()
                true
            } else {
                false
            }
        }
    }

    private fun fetchAndDisplayCurrentUserDetails() {
        progressBarProfile.visibility = View.VISIBLE

        currentUser?.uid?.let { uid ->
            val userDocumentRef = db.collection("users").document(uid)
            userDocumentRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    progressBarProfile.visibility = View.GONE

                    if (documentSnapshot.exists()) {
                        val fullName = documentSnapshot.getString("fullName")
                        val phoneNumber = documentSnapshot.getString("phoneNumber")
                        val address = documentSnapshot.getString("address")
                        val department = documentSnapshot.getString("department")
                        val courseName = documentSnapshot.getString("courseName")
                        val semester = documentSnapshot.getString("semester")
                        val profileImageUrl = documentSnapshot.getString("profileImageUrl")

                        editTextFullName.setText(fullName)
                        editTextPhoneNumber.setText(phoneNumber)
                        editTextAddress.setText(address)
                        editTextDepartment.setText(department)
                        editTextCourseName.setText(courseName)
                        editTextSemester.setText(semester)
                        if (!profileImageUrl.isNullOrEmpty()) {
                            // Example using Glide:
                            Glide.with(this@UpdateProfileFragment)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_profile) // Placeholder image while loading
                                .error(R.drawable.baseline_error_24) // Error image if loading fails
                                .into(imageViewProfilePhoto)
                        }
                    } else {
                        // Handle the case where there is no existing profile
                        Toast.makeText(requireContext(), "No existing profile found. Please fill in your details.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    progressBarProfile.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error fetching profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { pickPhotoIntent ->
            pickPhotoIntent.resolveActivity(requireActivity().packageManager)?.also {
                startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                startCrop(uri)
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            imageViewProfilePhoto.setImageURI(resultUri)
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(data!!)
            Toast.makeText(requireContext(), "Error cropping image: $error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationFileName = "cropped_image.jpg"
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, destinationFileName))

        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .start(requireContext(), this)
    }

    private fun updateProfile() {
        progressBarProfile.visibility = View.VISIBLE

        val fullName = editTextFullName.text.toString().trim()
        val phoneNumber = editTextPhoneNumber.text.toString().trim()
        val address = editTextAddress.text.toString().trim()
        val department = editTextDepartment.text.toString().trim()
        val courseName = editTextCourseName.text.toString().trim()
        val semester = editTextSemester.text.toString().trim()

        if (fullName.isEmpty() ||   phoneNumber.isEmpty() || address.isEmpty() || department.isEmpty() || courseName.isEmpty() || semester.isEmpty()) {
            progressBarProfile.visibility = View.GONE
            Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val profileData = hashMapOf(
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "address" to address,
            "department" to department,
            "courseName" to courseName,
            "semester" to semester
        )

        val profileRef = currentUser?.uid?.let { storageRef.child("images/$it.jpg") }
        val imageViewProfilePhoto = imageViewProfilePhoto
        imageViewProfilePhoto.isDrawingCacheEnabled = true
        imageViewProfilePhoto.buildDrawingCache()
        val bitmap = (imageViewProfilePhoto.drawable).toBitmap()
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = profileRef?.putBytes(data)
        uploadTask?.addOnSuccessListener {
            profileRef.downloadUrl.addOnSuccessListener { uri ->
                profileData["profileImageUrl"] = uri.toString()

                currentUser?.uid?.let { uid ->
                    val userDocumentRef = db.collection("users").document(uid)

                    // Using set instead of update to create the document if it doesn't exist
                    userDocumentRef.set(profileData)
                        .addOnSuccessListener {
                            progressBarProfile.visibility = View.GONE
                            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            hideUpdateProfileFragment()
                            reloadProfileFragmentData()
                        }
                        .addOnFailureListener { exception ->
                            progressBarProfile.visibility = View.GONE
                            Toast.makeText(requireContext(), "Error updating profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }?.addOnFailureListener {
            progressBarProfile.visibility = View.GONE
            Toast.makeText(requireContext(), "Error uploading image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideUpdateProfileFragment() {
        // Close or hide the UpdateProfileFragment
        requireActivity().supportFragmentManager.beginTransaction()
            .remove(this)  // Remove the fragment from the container
            .commit()
    }

    private fun reloadProfileFragmentData() {
        // Get reference to the ViewPager2 containing the ProfileFragment
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.fragmentcontainer)

        // Get the ProfileFragment from the adapter
        val profileFragment = (viewPager.adapter as? ViewPagerAdapter)?.fragments?.get(2) as? ProfileFragment

        // Reload data in the ProfileFragment if it's found
        profileFragment?.loadProfileData()
    }

}
