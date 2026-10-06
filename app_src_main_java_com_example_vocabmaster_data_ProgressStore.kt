package com.example.vocabmaster.data

import android.content.Context
import java.time.LocalDate

class ProgressStore(context: Context) {
    private val p = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private fun key(word: String, suffix: String) = "${word.lowercase()}_$suffix"
    fun score(word: String) = p.getInt(key(word,"score"), 0)
    fun correct(word: String) { p.edit().putInt(key(word,"score"), (score(word)+1).coerceAtMost(20)).apply() }
    fun wrong(word: String) { p.edit().putInt(key(word,"score"), (score(word)-1).coerceAtLeast(-10)).apply() }
    fun isFavorite(word: String) = p.getBoolean(key(word,"fav"), false)
    fun toggleFavorite(word: String) { p.edit().putBoolean(key(word,"fav"), !isFavorite(word)).apply() }
    fun attempts() = p.getInt("attempts",0)
    fun successes() = p.getInt("successes",0)
    fun streak() = p.getInt("streak",0)
    fun record(correct: Boolean) {
        val today = LocalDate.now().toString(); val last = p.getString("lastDay", "")
        val nextStreak = when { last == today -> streak(); last == LocalDate.now().minusDays(1).toString() -> streak()+1; else -> 1 }
        p.edit().putInt("attempts", attempts()+1).putInt("successes", successes()+if(correct)1 else 0).putInt("streak",nextStreak).putString("lastDay",today).apply()
    }
    fun learnedCount(words: List<Word>) = words.count { score(it.en) >= 2 }
    fun favorites(words: List<Word>) = words.filter { isFavorite(it.en) }
}
