package com.example.gymformcoach.core.utils

object PrEstimator {
    // Epley formula
    fun estimatedOneRepMax(weightKg: Float, reps: Int): Float = weightKg * (1 + reps / 30f)
}
