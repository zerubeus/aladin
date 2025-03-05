package com.github.zerubeus.aladin.ui

import com.github.zerubeus.aladin.services.LlmProviderFactory
import com.github.zerubeus.aladin.services.OllamaService
import com.github.zerubeus.aladin.services.OpenAiService
import com.github.zerubeus.aladin.settings.ApiProvider
import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.service
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.ui.UIUtil
import javax.swing.JComboBox
import org.junit.Assert.*

/**
 * Test for the ChatPanel provider selection functionality.
 */
class ChatPanelProviderTest : LightPlatformTestCase() {
    
    private lateinit var chatPanel: ChatPanel
    
    override fun setUp() {
        super.setUp()
        println("Setting up ChatPanelProviderTest")
        
        // Reset settings to default
        val settings = service<ApiSettingsState>()
        settings.apiProvider = ApiProvider.OPENAI
        
        chatPanel = ChatPanel()
    }
    
    /**
     * Tests that the provider selector is initialized with the correct provider.
     */
    fun testProviderSelectorInitialization() {
        // Get the provider selector
        val providerSelector = findProviderSelector()
        
        // Check that it's initialized with the correct provider
        assertEquals(ApiProvider.OPENAI.displayName, providerSelector.selectedItem)
        
        // Change the provider in settings
        val settings = service<ApiSettingsState>()
        settings.apiProvider = ApiProvider.OLLAMA
        
        // Create a new panel and check that it's initialized with the new provider
        val newPanel = ChatPanel()
        val newSelector = findProviderSelector(newPanel)
        assertEquals(ApiProvider.OLLAMA.displayName, newSelector.selectedItem)
    }
    
    /**
     * Tests that changing the provider in the selector updates the settings.
     */
    fun testProviderSelectorChangesSettings() {
        // Get the provider selector
        val providerSelector = findProviderSelector()
        
        // Change the selected provider
        UIUtil.dispatchAllInvocationEvents()
        providerSelector.selectedItem = ApiProvider.OLLAMA.displayName
        UIUtil.dispatchAllInvocationEvents()
        
        // Check that the settings were updated
        val settings = service<ApiSettingsState>()
        assertEquals(ApiProvider.OLLAMA, settings.apiProvider)
    }
    
    /**
     * Tests that the factory returns the correct provider based on the selection.
     */
    fun testProviderFactoryReturnsCorrectProvider() {
        // Set the provider to OpenAI
        val settings = service<ApiSettingsState>()
        settings.apiProvider = ApiProvider.OPENAI
        
        // Check that the factory returns an OpenAI provider
        var provider = service<LlmProviderFactory>().getProvider()
        assertTrue(provider is OpenAiService)
        
        // Set the provider to Ollama
        settings.apiProvider = ApiProvider.OLLAMA
        
        // Check that the factory returns an Ollama provider
        provider = service<LlmProviderFactory>().getProvider()
        assertTrue(provider is OllamaService)
    }
    
    /**
     * Helper method to find the provider selector in the panel.
     */
    private fun findProviderSelector(panel: ChatPanel = chatPanel): JComboBox<*> {
        // Find the provider selector
        for (component in panel.components) {
            if (component is JComboBox<*>) {
                return component
            }
            
            if (component is java.awt.Container) {
                for (child in component.components) {
                    if (child is JComboBox<*>) {
                        return child
                    }
                    
                    if (child is java.awt.Container) {
                        for (grandchild in child.components) {
                            if (grandchild is JComboBox<*>) {
                                return grandchild
                            }
                        }
                    }
                }
            }
        }
        
        throw IllegalStateException("Provider selector not found")
    }
} 