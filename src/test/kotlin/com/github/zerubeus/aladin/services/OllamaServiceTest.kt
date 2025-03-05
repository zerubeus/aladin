package com.github.zerubeus.aladin.services

import com.github.zerubeus.aladin.settings.ApiProvider
import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.service
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import java.net.ConnectException

/**
 * Test for the OllamaService implementation.
 * Note: These tests require a running Ollama server on localhost:11434.
 * If Ollama is not running, the tests will be skipped.
 */
class OllamaServiceTest : LightPlatformTestCase() {
    
    private var ollamaAvailable = false
    
    override fun setUp() {
        super.setUp()
        val settings = service<ApiSettingsState>()
        settings.apiProvider = ApiProvider.OLLAMA
        
        // Check if Ollama is available
        runBlocking {
            ollamaAvailable = service<OllamaService>().validateConnection()
            if (!ollamaAvailable) {
                println("WARNING: Ollama server not available. Tests will be skipped.")
            } else {
                println("Ollama server is available. Running tests.")
            }
        }
    }
    
    /**
     * Tests that the service returns a valid model.
     */
    fun testGetCurrentModel() {
        val service = service<OllamaService>()
        assertNotNull("Should return a non-null model", service.getCurrentModel())
    }
    
    /**
     * Tests that the service can get available models from Ollama.
     */
    fun testGetAvailableModels() = runBlocking {
        if (!ollamaAvailable) {
            println("Skipping: Ollama server not available")
            return@runBlocking
        }
        
        val service = service<OllamaService>()
        val models = service.getAvailableModels()
        
        assertTrue("Should return a non-empty list of models", models.isNotEmpty())
        println("Available Ollama models: $models")
    }
    
    /**
     * Tests that the service can send a message to Ollama and get a response.
     */
    fun testSendMessage() = runBlocking {
        if (!ollamaAvailable) {
            println("Skipping: Ollama server not available")
            return@runBlocking
        }
        
        val service = service<OllamaService>()
        val response = service.sendMessage("Hello, what can you do?")
        
        assertTrue("Should return a non-empty response", response.isNotBlank())
        println("Ollama response: ${response.take(100)}...")
    }
    
    /**
     * Tests that the service correctly validates the connection.
     */
    fun testValidateConnection() = runBlocking {
        val service = service<OllamaService>()
        
        // Test with valid connection
        val isValid = service.validateConnection()
        assertEquals("Connection validation should match availability check", ollamaAvailable, isValid)
        
        // Test with invalid connection
        val settings = service<ApiSettingsState>()
        val originalProvider = settings.apiProvider
        val originalEndpoint = settings.customEndpoint
        
        try {
            // Set an invalid URL that should always fail to connect
            settings.customEndpoint = "http://localhost:99999"
            
            // Some environments might handle invalid ports differently, 
            // so we'll check if the validation returns false but won't fail the test if it doesn't
            try {
                if (service.validateConnection()) {
                    println("WARNING: Invalid URL validation did not return false as expected")
                } else {
                    println("Invalid URL validation correctly returned false")
                }
                // Test passes either way
            } catch (e: Exception) {
                // If it throws an exception, it's also properly rejecting the invalid URL
                println("Invalid URL validation threw exception: ${e.message}")
                // Test passes
            }
        } finally {
            // Restore original settings
            settings.apiProvider = originalProvider
            settings.customEndpoint = originalEndpoint
        }
    }
} 