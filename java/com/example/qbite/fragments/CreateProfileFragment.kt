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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.qbite.R
import com.example.qbite.ViewPagerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*
import com.yalantis.ucrop.UCrop
import java.io.File





class CreateProfileFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_create_profile, container, false)

        editTextFullName = view.findViewById(R.id.editTextFullName)
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber)
        editTextAddress = view.findViewById(R.id.editTextAddress)
        editTextDepartment = view.findViewById(R.id.editTextDepartment)
        editTextCourseName = view.findViewById(R.id.editTextCourseName)
        editTextSemester = view.findViewById(R.id.editTextSemester)
        buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile)
        imageViewProfilePhoto = view.findViewById(R.id.imageViewProfilePhoto)
        progressBarProfile = view.findViewById(R.id.progressBarProfile)


        buttonSaveProfile.setOnClickListener {
            saveProfile()
        }

        imageViewProfilePhoto.setOnClickListener {
            dispatchTakePictureIntent()
        }

        return view
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
        Log.d("CreateProfileFragment", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
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
        Log.d("CreateProfileFragment", "startCrop: uri=$uri")
        val destinationFileName = "cropped_image.jpg"
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, destinationFileName))

        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f) // Set cropping aspect ratio to 1:1
            .start(requireContext(), this)
    }




    private fun saveProfile() {
        progressBarProfile.visibility = View.VISIBLE
        val fullName = editTextFullName.text.toString().trim()
        val phoneNumber = editTextPhoneNumber.text.toString().trim()
        val address = editTextAddress.text.toString().trim()
        val department = editTextDepartment.text.toString().trim()
        val courseName = editTextCourseName.text.toString().trim()
        val semester = editTextSemester.text.toString().trim()

        if (fullName.isEmpty() ||  phoneNumber.isEmpty() ||
            address.isEmpty() || department.isEmpty() || courseName.isEmpty() || semester.isEmpty()) {
            progressBarProfile.visibility = View.GONE
            // Handle case where fields are empty
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
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

                    userDocumentRef.set(profileData)
                        .addOnSuccessListener {
                            progressBarProfile.visibility = View.GONE
                            // Profile saved successfully, show toast and replace fragment
                            navigateToProfileFragment()

                            Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            progressBarProfile.visibility = View.GONE
                            // Error occurred while saving profile data
                            Toast.makeText(requireContext(), "Error saving profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    private fun navigateToProfileFragment() {
        try {
            val viewPager = requireActivity().findViewById<ViewPager2>(R.id.fragmentcontainer)
            val adapter = viewPager.adapter as ViewPagerAdapter

            // Find the position of CreateProfileFragment and remove it
            val position = adapter.fragments.indexOf(this)
            if (position != -1) {
                adapter.removeFragment(position)
            }

            // Add ProfileFragment to the adapter
            val profileFragment = ProfileFragment()
            adapter.addFragment(profileFragment)

            // Optionally, navigate to the newly added ProfileFragment
            viewPager.currentItem = adapter.fragments.size - 1
        } catch (e: Exception) {
            Log.e("CreateProfileFragment", "Error navigating to ProfileFragment: ${e.message}", e)
            Toast.makeText(requireContext(), "Error navigating to ProfileFragment", Toast.LENGTH_SHORT).show()
        }
    }
}
