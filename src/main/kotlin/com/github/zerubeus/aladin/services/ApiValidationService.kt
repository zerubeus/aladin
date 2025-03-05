package com.github.zerubeus.aladin.services

import com.github.zerubeus.aladin.settings.ApiProvider
import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for validating API credentials and handling API-related errors.
 */
@Service(Service.Level.PROJECT)
class ApiValidationService(private val project: Project) {
    
    private val logger = logger<ApiValidationService>()
    
    /**
     * Validation result with status and message
     */
    data class ValidationResult(val isValid: Boolean, val message: String)
    
    /**
     * Validates the current API credentials by making a minimal API call.
     * @return a ValidationResult with the status and a message
     */
    suspend fun validateApiCredentials(): ValidationResult = withContext(Dispatchers.IO) {
        val settings = service<ApiSettingsState>()
        val apiKey = settings.getDecryptedApiKey()
        
        if (apiKey.isBlank()) {
            return@withContext ValidationResult(false, "API key is empty. Please configure in Settings.")
        }
        
        return@withContext when (settings.apiProvider) {
            ApiProvider.OPENAI -> validateOpenAiKey(apiKey)
            ApiProvider.ANTHROPIC -> validateAnthropicKey(apiKey)
            ApiProvider.AZURE_OPENAI -> validateAzureOpenAiKey(apiKey, settings.customEndpoint)
            ApiProvider.OLLAMA -> validateOllamaConnection(settings.getEffectiveBaseUrl())
            ApiProvider.CUSTOM -> validateCustomEndpoint(apiKey, settings.customEndpoint)
        }
    }
    
    private fun validateOpenAiKey(apiKey: String): ValidationResult {
        try {
            val url = URL("https://api.openai.com/v1/models")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            return when (responseCode) {
                200 -> ValidationResult(true, "OpenAI API key is valid")
                401 -> ValidationResult(false, "Invalid API key")
                429 -> ValidationResult(false, "Rate limit exceeded")
                else -> ValidationResult(false, "Validation failed with code: $responseCode")
            }
        } catch (e: Exception) {
            logger.warn("Error validating OpenAI key", e)
            return ValidationResult(false, "Connection error: ${e.message}")
        }
    }
    
    private fun validateAnthropicKey(apiKey: String): ValidationResult {
        try {
            val url = URL("https://api.anthropic.com/v1/models")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("x-api-key", apiKey)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            return when (responseCode) {
                200 -> ValidationResult(true, "Anthropic API key is valid")
                401 -> ValidationResult(false, "Invalid API key")
                403 -> ValidationResult(false, "Forbidden - key may be revoked")
                429 -> ValidationResult(false, "Rate limit exceeded")
                else -> ValidationResult(false, "Validation failed with code: $responseCode")
            }
        } catch (e: Exception) {
            logger.warn("Error validating Anthropic key", e)
            return ValidationResult(false, "Connection error: ${e.message}")
        }
    }
    
    private fun validateAzureOpenAiKey(apiKey: String, endpoint: String): ValidationResult {
        if (endpoint.isBlank() || !endpoint.startsWith("https://")) {
            return ValidationResult(false, "Invalid Azure endpoint URL. It should start with https://")
        }
        
        try {
            val baseUrl = endpoint.removeSuffix("/")
            val url = URL("$baseUrl/openai/deployments?api-version=2023-05-15")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("api-key", apiKey)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            return when (responseCode) {
                200 -> ValidationResult(true, "Azure OpenAI API key is valid")
                401 -> ValidationResult(false, "Invalid API key")
                403 -> ValidationResult(false, "Forbidden - check key and permissions")
                404 -> ValidationResult(false, "Not found - endpoint URL may be incorrect")
                else -> ValidationResult(false, "Validation failed with code: $responseCode")
            }
        } catch (e: Exception) {
            logger.warn("Error validating Azure OpenAI key", e)
            return ValidationResult(false, "Connection error: ${e.message}")
        }
    }
    
    private fun validateCustomEndpoint(apiKey: String, endpoint: String): ValidationResult {
        if (endpoint.isBlank()) {
            return ValidationResult(false, "Custom endpoint URL is empty")
        }
        
        if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
            return ValidationResult(false, "Invalid endpoint URL. It should start with http:// or https://")
        }
        
        try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            return when (responseCode) {
                200, 204 -> ValidationResult(true, "Custom endpoint validation successful")
                401, 403 -> ValidationResult(false, "Authentication failed - check API key")
                404 -> ValidationResult(false, "Endpoint not found - check URL")
                else -> ValidationResult(false, "Validation failed with code: $responseCode")
            }
        } catch (e: Exception) {
            logger.warn("Error validating custom endpoint", e)
            return ValidationResult(false, "Connection error: ${e.message}")
        }
    }
    
    /**
     * Validates connection to a local Ollama server.
     * 
     * @param baseUrl The Ollama server URL (usually http://localhost:11434)
     * @return A ValidationResult with the result and a message
     */
    private fun validateOllamaConnection(baseUrl: String): ValidationResult {
        try {
            // Check if the base URL is valid
            if (baseUrl.isBlank()) {
                return ValidationResult(false, "Ollama server URL is empty.")
            }
            
            // Test connection to the Ollama API
            val url = URL("$baseUrl/api/tags")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            logger.info("Ollama API connection test returned code: $responseCode")
            
            if (responseCode == 200) {
                return ValidationResult(true, "Successfully connected to Ollama server.")
            }
            
            // Handle error response
            val errorStream = connection.errorStream
            val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            
            return ValidationResult(
                false,
                "Failed to connect to Ollama server. Response code: $responseCode. ${errorResponse.take(100)}"
            )
        } catch (e: Exception) {
            logger.error("Error connecting to Ollama server", e)
            return ValidationResult(
                false,
                "Error connecting to Ollama server: ${e.message ?: "Unknown error"}"
            )
        }
    }
    
    /**
     * Formats error messages for display to the user
     */
    fun formatErrorMessage(error: Exception, provider: ApiProvider): String {
        val baseMessage = when (error) {
            is java.net.UnknownHostException -> "Network error: Could not reach the API server. Check your internet connection."
            is java.net.SocketTimeoutException -> "Timeout error: The API request took too long. The service might be experiencing high load."
            is java.io.IOException -> "IO Error: ${error.message}"
            else -> "Error: ${error.message}"
        }
        
        val providerSpecific = when (provider) {
            ApiProvider.OPENAI -> "Check your OpenAI API key and quota."
            ApiProvider.ANTHROPIC -> "Check your Anthropic API key and quota."
            ApiProvider.AZURE_OPENAI -> "Check your Azure OpenAI API key, endpoint URL, and quota."
            ApiProvider.OLLAMA -> "Make sure Ollama is running on your machine."
            ApiProvider.CUSTOM -> "Check your custom endpoint configuration."
        }
        
        return "$baseMessage $providerSpecific"
    }
} 