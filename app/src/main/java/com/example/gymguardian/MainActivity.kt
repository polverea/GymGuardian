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

        // Setează fragmentul implicit înainte de a verifica profilul utilizatorului
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
                            // Navighează către ProfileFragment dacă profilul nu este complet
                            replaceFragment(ProfileFragment())
                            disableNavigation()
                        } else {
                            // Asigură-te că navigarea este activată dacă totul este complet
                            enableNavigation()
                        }
                    } else {
                        // Documentul nu există, deci navighează către ProfileFragment
                        replaceFragment(ProfileFragment())
                        disableNavigation()
                    }
                }
                .addOnFailureListener {
                    // În caz de eșec, navighează către ProfileFragment și dezactivează navigarea
                    replaceFragment(ProfileFragment())
                    disableNavigation()
                }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commitNowAllowingStateLoss()  // Asigură înlocuirea imediată a fragmentului
    }

    private fun disableNavigation() {
        binding.bottomNavigationView.menu.findItem(R.id.home).isEnabled = false
        binding.bottomNavigationView.menu.findItem(R.id.food).isEnabled = false
    }

    private fun enableNavigation() {
        binding.bottomNavigationView.menu.findItem(R.id.home).isEnabled = true
        binding.bottomNavigationView.menu.findItem(R.id.food).isEnabled = true
    }

    // Navigarea între fragmente din BottomNavigationView
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
