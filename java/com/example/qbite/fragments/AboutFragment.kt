package com.example.qbite.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.qbite.QuizActivity
import com.example.qbite.R

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle back button press within the fragment
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        (requireActivity() as QuizActivity).hideTransparentView()

    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Remove the fragment when back button is pressed
            requireActivity().supportFragmentManager.beginTransaction().remove(this@AboutFragment).commit()
            (requireActivity() as QuizActivity).hideTransparentView()



        }
    }
}
