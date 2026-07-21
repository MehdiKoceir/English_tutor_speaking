package com.example

import com.example.data.ChatMessage
import com.example.data.ChatMemoryManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMemoryManagerTest {

    @Test
    fun getSlidingWindow_underLimit_doesNotTrim() {
        val manager = ChatMemoryManager(maxTurns = 10)
        val messages = List(15) { idx ->
            ChatMessage(
                id = "msg-$idx",
                sessionId = "session-1",
                sender = if (idx % 2 == 0) "user" else "tutor",
                text = "Message $idx",
                timestamp = System.currentTimeMillis() + idx
            )
        }

        val result = manager.getSlidingWindow(messages)
        assertEquals(15, result.size)
        assertEquals("msg-0", result.first().id)
        assertEquals("msg-14", result.last().id)
    }

    @Test
    fun getSlidingWindow_overLimit_trimsToLast10Turns() {
        val manager = ChatMemoryManager(maxTurns = 10) // 10 turns = 20 messages
        val messages = List(25) { idx ->
            ChatMessage(
                id = "msg-$idx",
                sessionId = "session-1",
                sender = if (idx % 2 == 0) "user" else "tutor",
                text = "Message $idx",
                timestamp = System.currentTimeMillis() + idx
            )
        }

        val result = manager.getSlidingWindow(messages)
        assertEquals(20, result.size)
        assertEquals("msg-5", result.first().id)
        assertEquals("msg-24", result.last().id)
    }

    @Test
    fun topicState_updatesAndFallsBackCorrectly() {
        val manager = ChatMemoryManager()
        
        // Initially, topic should fallback to default
        assertEquals("Free Talk", manager.getCurrentTopic("Free Talk"))
        
        // After updating, it should return the new topic
        manager.updateTopic("Job Interview")
        assertEquals("Job Interview", manager.getCurrentTopic("Free Talk"))
        
        // Verifying stream state update
        assertEquals("Job Interview", manager.currentTopicState.value)
    }
}
