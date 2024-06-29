import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gymguardian.R
import com.example.gymguardian.Product

class SearchResultsAdapter(
    private val foodItems: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodCaloriesTextView: TextView = itemView.findViewById(R.id.foodCaloriesTextView)

        init {
            itemView.setOnClickListener {
                onItemClick(foodItems[bindingAdapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val foodItem = foodItems[position]
        holder.foodNameTextView.text = foodItem.product_name
        holder.foodCaloriesTextView.text = "${foodItem.nutriments.energyKcal?.toInt() ?: 0} kcal"

        println("Product in RecyclerView: ${foodItem.product_name}, Calories: ${foodItem.nutriments.energyKcal?.toInt()}")
    }

    override fun getItemCount(): Int {
        return foodItems.size
    }
}
