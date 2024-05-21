package com.example.gymguardian

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.gymguardian.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Dezactivează BottomNavigationView la început
        binding.bottomNavigationView.isEnabled = false

        binding.SignOutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            if (binding.bottomNavigationView.isEnabled) {
                when (menuItem.itemId) {
                    R.id.home -> {
                        replaceFragment(HomeFragment())
                        true
                    }
                    R.id.profile -> {
                        replaceFragment(ProfileFragment())
                        true
                    }
                    R.id.food -> {
                        replaceFragment(FoodFragment())
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }

        // Observă schimbările în profilul utilizatorului
        sharedViewModel.profileUpdated.observe(this, Observer { updated ->
            if (updated) {
                checkUserProfile()
            }
        })

        checkUserProfile()
    }

    override fun onStart() {
        super.onStart()
        checkUserProfile()
    }

    private fun checkUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("UsersInfo").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("preferredName") != null) {
                        // Informațiile sunt completate, redirecționează la Pagina Principală (Home)
                        replaceFragment(HomeFragment())
                        binding.bottomNavigationView.isEnabled = true
                    } else {
                        // Informațiile nu sunt completate, redirecționează la Pagina de Profil
                        replaceFragment(ProfileFragment())
                        binding.bottomNavigationView.menu.findItem(R.id.profile).isChecked = true
                        binding.bottomNavigationView.isEnabled = false
                    }
                }
                .addOnFailureListener {
                    // În caz de eroare, redirecționează la Pagina de Profil
                    replaceFragment(ProfileFragment())
                    binding.bottomNavigationView.menu.findItem(R.id.profile).isChecked = true
                    binding.bottomNavigationView.isEnabled = false
                }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
    }
}
