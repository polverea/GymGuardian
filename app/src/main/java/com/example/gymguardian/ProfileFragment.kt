package com.example.gymguardian

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gymguardian.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

        // Dezactivează butonul "Set Goals" inițial
        binding.goalsButton.isEnabled = false
        binding.goalsButton.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))

        loadUserProfile()

        binding.apply {
            saveButton.setOnClickListener { saveOrUpdateUserDetails() }
            goalsButton.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, GoalFragment())
                    .addToBackStack(null)
                    .commit()
            }
            signOutButton.setOnClickListener {
                auth.signOut()
                val intent = Intent(requireContext(), Login::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (_binding != null && document != null && document.exists()) {
                        with(binding) {
                            preferredNameEditText.setText(document.getString("preferredName"))
                            weightEditText.setText(document.getString("weight"))
                            heightEditText.setText(document.getString("height"))
                            ageEditText.setText(document.getString("age"))
                            saveButton.text = "Update"
                            enableGoalsButtonIfAllFieldsAreFilled() // Verifică și activează butonul "Set Goals"
                        }
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
                .set(userDetails, SetOptions.merge())
                .addOnSuccessListener {
                    if (_binding != null) {
                        Toast.makeText(context, "Details saved successfully", Toast.LENGTH_SHORT).show()
                        sharedViewModel.setProfileUpdated(true)
                        binding.saveButton.text = "Update"
                        enableGoalsButtonIfAllFieldsAreFilled()  // Verifică dacă toate câmpurile sunt completate și activează butonul "Set Goals"
                    }
                }
                .addOnFailureListener { e ->
                    if (_binding != null) {
                        Toast.makeText(context, "Failed to save details: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun enableGoalsButtonIfAllFieldsAreFilled() {
        val weight = binding.weightEditText.text.toString().trim()
        val height = binding.heightEditText.text.toString().trim()
        val age = binding.ageEditText.text.toString().trim()
        val preferredName = binding.preferredNameEditText.text.toString().trim()

        val allFieldsFilled = weight.isNotEmpty() && height.isNotEmpty() && age.isNotEmpty() && preferredName.isNotEmpty()

        if (allFieldsFilled) {
            binding.goalsButton.isEnabled = true
            binding.goalsButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lavender))
        } else {
            binding.goalsButton.isEnabled = false
            binding.goalsButton.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
