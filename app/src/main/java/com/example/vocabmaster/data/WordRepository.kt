package com.example.vocabmaster.data

import android.content.Context
import org.json.JSONArray

class WordRepository(private val context: Context) {
    val words: List<Word> by lazy {
        val json = context.assets.open("words.json").bufferedReader().use { it.readText() }
        val a = JSONArray(json)
        buildList(a.length()) {
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                add(Word(o.getString("en").trim(), o.getString("he").trim(), o.getString("category").trim()))
            }
        }.distinctBy { it.en.lowercase() }
    }

    private val order = listOf("בסיסי", "משפחה", "אוכל", "בית", "גוף", "זמן", "צבעים", "נסיעות", "טבע", "טכנולוגיה", "עבודה", "תארים", "מתקדמים")

    val categories: List<String> by lazy {
        val present = words.map { it.category }.distinct()
        present.sortedWith(compareBy({ order.indexOf(it).let { i -> if (i < 0) 999 else i } }, { it }))
    }

    fun inCategory(category: String): List<Word> =
        if (category == ALL) words else words.filter { it.category == category }

    companion object {
        const val ALL = "הכול"
        fun emoji(category: String) = when (category) {
            ALL -> "📚"; "בסיסי" -> "🧩"; "בית" -> "🏠"; "אוכל" -> "🍎"; "נסיעות" -> "✈️"
            "טכנולוגיה" -> "💻"; "טבע" -> "🌿"; "עבודה" -> "💼"; "תארים" -> "✨"
            "מתקדמים" -> "🎓"; "משפחה" -> "👨‍👩‍👧"; "גוף" -> "🖐️"; "זמן" -> "⏰"; "צבעים" -> "🎨"
            else -> "📖"
        }
    }
}
