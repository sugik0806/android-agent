package com.example.legalremoteagent

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AgentApi {
    @POST("api/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("api/heartbeat")
    suspend fun heartbeat(
        @Header("X-Agent-Token") token: String,
        @Body body: HeartbeatRequest
    )

    @GET("api/commands")
    suspend fun commands(
        @Header("X-Agent-Token") token: String,
        @Query("device_id") deviceId: String
    ): CommandEnvelope

    @POST("api/commands/{id}/executed")
    suspend fun commandExecuted(
        @Header("X-Agent-Token") token: String,
        @Path("id") id: Long,
        @Body body: CommandStatusRequest
    )

    @Multipart
    @POST("api/upload/photo")
    suspend fun uploadPhoto(
        @Header("X-Agent-Token") token: String,
        @Part("device_id") deviceId: RequestBody,
        @Part photo: MultipartBody.Part
    )

    @POST("api/location")
    suspend fun location(
        @Header("X-Agent-Token") token: String,
        @Body body: LocationPayload
    )
}
