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

    private val _correctionsEnabled = MutableStateFlow(prefs.correctionsEnabled)
    val correctionsEnabled: StateFlow<Boolean> = _correctionsEnabled.asStateFlow()

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
            "intermediate" -> when (topic.lowercase()) {
                "job interview" -> "Hello! Let's begin our mock interview. Tell me about yourself and why you're interested in this position."
                "travel" -> "Hi! Travel is always exciting. If you could visit any country in the world tomorrow, where would you go and why?"
                "business" -> "Welcome! Let's practice professional communication. How do you usually handle difficult projects or tight deadlines at work?"
                "daily life" -> "Hey! Let's catch up. How was your day? Did you do anything interesting or productive today?"
                else -> "Hello! I'm happy to practice English with you today. What is a topic you find interesting that you'd like to talk about?"
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

    private fun loadSession(session: ConversationSession) {
        _currentSession.value = session
        
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
                        // Check if explanations exist
                        val serializedCorrections = if (correctionResponse.explanations.isNotEmpty()) {
                            // Turn explanations list into a clean JSON array or simplified text list
                            val sb = StringBuilder()
                            correctionResponse.explanations.forEach { exp ->
                                sb.append("• \"${exp.original}\" -> \"${exp.fixed}\": ${exp.reason}\n")
                            }
                            sb.toString().trim()
                        } else {
                            ""
                        }
                        
                        val updatedUserMsg = userMsg.copy(
                            correctedText = correctionResponse.corrected,
                            correctionsJson = serializedCorrections
                        )
                        repository.insertMessage(updatedUserMsg)
                    }
                }
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
            val conversationHistory = _currentMessages.value.toMutableList()
            if (!conversationHistory.any { it.id == userMsgId }) {
                conversationHistory.add(userMsg)
            }

            // Stream response
            var speechTriggered = false
            apiService.generateTutorResponseStream(
                conversationId = session.id,
                level = session.level,
                topic = session.topic,
                messages = conversationHistory,
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

    fun fetchWordDefinition(word: String, sentence: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val definition = apiService.getWordDefinition(word, sentence)
            onResult(definition)
        }
    }
}
