package com.example.legalremoteagent

import android.content.Context
import java.util.UUID

class AgentPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("agent", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("server_url", "http://192.168.1.19:8000/")!!
        set(value) = prefs.edit().putString("server_url", value).apply()

    val deviceId: String
        get() {
            val existing = prefs.getString("device_id", null)
            if (existing != null) return existing

            val generated = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", generated).apply()
            return generated
        }

    var token: String?
        get() = prefs.getString("agent_token", null)
        set(value) = prefs.edit().putString("agent_token", value).apply()
}
