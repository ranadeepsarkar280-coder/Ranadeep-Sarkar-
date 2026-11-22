package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jarvis_preferences"
)

data class JarvisSettings(
    val wakeWordEnabled: Boolean = true,
    val voiceResponseEnabled: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val securityConfirmationRequired: Boolean = true,
    val backgroundServiceEnabled: Boolean = false,
    val autoNotifyOnBoot: Boolean = false,
)

class JarvisPreferencesRepository(private val context: Context) {

    private object Keys {
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val VOICE_RESPONSE_ENABLED = booleanPreferencesKey("voice_response_enabled")
        val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val SECURITY_CONFIRMATION = booleanPreferencesKey("security_confirmation")
        val BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("background_service_enabled")
        val AUTO_NOTIFY_ON_BOOT = booleanPreferencesKey("auto_notify_on_boot")
    }

    val settingsFlow: Flow<JarvisSettings> = context.jarvisDataStore.data.map { prefs ->
        JarvisSettings(
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: true,
            voiceResponseEnabled = prefs[Keys.VOICE_RESPONSE_ENABLED] ?: true,
            ttsSpeechRate = (prefs[Keys.TTS_SPEECH_RATE] ?: 1.0f).coerceIn(0.6f, 1.5f),
            securityConfirmationRequired = prefs[Keys.SECURITY_CONFIRMATION] ?: true,
            backgroundServiceEnabled = prefs[Keys.BACKGROUND_SERVICE_ENABLED] ?: false,
            autoNotifyOnBoot = prefs[Keys.AUTO_NOTIFY_ON_BOOT] ?: false,
        )
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setVoiceResponseEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.VOICE_RESPONSE_ENABLED] = enabled }
    }

    suspend fun setTtsSpeechRate(rate: Float) {
        context.jarvisDataStore.edit { it[Keys.TTS_SPEECH_RATE] = rate.coerceIn(0.6f, 1.5f) }
    }

    suspend fun setSecurityConfirmationRequired(required: Boolean) {
        context.jarvisDataStore.edit { it[Keys.SECURITY_CONFIRMATION] = required }
    }

    suspend fun setBackgroundServiceEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.BACKGROUND_SERVICE_ENABLED] = enabled }
    }

    suspend fun setAutoNotifyOnBoot(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.AUTO_NOTIFY_ON_BOOT] = enabled }
    }
}
