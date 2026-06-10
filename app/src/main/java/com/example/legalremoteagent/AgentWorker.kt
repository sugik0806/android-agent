package com.example.legalremoteagent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AgentWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AgentPrefs(context)
        val api = ApiClient.create(prefs.serverUrl)

        return try {
            if (prefs.token == null) {
                val registered = api.register(DeviceInfo.registerRequest(context, prefs.deviceId))
                prefs.token = registered.agent_token
            }

            val token = prefs.token ?: return Result.retry()

            api.heartbeat(token, DeviceInfo.heartbeatRequest(context, prefs.deviceId))

            val commands = api.commands(token, prefs.deviceId).commands
            commands.forEach { command ->
                notifyCommand(command)
                api.commandExecuted(
                    token,
                    command.id,
                    CommandStatusRequest(prefs.deviceId, "awaiting_user_consent")
                )
            }

            Result.success()
        } catch (error: Exception) {
            Result.retry()
        }
    }

    private fun notifyCommand(command: RemoteCommand) {
        val channelId = "agent_commands"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Agent Commands", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("command_id", command.id)
            putExtra("command", command.command)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            command.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Remote request: ${command.command}")
            .setContentText("Tap untuk melihat dan menyetujui aksi.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(command.id.toInt(), notification)
    }
}
