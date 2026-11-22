package com.example.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

enum class VoiceState {
    IDLE,
    WAKE_WORD_LISTENING,
    ACTIVE_COMMAND_LISTENING,
    SPEAKING,
}

class VoiceEngine(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onPartialSpeech: (String) -> Unit,
    onRmsChanged: (Float) -> Unit,
    private val onVoiceStateChanged: (VoiceState) -> Unit,
    private val onTtsReadyChanged: (Boolean, String) -> Unit,
    private val onSpeechError: (String, Boolean) -> Unit,
) : TextToSpeech.OnInitListener {

    private val emitRmsLevel: (Float) -> Unit = onRmsChanged

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var currentVoiceState = VoiceState.IDLE
    private var continueWakeLoopAfterTts = false
    private var pendingCommandListenAfterTts = false
    private var currentSpeechRate = 1.0f

    init {
        mainHandler.post {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val bnBd = Locale.Builder().setLanguage("bn").setRegion("BD").build()
            val bnIn = Locale.Builder().setLanguage("bn").setRegion("IN").build()

            val result = when {
                engine.isLanguageAvailable(bnBd) >= TextToSpeech.LANG_AVAILABLE -> {
                    engine.language = bnBd
                    "বাংলা (BD) ভয়েস সক্রিয়"
                }
                engine.isLanguageAvailable(bnIn) >= TextToSpeech.LANG_AVAILABLE -> {
                    engine.language = bnIn
                    "বাংলা (IN) ভয়েস সক্রিয়"
                }
                else -> {
                    engine.language = Locale.getDefault()
                    "ডিফল্ট সিস্টেম ভয়েস (বাংলা TTS প্যাক অনুপস্থিত)"
                }
            }

            engine.setSpeechRate(currentSpeechRate)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post {
                        setState(VoiceState.SPEAKING)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        handleTtsCompleted()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        handleTtsCompleted()
                    }
                }
            })

            isTtsReady = true
            onTtsReadyChanged(true, result)
        } else {
            isTtsReady = false
            onTtsReadyChanged(false, "TextToSpeech চালু করা যায়নি")
        }
    }

    fun updateSpeechRate(rate: Float) {
        currentSpeechRate = rate.coerceIn(0.6f, 1.5f)
        tts?.setSpeechRate(currentSpeechRate)
    }

    fun playActivationChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 190)
            mainHandler.postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {
                }
            }, 260)
        } catch (e: Exception) {
            Log.w(TAG, "Activation tone skipped", e)
        }
    }

    fun speak(
        text: String,
        enabled: Boolean,
        listenCommandAfter: Boolean = false,
        resumeWakeAfter: Boolean = false,
    ) {
        pendingCommandListenAfterTts = listenCommandAfter
        continueWakeLoopAfterTts = resumeWakeAfter
        if (!enabled || !isTtsReady || tts == null) {
            handleTtsCompleted()
            return
        }
        mainHandler.post {
            stopRecognizerInternal()
            val utteranceId = UUID.randomUUID().toString()
            setState(VoiceState.SPEAKING)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun handleTtsCompleted() {
        when {
            pendingCommandListenAfterTts -> {
                pendingCommandListenAfterTts = false
                startListening(isWakeWordMode = false)
            }
            continueWakeLoopAfterTts -> {
                startListening(isWakeWordMode = true)
            }
            else -> setState(VoiceState.IDLE)
        }
    }

    fun startListening(isWakeWordMode: Boolean) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                setState(VoiceState.IDLE)
                onSpeechError("Boss, এই ডিভাইসে স্পিচ রিকগনিশন সার্ভিস পাওয়া যায়নি।", true)
                return@post
            }
            continueWakeLoopAfterTts = isWakeWordMode
            stopRecognizerInternal()

            val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    setState(
                        if (isWakeWordMode) VoiceState.WAKE_WORD_LISTENING
                        else VoiceState.ACTIVE_COMMAND_LISTENING
                    )
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = if (rmsdB.isNaN()) {
                        0.05f
                    } else {
                        ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                    }
                    emitRmsLevel(normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    emitRmsLevel(0.08f)
                }

                override fun onError(error: Int) {
                    emitRmsLevel(0f)
                    val isFatal = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Boss, মাইক্রোফোন অনুমতি প্রয়োজন।"
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ""
                        SpeechRecognizer.ERROR_AUDIO -> "অডিও ইনপুট ত্রুটি।"
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্ক স্পিচ ত্রুটি।"
                        else -> "ভয়েস শনাক্তকরণে সাময়িক সমস্যা ($error)।"
                    }
                    if (!isWakeWordMode) {
                        setState(VoiceState.IDLE)
                    }
                    onSpeechError(msg, isFatal)
                }

                override fun onResults(results: Bundle?) {
                    emitRmsLevel(0f)
                    val matches =
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull().orEmpty().trim()
                    if (!isWakeWordMode) {
                        setState(VoiceState.IDLE)
                    }
                    if (best.isNotBlank()) {
                        onSpeechResult(best)
                    } else if (isWakeWordMode) {
                        onSpeechError("", false)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        onPartialSpeech(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            try {
                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "SpeechRecognizer.startListening failed", e)
                setState(VoiceState.IDLE)
                onSpeechError("Boss, মাইক্রোফোন চালু করা যায়নি।", true)
            }
        }
    }

    fun stopListening() {
        continueWakeLoopAfterTts = false
        pendingCommandListenAfterTts = false
        mainHandler.post {
            stopRecognizerInternal()
            emitRmsLevel(0f)
            if (currentVoiceState != VoiceState.SPEAKING) {
                setState(VoiceState.IDLE)
            }
        }
    }

    fun stopAll() {
        continueWakeLoopAfterTts = false
        pendingCommandListenAfterTts = false
        mainHandler.post {
            stopRecognizerInternal()
            tts?.stop()
            emitRmsLevel(0f)
            setState(VoiceState.IDLE)
        }
    }

    private fun stopRecognizerInternal() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    private fun setState(state: VoiceState) {
        currentVoiceState = state
        onVoiceStateChanged(state)
    }

    fun shutdown() {
        stopAll()
        mainHandler.post {
            tts?.shutdown()
            tts = null
        }
    }

    companion object {
        private const val TAG = "VoiceEngine"
    }
}
