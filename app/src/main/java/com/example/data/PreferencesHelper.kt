package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesHelper(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("tutor_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USE_GEMINI_DIRECT = "use_gemini_direct"
        private const val KEY_OLLAMA_URL = "ollama_url"
        private const val KEY_OLLAMA_API_KEY = "ollama_api_key"
        private const val KEY_OLLAMA_MODEL = "ollama_model"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_CORRECTIONS_ENABLED = "corrections_enabled"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"

        // Default local IP pointing to emulator host or blank
        private const val DEFAULT_OLLAMA_URL = "http://10.0.2.2:11434"
        private const val DEFAULT_OLLAMA_MODEL = "llama3.1:8b"
    }

    var useGeminiDirect: Boolean
        get() {
            if (!prefs.contains(KEY_USE_GEMINI_DIRECT)) {
                val userKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
                val buildKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
                val hasApiKey = userKey.isNotEmpty() || (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY" && buildKey != "AIzaSyDvGns0KeM1GSD53vN79HJ0XQccg6_cpgY")
                if (hasApiKey) {
                    return true
                }
            }
            return prefs.getBoolean(KEY_USE_GEMINI_DIRECT, false)
        }
        set(value) = prefs.edit().putBoolean(KEY_USE_GEMINI_DIRECT, value).apply()

    var ollamaUrl: String
        get() = prefs.getString(KEY_OLLAMA_URL, DEFAULT_OLLAMA_URL) ?: DEFAULT_OLLAMA_URL
        set(value) = prefs.edit().putString(KEY_OLLAMA_URL, value).apply()

    var ollamaApiKey: String
        get() = prefs.getString(KEY_OLLAMA_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OLLAMA_API_KEY, value).apply()

    var ollamaModel: String
        get() = prefs.getString(KEY_OLLAMA_MODEL, DEFAULT_OLLAMA_MODEL) ?: DEFAULT_OLLAMA_MODEL
        set(value) = prefs.edit().putString(KEY_OLLAMA_MODEL, value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var correctionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CORRECTIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CORRECTIONS_ENABLED, value).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var useDemoMode: Boolean
        get() {
            if (!prefs.contains("use_demo_mode")) {
                val userKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
                val buildKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
                val hasApiKey = userKey.isNotEmpty() || (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY" && buildKey != "AIzaSyDvGns0KeM1GSD53vN79HJ0XQccg6_cpgY")
                if (hasApiKey) {
                    return false
                }
            }
            return prefs.getBoolean("use_demo_mode", true)
        }
        set(value) = prefs.edit().putBoolean("use_demo_mode", value).apply()

    var ttsRate: Float
        get() = prefs.getFloat("tts_rate", 1.0f)
        set(value) = prefs.edit().putFloat("tts_rate", value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat("tts_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("tts_pitch", value).apply()

    var ttsLocale: String
        get() = prefs.getString("tts_locale", "US") ?: "US"
        set(value) = prefs.edit().putString("tts_locale", value).apply()

    var ttsVoiceName: String?
        get() = prefs.getString("tts_voice_name", null)
        set(value) = prefs.edit().putString("tts_voice_name", value).apply()

    var currentSessionId: String?
        get() = prefs.getString("current_session_id", null)
        set(value) = prefs.edit().putString("current_session_id", value).apply()

    var dailyPracticeGoalMinutes: Int
        get() = prefs.getInt("daily_practice_goal_minutes", 10)
        set(value) = prefs.edit().putInt("daily_practice_goal_minutes", value).apply()

    var useWebSpeechAPI: Boolean
        get() = prefs.getBoolean("use_web_speech_api", true)
        set(value) = prefs.edit().putBoolean("use_web_speech_api", value).apply()

    var dailyPracticedSeconds: Int
        get() {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val savedDate = prefs.getString("last_practice_date", "") ?: ""
            if (today != savedDate) {
                prefs.edit().putString("last_practice_date", today).putInt("daily_practiced_seconds", 0).apply()
                return 0
            }
            return prefs.getInt("daily_practiced_seconds", 0)
        }
        set(value) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            prefs.edit().putString("last_practice_date", today).putInt("daily_practiced_seconds", value).apply()
        }
}
