package com.github.zerubeus.aladin.services

import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Service for interacting with Ollama's API.
 * Handles message sending and response processing.
 */
@Service(Service.Level.APP)
class OllamaService : LlmProviderService {
    private val logger = logger<OllamaService>()
    private var cachedModels: List<String>? = null
    
    companion object {
        // Default models in order of preference
        private val DEFAULT_MODELS = listOf("phi3", "phi2", "llama2", "mistral", "gemma")
    }
    
    /**
     * Sends a message to Ollama and returns the response.
     * 
     * @param userMessage The message from the user
     * @return The AI's response
     * @throws Exception if there's an error communicating with the API
     */
    override suspend fun sendMessage(userMessage: String): String = withContext(Dispatchers.IO) {
        val settings = service<ApiSettingsState>()
        val baseUrl = settings.getEffectiveBaseUrl()
        
        logger.info("Preparing to send message to Ollama")
        
        try {
            // Prepare the request
            val url = URL("$baseUrl/api/generate")
            logger.info("Connecting to Ollama API: $url")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            // Create the request body
            val requestBody = JSONObject()
            val currentModel = getCurrentModel()
            requestBody.put("model", currentModel)
            requestBody.put("prompt", userMessage)
            requestBody.put("system", "You are Aladin, an AI assistant for coding in JetBrains IDEs. " +
                    "You help answer questions about code, suggest improvements, and assist with programming tasks.")
            requestBody.put("stream", false)
            
            val requestBodyString = requestBody.toString()
            logger.info("Sending request to Ollama API with body: $requestBodyString")
            
            // Send the request
            connection.outputStream.use { os ->
                os.write(requestBodyString.toByteArray())
            }
            
            // Process the response
            val responseCode = connection.responseCode
            logger.info("Received response from Ollama API with code: $responseCode")
            
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                logger.info("Received successful response from Ollama")
                val jsonResponse = JSONObject(response)
                
                // Extract the response text
                val content = jsonResponse.getString("response")
                logger.info("Successfully processed Ollama response")
                return@withContext content
            } else {
                // Handle error response
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                logger.warn("Ollama API error: $responseCode, $errorResponse")
                
                // Parse error message if possible
                return@withContext try {
                    val jsonError = JSONObject(errorResponse)
                    val errorMsg = jsonError.optString("error", "Unknown error")
                    logger.error("Ollama error: $errorMsg")
                    "Error from Ollama: $errorMsg"
                } catch (e: Exception) {
                    logger.error("Failed to parse Ollama error response", e)
                    "Error communicating with Ollama: ${e.message}"
                }
            }
        } catch (e: Exception) {
            logger.error("Error communicating with Ollama", e)
            return@withContext "Error communicating with Ollama: ${e.message ?: "Unknown error"} (${e.javaClass.simpleName})"
        }
    }
    
    /**
     * Gets the currently selected model for Ollama.
     * Tries to find an available model on the server.
     * 
     * @return The model name
     */
    override fun getCurrentModel(): String {
        // If we have cached models, use the first one
        cachedModels?.firstOrNull()?.let {
            return it
        }
        
        // Try to load models
        runCatching {
            val models = runBlocking {
                getAvailableModels()
            }
            
            if (models.isNotEmpty()) {
                cachedModels = models
                return models.first()
            }
        }
        
        // If all else fails, use phi3 as default
        return "phi3" 
    }
    
    /**
     * Gets available models from Ollama.
     * 
     * @return List of available model names
     */
    override suspend fun getAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val settings = service<ApiSettingsState>()
            val baseUrl = settings.getEffectiveBaseUrl()
            
            val url = URL("$baseUrl/api/tags")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val modelsArray = jsonResponse.getJSONArray("models")
                
                val models = mutableListOf<String>()
                for (i in 0 until modelsArray.length()) {
                    val model = modelsArray.getJSONObject(i)
                    models.add(model.getString("name"))
                }
                
                // Cache the models for future use
                cachedModels = models
                
                return@withContext models
            }
            
            // If we can't get models, return a default list
            return@withContext DEFAULT_MODELS.toList()
        } catch (e: Exception) {
            logger.warn("Failed to get Ollama models: ${e.message}")
            // Return some common models as fallback
            return@withContext DEFAULT_MODELS.toList()
        }
    }
    
    /**
     * Validates the Ollama connection.
     * 
     * @return True if connection is valid, false otherwise
     */
    override suspend fun validateConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = service<ApiSettingsState>()
            val baseUrl = settings.getEffectiveBaseUrl()
            
            val url = URL("$baseUrl/api/tags")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            val responseCode = connection.responseCode
            return@withContext responseCode == 200
        } catch (e: Exception) {
            when (e) {
                is ConnectException, is SocketTimeoutException -> {
                    logger.warn("Failed to connect to Ollama server: ${e.message}")
                    return@withContext false
                }
                else -> {
                    logger.warn("Failed to validate Ollama connection: ${e.message}")
                    return@withContext false
                }
            }
        }
    }
} 