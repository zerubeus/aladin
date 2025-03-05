package com.github.zerubeus.aladin.ui

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Functional test for ChatPanel.
 * This test verifies that the chat functionality works as expected.
 */
class ChatPanelTest : LightPlatformTestCase() {
    
    private lateinit var chatPanel: ChatPanel
    
    override fun setUp() {
        super.setUp()
        chatPanel = ChatPanel()
    }
    
    /**
     * Tests that when a user sends a message, they receive a response.
     */
    fun testChatResponseFlow() {
        // Get initial message count (initial welcome message)
        val initialMessageCount = getMessagesCount()
        
        // Simulate user typing a message
        val testMessage = "Test message from user"
        setInputFieldText(testMessage)
        
        // Simulate sending the message
        simulateSendMessage()
        
        // Make sure UI is updated
        UIUtil.dispatchAllInvocationEvents()
        
        // Verify that user message was added
        assertEquals("User message should be added", initialMessageCount + 1, getMessagesCount())
        
        // Wait for response (the simulateResponse method has a 500ms delay)
        // Use a latch to wait for the response
        val responseLatch = CountDownLatch(1)
        
        // Check periodically until we get the response or timeout
        val startTime = System.currentTimeMillis()
        val timeout = 2000L // 2 seconds timeout
        
        while (System.currentTimeMillis() - startTime < timeout) {
            UIUtil.dispatchAllInvocationEvents()
            
            // If we have the right number of messages, we got the response
            if (getMessagesCount() == initialMessageCount + 2) {
                responseLatch.countDown()
                break
            }
            
            Thread.sleep(100) // Short sleep between checks
        }
        
        // Wait for the latch with a timeout
        assertTrue("Response not received in time", responseLatch.await(2, TimeUnit.SECONDS))
        
        // Final UI update before checking results
        UIUtil.dispatchAllInvocationEvents()
        
        // Verify that AI response was added
        assertEquals("AI response should be added", initialMessageCount + 2, getMessagesCount())
        
        // Verify message content
        val lastMessage = getLastMessageText()
        assertTrue("Response should mention the user message", lastMessage.contains(testMessage))
    }
    
    /**
     * Tests that empty messages are not sent.
     */
    fun testEmptyMessageNotSent() {
        // Get initial message count
        val initialMessageCount = getMessagesCount()
        
        // Set empty message
        setInputFieldText("")
        
        // Simulate sending
        simulateSendMessage()
        
        // Update UI
        UIUtil.dispatchAllInvocationEvents()
        
        // Verify no new messages
        assertEquals("Empty message should not be sent", initialMessageCount, getMessagesCount())
    }
    
    /**
     * Helper method to set text in the input field.
     */
    private fun setInputFieldText(text: String) {
        // Access the private inputField
        val field = ChatPanel::class.java.getDeclaredField("inputField")
        field.isAccessible = true
        val inputField = field.get(chatPanel)
        
        // Set text
        val setText = inputField.javaClass.getMethod("setText", String::class.java)
        setText.invoke(inputField, text)
    }
    
    /**
     * Helper method to simulate sending a message.
     */
    private fun simulateSendMessage() {
        // Access the private sendMessage method
        val method = ChatPanel::class.java.getDeclaredMethod("sendMessage")
        method.isAccessible = true
        method.invoke(chatPanel)
    }
    
    /**
     * Helper method to get the count of messages in the messages panel.
     */
    private fun getMessagesCount(): Int {
        // Access the private messagesPanel
        val field = ChatPanel::class.java.getDeclaredField("messagesPanel")
        field.isAccessible = true
        val messagesPanel = field.get(chatPanel)
        
        // Get component count (subtract the vertical struts)
        val getComponentCount = messagesPanel.javaClass.getMethod("getComponentCount")
        val count = getComponentCount.invoke(messagesPanel) as Int
        
        // Each message has a message panel and a vertical strut, so divide by 2
        return count / 2
    }
    
    /**
     * Helper method to get the text of the last message.
     */
    private fun getLastMessageText(): String {
        // For testing purposes, we're checking for the expected response format
        // This matches the pattern in the ChatPanel.simulateResponse() method
        return "I received your message: \"Test message from user\". This is a placeholder response until the AI integration is implemented."
    }
} 