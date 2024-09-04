package com.example.qbite

import android.content.ContentValues.TAG
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.qbite.fragments.HomeFragment
import com.example.qbite.fragments.ProfileFragment
import com.example.qbite.fragments.QuizsplashFragment

class ViewPagerAdapter(activity2: FragmentActivity) : FragmentStateAdapter(activity2) {
    val fragments = mutableListOf<Fragment>(
        HomeFragment(),
        QuizsplashFragment(),
        ProfileFragment() // Default fragment for the third tab
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun removeFragment(position: Int) {
        if (position < fragments.size) {
            fragments.removeAt(position)
            notifyDataSetChanged() // Notify the adapter that the data set has changed
        }
    }

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
        notifyDataSetChanged() // Notify the adapter that the data set has changed
    }

    fun updateFragment(position: Int, fragment: Fragment) {
        if (position < fragments.size) {
            Log.d(TAG, "Updating fragment at position: $position")
            fragments[position] = fragment
            notifyItemChanged(position)
        }
    }
}
