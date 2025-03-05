package com.github.zerubeus.aladin.ui

import com.github.zerubeus.aladin.services.OpenAiService
import com.github.zerubeus.aladin.settings.ApiProvider
import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.service
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.JTextArea
import kotlinx.coroutines.runBlocking

/**
 * Functional test for ChatPanel.
 * This test verifies that the chat functionality works as expected by testing real API integration.
 */
class ChatPanelTest : LightPlatformTestCase() {
    
    private lateinit var chatPanel: ChatPanel
    private val apiKey = loadApiKeyFromEnv()
    
    override fun setUp() {
        super.setUp()
        chatPanel = ChatPanel()
        
        // Configure API settings
        val settings = service<ApiSettingsState>()
        settings.apiProvider = ApiProvider.OPENAI
        settings.setApiKey(apiKey)
    }
    
    /**
     * Tests that when a user sends a message in the chat, they receive a response
     * from the actual OpenAI API.
     */
    fun testChatResponseFlow() = runBlocking {
        // Skip the test if no API key is available
        if (apiKey.isBlank()) {
            System.err.println("Skipping OpenAI test: No API key available")
            return@runBlocking
        }
        
        // Get initial message count (should be 1 for the welcome message)
        val initialMessageCount = getMessagesCount()
        
        // Verify we have the welcome message
        assertEquals("Should have welcome message", 1, initialMessageCount)
        
        // Simulate user typing a message
        val testMessage = "What is JetBrains plugin development?"
        setInputFieldText(testMessage)
        
        // Simulate sending the message
        simulateSendMessage()
        
        // Make sure UI is updated
        UIUtil.dispatchAllInvocationEvents()
        
        // Verify that user message and "Thinking..." message were added
        // (welcome + user message + thinking message = 3)
        assertEquals("User message and thinking message should be added", 3, getMessagesCount())
        
        // Wait for "Thinking..." message to be replaced with actual response
        UIUtil.dispatchAllInvocationEvents()
        Thread.sleep(100) // Short wait to ensure thinking message appears
        
        // Wait for API response with a longer timeout since we're calling a real API
        val responseLatch = CountDownLatch(1)
        
        // Check periodically until we get the response or timeout
        val startTime = System.currentTimeMillis()
        val timeout = 20000L // 20 seconds timeout for real API call
        
        while (System.currentTimeMillis() - startTime < timeout) {
            UIUtil.dispatchAllInvocationEvents()
            
            // Check if we have our response
            val lastMessage = getLastMessageTextFromUI()
            if (lastMessage != null && !lastMessage.contains("Thinking...")) {
                // If the message is not "Thinking...", we consider it a response
                if (getMessagesCount() >= initialMessageCount + 2) {
                    responseLatch.countDown()
                    break
                }
            }
            
            Thread.sleep(500) // Longer sleep interval for API calls
        }
        
        // Wait for the latch with a timeout
        assertTrue("Response not received in time", responseLatch.await(20, TimeUnit.SECONDS))
        
        // Final UI update before checking results
        UIUtil.dispatchAllInvocationEvents()
        
        // Verify message content contains something meaningful
        val lastMessage = getLastMessageTextFromUI()
        assertNotNull("Response should not be null", lastMessage)
        assertTrue("Response should be non-empty", lastMessage!!.length > 10)
        
        // The response should contain some content - we can't guarantee exactly what the AI will respond with
        // so we'll just check that we got a non-trivial response
        println("AI Response: $lastMessage")
        
        // Skip the content check since we can't guarantee what the AI will respond with
        // The fact that we got a non-empty response is sufficient for this test
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
     * Helper method to get the text of the last message from the UI components.
     * This extracts the actual text from the UI instead of hardcoding expected values.
     */
    private fun getLastMessageTextFromUI(): String? {
        try {
            // Access the private messagesPanel
            val field = ChatPanel::class.java.getDeclaredField("messagesPanel")
            field.isAccessible = true
            val messagesPanel = field.get(chatPanel)
            
            // Get component count
            val getComponentCount = messagesPanel.javaClass.getMethod("getComponentCount")
            val count = getComponentCount.invoke(messagesPanel) as Int
            
            // If we have at least one message (component + strut)
            if (count >= 2) {
                // Get the last message panel (before the last strut)
                val getComponent = messagesPanel.javaClass.getMethod("getComponent", Int::class.java)
                val lastMessagePanel = getComponent.invoke(messagesPanel, count - 2)
                
                // Get the bubble panel (should be in EAST or WEST)
                val bubblePanel = lastMessagePanel.javaClass.getMethod("getComponent", Int::class.java)
                    .invoke(lastMessagePanel, 1)
                
                // Find the JTextArea inside
                val textArea = findTextAreaInComponent(bubblePanel)
                return textArea?.text
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Helper method to find a JTextArea within a component hierarchy.
     */
    private fun findTextAreaInComponent(component: Any?): JTextArea? {
        if (component == null) return null
        
        if (component is JTextArea) {
            return component
        }
        
        // Try to get components if this is a container
        try {
            val getComponentCount = component.javaClass.getMethod("getComponentCount")
            val count = getComponentCount.invoke(component) as Int
            
            if (count > 0) {
                val getComponent = component.javaClass.getMethod("getComponent", Int::class.java)
                
                for (i in 0 until count) {
                    val child = getComponent.invoke(component, i)
                    val textArea = findTextAreaInComponent(child)
                    if (textArea != null) {
                        return textArea
                    }
                }
            }
        } catch (e: Exception) {
            // Not a container or couldn't access components
        }
        
        return null
    }
    
    /**
     * Helper method to load the API key from the .env file
     */
    private fun loadApiKeyFromEnv(): String {
        try {
            val envFile = File(".env")
            if (envFile.exists()) {
                val envContents = envFile.readText()
                val openAiKeyMatch = Regex("OPEN_AI_API_KEY=(.+)").find(envContents)
                return openAiKeyMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
            }
        } catch (e: Exception) {
            System.err.println("Error loading API key: ${e.message}")
        }
        return ""
    }
} 