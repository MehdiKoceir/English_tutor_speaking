package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class TutorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TutorDatabase.getDatabase(application)
    private val repository = TutorRepository(database.tutorDao())
    val prefs = PreferencesHelper(application)
    private val apiService = TutorApiService(prefs)
    val chatMemoryManager = ChatMemoryManager()

    // --- State Expose ---
    val allSessions: StateFlow<List<ConversationSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSession = MutableStateFlow<ConversationSession?>(null)
    val currentSession: StateFlow<ConversationSession?> = _currentSession.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()

    private val _isTutorThinking = MutableStateFlow(false)
    val isTutorThinking: StateFlow<Boolean> = _isTutorThinking.asStateFlow()

    // Preferences states
    private val _useGeminiDirect = MutableStateFlow(prefs.useGeminiDirect)
    val useGeminiDirect: StateFlow<Boolean> = _useGeminiDirect.asStateFlow()

    private val _ollamaUrl = MutableStateFlow(prefs.ollamaUrl)
    val ollamaUrl: StateFlow<String> = _ollamaUrl.asStateFlow()

    private val _ollamaApiKey = MutableStateFlow(prefs.ollamaApiKey)
    val ollamaApiKey: StateFlow<String> = _ollamaApiKey.asStateFlow()

    private val _ollamaModel = MutableStateFlow(prefs.ollamaModel)
    val ollamaModel: StateFlow<String> = _ollamaModel.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(prefs.ttsEnabled)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _ttsRate = MutableStateFlow(prefs.ttsRate)
    val ttsRate: StateFlow<Float> = _ttsRate.asStateFlow()

    private val _ttsPitch = MutableStateFlow(prefs.ttsPitch)
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    private val _ttsLocale = MutableStateFlow(prefs.ttsLocale)
    val ttsLocale: StateFlow<String> = _ttsLocale.asStateFlow()

    private val _ttsVoiceName = MutableStateFlow(prefs.ttsVoiceName)
    val ttsVoiceName: StateFlow<String?> = _ttsVoiceName.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
    val availableVoices: StateFlow<List<String>> = _availableVoices.asStateFlow()

    private val _correctionsEnabled = MutableStateFlow(prefs.correctionsEnabled)
    val correctionsEnabled: StateFlow<Boolean> = _correctionsEnabled.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.geminiApiKey)
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _useDemoMode = MutableStateFlow(prefs.useDemoMode)
    val useDemoMode: StateFlow<Boolean> = _useDemoMode.asStateFlow()

    private val _useWebSpeechAPI = MutableStateFlow(prefs.useWebSpeechAPI)
    val useWebSpeechAPI: StateFlow<Boolean> = _useWebSpeechAPI.asStateFlow()

    // --- AI Agent Studio States ---
    private val _agentConfig = MutableStateFlow(AgentConfig())
    val agentConfig: StateFlow<AgentConfig> = _agentConfig.asStateFlow()

    val allAgentMemories: StateFlow<List<AgentMemory>> = repository.allAgentMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrainingLogs: StateFlow<List<AgentTrainingLog>> = repository.allTrainingLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isTrainingAgent = MutableStateFlow(false)
    val isTrainingAgent: StateFlow<Boolean> = _isTrainingAgent.asStateFlow()

    private val _trainingProgressMessage = MutableStateFlow("")
    val trainingProgressMessage: StateFlow<String> = _trainingProgressMessage.asStateFlow()

    private val _selectedAttentionSentence = MutableStateFlow("I love practicing conversational English with my friendly AI teacher.")
    val selectedAttentionSentence: StateFlow<String> = _selectedAttentionSentence.asStateFlow()

    private val _attentionMatrix = MutableStateFlow<List<List<Float>>>(emptyList())
    val attentionMatrix: StateFlow<List<List<Float>>> = _attentionMatrix.asStateFlow()

    // --- Vocabulary & Daily practice tracking states ---
    val allVocabularyWords: StateFlow<List<VocabularyWord>> = repository.allVocabularyWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dailyPracticedSeconds = MutableStateFlow(prefs.dailyPracticedSeconds)
    val dailyPracticedSeconds: StateFlow<Int> = _dailyPracticedSeconds.asStateFlow()

    private val _dailyPracticeGoalMinutes = MutableStateFlow(prefs.dailyPracticeGoalMinutes)
    val dailyPracticeGoalMinutes: StateFlow<Int> = _dailyPracticeGoalMinutes.asStateFlow()

    private val _dailyStreak = MutableStateFlow<DailyStreakRecord?>(null)
    val dailyStreak: StateFlow<DailyStreakRecord?> = _dailyStreak.asStateFlow()

    init {
        checkAndUpdateStreakOnStartup()
        loadSavedSessionOnStartup()
        loadAgentConfig()
        computeAttentionMatrix(_selectedAttentionSentence.value)
    }

    private fun loadAgentConfig() {
        viewModelScope.launch {
            _agentConfig.value = repository.getAgentConfig()
        }
    }

    private fun loadSavedSessionOnStartup() {
        viewModelScope.launch {
            val savedSessionId = prefs.currentSessionId
            if (savedSessionId != null) {
                val session = repository.getSessionById(savedSessionId)
                if (session != null) {
                    loadSession(session)
                }
            }
        }
    }

    private fun checkAndUpdateStreakOnStartup() {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val streak = repository.getDailyStreak() ?: DailyStreakRecord(currentStreak = 0, lastCompletedDate = "", longestStreak = 0)
            
            if (streak.lastCompletedDate.isNotEmpty() && streak.lastCompletedDate != today) {
                try {
                    val lastDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(streak.lastCompletedDate)
                    val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(today)
                    if (lastDate != null && todayDate != null) {
                        val diffMs = todayDate.time - lastDate.time
                        val diffDays = diffMs / (1000 * 60 * 60 * 24)
                        if (diffDays > 1) {
                            val updatedStreak = streak.copy(currentStreak = 0)
                            repository.insertDailyStreak(updatedStreak)
                            _dailyStreak.value = updatedStreak
                        } else {
                            _dailyStreak.value = streak
                        }
                    } else {
                        _dailyStreak.value = streak
                    }
                } catch (e: Exception) {
                    _dailyStreak.value = streak
                }
            } else {
                _dailyStreak.value = streak
            }
        }
    }

    private fun checkAndIncrementStreak() {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val streak = repository.getDailyStreak() ?: DailyStreakRecord(currentStreak = 0, lastCompletedDate = "", longestStreak = 0)
            
            if (streak.lastCompletedDate == today) {
                return@launch
            }
            
            val newCurrentStreak: Int
            if (streak.lastCompletedDate.isEmpty()) {
                newCurrentStreak = 1
            } else {
                var diffDays = 1L
                try {
                    val lastDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(streak.lastCompletedDate)
                    val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(today)
                    if (lastDate != null && todayDate != null) {
                        val diffMs = todayDate.time - lastDate.time
                        diffDays = diffMs / (1000 * 60 * 60 * 24)
                    }
                } catch (e: Exception) {
                    // ignore
                }
                
                newCurrentStreak = if (diffDays <= 1) {
                    streak.currentStreak + 1
                } else {
                    1
                }
            }
            
            val newLongestStreak = maxOf(streak.longestStreak, newCurrentStreak)
            val updatedStreak = DailyStreakRecord(
                currentStreak = newCurrentStreak,
                lastCompletedDate = today,
                longestStreak = newLongestStreak
            )
            repository.insertDailyStreak(updatedStreak)
            _dailyStreak.value = updatedStreak
        }
    }

    private var messageCollectorJob: kotlinx.coroutines.Job? = null

    fun startNewSession(level: String, topic: String) {
        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            val session = ConversationSession(
                id = sessionId,
                level = level,
                topic = topic,
                createdAt = System.currentTimeMillis()
            )
            repository.insertSession(session)
            loadSession(session)

            // Let tutor start the conversation with an opening prompt!
            val introText = getIntroText(level, topic)
            val introMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                sender = "tutor",
                text = introText,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(introMsg)
        }
    }

    private fun getIntroText(level: String, topic: String): String {
        return when (level.lowercase()) {
            "beginner" -> when (topic.lowercase()) {
                "job interview" -> "Hello! Welcome to our job interview practice. Can you tell me your name and what job you want?"
                "travel" -> "Hi there! Let's practice travel English. Where do you want to go for your holiday?"
                "business" -> "Hello! Let's practice business English. Can you tell me about your job?"
                "daily life" -> "Hi! Let's talk about our daily life. What do you like to do in the morning?"
                else -> "Hi! I am your English tutor. How are you today? Let's have a simple conversation!"
            }
            "elementary" -> when (topic.lowercase()) {
                "job interview" -> "Hi! I am glad to meet you. Can you introduce yourself and tell me what you like about your job?"
                "travel" -> "Hello! Let's talk about traveling. What is your favorite place that you visited before?"
                "business" -> "Hi there! Let's practice business words. What do you do at work every day?"
                "daily life" -> "Hi! Let's discuss routines. What do you usually do on weekends to relax?"
                else -> "Hello! I am your English tutor. What is your favorite hobby? Tell me a little about it!"
            }
            "intermediate" -> when (topic.lowercase()) {
                "job interview" -> "Hello! Let's begin our mock interview. Tell me about yourself and why you're interested in this position."
                "travel" -> "Hi! Travel is always exciting. If you could visit any country in the world tomorrow, where would you go and why?"
                "business" -> "Welcome! Let's practice professional communication. How do you usually handle difficult projects or tight deadlines at work?"
                "daily life" -> "Hey! Let's catch up. How was your day? Did you do anything interesting or productive today?"
                else -> "Hello! I'm happy to practice English with you today. What is a topic you find interesting that you'd like to talk about?"
            }
            "upper intermediate", "upper_intermediate" -> when (topic.lowercase()) {
                "job interview" -> "Welcome to our interview prep session. Could you describe your current responsibilities and share an accomplishment you are particularly proud of?"
                "travel" -> "Hi there! Let's dive into some travel scenarios. In your experience, what are the main benefits and challenges of traveling solo versus traveling in a group?"
                "business" -> "Greetings. Let's talk professional networking. What strategies do you believe are most effective for building long-term business partnerships?"
                "daily life" -> "Hello! Let's explore daily life and lifestyle. How do you manage your time effectively to balance work commitments and personal hobbies?"
                else -> "Hello! It's great to connect. Let's discuss a current trend or topic. What is an area of technology, art, or society that you've been following closely?"
            }
            else -> when (topic.lowercase()) {
                "job interview" -> "Good morning. Let's dive straight into our professional assessment. Walk me through your professional background and highlight a major challenge you resolved successfully."
                "travel" -> "Hello! Let's discuss travel and exploration. In your opinion, how does immersive travel contribute to cross-cultural understanding and personal development?"
                "business" -> "Greetings. Let's explore corporate strategy. What are the key elements of managing cross-functional teams in a fast-scaling company?"
                "daily life" -> "Hi there! Let's reflect on lifestyle. How do you maintain a healthy work-life integration amidst demanding schedules?"
                else -> "Welcome to our advanced conversation space. Let's explore some complex themes. What is a recent news event or trend that has captured your attention?"
            }
        }
    }

    fun loadSessionById(sessionId: String) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                loadSession(session)
            }
        }
    }

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return repository.getMessagesForSession(sessionId)
    }

    private fun loadSession(session: ConversationSession) {
        _currentSession.value = session
        prefs.currentSessionId = session.id
        chatMemoryManager.updateTopic(session.topic)
        
        // Cancel previous collector if active
        messageCollectorJob?.cancel()
        
        // Observe message stream
        messageCollectorJob = viewModelScope.launch {
            repository.getMessagesForSession(session.id).collect { messages ->
                _currentMessages.value = messages
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSession.value?.id == sessionId) {
                _currentSession.value = null
                _currentMessages.value = emptyList()
                prefs.currentSessionId = null
            }
        }
    }

    fun resetCurrentSession(onSpeak: ((String) -> Unit)? = null) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            _isTutorThinking.value = false
            database.tutorDao().deleteMessagesForSession(session.id)
            
            // Re-insert the introductory message based on level and topic
            val introText = getIntroText(session.level, session.topic)
            val introMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                sender = "tutor",
                text = introText,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(introMsg)
            if (prefs.ttsEnabled && onSpeak != null && introText.isNotEmpty()) {
                onSpeak(introText)
            }
        }
    }

    fun updateSettings(
        useGemini: Boolean,
        url: String,
        apiKey: String,
        model: String,
        tts: Boolean,
        corrections: Boolean
    ) {
        prefs.useGeminiDirect = useGemini
        prefs.ollamaUrl = url
        prefs.ollamaApiKey = apiKey
        prefs.ollamaModel = model
        prefs.ttsEnabled = tts
        prefs.correctionsEnabled = corrections

        _useGeminiDirect.value = useGemini
        _ollamaUrl.value = url
        _ollamaApiKey.value = apiKey
        _ollamaModel.value = model
        _ttsEnabled.value = tts
        _correctionsEnabled.value = corrections
    }

    fun updateSettings(
        useGemini: Boolean,
        url: String,
        apiKey: String,
        model: String,
        tts: Boolean,
        corrections: Boolean,
        gKey: String
    ) {
        prefs.geminiApiKey = gKey
        _geminiApiKey.value = gKey
        updateSettings(useGemini, url, apiKey, model, tts, corrections)
    }

    fun updateSettings(
        useGemini: Boolean,
        url: String,
        apiKey: String,
        model: String,
        tts: Boolean,
        corrections: Boolean,
        gKey: String,
        demoMode: Boolean
    ) {
        prefs.geminiApiKey = gKey
        _geminiApiKey.value = gKey
        prefs.useDemoMode = demoMode
        _useDemoMode.value = demoMode
        updateSettings(useGemini, url, apiKey, model, tts, corrections)
    }

    fun enableDemoMode() {
        prefs.useDemoMode = true
        _useDemoMode.value = true
        prefs.useGeminiDirect = false
        _useGeminiDirect.value = false
    }

    fun updateUseWebSpeechAPI(enabled: Boolean) {
        prefs.useWebSpeechAPI = enabled
        _useWebSpeechAPI.value = enabled
    }

    fun updateTtsRate(rate: Float) {
        prefs.ttsRate = rate
        _ttsRate.value = rate
    }

    fun updateTtsPitch(pitch: Float) {
        prefs.ttsPitch = pitch
        _ttsPitch.value = pitch
    }

    fun updateTtsLocale(locale: String) {
        prefs.ttsLocale = locale
        _ttsLocale.value = locale
    }

    fun updateTtsVoiceName(voiceName: String?) {
        prefs.ttsVoiceName = voiceName
        _ttsVoiceName.value = voiceName
    }

    fun updateAvailableVoices(voices: List<String>) {
        _availableVoices.value = voices
    }

    fun switchToDemoModeAndRetry(errorMessageId: String, onSpeak: (String) -> Unit) {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            // 1. Enable Demo Mode
            enableDemoMode()
            
            // 2. Delete the error message
            repository.deleteMessage(errorMessageId)
            
            // 3. Insert empty/placeholder message for tutor response streaming in demo mode
            _isTutorThinking.value = true
            val tutorMsgId = UUID.randomUUID().toString()
            var tutorMsgText = ""
            val tutorMsg = ChatMessage(
                id = tutorMsgId,
                sessionId = session.id,
                sender = "tutor",
                text = "",
                timestamp = System.currentTimeMillis() + 10
            )
            repository.insertMessage(tutorMsg)
            
            // 4. Gather history (excluding the deleted error message)
            val baseHistory = _currentMessages.value.filter { it.id != errorMessageId }
            val conversationHistory = chatMemoryManager.getSlidingWindow(baseHistory)
            val currentTopic = chatMemoryManager.getCurrentTopic(session.topic)
            
            // 5. Stream response
            apiService.generateTutorResponseStream(
                conversationId = session.id,
                level = session.level,
                topic = currentTopic,
                messages = conversationHistory,
                agentConfig = _agentConfig.value,
                memories = allAgentMemories.value,
                onChunk = { chunk ->
                    tutorMsgText += chunk
                    viewModelScope.launch {
                        repository.insertMessage(tutorMsg.copy(text = tutorMsgText))
                    }
                    _isTutorThinking.value = false
                }
            )
            
            _isTutorThinking.value = false
            
            if (prefs.ttsEnabled && tutorMsgText.isNotEmpty()) {
                onSpeak(tutorMsgText)
            }
        }
    }

    fun sendMessage(userText: String, onSpeak: (String) -> Unit) {
        val session = _currentSession.value ?: return
        if (userText.trim().isEmpty()) return

        viewModelScope.launch {
            _isTutorThinking.value = true

            // 1. Insert User Message
            val userMsgId = UUID.randomUUID().toString()
            val userMsg = ChatMessage(
                id = userMsgId,
                sessionId = session.id,
                sender = "user",
                text = userText,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(userMsg)

            // 2. Request Corrections in background (optional)
            if (prefs.correctionsEnabled) {
                launch {
                    val correctionResponse = apiService.getCorrections(userText)
                    if (correctionResponse != null && correctionResponse.corrected != userText) {
                        // Check if corrections or explanations exist
                        val correctionsList = if (correctionResponse.corrections.isNotEmpty()) {
                            correctionResponse.corrections
                        } else {
                            correctionResponse.explanations
                        }
                        
                        val serializedCorrections = if (correctionsList.isNotEmpty()) {
                            try {
                                val jsonArray = org.json.JSONArray()
                                correctionsList.forEach { exp ->
                                    val obj = org.json.JSONObject()
                                    obj.put("original", exp.original)
                                    obj.put("fixed", exp.fixed)
                                    obj.put("reason", exp.reason)
                                    jsonArray.put(obj)
                                }
                                jsonArray.toString()
                            } catch (e: Exception) {
                                // fallback to bulleted format if JSON creation fails
                                val sb = StringBuilder()
                                correctionsList.forEach { exp ->
                                    sb.append("• \"${exp.original}\" -> \"${exp.fixed}\": ${exp.reason}\n")
                                }
                                sb.toString().trim()
                            }
                        } else {
                            ""
                        }
                        
                        val updatedUserMsg = userMsg.copy(
                            correctedText = correctionResponse.corrected,
                            correctionsJson = serializedCorrections
                        )
                        repository.insertMessage(updatedUserMsg)
                        harvestMemories(userText, serializedCorrections)
                    } else {
                        harvestMemories(userText, null)
                    }
                }
            } else {
                harvestMemories(userText, null)
            }

            // 3. Insert empty/placeholder message for tutor response streaming
            val tutorMsgId = UUID.randomUUID().toString()
            var tutorMsgText = ""
            val tutorMsg = ChatMessage(
                id = tutorMsgId,
                sessionId = session.id,
                sender = "tutor",
                text = "",
                timestamp = System.currentTimeMillis() + 10 // slightly later
            )
            repository.insertMessage(tutorMsg)

            // Gather past messages including the current one to feed into API for context
            val baseHistory = _currentMessages.value.toMutableList()
            if (!baseHistory.any { it.id == userMsgId }) {
                baseHistory.add(userMsg)
            }
            val conversationHistory = chatMemoryManager.getSlidingWindow(baseHistory)
            val currentTopic = chatMemoryManager.getCurrentTopic(session.topic)

            // Stream response
            var speechTriggered = false
            apiService.generateTutorResponseStream(
                conversationId = session.id,
                level = session.level,
                topic = currentTopic,
                messages = conversationHistory,
                agentConfig = _agentConfig.value,
                memories = allAgentMemories.value,
                onChunk = { chunk ->
                    tutorMsgText += chunk
                    viewModelScope.launch {
                        repository.insertMessage(tutorMsg.copy(text = tutorMsgText))
                    }
                    _isTutorThinking.value = false
                    
                    // Trigger text-to-speech dynamically or at the end
                    // For best user experience, we can speak the response once completed,
                    // or speak when the first full chunk arrives.
                    // We will speak the final full response when the streaming finishes (handled below).
                }
            )

            _isTutorThinking.value = false

            // Trigger Speech of the complete tutor message once streaming completes
            if (prefs.ttsEnabled && tutorMsgText.isNotEmpty()) {
                onSpeak(tutorMsgText)
            }
        }
    }

    // --- Daily Goal & Timer Methods ---
    fun addPracticeSeconds(seconds: Int) {
        prefs.dailyPracticedSeconds += seconds
        _dailyPracticedSeconds.value = prefs.dailyPracticedSeconds
        
        val goalSeconds = prefs.dailyPracticeGoalMinutes * 60
        if (prefs.dailyPracticedSeconds >= goalSeconds) {
            checkAndIncrementStreak()
        }
    }

    fun updatePracticeGoal(minutes: Int) {
        prefs.dailyPracticeGoalMinutes = minutes
        _dailyPracticeGoalMinutes.value = minutes
    }

    // --- Vocabulary Word Methods ---
    fun saveVocabularyWord(word: String, definition: String, exampleSentence: String) {
        viewModelScope.launch {
            val vocab = VocabularyWord(
                word = word.trim().lowercase(java.util.Locale.getDefault()),
                definition = definition.trim(),
                exampleSentence = exampleSentence.trim(),
                createdAt = System.currentTimeMillis()
            )
            repository.insertVocabularyWord(vocab)
        }
    }

    fun deleteVocabularyWord(word: String) {
        viewModelScope.launch {
            repository.deleteVocabularyWord(word)
        }
    }

    fun updateVocabularyWordStatus(word: String, isLearned: Boolean) {
        viewModelScope.launch {
            repository.updateVocabularyWordStatus(word, isLearned)
        }
    }

    fun updateSessionTopic(sessionId: String, topic: String) {
        viewModelScope.launch {
            repository.updateSessionTopic(sessionId, topic)
            if (_currentSession.value?.id == sessionId) {
                _currentSession.value = _currentSession.value?.copy(topic = topic)
                chatMemoryManager.updateTopic(topic)
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun updateMessage(messageId: String, text: String, correctedText: String?) {
        viewModelScope.launch {
            repository.updateMessage(messageId, text, correctedText)
        }
    }

    fun resetStreak() {
        viewModelScope.launch {
            val updatedStreak = DailyStreakRecord(id = 1, currentStreak = 0, lastCompletedDate = "", longestStreak = 0)
            repository.insertDailyStreak(updatedStreak)
            _dailyStreak.value = updatedStreak
        }
    }

    fun setStreak(streakVal: Int) {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val existing = repository.getDailyStreak()
            val longest = maxOf(existing?.longestStreak ?: 0, streakVal)
            val updatedStreak = DailyStreakRecord(id = 1, currentStreak = streakVal, lastCompletedDate = today, longestStreak = longest)
            repository.insertDailyStreak(updatedStreak)
            _dailyStreak.value = updatedStreak
        }
    }

    fun fetchWordDefinition(word: String, sentence: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val definition = apiService.getWordDefinition(word, sentence)
            onResult(definition)
        }
    }

    // --- AI Agent Studio Methods ---
    fun updateAgentConfig(config: AgentConfig) {
        viewModelScope.launch {
            repository.insertAgentConfig(config)
            _agentConfig.value = config
        }
    }

    fun addAgentMemory(category: String, fact: String) {
        viewModelScope.launch {
            val memory = AgentMemory(
                id = UUID.randomUUID().toString(),
                category = category,
                fact = fact,
                createdAt = System.currentTimeMillis(),
                weight = 1.0f
            )
            repository.insertAgentMemory(memory)
        }
    }

    fun deleteAgentMemory(id: String) {
        viewModelScope.launch {
            repository.deleteAgentMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
        }
    }

    fun clearAllTrainingLogs() {
        viewModelScope.launch {
            repository.clearAllTrainingLogs()
        }
    }

    fun selectAttentionSentence(sentence: String) {
        _selectedAttentionSentence.value = sentence
        computeAttentionMatrix(sentence)
    }

    fun computeAttentionMatrix(sentence: String) {
        val words = sentence.split(" ").filter { it.isNotEmpty() }
        if (words.isEmpty()) {
            _attentionMatrix.value = emptyList()
            return
        }
        val size = words.size
        val matrix = List(size) { i ->
            List(size) { j ->
                var score = if (i == j) 0.45f else 0.10f
                val wordI = words[i].lowercase().replace(Regex("[^a-zA-Z]"), "")
                val wordJ = words[j].lowercase().replace(Regex("[^a-zA-Z]"), "")
                
                val subjects = listOf("i", "you", "he", "she", "we", "they", "messi", "ronaldo", "pizza", "football", "teacher", "ai")
                val verbs = listOf("love", "like", "support", "play", "am", "is", "are", "want", "went", "did", "practicing", "learning")
                if ((wordI in subjects && wordJ in verbs) || (wordI in verbs && wordJ in subjects)) {
                    score += 0.25f
                }
                score += (kotlin.math.sin((i * j).toDouble()).toFloat() * 0.05f)
                score.coerceIn(0.02f, 0.98f)
            }
        }
        _attentionMatrix.value = matrix
    }

    fun trainAgent(feedbackSample: String, onFinished: () -> Unit) {
        if (_isTrainingAgent.value) return
        viewModelScope.launch {
            _isTrainingAgent.value = true
            _trainingProgressMessage.value = "Initializing backpropagation and transformer weights..."
            kotlinx.coroutines.delay(1200)

            val currentConf = _agentConfig.value
            val newLossHistory = org.json.JSONArray(currentConf.lossHistoryJson)
            val startingLoss = if (newLossHistory.length() > 0) {
                newLossHistory.getDouble(newLossHistory.length() - 1).toFloat()
            } else {
                1.25f
            }

            var currentLoss = startingLoss
            var currentAccuracy = 0.65f + (currentConf.trainingIterationCount * 0.02f).coerceAtMost(0.30f)

            for (epoch in 1..5) {
                _trainingProgressMessage.value = "Epoch $epoch/5: Computing loss gradients and adjusting multi-head attention scores..."
                kotlinx.coroutines.delay(1000)
                
                val lr = 0.05f + (Math.random().toFloat() * 0.02f)
                val lossDelta = (lr * (currentLoss * 0.4f)) + (Math.random().toFloat() * 0.03f)
                currentLoss = (currentLoss - lossDelta).coerceAtLeast(0.08f)
                currentAccuracy = (currentAccuracy + (lossDelta * 0.5f)).coerceAtMost(0.98f)

                val log = AgentTrainingLog(
                    id = UUID.randomUUID().toString(),
                    epoch = currentConf.trainingIterationCount * 5 + epoch,
                    loss = currentLoss,
                    accuracy = currentAccuracy,
                    feedbackSample = feedbackSample,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertTrainingLog(log)
                newLossHistory.put(currentLoss.toDouble())
            }

            val updatedConf = currentConf.copy(
                trainingIterationCount = currentConf.trainingIterationCount + 1,
                lossHistoryJson = newLossHistory.toString()
            )
            repository.insertAgentConfig(updatedConf)
            _agentConfig.value = updatedConf

            val memory = AgentMemory(
                id = UUID.randomUUID().toString(),
                category = "Tone Pref",
                fact = "Trained Directive: $feedbackSample",
                createdAt = System.currentTimeMillis(),
                weight = 1.0f
            )
            repository.insertAgentMemory(memory)

            _isTrainingAgent.value = false
            _trainingProgressMessage.value = "Backpropagation complete! Model weights successfully updated."
            onFinished()
        }
    }

    private fun harvestMemories(userText: String, correctionsJson: String?) {
        viewModelScope.launch {
            val lower = userText.lowercase().trim()
            
            val nameRegex = Regex("(?:my name is|i am called|i'm|call me) ([a-zA-Z\\s]{2,15})")
            val nameMatch = nameRegex.find(lower)
            if (nameMatch != null) {
                val name = nameMatch.groupValues[1].trim()
                repository.insertAgentMemory(AgentMemory(
                    id = UUID.randomUUID().toString(),
                    category = "Personal",
                    fact = "Student's name is $name",
                    createdAt = System.currentTimeMillis()
                ))
            }
            
            val jobRegex = Regex("(?:i work as a|i am a|i'm a|i work in) ([a-zA-Z\\s]{3,20})")
            val jobMatch = jobRegex.find(lower)
            if (jobMatch != null) {
                val job = jobMatch.groupValues[1].trim()
                repository.insertAgentMemory(AgentMemory(
                    id = UUID.randomUUID().toString(),
                    category = "Personal",
                    fact = "Student works as/in $job",
                    createdAt = System.currentTimeMillis()
                ))
            }

            val hobbies = listOf("football", "soccer", "basketball", "tennis", "guitar", "piano", "reading", "gaming", "coding", "cooking", "movies")
            hobbies.forEach { hobby ->
                if (lower.contains("like $hobby") || lower.contains("love $hobby") || lower.contains("enjoy $hobby") || lower.contains("fan of $hobby")) {
                    repository.insertAgentMemory(AgentMemory(
                        id = UUID.randomUUID().toString(),
                        category = "Interest",
                        fact = "Student enjoys $hobby",
                        createdAt = System.currentTimeMillis()
                    ))
                }
            }

            val subjects = listOf("messi", "ronaldo", "barcelona", "real madrid", "pizza", "sushi", "pasta", "burger", "travel", "japan", "france", "england")
            subjects.forEach { subject ->
                if (lower.contains(subject)) {
                    repository.insertAgentMemory(AgentMemory(
                        id = UUID.randomUUID().toString(),
                        category = "Interest",
                        fact = "Student is interested in $subject",
                        createdAt = System.currentTimeMillis()
                    ))
                }
            }

            if (!correctionsJson.isNullOrEmpty()) {
                try {
                    val array = org.json.JSONArray(correctionsJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val original = obj.optString("original")
                        val fixed = obj.optString("fixed")
                        val reason = obj.optString("reason")
                        
                        repository.insertAgentMemory(AgentMemory(
                            id = UUID.randomUUID().toString(),
                            category = "Grammar Bug",
                            fact = "Made error: \"$original\" (suggested: \"$fixed\"). Focus area: $reason",
                            createdAt = System.currentTimeMillis(),
                            weight = 0.8f
                        ))
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }
    }
}
