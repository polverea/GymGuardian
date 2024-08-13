package com.example.gymguardian

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.gymguardian.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        replaceFragment(HomeFragment())
        checkUserProfile()
    }

    private fun checkUserProfile() {
        val currentUser = auth.currentUser
        currentUser?.let {
            db.collection("UsersInfo").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val preferredName = document.getString("preferredName")
                        val hasGoals = document.getString("caloriesGoal") != null &&
                                document.getString("carbsGoal") != null &&
                                document.getString("proteinGoal") != null &&
                                document.getString("fatGoal") != null

                        if (preferredName.isNullOrEmpty() || !hasGoals) {
                            replaceFragment(ProfileFragment())
                            disableNavigation()
                        } else {
                            enableNavigation()
                        }
                    } else {
                        replaceFragment(ProfileFragment())
                        disableNavigation()
                    }
                }
                .addOnFailureListener {
                    replaceFragment(ProfileFragment())
                    disableNavigation()
                }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commitNowAllowingStateLoss()
    }

    private fun disableNavigation() {
        binding.bottomNavigationView.menu.findItem(R.id.home).isEnabled = false
        binding.bottomNavigationView.menu.findItem(R.id.food).isEnabled = false
    }

    fun enableNavigation() {
        binding.bottomNavigationView.menu.findItem(R.id.home).isEnabled = true
        binding.bottomNavigationView.menu.findItem(R.id.food).isEnabled = true
    }

    override fun onStart() {
        super.onStart()

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    if (menuItem.isEnabled) {
                        replaceFragment(HomeFragment())
                    }
                    true
                }
                R.id.profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                R.id.food -> {
                    if (menuItem.isEnabled) {
                        replaceFragment(FoodFragment())
                    }
                    true
                }
                else -> false
            }
        }
    }
}
