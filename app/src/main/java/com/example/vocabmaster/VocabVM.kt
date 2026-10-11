package com.example.vocabmaster

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.vocabmaster.data.*
import java.time.LocalDate

enum class Screen { HOME, LEARN, QUIZ, SEARCH, FAVORITES, STATS, SETTINGS }

enum class QuizMode(val title: String, val desc: String, val emoji: String) {
    EN_HE("אנגלית ← עברית", "רואים מילה באנגלית ובוחרים תרגום", "🇬🇧"),
    HE_EN("עברית ← אנגלית", "רואים מילה בעברית ובוחרים באנגלית", "🇮🇱"),
    LISTEN("האזנה", "שומעים את המילה ובוחרים את התרגום", "🎧"),
    TYPE("כתיבה", "רואים תרגום וכותבים את המילה באנגלית", "⌨️")
}

data class Question(val word: Word, val options: List<Word>)

class VocabVM : ViewModel() {
    lateinit var repo: WordRepository
    lateinit var store: ProgressStore
    private var inited = false

    /** מונה שמעודכן בכל שינוי בהתקדמות כדי לרענן מסכים */
    var tick by mutableIntStateOf(0); private set
    private fun bump() { tick++ }

    var screen by mutableStateOf(Screen.HOME)
    var category by mutableStateOf(WordRepository.ALL)
    var query by mutableStateOf("")

    // הגדרות
    var themeMode by mutableIntStateOf(0)
    var goal by mutableIntStateOf(20)
    var rate by mutableStateOf(0.9f)
    var autoSpeak by mutableStateOf(true)
    var heFirst by mutableStateOf(false)

    // לימוד
    var learnTitle by mutableStateOf("לימוד")
    var learnList by mutableStateOf(listOf<Word>())
    var learnIndex by mutableIntStateOf(0)
    var flipped by mutableStateOf(false)
    var learnKnown by mutableIntStateOf(0)
    var learnAgain by mutableIntStateOf(0)
    private val requeued = mutableSetOf<String>()

    // חידון
    var quizMode by mutableStateOf<QuizMode?>(null)
    var questions by mutableStateOf(listOf<Question>())
    var qIndex by mutableIntStateOf(0)
    var qScore by mutableIntStateOf(0)
    var answered by mutableStateOf(false)
    var pickedEn by mutableStateOf<String?>(null)
    var lastCorrect by mutableStateOf(false)
    var typed by mutableStateOf("")

    fun init(context: Context) {
        if (inited) return
        val app = context.applicationContext
        repo = WordRepository(app); store = ProgressStore(app)
        themeMode = store.themeMode; goal = store.dailyGoal; rate = store.speechRate
        autoSpeak = store.autoSpeak; heFirst = store.heFirst
        inited = true
    }

    fun scope(): List<Word> = repo.inCategory(category)
    fun scopeTitle() = if (category == WordRepository.ALL) "כל המילים" else category
    fun wordOfDay(): Word = repo.words[(LocalDate.now().toEpochDay() % repo.words.size).toInt()]

    // ---------- הגדרות ----------
    fun setTheme(v: Int) { themeMode = v; store.themeMode = v }
    fun setGoal(v: Int) { goal = v; store.dailyGoal = v; bump() }
    fun setRate(v: Float) { rate = v; store.speechRate = v }
    fun setAutoSpeak(v: Boolean) { autoSpeak = v; store.autoSpeak = v }
    fun toggleDirection() { heFirst = !heFirst; store.heFirst = heFirst; flipped = false }
    fun reset() { store.resetProgress(); bump() }

    fun toggleFav(w: Word) { store.toggleFavorite(w.en); bump() }

    // ---------- לימוד ----------
    fun startLearn(source: List<Word>, title: String, limit: Int = 20) {
        learnList = store.sample(source, limit); learnTitle = title
        learnIndex = 0; flipped = false; learnKnown = 0; learnAgain = 0; requeued.clear()
        screen = Screen.LEARN
    }
    fun startWeak() = startLearn(store.weakWords(repo.words), "מילים לחיזוק")
    fun answerLearn(known: Boolean) {
        val w = learnList.getOrNull(learnIndex) ?: return
        store.mark(w.en, known); store.touch()
        if (known) learnKnown++ else {
            learnAgain++
            if (requeued.add(w.en.lowercase())) {
                val at = minOf(learnIndex + 4, learnList.size)
                learnList = learnList.toMutableList().apply { add(at, w) }
            }
        }
        learnIndex++; flipped = false; bump()
    }

    // ---------- חידון ----------
    fun openQuiz() { quizMode = null; screen = Screen.QUIZ }
    fun startQuiz(mode: QuizMode) {
        quizMode = mode
        val picks = store.sample(scope(), 10)
        val all = repo.words
        questions = picks.map { w ->
            val same = all.filter { it.category == w.category && it.heMain != w.heMain && it.en != w.en }.shuffled()
            val other = all.filter { it.category != w.category && it.heMain != w.heMain }.shuffled()
            val d = mutableListOf<Word>()
            for (c in same + other) { if (d.size == 3) break; if (d.none { it.heMain == c.heMain }) d += c }
            Question(w, (d + w).shuffled())
        }
        qIndex = 0; qScore = 0; answered = false; pickedEn = null; typed = ""
    }
    fun answerChoice(option: Word) {
        if (answered) return
        val q = questions.getOrNull(qIndex) ?: return
        finishAnswer(q.word, option.en == q.word.en); pickedEn = option.en
    }
    fun checkTyped() {
        if (answered) return
        val q = questions.getOrNull(qIndex) ?: return
        val t = typed.trim().lowercase()
        if (t.isEmpty()) return
        val ok = repo.words.filter { it.heMain == q.word.heMain }.any { it.en.lowercase() == t }
        finishAnswer(q.word, ok)
    }
    private fun finishAnswer(w: Word, correct: Boolean) {
        answered = true; lastCorrect = correct
        if (correct) qScore++
        store.mark(w.en, correct); store.recordQuiz(correct); store.touch(); bump()
    }
    fun nextQuestion() { qIndex++; answered = false; pickedEn = null; typed = "" }
    fun leaveQuiz() { quizMode = null; screen = Screen.HOME }
}
