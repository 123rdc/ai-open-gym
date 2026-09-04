package com.example.gymformcoach.core.analysis

import android.content.Context
import org.json.JSONArray

data class CorrectiveKbEntry(
    val issue: String,
    val likelyCauses: List<String>,
    val correctives: List<String>
)

object CorrectivesKnowledgeBase {
    @Volatile
    private var cached: List<CorrectiveKbEntry>? = null

    fun load(context: Context): List<CorrectiveKbEntry> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val json = context.assets.open("correctives_kb.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val entries = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                CorrectiveKbEntry(
                    issue = obj.getString("issue"),
                    likelyCauses = obj.getJSONArray("likely_causes").toStringList(),
                    correctives = obj.getJSONArray("correctives").toStringList()
                )
            }
            cached = entries
            return entries
        }
    }

    fun match(context: Context, issueKeys: List<String>): List<CorrectiveKbEntry> {
        if (issueKeys.isEmpty()) return emptyList()
        val all = load(context)
        return issueKeys.mapNotNull { key -> all.find { it.issue == key } }
    }

    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }
}
