package com.example.data.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceAssistantHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    var isMuted = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
            }
        }
    }

    fun speak(text: String) {
        if (isMuted || !isInitialized || text.isBlank()) return
        // Clean markdown formatting before speaking
        val cleanText = text.replace(Regex("[#*_`]"), "").take(250)
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "NotesAiVoice")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
