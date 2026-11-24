
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
        val youtubePattern = Regex(
            """(?i)(?:jarvis[ ,]*)?(?:abre|abrir|open)\\s+(?:la\\s+)?(?:app\\s+)?youtube\\s+(?:y\\s+)?(?:busca|buscar|search)\\s+(.+)"""
        )
        val query = youtubePattern.find(command.trim())?.groupValues?.getOrNull(1)?.trim()

        if (!query.isNullOrBlank()) {
            val searchUri = Uri.parse(
                "https://www.youtube.com/results?search_query=" + Uri.encode(query)
            )
            try {
                startActivity(Intent(Intent.ACTION_VIEW, searchUri))
                return
            } catch (_: Exception) {
                // Fall through to the AI agent if YouTube cannot be opened.
            }
        }

        agentCore.executeTask(command)
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
