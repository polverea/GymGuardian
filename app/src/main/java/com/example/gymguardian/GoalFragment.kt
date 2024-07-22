package com.example.gymguardian

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gymguardian.databinding.FragmentGoalBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GoalFragment : Fragment() {
    private var _binding: FragmentGoalBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        binding.saveGoalsButton.setOnClickListener {
            saveGoals()
        }

        // Adăugăm un text watcher pentru a calcula calorii
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateCalories()
            }
        }

        binding.caloriesGoalEditText.addTextChangedListener(textWatcher)
        binding.carbsGoalEditText.addTextChangedListener(textWatcher)
        binding.proteinGoalEditText.addTextChangedListener(textWatcher)
        binding.fatGoalEditText.addTextChangedListener(textWatcher)

        // Încărcăm valorile existente
        loadGoals()
    }

    private fun loadGoals() {
        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val caloriesGoal = document.getString("caloriesGoal") ?: "0"
                        val carbsGoal = document.getString("carbsGoal") ?: "0"
                        val proteinGoal = document.getString("proteinGoal") ?: "0"
                        val fatGoal = document.getString("fatGoal") ?: "0"

                        binding.caloriesGoalEditText.setText(caloriesGoal)
                        binding.carbsGoalEditText.setText(carbsGoal)
                        binding.proteinGoalEditText.setText(proteinGoal)
                        binding.fatGoalEditText.setText(fatGoal)

                        calculateCalories()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load goals: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun calculateCalories() {
        val caloriesGoal = binding.caloriesGoalEditText.text.toString().toIntOrNull() ?: 0
        val carbs = binding.carbsGoalEditText.text.toString().toIntOrNull() ?: 0
        val protein = binding.proteinGoalEditText.text.toString().toIntOrNull() ?: 0
        val fat = binding.fatGoalEditText.text.toString().toIntOrNull() ?: 0

        val carbsCalories = carbs * 4
        val proteinCalories = protein * 4
        val fatCalories = fat * 9

        binding.carbsCaloriesTextView.text = "Carbs: $carbs g (${carbsCalories} kcal)"
        binding.proteinCaloriesTextView.text = "Protein: $protein g (${proteinCalories} kcal)"
        binding.fatCaloriesTextView.text = "Fat: $fat g (${fatCalories} kcal)"

        val totalCalories = carbsCalories + proteinCalories + fatCalories
        binding.totalCaloriesTextView.text = "Total: $totalCalories kcal of $caloriesGoal kcal"

        if (totalCalories > caloriesGoal) {
            binding.totalCaloriesTextView.setTextColor(Color.RED)
            binding.errorTextView.visibility = View.VISIBLE
        } else {
            binding.totalCaloriesTextView.setTextColor(Color.WHITE)
            binding.errorTextView.visibility = View.GONE
        }
    }

    private fun saveGoals() {
        val caloriesGoal = binding.caloriesGoalEditText.text.toString().trim()
        val carbsGoal = binding.carbsGoalEditText.text.toString().trim()
        val proteinGoal = binding.proteinGoalEditText.text.toString().trim()
        val fatGoal = binding.fatGoalEditText.text.toString().trim()

        if (caloriesGoal.isEmpty() || carbsGoal.isEmpty() || proteinGoal.isEmpty() || fatGoal.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val goals = hashMapOf(
            "caloriesGoal" to caloriesGoal,
            "carbsGoal" to carbsGoal,
            "proteinGoal" to proteinGoal,
            "fatGoal" to fatGoal
        )

        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .update(goals as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(context, "Goals saved successfully", Toast.LENGTH_SHORT).show()
                    // Navigate back to the profile fragment
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Failed to save goals: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
