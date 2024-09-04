package com.example.qbite.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.qbite.MainActivity2
import com.example.qbite.R
import com.example.qbite.admin
import com.example.qbite.databinding.FragmentSigninBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SigninFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var binding: FragmentSigninBinding
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSigninBinding.inflate(inflater, container, false)

        changeStatusBarColor("#024DFF")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        registerEvents()
    }

    private fun init(view: View){
        navControl= Navigation.findNavController(view)
        auth=FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set the switch to user as default
        binding.userAdminSwitch.isChecked = true
    }

    private fun registerEvents(){
        binding.authTextView.setOnClickListener {
            navControl.navigate(R.id.action_signinFragment_to_signupFragment)
        }

        binding.nextBtn.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passEt.text.toString().trim()
            val progressBar = requireView().findViewById<ProgressBar>(R.id.progressBar1)
            val selectedRole = if (binding.userAdminSwitch.isChecked) "admin" else "user"

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE

                auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(
                    OnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Fetch user role from Firestore
                            fetchUserRole(email, selectedRole)
                        } else {
                            Toast.makeText(context, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                        }
                    })

            } else {
                Toast.makeText(context, "Empty fields are not allowed", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        }

    }

    private fun fetchUserRole(email: String, selectedRole: String) {
        val progressBar = requireView().findViewById<ProgressBar>(R.id.progressBar1)

        // Fetch user role from Firestore
        firestore.collection("userRoles").document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    if (role != null) {
                        if (selectedRole == "admin" && role == "admin") {
                            // Redirect to AdminActivity
                            val intent = Intent(requireContext(), admin::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        } else if (selectedRole == "user" && role == "user") {
                            // Redirect to MainActivity2
                            val intent = Intent(requireContext(), MainActivity2::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        } else if (selectedRole == "admin" && role == "user") {
                            // Show message that it's not an admin account
                            Toast.makeText(context, "This is not an admin account. Please register as an admin.", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                        } else {
                            val intent = Intent(requireContext(), MainActivity2::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                            progressBar.visibility = View.GONE
                        }
                    } else {
                        // User role not found
                        Toast.makeText(context, "User role not found", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                } else {
                    // Document not found
                    Toast.makeText(context, "Document not found", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                // Handle failure
                Toast.makeText(context, "Failed to fetch user role: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }


    private fun changeStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = requireActivity().window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.parseColor(color)
        }
    }
}
