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

        // Default local IP pointing to emulator host or blank
        private const val DEFAULT_OLLAMA_URL = "http://10.0.2.2:11434"
        private const val DEFAULT_OLLAMA_MODEL = "llama3.1:8b"
    }

    var useGeminiDirect: Boolean
        get() = prefs.getBoolean(KEY_USE_GEMINI_DIRECT, true) // Default to Gemini Direct for out-of-the-box usage
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

    var dailyPracticeGoalMinutes: Int
        get() = prefs.getInt("daily_practice_goal_minutes", 10)
        set(value) = prefs.edit().putInt("daily_practice_goal_minutes", value).apply()

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
