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

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        loadUserProfile()

        binding.saveButton.setOnClickListener {
            saveOrUpdateUserDetails()
        }

        binding.goalsButton.setOnClickListener {
            // Navighează la GoalFragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, GoalFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (_binding != null && document != null && document.exists()) {
                        binding.preferredNameEditText.setText(document.getString("preferredName"))
                        binding.weightEditText.setText(document.getString("weight"))
                        binding.heightEditText.setText(document.getString("height"))
                        binding.ageEditText.setText(document.getString("age"))

                        binding.saveButton.text = "Update"
                    }
                }
                .addOnFailureListener { e ->
                    if (_binding != null) {
                        Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveOrUpdateUserDetails() {
        if (_binding == null) return

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
                    if (_binding != null) {
                        Toast.makeText(context, "Details saved successfully", Toast.LENGTH_SHORT).show()
                        sharedViewModel.setProfileUpdated(true)
                        binding.saveButton.text = "Update"
                    }
                }
                .addOnFailureListener { e ->
                    if (_binding != null) {
                        Toast.makeText(context, "Failed to save details: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
