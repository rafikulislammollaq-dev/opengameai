package ch.duartesantos.opengym.ai.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceCoachService(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isMuted = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
                isInitialized = true
            }
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (isMuted || !isInitialized) return
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "VOICE_COACH_${System.currentTimeMillis()}")
    }

    fun announceRep(repNumber: Int, score: Int, isParallel: Boolean) {
        val repWord = "Rep $repNumber."
        val cue = if (isParallel) {
            if (score >= 90) "Great depth!" else "Good rep!"
        } else {
            "Go deeper next rep."
        }
        speak("$repWord $cue", flush = true)
    }

    fun announcePhase(phaseName: String) {
        speak(phaseName, flush = true)
    }

    fun announceCue(cue: String) {
        speak(cue, flush = false)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
