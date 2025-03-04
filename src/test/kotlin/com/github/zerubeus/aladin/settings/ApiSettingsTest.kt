package com.github.zerubeus.aladin.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ApiSettingsTest : BasePlatformTestCase() {

    private lateinit var apiSettingsState: ApiSettingsState
    private lateinit var apiSettingsPanel: ApiSettingsPanel

    override fun setUp() {
        super.setUp()
        apiSettingsState = ApiSettingsState()
        apiSettingsState.apiProvider = ApiProvider.ANTHROPIC // Start with ANTHROPIC
        apiSettingsPanel = ApiSettingsPanel(apiSettingsState)
    }

    fun testSettingsArePersistedCorrectly() {
        // Test API key persistence
        val testApiKey = "test-api-key-123456"
        apiSettingsPanel.apiKeyField.text = testApiKey
        
        assertTrue("Settings should be modified after changing API key", apiSettingsPanel.isModified(apiSettingsState))
        
        apiSettingsPanel.apply(apiSettingsState)
        assertEquals("API key should be saved in settings state", testApiKey, apiSettingsState.apiKey)
        
        // Should not be modified after apply
        assertFalse("Settings should not be modified after apply", apiSettingsPanel.isModified(apiSettingsState))
    }
    
    fun testApiProviderSelection() {
        // Test provider selection persistence
        // Make sure we're changing to a different provider than what's in the state
        assertNotEquals("Initial provider should not be OPENAI for this test", 
            ApiProvider.OPENAI, apiSettingsState.apiProvider)
            
        // Now set to OPENAI (different from ANTHROPIC)
        apiSettingsPanel.providerComboBox.selectedItem = ApiProvider.OPENAI
        
        assertTrue("Settings should be modified after changing provider", 
            apiSettingsPanel.isModified(apiSettingsState))
        
        apiSettingsPanel.apply(apiSettingsState)
        assertEquals("Provider should be saved in settings state", 
            ApiProvider.OPENAI, apiSettingsState.apiProvider)
    }
    
    fun testSecureApiKeyStorage() {
        // The actual API key should never be stored in plain text or logged
        val testApiKey = "test-api-key-secure"
        apiSettingsPanel.apiKeyField.text = testApiKey
        apiSettingsPanel.apply(apiSettingsState)
        
        // The key should be stored in the secure storage, not directly in the state
        assertNotEquals("API key should not be stored in plain text", testApiKey, apiSettingsState.apiKeyPlainText)
        // The key should be retrievable
        assertEquals("API key should be retrievable from secure storage", testApiKey, apiSettingsState.getDecryptedApiKey())
    }
} 