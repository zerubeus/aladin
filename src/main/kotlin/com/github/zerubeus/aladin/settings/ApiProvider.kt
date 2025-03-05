package com.github.zerubeus.aladin.settings

/**
 * Enum representing the available LLM API providers that can be used with the plugin.
 */
enum class ApiProvider(val displayName: String, val baseUrl: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1"),
    AZURE_OPENAI("Azure OpenAI", "https://YOUR_RESOURCE_NAME.openai.azure.com"),
    OLLAMA("Ollama", "http://localhost:11434"),
    CUSTOM("Custom Endpoint", "");
    
    override fun toString(): String = displayName
    
    companion object {
        fun fromDisplayName(displayName: String): ApiProvider {
            return entries.find { it.displayName == displayName } ?: OPENAI
        }
    }
} 