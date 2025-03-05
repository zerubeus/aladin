package com.github.zerubeus.aladin.services

import com.github.zerubeus.aladin.settings.ApiProvider
import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.service
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.*

/**
 * Test for the LlmProviderService interface and factory.
 */
class LlmProviderServiceTest : LightPlatformTestCase() {
    
    private val apiKey = loadApiKeyFromEnv()
    
    override fun setUp() {
        super.setUp()
        val settings = service<ApiSettingsState>()
        
        // Set up with OpenAI for testing
        if (apiKey.isNotBlank()) {
            settings.apiProvider = ApiProvider.OPENAI
            settings.setApiKey(apiKey)
        }
    }
    
    /**
     * Tests that the factory correctly returns a provider based on settings.
     */
    fun testProviderFactory() {
        val provider = service<LlmProviderFactory>().getProvider()
        assertNotNull("Provider should not be null", provider)
        
        // Test with OpenAI
        val settings = service<ApiSettingsState>()
        settings.apiProvider = ApiProvider.OPENAI
        val openAiProvider = service<LlmProviderFactory>().getProvider()
        assertTrue("Should return OpenAiService for OPENAI provider", openAiProvider is OpenAiService)
        
        // We'll test Ollama once it's implemented
    }
    
    /**
     * Tests that the OpenAI provider can get available models.
     */
    fun testGetAvailableModels() = runBlocking {
        // Skip if no API key
        if (apiKey.isBlank()) {
            println("Skipping: No API key available")
            return@runBlocking
        }
        
        val provider = service<OpenAiService>()
        val models = provider.getAvailableModels()
        
        assertTrue("Should return a non-empty list of models", models.isNotEmpty())
        assertTrue("Should contain the default model", models.contains("gpt-3.5-turbo"))
    }
    
    /**
     * Tests that the OpenAI provider returns the correct current model.
     */
    fun testGetCurrentModel() {
        val provider = service<OpenAiService>()
        val model = provider.getCurrentModel()
        assertEquals("Should return the default model", "gpt-3.5-turbo", model)
    }
    
    /**
     * Helper method to load API key from .env file for testing
     */
    private fun loadApiKeyFromEnv(): String {
        try {
            val envFile = File(".env")
            if (!envFile.exists()) return ""
            
            envFile.readLines().forEach { line ->
                if (line.startsWith("OPENAI_API_KEY=")) {
                    return line.substringAfter("=").trim()
                }
            }
        } catch (e: Exception) {
            println("Error reading .env file: ${e.message}")
        }
        return ""
    }
} 