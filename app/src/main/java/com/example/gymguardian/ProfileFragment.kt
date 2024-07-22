package com.example.gymguardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gymguardian.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {
    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentProfileBinding
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        binding.goalsButton.setOnClickListener {
            navigateToGoalFragment()
        }
    }

    private fun saveUserDetails() {
        val weight = binding.weightEditText.text.toString().trim()
        val height = binding.heightEditText.text.toString().trim()
        val age = binding.ageEditText.text.toString().trim()
        val preferredName = binding.preferredNameEditText.text.toString().trim()

        if (weight.isEmpty() || height.isEmpty() || age.isEmpty() || preferredName.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userDetails = hashMapOf(
            "weight" to weight,
            "height" to height,
            "age" to age,
            "preferredName" to preferredName
        )

        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .set(userDetails)
                .addOnSuccessListener {
                    Toast.makeText(context, "Details saved successfully", Toast.LENGTH_SHORT).show()
                    sharedViewModel.setProfileUpdated(true)
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

    private fun navigateToGoalFragment() {
        val fragment = GoalFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
