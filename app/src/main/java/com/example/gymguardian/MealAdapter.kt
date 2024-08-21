package com.example.gymguardian

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class MealAdapter(
    val meals: MutableList<Meal>,
    private val date: String,  // Adăugăm date aici
    private val mealType: String,  // Adăugăm mealType aici
    private val onDeleteMeal: (Meal) -> Unit
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    inner class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealNameTextView: TextView = itemView.findViewById(R.id.mealNameTextView)
        val mealCaloriesTextView: TextView = itemView.findViewById(R.id.mealCaloriesTextView)
        val deleteMealButton: ImageButton = itemView.findViewById(R.id.deleteMealButton)

        fun bind(meal: Meal) {
            mealNameTextView.text = meal.name
            mealCaloriesTextView.text = "${meal.calories} kcal"

            itemView.setOnClickListener {
                val dialog = MealDetailDialogFragment.newInstance(meal, date, mealType)
                dialog.show((itemView.context as AppCompatActivity).supportFragmentManager, "MealDetailDialog")
            }

            deleteMealButton.setOnClickListener {
                onDeleteMeal(meal)
            }
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
