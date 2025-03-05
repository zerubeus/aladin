package com.github.zerubeus.aladin.services

import com.github.zerubeus.aladin.settings.ApiProvider
import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service for interacting with OpenAI's API.
 * Handles message sending and response processing.
 */
@Service(Service.Level.APP)
class OpenAiService : LlmProviderService {
    private val logger = logger<OpenAiService>()
    private val tokenUsageService = service<TokenUsageService>()
    
    companion object {
        private const val DEFAULT_MODEL = "gpt-3.5-turbo"
        private const val SYSTEM_PROMPT = "You are Aladin, an AI assistant for coding in JetBrains IDEs. " +
                "You help answer questions about code, suggest improvements, and assist with programming tasks."
    }
    
    /**
     * Sends a message to OpenAI and returns the response.
     * 
     * @param userMessage The message from the user
     * @return The AI's response
     * @throws Exception if there's an error communicating with the API
     */
    override suspend fun sendMessage(userMessage: String): String = withContext(Dispatchers.IO) {
        val settings = service<ApiSettingsState>()
        val apiKey = settings.getDecryptedApiKey()
        
        if (apiKey.isBlank()) {
            logger.warn("API key is blank, cannot send message to OpenAI")
            return@withContext "Please configure your API key in Settings → Aladin AI Settings."
        }
        
        logger.info("Preparing to send message to OpenAI, estimated token count: ${estimateTokens(userMessage)}")
        
        // Estimate tokens to ensure we don't exceed limits
        val estimatedTokens = estimateTokens(userMessage)
        if (!tokenUsageService.canMakeRequest(estimatedTokens)) {
            logger.warn("Daily token limit exceeded")
            return@withContext "Daily token limit exceeded. Please try again tomorrow or adjust your limits in settings."
        }
        
        try {
            // Prepare the request
            val url = URL("https://api.openai.com/v1/chat/completions")
            logger.info("Connecting to OpenAI API: ${url}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            
            // Create the messages array
            val messagesArray = JSONArray()
            
            // Add system message
            val systemMessage = JSONObject()
            systemMessage.put("role", "system")
            systemMessage.put("content", SYSTEM_PROMPT)
            messagesArray.put(systemMessage)
            
            // Add user message
            val userMsg = JSONObject()
            userMsg.put("role", "user")
            userMsg.put("content", userMessage)
            messagesArray.put(userMsg)
            
            // Create the request body
            val requestBody = JSONObject()
            requestBody.put("model", DEFAULT_MODEL)
            requestBody.put("messages", messagesArray)
            requestBody.put("temperature", 0.7)
            requestBody.put("max_tokens", 1000)
            
            val requestBodyString = requestBody.toString()
            logger.info("Sending request to OpenAI API with body: $requestBodyString")
            
            // Send the request
            connection.outputStream.use { os ->
                os.write(requestBodyString.toByteArray())
            }
            
            // Process the response
            val responseCode = connection.responseCode
            logger.info("Received response from OpenAI API with code: $responseCode")
            
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                logger.info("Received successful response from OpenAI: $response")
                val jsonResponse = JSONObject(response)
                
                // Extract the response text
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    val content = message.getString("content")
                    
                    // Record token usage
                    val usage = jsonResponse.getJSONObject("usage")
                    val totalTokens = usage.getInt("total_tokens")
                    tokenUsageService.recordTokenUsage(totalTokens)
                    
                    logger.info("Successfully processed OpenAI response with $totalTokens tokens")
                    return@withContext content
                }
                logger.warn("Received empty response from OpenAI")
                return@withContext "Received empty response from OpenAI."
            } else {
                // Handle error response
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                logger.warn("OpenAI API error: $responseCode, $errorResponse")
                
                // Parse error message if possible
                return@withContext try {
                    val jsonError = JSONObject(errorResponse)
                    val errorMsg = jsonError.optJSONObject("error")?.optString("message") ?: "Unknown error"
                    
                    // Special handling for rate limit errors
                    if (errorMsg.contains("rate limit", ignoreCase = true)) {
                        logger.warn("OpenAI rate limit reached: $errorMsg")
                        "Rate limit exceeded. Please try again later. ($errorMsg)"
                    } else {
                        logger.error("OpenAI error: $errorMsg")
                        "Error from OpenAI: $errorMsg"
                    }
                } catch (e: Exception) {
                    logger.error("Failed to parse OpenAI error response", e)
                    "Error communicating with OpenAI: ${e.message}"
                }
            }
        } catch (e: Exception) {
            logger.error("Error communicating with OpenAI", e)
            return@withContext "Error communicating with OpenAI: ${e.message ?: "Unknown error"} (${e.javaClass.simpleName})"
        }
    }
    
    /**
     * Gets the currently selected model for OpenAI.
     * 
     * @return The model name
     */
    override fun getCurrentModel(): String {
        return DEFAULT_MODEL
    }
    
    /**
     * Gets available models from OpenAI.
     * For now, returns a hardcoded list of common models.
     * 
     * @return List of available model names
     */
    override suspend fun getAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        return@withContext listOf(
            "gpt-3.5-turbo",
            "gpt-4",
            "gpt-4-turbo"
        )
    }
    
    /**
     * Validates the OpenAI API connection and credentials.
     * 
     * @return True if connection is valid, false otherwise
     */
    override suspend fun validateConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val settings = service<ApiSettingsState>()
            val apiKey = settings.getDecryptedApiKey()
            
            if (apiKey.isBlank()) {
                return@withContext false
            }
            
            val url = URL("${settings.getEffectiveBaseUrl()}/models")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            return@withContext responseCode == 200
        } catch (e: Exception) {
            logger.warn("Failed to validate OpenAI connection: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Roughly estimates the number of tokens a message will use.
     * This is a simplified estimation: ~4 chars per token for English.
     * 
     * @param message The message to estimate
     * @return Estimated token count
     */
    private fun estimateTokens(message: String): Int {
        // Very rough estimation: ~4 chars per token for English text
        val estimatedTokens = (message.length / 4) + 1
        
        // Add tokens for system prompt and other overhead
        return estimatedTokens + 150 // System prompt + overhead
    }
} 