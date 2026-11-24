package com.openjarvis.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.graphify.nodes.TaskNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF080812)
private val SurfaceColor = Color(0xFF151525)
private val MainText = Color(0xFFF2F0FF)
private val Muted = Color(0xFFAAA8C0)
private val Accent = Color(0xFF9B6DFF)
private val Green = Color(0xFF42D392)
private val Red = Color(0xFFFF647C)

@Composable
fun TasksScreen(
    graphifyRepo: GraphifyRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tasks by remember { mutableStateOf<List<TaskNode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(graphifyRepo) {
        loading = true
        loadError = false
        try {
            tasks = withContext(Dispatchers.IO) { graphifyRepo.getRecentTasks(50) }
        } catch (_: Exception) {
            tasks = emptyList()
            loadError = true
        } finally {
            loading = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(Bg).statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MainText)
            }
            Text("Historial de tareas", color = MainText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = Color(0xFF303047))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Text("ACTIVIDAD DE JARVIS", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            when {
                loading -> Text("Cargando tareas...", color = Muted)
                loadError -> Text("No se pudo cargar el historial. Inténtalo de nuevo más tarde.", color = Red)
                tasks.isEmpty() -> Text("Todavía no hay tareas registradas.", color = Muted)
                else -> tasks.forEach { task ->
                    val success = task.result.contains("Opened", true) || task.result.contains("Success", true) || task.result.contains("abierto", true) || task.result.contains("éxito", true)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceColor,
                        border = BorderStroke(1.dp, Color(0xFF303047))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(task.command, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text(task.result.ifBlank { "Sin detalles" }, color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(if (success) "Completada" else "Revisar resultado", color = if (success) Green else Red, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
