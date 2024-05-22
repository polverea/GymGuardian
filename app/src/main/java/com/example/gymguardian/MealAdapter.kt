package com.example.gymguardian

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MealAdapter(private val mealList: List<Meal>) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = mealList[position]
        holder.mealNameTextView.text = meal.name
        holder.mealCaloriesTextView.text = "${meal.calories} kcal"
        holder.mealQuantityTextView.text = "Quantity: ${meal.quantity} g"
        holder.mealCarbsTextView.text = "Carbs: ${meal.carbs} g"
        holder.mealProteinTextView.text = "Protein: ${meal.protein} g"
        holder.mealFatTextView.text = "Fat: ${meal.fat} g"

        holder.headerLayout.setOnClickListener {
            val isVisible = holder.expandableLayout.visibility == View.VISIBLE
            holder.expandableLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }

    override fun getItemCount() = mealList.size

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealNameTextView: TextView = itemView.findViewById(R.id.mealNameTextView)
        val mealCaloriesTextView: TextView = itemView.findViewById(R.id.mealCaloriesTextView)
        val mealQuantityTextView: TextView = itemView.findViewById(R.id.mealQuantityTextView)
        val mealCarbsTextView: TextView = itemView.findViewById(R.id.mealCarbsTextView)
        val mealProteinTextView: TextView = itemView.findViewById(R.id.mealProteinTextView)
        val mealFatTextView: TextView = itemView.findViewById(R.id.mealFatTextView)
        val headerLayout: View = itemView.findViewById(R.id.headerLayout)
        val expandableLayout: View = itemView.findViewById(R.id.expandableLayout)
    }
}
