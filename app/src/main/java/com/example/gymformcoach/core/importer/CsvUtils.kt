package com.example.gymformcoach.core.importer

/** Minimal RFC 4180-ish CSV line splitter - handles quoted fields containing commas. No external dependency. */
fun splitCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields += current.toString()
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    fields += current.toString()
    return fields.map { it.trim() }
}
