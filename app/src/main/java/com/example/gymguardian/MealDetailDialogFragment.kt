package com.example.gymguardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.Locale

class MealDetailDialogFragment : DialogFragment() {

    private lateinit var meal: Meal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            meal = it.getParcelable("meal")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_meal_info, container, false)
        setupUI(view)
        return view
    }

    private fun setupUI(view: View) {
        val mealNameTextView = view.findViewById<TextView>(R.id.mealNameTextView)
        val mealCaloriesTextView = view.findViewById<TextView>(R.id.mealCaloriesTextView)
        val mealQuantityTextView = view.findViewById<TextView>(R.id.mealQuantityTextView)
        val mealCarbsTextView = view.findViewById<TextView>(R.id.mealCarbsTextView)
        val mealProteinTextView = view.findViewById<TextView>(R.id.mealProteinTextView)
        val mealFatTextView = view.findViewById<TextView>(R.id.mealFatTextView)
        val timeAddedTextView = view.findViewById<TextView>(R.id.timeAddedTextView)
        val nutriScoreTextView = view.findViewById<TextView>(R.id.nutriScoreTextView)

        mealNameTextView.text = meal.name
        mealCaloriesTextView.text = "${meal.calories} kcal"
        mealQuantityTextView.text = "Quantity: ${meal.quantity} g"
        mealCarbsTextView.text = "Carbs: ${meal.carbs} g"
        mealProteinTextView.text = "Protein: ${meal.protein} g"
        mealFatTextView.text = "Fat: ${meal.fat} g"

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = sdf.format(meal.timestamp.toDate())
        timeAddedTextView.text = "Time Added: $formattedTime"

        val nutriScore = meal.nutriScore
        if (nutriScore != "N/A" && nutriScore.isNotEmpty()) {
            nutriScoreTextView.text = "Nutri-Score: $nutriScore"
        } else {
            nutriScoreTextView.visibility = View.GONE
        }
    }
    companion object {
        @JvmStatic
        fun newInstance(meal: Meal) =
            MealDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("meal", meal)
                }
            }
    }
}

