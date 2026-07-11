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
        if (prefs.useDemoMode) {
            generateDemoStream(level, topic, messages, onChunk)
        } else if (prefs.useGeminiDirect) {
            generateGeminiStream(level, topic, messages, onChunk)
        } else {
            generateOllamaStream(conversationId, level, messages.lastOrNull()?.text ?: "", onChunk)
        }
    }

    /**
     * Gets corrections for a user message.
     */
    suspend fun getCorrections(text: String): CustomCorrectionResponse? = withContext(Dispatchers.IO) {
        if (prefs.useDemoMode) {
            getDemoCorrections(text)
        } else if (prefs.useGeminiDirect) {
            getGeminiCorrections(text)
        } else {
            getOllamaCorrections(text)
        }
    }

    // --- Gemini API Key Resolution Helper ---
    private fun getGeminiApiKey(): String {
        val userKey = prefs.geminiApiKey.trim()
        if (userKey.isNotEmpty()) {
            return userKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        // Proactively block the known leaked key or the template placeholder to guide the user
        if (buildKey.isEmpty() || 
            buildKey == "MY_GEMINI_API_KEY" || 
            buildKey == "AIzaSyDvGns0KeM1GSD53vN79HJ0XQccg6_cpgY") {
            return ""
        }
        return buildKey
    }

    // --- Gemini Streaming Implementation ---
    private suspend fun generateGeminiStream(
        level: String,
        topic: String,
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ) {
        val apiKey = getGeminiApiKey()
        if (apiKey.isEmpty()) {
            onChunk("[Error: Gemini API Key is missing or invalid (the default developer key is disabled). Please enter your own Google Gemini API Key in the App Settings screen or configure it via the Secrets panel in AI Studio to start practicing!]")
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
        val apiKey = getGeminiApiKey()
        if (apiKey.isEmpty()) return null

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
        val apiKey = getGeminiApiKey()
        if (prefs.useDemoMode || apiKey.isEmpty()) {
            val w = word.lowercase().trim().removeSurrounding("\"").removeSurrounding("'")
            val offlineDictionary = mapOf(
                "hello" to "A common greeting used to begin a conversation.",
                "practice" to "To perform an activity repeatedly or regularly in order to improve or maintain one's proficiency.",
                "tutor" to "A private teacher, typically one who teaches a single student or a very small group.",
                "english" to "The language originally of England, now spoken worldwide.",
                "travel" to "To go from one place to another, typically over a distance.",
                "interview" to "A formal meeting in which one or more persons question, consult, or evaluate another person.",
                "business" to "The practice of making one's living by engaging in commerce, trade, or professional services.",
                "hobby" to "An activity done regularly in one's leisure time for pleasure.",
                "routine" to "A sequence of actions regularly followed; a fixed program."
            )
            val def = offlineDictionary[w]
            if (def != null) {
                return@withContext "📚 Definition of '$word':\n$def\n\n*(Offline Demo Mode)*"
            } else {
                return@withContext "📚 Definition of '$word':\nIn this context, '$word' refers to a key term used in your conversation. To get a comprehensive, context-aware AI definition, please enter your Gemini API key in Settings!"
            }
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

    private fun getTopicQuestion(topic: String, index: Int, lastUserMessage: String): String {
        return when (topic.lowercase().trim()) {
            "job interview" -> {
                when (index) {
                    0 -> "Welcome to your mock interview. I'll be acting as the hiring manager today. To start, could you please introduce yourself and tell me why you're interested in this role?"
                    1 -> {
                        val roleKeywords = listOf("developer", "engineer", "programmer", "designer", "manager", "assistant", "teacher", "sales", "consultant", "analyst", "student", "doctor", "nurse")
                        val matchedRole = roleKeywords.firstOrNull { lastUserMessage.lowercase().contains(it) }
                        if (matchedRole != null) {
                            "That is a wonderful and highly demanding role! Being a $matchedRole requires a great set of skills. Why do you believe you are the best candidate for this position, and what are your main strengths in this field?"
                        } else {
                            "Thank you for sharing that introduction. Why do you believe you are the best candidate for this position, and what are your main strengths?"
                        }
                    }
                    2 -> "Excellent! Having those strengths is highly valuable in any team. Can you describe a challenging situation you faced in a previous project, school work, or job, and how you resolved it?"
                    3 -> "That is a great example of problem-solving and resilience under pressure. Where do you see yourself professionally in five years?"
                    4 -> "Wonderful! We really value growth and long-term vision in our organization. Do you have any questions for me about our company, the team culture, or the next steps?"
                    else -> "Thank you so much for your time today! That concludes our mock interview. You did a fantastic job explaining your background, and we will get back to you with detailed feedback soon."
                }
            }
            "travel" -> {
                when (index) {
                    0 -> "Hi there! Let's practice some travel English. Imagine you've just arrived at a hotel front desk. How can I help you check-in today?"
                    1 -> {
                        val bookingKeywords = listOf("reservation", "booking", "book", "name", "id", "passport")
                        if (bookingKeywords.any { lastUserMessage.lowercase().contains(it) }) {
                            "Ah, yes! I have your reservation right here. I just need to verify a quick detail. Would you prefer a room with a beautiful city view or a quiet garden view?"
                        } else {
                            "Sure, I can help you with that check-in! Can I have your reservation name or booking confirmation, please? Also, would you prefer a room with a beautiful city view or a quiet garden view?"
                        }
                    }
                    2 -> {
                        if (lastUserMessage.lowercase().contains("city")) {
                            "Great choice! A city view room on our 12th floor is ready for you. Breakfast is served from 7 AM to 10 AM in the main dining hall. Do you have any questions about our hotel amenities or local directions?"
                        } else if (lastUserMessage.lowercase().contains("garden") || lastUserMessage.lowercase().contains("quiet")) {
                            "Perfect! A peaceful garden view room on our 3rd floor is ready. Breakfast is served from 7 AM to 10 AM in the main dining hall. Do you have any questions about our hotel amenities or local directions?"
                        } else {
                            "Excellent! I'll get that all sorted for you. Breakfast is served from 7 AM to 10 AM in the main dining hall. Do you have any questions about our hotel amenities, Wi-Fi, or local directions?"
                        }
                    }
                    3 -> {
                        val amenityKeywords = listOf("pool", "gym", "fitness", "spa", "wifi", "internet", "parking", "restaurant", "food", "eat")
                        if (amenityKeywords.any { lastUserMessage.lowercase().contains(it) }) {
                            "Yes! Our beautiful rooftop pool and complete fitness center are located on the top floor, and Wi-Fi is completely free. We also have a lovely restaurant on the ground floor. Are you planning to do any sightseeing or exploring tomorrow?"
                        } else {
                            "We have a beautiful rooftop pool, a modern gym, and complimentary high-speed Wi-Fi throughout the property. There are also several great local restaurants just a five-minute walk away. Are you planning to do any sightseeing or exploring tomorrow?"
                        }
                    }
                    else -> "That sounds like a wonderful plan! I hope you have an incredible time exploring. Enjoy your stay, and let me know if you need any further assistance!"
                }
            }
            "daily life" -> {
                when (index) {
                    0 -> "Hello! Let's talk about daily life and routines. What does a typical morning look like for you?"
                    1 -> {
                        val morningKeywords = listOf("coffee", "tea", "breakfast", "eat", "food", "cook")
                        val activeKeywords = listOf("gym", "run", "exercise", "workout", "walk", "stretch")
                        if (morningKeywords.any { lastUserMessage.lowercase().contains(it) }) {
                            "Ah, starting the day with a warm drink or a good breakfast is the absolute best! Do you have any favorite hobbies or activities that you look forward to doing on the weekends?"
                        } else if (activeKeywords.any { lastUserMessage.lowercase().contains(it) }) {
                            "Starting your day with physical movement is so energizing! Do you have any favorite hobbies or activities that you look forward to doing on the weekends?"
                        } else {
                            "That sounds like a busy but very productive morning routine! Do you have any favorite hobbies or activities that you look forward to doing on the weekends?"
                        }
                    }
                    2 -> {
                        val hobbyKeywords = listOf("read", "book", "game", "gaming", "music", "sport", "football", "soccer", "hike", "cook", "movie", "watch", "show")
                        val matchedHobby = hobbyKeywords.firstOrNull { lastUserMessage.lowercase().contains(it) }
                        if (matchedHobby != null) {
                            "That sounds incredibly fun! Having dedicated time for $matchedHobby is so important. How long have you been into that, and do you usually practice or do it with friends?"
                        } else {
                            "That's super cool! Engaging in hobbies is a great way to stay creative. How long have you been doing that, and do you practice or share this with friends?"
                        }
                    }
                    3 -> "It's always wonderful to share hobbies or have dedicated personal time. How do you usually like to unwind or relax after a long day?"
                    else -> "Perfect! Thank you for sharing. Daily habits, hobbies, and relaxation really shape our lifestyle. What is one new habit or skill you'd like to build next?"
                }
            }
            "business" -> {
                when (index) {
                    0 -> "Welcome to our business English practice. Today, let's discuss project updates and team collaboration. Can you give me a brief overview of a project you're currently working on?"
                    1 -> {
                        val projectKeywords = listOf("software", "app", "code", "design", "marketing", "sales", "client", "website", "campaign", "business", "product")
                        val matchedProj = projectKeywords.firstOrNull { lastUserMessage.lowercase().contains(it) }
                        if (matchedProj != null) {
                            "That sounds like an exciting $matchedProj project with a lot of potential impact! What are the biggest challenges you're currently facing with this, and how are you managing them?"
                        } else {
                            "Thank you for the overview. That sounds like an important piece of work! What are the biggest challenges you're facing with this project, and how are you managing them?"
                        }
                    }
                    2 -> "Managing project deadlines, constraints, and resources can definitely be tough. How do you usually handle communication within your team to ensure everyone is aligned?"
                    3 -> "Effective communication is definitely key to successful business projects. What is your preferred way to receive feedback on your performance?"
                    else -> "That's a very professional approach. Continuous feedback and open communication are crucial in any high-performing team. Thank you for sharing your experience today!"
                }
            }
            else -> ""
        }
    }

    private suspend fun generateDemoStream(
        level: String,
        topic: String,
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ) {
        val userMessages = messages.filter { it.sender == "user" }
        val lastUserMessage = userMessages.lastOrNull()?.text?.lowercase()?.trim() ?: ""
        val userMessageCount = userMessages.size

        val lastUserCleaned = lastUserMessage.replace("?", "").replace(".", "").replace(",", "").trim()
        val isGreeting = lastUserCleaned in listOf("hi", "hello", "hey", "greetings", "good morning", "good afternoon", "good evening", "howdy", "sup", "hey you")
        val isHowAreYou = lastUserCleaned.contains("how are you") || lastUserCleaned.contains("hows it going") || lastUserCleaned.contains("how do you do") || lastUserCleaned.contains("how are you doing") || lastUserCleaned.contains("how you doing")
        val isClarification = lastUserCleaned in listOf("what", "huh", "pardon", "excuse me", "i dont get it", "i don't get it", "i do not understand", "what do you mean", "what do you mean by that", "explain", "explain this", "can you explain", "pardon me", "huh?", "what?") || lastUserCleaned.startsWith("i dont understand") || lastUserCleaned.startsWith("i don't understand")
        val isContinuation = lastUserCleaned in listOf("go on", "continue", "and", "so", "then", "tell me more", "next", "go ahead")
        val isAcknowledgment = lastUserCleaned in listOf("ok", "okay", "yes", "yeah", "sure", "cool", "fine", "great", "awesome", "good", "no", "nope", "not really", "never", "i see")

        val topicLower = topic.lowercase().trim()
        val isFreeTalk = topicLower == "free talk"

        val responseText = when {
            isGreeting -> {
                if (isFreeTalk) {
                    "Hello there! It is wonderful to talk with you. How has your day been so far, or is there any specific topic you'd like to chat about today?"
                } else {
                    "Hello! Let's get started with our practice on **$topic**. " + getTopicQuestion(topic, 0, lastUserMessage)
                }
            }
            isHowAreYou -> {
                if (isFreeTalk) {
                    "I'm doing great, thank you so much for asking! It is a pleasure to practice English with you today. How are you doing? What have you been up to?"
                } else {
                    "I'm doing wonderful, thank you for asking! Ready to focus on our **$topic** practice? " + getTopicQuestion(topic, userMessageCount.coerceAtLeast(1) - 1, lastUserMessage)
                }
            }
            isClarification -> {
                if (isFreeTalk) {
                    "Oh, my apologies if I was unclear! Since this is a Free Talk session, we can chat about absolutely anything you'd like today. We can talk about your favorite things, travel, hobbies, or how your day is going! What's on your mind?"
                } else {
                    val currentQuestion = getTopicQuestion(topic, (userMessageCount - 1).coerceAtLeast(0), lastUserMessage)
                    "Oh, sorry if that was a bit confusing! Let me rephrase. Since we are practicing **$topic**, my question for you was: $currentQuestion"
                }
            }
            isContinuation -> {
                if (isFreeTalk) {
                    "I'd love to! Tell me, what's a favorite hobby of yours, or something fun you like to do on the weekends?"
                } else {
                    val currentQuestion = getTopicQuestion(topic, userMessageCount.coerceAtLeast(0), lastUserMessage)
                    "Great, let's keep going! $currentQuestion"
                }
            }
            isAcknowledgment -> {
                if (isFreeTalk) {
                    when {
                        lastUserCleaned in listOf("yes", "yeah", "sure", "ok", "okay") -> {
                            "Awesome! Tell me, what is your favorite way to relax or spend a day off?"
                        }
                        lastUserCleaned in listOf("no", "nope", "not really", "never") -> {
                            "No problem at all! Let's talk about something else. What kind of music, movies, or books do you enjoy?"
                        }
                        else -> {
                            "Great! Tell me, what is your favorite movie, TV show, or book that you've experienced recently?"
                        }
                    }
                } else {
                    val currentQuestion = getTopicQuestion(topic, userMessageCount.coerceAtLeast(0), lastUserMessage)
                    "Perfect! $currentQuestion"
                }
            }
            lastUserCleaned in listOf("thank you", "thanks", "thanks a lot", "thank you so much") -> {
                "You are very welcome! I'm absolutely delighted to help you practice and improve your English communication. " + (if (isFreeTalk) "What would you like to discuss next?" else "Let's keep going. " + getTopicQuestion(topic, userMessageCount.coerceAtLeast(0), lastUserMessage))
            }
            lastUserCleaned in listOf("idk", "i don't know", "i dont know", "not sure", "no idea") -> {
                if (isFreeTalk) {
                    "No worries at all! It's completely natural to take a moment to think. Let's talk about travel! If you could travel to any country right now, where would you go?"
                } else {
                    val currentQuestion = getTopicQuestion(topic, (userMessageCount - 1).coerceAtLeast(0), lastUserMessage)
                    "No problem! Let's take it step by step. Tell me what comes to mind when you think about: $currentQuestion"
                }
            }
            else -> {
                if (isFreeTalk) {
                    val weatherKeywords = listOf("weather", "rain", "sunny", "hot", "cold", "snow", "degrees", "summer", "winter", "cloudy", "forecast")
                    val familyKeywords = listOf("family", "friend", "friends", "parents", "brother", "sister", "wife", "husband", "children", "kid", "son", "daughter")
                    val mediaKeywords = listOf("music", "song", "band", "movie", "film", "book", "read", "novel", "art", "painting", "actor", "watch", "singer")
                    val foodKeywords = listOf("food", "cooking", "eat", "restaurant", "recipe", "pizza", "burger", "cuisine", "chef", "dinner", "lunch")
                    val techKeywords = listOf("tech", "computer", "ai", "coding", "programming", "software", "gpt", "gemini", "phone", "internet")
                    val sportKeywords = listOf("sport", "football", "soccer", "basketball", "gym", "run", "workout", "tennis", "game", "team")
                    val travelKeywords = listOf("travel", "trip", "vacation", "country", "europe", "asia", "america", "flight", "visit", "explore", "holiday")

                    when {
                        weatherKeywords.any { lastUserMessage.contains(it) } -> {
                            "I always find that the weather affects my daily plans and mood! Is it nice and pleasant where you are right now, or are you hoping for a change in the season?"
                        }
                        familyKeywords.any { lastUserMessage.contains(it) } -> {
                            "Spending quality time with family and close friends is so vital. Do you have any plans to meet up with them soon, or do you live close by?"
                        }
                        mediaKeywords.any { lastUserMessage.contains(it) } -> {
                            "Art, movies, and music are such wonderful ways to connect, unwind, and express ourselves! What is your absolute favorite piece of media (like a song, movie, or book) that you'd recommend to everyone?"
                        }
                        foodKeywords.any { lastUserMessage.contains(it) } -> {
                            "That sounds delicious! I love exploring different cuisines. Do you prefer cooking at home with recipes, or do you enjoy finding new local restaurants to try?"
                        }
                        techKeywords.any { lastUserMessage.contains(it) } -> {
                            "Technology is evolving so rapidly, and software/AI development is at the absolute heart of it! Are you working on any personal tech projects or learning new digital skills recently?"
                        }
                        sportKeywords.any { lastUserMessage.contains(it) } -> {
                            "Staying active is fantastic for both physical and mental well-being! Do you play competitively or follow a specific sport, or do you do it purely for recreation and health?"
                        }
                        travelKeywords.any { lastUserMessage.contains(it) } -> {
                            "Exploring new places and experiencing different cultures is one of life's greatest joys! If you could travel anywhere in the world right now with no budget, where would you go?"
                        }
                        else -> {
                            when (userMessageCount) {
                                0 -> "Hello! Welcome to our Free Talk session. We can discuss anything you'd like today. How has your day been so far, or is there a specific topic you want to chat about?"
                                1 -> "That sounds really interesting! Can you tell me a bit more about that, or what made you think about it?"
                                2 -> "I completely agree with you. In your opinion, what's the most important aspect or element of that?"
                                3 -> "Interesting! Many people feel exactly the same way. By the way, what do you usually like to do in your free time when you want to relax?"
                                4 -> "That's wonderful! What got you interested in that originally, and how often do you get to do it?"
                                else -> "That is super fascinating. I'm really enjoying our conversation! What other aspects would you like to explore together?"
                            }
                        }
                    }
                } else {
                    getTopicQuestion(topic, userMessageCount, lastUserMessage)
                }
            }
        }

        // Stream the response with a tiny delay to simulate real AI typing!
        val words = responseText.split(" ")
        for ((index, word) in words.withIndex()) {
            val chunk = if (index < words.size - 1) "$word " else word
            onChunk(chunk)
            kotlinx.coroutines.delay(50) // 50ms delay per word for a smooth simulation!
        }
    }

    private fun getDemoCorrections(text: String): CustomCorrectionResponse {
        val trimText = text.trim()
        if (trimText.isEmpty()) return CustomCorrectionResponse(text)

        val explanations = mutableListOf<CorrectionExplanation>()
        var corrected = trimText

        // 1. Standalone lowercase 'i'
        val iPattern = Regex("\\bi\\b")
        if (iPattern.containsMatchIn(corrected)) {
            corrected = corrected.replace(iPattern, "I")
            explanations.add(
                CorrectionExplanation(
                    original = "i",
                    fixed = "I",
                    reason = "The personal pronoun 'I' should always be capitalized."
                )
            )
        }

        // 2. Common contractions without apostrophe
        val contractions = mapOf(
            "dont" to "don't",
            "cant" to "can't",
            "im" to "I'm",
            "youre" to "you're",
            "didnt" to "didn't",
            "isnt" to "isn't",
            "arent" to "aren't",
            "theyre" to "they're",
            "weve" to "we've",
            "hes" to "he's",
            "shes" to "she's",
            "its" to "it's"
        )
        for ((wrong, right) in contractions) {
            val pattern = Regex("\\b$wrong\\b", RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(corrected)) {
                corrected = corrected.replace(pattern, right)
                explanations.add(
                    CorrectionExplanation(
                        original = wrong,
                        fixed = right,
                        reason = "Missing apostrophe in the contraction '$right'."
                    )
                )
            }
        }

        // 3. Double spaces
        if (corrected.contains("  ")) {
            corrected = corrected.replace(Regex(" +"), " ")
            explanations.add(
                CorrectionExplanation(
                    original = "  ",
                    fixed = " ",
                    reason = "Remove duplicate spaces for clean formatting."
                )
            )
        }

        // 4. Capitalization of first letter
        if (corrected.isNotEmpty() && corrected[0].isLowerCase()) {
            val originalFirst = corrected[0].toString()
            val fixedFirst = corrected[0].uppercaseChar().toString()
            corrected = fixedFirst + corrected.substring(1)
            explanations.add(
                CorrectionExplanation(
                    original = originalFirst,
                    fixed = fixedFirst,
                    reason = "The first letter of a sentence should be capitalized."
                )
            )
        }

        // 5. Ending punctuation
        if (corrected.isNotEmpty() && !corrected.endsWith(".") && !corrected.endsWith("?") && !corrected.endsWith("!")) {
            val originalEnd = corrected.takeLast(1)
            corrected += "."
            explanations.add(
                CorrectionExplanation(
                    original = originalEnd,
                    fixed = originalEnd + ".",
                    reason = "Remember to end your sentences with a proper punctuation mark (., ?, !)."
                )
            )
        }

        return CustomCorrectionResponse(corrected, explanations)
    }
}
