package com.example.qbite.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigation

import com.example.qbite.MainActivity2
import com.example.qbite.R
import com.example.qbite.admin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot

class SplashFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        auth = FirebaseAuth.getInstance()
        navController = Navigation.findNavController(view)
        firestore = FirebaseFirestore.getInstance()




        Handler(Looper.myLooper()!!).postDelayed(Runnable {

            if (auth.currentUser != null) {
                // User is logged in
                fetchUserRole(auth.currentUser!!.email ?: "")
            } else {
                // User is not logged in, navigate to signinFragment
                navController.navigate(
                    R.id.action_splashFragment_to_signinFragment,
                    null,
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.fade_in)
                        .setExitAnim(R.anim.fade_out)
                        .setPopEnterAnim(R.anim.fade_in)
                        .setPopExitAnim(R.anim.fade_out)
                        .build()
                )
            }
        }, 2000)
    }

    private fun fetchUserRole(email: String) {
        firestore.collection("userRoles").document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    if (role != null) {
                        if (role == "admin") {
                            // Redirect to AdminActivity
                            val intent = Intent(requireContext(), admin::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            // Redirect to MainActivity2
                            val intent = Intent(requireContext(), MainActivity2::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    } else {
                        // User role not found
                        navigateToSignin()
                    }
                } else {
                    // Document not found
                    navigateToSignin()
                }
            }
            .addOnFailureListener { e ->
                // Handle failure
                navigateToSignin()
            }
    }

    private fun navigateToSignin() {
        navController.navigate(
            R.id.action_splashFragment_to_signinFragment,
            null,
            NavOptions.Builder()
                .setEnterAnim(R.anim.fade_in)
                .setExitAnim(R.anim.fade_out)
                .setPopEnterAnim(R.anim.fade_in)
                .setPopExitAnim(R.anim.fade_out)
                .build()
        )
    }
}
