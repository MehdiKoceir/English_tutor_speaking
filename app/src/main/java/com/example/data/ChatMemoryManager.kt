package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ChatMemoryManager handles dialogue context optimization using a sliding window approach.
 * It retains the last 10 turns (20 messages) of conversation context and tracks the current
 * topic state to ensure continuity and prevent model memory overflow or API token exhaustion.
 */
class ChatMemoryManager(private val maxTurns: Int = 10) {
    private val TAG = "ChatMemoryManager"

    // Tracks the current active topic state
    private val _currentTopicState = MutableStateFlow<String>("")
    val currentTopicState: StateFlow<String> = _currentTopicState.asStateFlow()

    /**
     * Updates the current topic state to ensure continuity.
     */
    fun updateTopic(topic: String) {
        _currentTopicState.value = topic
    }

    /**
     * Gets the current tracked topic state, with a fallback to a default topic.
     */
    fun getCurrentTopic(defaultTopic: String): String {
        val current = _currentTopicState.value
        return if (current.isNotEmpty()) current else defaultTopic
    }

    /**
     * Trims a list of messages to keep only the last [maxTurns] turns of dialogue.
     * Each complete turn of conversation typically consists of a user query and a tutor response.
     * Therefore, a sliding window of [maxTurns] turns translates to the last [maxTurns * 2] messages.
     */
    fun getSlidingWindow(messages: List<ChatMessage>): List<ChatMessage> {
        val maxMessages = maxTurns * 2
        if (messages.size <= maxMessages) {
            return messages
        }

        // Apply sliding window: take the last N messages
        val trimmed = messages.takeLast(maxMessages)
        return trimmed
    }
}
