package com.example.legalremoteagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var prefs: AgentPrefs
    private lateinit var status: TextView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { registerAndStart() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = AgentPrefs(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        val serverInput = EditText(this).apply {
            hint = "Laravel server URL"
            setText(prefs.serverUrl)
        }

        status = TextView(this).apply {
            text = "Device ID: ${prefs.deviceId}"
        }

        val saveButton = Button(this).apply {
            text = "Simpan Server + Start Agent"
            setOnClickListener {
                prefs.serverUrl = serverInput.text.toString()
                requestPermissions()
            }
        }

        val approveButton = Button(this).apply {
            text = "Setujui Command Masuk"
            setOnClickListener { approveIncomingCommand() }
        }

        val snapshotButton = Button(this).apply {
            text = "Ambil Foto Manual"
            setOnClickListener { captureAndUploadSnapshot(null) }
        }

        layout.addView(serverInput)
        layout.addView(saveButton)
        layout.addView(approveButton)
        layout.addView(snapshotButton)
        layout.addView(status)
        setContentView(layout)

        requestPermissions()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val command = intent.getStringExtra("command") ?: return
        status.text = "Command meminta izin: $command. Tekan Setujui Command Masuk."
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missing.isEmpty()) {
            registerAndStart()
        } else {
            permissionLauncher.launch(missing)
        }
    }

    private fun registerAndStart() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.create(prefs.serverUrl)
                val response = api.register(DeviceInfo.registerRequest(this@MainActivity, prefs.deviceId))
                prefs.token = response.agent_token

                ContextCompat.startForegroundService(
                    this@MainActivity,
                    Intent(this@MainActivity, AgentService::class.java)
                )

                status.text = "Agent aktif. Token tersimpan. Device ID: ${prefs.deviceId}"
            } catch (error: Exception) {
                status.text = "Gagal register: ${error.message}"
            }
        }
    }

    private fun approveIncomingCommand() {
        val commandId = intent.getLongExtra("command_id", -1)
        val command = intent.getStringExtra("command") ?: return

        when (command) {
            "camera_snapshot" -> captureAndUploadSnapshot(commandId)
            "get_location" -> status.text = "GPS consent diterima. Tambahkan LocationProvider sesuai kebutuhan."
            "start_stream" -> status.text = "Live stream consent diterima. Implementasi frame loop memakai CameraX analyzer."
            "stop_stream" -> status.text = "Stream dihentikan."
            "record_video" -> status.text = "Video consent diterima. Implementasi Recorder CameraX bisa ditambahkan di CameraRepository."
            "record_audio" -> status.text = "Audio consent diterima. Implementasi MediaRecorder bisa ditambahkan."
            "take_screenshot" -> status.text = "Screenshot membutuhkan prompt MediaProjection Android."
        }
    }

    private fun captureAndUploadSnapshot(commandId: Long?) {
        lifecycleScope.launch {
            try {
                CameraRepository(this@MainActivity).captureJpegAndUpload(commandId)
                status.text = "Foto berhasil diupload."
            } catch (error: Exception) {
                status.text = "Gagal foto: ${error.message}"
            }
        }
    }
}
