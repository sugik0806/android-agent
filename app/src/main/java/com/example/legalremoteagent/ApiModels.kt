package com.example.legalremoteagent

data class RegisterRequest(
    val device_id: String,
    val device_name: String,
    val brand: String,
    val model: String,
    val android_version: String,
    val battery: Int,
    val charging: Boolean,
    val network: String,
    val signal: Int,
    val local_ip: String
)

data class RegisterResponse(
    val success: Boolean,
    val device_id: String,
    val agent_token: String
)

data class HeartbeatRequest(
    val device_id: String,
    val battery: Int,
    val charging: Boolean,
    val network: String,
    val signal: Int,
    val local_ip: String
)

data class CommandEnvelope(val commands: List<RemoteCommand>)

data class RemoteCommand(
    val id: Long,
    val command: String,
    val payload: Map<String, Any>?,
    val status: String
)

data class CommandStatusRequest(
    val device_id: String,
    val status: String
)

data class LocationPayload(
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val recorded_at: String
)
