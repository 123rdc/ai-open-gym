package com.example.gymformcoach.core.data

import androidx.room.TypeConverter

private const val DELIMITER = "||"

class Converters {
    @TypeConverter
    fun fromStringSet(value: Set<String>): String = value.joinToString(DELIMITER)

    @TypeConverter
    fun toStringSet(value: String): Set<String> =
        if (value.isBlank()) emptySet() else value.split(DELIMITER).toSet()

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(DELIMITER)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(DELIMITER)
}
