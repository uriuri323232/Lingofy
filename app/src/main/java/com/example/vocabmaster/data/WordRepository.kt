package com.example.vocabmaster.data

import android.content.Context
import org.json.JSONArray

class WordRepository(private val context: Context) {
    val words: List<Word> by lazy {
        val json = context.assets.open("words.json").bufferedReader().use { it.readText() }
        val a = JSONArray(json)
        buildList(a.length()) { for (i in 0 until a.length()) { val o=a.getJSONObject(i); add(Word(o.getString("en"),o.getString("he"),o.getString("category"))) } }
            .distinctBy { it.en.lowercase() }
    }
    val categories: List<String> get() = words.map { it.category }.distinct().sorted()
}
