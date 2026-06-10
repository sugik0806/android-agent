package com.example.legalremoteagent

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class CameraRepository(private val context: Context) {
    suspend fun captureJpegAndUpload(commandId: Long?) {
        val owner = context as? LifecycleOwner
            ?: error("Camera capture must run from a visible consent Activity")

        val imageCapture = ImageCapture.Builder()
            .setJpegQuality(82)
            .build()

        val provider = cameraProvider()
        provider.unbindAll()
        provider.bindToLifecycle(
            owner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            imageCapture
        )

        val file = File(context.cacheDir, "snapshot-${System.currentTimeMillis()}.jpg")
        takePicture(imageCapture, file)

        val prefs = AgentPrefs(context)
        val token = prefs.token ?: error("Agent token kosong")
        val api = ApiClient.create(prefs.serverUrl)

        val deviceBody = prefs.deviceId.toRequestBody("text/plain".toMediaType())
        val photoBody = file.asRequestBody("image/jpeg".toMediaType())
        val photoPart = MultipartBody.Part.createFormData("photo", file.name, photoBody)

        api.uploadPhoto(token, deviceBody, photoPart)

        if (commandId != null && commandId > 0) {
            api.commandExecuted(
                token,
                commandId,
                CommandStatusRequest(prefs.deviceId, "executed")
            )
        }

        provider.unbindAll()
    }

    private suspend fun cameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cont.resume(future.get())
            } catch (error: Exception) {
                cont.resumeWithException(error)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun takePicture(imageCapture: ImageCapture, file: File) = suspendCoroutine<Unit> { cont ->
        val output = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cont.resume(Unit)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
        )
    }
}
