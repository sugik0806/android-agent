package com.example.legalremoteagent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AgentService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(100, notification())
        scheduleWorker()
        startHeartbeatLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleWorker()
        startHeartbeatLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        heartbeatJob?.cancel()
        super.onDestroy()
    }

    private fun scheduleWorker() {
        val request = PeriodicWorkRequestBuilder<AgentWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "legal-agent-heartbeat",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun startHeartbeatLoop() {
        if (heartbeatJob?.isActive == true) {
            return
        }

        heartbeatJob = scope.launch {
            while (true) {
                syncOnce()
                delay(10_000)
            }
        }
    }

    private suspend fun syncOnce() {
        try {
            val prefs = AgentPrefs(this)
            val api = ApiClient.create(prefs.serverUrl)

            if (prefs.token == null) {
                val registered = api.register(DeviceInfo.registerRequest(this, prefs.deviceId))
                prefs.token = registered.agent_token
            }

            val token = prefs.token ?: return
            api.heartbeat(token, DeviceInfo.heartbeatRequest(this, prefs.deviceId))

            api.commands(token, prefs.deviceId).commands.forEach { command ->
                notifyCommand(command)
                api.commandExecuted(
                    token,
                    command.id,
                    CommandStatusRequest(prefs.deviceId, "awaiting_user_consent")
                )
            }
        } catch (_: Exception) {
            // Foreground service stays alive; WorkManager will also retry.
        }
    }

    private fun notifyCommand(command: RemoteCommand) {
        val channelId = "agent_commands"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Agent Commands", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("command_id", command.id)
            putExtra("command", command.command)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            command.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Remote request: ${command.command}")
            .setContentText("Tap untuk menyetujui aksi di Android.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(command.id.toInt(), notification)
    }

    private fun notification() = NotificationCompat.Builder(this, channel())
        .setSmallIcon(android.R.drawable.ic_menu_manage)
        .setContentTitle("Legal Android Agent aktif")
        .setContentText("Heartbeat dan command check berjalan dengan notifikasi terlihat.")
        .setOngoing(true)
        .build()

    private fun channel(): String {
        val id = "agent_foreground"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(id, "Legal Agent Service", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return id
    }
}
