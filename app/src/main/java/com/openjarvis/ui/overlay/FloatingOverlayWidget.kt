
package com.openjarvis.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openjarvis.agent.AgentState
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.voice.VoiceManager
import com.openjarvis.voice.VoiceManager.VoiceState
import kotlinx.coroutines.delay

private val OverlayViolet = Color(0xFF9B7BFF)
private val OverlayCyan = Color(0xFF42D9FF)
private val OverlayGreen = Color(0xFF4ADE80)
private val OverlayRed = Color(0xFFFF5C70)
private val OverlayAmber = Color(0xFFFFC15A)
private val OverlayBackground = Color(0xFF101018)
private val OverlaySurface = Color(0xFF20202B)
private val OverlaySurfaceDark = Color(0xFF181820)
private val OverlayText = Color(0xFFF2F0FA)
private val OverlayMuted = Color(0xFF92909F)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FloatingOverlayWidget(
    agentState: AgentState,
    voiceManager: VoiceManager?,
    recentTasks: List<TaskNode>,
    suggestions: List<String> = emptyList(),
    onCommand: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var commandText by remember { mutableStateOf("") }
    var hasAnimatedOnce by remember { mutableStateOf(false) }
    var placeholderText by remember { mutableStateOf("") }

    val voiceState by (
        voiceManager?.state?.collectAsState()
            ?: remember { mutableStateOf(VoiceState.Idle) }
        )

    LaunchedEffect(isExpanded) {
        if (isExpanded && !hasAnimatedOnce) {
            hasAnimatedOnce = true
            val full = "command..."
            for (char in full) {
                placeholderText += char
                delay(30)
            }
        }
    }

    LaunchedEffect(voiceState) {
        if (voiceState is VoiceState.Result) {
            commandText = (voiceState as VoiceState.Result).text
        }
    }

    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            if (targetState) {
                fadeIn(
                    animationSpec = tween(250)
                ) with fadeOut(
                    animationSpec = tween(100)
                )
            } else {
                fadeIn(
                    animationSpec = tween(100)
                ) with fadeOut(
                    animationSpec = tween(150)
                )
            }
        },
        label = "overlay_expand"
    ) { expanded ->
        if (expanded) {
            ExpandedOverlay(
                agentState = agentState,
                voiceState = voiceState,
                voiceManager = voiceManager,
                commandText = commandText,
                onCommandTextChange = { commandText = it },
                placeholderText = placeholderText,
                recentTasks = recentTasks,
                suggestions = suggestions,
                onSend = {
                    if (commandText.isNotBlank()) {
                        onCommand(commandText.trim())
                        commandText = ""
                    }
                },
                onSuggestionClick = { commandText = it },
                onVoicePressStart = {
                    voiceManager?.startListening()
                },
                onVoicePressEnd = {
                    val text = voiceManager?.stopListening()
                    if (!text.isNullOrBlank()) {
                        commandText = text
                    }
                },
                onCollapse = {
                    voiceManager?.stopSpeaking()
                    isExpanded = false
                    onCollapse()
                },
                modifier = modifier
            )
        } else {
            CollapsedPill(
                agentState = agentState,
                onClick = { isExpanded = true },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CollapsedPill(
    agentState: AgentState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val statusScale by rememberInfiniteTransition(label = "status")
        .animateFloat(
            initialValue = 0.75f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "status_scale"
        )

    val statusColor = when (agentState) {
        is AgentState.Idle -> OverlayViolet
        is AgentState.Running -> OverlayCyan
        is AgentState.Done -> OverlayGreen
        is AgentState.Error -> OverlayRed
    }

    Surface(
        modifier = modifier
            .width(140.dp)
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(999.dp),
        color = OverlayBackground.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        Box {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = OverlayViolet.copy(alpha = glowAlpha),
                    style = Stroke(width = 1.dp.toPx()),
                    cornerRadius = CornerRadius(999.dp.toPx())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .scale(statusScale),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = statusColor)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "JARVIS",
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 4.sp,
                        color = OverlayMuted
                    )
                )
            }
        }
    }
}

