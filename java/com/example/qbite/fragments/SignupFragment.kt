package com.example.qbite.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation

import com.example.qbite.MainActivity2
import com.example.qbite.R
import com.example.qbite.admin
import com.example.qbite.databinding.FragmentSignupBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var navControl: NavController
    private lateinit var binding: FragmentSignupBinding
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        registerEvents()
    }

    private fun init(view: View){
        navControl=Navigation.findNavController(view)
        auth=FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set the switch to user as default
        binding.userAdminSwitch.isChecked = true
    }

    private fun registerEvents(){
        binding.authTextView.setOnClickListener {
            navControl.navigate(R.id.action_signupFragment_to_signinFragment)
        }

        binding.nextBtn.setOnClickListener {
            val email= binding.emailEt.text.toString().trim()
            val pass = binding.passEt.text.toString().trim()
            val verifyPass = binding.repassEt.text.toString().trim()
            val isAdmin = binding.userAdminSwitch.isChecked // Check if admin switch is checked

            if(email.isNotEmpty() && pass.isNotEmpty() && verifyPass.isNotEmpty()){
                if (pass == verifyPass){
                    val progressBar = requireView().findViewById<ProgressBar>(R.id.progressBar1)
                    progressBar.visibility = View.VISIBLE

                    registerUser(email, pass, isAdmin)
                } else {
                    Toast.makeText(context,"Passwords did not match",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun registerUser(email: String, pass: String, isAdmin: Boolean) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                val role = if (isAdmin) "admin" else "user"
                saveUserRole(email, role)
            } else {
                // Handle registration failure
                Toast.makeText(context, task.exception?.message, Toast.LENGTH_SHORT).show()
                val progressBar = requireView().findViewById<ProgressBar>(R.id.progressBar1)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveUserRole(email: String, role: String) {
        val userRole = hashMapOf(
            "email" to email,
            "role" to role
        )

        // Save the user's role to Firestore
        firestore.collection("userRoles").document(email)
            .set(userRole)
            .addOnSuccessListener {
                // Role saved successfully
                // Redirect to the appropriate activity based on role
                if (role == "admin") {
                    val intent = Intent(requireContext(), admin::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(requireContext(), MainActivity2::class.java)
                    startActivity(intent)
                }
                requireActivity().finish()
            }
            .addOnFailureListener { e ->
                // Handle failure
                Toast.makeText(context, "Failed to save user role: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
