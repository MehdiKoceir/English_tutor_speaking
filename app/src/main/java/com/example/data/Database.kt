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
}

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

@Database(entities = [ConversationSession::class, ChatMessage::class, VocabularyWord::class, DailyStreakRecord::class], version = 3, exportSchema = false)
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
}
