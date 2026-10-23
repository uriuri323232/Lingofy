package com.example.vocabmaster

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.vocabmaster.data.*
import java.time.LocalDate
import kotlin.random.Random

enum class Screen { HOME, LEARN, QUIZ, SEARCH, FAVORITES, STATS, SETTINGS }

enum class QuizMode(val title: String, val desc: String, val emoji: String) {
    EN_HE("אנגלית ← עברית", "רואים מילה באנגלית ובוחרים תרגום", "🇬🇧"),
    HE_EN("עברית ← אנגלית", "רואים מילה בעברית ובוחרים באנגלית", "🇮🇱"),
    LISTEN("האזנה", "שומעים את המילה ובוחרים את התרגום", "🎧"),
    TYPE("כתיבה", "רואים תרגום וכותבים את המילה באנגלית", "⌨️"),
    TF("נכון / לא נכון", "האם התרגום שמוצג מתאים למילה?", "✅"),
    MIX("מעורב", "כל שאלה בסוג אחר, לאתגר מלא", "🎲")
}

/** [shown] משמש רק בנכון/לא־נכון: המילה שהתרגום שלה מוצג (זהה ל־word כשהטענה נכונה). [mode] הסוג בפועל של השאלה. */
data class Question(val word: Word, val options: List<Word>, val mode: QuizMode, val shown: Word = word)

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
    var quizLen by mutableIntStateOf(10)
    var learnLen by mutableIntStateOf(20)
    var skipLearned by mutableStateOf(false)
    /** מקור החידון: 0 הקטגוריה הנבחרת, 1 מילים קשות, 2 מועדפים */
    var quizSource by mutableIntStateOf(0)
    var hint by mutableStateOf(false)

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
    var combo by mutableIntStateOf(0)
    var bestCombo by mutableIntStateOf(0)

    fun init(context: Context) {
        if (inited) return
        val app = context.applicationContext
        repo = WordRepository(app); store = ProgressStore(app)
        themeMode = store.themeMode; goal = store.dailyGoal; rate = store.speechRate
        autoSpeak = store.autoSpeak; heFirst = store.heFirst
        quizLen = store.quizLen; learnLen = store.learnLen; skipLearned = store.skipLearned
        inited = true
    }

    fun scope(): List<Word> = repo.inCategory(category)
    /** הרשימה לתרגול: אם הוגדר "דלג על מילים שנלמדו" – מסננים אותן (כל עוד נשארות לפחות 4 מילים) */
    fun pool(): List<Word> {
        val all = scope()
        if (!skipLearned) return all
        val rest = all.filter { store.level(it.en) != 3 }
        return if (rest.size >= 4) rest else all
    }
    fun sourceList(src: Int = quizSource): List<Word> = when (src) {
        1 -> store.weakWords(repo.words)
        2 -> store.favorites(repo.words)
        else -> pool()
    }
    fun scopeTitle() = if (category == WordRepository.ALL) "כל המילים" else category
    fun wordOfDay(): Word = repo.words[(LocalDate.now().toEpochDay() % repo.words.size).toInt()]

    // ---------- הגדרות ----------
    fun setTheme(v: Int) { themeMode = v; store.themeMode = v }
    fun updateGoal(v: Int) { goal = v; store.dailyGoal = v; bump() }
    fun updateRate(v: Float) { rate = v; store.speechRate = v }
    fun updateAutoSpeak(v: Boolean) { autoSpeak = v; store.autoSpeak = v }
    fun toggleDirection() { heFirst = !heFirst; store.heFirst = heFirst; flipped = false }
    fun updateQuizLen(v: Int) { quizLen = v; store.quizLen = v }
    fun updateLearnLen(v: Int) { learnLen = v; store.learnLen = v }
    fun updateSkipLearned(v: Boolean) { skipLearned = v; store.skipLearned = v; bump() }
    fun reset() { store.resetProgress(); bump() }

    fun toggleFav(w: Word) { store.toggleFavorite(w.en); bump() }

    // ---------- לימוד ----------
    fun startLearn(source: List<Word>, title: String, limit: Int = learnLen) {
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
        var src = sourceList()
        if (src.size < 4) src = pool()
        val picks = store.sample(src, quizLen)
        val all = repo.words
        val kinds = listOf(QuizMode.EN_HE, QuizMode.HE_EN, QuizMode.LISTEN, QuizMode.TYPE, QuizMode.TF)
        questions = picks.map { w ->
            val same = all.filter { it.category == w.category && it.heMain != w.heMain && it.en != w.en }.shuffled()
            val other = all.filter { it.category != w.category && it.heMain != w.heMain }.shuffled()
            val d = mutableListOf<Word>()
            for (c in same + other) { if (d.size == 3) break; if (d.none { it.heMain == c.heMain }) d += c }
            val m = if (mode == QuizMode.MIX) kinds.random() else mode
            // נכון/לא־נכון: חצי מהשאלות מציגות תרגום שגוי
            val shown = if (m == QuizMode.TF && d.isNotEmpty() && Random.nextBoolean()) d.first() else w
            Question(w, (d + w).shuffled(), m, shown)
        }
        qIndex = 0; qScore = 0; combo = 0; bestCombo = 0; answered = false; pickedEn = null; typed = ""; hint = false
    }
    fun answerTF(saidTrue: Boolean) {
        if (answered) return
        val q = questions.getOrNull(qIndex) ?: return
        pickedEn = saidTrue.toString()
        finishAnswer(q.word, (q.shown.en == q.word.en) == saidTrue)
    }
    fun answerChoice(option: Word) {
        if (answered) return
        val q = questions.getOrNull(qIndex) ?: return
        finishAnswer(q.word, option.en == q.word.en); pickedEn = option.en
    }
    fun checkTyped() {
        if (answered) return
        val q = questions.getOrNull(qIndex) ?: return
        val t = typed.trim().lowercase().replace('’', '\'').replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return
        val ok = repo.words.filter { it.heMain == q.word.heMain }.any { it.en.lowercase() == t }
        finishAnswer(q.word, ok)
    }
    private fun finishAnswer(w: Word, correct: Boolean) {
        answered = true; lastCorrect = correct
        if (correct) { qScore++; combo++; if (combo > bestCombo) bestCombo = combo } else combo = 0
        store.mark(w.en, correct); store.recordQuiz(correct); store.touch(); bump()
    }
    fun nextQuestion() { qIndex++; answered = false; pickedEn = null; typed = ""; hint = false }
    fun leaveQuiz() { quizMode = null; screen = Screen.HOME }
}
