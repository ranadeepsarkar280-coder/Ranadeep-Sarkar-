package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ai.AiMessage
import com.example.ai.PlaceholderAiRepository
import com.example.assistant.AppLauncher
import com.example.assistant.BengaliCommandParser
import com.example.assistant.CommandExecutor
import com.example.assistant.HardwareController
import com.example.assistant.JarvisRuntimeHub
import com.example.assistant.VoiceEngine
import com.example.assistant.VoiceState
import com.example.data.JarvisPreferencesRepository
import com.example.data.JarvisSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class JarvisForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val restartHandler = Handler(Looper.getMainLooper())

    private lateinit var prefsRepository: JarvisPreferencesRepository
    private lateinit var hardwareController: HardwareController
    private lateinit var appLauncher: AppLauncher
    private lateinit var commandExecutor: CommandExecutor
    private lateinit var voiceEngine: VoiceEngine

    private var currentSettings = JarvisSettings()
    private var isListeningRequested = false
    private var awaitingCommandAfterWake = false

    private val restartWakeRunnable = Runnable {
        if (isListeningRequested && hasMicPermission()) {
            voiceEngine.startListening(isWakeWordMode = !awaitingCommandAfterWake)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        prefsRepository = JarvisPreferencesRepository(applicationContext)
        hardwareController = HardwareController(applicationContext)
        appLauncher = AppLauncher(applicationContext)
        commandExecutor = CommandExecutor(
            appLauncher = appLauncher,
            hardwareController = hardwareController,
            aiRepository = PlaceholderAiRepository(),
        )

        voiceEngine = VoiceEngine(
            context = applicationContext,
            onSpeechResult = ::handleSpeechRecognized,
            onPartialSpeech = { partial ->
                JarvisRuntimeHub.setPartialTranscript(partial)
            },
            onRmsChanged = { rms ->
                JarvisRuntimeHub.setMicRmsLevel(rms)
            },
            onVoiceStateChanged = { state ->
                JarvisRuntimeHub.setVoiceState(state)
            },
            onTtsReadyChanged = { ready, status ->
                JarvisRuntimeHub.setTtsStatus(ready, status)
            },
            onSpeechError = { message, isFatal ->
                JarvisRuntimeHub.setPartialTranscript("")
                if (isFatal) {
                    isListeningRequested = false
                    JarvisRuntimeHub.setBackgroundListening(false)
                    if (message.isNotBlank()) {
                        JarvisRuntimeHub.appendMessage(AiMessage.Sender.SYSTEM, message)
                    }
                    updateNotification()
                } else if (isListeningRequested) {
                    awaitingCommandAfterWake = false
                    scheduleWakeRestart(delayMs = 450L)
                }
            },
        )

        serviceScope.launch {
            prefsRepository.settingsFlow.collect { settings ->
                currentSettings = settings
                voiceEngine.updateSpeechRate(settings.ttsSpeechRate)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasMicPermission()) {
            Log.w(TAG, "RECORD_AUDIO missing; cannot run microphone foreground service.")
            JarvisRuntimeHub.appendMessage(
                AiMessage.Sender.SYSTEM,
                "Boss, মাইক্রোফোন অনুমতি না থাকায় ব্যাকগ্রাউন্ড সার্ভিস চালু করা যায়নি।"
            )
            JarvisRuntimeHub.setServiceRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification(isListening = true)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enter foreground microphone service", e)
            JarvisRuntimeHub.setServiceRunning(false)
            JarvisRuntimeHub.appendMessage(
                AiMessage.Sender.SYSTEM,
                "Boss, অ্যান্ড্রয়েড সিস্টেম সীমাবদ্ধতার কারণে ব্যাকগ্রাউন্ড মাইক্রোফোন সার্ভিস চালু হয়নি।"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        JarvisRuntimeHub.setServiceRunning(true)

        when (intent?.action) {
            ACTION_STOP_LISTENING -> {
                pauseListening()
            }
            ACTION_SHUTDOWN_SERVICE -> {
                isListeningRequested = false
                voiceEngine.stopAll()
                JarvisRuntimeHub.setServiceRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_ACTIVE_COMMAND -> {
                startCommandListeningDirectly()
            }
            else -> {
                // ACTION_START_LISTENING or OS restart (START_STICKY)
                startWakeWordListening()
            }
        }

        return START_STICKY
    }

    private fun startWakeWordListening() {
        restartHandler.removeCallbacks(restartWakeRunnable)
        isListeningRequested = true
        awaitingCommandAfterWake = false
        JarvisRuntimeHub.setBackgroundListening(true)
        updateNotification()
        voiceEngine.startListening(isWakeWordMode = true)
    }

    private fun startCommandListeningDirectly() {
        restartHandler.removeCallbacks(restartWakeRunnable)
        isListeningRequested = true
        awaitingCommandAfterWake = true
        JarvisRuntimeHub.setBackgroundListening(true)
        updateNotification()
        voiceEngine.playActivationChime()
        voiceEngine.startListening(isWakeWordMode = false)
    }

    private fun pauseListening() {
        restartHandler.removeCallbacks(restartWakeRunnable)
        isListeningRequested = false
        awaitingCommandAfterWake = false
        JarvisRuntimeHub.setBackgroundListening(false)
        JarvisRuntimeHub.setPartialTranscript("")
        voiceEngine.stopListening()
        updateNotification()
    }

    private fun handleSpeechRecognized(spokenText: String) {
        JarvisRuntimeHub.setPartialTranscript("")
        val hasWakeWord = BengaliCommandParser.containsWakeWord(spokenText)

        if (!awaitingCommandAfterWake && currentSettings.wakeWordEnabled && !hasWakeWord) {
            // Wake word was required in background mode and not heard; restart wake listening politely
            if (isListeningRequested) {
                scheduleWakeRestart(delayMs = 350L)
            }
            return
        }

        if (hasWakeWord) {
            voiceEngine.playActivationChime()
        }

        JarvisRuntimeHub.appendMessage(AiMessage.Sender.BOSS, spokenText)

        serviceScope.launch {
            val latestSettings = runCatching { prefsRepository.settingsFlow.first() }
                .getOrDefault(currentSettings)
            val outcome = commandExecutor.execute(
                rawInput = spokenText,
                settings = latestSettings,
                recentHistory = JarvisRuntimeHub.messages.value,
            )

            outcome.requiresSecurityConfirmation?.let { pending ->
                JarvisRuntimeHub.requestSecurityConfirmation(pending)
            }

            JarvisRuntimeHub.appendMessage(
                sender = AiMessage.Sender.JARVIS,
                text = outcome.replyText,
                isPlaceholder = outcome.isPlaceholderAi,
            )

            if (outcome.isWakeOnly) {
                awaitingCommandAfterWake = true
                voiceEngine.speak(
                    text = outcome.replyText,
                    enabled = latestSettings.voiceResponseEnabled,
                    listenCommandAfter = true,
                    resumeWakeAfter = isListeningRequested,
                )
            } else {
                awaitingCommandAfterWake = false
                voiceEngine.speak(
                    text = outcome.replyText,
                    enabled = latestSettings.voiceResponseEnabled,
                    listenCommandAfter = false,
                    resumeWakeAfter = isListeningRequested,
                )
            }
        }
    }

    private fun scheduleWakeRestart(delayMs: Long) {
        restartHandler.removeCallbacks(restartWakeRunnable)
        restartHandler.postDelayed(restartWakeRunnable, delayMs)
    }

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify(NOTIFICATION_ID, buildNotification(isListening = isListeningRequested))
    }

    private fun buildNotification(isListening: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val startPending = PendingIntent.getService(
            this,
            101,
            Intent(this, JarvisForegroundService::class.java).apply {
                action = ACTION_START_LISTENING
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopPending = PendingIntent.getService(
            this,
            102,
            Intent(this, JarvisForegroundService::class.java).apply {
                action = ACTION_STOP_LISTENING
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val shutdownPending = PendingIntent.getService(
            this,
            103,
            Intent(this, JarvisForegroundService::class.java).apply {
                action = ACTION_SHUTDOWN_SERVICE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = if (isListening) {
            getString(R.string.notification_listening)
        } else {
            getString(R.string.notification_paused)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (isListening) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_action_stop),
                stopPending,
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_btn_speak_now,
                getString(R.string.notification_action_start),
                startPending,
            )
        }
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.notification_action_shutdown),
            shutdownPending,
        )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        restartHandler.removeCallbacksAndMessages(null)
        isListeningRequested = false
        voiceEngine.shutdown()
        hardwareController.release()
        JarvisRuntimeHub.setServiceRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "JarvisForegroundSvc"
        const val CHANNEL_ID = "jarvis_assistant_channel"
        const val NOTIFICATION_ID = 4042

        const val ACTION_START_LISTENING = "com.example.action.START_LISTENING"
        const val ACTION_START_ACTIVE_COMMAND = "com.example.action.START_ACTIVE_COMMAND"
        const val ACTION_STOP_LISTENING = "com.example.action.STOP_LISTENING"
        const val ACTION_SHUTDOWN_SERVICE = "com.example.action.SHUTDOWN_SERVICE"

        fun start(context: Context, action: String = ACTION_START_LISTENING) {
            val intent = Intent(context, JarvisForegroundService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, JarvisForegroundService::class.java))
            JarvisRuntimeHub.setServiceRunning(false)
        }
    }
}
