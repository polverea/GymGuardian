package com.example.gymguardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
    ): View? {
        binding = FragmentFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupDateNavigation()
        setupRecyclerViews()
        loadMeals()

        binding.addBreakfastButton.setOnClickListener {
            showAddMealDialog("breakfast")
        }
        binding.addLunchButton.setOnClickListener {
            showAddMealDialog("lunch")
        }
        binding.addDinnerButton.setOnClickListener {
            showAddMealDialog("dinner")
        }
        binding.addSnacksButton.setOnClickListener {
            showAddMealDialog("snacks")
        }
    }

    private fun setupDateNavigation() {
        updateDateTextView()
        binding.previousDayButton.setOnClickListener { changeDate(-1) }
        binding.nextDayButton.setOnClickListener { changeDate(1) }
    }

    private fun changeDate(days: Int) {
        selectedDate.add(Calendar.DAY_OF_YEAR, days)
        updateDateTextView()
        loadMeals()
    }

    private fun updateDateTextView() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateText = sdf.format(selectedDate.time)
        binding.dateTextView.text = dateText
    }

    private fun setupRecyclerViews() {
        binding.breakfastRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.lunchRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.dinnerRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.snacksRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun loadMeals() {
        val user = auth.currentUser
        user?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(selectedDate.time)

            val mealTypes = listOf("breakfast", "lunch", "dinner", "snacks")
            mealTypes.forEach { mealType ->
                db.collection("UsersInfo").document(it.uid)
                    .collection("Meals").document(today)
                    .collection(mealType)
                    .get()
                    .addOnSuccessListener { documents ->
                        val mealList = mutableListOf<Meal>()
                        for (document in documents) {
                            val meal = document.toObject(Meal::class.java)
                            mealList.add(meal)
                        }
                        updateRecyclerView(mealType, mealList)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Failed to load $mealType: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun updateRecyclerView(mealType: String, mealList: List<Meal>) {
        val adapter = MealAdapter(mealList)
        when (mealType) {
            "breakfast" -> binding.breakfastRecyclerView.adapter = adapter
            "lunch" -> binding.lunchRecyclerView.adapter = adapter
            "dinner" -> binding.dinnerRecyclerView.adapter = adapter
            "snacks" -> binding.snacksRecyclerView.adapter = adapter
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
