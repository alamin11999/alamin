package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class VoiceController(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit,
    private val onListeningStatusChanged: (Boolean) -> Unit,
    private val onRmsDbChanged: (Float) -> Unit,
    private val onErrorOccurred: ((String) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private var currentGender = "male"

    init {
        // Initialize Text-to-Speech
        tts = TextToSpeech(context, this)
        
        // Initialize Speech Recognizer
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            } else {
                Log.e("VoiceController", "Speech recognition not available on this device.")
            }
        } catch (e: Exception) {
            Log.e("VoiceController", "Failed to init SpeechRecognizer", e)
        }
    }

    private var pendingSpeech: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            applyVoiceSettings()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            Log.d("VoiceController", "TTS Initialized successfully with gender: $currentGender.")
            
            // Speak queued pending speech if there was any requested before init
            pendingSpeech?.let {
                speak(it)
                pendingSpeech = null
            }
        } else {
            Log.e("VoiceController", "TTS Initialization failed.")
        }
    }

    fun setVoiceGender(gender: String) {
        currentGender = gender.lowercase()
        applyVoiceSettings()
    }

    private fun applyVoiceSettings() {
        if (!isTtsInitialized) return
        val ttsInstance = tts ?: return
        try {
            // Set locale to US standard English for custom assistant voice matching
            ttsInstance.language = Locale.US
            
            if (currentGender == "female") {
                // Female (Friday): Crystal clear, natural, elegant conversational tone
                ttsInstance.setPitch(1.02f) // Comfortable natural female pitch
                ttsInstance.setSpeechRate(1.02f) // Natural conversational speech rate
                
                val voices = ttsInstance.voices
                if (!voices.isNullOrEmpty()) {
                    // Filter system voices for premium English female variants
                    val femaleVoice = voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        val isEnglish = voice.locale?.language == "en"
                        isEnglish && (name.contains("female") || name.contains("-f-") || name.contains("sfg") || name.contains("tpf") || name.contains("b-local")) && 
                        (name.contains("natural") || name.contains("neural") || name.contains("premium") || name.contains("google") || name.contains("en-us-x"))
                    } ?: voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        val isEnglish = voice.locale?.language == "en"
                        isEnglish && (name.contains("female") || name.contains("-f-") || name.contains("sfg") || name.contains("tpf") || name.contains("b-local")) && !voice.isNetworkConnectionRequired
                    } ?: voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        val isEnglish = voice.locale?.language == "en"
                        isEnglish && (name.contains("female") || name.contains("-f-") || name.contains("sfg") || name.contains("tpf"))
                    }
                    if (femaleVoice != null) {
                        ttsInstance.voice = femaleVoice
                        Log.d("VoiceController", "Selected realistic system female voice: ${femaleVoice.name}")
                    }
                }
            } else {
                // Male (Jarvis): Smooth, deep-tech cinematic masculine tone
                ttsInstance.setPitch(0.96f) // Smooth natural male pitch (slight baritone)
                ttsInstance.setSpeechRate(1.01f) // Cinematic comfortable conversational rate
                
                val voices = ttsInstance.voices
                if (!voices.isNullOrEmpty()) {
                    // Filter system voices for premium English male variants
                    val maleVoice = voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        val isEnglish = voice.locale?.language == "en"
                        isEnglish && (name.contains("male") || name.contains("-m-") || name.contains("iom") || name.contains("col") || name.contains("a-local")) && 
                        (name.contains("natural") || name.contains("neural") || name.contains("premium") || name.contains("google") || name.contains("en-us-x"))
                    } ?: voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        val isEnglish = voice.locale?.language == "en"
                        isEnglish && (name.contains("male") || name.contains("-m-") || name.contains("iom") || name.contains("col") || name.contains("a-local")) && !voice.isNetworkConnectionRequired
                    } ?: voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        val isEnglish = voice.locale?.language == "en"
                        isEnglish && (name.contains("male") || name.contains("-m-") || name.contains("iom") || name.contains("col"))
                    }
                    if (maleVoice != null) {
                        ttsInstance.voice = maleVoice
                        Log.d("VoiceController", "Selected realistic system male voice: ${maleVoice.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceController", "Error applying voice settings: ${e.localizedMessage}")
        }
    }

    fun setLanguage(isBangla: Boolean) {
        if (!isTtsInitialized) return
        val ttsInstance = tts ?: return
        try {
            if (isBangla) {
                ttsInstance.language = Locale("bn", "BD")
                val voices = ttsInstance.voices
                if (!voices.isNullOrEmpty()) {
                    val bnVoice = voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        val isBengali = voice.locale?.language == "bn"
                        isBengali && (name.contains("natural") || name.contains("neural") || name.contains("premium") || name.contains("google") || name.contains("bn-bd-x"))
                    } ?: voices.firstOrNull { voice ->
                        voice.locale?.language == "bn"
                    }
                    if (bnVoice != null) {
                        ttsInstance.voice = bnVoice
                        Log.d("VoiceController", "Selected realistic system Bangla voice: ${bnVoice.name}")
                    }
                }
                ttsInstance.setPitch(1.0f)
                ttsInstance.setSpeechRate(1.0f)
            } else {
                ttsInstance.language = Locale.US
                applyVoiceSettings()
            }
        } catch (e: Exception) {
            Log.e("VoiceController", "Error setting language", e)
        }
    }

    fun speak(text: String) {
        if (!isTtsInitialized) {
            Log.w("VoiceController", "TTS not ready yet. Queued pending speech: $text")
            pendingSpeech = text
            return
        }
        
        // Stop listening before speaking to prevent feedback loops
        stopListening()
        
        _isSpeaking.value = true
        val utteranceId = UUID.randomUUID().toString()
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        Log.d("VoiceController", "TTS speak initialized. Code: $result for text: $text")
    }

    fun startListening(isBangla: Boolean) {
        if (speechRecognizer == null) {
            Log.e("VoiceController", "Recognizer is null.")
            return
        }
        
        // Stop active speech before listening
        stopSpeaking()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            if (isBangla) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "bn-BD")
            } else {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            }
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            onListeningStatusChanged(true)
            Log.d("VoiceController", "Speech recognition started.")
        } catch (e: Exception) {
            Log.e("VoiceController", "Failed to start listening", e)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            onListeningStatusChanged(false)
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun stopSpeaking() {
        if (isTtsInitialized) {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignored
        }
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VoiceController", "Ready for speech.")
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
            onListeningStatusChanged(true)
        }

        override fun onRmsChanged(rmsdB: Float) {
            onRmsDbChanged(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            onListeningStatusChanged(false)
        }

        override fun onError(error: Int) {
            _isListening.value = false
            onListeningStatusChanged(false)
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions missing"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Speech recognizer error"
            }
            Log.e("VoiceController", "Error: $error ($message)")
            onErrorOccurred?.invoke(message)
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            onListeningStatusChanged(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                Log.d("VoiceController", "Recognized result: $recognizedText")
                onTextRecognized(recognizedText)
            } else {
                Log.w("VoiceController", "Empty recognition matches.")
                onErrorOccurred?.invoke("No speech recognized")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                Log.d("VoiceController", "Partial result: ${matches[0]}")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
