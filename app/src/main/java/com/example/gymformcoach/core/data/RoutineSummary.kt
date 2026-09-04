package com.example.gymformcoach.core.data

data class RoutineSummary(
    val id: String,
    val name: String,
    val description: String,
    val exerciseCount: Int,
    val lastPerformedDate: Long?
)
