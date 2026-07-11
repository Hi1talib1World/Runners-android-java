package com.denzo.runners.core.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AudioCoach(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isReady = true
        }
    }

    fun announceSplit(km: Int) {
        speak("$km kilometers completed.")
    }

    fun announceCheer(from: String) {
        speak("$from sent you a cheer! Keep pushing!")
    }

    fun announceWorkoutStep(instruction: String) {
        speak("New Step: $instruction")
    }

    private fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
