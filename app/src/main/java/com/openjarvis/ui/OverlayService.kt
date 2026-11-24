
package com.openjarvis.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.openjarvis.R
import com.openjarvis.agent.AgentCore
import com.openjarvis.bridge.SocketServer
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.voice.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    private lateinit var agentCore: AgentCore
    private lateinit var voiceManager: VoiceManager
    private lateinit var graphifyRepo: GraphifyRepository
    private lateinit var socketServer: SocketServer

    private var stateCollectJob: Job? = null

    @Volatile
    private var recentTasks = emptyList<TaskNode>()

    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()

        agentCore = AgentCore(this)
        voiceManager = VoiceManager(this)
        graphifyRepo = GraphifyRepository(this)
        socketServer = SocketServer(this, agentCore, graphifyRepo)

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        socketServer.start()

        serviceScope.launch {
            initializeServices()
        }
    }

    private suspend fun initializeServices() {
        if (isInitialized) return
        isInitialized = true

        recentTasks = withContext(Dispatchers.IO) {
            try {
                graphifyRepo.getRecentTasks(10)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stateCollectJob?.cancel()

        try {
            socketServer.stop()
        } catch (e: Exception) {
            // Server may already be stopped.
        }

        try {
            voiceManager.release()
        } catch (e: Exception) {
            // Voice resources may already be released.
        }

        serviceScope.cancel()

        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(
                R.string.notification_channel_description
            )
        }

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun getAgentCore(): AgentCore = agentCore

    fun getVoiceManager(): VoiceManager = voiceManager

    fun getRecentTasks(): List<TaskNode> = recentTasks

    fun executeCommand(command: String) {
        if (command.isBlank()) return

        serviceScope.launch {
            try {
                agentCore.executeTask(command)

                delay(1000)

                recentTasks = withContext(Dispatchers.IO) {
                    graphifyRepo.getRecentTasks(10)
                }
            } catch (e: Exception) {
                // Keep the service alive if a command fails.
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "jarvis_overlay_channel"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(
                context,
                OverlayService::class.java
            )

            context.startForegroundService(intent)
        }
    }
}
