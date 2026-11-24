
package com.openjarvis.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val SettingsBackground = Color(0xFF09090F)
private val SettingsSurface = Color(0xFF181820)
private val SettingsSurfaceLight = Color(0xFF22222D)
private val SettingsViolet = Color(0xFF9B7BFF)
private val SettingsCyan = Color(0xFF42D9FF)
private val SettingsGreen = Color(0xFF4ADE80)
private val SettingsRed = Color(0xFFFF5C70)
private val SettingsAmber = Color(0xFFFFC15A)
private val SettingsText = Color(0xFFF2F0FA)
private val SettingsSecondary = Color(0xFFB0ADBC)
private val SettingsMuted = Color(0xFF777584)
private val SettingsBorder = Color(0xFF343442)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSaveProvider: (String, String, String, String) -> Unit,
    initialVoiceEnabled: Boolean = false,
    initialSpeakResults: Boolean = true,
    onVoiceEnabledChange: (Boolean) -> Unit = {},
    onSpeakResultsChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedProvider by remember { mutableStateOf("Groq") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("llama-3.1-70b-versatile") }
    var showBaseUrlField by remember { mutableStateOf(false) }
    var showProvidersDropdown by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var savedVisible by remember { mutableStateOf(false) }
    var voiceEnabled by remember(initialVoiceEnabled) { mutableStateOf(initialVoiceEnabled) }
    var speakResults by remember(initialSpeakResults) { mutableStateOf(initialSpeakResults) }
    var sttMode by remember { mutableStateOf("Pulsar para hablar") }

    LaunchedEffect(selectedProvider) {
        showBaseUrlField = selectedProvider == "Custom" ||
            selectedProvider == "Ollama (Local)"
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SettingsBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SettingsHeader(onNavigateBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                SectionLabel("PROVEEDOR DE IA")
                Spacer(modifier = Modifier.height(12.dp))

                ProviderSelectorCard(
                    selectedProvider = selectedProvider,
                    isExpanded = showProvidersDropdown,
                    onToggle = {
                        showProvidersDropdown = !showProvidersDropdown
                    },
                    onSelect = {
                        selectedProvider = it
                        showProvidersDropdown = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                FloatingLabelTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = "URL base",
                    isFocused = false,
                    visible = showBaseUrlField
                )

                FloatingLabelTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "Clave API",
                    isFocused = false,
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = {
                        showPassword = !showPassword
                    }
                )

                FloatingLabelTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "Modelo",
                    isFocused = false
                )

                Text(
                    text = "Compatible con proveedores de inteligencia artificial",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = SettingsMuted
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                SaveProviderButton(
                    onClick = {
                        onSaveProvider(
                            selectedProvider,
                            baseUrl,
                            apiKey,
                            model
                        )
                        savedVisible = true
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                SectionLabel("VOZ")
                Spacer(modifier = Modifier.height(12.dp))

                SettingsToggleRow(
                    title = "Asistente de voz",
                    subtitle = "Pulsar para hablar · detección automática disponible",
                    enabled = voiceEnabled,
                    onToggle = {
                        voiceEnabled = !voiceEnabled
                        onVoiceEnabledChange(voiceEnabled)
                    }
                )

                if (voiceEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsToggleRow(
                        title = "Leer respuestas en voz alta",
                        subtitle = "Jarvis leerá los resultados en voz alta",
                        enabled = speakResults,
                        onToggle = {
                            speakResults = !speakResults
                            onSpeakResultsChange(speakResults)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sttMode =
                                    if (sttMode == "Pulsar para hablar") {
                                        "Automático (VAD)"
                                    } else {
                                        "Pulsar para hablar"
                                    }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = SettingsSurface,
                        border = BorderStroke(1.dp, SettingsBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Modo de reconocimiento",
                                    color = SettingsText,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (sttMode == "Pulsar para hablar") {
                                        "Mantén pulsado para grabar y suelta para enviar"
                                    } else {
                                        "Toca para grabar; se envía al detectar silencio"
                                    },
                                    color = SettingsMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = sttMode,
                                color = SettingsViolet,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SettingsSurface,
                        border = BorderStroke(1.dp, SettingsBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = SettingsGreen,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Reconocimiento de voz",
                                    color = SettingsText,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Reconocimiento de Google · En línea",
                                    color = SettingsMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = "Listo",
                                color = SettingsGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                SectionLabel("PERMISOS")
                Spacer(modifier = Modifier.height(12.dp))

                PermissionRow(
                    title = "Accesibilidad",
                    subtitle = "Obligatorio — Toca para activar"
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionRow(
                    title = "Ventana flotante",
                    subtitle = "Obligatorio — Toca para activar"
                )

                Spacer(modifier = Modifier.height(32.dp))

                SectionLabel("ACERCA DE")
                Spacer(modifier = Modifier.height(12.dp))

                AboutSection()

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        SavedToast(
            visible = savedVisible,
            onDismiss = { savedVisible = false },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Volver",
                    tint = SettingsSecondary
                )
            }

            Text(
                text = "Ajustes",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsText
            )
        }

        Divider(color = SettingsBorder, thickness = 1.dp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 3.sp,
        color = SettingsMuted
    )
}

@Composable
private fun ProviderSelectorCard(
    selectedProvider: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String) -> Unit
) {
    val providers = listOf(
        "Groq",
        "Google Gemini",
        "OpenRouter",
        "Anthropic Claude",
        "OpenAI",
        "Ollama (local)",
        "Personalizado"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) SettingsViolet else SettingsBorder,
        animationSpec = tween(200),
        label = "provider_border"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        color = SettingsSurface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProviderDot(selectedProvider)
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = selectedProvider,
                color = SettingsText,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = SettingsMuted,
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (isExpanded) 90f else 0f
                }
            )
        }
    }

    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SettingsSurface,
            border = BorderStroke(1.dp, SettingsBorder)
        ) {
            Column {
                providers.forEach { provider ->
                    ProviderOption(
                        label = provider,
                        isSelected = provider == selectedProvider,
                        onClick = { onSelect(provider) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderDot(provider: String) {
    val color = when (provider) {
        "Groq" -> SettingsViolet
        "Google Gemini" -> SettingsCyan
        "OpenRouter" -> SettingsGreen
        "Anthropic Claude" -> SettingsAmber
        "OpenAI" -> SettingsGreen
        "Ollama (local)" -> SettingsCyan
        else -> SettingsViolet
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun ProviderOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(SettingsViolet)
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Spacer(modifier = Modifier.width(15.dp))
        }

        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isSelected) SettingsViolet else SettingsText
        )
    }
}

@Composable
fun FloatingLabelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isFocused: Boolean,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    visible: Boolean = true,
    onTogglePassword: (() -> Unit)? = null
) {
    if (!visible) return

    val hasContent = value.isNotEmpty()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) SettingsViolet else SettingsBorder,
        animationSpec = tween(200),
        label = "field_border"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .height(60.dp),
        shape = RoundedCornerShape(14.dp),
        color = SettingsSurface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = label,
                fontSize = if (hasContent) 10.sp else 14.sp,
                color = if (hasContent) SettingsViolet else SettingsSecondary,
                modifier = Modifier
                    .align(
                        if (hasContent) Alignment.TopStart
                        else Alignment.CenterStart
                    )
                    .padding(top = if (hasContent) 6.dp else 0.dp)
            )

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(
                        bottom = 8.dp,
                        end = if (isPassword) 36.dp else 0.dp
                    ),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = SettingsText
                ),
                visualTransformation = if (
                    isPassword && !showPassword
                ) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                }
            )

            if (isPassword && onTogglePassword != null) {
                IconButton(
                    onClick = onTogglePassword,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (showPassword) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = "Toggle password visibility",
                        tint = SettingsMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveProviderButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SettingsViolet),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SettingsViolet
        )
    ) {
        Text(
            text = "Save Provider",
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        color = SettingsSurface,
        border = BorderStroke(1.dp, SettingsBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = SettingsSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = SettingsText
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SettingsMuted
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SettingsText,
                    checkedTrackColor = SettingsViolet,
                    uncheckedThumbColor = SettingsMuted,
                    uncheckedTrackColor = SettingsSurfaceLight
                )
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SettingsSurface,
        border = BorderStroke(1.dp, SettingsBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SettingsRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SettingsRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = SettingsText
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SettingsRed
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = SettingsRed
            )
        }
    }
}

@Composable
private fun AboutSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SettingsSurface,
        border = BorderStroke(1.dp, SettingsBorder)
    ) {
        Column {
            AboutRow("Version", "M1.0 (build 1)")
            Divider(color = SettingsBorder)
            AboutRow("License", "MIT Open Source")
            Divider(color = SettingsBorder)
            AboutRow("GitHub", "tokenarc/open-jarvis", true)
            Divider(color = SettingsBorder)

            Text(
                text = "Built on Android · From Termux with love",
                fontSize = 12.sp,
                color = SettingsMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun AboutRow(
    title: String,
    value: String,
    isLink: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = SettingsSecondary
        )

        Text(
            text = value,
            fontSize = 14.sp,
            color = if (isLink) SettingsViolet else SettingsText
        )
    }
}

@Composable
private fun SavedToast(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(1500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { 40 } + fadeIn(),
        exit = slideOutVertically { 40 } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = SettingsSurface,
            border = BorderStroke(
                1.dp,
                SettingsGreen.copy(alpha = 0.25f)
            )
        ) {
            Text(
                text = "✓ saved",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsGreen,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            )
        }
    }
}
