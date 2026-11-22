package com.example.assistant

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceTelemetry(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val volumePercent: Int = 0,
    val isTorchOn: Boolean = false,
    val hasFlash: Boolean = false,
    val networkLabelBn: String = "অজানা",
    val isIgnoringBatteryOptimization: Boolean = false,
)

class HardwareController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val torchCameraId: String? by lazy { findFlashCameraId() }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                _isTorchOn.value = enabled
            }
        }
    }

    init {
        try {
            cameraManager?.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register TorchCallback", e)
        }
    }

    fun release() {
        try {
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (_: Exception) {
        }
    }

    private fun findFlashCameraId(): String? {
        val mgr = cameraManager ?: return null
        return try {
            mgr.cameraIdList.firstOrNull { id ->
                val chars = mgr.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val isBack =
                    chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                hasFlash && isBack
            } ?: mgr.cameraIdList.firstOrNull { id ->
                mgr.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect camera flash", e)
            null
        }
    }

    fun setFlashlight(turnOn: Boolean): String {
        val camId = torchCameraId
        if (camId == null || cameraManager == null) {
            return "Boss, দুঃখিত, এই ডিভাইসে ফ্ল্যাশলাইট হার্ডওয়্যার পাওয়া যায়নি।"
        }
        return try {
            cameraManager.setTorchMode(camId, turnOn)
            _isTorchOn.value = turnOn
            if (turnOn) {
                "জি Boss, ফ্ল্যাশলাইট চালু করেছি।"
            } else {
                "জি Boss, ফ্ল্যাশলাইট বন্ধ করেছি।"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            "Boss, ফ্ল্যাশলাইট নিয়ন্ত্রণ করতে সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}।"
        }
    }

    fun adjustVolume(action: VolumeAction): String {
        val audio = audioManager
            ?: return "Boss, অডিও কন্ট্রোলার সিস্টেমে পাওয়া যায়নি।"
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        when (action) {
            VolumeAction.UP -> audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI,
            )
            VolumeAction.DOWN -> audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI,
            )
            VolumeAction.MUTE -> audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI,
            )
            VolumeAction.MAX -> audio.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                max,
                AudioManager.FLAG_SHOW_UI,
            )
        }
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val pct = ((current.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
        val pctBn = BengaliFormatter.toBengaliDigits(pct)
        return when (action) {
            VolumeAction.UP -> "জি Boss, ভলিউম বাড়ানো হয়েছে। বর্তমান মিডিয়া ভলিউম $pctBn শতাংশ।"
            VolumeAction.DOWN -> "জি Boss, ভলিউম কমানো হয়েছে। বর্তমান মিডিয়া ভলিউম $pctBn শতাংশ।"
            VolumeAction.MUTE -> "জি Boss, মিডিয়া সাউন্ড মিউট করা হয়েছে।"
            VolumeAction.MAX -> "জি Boss, মিডিয়া ভলিউম সর্বোচ্চ ($pctBn শতাংশ) করা হয়েছে।"
        }
    }

    fun getVolumePercent(): Int {
        val audio = audioManager ?: return 0
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun getBatteryInfo(): Pair<Int, Boolean> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 0
        }
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return pct to charging
    }

    fun getBatteryResponseBn(): String {
        val (pct, charging) = getBatteryInfo()
        val pctBn = BengaliFormatter.toBengaliDigits(pct)
        val chargeText = if (charging) "এবং ফোনটি বর্তমানে চার্জে আছে" else ""
        return "Boss, আপনার ফোনের ব্যাটারি বর্তমানে $pctBn শতাংশ $chargeText।".replace(" ।", "।")
    }

    fun getNetworkStatusBn(): String {
        val cm = connectivityManager ?: return "অজানা"
        val network = cm.activeNetwork ?: return "অফলাইন"
        val caps = cm.getNetworkCapabilities(network) ?: return "অফলাইন"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "ওয়াই-ফাই সংযুক্ত"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "মোবাইল ডেটা সংযুক্ত"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ইথারনেট সংযুক্ত"
            else -> "সংযুক্ত"
        }
    }

    fun getPhoneStatusResponseBn(): String {
        val telemetry = readTelemetry()
        val batBn = BengaliFormatter.toBengaliDigits(telemetry.batteryPercent)
        val volBn = BengaliFormatter.toBengaliDigits(telemetry.volumePercent)
        val chargeLabel = if (telemetry.isCharging) "চার্জিং" else "ব্যাটারি মোড"
        val torchLabel = if (telemetry.isTorchOn) "চালু" else "বন্ধ"
        return "Boss, আপনার ফোনের বর্তমান অবস্থা: ব্যাটারি $batBn শতাংশ ($chargeLabel), নেটওয়ার্ক ${telemetry.networkLabelBn}, মিডিয়া ভলিউম $volBn শতাংশ এবং ফ্ল্যাশলাইট $torchLabel।"
    }

    fun isIgnoringBatteryOptimization(): Boolean {
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun readTelemetry(): DeviceTelemetry {
        val (pct, charging) = getBatteryInfo()
        return DeviceTelemetry(
            batteryPercent = pct,
            isCharging = charging,
            volumePercent = getVolumePercent(),
            isTorchOn = _isTorchOn.value,
            hasFlash = torchCameraId != null,
            networkLabelBn = getNetworkStatusBn(),
            isIgnoringBatteryOptimization = isIgnoringBatteryOptimization(),
        )
    }

    fun openDisplayBrightnessSettings(): String =
        launchSettingsIntent(
            Intent(Settings.ACTION_DISPLAY_SETTINGS),
            "Boss, ডিসপ্লে ও ব্রাইটনেস সেটিংস খুলে দিচ্ছি।",
        )

    fun openWifiSettings(): String {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        return launchSettingsIntent(
            intent,
            "জি Boss, ওয়াই-ফাই সেটিংস খুলে দিচ্ছি।",
            fallback = Intent(Settings.ACTION_WIFI_SETTINGS),
        )
    }

    fun openBluetoothSettings(): String =
        launchSettingsIntent(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            "জি Boss, ব্লুটুথ সেটিংস খুলে দিচ্ছি।",
        )

    fun openMobileNetworkSettings(): String =
        launchSettingsIntent(
            Intent(Settings.ACTION_DATA_ROAMING_SETTINGS),
            "জি Boss, মোবাইল নেটওয়ার্ক সেটিংস খুলে দিচ্ছি।",
            fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS),
        )

    fun openBatteryOptimizationSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open battery optimization settings", e)
            false
        }
    }

    fun openAppDetailsSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open app settings", e)
            false
        }
    }

    private fun launchSettingsIntent(
        primary: Intent,
        successMessage: String,
        fallback: Intent? = null,
    ): String {
        return try {
            primary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(primary)
            successMessage
        } catch (e: Exception) {
            if (fallback != null) {
                try {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                    return successMessage
                } catch (_: Exception) {
                }
            }
            Log.e(TAG, "Failed to launch settings screen", e)
            "Boss, এই সেটিংস পৃষ্ঠাটি খোলা যায়নি।"
        }
    }

    companion object {
        private const val TAG = "HardwareController"
    }
}
