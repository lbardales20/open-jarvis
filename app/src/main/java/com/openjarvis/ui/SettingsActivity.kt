
package com.openjarvis.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.openjarvis.ui.settings.SettingsScreen
import com.openjarvis.voice.VoiceManager

class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var voiceManager: VoiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            prefs = EncryptedSharedPreferences.create(
                this,
                "jarvis_encrypted_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            finish()
            return
        }

        voiceManager = VoiceManager(applicationContext)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                    onSaveProvider = { name, baseUrl, apiKey, model ->
                        saveProviderSettings(name, baseUrl, apiKey, model)
                    },
                    initialVoiceEnabled = voiceManager.isVoiceEnabled(),
                    initialSpeakResults = voiceManager.isSpeakResultsEnabled(),
                    onVoiceEnabledChange = { enabled ->
                        voiceManager.setVoiceEnabled(enabled)
                    },
                    onSpeakResultsChange = { enabled ->
                        voiceManager.setSpeakResultsEnabled(enabled)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun saveProviderSettings(
        name: String,
        baseUrl: String,
        apiKey: String,
        model: String
    ) {
        prefs.edit()
            .putString("provider_name", name)
            .putString("provider_base_url", baseUrl)
            .putString("provider_api_key", apiKey)
            .putString("provider_model", model)
            .apply()
    }
}
