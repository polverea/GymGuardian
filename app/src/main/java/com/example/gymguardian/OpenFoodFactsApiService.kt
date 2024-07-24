package com.example.gymguardian

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFoodFactsApiService {
    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("lang") lang: String = "en"
    ): Response<ProductResponse>
}

data class ProductResponse(
    val products: List<Product>
)

data class Product(
    val product_name: String = "",
    val nutriments: Nutriments = Nutriments(),
    var timestamp: com.google.firebase.Timestamp? = null
)

data class Nutriments(
    @SerializedName("energy-kcal")
    val energyKcal: Float? = 0f,
    val carbohydrates: Float? = 0f,
    val proteins: Float? = 0f,
    val fat: Float? = 0f
)
