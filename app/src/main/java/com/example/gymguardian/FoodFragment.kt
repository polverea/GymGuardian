package com.example.gymguardian

import SearchResultsAdapter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gymguardian.api.RetrofitInstance
import com.example.gymguardian.databinding.FragmentFoodBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        binding.root.findViewById<View>(R.id.previousDayButton)
            .setOnClickListener { changeDate(-1) }
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
        binding.root.findViewById<View>(R.id.breakfastSection)
            .findViewById<View>(R.id.addMealButton).setOnClickListener {
                showSearchMealDialog("breakfast")
            }
        binding.root.findViewById<View>(R.id.lunchSection).findViewById<View>(R.id.addMealButton)
            .setOnClickListener {
                showSearchMealDialog("lunch")
            }
        binding.root.findViewById<View>(R.id.dinnerSection).findViewById<View>(R.id.addMealButton)
            .setOnClickListener {
                showSearchMealDialog("dinner")
            }
        binding.root.findViewById<View>(R.id.snacksSection).findViewById<View>(R.id.addMealButton)
            .setOnClickListener {
                showSearchMealDialog("snacks")
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

                sectionView.findViewById<TextView>(R.id.totalCaloriesTextView).text =
                    "Calories: $totalCalories kcal"
                sectionView.findViewById<TextView>(R.id.totalMacrosTextView).text =
                    "Carbs: ${totalCarbs}g, Protein: ${totalProtein}g, Fat: ${totalFat}g"

                val recyclerView = sectionView.findViewById<RecyclerView>(R.id.recyclerView)
                recyclerView.adapter = MealAdapter(mealList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Failed to load $mealType: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showSearchMealDialog(mealType: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_meal, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val alertDialog = dialogBuilder.show()

        val mealNameEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.mealNameEditText)
        val searchResultsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.searchResultsRecyclerView)
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(context)

        mealNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    searchFood(s.toString()) { foodItems ->
                        if (foodItems != null && foodItems.isNotEmpty()) {
                            val adapter = SearchResultsAdapter(foodItems) { selectedFoodItem ->
                                showFoodDetailsDialog(selectedFoodItem, mealType)
                                alertDialog.dismiss()
                            }
                            searchResultsRecyclerView.adapter = adapter
                        } else {
                            Toast.makeText(context, "Food not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun showFoodDetailsDialog(foodItem: Product, mealType: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_food_details, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val alertDialog = dialogBuilder.show()

        val foodNameTextView = dialogView.findViewById<TextView>(R.id.foodNameTextView)
        val caloriesTextView = dialogView.findViewById<TextView>(R.id.caloriesTextView)
        val carbsTextView = dialogView.findViewById<TextView>(R.id.carbsTextView)
        val proteinTextView = dialogView.findViewById<TextView>(R.id.proteinTextView)
        val fatTextView = dialogView.findViewById<TextView>(R.id.fatTextView)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)
        val addMealButton = dialogView.findViewById<Button>(R.id.addMealButton)

        foodNameTextView.text = foodItem.product_name
        caloriesTextView.text = "Calories: ${(foodItem.nutriments.energyKcal ?: 0f).toInt()} kcal"
        carbsTextView.text = "Carbs: ${(foodItem.nutriments.carbohydrates ?: 0f).toInt()}g"
        proteinTextView.text = "Protein: ${(foodItem.nutriments.proteins ?: 0f).toInt()}g"
        fatTextView.text = "Fat: ${(foodItem.nutriments.fat ?: 0f).toInt()}g"

        addMealButton.setOnClickListener {
            val quantity = quantityEditText.text.toString().toIntOrNull() ?: 100
            val factor = quantity / 100.0

            val calories = (foodItem.nutriments.energyKcal ?: 0f) * factor
            val carbs = (foodItem.nutriments.carbohydrates ?: 0f) * factor
            val protein = (foodItem.nutriments.proteins ?: 0f) * factor
            val fat = (foodItem.nutriments.fat ?: 0f) * factor

            addSelectedFoodToMeal(foodItem, mealType, alertDialog, calories.toInt(), carbs.toInt(), protein.toInt(), fat.toInt(), quantity)
        }
    }

    private fun addSelectedFoodToMeal(
        selectedFoodItem: Product,
        mealType: String,
        alertDialog: AlertDialog,
        calories: Int,
        carbs: Int,
        protein: Int,
        fat: Int,
        quantity: Int
    ) {
        val user = auth.currentUser
        user?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(selectedDate.time)

            val meal = Meal(selectedFoodItem.product_name, calories, carbs, protein, fat, quantity)
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

    private fun searchFood(foodName: String, callback: (List<Product>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.searchProducts(foodName)
                if (response.isSuccessful && response.body() != null) {
                    val products = response.body()?.products ?: emptyList()
                    withContext(Dispatchers.Main) {
                        callback(products)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }
}
