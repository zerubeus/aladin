package com.github.zerubeus.aladin.services

import com.github.zerubeus.aladin.settings.ApiProvider
import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.service
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking

class ApiValidationServiceTest : LightPlatformTestCase() {

    private lateinit var validationService: ApiValidationService
    
    override fun setUp() {
        super.setUp()
        validationService = ApiValidationService(project)
    }
    
    fun testEmptyApiKeyValidation() = runBlocking {
        val settings = service<ApiSettingsState>()
        // Ensure API key is empty
        settings.setApiKey("")
        
        val result = validationService.validateApiCredentials()
        assertFalse("Empty API key should be invalid", result.isValid)
        assertTrue("Error message should mention API key", 
            result.message.contains("API key is empty"))
    }
    
    fun testFormatErrorMessage() {
        // Test with different types of errors
        val unknownHostError = java.net.UnknownHostException("No such host")
        val message1 = validationService.formatErrorMessage(unknownHostError, ApiProvider.OPENAI)
        assertTrue("Should mention network error", message1.contains("Network error"))
        assertTrue("Should mention OpenAI", message1.contains("OpenAI"))
        
        val timeoutError = java.net.SocketTimeoutException("Connection timed out")
        val message2 = validationService.formatErrorMessage(timeoutError, ApiProvider.ANTHROPIC)
        assertTrue("Should mention timeout", message2.contains("Timeout"))
        assertTrue("Should mention Anthropic", message2.contains("Anthropic"))
        
        val ioError = java.io.IOException("Failed to connect")
        val message3 = validationService.formatErrorMessage(ioError, ApiProvider.AZURE_OPENAI)
        assertTrue("Should mention IO Error", message3.contains("IO Error"))
        assertTrue("Should mention Azure", message3.contains("Azure"))
        
        val genericError = Exception("Unknown error")
        val message4 = validationService.formatErrorMessage(genericError, ApiProvider.CUSTOM)
        assertTrue("Should mention error", message4.contains("Error"))
        assertTrue("Should mention custom endpoint", message4.contains("custom endpoint"))
    }
    
    // Note: For real API validation tests, we would use mocking to simulate API responses
    // But mocking is not available in this test environment
} 