@Composable
private fun ExpandedOverlay(
    agentState: AgentState,
    voiceState: VoiceState,
    voiceManager: VoiceManager?,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    placeholderText: String,
    recentTasks: List<TaskNode>,
    suggestions: List<String> = emptyList(),
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onVoicePressStart: () -> Unit,
    onVoicePressEnd: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stepText = when (agentState) {
        is AgentState.Running -> agentState.step
        is AgentState.Done -> agentState.result
        is AgentState.Error -> agentState.message
        else -> ""
    }

    LaunchedEffect(agentState) {
        if (agentState is AgentState.Done) {
            delay(2000)
            onCollapse()
        } else if (agentState is AgentState.Error) {
            delay(4000)
            onCollapse()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = OverlayBackground,
        tonalElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(OverlayViolet)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(OverlayViolet.copy(alpha = 0.2f))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusOrb(state = agentState)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JARVIS",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = OverlayText
                            )
                        )
                    }

                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = OverlayMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                StatusLine(state = agentState, stepText = stepText)

                Spacer(modifier = Modifier.height(12.dp))

                DividerLine()

                Spacer(modifier = Modifier.height(12.dp))

                if (suggestions.isNotEmpty() && agentState is AgentState.Idle) {
                    SuggestionChips(
                        suggestions = suggestions,
                        onSuggestionClick = onSuggestionClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                InputRowWithVoice(
                    commandText = commandText,
                    onCommandTextChange = onCommandTextChange,
                    placeholderText = if (commandText.isEmpty()) placeholderText else "",
                    voiceState = voiceState,
                    onVoicePressStart = onVoicePressStart,
                    onVoicePressEnd = onVoicePressEnd,
                    onSend = onSend
                )

                Spacer(modifier = Modifier.height(12.dp))

                DividerLine()

                Spacer(modifier = Modifier.height(12.dp))

                TaskLogWidget(tasks = recentTasks.take(3))
            }
        }
    }
}

@Composable
private fun StatusOrb(state: AgentState) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    val color = when (state) {
        is AgentState.Idle -> OverlayViolet
        is AgentState.Running -> OverlayCyan
        is AgentState.Done -> OverlayGreen
        is AgentState.Error -> OverlayRed
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = color)
        }
    }
}

@Composable
private fun StatusLine(state: AgentState, stepText: String) {
    val cursorAlpha by rememberInfiniteTransition(label = "cursor")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cursor_alpha"
        )

    val textColor = when (state) {
        is AgentState.Idle -> OverlayMuted
        is AgentState.Running -> OverlayCyan
        is AgentState.Done -> OverlayGreen
        is AgentState.Error -> OverlayRed
    }

    Row(
        modifier = Modifier.height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (state) {
                is AgentState.Idle -> "_ ready for command"
                is AgentState.Running -> "◉ $stepText"
                is AgentState.Done -> "✓ done in $stepText"
                is AgentState.Error -> "✗ $stepText"
            },
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = textColor
            ),
            maxLines = 1
        )

        if (state is AgentState.Idle) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(OverlayMuted.copy(alpha = cursorAlpha))
            )
        }
    }
}

@Composable
private fun InputRowWithVoice(
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    placeholderText: String,
    voiceState: VoiceState,
    onVoicePressStart: () -> Unit,
    onVoicePressEnd: () -> Unit,
    onSend: () -> Unit
) {
    val isRecording = voiceState is VoiceState.Recording
    val isTranscribing = voiceState is VoiceState.Transcribing

    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.5f),
        label = "voice_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isRecording) 0.6f else 0f,
        animationSpec = tween(200),
        label = "voice_glow"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isRecording -> OverlayRed
            isTranscribing -> OverlayAmber
            else -> OverlaySurface
        },
        animationSpec = tween(200),
        label = "voice_background"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(scale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onVoicePressStart()
                            tryAwaitRelease()
                            onVoicePressEnd()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (glowAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = OverlayRed.copy(alpha = glowAlpha * 0.3f),
                            shape = CircleShape
                        )
                )
            }

            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "›",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                color = OverlayViolet
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        BasicTextField(
            value = commandText,
            onValueChange = onCommandTextChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = OverlayText
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { onSend() }
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (commandText.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = OverlayMuted.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (commandText.isEmpty()) OverlaySurface else OverlayViolet,
            onClick = onSend
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DividerLine() {
    Divider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = OverlaySurface
    )
}

@Composable
private fun TaskLogWidget(tasks: List<TaskNode>) {
    Column(modifier = Modifier.heightIn(max = 80.dp)) {
        tasks.forEachIndexed { index, task ->
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(index) {
                delay(index * 80L)
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically { -24 } + fadeIn()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (
                                    task.result.contains("Error", ignoreCase = true) ||
                                    task.result.contains("Failed", ignoreCase = true)
                                ) OverlayRed else OverlayGreen,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = task.command,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = OverlayMuted
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = formatRelativeTime(task.timestamp),
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 10.sp,
                            color = OverlayMuted
                        )
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val minutes = diff / 60000

    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h"
    }
}

@Composable
private fun SuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEachIndexed { index, suggestion ->
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(index) {
                delay(index * 60L)
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(200)) +
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(200)
                    )
            ) {
                Surface(
                    modifier = Modifier.clickable {
                        onSuggestionClick(suggestion)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = OverlaySurface
                ) {
                    Text(
                        text = suggestion,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = OverlayMuted
                        ),
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }
        }
    }
}
