package com.example.vocabmaster.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** עטיפה למנוע הדיבור של המכשיר. אם אין מנוע מותקן – פשוט לא משמיע. */
class Speaker(context: Context) {
    private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.US)
                ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speak(text: String, rate: Float) {
        if (!ready) return
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "w")
    }

    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null; ready = false }
}
