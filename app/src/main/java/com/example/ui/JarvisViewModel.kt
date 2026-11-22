package com.example.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiMessage
import com.example.ai.AiRepository
import com.example.ai.PlaceholderAiRepository
import com.example.assistant.AppLauncher
import com.example.assistant.BengaliCommandParser
import com.example.assistant.CommandExecutor
import com.example.assistant.DeviceTelemetry
import com.example.assistant.HardwareController
import com.example.assistant.JarvisRuntimeHub
import com.example.assistant.PendingSecurityConfirmation
import com.example.assistant.VoiceEngine
import com.example.assistant.VoiceState
import com.example.data.JarvisPreferencesRepository
import com.example.data.JarvisSettings
import com.example.service.JarvisForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JarvisUiState(
    val settings: JarvisSettings = JarvisSettings(),
    val telemetry: DeviceTelemetry = DeviceTelemetry(),
    val messages: List<AiMessage> = emptyList(),
    val voiceState: VoiceState = VoiceState.IDLE,
    val micRmsLevel: Float = 0f,
    val partialTranscript: String = "",
    val isServiceRunning: Boolean = false,
    val isBackgroundListening: Boolean = false,
    val isTtsReady: Boolean = false,
    val ttsStatusLabel: String = "",
    val hasMicPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isProcessingCommand: Boolean = false,
    val isCloudAiConnected: Boolean = false,
    val aiStatusNotice: String = "",
    val pendingConfirmation: PendingSecurityConfirmation? = null,
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefsRepository = JarvisPreferencesRepository(appContext)
    private val hardwareController = HardwareController(appContext)
    private val appLauncher = AppLauncher(appContext)
    private val aiRepository: AiRepository = PlaceholderAiRepository()
    private val commandExecutor = CommandExecutor(
        appLauncher = appLauncher,
        hardwareController = hardwareController,
        aiRepository = aiRepository,
    )

    private val _telemetry = MutableStateFlow(hardwareController.readTelemetry())
    private val _hasMicPermission = MutableStateFlow(checkMicPermission())
    private val _hasNotificationPermission = MutableStateFlow(checkNotificationPermission())
    private val _isProcessing = MutableStateFlow(false)

    private var foregroundVoiceEngine: VoiceEngine? = null

    init {
        initForegroundVoiceEngine()
        viewModelScope.launch {
            hardwareController.isTorchOn.collect {
                refreshTelemetry()
            }
        }
        viewModelScope.launch {
            prefsRepository.settingsFlow.collect { settings ->
                foregroundVoiceEngine?.updateSpeechRate(settings.ttsSpeechRate)
            }
        }
    }

    private val runtimeSlice = combine(
        JarvisRuntimeHub.voiceState,
        JarvisRuntimeHub.micRmsLevel,
        JarvisRuntimeHub.partialTranscript,
        JarvisRuntimeHub.isServiceRunning,
        JarvisRuntimeHub.isBackgroundListening,
    ) { voiceState, rms, partial, serviceRunning, bgListening ->
        RuntimeSlice(voiceState, rms, partial, serviceRunning, bgListening)
    }

    private val permissionSlice = combine(
        _hasMicPermission,
        _hasNotificationPermission,
        _isProcessing,
        JarvisRuntimeHub.isTtsReady,
        JarvisRuntimeHub.ttsStatus,
    ) { micPerm, notifPerm, processing, ttsReady, ttsStatus ->
        PermissionSlice(micPerm, notifPerm, processing, ttsReady, ttsStatus)
    }

    val uiState: StateFlow<JarvisUiState> = combine(
        prefsRepository.settingsFlow,
        _telemetry,
        JarvisRuntimeHub.messages,
        runtimeSlice,
        permissionSlice,
        JarvisRuntimeHub.pendingConfirmation,
    ) { values ->
        val settings = values[0] as JarvisSettings
        val telemetry = values[1] as DeviceTelemetry
        @Suppress("UNCHECKED_CAST")
        val messages = values[2] as List<AiMessage>
        val runtime = values[3] as RuntimeSlice
        val perms = values[4] as PermissionSlice
        val confirmation = values[5] as PendingSecurityConfirmation?

        JarvisUiState(
            settings = settings,
            telemetry = telemetry,
            messages = messages,
            voiceState = runtime.voiceState,
            micRmsLevel = runtime.rms,
            partialTranscript = runtime.partialTranscript,
            isServiceRunning = runtime.serviceRunning,
            isBackgroundListening = runtime.bgListening,
            isTtsReady = perms.ttsReady,
            ttsStatusLabel = perms.ttsStatus,
            hasMicPermission = perms.micPerm,
            hasNotificationPermission = perms.notifPerm,
            isProcessingCommand = perms.processing,
            isCloudAiConnected = aiRepository.isCloudAiConnected,
            aiStatusNotice = aiRepository.statusNotice,
            pendingConfirmation = confirmation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = JarvisUiState(
            telemetry = _telemetry.value,
            hasMicPermission = _hasMicPermission.value,
            hasNotificationPermission = _hasNotificationPermission.value,
            isCloudAiConnected = aiRepository.isCloudAiConnected,
            aiStatusNotice = aiRepository.statusNotice,
        ),
    )

    private fun initForegroundVoiceEngine() {
        foregroundVoiceEngine = VoiceEngine(
            context = appContext,
            onSpeechResult = { spoken ->
                JarvisRuntimeHub.setPartialTranscript("")
                handleVoiceInput(spoken)
            },
            onPartialSpeech = { partial ->
                if (!JarvisRuntimeHub.isServiceRunning.value) {
                    JarvisRuntimeHub.setPartialTranscript(partial)
                }
            },
            onRmsChanged = { rms ->
                if (!JarvisRuntimeHub.isServiceRunning.value) {
                    JarvisRuntimeHub.setMicRmsLevel(rms)
                }
            },
            onVoiceStateChanged = { state ->
                if (!JarvisRuntimeHub.isServiceRunning.value) {
                    JarvisRuntimeHub.setVoiceState(state)
                }
            },
            onTtsReadyChanged = { ready, label ->
                JarvisRuntimeHub.setTtsStatus(ready, label)
            },
            onSpeechError = { msg, _ ->
                if (!JarvisRuntimeHub.isServiceRunning.value) {
                    JarvisRuntimeHub.setPartialTranscript("")
                    if (msg.isNotBlank()) {
                        JarvisRuntimeHub.appendMessage(AiMessage.Sender.SYSTEM, msg)
                    }
                }
            },
        )
    }

    fun refreshPermissionsAndTelemetry() {
        _hasMicPermission.value = checkMicPermission()
        _hasNotificationPermission.value = checkNotificationPermission()
        refreshTelemetry()
    }

    fun refreshTelemetry() {
        _telemetry.value = hardwareController.readTelemetry()
    }

    fun startListening() {
        refreshPermissionsAndTelemetry()
        if (!_hasMicPermission.value) {
            JarvisRuntimeHub.appendMessage(
                AiMessage.Sender.SYSTEM,
                "Boss, ভয়েস কমান্ড শুনতে মাইক্রোফোন অনুমতি প্রয়োজন। অনুগ্রহ করে অনুমতি প্রদান করুন।"
            )
            return
        }
        if (JarvisRuntimeHub.isServiceRunning.value) {
            JarvisForegroundService.start(
                appContext,
                JarvisForegroundService.ACTION_START_ACTIVE_COMMAND,
            )
        } else {
            foregroundVoiceEngine?.playActivationChime()
            foregroundVoiceEngine?.startListening(isWakeWordMode = false)
        }
    }

    fun stopListening() {
        if (JarvisRuntimeHub.isServiceRunning.value) {
            JarvisForegroundService.start(
                appContext,
                JarvisForegroundService.ACTION_STOP_LISTENING,
            )
        } else {
            foregroundVoiceEngine?.stopAll()
        }
        JarvisRuntimeHub.setPartialTranscript("")
    }

    fun toggleBackgroundService(enable: Boolean) {
        refreshPermissionsAndTelemetry()
        viewModelScope.launch {
            prefsRepository.setBackgroundServiceEnabled(enable)
        }
        if (enable) {
            if (!_hasMicPermission.value) {
                JarvisRuntimeHub.appendMessage(
                    AiMessage.Sender.SYSTEM,
                    "Boss, ব্যাকগ্রাউন্ড মাইক্রোফোন সার্ভিস চালু করার আগে মাইক্রোফোন অনুমতি দিন।"
                )
                return
            }
            foregroundVoiceEngine?.stopListening()
            JarvisForegroundService.start(
                appContext,
                JarvisForegroundService.ACTION_START_LISTENING,
            )
        } else {
            JarvisForegroundService.stop(appContext)
        }
    }

    fun submitTextCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return
        processCommand(trimmed, speakResponse = uiState.value.settings.voiceResponseEnabled)
    }

    private fun handleVoiceInput(spoken: String) {
        val hasWakeWord = BengaliCommandParser.containsWakeWord(spoken)
        if (hasWakeWord) {
            foregroundVoiceEngine?.playActivationChime()
        }
        processCommand(spoken, speakResponse = uiState.value.settings.voiceResponseEnabled)
    }

    private fun processCommand(rawInput: String, speakResponse: Boolean) {
        JarvisRuntimeHub.appendMessage(AiMessage.Sender.BOSS, rawInput)
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val settings = uiState.value.settings
                val outcome = commandExecutor.execute(
                    rawInput = rawInput,
                    settings = settings,
                    recentHistory = JarvisRuntimeHub.messages.value,
                )
                refreshTelemetry()

                outcome.requiresSecurityConfirmation?.let { pending ->
                    JarvisRuntimeHub.requestSecurityConfirmation(pending)
                }

                JarvisRuntimeHub.appendMessage(
                    sender = AiMessage.Sender.JARVIS,
                    text = outcome.replyText,
                    isPlaceholder = outcome.isPlaceholderAi,
                )

                val shouldListenNext = outcome.isWakeOnly && _hasMicPermission.value
                if (!JarvisRuntimeHub.isServiceRunning.value) {
                    foregroundVoiceEngine?.speak(
                        text = outcome.replyText,
                        enabled = speakResponse,
                        listenCommandAfter = shouldListenNext,
                        resumeWakeAfter = false,
                    )
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun resolveSecurityConfirmation(approved: Boolean) {
        val pending = JarvisRuntimeHub.pendingConfirmation.value ?: return
        JarvisRuntimeHub.requestSecurityConfirmation(null)
        val reply = commandExecutor.resolveSecurityConfirmation(pending, approved)
        JarvisRuntimeHub.appendMessage(AiMessage.Sender.JARVIS, reply)
        if (!JarvisRuntimeHub.isServiceRunning.value) {
            foregroundVoiceEngine?.speak(
                text = reply,
                enabled = uiState.value.settings.voiceResponseEnabled,
            )
        }
    }

    fun toggleFlashlightDirect() {
        val targetOn = !uiState.value.telemetry.isTorchOn
        val reply = hardwareController.setFlashlight(targetOn)
        refreshTelemetry()
        JarvisRuntimeHub.appendMessage(AiMessage.Sender.JARVIS, reply)
        if (!JarvisRuntimeHub.isServiceRunning.value) {
            foregroundVoiceEngine?.speak(
                text = reply,
                enabled = uiState.value.settings.voiceResponseEnabled,
            )
        }
    }

    fun openBatteryOptimizationSettings() {
        hardwareController.openBatteryOptimizationSettings()
    }

    fun openAppSystemSettings() {
        hardwareController.openAppDetailsSettings()
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setWakeWordEnabled(enabled) }
    }

    fun setVoiceResponseEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setVoiceResponseEnabled(enabled) }
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch { prefsRepository.setTtsSpeechRate(rate) }
    }

    fun setSecurityConfirmationRequired(required: Boolean) {
        viewModelScope.launch { prefsRepository.setSecurityConfirmationRequired(required) }
    }

    fun setAutoNotifyOnBoot(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setAutoNotifyOnBoot(enabled) }
    }

    fun clearConversation() {
        aiRepository.resetSession()
        JarvisRuntimeHub.clearConversation()
    }

    private fun checkMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onCleared() {
        foregroundVoiceEngine?.shutdown()
        hardwareController.release()
        super.onCleared()
    }

    private data class RuntimeSlice(
        val voiceState: VoiceState,
        val rms: Float,
        val partialTranscript: String,
        val serviceRunning: Boolean,
        val bgListening: Boolean,
    )

    private data class PermissionSlice(
        val micPerm: Boolean,
        val notifPerm: Boolean,
        val processing: Boolean,
        val ttsReady: Boolean,
        val ttsStatus: String,
    )
}
