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
    private lateinit var db: FirebaseFirestore
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
        db = Firebase.firestore
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

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(selectedDate.time)
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
            auth.currentUser?.let {
                deleteMeal(it.uid, getCurrentDate(), mealName.lowercase(Locale.ROOT), meal, sectionView)
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
            loadMealType(it.uid, getCurrentDate(), "breakfast", binding.root.findViewById(R.id.breakfastSection))
            loadMealType(it.uid, getCurrentDate(), "lunch", binding.root.findViewById(R.id.lunchSection))
            loadMealType(it.uid, getCurrentDate(), "dinner", binding.root.findViewById(R.id.dinnerSection))
            loadMealType(it.uid, getCurrentDate(), "snacks", binding.root.findViewById(R.id.snacksSection))
        }
    }
    private fun loadMealType(uid: String, date: String, mealType: String, sectionView: View) {
        db.collection("UsersInfo").document(uid)
            .collection("Meals").document(date)
            .collection(mealType)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener // Verifică dacă fragmentul este atașat

                val mealList = documents.map { it.toObject(Meal::class.java).apply { id = it.id } }
                val totalCalories = mealList.sumOf { it.calories }
                val totalCarbs = mealList.sumOf { it.carbs }
                val totalProtein = mealList.sumOf { it.protein }
                val totalFat = mealList.sumOf { it.fat }

                // Actualizează valorile pentru noile TextView-uri
                sectionView.findViewById<TextView>(R.id.totalCaloriesTextView).text =
                    getString(R.string.calories_text, totalCalories)
                sectionView.findViewById<TextView>(R.id.proteinTextView).text =
                    getString(R.string.protein_text, totalProtein)
                sectionView.findViewById<TextView>(R.id.carbsTextView).text =
                    getString(R.string.carbs_text, totalCarbs)
                sectionView.findViewById<TextView>(R.id.fatTextView).text =
                    getString(R.string.fat_text, totalFat)

                val recyclerView = sectionView.findViewById<RecyclerView>(R.id.recyclerView)
                val adapter = MealAdapter(mealList.toMutableList()) { meal ->
                    deleteMeal(uid, date, mealType, meal, sectionView)
                }
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener

                Toast.makeText(context, "Failed to load $mealType: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val totalCalories = mealList.sumOf { it.calories }
        val totalCarbs = mealList.sumOf { it.carbs }
        val totalProtein = mealList.sumOf { it.protein }
        val totalFat = mealList.sumOf { it.fat }

        // Actualizează valorile pentru noile TextView-uri
        sectionView.findViewById<TextView>(R.id.totalCaloriesTextView).text =
            getString(R.string.calories_text, totalCalories)
        sectionView.findViewById<TextView>(R.id.proteinTextView).text =
            getString(R.string.protein_text, totalProtein)
        sectionView.findViewById<TextView>(R.id.carbsTextView).text =
            getString(R.string.carbs_text, totalCarbs)
        sectionView.findViewById<TextView>(R.id.fatTextView).text =
            getString(R.string.fat_text, totalFat)
    }
    private fun showSearchMealDialog(mealType: String) {
        val dialogView = showDialog(R.layout.dialog_add_meal)
        val alertDialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        alertDialog.show()

        val mealNameEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.mealNameEditText)
        val searchButton = dialogView.findViewById<Button>(R.id.searchButton)
        val searchResultsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.searchResultsRecyclerView)
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(context)

        dialogView.findViewById<Button>(R.id.addOwnMealButton).setOnClickListener {
            showManualMealDialog(mealType)
            alertDialog.dismiss()  // Dismiss dialog when adding a manual meal
        }

        loadRecentFoods { recentFoods ->
            setupSearchResultsAdapter(recentFoods, mealType, searchResultsRecyclerView, alertDialog)
        }

        mealNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    searchRecentFoods(s.toString()) { recentFoods ->
                        setupSearchResultsAdapter(recentFoods, mealType, searchResultsRecyclerView, alertDialog)
                    }
                } else {
                    loadRecentFoods { recentFoods ->
                        setupSearchResultsAdapter(recentFoods, mealType, searchResultsRecyclerView, alertDialog)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchButton.setOnClickListener {
            val foodName = mealNameEditText.text.toString()
            if (foodName.isNotEmpty()) {
                searchFood(foodName) { foodItems ->
                    setupSearchResultsAdapter(foodItems, mealType, searchResultsRecyclerView, alertDialog)
                }
            }
        }
    }

    private fun setupSearchResultsAdapter(
        foodItems: List<Product>?,
        mealType: String,
        recyclerView: RecyclerView,
        alertDialog: AlertDialog
    ) {
        if (!foodItems.isNullOrEmpty()) {
            val adapter = SearchResultsAdapter(foodItems) { selectedFoodItem ->
                showFoodDetailsDialog(selectedFoodItem, mealType, alertDialog)
            }
            recyclerView.adapter = adapter
            recyclerView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.GONE
        }
    }

    private fun showManualMealDialog(mealType: String) {
        val dialogView = showDialog(R.layout.dialog_add_own_meal)
        val alertDialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        alertDialog.show()

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
                addManualMealToDatabase(mealType, mealName, calories, carbs, protein, fat, quantity, alertDialog)
            } else {
                Toast.makeText(context, "Please enter a meal name", Toast.LENGTH_SHORT).show()
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
        quantity: Int,
        alertDialog: AlertDialog
    ) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val mealId = UUID.randomUUID().toString()

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

        db.collection("UsersInfo").document(uid)
            .collection("Meals").document(getCurrentDate())
            .collection(mealType)
            .document(mealId)
            .set(normalizedMeal)
            .addOnSuccessListener {
                Toast.makeText(context, "Meal added successfully", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()  // Dismiss the dialog after meal is added
                loadMeals()
                saveRecentFood(uid, normalizedMeal.toProduct())
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to add meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showFoodDetailsDialog(foodItem: Product, mealType: String, alertDialog: AlertDialog) {
        val dialogView = showDialog(R.layout.dialog_food_details)
        val detailsDialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        detailsDialog.show()

        dialogView.findViewById<TextView>(R.id.foodNameTextView).text = foodItem.product_name
        dialogView.findViewById<TextView>(R.id.caloriesTextView).text = getString(R.string.calories_text, foodItem.nutriments.energyKcal?.toInt() ?: 0)
        dialogView.findViewById<TextView>(R.id.carbsTextView).text = getString(R.string.macros_text, foodItem.nutriments.carbohydrates?.toInt() ?: 0, foodItem.nutriments.proteins?.toInt() ?: 0, foodItem.nutriments.fat?.toInt() ?: 0)

        val nutriScoreTextView = dialogView.findViewById<TextView>(R.id.nutriScoreTextView)
        val nutriScore = foodItem.nutriscoreGrade?.uppercase() ?: "N/A"
        nutriScoreTextView.setTextOrHide(nutriScore)

        val quantityEditText = dialogView.findViewById<EditText>(R.id.quantityEditText)
        val addMealButton = dialogView.findViewById<Button>(R.id.addMealButton)

        addMealButton.setOnClickListener {
            val quantity = quantityEditText.text.toString().toIntOrNull() ?: 100
            val factor = quantity / 100.0

            val calories = (foodItem.nutriments.energyKcal ?: 0f) * factor
            val carbs = (foodItem.nutriments.carbohydrates ?: 0f) * factor
            val protein = (foodItem.nutriments.proteins ?: 0f) * factor
            val fat = (foodItem.nutriments.fat ?: 0f) * factor

            addSelectedFoodToMeal(foodItem, mealType, alertDialog, detailsDialog, calories.toInt(), carbs.toInt(), protein.toInt(), fat.toInt(), quantity)
        }
    }
    private fun addSelectedFoodToMeal(
        selectedFoodItem: Product,
        mealType: String,
        alertDialog: AlertDialog,
        detailsDialog: AlertDialog,
        calories: Int,
        carbs: Int,
        protein: Int,
        fat: Int,
        quantity: Int
    ) {
        val user = auth.currentUser ?: return
        val uid = user.uid
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
            .collection("Meals").document(getCurrentDate())
            .collection(mealType)
            .document(mealId)
            .set(meal)
            .addOnSuccessListener {
                Toast.makeText(context, "Meal added successfully", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()  // Dismiss the search dialog
                detailsDialog.dismiss()  // Dismiss the details dialog
                loadMeals()
                saveRecentFood(uid, selectedFoodItem)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to add meal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDialog(layoutId: Int): View {
        val dialogView = LayoutInflater.from(context).inflate(layoutId, null)
        return dialogView
    }

    private fun loadRecentFoods(callback: (List<Product>?) -> Unit) {
        auth.currentUser?.let { user ->
            db.collection("UsersInfo").document(user.uid)
                .collection("RecentFoods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener { documents ->
                    callback(documents.mapNotNull { it.toObject(Product::class.java) })
                }
                .addOnFailureListener {
                    callback(null)
                }
        } ?: callback(null)
    }

    private fun searchRecentFoods(query: String, callback: (List<Product>?) -> Unit) {
        auth.currentUser?.let { user ->
            db.collection("UsersInfo").document(user.uid)
                .collection("RecentFoods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener { documents ->
                    val filteredFoods = documents.mapNotNull { it.toObject(Product::class.java) }
                        .filter { it.product_name.contains(query, ignoreCase = true) }
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

                val firebaseProducts = fetchFirebaseProducts(foodName)

                withContext(Dispatchers.Main) {
                    callback(apiProducts + firebaseProducts)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    private suspend fun fetchFirebaseProducts(foodName: String): List<Product> {
        return auth.currentUser?.let { user ->
            val snapshot = db.collection("UsersInfo").document(user.uid)
                .collection("RecentFoods")
                .whereEqualTo("product_name", foodName)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
        } ?: emptyList()
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
                        .addOnFailureListener { e ->
                            Log.e("FoodFragment", "Failed to update timestamp: ${e.message}")
                        }
                } else {
                    product.timestamp = com.google.firebase.Timestamp.now()
                    recentFoodsRef.add(product)
                        .addOnSuccessListener {
                            Log.d("FoodFragment", "Saved recent food: $product")
                            enforceRecentFoodsLimit(recentFoodsRef)
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

    private fun enforceRecentFoodsLimit(recentFoodsRef: com.google.firebase.firestore.CollectionReference) {
        recentFoodsRef.get().addOnSuccessListener { documents ->
            if (documents.size() > 5) {
                val sortedDocs = documents.documents.sortedBy { it.getTimestamp("timestamp") }
                for (i in 0 until documents.size() - 5) {
                    recentFoodsRef.document(sortedDocs[i].id).delete()
                }
            }
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
    private fun TextView.setTextOrHide(nutriScore: String) {
        if (nutriScore != "N/A") {
            this.text = "Nutri-Score: $nutriScore"
            this.visibility = View.VISIBLE
        } else {
            this.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
