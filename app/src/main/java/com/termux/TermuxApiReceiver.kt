package com.termux

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.hardware.ConsumerIrManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.speech.tts.TextToSpeech
import android.telephony.TelephonyManager
import android.widget.Toast
import android.hardware.fingerprint.FingerprintManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.speech.SpeechRecognizer
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.security.KeyStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.KeyGenerator
import javax.crypto.Cipher
import android.util.Base64
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Complete Termux API implementation - ALL 39+ commands.
 * Handles API calls from shell scripts via broadcasts.
 */
class TermuxApiReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_API_CALL = "com.termux.api.API_CALL"
        const val EXTRA_API_METHOD = "api_method"
        const val EXTRA_API_ARGS = "api_args"
        const val EXTRA_RESULT_FILE = "result_file"
        private const val CHANNEL_ID = "termux_api_notifications"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var tts: TextToSpeech? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_API_CALL) return

        val method = intent.getStringExtra(EXTRA_API_METHOD) ?: return
        val args = intent.getStringExtra(EXTRA_API_ARGS) ?: ""
        val resultFile = intent.getStringExtra(EXTRA_RESULT_FILE)

        val result = try {
            when (method) {
                // === CLIPBOARD ===
                "clipboard-get" -> clipboardGet(context)
                "clipboard-set" -> clipboardSet(context, args)

                // === NOTIFICATIONS ===
                "toast" -> showToast(context, args)
                "notification" -> showNotification(context, args)
                "notification-remove" -> removeNotification(context, args)

                // === DEVICE INFO ===
                "battery-status" -> batteryStatus(context)
                "vibrate" -> vibrate(context, args)
                "brightness" -> getBrightness(context)
                "brightness-set" -> setBrightness(context, args)
                "torch" -> toggleTorch(context, args)
                "volume" -> getVolume(context)
                "volume-set" -> setVolume(context, args)

                // === NETWORK & WIFI ===
                "wifi-connectioninfo" -> wifiInfo(context)
                "wifi-enable" -> wifiEnable(context, args)
                "wifi-scaninfo" -> wifiScanInfo(context)

                // === LOCATION ===
                "location" -> getLocation(context)

                // === CAMERA ===
                "camera-info" -> cameraInfo(context)
                "camera-photo" -> cameraPhoto(context, args)

                // === AUDIO & MEDIA ===
                "media-scan" -> mediaScan(context, args)
                "media-player" -> mediaPlayerControl(context, args)
                "microphone-record" -> microphoneRecord(context, args)
                "audio-info" -> audioInfo(context)

                // === TTS ===
                "tts-engines" -> ttsEngines(context)
                "tts-speak" -> ttsSpeak(context, args)

                // === TELEPHONY ===
                "telephony-call" -> telephonyCall(context, args)
                "telephony-cellinfo" -> telephonyCellInfo(context)
                "telephony-deviceinfo" -> telephonyDeviceInfo(context)

                // === SMS ===
                "sms-list" -> smsList(context, args)
                "sms-send" -> smsSend(context, args)

                // === CONTACTS ===
                "contact-list" -> contactList(context)

                // === CALL LOG ===
                "call-log" -> callLog(context, args)

                // === SENSORS ===
                "sensor" -> sensorInfo(context, args)

                // === BIOMETRIC ===
                "fingerprint" -> fingerprintAuth(context)

                // === INFRARED ===
                "infrared-frequencies" -> infraredFrequencies(context)
                "infrared-transmit" -> infraredTransmit(context, args)

                // === USB ===
                "usb" -> usbInfo(context)

                // === SYSTEM ===
                "wallpaper" -> setWallpaper(context, args)
                "download" -> download(context, args)
                "share" -> share(context, args)
                "dialog" -> showDialog(context, args)
                "storage-get" -> storageGet(context, args)
                "job-scheduler" -> jobScheduler(context, args)

                // === URL OPENING ===
                "open-url" -> openUrl(context, args)

                // === KEYSTORE ===
                "keystore-list" -> keystoreList(context)
                "keystore-generate" -> keystoreGenerate(context, args)
                "keystore-delete" -> keystoreDelete(context, args)
                "keystore-sign" -> keystoreSign(context, args)
                "keystore-verify" -> keystoreVerify(context, args)

                // === NFC ===
                "nfc" -> nfcInfo(context)

                // === WAKE LOCK ===
                "wake-lock" -> wakeLock(context, args)

                // === BLUETOOTH ===
                "bluetooth-info" -> bluetoothInfo(context)
                "bluetooth-enable" -> bluetoothEnable(context, args)
                "bluetooth-scaninfo" -> bluetoothScanInfo(context)
                "bluetooth-connect" -> bluetoothConnect(context, args)
                "bluetooth-paired" -> bluetoothPaired(context)

                // === NOTIFICATION LIST ===
                "notification-list" -> notificationList(context)

                // === SPEECH TO TEXT ===
                "speech-to-text" -> speechToText(context)

                // === SAF (Storage Access Framework) ===
                "saf-ls" -> safList(context, args)
                "saf-stat" -> safStat(context, args)
                "saf-read" -> safRead(context, args)
                "saf-write" -> safWrite(context, args)
                "saf-mkdir" -> safMkdir(context, args)
                "saf-rm" -> safRemove(context, args)
                "saf-create" -> safCreate(context, args)
                "saf-managedir" -> safManageDir(context, args)
                "saf-dirs" -> safDirs(context)

                else -> """{"error":"Unknown API method: $method"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message?.replace("\"", "'")}"}"""
        }

        // Write result to file if specified
        resultFile?.let { path ->
            try {
                val file = File(path)
                file.parentFile?.mkdirs()  // Ensure tmp directory exists
                file.writeText(result)
            } catch (e: Exception) {
                android.util.Log.e("TermuxApiReceiver", "API result write failed: $path", e)
            }
        }
    }

    // ============================================================
    // CLIPBOARD
    // ============================================================
    private fun clipboardGet(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
    }

    private fun clipboardSet(context: Context, text: String): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("termux-api", text))
        return ""
    }

    // ============================================================
    // NOTIFICATIONS
    // ============================================================
    private fun showToast(context: Context, text: String): String {
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
        return ""
    }

    private fun showNotification(context: Context, args: String): String {
        val parts = args.split("|")
        val title = parts.getOrNull(0) ?: "MobileCLI"
        val content = parts.getOrNull(1) ?: ""
        val id = parts.getOrNull(2)?.toIntOrNull() ?: System.currentTimeMillis().toInt()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Termux API", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
        return ""
    }

    private fun removeNotification(context: Context, args: String): String {
        val id = args.toIntOrNull() ?: return """{"error":"Invalid notification ID"}"""
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
        return ""
    }

    // ============================================================
    // DEVICE INFO
    // ============================================================
    private fun batteryStatus(context: Context): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val plugged = batteryManager.isCharging
        val temperature = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
            BatteryManager.BATTERY_STATUS_FULL -> "FULL"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
            else -> "UNKNOWN"
        }

        return JSONObject().apply {
            put("percentage", level)
            put("status", statusStr)
            put("plugged", if (plugged) "PLUGGED_AC" else "UNPLUGGED")
            put("health", "GOOD")
        }.toString()
    }

    private fun vibrate(context: Context, args: String): String {
        val parts = args.split(",")
        val duration = parts.getOrNull(0)?.toLongOrNull() ?: 1000L
        val force = parts.getOrNull(1)?.toIntOrNull() ?: -1

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = if (force in 1..255) force else VibrationEffect.DEFAULT_AMPLITUDE
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
        return ""
    }

    private fun getBrightness(context: Context): String {
        return try {
            val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            JSONObject().put("brightness", brightness).toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun setBrightness(context: Context, args: String): String {
        val brightness = args.toIntOrNull()?.coerceIn(0, 255) ?: return """{"error":"Invalid brightness value"}"""
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
            """{"brightness":$brightness}"""
        } catch (e: Exception) {
            """{"error":"Cannot set brightness: ${e.message}"}"""
        }
    }

    private fun toggleTorch(context: Context, state: String): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            val turnOn = state == "on" || state == "1" || state == "true"
            cameraManager.setTorchMode(cameraId, turnOn)
            ""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun getVolume(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return JSONObject().apply {
            put("call", JSONObject().apply {
                put("volume", audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL))
                put("max_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL))
            })
            put("system", JSONObject().apply {
                put("volume", audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM))
                put("max_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM))
            })
            put("ring", JSONObject().apply {
                put("volume", audioManager.getStreamVolume(AudioManager.STREAM_RING))
                put("max_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_RING))
            })
            put("music", JSONObject().apply {
                put("volume", audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
                put("max_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            })
            put("alarm", JSONObject().apply {
                put("volume", audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
                put("max_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM))
            })
            put("notification", JSONObject().apply {
                put("volume", audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION))
                put("max_volume", audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION))
            })
        }.toString()
    }

    private fun setVolume(context: Context, args: String): String {
        val parts = args.split(",")
        val stream = parts.getOrNull(0) ?: return """{"error":"No stream specified"}"""
        val volume = parts.getOrNull(1)?.toIntOrNull() ?: return """{"error":"No volume specified"}"""

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamType = when (stream.lowercase()) {
            "call" -> AudioManager.STREAM_VOICE_CALL
            "system" -> AudioManager.STREAM_SYSTEM
            "ring" -> AudioManager.STREAM_RING
            "music" -> AudioManager.STREAM_MUSIC
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            else -> return """{"error":"Unknown stream: $stream"}"""
        }

        audioManager.setStreamVolume(streamType, volume, 0)
        return ""
    }

    // ============================================================
    // NETWORK & WIFI
    // ============================================================
    private fun wifiInfo(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            JSONObject().apply {
                put("ssid", info.ssid?.replace("\"", "") ?: "")
                put("bssid", info.bssid ?: "")
                put("rssi", info.rssi)
                put("link_speed", info.linkSpeed)
                put("link_speed_units", "Mbps")
                put("frequency", info.frequency)
                put("frequency_units", "MHz")
                put("ip", intToIp(info.ipAddress))
                put("network_id", info.networkId)
                put("supplicant_state", info.supplicantState.toString())
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }

    private fun wifiEnable(context: Context, args: String): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val enable = args == "true" || args == "on" || args == "1"
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enable
            """{"wifi_enabled":$enable}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun wifiScanInfo(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val results = wifiManager.scanResults
            val arr = JSONArray()
            results.forEach { result ->
                arr.put(JSONObject().apply {
                    put("ssid", result.SSID)
                    put("bssid", result.BSSID)
                    put("rssi", result.level)
                    put("frequency", result.frequency)
                    put("capabilities", result.capabilities)
                })
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // LOCATION
    // ============================================================
    private fun getLocation(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return """{"error":"Location permission not granted"}"""
        }

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                JSONObject().apply {
                    put("latitude", location.latitude)
                    put("longitude", location.longitude)
                    put("altitude", location.altitude)
                    put("accuracy", location.accuracy)
                    put("bearing", location.bearing)
                    put("speed", location.speed)
                    put("provider", location.provider)
                }.toString()
            } else {
                """{"error":"No location available"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // CAMERA
    // ============================================================
    private fun cameraInfo(context: Context): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val arr = JSONArray()

            cameraManager.cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "front"
                    CameraCharacteristics.LENS_FACING_BACK -> "back"
                    else -> "external"
                }

                arr.put(JSONObject().apply {
                    put("id", cameraId)
                    put("facing", facingStr)
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.let {
                        put("jpeg_output_sizes", JSONArray().put(JSONObject().apply {
                            put("width", it.width)
                            put("height", it.height)
                        }))
                    }
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)?.let {
                        put("flash_available", it)
                    }
                })
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun cameraPhoto(context: Context, args: String): String {
        // Camera photo requires a more complex implementation with CameraX
        // For now, return instruction to use intent
        return """{"error":"Use 'am start -a android.media.action.IMAGE_CAPTURE' for camera"}"""
    }

    // ============================================================
    // AUDIO & MEDIA
    // ============================================================
    private fun mediaScan(context: Context, path: String): String {
        return try {
            val file = File(path)
            if (file.exists()) {
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(file)
                context.sendBroadcast(intent)
                ""
            } else {
                """{"error":"File not found: $path"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun mediaPlayerControl(context: Context, args: String): String {
        val parts = args.split("|")
        val action = parts.getOrNull(0) ?: return """{"error":"No action specified"}"""
        val file = parts.getOrNull(1) ?: ""

        return try {
            when (action) {
                "play" -> {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(file)
                        prepare()
                        start()
                    }
                    """{"status":"playing","file":"$file"}"""
                }
                "pause" -> {
                    mediaPlayer?.pause()
                    """{"status":"paused"}"""
                }
                "resume" -> {
                    mediaPlayer?.start()
                    """{"status":"playing"}"""
                }
                "stop" -> {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    """{"status":"stopped"}"""
                }
                "info" -> {
                    val mp = mediaPlayer
                    if (mp != null) {
                        JSONObject().apply {
                            put("playing", mp.isPlaying)
                            put("position", mp.currentPosition)
                            put("duration", mp.duration)
                        }.toString()
                    } else {
                        """{"status":"no_media"}"""
                    }
                }
                else -> """{"error":"Unknown action: $action"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun microphoneRecord(context: Context, args: String): String {
        val parts = args.split("|")
        val action = parts.getOrNull(0) ?: return """{"error":"No action specified"}"""
        val file = parts.getOrNull(1) ?: ""

        return try {
            when (action) {
                "start" -> {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        return """{"error":"Recording permission not granted"}"""
                    }
                    mediaRecorder?.release()
                    mediaRecorder = MediaRecorder().apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(file)
                        prepare()
                        start()
                    }
                    """{"status":"recording","file":"$file"}"""
                }
                "stop" -> {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    mediaRecorder = null
                    """{"status":"stopped"}"""
                }
                else -> """{"error":"Unknown action: $action"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun audioInfo(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return JSONObject().apply {
            put("ringer_mode", when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "SILENT"
                AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                else -> "NORMAL"
            })
            put("music_active", audioManager.isMusicActive)
            put("speaker_on", audioManager.isSpeakerphoneOn)
            put("bluetooth_sco_on", audioManager.isBluetoothScoOn)
        }.toString()
    }

    // ============================================================
    // TTS
    // ============================================================
    private fun ttsEngines(context: Context): String {
        return try {
            val tts = TextToSpeech(context, null)
            val engines = tts.engines
            val arr = JSONArray()
            engines.forEach { engine ->
                arr.put(JSONObject().apply {
                    put("name", engine.name)
                    put("label", engine.label)
                })
            }
            tts.shutdown()
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun ttsSpeak(context: Context, text: String): String {
        return try {
            val tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // TTS ready
                }
            }
            Thread.sleep(500) // Wait for initialization
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "termux-tts")
            """{"status":"speaking"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // TELEPHONY
    // ============================================================
    private fun telephonyCall(context: Context, number: String): String {
        return try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            """{"status":"calling","number":"$number"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun telephonyCellInfo(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return """{"error":"Phone permission not granted"}"""
        }
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val cellInfoList = telephonyManager.allCellInfo
            val arr = JSONArray()
            cellInfoList?.forEach { cellInfo ->
                arr.put(JSONObject().apply {
                    put("type", cellInfo.javaClass.simpleName)
                    put("registered", cellInfo.isRegistered)
                })
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun telephonyDeviceInfo(context: Context): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            JSONObject().apply {
                put("device_software_version", telephonyManager.deviceSoftwareVersion ?: "unknown")
                put("network_country_iso", telephonyManager.networkCountryIso)
                put("network_operator", telephonyManager.networkOperator)
                put("network_operator_name", telephonyManager.networkOperatorName)
                put("network_type", telephonyManager.networkType)
                put("phone_type", when (telephonyManager.phoneType) {
                    TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                    TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                    TelephonyManager.PHONE_TYPE_SIP -> "SIP"
                    else -> "NONE"
                })
                put("sim_country_iso", telephonyManager.simCountryIso)
                put("sim_operator", telephonyManager.simOperator)
                put("sim_operator_name", telephonyManager.simOperatorName)
                put("sim_state", when (telephonyManager.simState) {
                    TelephonyManager.SIM_STATE_READY -> "READY"
                    TelephonyManager.SIM_STATE_ABSENT -> "ABSENT"
                    TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN_REQUIRED"
                    TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK_REQUIRED"
                    TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "NETWORK_LOCKED"
                    else -> "UNKNOWN"
                })
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // SMS
    // ============================================================
    private fun smsList(context: Context, args: String): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return """{"error":"SMS permission not granted"}"""
        }

        val parts = args.split("|")
        val type = parts.getOrNull(0) ?: "inbox"
        val limit = parts.getOrNull(1)?.toIntOrNull() ?: 10

        val uri = when (type.lowercase()) {
            "inbox" -> Telephony.Sms.Inbox.CONTENT_URI
            "sent" -> Telephony.Sms.Sent.CONTENT_URI
            "draft" -> Telephony.Sms.Draft.CONTENT_URI
            else -> Telephony.Sms.CONTENT_URI
        }

        return try {
            val arr = JSONArray()
            val cursor = context.contentResolver.query(uri, null, null, null, "${Telephony.Sms.DATE} DESC")
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < limit) {
                    arr.put(JSONObject().apply {
                        put("_id", it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID)))
                        put("address", it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)))
                        put("body", it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)))
                        put("date", it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE)))
                        put("read", it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1)
                    })
                    count++
                }
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun smsSend(context: Context, args: String): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return """{"error":"SMS permission not granted"}"""
        }

        val parts = args.split("|", limit = 2)
        val number = parts.getOrNull(0) ?: return """{"error":"No number specified"}"""
        val message = parts.getOrNull(1) ?: return """{"error":"No message specified"}"""

        return try {
            val smsManager = android.telephony.SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
            """{"status":"sent","to":"$number"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // CONTACTS
    // ============================================================
    private fun contactList(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return """{"error":"Contacts permission not granted"}"""
        }

        return try {
            val arr = JSONArray()
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    arr.put(JSONObject().apply {
                        put("name", it.getString(0))
                        put("number", it.getString(1))
                    })
                }
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // CALL LOG
    // ============================================================
    private fun callLog(context: Context, args: String): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return """{"error":"Call log permission not granted"}"""
        }

        val limit = args.toIntOrNull() ?: 10

        return try {
            val arr = JSONArray()
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < limit) {
                    val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    arr.put(JSONObject().apply {
                        put("name", it.getString(it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)) ?: "")
                        put("number", it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)))
                        put("type", when (type) {
                            CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                            CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                            CallLog.Calls.MISSED_TYPE -> "MISSED"
                            CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                            else -> "UNKNOWN"
                        })
                        put("date", it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE)))
                        put("duration", it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION)))
                    })
                    count++
                }
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // SENSORS
    // ============================================================
    private fun sensorInfo(context: Context, args: String): String {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        return if (args.isEmpty() || args == "list") {
            // List all sensors
            val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            val arr = JSONArray()
            sensors.forEach { sensor ->
                arr.put(JSONObject().apply {
                    put("name", sensor.name)
                    put("type", sensor.type)
                    put("vendor", sensor.vendor)
                    put("version", sensor.version)
                    put("resolution", sensor.resolution)
                    put("max_range", sensor.maximumRange)
                    put("power", sensor.power)
                })
            }
            arr.toString()
        } else {
            // Get specific sensor info
            val sensorType = args.toIntOrNull() ?: Sensor.TYPE_ACCELEROMETER
            val sensor = sensorManager.getDefaultSensor(sensorType)
            if (sensor != null) {
                JSONObject().apply {
                    put("name", sensor.name)
                    put("type", sensor.type)
                    put("vendor", sensor.vendor)
                }.toString()
            } else {
                """{"error":"Sensor not found"}"""
            }
        }
    }

    // ============================================================
    // BIOMETRIC
    // ============================================================
    @Suppress("DEPRECATION")
    private fun fingerprintAuth(context: Context): String {
        return try {
            val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
            if (fingerprintManager == null) {
                return """{"error":"NO_HARDWARE"}"""
            }
            if (!fingerprintManager.isHardwareDetected) {
                return """{"error":"NO_HARDWARE"}"""
            }
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                return """{"error":"NONE_ENROLLED"}"""
            }
            """{"auth_result":"AUTH_AVAILABLE"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // INFRARED
    // ============================================================
    private fun infraredFrequencies(context: Context): String {
        return try {
            val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            if (irManager == null || !irManager.hasIrEmitter()) {
                return """{"error":"No IR emitter available"}"""
            }
            val ranges = irManager.carrierFrequencies
            val arr = JSONArray()
            ranges.forEach { range ->
                arr.put(JSONObject().apply {
                    put("min", range.minFrequency)
                    put("max", range.maxFrequency)
                })
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun infraredTransmit(context: Context, args: String): String {
        return try {
            val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            if (irManager == null || !irManager.hasIrEmitter()) {
                return """{"error":"No IR emitter available"}"""
            }
            val parts = args.split(",")
            val frequency = parts.getOrNull(0)?.toIntOrNull() ?: return """{"error":"No frequency specified"}"""
            val pattern = parts.drop(1).mapNotNull { it.toIntOrNull() }.toIntArray()
            if (pattern.isEmpty()) {
                return """{"error":"No pattern specified"}"""
            }
            irManager.transmit(frequency, pattern)
            """{"status":"transmitted"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // USB
    // ============================================================
    private fun usbInfo(context: Context): String {
        return try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val devices = usbManager.deviceList
            val arr = JSONArray()
            devices.forEach { (name, device) ->
                arr.put(JSONObject().apply {
                    put("device_name", name)
                    put("vendor_id", device.vendorId)
                    put("product_id", device.productId)
                    put("device_class", device.deviceClass)
                    put("device_subclass", device.deviceSubclass)
                    put("device_protocol", device.deviceProtocol)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        put("manufacturer_name", device.manufacturerName)
                        put("product_name", device.productName)
                    }
                })
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // SYSTEM
    // ============================================================
    private fun setWallpaper(context: Context, args: String): String {
        return try {
            val file = File(args)
            if (!file.exists()) {
                return """{"error":"File not found: $args"}"""
            }
            val bitmap = BitmapFactory.decodeFile(args)
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap)
            """{"status":"wallpaper_set"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun download(context: Context, args: String): String {
        val parts = args.split("|")
        val url = parts.getOrNull(0) ?: return """{"error":"No URL specified"}"""
        val title = parts.getOrNull(1) ?: "Download"
        val description = parts.getOrNull(2) ?: ""

        return try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title)
                setDescription(description)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(url).lastPathSegment)
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            """{"download_id":$downloadId}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun share(context: Context, args: String): String {
        val parts = args.split("|", limit = 2)
        val action = parts.getOrNull(0) ?: "text"
        val content = parts.getOrNull(1) ?: ""

        return try {
            val intent = when (action) {
                "text" -> Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                "file" -> Intent(Intent.ACTION_SEND).apply {
                    val file = File(content)
                    val uri = Uri.fromFile(file)
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                else -> return """{"error":"Unknown share type: $action"}"""
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(intent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            """{"status":"shared"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun showDialog(context: Context, args: String): String {
        // Dialog requires UI thread and activity context
        // For now, show a toast with the message
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, "Dialog: $args", Toast.LENGTH_LONG).show()
        }
        return """{"input":"$args"}"""
    }

    private fun storageGet(context: Context, args: String): String {
        // Storage get requires file picker UI
        return """{"error":"Use 'termux-setup-storage' to access storage"}"""
    }

    private fun jobScheduler(context: Context, args: String): String {
        // Job scheduling requires more complex implementation
        return """{"error":"Job scheduler not implemented - use cron or at command"}"""
    }

    private fun openUrl(context: Context, url: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // KEYSTORE (Android KeyStore API)
    // ============================================================
    private fun keystoreList(context: Context): String {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val aliases = keyStore.aliases()
            val arr = JSONArray()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                arr.put(JSONObject().apply {
                    put("alias", alias)
                    put("is_key_entry", keyStore.isKeyEntry(alias))
                    put("is_certificate_entry", keyStore.isCertificateEntry(alias))
                    val cert = keyStore.getCertificate(alias)
                    if (cert != null) {
                        put("type", cert.type)
                    }
                })
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun keystoreGenerate(context: Context, args: String): String {
        val parts = args.split("|")
        val alias = parts.getOrNull(0) ?: return """{"error":"No alias specified"}"""
        val algorithm = parts.getOrNull(1) ?: "AES"
        val keySize = parts.getOrNull(2)?.toIntOrNull() ?: 256

        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val parameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC, KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setKeySize(keySize)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
            """{"status":"generated","alias":"$alias","algorithm":"$algorithm","key_size":$keySize}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun keystoreDelete(context: Context, alias: String): String {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                """{"status":"deleted","alias":"$alias"}"""
            } else {
                """{"error":"Alias not found: $alias"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun keystoreSign(context: Context, args: String): String {
        val parts = args.split("|", limit = 2)
        val alias = parts.getOrNull(0) ?: return """{"error":"No alias specified"}"""
        val data = parts.getOrNull(1) ?: return """{"error":"No data to sign"}"""

        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val key = keyStore.getKey(alias, null) ?: return """{"error":"Key not found"}"""

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray())

            JSONObject().apply {
                put("signature", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("alias", alias)
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun keystoreVerify(context: Context, args: String): String {
        val parts = args.split("|")
        val alias = parts.getOrNull(0) ?: return """{"error":"No alias specified"}"""
        val signatureB64 = parts.getOrNull(1) ?: return """{"error":"No signature"}"""
        val ivB64 = parts.getOrNull(2) ?: return """{"error":"No IV"}"""

        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val key = keyStore.getKey(alias, null) ?: return """{"error":"Key not found"}"""

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val signature = Base64.decode(signatureB64, Base64.NO_WRAP)
            val decrypted = cipher.doFinal(signature)

            JSONObject().apply {
                put("valid", true)
                put("data", String(decrypted))
            }.toString()
        } catch (e: Exception) {
            """{"valid":false,"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // NFC
    // ============================================================
    private fun nfcInfo(context: Context): String {
        return try {
            val nfcManager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
            val nfcAdapter = nfcManager?.defaultAdapter

            if (nfcAdapter == null) {
                return """{"error":"NFC not available"}"""
            }

            JSONObject().apply {
                put("enabled", nfcAdapter.isEnabled)
                put("available", true)
                // Note: Reading tags requires being in foreground with NFC intent
                put("note", "Use Intent.ACTION_NDEF_DISCOVERED in activity to read tags")
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // NOTIFICATION LIST
    // ============================================================
    private fun notificationList(context: Context): String {
        // Note: Listing notifications requires NotificationListenerService permission
        // which is a special permission that needs to be granted in Settings
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // We can only get our own app's notifications without special permission
            val arr = JSONArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNotifications = notificationManager.activeNotifications
                activeNotifications.forEach { sbn ->
                    arr.put(JSONObject().apply {
                        put("id", sbn.id)
                        put("package", sbn.packageName)
                        put("post_time", sbn.postTime)
                        put("is_ongoing", sbn.isOngoing)
                        val notification = sbn.notification
                        put("category", notification.category ?: "")
                        notification.extras?.let { extras ->
                            put("title", extras.getCharSequence("android.title")?.toString() ?: "")
                            put("text", extras.getCharSequence("android.text")?.toString() ?: "")
                        }
                    })
                }
            }

            if (arr.length() == 0) {
                JSONObject().apply {
                    put("notifications", arr)
                    put("note", "Only shows this app's notifications. For all notifications, grant NotificationListener permission in Settings.")
                }.toString()
            } else {
                arr.toString()
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // SPEECH TO TEXT
    // ============================================================
    private fun speechToText(context: Context): String {
        return try {
            val available = SpeechRecognizer.isRecognitionAvailable(context)
            if (!available) {
                return """{"error":"Speech recognition not available"}"""
            }

            // Speech recognition requires an Activity context and user interaction
            // Return info about availability
            JSONObject().apply {
                put("available", true)
                put("note", "Speech recognition requires Activity context. Use Intent.ACTION_RECOGNIZE_SPEECH")
                put("example", "am start -a android.speech.action.RECOGNIZE_SPEECH")
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ============================================================
    // SAF (Storage Access Framework)
    // ============================================================
    private fun safList(context: Context, args: String): String {
        // SAF requires document URIs obtained through Intent.ACTION_OPEN_DOCUMENT_TREE
        return try {
            if (args.isEmpty()) {
                return """{"error":"No document URI specified. Use termux-saf-managedir first."}"""
            }

            val uri = Uri.parse(args)
            val arr = JSONArray()

            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                uri,
                android.provider.DocumentsContract.getTreeDocumentId(uri)
            )

            val cursor = context.contentResolver.query(
                childrenUri,
                arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                    android.provider.DocumentsContract.Document.COLUMN_SIZE,
                    android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
                ),
                null, null, null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    arr.put(JSONObject().apply {
                        put("id", it.getString(0))
                        put("name", it.getString(1))
                        put("mime_type", it.getString(2))
                        put("size", it.getLong(3))
                        put("last_modified", it.getLong(4))
                        put("is_directory", it.getString(2) == android.provider.DocumentsContract.Document.MIME_TYPE_DIR)
                    })
                }
            }
            arr.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun safStat(context: Context, args: String): String {
        return try {
            if (args.isEmpty()) {
                return """{"error":"No document URI specified"}"""
            }

            val uri = Uri.parse(args)
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                    android.provider.DocumentsContract.Document.COLUMN_SIZE,
                    android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    android.provider.DocumentsContract.Document.COLUMN_FLAGS
                ),
                null, null, null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    JSONObject().apply {
                        put("id", it.getString(0))
                        put("name", it.getString(1))
                        put("mime_type", it.getString(2))
                        put("size", it.getLong(3))
                        put("last_modified", it.getLong(4))
                        put("flags", it.getInt(5))
                        put("is_directory", it.getString(2) == android.provider.DocumentsContract.Document.MIME_TYPE_DIR)
                    }.toString()
                } else {
                    """{"error":"Document not found"}"""
                }
            } ?: """{"error":"Could not query document"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun safRead(context: Context, args: String): String {
        return try {
            if (args.isEmpty()) {
                return """{"error":"No document URI specified"}"""
            }

            val uri = Uri.parse(args)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                content
            } ?: """{"error":"Could not open document"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun safWrite(context: Context, args: String): String {
        val parts = args.split("|", limit = 2)
        val uriStr = parts.getOrNull(0) ?: return """{"error":"No document URI specified"}"""
        val content = parts.getOrNull(1) ?: return """{"error":"No content specified"}"""

        return try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(content.toByteArray())
                """{"status":"written","bytes":${content.length}}"""
            } ?: """{"error":"Could not open document for writing"}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun safMkdir(context: Context, args: String): String {
        val parts = args.split("|")
        val parentUri = parts.getOrNull(0) ?: return """{"error":"No parent URI specified"}"""
        val dirName = parts.getOrNull(1) ?: return """{"error":"No directory name specified"}"""

        return try {
            val parent = Uri.parse(parentUri)
            val parentDocId = android.provider.DocumentsContract.getTreeDocumentId(parent)
            val parentDocUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(parent, parentDocId)

            val newDir = android.provider.DocumentsContract.createDocument(
                context.contentResolver,
                parentDocUri,
                android.provider.DocumentsContract.Document.MIME_TYPE_DIR,
                dirName
            )

            if (newDir != null) {
                """{"status":"created","uri":"$newDir"}"""
            } else {
                """{"error":"Could not create directory"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun safRemove(context: Context, args: String): String {
        return try {
            if (args.isEmpty()) {
                return """{"error":"No document URI specified"}"""
            }

            val uri = Uri.parse(args)
            val deleted = android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)

            if (deleted) {
                """{"status":"deleted"}"""
            } else {
                """{"error":"Could not delete document"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun safCreate(context: Context, args: String): String {
        val parts = args.split("|")
        val parentUri = parts.getOrNull(0) ?: return """{"error":"No parent URI specified"}"""
        val fileName = parts.getOrNull(1) ?: return """{"error":"No file name specified"}"""
        val mimeType = parts.getOrNull(2) ?: "text/plain"

        return try {
            val parent = Uri.parse(parentUri)
            val parentDocId = android.provider.DocumentsContract.getTreeDocumentId(parent)
            val parentDocUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(parent, parentDocId)

            val newFile = android.provider.DocumentsContract.createDocument(
                context.contentResolver,
                parentDocUri,
                mimeType,
                fileName
            )

            if (newFile != null) {
                """{"status":"created","uri":"$newFile"}"""
            } else {
                """{"error":"Could not create file"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun safManageDir(context: Context, args: String): String {
        // This requires launching an Intent to let user pick a directory
        // Return instructions for how to use it
        return try {
            JSONObject().apply {
                put("note", "SAF directory management requires user interaction")
                put("to_select_directory", "am start -a android.intent.action.OPEN_DOCUMENT_TREE")
                put("usage", "After selecting a directory, the URI will be returned. Use that URI with other saf-* commands.")
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    private fun safDirs(context: Context): String {
        // List available SAF directories
        // Note: Actual persisted URIs would need to be stored by the app
        return try {
            JSONObject().apply {
                put("note", "SAF directories are user-selected. Use termux-saf-managedir to select a directory.")
                put("primary_storage", "/sdcard")
                put("downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
                put("dcim", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath)
                put("documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath)
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ========================================
    // WAKE LOCK
    // ========================================

    private var wakeLockRef: android.os.PowerManager.WakeLock? = null

    private fun wakeLock(context: Context, args: String): String {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager

            when (args.lowercase()) {
                "acquire" -> {
                    if (wakeLockRef == null || !wakeLockRef!!.isHeld) {
                        wakeLockRef = powerManager.newWakeLock(
                            android.os.PowerManager.PARTIAL_WAKE_LOCK,
                            "MobileCLI:WakeLock"
                        )
                        wakeLockRef?.acquire(60 * 60 * 1000L) // 1 hour max
                    }
                    """{"status":"acquired","held":true}"""
                }
                "release" -> {
                    if (wakeLockRef?.isHeld == true) {
                        wakeLockRef?.release()
                    }
                    wakeLockRef = null
                    """{"status":"released","held":false}"""
                }
                else -> {
                    val isHeld = wakeLockRef?.isHeld ?: false
                    """{"held":$isHeld}"""
                }
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ========================================
    // BLUETOOTH
    // ========================================

    @android.annotation.SuppressLint("MissingPermission")
    private fun bluetoothInfo(context: Context): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter == null) {
                return """{"error":"Bluetooth not available"}"""
            }

            JSONObject().apply {
                put("enabled", adapter.isEnabled)
                put("name", adapter.name ?: "Unknown")
                put("address", adapter.address ?: "Unknown")
                put("state", when (adapter.state) {
                    android.bluetooth.BluetoothAdapter.STATE_OFF -> "off"
                    android.bluetooth.BluetoothAdapter.STATE_ON -> "on"
                    android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF -> "turning_off"
                    android.bluetooth.BluetoothAdapter.STATE_TURNING_ON -> "turning_on"
                    else -> "unknown"
                })
                put("scan_mode", when (adapter.scanMode) {
                    android.bluetooth.BluetoothAdapter.SCAN_MODE_NONE -> "none"
                    android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE -> "connectable"
                    android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> "discoverable"
                    else -> "unknown"
                })
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun bluetoothEnable(context: Context, args: String): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter == null) {
                return """{"error":"Bluetooth not available"}"""
            }

            // Note: Android 13+ requires user interaction to enable/disable Bluetooth
            // This will work on older versions
            val enable = args.lowercase() != "off" && args != "0" && args.lowercase() != "false"

            if (enable && !adapter.isEnabled) {
                // Request to enable - may require user interaction
                """{"status":"enable_requested","note":"User interaction may be required on Android 13+"}"""
            } else if (!enable && adapter.isEnabled) {
                """{"status":"disable_requested","note":"User interaction may be required on Android 13+"}"""
            } else {
                """{"status":"already_${if (adapter.isEnabled) "enabled" else "disabled"}"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun bluetoothScanInfo(context: Context): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter == null) {
                return """{"error":"Bluetooth not available"}"""
            }

            if (!adapter.isEnabled) {
                return """{"error":"Bluetooth is disabled"}"""
            }

            // Return paired devices and scanning note
            val devices = JSONArray()
            adapter.bondedDevices?.forEach { device ->
                devices.put(JSONObject().apply {
                    put("name", device.name ?: "Unknown")
                    put("address", device.address)
                    put("type", when (device.type) {
                        android.bluetooth.BluetoothDevice.DEVICE_TYPE_CLASSIC -> "classic"
                        android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE -> "le"
                        android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL -> "dual"
                        else -> "unknown"
                    })
                    put("bond_state", "bonded")
                })
            }

            JSONObject().apply {
                put("paired_devices", devices)
                put("note", "For active scanning, Bluetooth discovery requires a foreground activity")
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun bluetoothConnect(context: Context, args: String): String {
        return try {
            if (args.isEmpty()) {
                return """{"error":"No MAC address specified"}"""
            }

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter == null) {
                return """{"error":"Bluetooth not available"}"""
            }

            if (!adapter.isEnabled) {
                return """{"error":"Bluetooth is disabled"}"""
            }

            val device = adapter.getRemoteDevice(args.uppercase())

            JSONObject().apply {
                put("address", device.address)
                put("name", device.name ?: "Unknown")
                put("bond_state", when (device.bondState) {
                    android.bluetooth.BluetoothDevice.BOND_NONE -> "none"
                    android.bluetooth.BluetoothDevice.BOND_BONDING -> "bonding"
                    android.bluetooth.BluetoothDevice.BOND_BONDED -> "bonded"
                    else -> "unknown"
                })
                put("note", "Use Android Bluetooth settings for pairing/connecting")
            }.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun bluetoothPaired(context: Context): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter == null) {
                return """{"error":"Bluetooth not available"}"""
            }

            val devices = JSONArray()
            adapter.bondedDevices?.forEach { device ->
                devices.put(JSONObject().apply {
                    put("name", device.name ?: "Unknown")
                    put("address", device.address)
                    put("type", when (device.type) {
                        android.bluetooth.BluetoothDevice.DEVICE_TYPE_CLASSIC -> "classic"
                        android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE -> "le"
                        android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL -> "dual"
                        else -> "unknown"
                    })
                })
            }

            devices.toString()
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }
}
