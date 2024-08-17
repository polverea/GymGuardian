package com.example.gymguardian

import SearchResultsAdapter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class FoodFragment : Fragment() {
    private var db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentFoodBinding? = null
    private val binding get() = _binding!!
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupUI()
    }

    private fun setupUI() {
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
        recyclerView.adapter = MealAdapter(mutableListOf()) { meal ->
            val user = auth.currentUser
            user?.let {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = sdf.format(selectedDate.time)
                deleteMeal(it.uid, today, mealName.lowercase(Locale.ROOT), meal, sectionView)
            }
        }
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
                    meal.id = document.id // Set the document ID as the meal ID
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
                val adapter = MealAdapter(mealList.toMutableList()) { meal ->
                    deleteMeal(uid, date, mealType, meal, sectionView)
                }
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Failed to load $mealType: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun deleteMeal(uid: String, date: String, mealType: String, meal: Meal, sectionView: View) {
        db.collection("UsersInfo").document(uid)
            .collection("Meals").document(date)
            .collection(mealType)
            .document(meal.id)
            .delete()
            .addOnSuccessListener {
                val adapter = sectionView.findViewById<RecyclerView>(R.id.recyclerView).adapter as MealAdapter
                adapter.removeMeal(meal)
                updateMealSectionUI(sectionView, adapter.meals)
                Toast.makeText(context, "Meal deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateMealSectionUI(sectionView: View, mealList: List<Meal>) {
        var totalCalories = 0
        var totalCarbs = 0
        var totalProtein = 0
        var totalFat = 0

        for (meal in mealList) {
            totalCalories += meal.calories
            totalCarbs += meal.carbs
            totalProtein += meal.protein
            totalFat += meal.fat
        }

        sectionView.findViewById<TextView>(R.id.totalCaloriesTextView).text =
            "Calories: $totalCalories kcal"
        sectionView.findViewById<TextView>(R.id.totalMacrosTextView).text =
            "Carbs: ${totalCarbs}g, Protein: ${totalProtein}g, Fat: ${totalFat}g"
    }

    private fun showSearchMealDialog(mealType: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_meal, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val alertDialog = dialogBuilder.show()

        val mealNameEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.mealNameEditText)
        val searchButton = dialogView.findViewById<Button>(R.id.searchButton)
        val searchResultsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.searchResultsRecyclerView)
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(context)

        val addManualMealButton = dialogView.findViewById<Button>(R.id.addOwnMealButton)
        addManualMealButton.setOnClickListener {
            showManualMealDialog(mealType)
            alertDialog.dismiss()
        }
    // Load recent foods when the dialog is opened
        loadRecentFoods { recentFoods ->
            Log.d("FoodFragment", "Recent foods loaded: ${recentFoods?.size ?: 0}")
            if (!recentFoods.isNullOrEmpty()) {
                val adapter = SearchResultsAdapter(recentFoods) { selectedFoodItem ->
                    showFoodDetailsDialog(selectedFoodItem, mealType)
                    alertDialog.dismiss()
                }
                searchResultsRecyclerView.adapter = adapter
                searchResultsRecyclerView.visibility = View.VISIBLE
            } else {
                searchResultsRecyclerView.visibility = View.GONE
            }
        }

        // Set up recent foods search
        mealNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    searchRecentFoods(s.toString()) { recentFoods ->
                        Log.d("FoodFragment", "Search result size: ${recentFoods?.size ?: 0}")
                        if (recentFoods != null && recentFoods.isNotEmpty()) {
                            val adapter = SearchResultsAdapter(recentFoods) { selectedFoodItem ->
                                showFoodDetailsDialog(selectedFoodItem, mealType)
                                alertDialog.dismiss()
                            }
                            searchResultsRecyclerView.adapter = adapter
                            searchResultsRecyclerView.visibility = View.VISIBLE
                        } else {
                            searchResultsRecyclerView.visibility = View.GONE
                        }
                    }
                } else {
                    loadRecentFoods { recentFoods ->
                        Log.d("FoodFragment", "Recent foods loaded: ${recentFoods?.size ?: 0}")
                        if (recentFoods != null && recentFoods.isNotEmpty()) {
                            val adapter = SearchResultsAdapter(recentFoods) { selectedFoodItem ->
                                showFoodDetailsDialog(selectedFoodItem, mealType)
                                alertDialog.dismiss()
                            }
                            searchResultsRecyclerView.adapter = adapter
                            searchResultsRecyclerView.visibility = View.VISIBLE
                        } else {
                            searchResultsRecyclerView.visibility = View.GONE
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Set up button search
        searchButton.setOnClickListener {
            val foodName = mealNameEditText.text.toString()
            if (foodName.isNotEmpty()) {
                searchFood(foodName) { foodItems ->
                    Log.d("FoodFragment", "Search food size: ${foodItems?.size ?: 0}")
                    if (foodItems != null && foodItems.isNotEmpty()) {
                        val adapter = SearchResultsAdapter(foodItems) { selectedFoodItem ->
                            showFoodDetailsDialog(selectedFoodItem, mealType)
                            alertDialog.dismiss()
                        }
                        searchResultsRecyclerView.adapter = adapter
                        searchResultsRecyclerView.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(context, "Food not found", Toast.LENGTH_SHORT).show()
                        searchResultsRecyclerView.visibility = View.GONE
                    }
                }
            }
        }

        // Add manual meal button
        addManualMealButton.setOnClickListener {
            alertDialog.dismiss()
            showAddManualMealDialog(mealType)
        }
    }
    private fun showManualMealDialog(mealType: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_own_meal, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val alertDialog = dialogBuilder.show()

        val mealNameEditText = dialogView.findViewById<EditText>(R.id.mealNameEditText)
        val caloriesEditText = dialogView.findViewById<EditText>(R.id.caloriesEditText)
        val carbsEditText = dialogView.findViewById<EditText>(R.id.carbsEditText)
        val proteinEditText = dialogView.findViewById<EditText>(R.id.proteinEditText)
        val fatEditText = dialogView.findViewById<EditText>(R.id.fatEditText)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)
        val saveMealButton = dialogView.findViewById<Button>(R.id.saveMealButton)

        saveMealButton.setOnClickListener {
            val mealName = mealNameEditText.text.toString().trim()
            val calories = caloriesEditText.text.toString().toIntOrNull() ?: 0
            val carbs = carbsEditText.text.toString().toIntOrNull() ?: 0
            val protein = proteinEditText.text.toString().toIntOrNull() ?: 0
            val fat = fatEditText.text.toString().toIntOrNull() ?: 0
            val quantity = quantityEditText.text.toString().toIntOrNull() ?: 100

            if (mealName.isEmpty() || calories <= 0 || carbs < 0 || protein < 0 || fat < 0) {
                Toast.makeText(context, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
            } else {
                val manualMeal = Product(
                    product_name = mealName,
                    nutriments = Nutriments(
                        energyKcal = calories.toFloat(),
                        carbohydrates = carbs.toFloat(),
                        proteins = protein.toFloat(),
                        fat = fat.toFloat()
                    ),
                    timestamp = com.google.firebase.Timestamp.now()
                )
                addSelectedFoodToMeal(manualMeal, mealType, alertDialog, calories, carbs, protein, fat, quantity)
            }
        }
    }
    private fun addManualMealToDatabase(
        mealType: String,
        mealName: String,
        calories: Int,
        carbs: Int,
        protein: Int,
        fat: Int,
        quantity: Int
    ) {
        val user = auth.currentUser
        user?.let {
            val uid = it.uid
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(selectedDate.time)
            val mealId = UUID.randomUUID().toString()

            val meal = Meal(
                id = mealId,
                name = mealName,
                calories = calories,
                carbs = carbs,
                protein = protein,
                fat = fat,
                quantity = quantity,
                timestamp = com.google.firebase.Timestamp.now()
            )

            db.collection("UsersInfo").document(uid)
                .collection("Meals").document(today)
                .collection(mealType)
                .document(mealId)
                .set(meal)
                .addOnSuccessListener {
                    Toast.makeText(context, "Meal added successfully", Toast.LENGTH_SHORT).show()
                    loadMeals()

                    // Normalizează valorile pentru 100g înainte de a salva în recentFoods
                    val normalizedMeal = Meal(
                        id = mealId,
                        name = mealName,
                        calories = (calories / (quantity / 100f)).roundToInt(),
                        carbs = (carbs / (quantity / 100f)).roundToInt(),
                        protein = (protein / (quantity / 100f)).roundToInt(),
                        fat = (fat / (quantity / 100f)).roundToInt(),
                        quantity = 100,
                        timestamp = com.google.firebase.Timestamp.now()
                    )

                    saveRecentFood(uid, normalizedMeal.toProduct())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add meal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun showFoodDetailsDialog(foodItem: Product, mealType: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_food_details, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val alertDialog = dialogBuilder.show()

        dialogView.findViewById<TextView>(R.id.foodNameTextView).text = foodItem.product_name
        dialogView.findViewById<TextView>(R.id.caloriesTextView).text = "Calories: ${(foodItem.nutriments.energyKcal ?: 0f)} kcal"
        dialogView.findViewById<TextView>(R.id.carbsTextView).text = "Carbs: ${(foodItem.nutriments.carbohydrates ?: 0f)}g"
        dialogView.findViewById<TextView>(R.id.proteinTextView).text = "Protein: ${(foodItem.nutriments.proteins ?: 0f)}g"
        dialogView.findViewById<TextView>(R.id.fatTextView).text = "Fat: ${(foodItem.nutriments.fat ?: 0f).toInt()}g"

        val nutriScoreTextView = dialogView.findViewById<TextView>(R.id.nutriScoreTextView)
        val nutriScore = foodItem.nutriscoreGrade?.uppercase() ?: "N/A"
        nutriScoreTextView.text = "Nutri-Score: $nutriScore"

        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)
        val addMealButton = dialogView.findViewById<Button>(R.id.addMealButton)

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
            val uid = it.uid
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(selectedDate.time)
            val mealId = UUID.randomUUID().toString()

            val meal = Meal(
                id = mealId,
                name = selectedFoodItem.product_name,
                calories = calories,
                carbs = carbs,
                protein = protein,
                fat = fat,
                quantity = quantity,
                nutriScore = selectedFoodItem.nutriscoreGrade ?: "",
                timestamp = com.google.firebase.Timestamp.now()
            )

            db.collection("UsersInfo").document(uid)
                .collection("Meals").document(today)
                .collection(mealType)
                .document(mealId)
                .set(meal)
                .addOnSuccessListener {
                    Toast.makeText(context, "Meal added successfully", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                    loadMeals()

                    saveRecentFood(uid, selectedFoodItem)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add meal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun showAddManualMealDialog(mealType: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_own_meal, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val alertDialog = dialogBuilder.show()

        val mealNameEditText = dialogView.findViewById<EditText>(R.id.mealNameEditText)
        val caloriesEditText = dialogView.findViewById<EditText>(R.id.caloriesEditText)
        val carbsEditText = dialogView.findViewById<EditText>(R.id.carbsEditText)
        val proteinEditText = dialogView.findViewById<EditText>(R.id.proteinEditText)
        val fatEditText = dialogView.findViewById<EditText>(R.id.fatEditText)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)
        val saveMealButton = dialogView.findViewById<Button>(R.id.saveMealButton)

        saveMealButton.setOnClickListener {
            val mealName = mealNameEditText.text.toString().trim()
            val calories = caloriesEditText.text.toString().toIntOrNull() ?: 0
            val carbs = carbsEditText.text.toString().toIntOrNull() ?: 0
            val protein = proteinEditText.text.toString().toIntOrNull() ?: 0
            val fat = fatEditText.text.toString().toIntOrNull() ?: 0
            val quantity = quantityEditText.text.toString().toIntOrNull() ?: 100

            if (mealName.isNotEmpty()) {
                addManualMealToDatabase(mealType, mealName, calories, carbs, protein, fat, quantity)
                alertDialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter a meal name", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadRecentFoods(callback: (List<Product>?) -> Unit) {
        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .collection("RecentFoods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener { documents ->
                    val recentFoods = documents.mapNotNull { it.toObject(Product::class.java) }
                    callback(recentFoods)
                }
                .addOnFailureListener {
                    callback(null)
                }
        } ?: callback(null)
    }

    private fun searchRecentFoods(query: String, callback: (List<Product>?) -> Unit) {
        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .collection("RecentFoods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener { documents ->
                    val recentFoods = documents.mapNotNull { it.toObject(Product::class.java) }
                    val filteredFoods = if (query.isNotEmpty()) {
                        recentFoods.filter { it.product_name.contains(query, ignoreCase = true) }
                    } else {
                        recentFoods
                    }
                    callback(filteredFoods)
                }
                .addOnFailureListener {
                    callback(null)
                }
        } ?: callback(null)
    }

    private fun searchFood(foodName: String, callback: (List<Product>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiResponse = RetrofitInstance.api.searchProducts(foodName)
                val apiProducts = if (apiResponse.isSuccessful && apiResponse.body() != null) {
                    apiResponse.body()?.products?.filter {
                        it.product_name.contains(foodName, ignoreCase = true)
                    } ?: emptyList()
                } else {
                    emptyList()
                }

                val user = auth.currentUser
                val firebaseProducts = user?.let {
                    val snapshot = db.collection("UsersInfo").document(it.uid)
                        .collection("RecentFoods")
                        .whereEqualTo("product_name", foodName)
                        .get()
                        .await()
                    snapshot.documents.mapNotNull { doc -> doc.toObject(Product::class.java) }
                } ?: emptyList()

                val combinedResults = apiProducts + firebaseProducts

                withContext(Dispatchers.Main) {
                    callback(combinedResults)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }


    private fun saveRecentFood(uid: String, product: Product) {
        val recentFoodsRef = db.collection("UsersInfo").document(uid).collection("RecentFoods")

        recentFoodsRef.whereEqualTo("product_name", product.product_name)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val existingDocId = documents.documents[0].id
                    recentFoodsRef.document(existingDocId)
                        .update("timestamp", com.google.firebase.Timestamp.now())
                        .addOnSuccessListener {
                            Log.d("FoodFragment", "Updated timestamp for recent food: ${product.product_name}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FoodFragment", "Failed to update timestamp: ${e.message}")
                        }
                } else {
                    product.timestamp = com.google.firebase.Timestamp.now()
                    recentFoodsRef.add(product)
                        .addOnSuccessListener {
                            Log.d("FoodFragment", "Saved recent food: $product")
                            recentFoodsRef.get().addOnSuccessListener { documents ->
                                if (documents.size() > 5) {
                                    val sortedDocs = documents.documents.sortedBy { it.getTimestamp("timestamp") }
                                    for (i in 0 until documents.size() - 5) {
                                        recentFoodsRef.document(sortedDocs[i].id).delete()
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FoodFragment", "Failed to save recent food: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FoodFragment", "Failed to check for existing recent food: ${e.message}")
            }
    }

    private fun Meal.toProduct(): Product {
        return Product(
            product_name = this.name,
            nutriments = Nutriments(
                energyKcal = this.calories.toFloat(),
                carbohydrates = this.carbs.toFloat(),
                proteins = this.protein.toFloat(),
                fat = this.fat.toFloat()
            ),
            timestamp = this.timestamp
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
