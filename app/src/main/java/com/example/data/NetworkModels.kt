package com.example.data

import com.squareup.moshi.JsonClass

// --- Custom VPS Ollama Proxy Chat Models ---

@JsonClass(generateAdapter = true)
data class CustomChatRequest(
    val conversation_id: String,
    val message: String,
    val level: String
)

@JsonClass(generateAdapter = true)
data class CorrectionExplanation(
    val original: String,
    val fixed: String,
    val reason: String
)

@JsonClass(generateAdapter = true)
data class CustomCorrectionResponse(
    val corrected: String,
    val explanations: List<CorrectionExplanation> = emptyList(),
    val corrections: List<CorrectionExplanation> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CustomCorrectionRequest(
    val text: String
)


// --- Gemini Direct API Models (from gemini-api skill) ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponseFormat(
    val responseMimeType: String? = null,
    val responseSchema: GeminiResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponseSchema(
    val type: String,
    val description: String? = null,
    val properties: Map<String, GeminiSchemaProperty>? = null,
    val required: List<String>? = null,
    val items: GeminiSchemaItems? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSchemaProperty(
    val type: String,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSchemaItems(
    val type: String,
    val properties: Map<String, GeminiSchemaProperty>? = null,
    val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseFormat: GeminiResponseFormat? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)
