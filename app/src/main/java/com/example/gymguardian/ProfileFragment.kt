package com.example.gymguardian

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.gymguardian.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {
    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentProfileBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using binding
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.saveButton.setOnClickListener {
            saveUserDetails()
        }
    }

    private fun saveUserDetails() {
        val weight = binding.weightEditText.text.toString().trim()
        val height = binding.heightEditText.text.toString().trim()
        val age = binding.ageEditText.text.toString().trim()
        val preferredName = binding.preferredName.text.toString().trim()
        val dailyCalories = binding.caloriesEditText.text.toString().trim()

        if (weight.isEmpty() || height.isEmpty() || age.isEmpty() || preferredName.isEmpty() || dailyCalories.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userDetails = hashMapOf(
            "preferredName" to preferredName,
            "weight" to weight,
            "height" to height,
            "age" to age,
            "dailyCalories" to dailyCalories
        )

        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .set(userDetails)
                .addOnSuccessListener {
                    Toast.makeText(context, "Details saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Failed to save details: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}