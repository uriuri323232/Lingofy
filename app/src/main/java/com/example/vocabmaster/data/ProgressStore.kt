package com.example.vocabmaster.data

import android.content.Context
import java.time.LocalDate
import kotlin.random.Random

class ProgressStore(context: Context) {
    private val p = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private fun key(word: String, suffix: String) = "${word.lowercase()}_$suffix"

    // ---- התקדמות למילה ----
    fun score(word: String) = p.getInt(key(word, "score"), 0)
    fun tries(word: String) = p.getInt(key(word, "n"), 0)
    fun mark(word: String, correct: Boolean) {
        val s = if (correct) (score(word) + 1).coerceAtMost(20) else (score(word) - 1).coerceAtLeast(-10)
        p.edit().putInt(key(word, "score"), s).putInt(key(word, "n"), tries(word) + 1).apply()
    }
    fun isFavorite(word: String) = p.getBoolean(key(word, "fav"), false)
    fun toggleFavorite(word: String) { p.edit().putBoolean(key(word, "fav"), !isFavorite(word)).apply() }

    /** 0 חדשה, 1 קשה, 2 בלמידה, 3 נלמדה */
    fun level(word: String): Int {
        if (tries(word) == 0) return 0
        val s = score(word)
        return when { s <= 0 -> 1; s < 3 -> 2; else -> 3 }
    }
    fun learnedCount(words: List<Word>) = words.count { level(it.en) == 3 }
    fun favorites(words: List<Word>) = words.filter { isFavorite(it.en) }
    fun weakWords(words: List<Word>) = words.filter { level(it.en) == 1 }.sortedBy { score(it.en) }

    /** דגימה משוקללת: מילים חדשות וחלשות מופיעות לעתים קרובות יותר (חזרה מרווחת פשוטה) */
    fun sample(words: List<Word>, n: Int): List<Word> {
        val rest = words.toMutableList(); val out = mutableListOf<Word>()
        while (out.size < n && rest.isNotEmpty()) {
            val w = rest.map { if (tries(it.en) == 0) 12.0 else (10 - 2 * score(it.en)).coerceIn(1, 22).toDouble() }
            var r = Random.nextDouble() * w.sum(); var idx = w.lastIndex
            for (i in w.indices) { r -= w[i]; if (r <= 0) { idx = i; break } }
            out += rest.removeAt(idx)
        }
        return out
    }

    // ---- חידון ----
    fun attempts() = p.getInt("attempts", 0)
    fun successes() = p.getInt("successes", 0)
    fun recordQuiz(correct: Boolean) {
        p.edit().putInt("attempts", attempts() + 1).putInt("successes", successes() + if (correct) 1 else 0).apply()
    }

    // ---- רצף ויעד יומי ----
    private fun today() = LocalDate.now().toString()
    fun todayCount() = p.getInt("day_${today()}", 0)
    fun reviews() = p.getInt("reviews", 0)
    fun bestStreak() = p.getInt("best", 0)
    fun streak(): Int {
        val last = p.getString("lastDay", "") ?: ""
        return if (last == today() || last == LocalDate.now().minusDays(1).toString()) p.getInt("streak", 0) else 0
    }
    fun touch() {
        val last = p.getString("lastDay", "") ?: ""
        val next = when (last) { today() -> p.getInt("streak", 1); LocalDate.now().minusDays(1).toString() -> p.getInt("streak", 0) + 1; else -> 1 }
        p.edit().putInt("streak", next).putInt("best", maxOf(bestStreak(), next)).putString("lastDay", today())
            .putInt("day_${today()}", todayCount() + 1).putInt("reviews", reviews() + 1).apply()
    }

    fun lastDays(n: Int): List<Pair<LocalDate, Int>> =
        (n - 1 downTo 0).map { val d = LocalDate.now().minusDays(it.toLong()); d to p.getInt("day_$d", 0) }
    fun levelCounts(words: List<Word>): IntArray { val c = IntArray(4); words.forEach { c[level(it.en)]++ }; return c }

    // ---- הגדרות ----
    var themeMode: Int
        get() = p.getInt("theme", 0)
        set(v) { p.edit().putInt("theme", v).apply() }
    var dailyGoal: Int
        get() = p.getInt("goal", 20)
        set(v) { p.edit().putInt("goal", v).apply() }
    var speechRate: Float
        get() = p.getFloat("rate", 0.9f)
        set(v) { p.edit().putFloat("rate", v).apply() }
    var autoSpeak: Boolean
        get() = p.getBoolean("autoSpeak", true)
        set(v) { p.edit().putBoolean("autoSpeak", v).apply() }
    var heFirst: Boolean
        get() = p.getBoolean("heFirst", false)
        set(v) { p.edit().putBoolean("heFirst", v).apply() }

    fun resetProgress() {
        val keep = mapOf("theme" to themeMode, "goal" to dailyGoal)
        val rate = speechRate; val auto = autoSpeak; val he = heFirst
        p.edit().clear().apply()
        p.edit().putInt("theme", keep["theme"]!!).putInt("goal", keep["goal"]!!).putFloat("rate", rate)
            .putBoolean("autoSpeak", auto).putBoolean("heFirst", he).apply()
    }
}
