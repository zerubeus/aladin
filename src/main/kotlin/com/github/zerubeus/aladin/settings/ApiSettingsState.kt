package com.github.zerubeus.aladin.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent state component for storing API settings.
 * Credentials are stored securely using the PasswordSafe.
 */
@Service(Service.Level.APP)
@State(
    name = "com.github.zerubeus.aladin.settings.ApiSettingsState",
    storages = [Storage("AladinApiSettings.xml")]
)
class ApiSettingsState : PersistentStateComponent<ApiSettingsState> {
    // Settings that are safe to store in plain text
    var apiProvider: ApiProvider = ApiProvider.OPENAI
    var customEndpoint: String = ""
    var tokenUsage: Long = 0
    var requestLimit: Long = 1000 // Default limit of requests
    var dailyTokenLimit: Long = 100000 // Default token limit per day
    
    // For displaying masked key in UI (never store the actual key here)
    var apiKeyPlainText: String = "" // This should never contain the actual key!
    
    // We're not storing the API key in this state directly, but in the password safe
    
    companion object {
        private val logger = logger<ApiSettingsState>()
        private const val CREDENTIAL_SERVICE_NAME = "AladinApiKey"
    }
    
    // Utility methods for secure credential management
    private fun getCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName(CREDENTIAL_SERVICE_NAME, apiProvider.name)
        )
    }
    
    fun getDecryptedApiKey(): String {
        return try {
            PasswordSafe.instance.getPassword(getCredentialAttributes()) ?: ""
        } catch (e: Exception) {
            logger.error("Failed to retrieve API key from secure storage", e)
            ""
        }
    }
    
    fun setApiKey(apiKey: String) {
        try {
            val credentials = Credentials("", apiKey)
            PasswordSafe.instance.set(getCredentialAttributes(), credentials)
            // Set a masked version for display only
            apiKeyPlainText = if (apiKey.isNotEmpty()) "********" else ""
        } catch (e: Exception) {
            logger.error("Failed to store API key in secure storage", e)
        }
    }
    
    val apiKey: String
        get() = getDecryptedApiKey()
    
    // Track token usage for limiting/display
    fun recordTokenUsage(tokens: Int) {
        tokenUsage += tokens
        if (logger.isDebugEnabled) {
            logger.debug("Recorded $tokens tokens, total usage: $tokenUsage")
        }
    }
    
    fun resetTokenUsage() {
        tokenUsage = 0
    }
    
    fun isApproachingLimit(): Boolean {
        return tokenUsage > (dailyTokenLimit * 0.8)
    }
    
    // PersistentStateComponent implementation
    override fun getState(): ApiSettingsState = this
    
    override fun loadState(state: ApiSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
} 