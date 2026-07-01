package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class TutorApiService(private val prefs: PreferencesHelper) {
    private val TAG = "TutorApiService"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    /**
     * Streams the tutor's response chunk-by-chunk using a callback.
     */
    suspend fun generateTutorResponseStream(
        conversationId: String,
        level: String,
        topic: String,
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (prefs.useGeminiDirect) {
            generateGeminiStream(level, topic, messages, onChunk)
        } else {
            generateOllamaStream(conversationId, level, messages.lastOrNull()?.text ?: "", onChunk)
        }
    }

    /**
     * Gets corrections for a user message.
     */
    suspend fun getCorrections(text: String): CustomCorrectionResponse? = withContext(Dispatchers.IO) {
        if (prefs.useGeminiDirect) {
            getGeminiCorrections(text)
        } else {
            getOllamaCorrections(text)
        }
    }

    // --- Gemini Streaming Implementation ---
    private suspend fun generateGeminiStream(
        level: String,
        topic: String,
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            onChunk("[Error: Gemini API Key is missing. Please add it to the Secrets Panel or set up your Ollama VPS in Settings.]")
            return
        }

        // System prompt for English Tutor
        val systemPrompt = """
            You are a friendly, patient English tutor. 
            You help the user practice English conversation.
            Topic for this session: $topic.
            User's English Level: $level.
            
            Guidelines:
            - Adapt your vocabulary and sentence complexity to the user's level ($level).
              * beginner: Use simple vocabulary, short sentences, and highly clear syntax.
              * intermediate: Use moderate vocabulary, clear grammar, and some useful idioms.
              * advanced: Speak like a native, using rich vocabulary, idioms, and natural flow.
            - Correct the user's mistakes gently if you spot any, explain why, and keep the conversation flowing.
            - Always ask a friendly, open-ended follow-up question at the end to prompt the user to reply.
            - Keep your responses concise (2 to 4 sentences) so the user doesn't get overwhelmed.
        """.trimIndent()

        // Build history in Gemini format
        val geminiContents = mutableListOf<JSONObject>()
        messages.forEach { msg ->
            val role = if (msg.sender == "user") "user" else "model"
            geminiContents.add(
                JSONObject().put("role", role).put(
                    "parts", JSONArray().put(JSONObject().put("text", msg.text))
                )
            )
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray(geminiContents))
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put("generationConfig", JSONObject().put("temperature", 0.7))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:streamGenerateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(mediaTypeJson))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onChunk("[Error: HTTP ${response.code} from Gemini. Make sure your API key is correct.]")
                    return@use
                }
                val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                var line: String?
                // Gemini returns stream as a JSON array of responses, line-by-line
                while (reader.readLine().also { line = it } != null) {
                    val lineText = line?.trim() ?: continue
                    if (lineText.isEmpty()) continue
                    
                    try {
                        // Extract text from SSE chunk
                        // Format is typically a line starting with {"candidates": ...}
                        // Or wrapped in [ ..., ... ]
                        var cleaned = lineText
                        if (cleaned.startsWith("[")) cleaned = cleaned.substring(1)
                        if (cleaned.endsWith(",")) cleaned = cleaned.substring(0, cleaned.length - 1)
                        if (cleaned.endsWith("]")) cleaned = cleaned.substring(0, cleaned.length - 1)
                        
                        val json = JSONObject(cleaned)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCand = candidates.getJSONObject(0)
                            val contentObj = firstCand.optJSONObject("content")
                            val parts = contentObj?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text")
                                if (text.isNotEmpty()) {
                                    onChunk(text)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore individual line parse errors as some lines may be delimiters
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini stream error", e)
            onChunk("[Error: Network connection failed. ${e.localizedMessage}]")
        }
    }

    // --- Ollama Custom VPS Streaming Implementation ---
    private suspend fun generateOllamaStream(
        conversationId: String,
        level: String,
        userMessage: String,
        onChunk: (String) -> Unit
    ) {
        val serverUrl = prefs.ollamaUrl.trimEnd('/')
        val url = "$serverUrl/chat"
        val apiKey = prefs.ollamaApiKey

        val requestBody = JSONObject().apply {
            put("conversation_id", conversationId)
            put("message", userMessage)
            put("level", level.lowercase())
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(mediaTypeJson))

        if (apiKey.isNotEmpty()) {
            requestBuilder.addHeader("X-API-Key", apiKey)
        }

        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 401) {
                    onChunk("[Error: Unauthorized. Check your VPS X-API-Key in Settings.]")
                    return@use
                }
                if (!response.isSuccessful) {
                    onChunk("[Error: Server error ${response.code} from custom VPS.]")
                    return@use
                }
                
                val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                var line: String?
                // SSE streams are sent as:
                // "data: <token>" or simple raw chunks depending on how they wrote their VPS.
                // Let's parse both standard SSE "data: ..." and plain lines!
                while (reader.readLine().also { line = it } != null) {
                    val rawLine = line ?: continue
                    if (rawLine.trim().isEmpty()) continue
                    
                    var textChunk = ""
                    if (rawLine.startsWith("data:")) {
                        textChunk = rawLine.substring(5).trim()
                    } else {
                        textChunk = rawLine.trim()
                    }

                    // Sometimes the SSE sends JSON strings, or raw text. Let's handle both.
                    if (textChunk.startsWith("\"") && textChunk.endsWith("\"") && textChunk.length > 1) {
                        // Unescape simple JSON string
                        try {
                            textChunk = JSONObject("{\"val\":$textChunk}").getString("val")
                        } catch (e: Exception) {}
                    }
                    
                    if (textChunk.isNotEmpty() && textChunk != "[DONE]") {
                        onChunk(textChunk)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ollama stream error", e)
            onChunk("[Error: Unable to connect to your VPS at $serverUrl. Check your network or server status.]")
        }
    }

    // --- Gemini Corrections Implementation ---
    private suspend fun getGeminiCorrections(text: String): CustomCorrectionResponse? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return null

        val prompt = """
            Analyze the following English sentence for grammar, spelling, vocabulary, or phrasing mistakes:
            "$text"
            
            Return a JSON object matching this exact structure:
            {
              "corrected": "the fully corrected sentence, or identical to original if there are absolutely no mistakes",
              "explanations": [
                {
                  "original": "the exact segment that was wrong",
                  "fixed": "the corrected segment",
                  "reason": "short explanation of why it was wrong and how to fix it"
                }
              ]
            }
            If the input sentence is perfectly correct, explanations should be empty and corrected should match the input exactly. Do not output anything else than JSON.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(mediaTypeJson))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                val json = JSONObject(bodyString)
                val candidates = json.optJSONArray("candidates")
                val textResponse = candidates?.getJSONObject(0)
                    ?.getJSONObject("content")
                    ?.getJSONArray("parts")
                    ?.getJSONObject(0)
                    ?.getString("text") ?: return null

                // Parse correction response using Moshi
                val adapter = moshi.adapter(CustomCorrectionResponse::class.java)
                adapter.fromJson(textResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini correction error", e)
            null
        }
    }

    // --- Ollama Corrections Implementation ---
    private suspend fun getOllamaCorrections(text: String): CustomCorrectionResponse? {
        val serverUrl = prefs.ollamaUrl.trimEnd('/')
        val url = "$serverUrl/correct"
        val apiKey = prefs.ollamaApiKey

        val requestBody = JSONObject().apply {
            put("text", text)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(mediaTypeJson))

        if (apiKey.isNotEmpty()) {
            requestBuilder.addHeader("X-API-Key", apiKey)
        }

        val request = requestBuilder.build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body?.string() ?: return null
                val adapter = moshi.adapter(CustomCorrectionResponse::class.java)
                adapter.fromJson(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ollama correction error", e)
            null
        }
    }

    /**
     * Fetches a concise, context-aware definition of a word in a sentence using Gemini.
     */
    suspend fun getWordDefinition(word: String, sentence: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "No API Key configured. Please enter the definition manually."
        }

        val prompt = """
            You are a helpful language learning assistant. 
            Define the English word "$word" as it is used in the following sentence:
            "$sentence"
            
            Provide a clear, brief, and beginner-friendly definition or explanation (maximum 15-20 words). Do not include any formatting, markdown, or other text, just the direct definition.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(mediaTypeJson))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Word saved. Enter definition manually."
                }
                val bodyString = response.body?.string() ?: return@withContext "Word saved. Enter definition manually."
                val json = JSONObject(bodyString)
                val candidates = json.optJSONArray("candidates")
                val textResponse = candidates?.getJSONObject(0)
                    ?.getJSONObject("content")
                    ?.getJSONArray("parts")
                    ?.getJSONObject(0)
                    ?.getString("text") ?: return@withContext "Word saved. Enter definition manually."
                textResponse.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Word definition fetch error", e)
            "Word saved. Enter definition manually."
        }
    }
}
