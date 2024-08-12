package com.example.gymguardian

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gymguardian.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

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
                        val dailyCalories = document.getString("caloriesGoal")?.toInt() ?: 0
                        val dailyCarbs = document.getString("carbsGoal")?.toInt() ?: 0
                        val dailyProtein = document.getString("proteinGoal")?.toInt() ?: 0
                        val dailyFat = document.getString("fatGoal")?.toInt() ?: 0
                        binding.welcomeTextView.text = "Welcome, $preferredName"

                        // Load the consumed calories and macros
                        loadMeals(it.uid, dailyCalories, dailyCarbs, dailyProtein, dailyFat)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadMeals(uid: String, dailyCalories: Int, dailyCarbs: Int, dailyProtein: Int, dailyFat: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Calendar.getInstance().time)
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
                    updateProgressBars(totalCarbs, totalFat, totalProtein, dailyCarbs, dailyFat, dailyProtein)
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

            // Verifică dacă caloriile consumate depășesc obiectivul zilnic
            if (consumedCalories > dailyCalories) {
                binding.consumedCaloriesTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                binding.consumedCaloriesTextView.text = "${binding.consumedCaloriesTextView.text} (Exceeded)"
                binding.consumedCaloriesTextView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
            } else {
                binding.consumedCaloriesTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
        }
    }

    private fun updateProgressBars(carbs: Int, fat: Int, protein: Int, dailyCarbs: Int, dailyFat: Int, dailyProtein: Int) {
        if (_binding != null) {
            updateSingleProgressBar(binding.carbsProgressBar, binding.carbsTextView, carbs, dailyCarbs, "Carbs")
            updateSingleProgressBar(binding.fatProgressBar, binding.fatTextView, fat, dailyFat, "Fat")
            updateSingleProgressBar(binding.proteinProgressBar, binding.proteinTextView, protein, dailyProtein, "Protein")
        }
    }

    private fun updateSingleProgressBar(progressBar: ProgressBar, textView: TextView, total: Int, goal: Int, nutrientName: String) {
        progressBar.max = goal
        progressBar.progress = total

        if (total > goal) {
            textView.text = "$nutrientName: $total g of $goal g (Exceeded)"
            textView.setTextColor(Color.RED)
            textView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
        } else {
            textView.text = "$nutrientName: $total g of $goal g"
            textView.setTextColor(Color.WHITE)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
