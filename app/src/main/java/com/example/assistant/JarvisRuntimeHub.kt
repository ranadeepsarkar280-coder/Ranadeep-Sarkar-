package com.example.assistant

import com.example.ai.AiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class PendingSecurityConfirmation(
    val id: String = UUID.randomUUID().toString(),
    val actionType: SensitiveActionType,
    val rawCommand: String,
    val promptBn: String,
)

object JarvisRuntimeHub {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isBackgroundListening = MutableStateFlow(false)
    val isBackgroundListening: StateFlow<Boolean> = _isBackgroundListening.asStateFlow()

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _micRmsLevel = MutableStateFlow(0f)
    val micRmsLevel: StateFlow<Float> = _micRmsLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _ttsStatus = MutableStateFlow("বাংলা TTS প্রস্তুত হচ্ছে…")
    val ttsStatus: StateFlow<String> = _ttsStatus.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingSecurityConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingSecurityConfirmation?> =
        _pendingConfirmation.asStateFlow()

    private val _messages = MutableStateFlow(
        listOf(
            AiMessage(
                id = "welcome_boss",
                sender = AiMessage.Sender.JARVIS,
                text = "নমস্কার Boss। আমি জার্ভিস, আপনার ব্যক্তিগত এআই সহকারী। শুনুন (Start) বাটনে চাপুন অথবা \"Hey JARVIS\" / \"হে জার্ভিস\" বলুন।",
            )
        )
    )
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    fun appendMessage(
        sender: AiMessage.Sender,
        text: String,
        isPlaceholder: Boolean = false,
    ) {
        val msg = AiMessage(
            id = UUID.randomUUID().toString(),
            sender = sender,
            text = text,
            isPlaceholderResponse = isPlaceholder,
        )
        _messages.update { current -> (current + msg).takeLast(60) }
    }

    fun clearConversation() {
        _messages.value = listOf(
            AiMessage(
                id = UUID.randomUUID().toString(),
                sender = AiMessage.Sender.JARVIS,
                text = "কথোপকথনের ইতিহাস পরিষ্কার করা হয়েছে, Boss। নতুন নির্দেশের জন্য প্রস্তুত।",
            )
        )
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
        if (!running) {
            _isBackgroundListening.value = false
            _voiceState.value = VoiceState.IDLE
            _micRmsLevel.value = 0f
        }
    }

    fun setBackgroundListening(listening: Boolean) {
        _isBackgroundListening.value = listening
    }

    fun setVoiceState(state: VoiceState) {
        _voiceState.value = state
    }

    fun setMicRmsLevel(level: Float) {
        _micRmsLevel.value = level
    }

    fun setPartialTranscript(text: String) {
        _partialTranscript.value = text
    }

    fun setTtsStatus(ready: Boolean, label: String) {
        _isTtsReady.value = ready
        _ttsStatus.value = label
    }

    fun requestSecurityConfirmation(confirmation: PendingSecurityConfirmation?) {
        _pendingConfirmation.value = confirmation
    }
}
