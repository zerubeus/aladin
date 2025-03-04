package com.github.zerubeus.aladin.settings

import com.intellij.testFramework.LightPlatformTestCase

class ApiSecurityTest : LightPlatformTestCase() {

    private lateinit var apiSettingsState: ApiSettingsState
    
    override fun setUp() {
        super.setUp()
        apiSettingsState = ApiSettingsState()
    }
    
    fun testApiKeyIsStoredSecurely() {
        // Store a test API key
        val testKey = "test-api-key-123456"
        apiSettingsState.setApiKey(testKey)
        
        // Verify the key is not stored directly in the state
        assertNotSame("API key should not be stored directly in the state", 
            testKey, apiSettingsState.apiKeyPlainText)
        
        // Verify the key is retrievable from secure storage
        assertEquals("API key should be retrievable from secure storage", 
            testKey, apiSettingsState.getDecryptedApiKey())
    }
    
    fun testApiKeyIsNeverLoggedOrExposed() {
        val testKey = "test-secret-key-789012"
        apiSettingsState.setApiKey(testKey)
        
        // Key should be masked in the state
        assertTrue("API key should be masked or empty in plain text state", 
            apiSettingsState.apiKeyPlainText.isEmpty() || 
            apiSettingsState.apiKeyPlainText.matches(Regex("\\*+")))
            
        // Key should not appear in toString()
        val stateString = apiSettingsState.toString()
        assertFalse("API key should not appear in toString()", 
            stateString.contains(testKey))
    }
    
    fun testDifferentProvidersHaveDifferentStoredKeys() {
        // Set key for OpenAI
        apiSettingsState.apiProvider = ApiProvider.OPENAI
        val openAiKey = "openai-key-123"
        apiSettingsState.setApiKey(openAiKey)
        
        // Verify OpenAI key is stored correctly
        assertEquals("OpenAI key should be retrievable", 
            openAiKey, apiSettingsState.getDecryptedApiKey())
            
        // Change provider to Anthropic and set different key
        apiSettingsState.apiProvider = ApiProvider.ANTHROPIC
        val anthropicKey = "anthropic-key-456"
        apiSettingsState.setApiKey(anthropicKey)
        
        // Verify Anthropic key is stored correctly
        assertEquals("Anthropic key should be retrievable", 
            anthropicKey, apiSettingsState.getDecryptedApiKey())
            
        // Change back to OpenAI and verify its key is still there
        apiSettingsState.apiProvider = ApiProvider.OPENAI
        assertEquals("Original OpenAI key should still be accessible", 
            openAiKey, apiSettingsState.getDecryptedApiKey())
    }
    
    fun testTokenLimitTracking() {
        // Start with zero tokens
        apiSettingsState.resetTokenUsage()
        assertEquals("Token usage should start at zero", 0, apiSettingsState.tokenUsage)
        
        // Record some token usage
        apiSettingsState.recordTokenUsage(100)
        assertEquals("Token usage should be tracked correctly", 100, apiSettingsState.tokenUsage)
        
        // Record more token usage
        apiSettingsState.recordTokenUsage(150)
        assertEquals("Token usage should be cumulative", 250, apiSettingsState.tokenUsage)
        
        // Check if approaching limit works
        apiSettingsState.dailyTokenLimit = 300
        assertTrue("Should detect approaching limit", apiSettingsState.isApproachingLimit())
        
        // Increase limit and check again
        apiSettingsState.dailyTokenLimit = 1000
        assertFalse("Should not detect approaching limit after increasing it", 
            apiSettingsState.isApproachingLimit())
    }
} 