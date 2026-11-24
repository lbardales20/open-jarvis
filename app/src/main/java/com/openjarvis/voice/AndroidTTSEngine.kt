
package com.openjarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class AndroidTTSEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var isReleased = false

    private var speechRate = 1.05f
    private var pitch = 0.95f

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS && !isReleased) {
                val engine = tts ?: return@TextToSpeech

                val languageResult = engine.setLanguage(Locale("es", "ES"))

                isReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED

                engine.setSpeechRate(speechRate)
                engine.setPitch(pitch)

                engine.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}

                        override fun onDone(utteranceId: String?) {}

                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "Error speaking utterance: $utteranceId")
                        }
                    }
                )

                if (!isReady) {
                    Log.w(TAG, "Spanish voice unavailable; using default language")
                    isReady = true
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed: $status")
            }
        }
    }

    fun speak(text: String) {
        speakInternal(text, TextToSpeech.QUEUE_FLUSH)
    }

    fun speakQueued(text: String) {
        speakInternal(text, TextToSpeech.QUEUE_ADD)
    }

    private fun speakInternal(text: String, queueMode: Int) {
        if (!isReady || isReleased) return

        val clean = cleanText(text)
        if (clean.isBlank()) return

        val utteranceId = "jarvis_${UUID.randomUUID()}"

        val result = tts?.speak(
            clean,
            queueMode,
            null,
            utteranceId
        )

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Could not start speech")
        }
    }

    private fun cleanText(text: String): String {
        return text
            .replace(Regex("\\[\\{.*?\\}\\]"), "")
            .replace(Regex("[\\[\\]{}\":]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech", e)
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        if (isReady) {
            tts?.setSpeechRate(speechRate)
        }
    }

    fun setPitch(pitchValue: Float) {
        pitch = pitchValue.coerceIn(0.5f, 2.0f)
        if (isReady) {
            tts?.setPitch(pitch)
        }
    }

    fun getSpeechRate(): Float = speechRate

    fun getPitch(): Float = pitch

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    fun isReady(): Boolean = isReady && !isReleased

    fun release() {
        if (isReleased) return

        isReleased = true
        isReady = false

        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing TextToSpeech", e)
        } finally {
            tts = null
        }
    }

    companion object {
        private const val TAG = "AndroidTTSEngine"
    }
}
