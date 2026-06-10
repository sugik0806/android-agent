package com.example.legalremoteagent

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import java.net.NetworkInterface

object DeviceInfo {
    fun registerRequest(context: Context, deviceId: String): RegisterRequest {
        return RegisterRequest(
            device_id = deviceId,
            device_name = "${Build.MANUFACTURER} ${Build.MODEL}",
            brand = Build.MANUFACTURER ?: "",
            model = Build.MODEL ?: "",
            android_version = Build.VERSION.RELEASE ?: "",
            battery = battery(context),
            charging = charging(context),
            network = network(context),
            signal = 100,
            local_ip = localIp()
        )
    }

    fun heartbeatRequest(context: Context, deviceId: String): HeartbeatRequest {
        return HeartbeatRequest(
            device_id = deviceId,
            battery = battery(context),
            charging = charging(context),
            network = network(context),
            signal = 100,
            local_ip = localIp()
        )
    }

    private fun battery(context: Context): Int {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun charging(context: Context): Boolean {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return manager.isCharging
    }

    private fun network(context: Context): String {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return "offline"
        val caps = manager.getNetworkCapabilities(network) ?: return "unknown"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            else -> "other"
        }
    }

    private fun localIp(): String {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
            ?.hostAddress ?: ""
    }
}
