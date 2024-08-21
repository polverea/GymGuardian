package com.example.gymguardian

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class MealDetailDialogFragment : DialogFragment() {

    private lateinit var meal: Meal
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var date: String
    private lateinit var mealType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            meal = it.getParcelable("meal")!!
            date = it.getString("date", "")
            mealType = it.getString("mealType", "")
        }
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
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

        val satietyRatingBar = view.findViewById<RatingBar>(R.id.satietyRatingBar)
        val wellBeingRatingBar = view.findViewById<RatingBar>(R.id.wellBeingRatingBar)
        val energyLevelRatingBar = view.findViewById<RatingBar>(R.id.energyLevelRatingBar)

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
            nutriScoreTextView.text = "Nutri-Score: ${nutriScore.uppercase(Locale.getDefault())}"
        } else {
            nutriScoreTextView.visibility = View.GONE
        }

        // Setare inițială pentru RatingBar-uri
        satietyRatingBar.rating = meal.satiety
        wellBeingRatingBar.rating = meal.wellBeing
        energyLevelRatingBar.rating = meal.energyLevel

        // Salvare feedback când utilizatorul interacționează cu RatingBar-urile
        satietyRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            meal.satiety = rating
            saveFeedback()
        }

        wellBeingRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            meal.wellBeing = rating
            saveFeedback()
        }

        energyLevelRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            meal.energyLevel = rating
            saveFeedback()
        }
    }

    private fun saveFeedback() {
        val user = auth.currentUser
        user?.let {
            db.collection("UsersInfo").document(it.uid)
                .collection("Meals").document(date)
                .collection(mealType)
                .document(meal.id)
                .update(
                    "satiety", meal.satiety,
                    "wellBeing", meal.wellBeing,
                    "energyLevel", meal.energyLevel
                )
                .addOnSuccessListener {
                    Toast.makeText(context, "Feedback saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to save feedback: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(meal: Meal, date: String, mealType: String) =
            MealDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("meal", meal)
                    putString("date", date)
                    putString("mealType", mealType)
                }
            }
    }
}
