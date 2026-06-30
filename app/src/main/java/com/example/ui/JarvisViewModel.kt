package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiService
import com.example.data.db.AppAliasEntity
import com.example.data.db.ConversationEntity
import com.example.data.db.MemoryEntity
import com.example.data.db.JarvisDatabase
import com.example.data.repository.JarvisRepository
import com.example.util.AppControlManager
import com.example.util.JarvisActionProcessor
import com.example.util.VoiceController
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val repository = JarvisRepository(db)
    
    private val appControlManager = AppControlManager(application, repository)
    private val aiService = AiService(repository)

    sealed class UiEvent {
        data class Navigate(val route: String) : UiEvent()
        object NavigateBack : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val actionProcessor = JarvisActionProcessor(
        application,
        repository,
        appControlManager,
        onNavigationRequested = { route ->
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.Navigate(route))
            }
        }
    )

    // Voice Engine
    private var voiceController: VoiceController? = null

    // UI States
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb

    private val _userName = MutableStateFlow("Sir")
    val userName: StateFlow<String> = _userName

    private val _isFirstLaunch = MutableStateFlow(false)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch

    // Database Streams
    val conversationHistory: StateFlow<List<ConversationEntity>> = repository.conversationHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appAliases: StateFlow<List<AppAliasEntity>> = repository.allAppAliases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Settings Streams
    private val _activeProvider = MutableStateFlow("gemini")
    val activeProvider: StateFlow<String> = _activeProvider

    private val _activeModel = MutableStateFlow("gemini-3.5-flash")
    val activeModel: StateFlow<String> = _activeModel

    private val _isBangla = MutableStateFlow(false)
    val isBangla: StateFlow<Boolean> = _isBangla

    private val _voiceGender = MutableStateFlow("male")
    val voiceGender: StateFlow<String> = _voiceGender

    private val _testStatus = MutableStateFlow<Map<String, String>>(emptyMap()) // Provider to Status String
    val testStatus: StateFlow<Map<String, String>> = _testStatus

    init {
        // Load initial settings
        viewModelScope.launch {
            _userName.value = repository.getSetting("user_name", "")
            if (_userName.value.isEmpty()) {
                _isFirstLaunch.value = true
                _userName.value = "Sir"
            } else {
                _isFirstLaunch.value = false
            }

            _activeProvider.value = repository.getSetting("ai_provider", "gemini")
            _activeModel.value = repository.getSetting("ai_model", "gemini-3.5-flash")
            _isBangla.value = repository.getSetting("is_bangla", "false").toBoolean()
            _voiceGender.value = repository.getSetting("voice_gender", "male")

            // Update voice engine configuration
            voiceController?.setVoiceGender(_voiceGender.value)

            // Scan device apps and synchronize database
            appControlManager.syncInstalledApps()
        }

        // Initialize voice engine
        voiceController = VoiceController(
            context = application,
            onTextRecognized = { speech -> handleVoiceInput(speech) },
            onListeningStatusChanged = { listening -> _isListening.value = listening },
            onRmsDbChanged = { db -> _rmsDb.value = db },
            onErrorOccurred = { errorMsg ->
                viewModelScope.launch {
                    val fallbackMsg = if (_isBangla.value) {
                        when {
                            errorMsg.contains("Permissions") -> "অনুগ্রহ করে রেকর্ডিং পারমিশন দিন, স্যার।"
                            errorMsg.contains("Network") -> "নেটওয়ার্ক সংযোগ ত্রুটি, স্যার।"
                            else -> "দুঃখিত স্যার, আমি আপনার কথা শুনতে পাইনি। দয়া করে আবার বলুন।"
                        }
                    } else {
                        when {
                            errorMsg.contains("Permissions") -> "Permissions required to process voice commands, Sir."
                            errorMsg.contains("Network") -> "Network communication error. Check your link, Sir."
                            else -> "Sir, I detected a communication error. Please repeat your command."
                        }
                    }
                    repository.addMessage("jarvis", fallbackMsg)
                    voiceController?.speak(fallbackMsg)
                }
            }
        )

        // Sync voice controller speaking state
        viewModelScope.launch {
            voiceController?.isSpeaking?.collect { speaking ->
                _isSpeaking.value = speaking
            }
        }
    }

    fun completeFirstLaunch(name: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val finalName = if (trimmedName.isEmpty()) "Sir" else trimmedName
            repository.saveSetting("user_name", finalName)
            _userName.value = finalName
            _isFirstLaunch.value = false
            repository.addMemory("User's name is $finalName", "profile")

            val welcomeSpeech = "Greetings $finalName. I am JARVIS, your native neural assistant. Core systems are fully online and synchronized."
            voiceController?.setLanguage(false)
            voiceController?.speak(welcomeSpeech)
            repository.addMessage("jarvis", welcomeSpeech)
        }
    }

    fun toggleListening() {
        if (_isListening.value) {
            voiceController?.stopListening()
        } else {
            voiceController?.setLanguage(_isBangla.value)
            voiceController?.startListening(_isBangla.value)
        }
    }

    private fun handleVoiceInput(speech: String) {
        viewModelScope.launch {
            // Log user message
            repository.addMessage("user", speech)

            // Step 1: Process local actions (time, weather, battery, open apps, etc.)
            val localResult = actionProcessor.processVoiceCommand(speech)
            if (localResult != null) {
                val (responseMsg, success) = localResult
                repository.addMessage("jarvis", responseMsg)
                voiceController?.setLanguage(_isBangla.value)
                voiceController?.speak(responseMsg)
                return@launch
            }

            // Step 2: Fallback to generative AI model configured
            val waitingSpeech = if (_isBangla.value) {
                "দয়া করে অপেক্ষা করুন স্যার, আমি হিসাব করছি।"
            } else {
                "Processing prompt, Sir. Querying global intelligence net."
            }
            voiceController?.setLanguage(_isBangla.value)
            voiceController?.speak(waitingSpeech)

            val aiResponse = aiService.generateSpeechResponse(speech)
            repository.addMessage("jarvis", aiResponse)
            voiceController?.speak(aiResponse)
        }
    }

    fun sendTextMessage(message: String) {
        if (message.trim().isEmpty()) return
        handleVoiceInput(message)
    }

    fun speak(text: String) {
        voiceController?.setLanguage(_isBangla.value)
        voiceController?.speak(text)
    }

    // --- Settings controls ---
    fun updateAiProvider(provider: String) {
        _activeProvider.value = provider
        viewModelScope.launch {
            repository.saveSetting("ai_provider", provider)
            // Auto update default model based on provider
            val defaultModel = when (provider.lowercase()) {
                "openai" -> "gpt-4o-mini"
                "claude" -> "claude-3-5-sonnet-20241022"
                "deepseek" -> "deepseek-chat"
                else -> "gemini-3.5-flash"
            }
            updateAiModel(defaultModel)
        }
    }

    fun updateAiModel(model: String) {
        _activeModel.value = model
        viewModelScope.launch {
            repository.saveSetting("ai_model", model)
        }
    }

    fun updateLanguage(bangla: Boolean) {
        _isBangla.value = bangla
        viewModelScope.launch {
            repository.saveSetting("is_bangla", bangla.toString())
            voiceController?.setLanguage(bangla)
        }
    }

    fun updateVoiceGender(gender: String) {
        _voiceGender.value = gender.lowercase()
        viewModelScope.launch {
            repository.saveSetting("voice_gender", gender.lowercase())
            voiceController?.setVoiceGender(gender)
        }
    }

    fun saveApiKey(provider: String, key: String) {
        val trimmedKey = key.trim()
        viewModelScope.launch {
            repository.saveSetting("api_key_${provider.lowercase()}", trimmedKey)
        }
    }

    suspend fun getApiKey(provider: String): String {
        return repository.getSetting("api_key_${provider.lowercase()}", "")
    }

    fun saveCustomUrl(url: String) {
        viewModelScope.launch {
            repository.saveSetting("custom_api_url", url)
        }
    }

    suspend fun getCustomUrl(): String {
        return repository.getSetting("custom_api_url", "")
    }

    fun testConnection(provider: String) {
        viewModelScope.launch {
            _testStatus.update { it + (provider to "Testing link...") }
            val success = aiService.testConnection(provider)
            _testStatus.update {
                it + (provider to if (success) "ONLINE (Active connection)" else "LINK FAILURE (Check key/endpoint)")
            }
        }
    }

    // --- App Alias Management ---
    fun updateAppAlias(packageName: String, alias: String) {
        viewModelScope.launch {
            repository.updateAlias(packageName, alias)
        }
    }

    fun toggleAppFavorite(packageName: String, currentFav: Boolean) {
        viewModelScope.launch {
            repository.setAppFavorite(packageName, !currentFav)
        }
    }

    fun toggleAppEnabled(packageName: String, currentEnabled: Boolean) {
        viewModelScope.launch {
            repository.setAppEnabled(packageName, !currentEnabled)
        }
    }

    fun purgeHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun purgeMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
            // Reset profile name too
            repository.saveSetting("user_name", "")
            _userName.value = "Sir"
            _isFirstLaunch.value = true
        }
    }

    fun deleteMemory(id: Int) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceController?.destroy()
    }
}
