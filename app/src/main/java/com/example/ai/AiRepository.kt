package com.example.ai

data class AiMessage(
    val id: String,
    val sender: Sender,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isPlaceholderResponse: Boolean = false,
) {
    enum class Sender {
        BOSS,
        JARVIS,
        SYSTEM,
    }
}

data class AiResponse(
    val text: String,
    val isPlaceholder: Boolean,
)

interface AiRepository {
    val isCloudAiConnected: Boolean
    val statusNotice: String

    suspend fun generateReply(
        userMessage: String,
        recentHistory: List<AiMessage>,
    ): AiResponse

    fun resetSession()
}
