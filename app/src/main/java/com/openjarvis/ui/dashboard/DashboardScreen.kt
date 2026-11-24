
package com.openjarvis.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.graphify.nodes.AppNode
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.voice.VoiceManager
import kotlinx.coroutines.delay

private val JarvisBackground = Color(0xFF080812)
private val JarvisSurface = Color(0xFF151525)
private val JarvisSurfaceDark = Color(0xFF10101D)
private val JarvisViolet = Color(0xFF9B6DFF)
private val JarvisCyan = Color(0xFF42D9F5)
private val JarvisGreen = Color(0xFF42D392)
private val JarvisRed = Color(0xFFFF647C)
private val JarvisAmber = Color(0xFFFFC66D)
private val JarvisText = Color(0xFFF2F0FF)
private val JarvisSecondary = Color(0xFFAAA8C0)
private val JarvisDisabled = Color(0xFF77758D)
private val JarvisBorder = Color(0xFF303047)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartVoice: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSettings: () -> Unit,
    graphifyRepo: GraphifyRepository,
    voiceManager: VoiceManager,
    modifier: Modifier = Modifier
) {
    var recentTasks by remember {
        mutableStateOf<List<TaskNode>>(emptyList())
    }

    var mostUsedApps by remember {
        mutableStateOf<List<AppNode>>(emptyList())
    }

    val isSystemActive = true

    var statsAlpha by remember { mutableFloatStateOf(0f) }
    var tasksAlpha by remember { mutableFloatStateOf(0f) }
    var appsAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(graphifyRepo) {
        try {
            recentTasks = graphifyRepo.getRecentTasks(10)
            mostUsedApps = graphifyRepo.getMostUsedApps(4)
        } catch (e: Exception) {
            recentTasks = emptyList()
            mostUsedApps = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        statsAlpha = 1f
        delay(150)
        tasksAlpha = 1f
        delay(200)
        appsAlpha = 1f
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        OrbBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            HeroSection(isSystemActive = isSystemActive)

            Spacer(modifier = Modifier.height(22.dp))

            VoiceControlCard(voiceManager = voiceManager, onStartVoice = onStartVoice)

            Spacer(modifier = Modifier.height(24.dp))

            StatsRow(
                tasks = recentTasks,
                apps = mostUsedApps,
                alpha = statsAlpha
            )

            Spacer(modifier = Modifier.height(24.dp))

            RecentTasksSection(
                tasks = recentTasks,
                alpha = tasksAlpha
            )

            Spacer(modifier = Modifier.height(24.dp))

            QuickAppsSection(
                apps = mostUsedApps,
                alpha = appsAlpha
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        BottomNavBar(
            onSettingsClick = onOpenSettings,
            onHomeClick = { },
            onTasksClick = onOpenTasks,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun VoiceControlCard(voiceManager: VoiceManager, onStartVoice: () -> Unit) {
    val voiceState by voiceManager.state.collectAsState()
    val isRecording = voiceState is VoiceManager.VoiceState.Recording ||
        voiceState is VoiceManager.VoiceState.Transcribing
    val enabled = voiceManager.isVoiceEnabled()
    val pulse = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.10f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_scale"
    )

    val statusText = when (val state = voiceState) {
        is VoiceManager.VoiceState.Recording -> "Escuchando... habla ahora"
        is VoiceManager.VoiceState.Transcribing -> "Procesando tu voz..."
        is VoiceManager.VoiceState.Result -> state.text
        is VoiceManager.VoiceState.Error -> state.message
        else -> if (enabled) "Toca el micrófono para hablar" else "Activa el asistente de voz en Ajustes"
    }

    val statusColor = when (voiceState) {
        is VoiceManager.VoiceState.Error -> JarvisRed
        is VoiceManager.VoiceState.Recording,
        is VoiceManager.VoiceState.Transcribing -> JarvisCyan
        else -> JarvisSecondary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = JarvisSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, JarvisViolet.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "COMANDO DE VOZ",
                color = JarvisDisabled,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .scale(if (isRecording) pulseScale else 1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = if (isRecording) listOf(JarvisCyan, JarvisViolet)
                            else listOf(JarvisViolet, Color(0xFF5540A8))
                        )
                    )
                    .border(
                        2.dp,
                        JarvisCyan.copy(alpha = if (isRecording) 0.9f else 0.35f),
                        RoundedCornerShape(50)
                    )
                    .clickable {
                        if (isRecording) {
                            voiceManager.cancelListening()
                        } else if (enabled) {
                            onStartVoice()
                        } else {
                            voiceManager.resetState()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop listening" else "Start voice command",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isRecording) "JARVIS TE ESCUCHA" else "HABLAR CON JARVIS",
                color = JarvisText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun OrbBackground() {
    val infiniteTransition = rememberInfiniteTransition(
        label = "orb"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                8000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(
            size.width / 2,
            200.dp.toPx() + offsetY.dp.toPx()
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    JarvisViolet.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = 200.dp.toPx()
            ),
            center = center,
            radius = 200.dp.toPx()
        )
    }
}

@Composable
private fun HeroSection(isSystemActive: Boolean) {
    Column {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = JarvisSurface,
            border = BorderStroke(1.dp, JarvisViolet.copy(alpha = 0.4f))
        ) {
            Text(
                text = "AGENTE IA v0.1",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 4.sp,
                    color = JarvisViolet
                ),
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val annotatedText = buildAnnotatedString {
            withStyle(SpanStyle(color = JarvisText)) {
                append("OPEN")
            }

            append(" ")

            withStyle(
                SpanStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(JarvisViolet, JarvisCyan)
                    )
                )
            ) {
                append("JARVIS")
            }
        }

        Text(
            text = annotatedText,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                letterSpacing = (-0.5).sp
            )
        )

        Text(
            text = "Tu dispositivo. Tus órdenes.",
            style = TextStyle(
                fontSize = 13.sp,
                color = JarvisDisabled
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusBadge(isActive = isSystemActive)
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "badge"
    )

    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    val statusColor = if (isActive) JarvisGreen else JarvisRed

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = statusColor.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            statusColor.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(if (isActive) dotScale else 1f)
                    .background(
                        statusColor,
                        RoundedCornerShape(50)
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isActive) "Sistema activo" else "Toca para activar",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            )
        }
    }
}

@Composable
private fun StatsRow(
    tasks: List<TaskNode>,
    apps: List<AppNode>,
    alpha: Float
) {
    val animatedCount by animateIntAsState(
        targetValue = tasks.size,
        animationSpec = tween(
            500,
            easing = FastOutSlowInEasing
        ),
        label = "count"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            count = animatedCount,
            label = "Tareas realizadas",
            modifier = Modifier.weight(1f)
        )

        StatCard(
            count = apps.size,
            label = "Aplicaciones usadas",
            modifier = Modifier.weight(1f)
        )

        StatCard(
            count = 0,
            label = "Patrones",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = JarvisSurface,
        border = BorderStroke(1.dp, JarvisBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = JarvisText
                )
            )

            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = JarvisDisabled
                )
            )
        }
    }
}

