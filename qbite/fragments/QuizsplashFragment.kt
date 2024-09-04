package com.example.qbite.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.qbite.QuizActivity
import com.example.qbite.R

class QuizsplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_quizsplash, container, false)

        val imageView3 = view.findViewById<ImageView>(R.id.imageView3)
        val letsGoTextView = view.findViewById<TextView>(R.id.letsGoTextView)

        // Set onClickListener for imageView3
        imageView3.setOnClickListener {
            navigateToQuizActivity()
        }

        // Set onClickListener for letsGoTextView
        letsGoTextView.setOnClickListener {
            navigateToQuizActivity()
        }

        return view
    }

    private fun navigateToQuizActivity() {
        val intent = Intent(activity, QuizActivity::class.java)
        startActivity(intent)
        activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

}
