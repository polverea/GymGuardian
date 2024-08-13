package com.example.gymguardian

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.gymguardian.databinding.FragmentGoalBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateCalories()
            }
        }

        binding.caloriesGoalEditText.addTextChangedListener(textWatcher)
        binding.proteinGoalEditText.addTextChangedListener(textWatcher)
        binding.carbsGoalEditText.addTextChangedListener(textWatcher)
        binding.fatGoalEditText.addTextChangedListener(textWatcher)

        loadGoals()
    }

    private fun loadGoals() {
        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val caloriesGoal = document.getString("caloriesGoal") ?: ""
                        val proteinGoal = document.getString("proteinGoal") ?: ""
                        val carbsGoal = document.getString("carbsGoal") ?: ""
                        val fatGoal = document.getString("fatGoal") ?: ""

                        binding.caloriesGoalEditText.setText(caloriesGoal)
                        binding.proteinGoalEditText.setText(proteinGoal)
                        binding.carbsGoalEditText.setText(carbsGoal)
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
        val protein = binding.proteinGoalEditText.text.toString().toIntOrNull() ?: 0
        val carbs = binding.carbsGoalEditText.text.toString().toIntOrNull() ?: 0
        val fat = binding.fatGoalEditText.text.toString().toIntOrNull() ?: 0

        val proteinCalories = protein * 4
        val carbsCalories = carbs * 4
        val fatCalories = fat * 9

        val totalCalories = proteinCalories + carbsCalories + fatCalories

        // Update the calories goal to match the total calories from macronutrients
        if (totalCalories != caloriesGoal) {
            binding.caloriesGoalEditText.setText(totalCalories.toString())
        }

        binding.proteinCaloriesTextView.text = "Protein: $protein g (${proteinCalories} kcal)"
        binding.carbsCaloriesTextView.text = "Carbs: $carbs g (${carbsCalories} kcal)"
        binding.fatCaloriesTextView.text = "Fat: $fat g (${fatCalories} kcal)"
        binding.totalCaloriesTextView.text = "Total: $totalCalories kcal of ${binding.caloriesGoalEditText.text} kcal"
    }

    private fun saveGoals() {
        val goals = hashMapOf(
            "caloriesGoal" to binding.caloriesGoalEditText.text.toString(),
            "proteinGoal" to binding.proteinGoalEditText.text.toString(),
            "carbsGoal" to binding.carbsGoalEditText.text.toString(),
            "fatGoal" to binding.fatGoalEditText.text.toString()
        )

        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        user?.let { user ->
            val uid = user.uid
            db.collection("UsersInfo").document(uid)
                .set(goals, SetOptions.merge())  // Adaugă obiectivele fără a suprascrie alte câmpuri
                .addOnSuccessListener {
                    Toast.makeText(context, "Goals saved successfully", Toast.LENGTH_SHORT).show()

                    // Verifică dacă informațiile din profil sunt completate
                    db.collection("UsersInfo").document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val preferredName = document.getString("preferredName")
                                val weight = document.getString("weight")
                                val height = document.getString("height")
                                val age = document.getString("age")

                                if (!preferredName.isNullOrEmpty() && !weight.isNullOrEmpty() &&
                                    !height.isNullOrEmpty() && !age.isNullOrEmpty()) {
                                    // Activează navigarea în bara de meniu dacă toate câmpurile sunt completate
                                    (activity as? MainActivity)?.enableNavigation()
                                }
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to save goals: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
