package com.example.gymguardian

import java.util.UUID

data class Meal(
    var id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val calories: Int = 0,
    val carbs: Int = 0,
    val protein: Int = 0,
    val fat: Int = 0,
    val quantity: Int = 0,
    var timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)
