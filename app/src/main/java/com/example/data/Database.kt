package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversation_sessions")
data class ConversationSession(
    @PrimaryKey val id: String, // UUID
    val level: String,          // beginner, intermediate, advanced
    val topic: String,          // Free talk, Job interview, Travel, Daily life, Business
    val createdAt: Long,
    val summary: String? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String, // UUID
    val sessionId: String,
    val sender: String,         // "user" or "tutor"
    val text: String,
    val timestamp: Long,
    val correctedText: String? = null,
    // JSON string containing the list of corrections for user messages
    val correctionsJson: String? = null
)

@Dao
interface TutorDao {
    @Query("SELECT * FROM conversation_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<ConversationSession>>

    @Query("SELECT * FROM conversation_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ConversationSession?

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM conversation_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("UPDATE conversation_sessions SET summary = :summary WHERE id = :sessionId")
    suspend fun updateSessionSummary(sessionId: String, summary: String)

    @Query("UPDATE conversation_sessions SET topic = :topic WHERE id = :sessionId")
    suspend fun updateSessionTopic(sessionId: String, topic: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("UPDATE chat_messages SET text = :text, correctedText = :correctedText WHERE id = :messageId")
    suspend fun updateMessage(messageId: String, text: String, correctedText: String?)

    // --- Vocabulary Words Queries ---
    @Query("SELECT * FROM vocabulary_words ORDER BY createdAt DESC")
    fun getAllVocabularyWords(): Flow<List<VocabularyWord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabularyWord(word: VocabularyWord)

    @Query("DELETE FROM vocabulary_words WHERE word = :word")
    suspend fun deleteVocabularyWord(word: String)

    @Query("UPDATE vocabulary_words SET isLearned = :isLearned WHERE word = :word")
    suspend fun updateVocabularyWordStatus(word: String, isLearned: Boolean)

    // --- Daily Streak Queries ---
    @Query("SELECT * FROM daily_streaks WHERE id = 1 LIMIT 1")
    suspend fun getDailyStreak(): DailyStreakRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStreak(streak: DailyStreakRecord)

    // --- AI Agent Memories Queries ---
    @Query("SELECT * FROM agent_memories ORDER BY createdAt DESC")
    fun getAllAgentMemories(): Flow<List<AgentMemory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentMemory(memory: AgentMemory)

    @Query("DELETE FROM agent_memories WHERE id = :memoryId")
    suspend fun deleteAgentMemory(memoryId: String)

    @Query("DELETE FROM agent_memories")
    suspend fun clearAllMemories()

    // --- AI Agent Config Queries ---
    @Query("SELECT * FROM agent_config WHERE id = 1 LIMIT 1")
    suspend fun getAgentConfig(): AgentConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentConfig(config: AgentConfig)

    // --- AI Agent Training Logs Queries ---
    @Query("SELECT * FROM agent_training_logs ORDER BY timestamp DESC")
    fun getAllTrainingLogs(): Flow<List<AgentTrainingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingLog(log: AgentTrainingLog)

    @Query("DELETE FROM agent_training_logs")
    suspend fun clearAllTrainingLogs()
}

@Entity(tableName = "agent_memories")
data class AgentMemory(
    @PrimaryKey val id: String, // UUID
    val category: String,       // "Interest", "Grammar Bug", "Tone Pref", "Vocabulary"
    val fact: String,           // e.g., "Enjoys talking about Lionel Messi and football"
    val createdAt: Long,
    val weight: Float = 1.0f    // Connection weight / relevance
)

@Entity(tableName = "agent_config")
data class AgentConfig(
    @PrimaryKey val id: Int = 1,
    val name: String = "AhdrAnglais",
    val personaType: String = "Balanced Teacher", // "Friendly Peer", "Strict Grammarian", "IELTS Expert"
    val temperature: Float = 0.75f,
    val baseInstructionOverride: String = "",
    val trainingIterationCount: Int = 0,
    val lossHistoryJson: String = "[]" // JSON representation of training loss list
)

@Entity(tableName = "agent_training_logs")
data class AgentTrainingLog(
    @PrimaryKey val id: String, // UUID
    val epoch: Int,
    val loss: Float,
    val accuracy: Float,
    val feedbackSample: String,
    val timestamp: Long
)

@Entity(tableName = "vocabulary_words")
data class VocabularyWord(
    @PrimaryKey val word: String,
    val definition: String,
    val exampleSentence: String,
    val createdAt: Long,
    val isLearned: Boolean = false
)

@Entity(tableName = "daily_streaks")
data class DailyStreakRecord(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int,
    val lastCompletedDate: String, // "yyyy-MM-dd"
    val longestStreak: Int = 0
)

@Database(
    entities = [
        ConversationSession::class,
        ChatMessage::class,
        VocabularyWord::class,
        DailyStreakRecord::class,
        AgentMemory::class,
        AgentConfig::class,
        AgentTrainingLog::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TutorDatabase : RoomDatabase() {
    abstract fun tutorDao(): TutorDao

    companion object {
        @Volatile
        private var INSTANCE: TutorDatabase? = null

        fun getDatabase(context: Context): TutorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TutorDatabase::class.java,
                    "tutor_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TutorRepository(private val tutorDao: TutorDao) {
    val allSessions: Flow<List<ConversationSession>> = tutorDao.getAllSessions()
    val allVocabularyWords: Flow<List<VocabularyWord>> = tutorDao.getAllVocabularyWords()
    val allAgentMemories: Flow<List<AgentMemory>> = tutorDao.getAllAgentMemories()
    val allTrainingLogs: Flow<List<AgentTrainingLog>> = tutorDao.getAllTrainingLogs()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return tutorDao.getMessagesForSession(sessionId)
    }

    suspend fun getSessionById(sessionId: String): ConversationSession? {
        return tutorDao.getSessionById(sessionId)
    }

    suspend fun insertSession(session: ConversationSession) {
        tutorDao.insertSession(session)
    }

    suspend fun insertMessage(message: ChatMessage) {
        tutorDao.insertMessage(message)
    }

    suspend fun deleteSession(sessionId: String) {
        tutorDao.deleteSession(sessionId)
        tutorDao.deleteMessagesForSession(sessionId)
    }

    suspend fun updateSessionSummary(sessionId: String, summary: String) {
        tutorDao.updateSessionSummary(sessionId, summary)
    }

    suspend fun updateSessionTopic(sessionId: String, topic: String) {
        tutorDao.updateSessionTopic(sessionId, topic)
    }

    suspend fun deleteMessage(messageId: String) {
        tutorDao.deleteMessage(messageId)
    }

    suspend fun updateMessage(messageId: String, text: String, correctedText: String?) {
        tutorDao.updateMessage(messageId, text, correctedText)
    }

    suspend fun insertVocabularyWord(word: VocabularyWord) {
        tutorDao.insertVocabularyWord(word)
    }

    suspend fun deleteVocabularyWord(word: String) {
        tutorDao.deleteVocabularyWord(word)
    }

    suspend fun updateVocabularyWordStatus(word: String, isLearned: Boolean) {
        tutorDao.updateVocabularyWordStatus(word, isLearned)
    }

    suspend fun getDailyStreak(): DailyStreakRecord? {
        return tutorDao.getDailyStreak()
    }

    suspend fun insertDailyStreak(streak: DailyStreakRecord) {
        tutorDao.insertDailyStreak(streak)
    }

    // --- AI Agent Methods ---
    suspend fun getAgentConfig(): AgentConfig {
        return tutorDao.getAgentConfig() ?: AgentConfig().also {
            tutorDao.insertAgentConfig(it)
        }
    }

    suspend fun insertAgentConfig(config: AgentConfig) {
        tutorDao.insertAgentConfig(config)
    }

    suspend fun insertAgentMemory(memory: AgentMemory) {
        tutorDao.insertAgentMemory(memory)
    }

    suspend fun deleteAgentMemory(memoryId: String) {
        tutorDao.deleteAgentMemory(memoryId)
    }

    suspend fun clearAllMemories() {
        tutorDao.clearAllMemories()
    }

    suspend fun insertTrainingLog(log: AgentTrainingLog) {
        tutorDao.insertTrainingLog(log)
    }

    suspend fun clearAllTrainingLogs() {
        tutorDao.clearAllTrainingLogs()
    }
}
