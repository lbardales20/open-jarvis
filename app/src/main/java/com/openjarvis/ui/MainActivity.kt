
package com.openjarvis.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.agent.AgentCore
import com.openjarvis.agent.AgentState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.ui.dashboard.DashboardScreen
import com.openjarvis.ui.tasks.TasksScreen
import com.openjarvis.voice.VoiceManager

class MainActivity : ComponentActivity() {

    private lateinit var graphifyRepo: GraphifyRepository
    private lateinit var voiceManager: VoiceManager
    private lateinit var agentCore: AgentCore

    private var permissionRefresh by mutableStateOf(0)
    private var showTasks by mutableStateOf(false)

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (::voiceManager.isInitialized) voiceManager.startListening()
        } else {
            Toast.makeText(this, "Jarvis necesita permiso de micrófono para escucharte", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        graphifyRepo = GraphifyRepository(this)
        agentCore = AgentCore(applicationContext)
        voiceManager = VoiceManager(applicationContext) { command ->
            handleVoiceCommand(command)
        }

        lifecycleScope.launch {
            agentCore.state.collect { state ->
                when (state) {
                    is AgentState.Done -> {
                        if (state.result.startsWith("CHAT:")) {
                            voiceManager.speak(state.result.removePrefix("CHAT:").trim())
                        } else {
                            voiceManager.speak("Listo. He terminado la tarea.")
                        }
                    }
                    is AgentState.Error -> voiceManager.speak("No pude completar la orden. " + state.message)
                    else -> Unit
                }
            }
        }

        setContent {
            MaterialTheme {
                val refresh = permissionRefresh

                val accessibilityEnabled = isAccessibilityServiceEnabled()
                val overlayEnabled = Settings.canDrawOverlays(this)

                if (!accessibilityEnabled || !overlayEnabled) {
                    PermissionScreen(
                        accessibilityEnabled = accessibilityEnabled,
                        overlayEnabled = overlayEnabled,
                        onEnableAccessibility = {
                            startAccessibilitySettings()
                        },
                        onEnableOverlay = {
                            startOverlaySettings()
                        }
                    )
                } else if (showTasks) {
                    TasksScreen(
                        graphifyRepo = graphifyRepo,
                        onNavigateBack = { showTasks = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DashboardScreen(
                        onStartVoice = {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                voiceManager.startListening()
                            } else {
                                requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onOpenTasks = {
                            showTasks = true
                        },
                        onOpenSettings = {
                            openSettings()
                        },
                        graphifyRepo = graphifyRepo,
                        voiceManager = voiceManager,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionRefresh++
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val componentName = ComponentName(
            this,
            JarvisAccessibilityService::class.java
        )

        return enabledServices.contains(
            componentName.flattenToString(),
            ignoreCase = true
        )
    }

    private fun startAccessibilitySettings() {
        startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
    }

    private fun startOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
        )
    }

    private fun startOverlayService() {
        startService(
            Intent(this, OverlayService::class.java)
        )

        Toast.makeText(
            this,
            "Jarvis está activo",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleVoiceCommand(command: String) {
        val clean = command.trim().replace(Regex("(?i)^jarvis[,:\\s]*"), "").trim().trimEnd('.', ',', '!', '?')

        val youtubePattern = Regex(
            """(?i)(?:abre|abrir|open)\s+(?:la\s+)?(?:app\s+)?youtube\s+(?:y\s+)?(?:busca|buscar|search)\s+(.+)"""
        )
        val query = youtubePattern.find(clean)?.groupValues?.getOrNull(1)?.trim()
        if (!query.isNullOrBlank()) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                    )
                )
                return
            } catch (_: Exception) { }
        }

        val openPattern = Regex(
            """(?i)^(?:por favor\s+)?(?:abre|abrir|open|inicia|iniciar|lanza|launch)\s+(?:la\s+)?(?:app\s+|aplicación\s+)?(.+)$"""
        )
        val appName = openPattern.find(clean)?.groupValues?.getOrNull(1)
            ?.trim()?.trimEnd('.', ',', '!', '?')
        if (!appName.isNullOrBlank() && openInstalledApp(appName)) return

        val looksLikeQuestion = clean.contains("?") ||
            clean.startsWith("qué", true) || clean.startsWith("que", true) ||
            clean.startsWith("cuál", true) || clean.startsWith("cual", true) ||
            clean.startsWith("quién", true) || clean.startsWith("quien", true) ||
            clean.startsWith("cómo", true) || clean.startsWith("como", true) ||
            clean.startsWith("dónde", true) || clean.startsWith("donde", true) ||
            clean.contains("capital de", true) ||
            clean.startsWith("why", true) || clean.startsWith("what", true) ||
            clean.startsWith("who", true) || clean.startsWith("how", true)
        if (looksLikeQuestion) {
            agentCore.answerQuestion(clean)
        } else {
            agentCore.executeTask(clean)
        }
    }

    private fun openInstalledApp(requestedName: String): Boolean {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcherIntent, 0)
        val requested = requestedName.lowercase().replace(" ", "").replace("á", "a")
        val match = apps.firstOrNull { info ->
            val label = info.loadLabel(pm).toString().lowercase()
                .replace(" ", "").replace("á", "a")
            label == requested || label.contains(requested) || requested.contains(label)
        } ?: return false
        val launchIntent = pm.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return false
        return try {
            startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openSettings() {
        startActivity(
            Intent(this, SettingsActivity::class.java)
        )
    }
}

@Composable
fun PermissionScreen(
    accessibilityEnabled: Boolean,
    overlayEnabled: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permisos necesarios",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Jarvis necesita permisos de accesibilidad y ventana flotante para controlar tu dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!accessibilityEnabled) {
            Button(
                onClick = onEnableAccessibility,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Activar servicio de accesibilidad")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!overlayEnabled) {
            Button(
                onClick = onEnableOverlay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Permitir ventana flotante")
            }
        }
    }
}
