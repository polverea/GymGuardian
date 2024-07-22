package com.example.gymguardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gymguardian.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        sharedViewModel.profileUpdated.observe(viewLifecycleOwner, { updated ->
            if (updated) {
                loadUserData()
            }
        })

        loadUserData()
    }

    private fun loadUserData() {
        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && _binding != null) {
                        val preferredName = document.getString("preferredName") ?: "User"
                        val dailyCalories = document.getString("dailyCalories")?.toInt() ?: 0
                        binding.welcomeTextView.text = "Welcome, $preferredName"

                        // Load the consumed calories
                        loadMeals(it.uid, dailyCalories)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadMeals(uid: String, dailyCalories: Int) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = sdf.format(java.util.Calendar.getInstance().time)
        val mealTypes = listOf("breakfast", "lunch", "dinner", "snacks")
        var consumedCalories = 0
        var totalCarbs = 0
        var totalProtein = 0
        var totalFat = 0

        mealTypes.forEach { mealType ->
            db.collection("UsersInfo").document(uid)
                .collection("Meals").document(today)
                .collection(mealType)
                .get()
                .addOnSuccessListener { documents ->
                    var mealCalories = 0
                    var mealCarbs = 0
                    var mealProtein = 0
                    var mealFat = 0

                    for (document in documents) {
                        val meal = document.toObject(Meal::class.java)
                        mealCalories += meal.calories
                        mealCarbs += meal.carbs
                        mealProtein += meal.protein
                        mealFat += meal.fat
                    }

                    consumedCalories += mealCalories
                    totalCarbs += mealCarbs
                    totalProtein += mealProtein
                    totalFat += mealFat

                    updateCalorieSummary(dailyCalories, consumedCalories)
                    updateProgressBars(totalCarbs, totalFat, totalProtein)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load $mealType: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateCalorieSummary(dailyCalories: Int, consumedCalories: Int) {
        if (_binding != null) {
            binding.dailyGoalTextView.text = "Daily Goal: $dailyCalories kcal"
            binding.consumedCaloriesTextView.text = "Consumed: $consumedCalories kcal"
            binding.remainingCaloriesTextView.text = "Remaining: ${dailyCalories - consumedCalories} kcal"

            // Update progress bar
            binding.caloriesProgressBar.max = dailyCalories
            binding.caloriesProgressBar.progress = consumedCalories
        }
    }

    private fun updateProgressBars(carbs: Int, fat: Int, protein: Int) {
        val total = carbs + fat + protein

        binding.carbsProgressBar.max = total
        binding.carbsProgressBar.progress = carbs
        binding.carbsTextView.text = "$carbs g"

        binding.fatProgressBar.max = total
        binding.fatProgressBar.progress = fat
        binding.fatTextView.text = "$fat g"

        binding.proteinProgressBar.max = total
        binding.proteinProgressBar.progress = protein
        binding.proteinTextView.text = "$protein g"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
