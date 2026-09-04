package com.example.gymformcoach.core.utils

object WeightUnit {
    const val KG = "kg"
    const val LBS = "lbs"

    private const val KG_TO_LBS = 2.2046226f

    fun kgToDisplay(kg: Float, unit: String): Float = if (unit == LBS) kg * KG_TO_LBS else kg

    fun displayToKg(value: Float, unit: String): Float = if (unit == LBS) value / KG_TO_LBS else value

    fun formatKg(kg: Float, unit: String): String {
        val value = kgToDisplay(kg, unit)
        return if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
    }
}
