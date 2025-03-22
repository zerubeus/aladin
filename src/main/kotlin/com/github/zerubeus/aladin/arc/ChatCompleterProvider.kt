
package com.github.zerubeus.aladin.arc

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.KeyCredential
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.client.azure.AzureAIClient
import org.eclipse.lmos.arc.client.azure.AzureClientConfig
import org.eclipse.lmos.arc.client.langchain4j.LangChainClient
import org.eclipse.lmos.arc.client.langchain4j.LangChainConfig
import org.eclipse.lmos.arc.client.langchain4j.builders.bedrockBuilder
import org.eclipse.lmos.arc.client.langchain4j.builders.geminiBuilder
import org.eclipse.lmos.arc.client.langchain4j.builders.groqBuilder
import org.eclipse.lmos.arc.client.langchain4j.builders.ollamaBuilder

/**
 * Provides a ChatCompleterProvider based on the given configuration.
 */
fun chatCompleterProvider(config: AIClientConfig, eventPublisher: EventPublisher): ChatCompleterProvider {
    val langChainConfig = LangChainConfig(
        modelName = config.modelName,
        url = config.url,
        apiKey = config.apiKey,
        accessKeyId = config.accessKey,
        secretAccessKey = config.accessSecret,
    )

    val aiClient = when {
        "gemini" == config.client -> LangChainClient(langChainConfig, geminiBuilder(), eventPublisher)

        "bedrock" == config.client -> LangChainClient(langChainConfig, bedrockBuilder(), eventPublisher)

        "groq" == config.client -> LangChainClient(langChainConfig, groqBuilder(), eventPublisher)

        "ollama" == config.client -> LangChainClient(langChainConfig, ollamaBuilder(), eventPublisher)

        "openai" == config.client -> AzureAIClient(
            AzureClientConfig(config.modelName, config.url ?: "", config.apiKey ?: ""),
            com.azure.ai.openai.OpenAIClientBuilder()
                .apply { if (config.url != null) endpoint(config.url) }
                .credential(KeyCredential(config.apiKey))
                .buildAsyncClient(),
            eventPublisher,
        )

        else -> error("No client could be configured for client: ${config.client}")
    }

    return ChatCompleterProvider { aiClient }
}

data class AIClientConfig(
    val id: String,
    val client: String,
    val modelName: String,
    val url: String? = null,
    val apiKey: String? = null,
    val accessKey: String? = null,
    val accessSecret: String? = null,
)
