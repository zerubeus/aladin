package com.github.zerubeus.aladin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface defining the contract for all LLM provider services.
 * This abstraction allows for multiple providers to be used interchangeably.
 */
interface LlmProviderService {
    /**
     * Sends a message to the LLM provider and returns the response.
     * 
     * @param userMessage The message from the user
     * @return The AI's response
     * @throws Exception if there's an error communicating with the API
     */
    suspend fun sendMessage(userMessage: String): String
    
    /**
     * Gets the name of the currently selected model for this provider.
     * 
     * @return The model name
     */
    fun getCurrentModel(): String
    
    /**
     * Gets the available models for this provider.
     * 
     * @return List of available model names
     */
    suspend fun getAvailableModels(): List<String>
    
    /**
     * Validates the connection and credentials for this provider.
     * 
     * @return True if connection is valid, false otherwise
     */
    suspend fun validateConnection(): Boolean
}

/**
 * Factory service that provides the appropriate LLM provider based on configuration.
 */
@Service(Service.Level.APP)
class LlmProviderFactory {
    /**
     * Gets the appropriate LLM provider service based on the current settings.
     * 
     * @return The configured LLM provider service
     */
    fun getProvider(): LlmProviderService {
        val settings = service<com.github.zerubeus.aladin.settings.ApiSettingsState>()
        return when (settings.apiProvider) {
            com.github.zerubeus.aladin.settings.ApiProvider.OPENAI -> service<OpenAiService>()
            com.github.zerubeus.aladin.settings.ApiProvider.OLLAMA -> service<OllamaService>()
            else -> service<OpenAiService>() // Default to OpenAI for now
        }
    }
} 