@Composable
private fun RecentTasksSection(
    tasks: List<TaskNode>,
    alpha: Float
) {
    Column(modifier = Modifier.alpha(alpha)) {
        Text(
            text = "TAREAS RECIENTES",
            style = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = JarvisDisabled
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Text(
                text = "Aún no hay tareas. ¡Prueba abrir una aplicación!",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = JarvisDisabled
                )
            )
        } else {
            tasks.take(5).forEach { task ->
                TaskCard(task = task)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskNode) {
    val isSuccess = task.result.contains(
        "Opened",
        ignoreCase = true
    ) || task.result.contains(
        "Success",
        ignoreCase = true
    )

    val outcomeColor = if (isSuccess) JarvisGreen else JarvisRed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = JarvisSurfaceDark,
        border = BorderStroke(1.dp, JarvisBorder)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = outcomeColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = task.command,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = JarvisText
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatRelativeTime(task.timestamp),
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = JarvisDisabled
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppInitialBadge(label = task.command)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Aplicación del sistema",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = JarvisDisabled
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = outcomeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isSuccess) "completada" else "fallida",
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = outcomeColor
                            ),
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppInitialBadge(label: String) {
    val colors = listOf(
        JarvisViolet,
        JarvisCyan,
        JarvisGreen,
        JarvisAmber
    )

    val colorIndex = (label.hashCode().toLong()
        .and(Long.MAX_VALUE) % colors.size).toInt()

    val bgColor = colors[colorIndex]
    val initial = label.firstOrNull()?.uppercaseChar() ?: 'J'

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = Color.White
            )
        )
    }
}

@Composable
private fun QuickAppsSection(
    apps: List<AppNode>,
    alpha: Float
) {
    Column(modifier = Modifier.alpha(alpha)) {
        Text(
            text = "APLICACIONES RÁPIDAS",
            style = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = JarvisDisabled
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (apps.isEmpty()) {
            Text(
                text = "Aquí aparecerán tus aplicaciones frecuentes.",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = JarvisDisabled
                )
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(220.dp)
            ) {
                items(apps) { app ->
                    AppTile(app = app)
                }
            }
        }
    }
}

@Composable
private fun AppTile(app: AppNode) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = JarvisSurfaceDark,
        border = BorderStroke(1.dp, JarvisBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppInitialBadge(label = app.label)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = app.label,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = JarvisSecondary
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    onSettingsClick: () -> Unit,
    onHomeClick: () -> Unit,
    onTasksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = JarvisSurfaceDark
    ) {
        Column {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = JarvisBorder
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavigationItem(
                    label = "Inicio",
                    selected = true,
                    onClick = onHomeClick
                )

                NavigationItem(
                    label = "Tareas",
                    selected = false,
                    onClick = onTasksClick
                )

                NavigationItem(
                    label = "Ajustes",
                    selected = false,
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun NavigationItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = when (label) {
                "Ajustes" -> Icons.Filled.Settings
                else -> Icons.Filled.Task
            },
            contentDescription = label,
            tint = if (selected) JarvisViolet else JarvisDisabled,
            modifier = Modifier.size(22.dp)
        )

        Text(
            text = label,
            fontSize = 10.sp,
            color = if (selected) JarvisViolet else JarvisDisabled
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp)
        .coerceAtLeast(0L)

    val minutes = diff / 60000

    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h"
    }
}
