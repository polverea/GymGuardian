package com.example.gymguardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gymguardian.databinding.FragmentFoodBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class FoodFragment : Fragment() {
    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentFoodBinding
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupDateNavigation()
        setupRecyclerViews()
        setupAddMealButtons()
        loadMeals()
    }

    private fun setupDateNavigation() {
        updateDateTextView()
        binding.root.findViewById<View>(R.id.previousDayButton).setOnClickListener { changeDate(-1) }
        binding.root.findViewById<View>(R.id.nextDayButton).setOnClickListener { changeDate(1) }
    }

    private fun updateDateTextView() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateText = sdf.format(selectedDate.time)
        binding.root.findViewById<TextView>(R.id.dateTextView).text = dateText
    }

    private fun changeDate(days: Int) {
        selectedDate.add(Calendar.DAY_OF_YEAR, days)
        updateDateTextView()
        loadMeals()
    }

    private fun setupRecyclerViews() {
        setupRecyclerViewSection(binding.root.findViewById(R.id.breakfastSection), "Breakfast")
        setupRecyclerViewSection(binding.root.findViewById(R.id.lunchSection), "Lunch")
        setupRecyclerViewSection(binding.root.findViewById(R.id.dinnerSection), "Dinner")
        setupRecyclerViewSection(binding.root.findViewById(R.id.snacksSection), "Snacks")
    }

    private fun setupRecyclerViewSection(sectionView: View, mealName: String) {
        sectionView.findViewById<TextView>(R.id.mealTitle).text = mealName
        val recyclerView = sectionView.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = MealAdapter(emptyList()) // Set an empty list initially
    }

    private fun setupAddMealButtons() {
        binding.root.findViewById<View>(R.id.breakfastSection).findViewById<View>(R.id.addMealButton).setOnClickListener {
            showAddMealDialog("breakfast")
        }
        binding.root.findViewById<View>(R.id.lunchSection).findViewById<View>(R.id.addMealButton).setOnClickListener {
            showAddMealDialog("lunch")
        }
        binding.root.findViewById<View>(R.id.dinnerSection).findViewById<View>(R.id.addMealButton).setOnClickListener {
            showAddMealDialog("dinner")
        }
        binding.root.findViewById<View>(R.id.snacksSection).findViewById<View>(R.id.addMealButton).setOnClickListener {
            showAddMealDialog("snacks")
        }
    }

    private fun loadMeals() {
        val user = auth.currentUser
        user?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(selectedDate.time)

            loadMealType(it.uid, today, "breakfast", binding.root.findViewById(R.id.breakfastSection))
            loadMealType(it.uid, today, "lunch", binding.root.findViewById(R.id.lunchSection))
            loadMealType(it.uid, today, "dinner", binding.root.findViewById(R.id.dinnerSection))
            loadMealType(it.uid, today, "snacks", binding.root.findViewById(R.id.snacksSection))
        }
    }

    private fun loadMealType(uid: String, date: String, mealType: String, sectionView: View) {
        db.collection("UsersInfo").document(uid)
            .collection("Meals").document(date)
            .collection(mealType)
            .get()
            .addOnSuccessListener { documents ->
                val mealList = mutableListOf<Meal>()
                var totalCalories = 0
                var totalCarbs = 0
                var totalProtein = 0
                var totalFat = 0

                for (document in documents) {
                    val meal = document.toObject(Meal::class.java)
                    mealList.add(meal)
                    totalCalories += meal.calories
                    totalCarbs += meal.carbs
                    totalProtein += meal.protein
                    totalFat += meal.fat
                }

                sectionView.findViewById<TextView>(R.id.totalCaloriesTextView).text = "Calories: $totalCalories kcal"
                sectionView.findViewById<TextView>(R.id.totalMacrosTextView).text = "Carbs: ${totalCarbs}g, Protein: ${totalProtein}g, Fat: ${totalFat}g"

                val recyclerView = sectionView.findViewById<RecyclerView>(R.id.recyclerView)
                recyclerView.adapter = MealAdapter(mealList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load $mealType: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddMealDialog(mealType: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_meal, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val alertDialog = dialogBuilder.show()

        val mealNameEditText = dialogView.findViewById<EditText>(R.id.mealNameEditText)
        val caloriesEditText = dialogView.findViewById<EditText>(R.id.caloriesEditText)
        val carbsEditText = dialogView.findViewById<EditText>(R.id.carbsEditText)
        val proteinEditText = dialogView.findViewById<EditText>(R.id.proteinEditText)
        val fatEditText = dialogView.findViewById<EditText>(R.id.fatEditText)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)

        dialogView.findViewById<View>(R.id.saveMealButton).setOnClickListener {
            val mealName = mealNameEditText.text.toString().trim()
            val calories = caloriesEditText.text.toString().trim().toIntOrNull()
            val carbs = carbsEditText.text.toString().trim().toIntOrNull()
            val protein = proteinEditText.text.toString().trim().toIntOrNull()
            val fat = fatEditText.text.toString().trim().toIntOrNull()
            val quantity = quantityEditText.text.toString().trim().toIntOrNull()

            if (mealName.isEmpty() || calories == null || carbs == null || protein == null || fat == null || quantity == null) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            user?.let {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = sdf.format(selectedDate.time)

                val meal = Meal(mealName, calories, carbs, protein, fat, quantity)
                db.collection("UsersInfo").document(it.uid)
                    .collection("Meals").document(today)
                    .collection(mealType)
                    .add(meal)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Meal added successfully", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                        loadMeals()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to add meal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
