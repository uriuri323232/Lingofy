package com.example.vocabmaster.data

data class Word(val en: String, val he: String, val category: String) {
    /** התרגום הראשי (לפני " / "), משמש להשוואה ולמניעת תשובות כפולות בחידון */
    val heMain: String get() = he.substringBefore(" / ").trim()
}
