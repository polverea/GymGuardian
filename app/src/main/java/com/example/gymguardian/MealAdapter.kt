package com.example.gymguardian

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MealAdapter(
    val meals: MutableList<Meal>,
    private val onDeleteMeal: (Meal) -> Unit
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    inner class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealNameTextView: TextView = itemView.findViewById(R.id.mealNameTextView)
        val mealCaloriesTextView: TextView = itemView.findViewById(R.id.mealCaloriesTextView)
        val deleteMealButton: ImageButton = itemView.findViewById(R.id.deleteMealButton)
        val expandableLayout: LinearLayout = itemView.findViewById(R.id.expandableLayout)
        val mealQuantityTextView: TextView = itemView.findViewById(R.id.mealQuantityTextView)
        val mealCarbsTextView: TextView = itemView.findViewById(R.id.mealCarbsTextView)
        val mealProteinTextView: TextView = itemView.findViewById(R.id.mealProteinTextView)
        val mealFatTextView: TextView = itemView.findViewById(R.id.mealFatTextView)
        val timeAddedTextView: TextView = itemView.findViewById(R.id.timeAddedTextView) // Adăugăm TextView-ul pentru ora adăugării

        fun bind(meal: Meal) {
            mealNameTextView.text = meal.name
            mealCaloriesTextView.text = "${meal.calories} kcal"
            timeAddedTextView.text = "Added at: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(meal.timestamp.toDate())}" // Setăm ora adăugării

            // Bind expandable content initially
            bindExpandableContent(meal)

            itemView.setOnClickListener {
                if (expandableLayout.visibility == View.GONE) {
                    expandableLayout.visibility = View.VISIBLE
                } else {
                    expandableLayout.visibility = View.GONE
                }
            }
            deleteMealButton.setOnClickListener {
                onDeleteMeal(meal)
            }
        }

        private fun bindExpandableContent(meal: Meal) {
            mealQuantityTextView.text = "Quantity: ${meal.quantity} g"
            mealCarbsTextView.text = "Carbs: ${meal.carbs} g"
            mealProteinTextView.text = "Protein: ${meal.protein} g"
            mealFatTextView.text = "Fat: ${meal.fat} g"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(meals[position])
    }

    override fun getItemCount() = meals.size

    fun removeMeal(meal: Meal) {
        val position = meals.indexOf(meal)
        if (position != -1) {
            meals.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
