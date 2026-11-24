
package com.openjarvis.voice

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceManager(
    context: Context,
    private val onCommand: ((String) -> Unit)? = null
) {

    companion object {
        private const val PREFS_NAME = "voice_prefs"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_STT_MODE = "stt_mode"
        private const val KEY_SPEAK_RESULTS = "speak_results"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_LANGUAGE = "assistant_language"
    }

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val sttEngine = AndroidSTTEngine(appContext)
    private val ttsEngine = AndroidTTSEngine(appContext)

    private val scope = CoroutineScope(
        Dispatchers.Main + SupervisorJob()
    )

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    sealed class VoiceState {
        object Idle : VoiceState()
        object Recording : VoiceState()
        object Transcribing : VoiceState()
        data class Result(val text: String) : VoiceState()
        data class Error(val message: String) : VoiceState()
    }

    init {
        ttsEngine.setSpeechRate(getSpeechRate())
    }

    fun isVoiceEnabled(): Boolean =
        prefs.getBoolean(KEY_VOICE_ENABLED, false)

    fun setVoiceEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_VOICE_ENABLED, enabled)
            .apply()
    }

    fun getSTTMode(): STTMode {
        val mode = prefs.getString(
            KEY_STT_MODE,
            STTMode.PUSH_TO_TALK.name
        )

        return try {
            STTMode.valueOf(mode ?: STTMode.PUSH_TO_TALK.name)
        } catch (e: Exception) {
            STTMode.PUSH_TO_TALK
        }
    }

    fun setSTTMode(mode: STTMode) {
        prefs.edit()
            .putString(KEY_STT_MODE, mode.name)
            .apply()
    }

    fun isSpeakResultsEnabled(): Boolean =
        prefs.getBoolean(KEY_SPEAK_RESULTS, true)

    fun setSpeakResultsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SPEAK_RESULTS, enabled)
            .apply()
    }

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "es-ES") ?: "es-ES"

    fun setLanguage(languageTag: String) {
        val safe = if (languageTag == "en-US") "en-US" else "es-ES"
        prefs.edit().putString(KEY_LANGUAGE, safe).apply()
    }

    fun getSpeechRate(): Float =
        prefs.getFloat(KEY_SPEECH_RATE, 1.05f)

    fun setSpeechRate(rate: Float) {
        val safeRate = rate.coerceIn(0.5f, 2.0f)

        prefs.edit()
            .putFloat(KEY_SPEECH_RATE, safeRate)
            .apply()

        ttsEngine.setSpeechRate(safeRate)
    }

    fun startListening() {
        if (!isVoiceEnabled()) {
            _state.value = VoiceState.Error(
                "Voice disabled in settings"
            )
            return
        }

        scope.launch {
            _state.value = VoiceState.Recording

            try {
                sttEngine.startListening(
                    onResult = { text ->
                        if (!text.isNullOrBlank()) {
                            _state.value = VoiceState.Result(text)
                            if (onCommand != null) {
                                if (isSpeakResultsEnabled()) ttsEngine.speak("Entendido. Ejecutaré tu orden.")
                                onCommand.invoke(text)
                            } else if (isSpeakResultsEnabled()) {
                                ttsEngine.speak("He escuchado: $text")
                            }
                        } else {
                            _state.value = VoiceState.Error(
                                "Could not understand audio"
                            )
                        }
                    },
                    onError = { error ->
                        _state.value = VoiceState.Error(error)
                    },
                    languageTag = getLanguage()
                )
            } catch (e: Exception) {
                _state.value = VoiceState.Error(
                    e.message ?: "Could not start voice recognition"
                )
            }
        }
    }

    fun stopListening(): String? {
        return try {
            val text = sttEngine.stop()

            _state.value = if (!text.isNullOrBlank()) {
                VoiceState.Result(text)
            } else {
                VoiceState.Idle
            }

            text
        } catch (e: Exception) {
            _state.value = VoiceState.Error(
                e.message ?: "Could not stop voice recognition"
            )
            null
        }
    }

    fun cancelListening() {
        try {
            sttEngine.cancel()
            _state.value = VoiceState.Idle
        } catch (e: Exception) {
            _state.value = VoiceState.Error(
                e.message ?: "No se pudo cancelar la grabación"
            )
        }
    }

    fun startListeningWithVAD() {
        if (!isVoiceEnabled()) {
            _state.value = VoiceState.Error(
                "Voice disabled in settings"
            )
            return
        }

        scope.launch {
            _state.value = VoiceState.Recording

            try {
                sttEngine.startListening(
                    onResult = { text ->
                        if (!text.isNullOrBlank()) {
                            _state.value = VoiceState.Result(text)
                            if (onCommand != null) {
                                if (isSpeakResultsEnabled()) ttsEngine.speak("Entendido. Ejecutaré tu orden.")
                                onCommand.invoke(text)
                            } else if (isSpeakResultsEnabled()) {
                                ttsEngine.speak("He escuchado: $text")
                            }
                        } else {
                            _state.value = VoiceState.Idle
                        }
                    },
                    onError = { error ->
                        _state.value = VoiceState.Error(error)
                    }
                )
            } catch (e: Exception) {
                _state.value = VoiceState.Error(
                    e.message ?: "Voice recognition failed"
                )
            }
        }
    }

    fun speak(text: String) {
        if (isSpeakResultsEnabled()) {
            ttsEngine.speak(text)
        }
    }

    fun stopSpeaking() {
        ttsEngine.stop()
    }

    fun resetState() {
        if (
            _state.value is VoiceState.Result ||
            _state.value is VoiceState.Error
        ) {
            _state.value = VoiceState.Idle
        }
    }

    fun release() {
        try {
            sttEngine.release()
        } catch (e: Exception) {
            // Avoid crashing during cleanup.
        }

        try {
            ttsEngine.release()
        } catch (e: Exception) {
            // Avoid crashing during cleanup.
        }

        scope.cancel()
    }
}
