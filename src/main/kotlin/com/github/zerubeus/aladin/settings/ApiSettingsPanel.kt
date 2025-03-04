package com.github.zerubeus.aladin.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.awt.Color
import javax.swing.*

/**
 * Panel for configuring API settings.
 */
class ApiSettingsPanel(private val settings: ApiSettingsState) {
    val mainPanel: JPanel
    val apiKeyField: JPasswordField = JBPasswordField()
    val providerComboBox: JComboBox<ApiProvider> = ComboBox(ApiProvider.values())
    private val customEndpointField: JBTextField = JBTextField()
    private val tokenUsageLabel: JLabel = JBLabel()
    private val dailyLimitField: JBTextField = JBTextField()
    private val warningLabel: JLabel = JBLabel().apply {
        foreground = UIManager.getColor("PasswordField.capsLockIconColor") ?: Color.RED
        isVisible = false
    }
    
    init {
        // Set up the provider combobox
        providerComboBox.selectedItem = settings.apiProvider
        
        // Custom endpoint field visibility depends on provider selection
        customEndpointField.isVisible = settings.apiProvider == ApiProvider.CUSTOM
        customEndpointField.text = settings.customEndpoint
        
        // Set up token usage display
        updateTokenUsageLabel()
        
        // Set daily token limit
        dailyLimitField.text = settings.dailyTokenLimit.toString()
        
        // Set the API key from secure storage (will be masked)
        val storedKey = settings.getDecryptedApiKey()
        if (storedKey.isNotEmpty()) {
            apiKeyField.text = storedKey
        }
        
        // Set up event listeners
        providerComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val selectedProvider = e.item as ApiProvider
                customEndpointField.isVisible = selectedProvider == ApiProvider.CUSTOM
                // Clear and show warning if custom endpoint is empty but selected
                if (selectedProvider == ApiProvider.CUSTOM && customEndpointField.text.isEmpty()) {
                    warningLabel.text = "Please enter a custom endpoint URL"
                    warningLabel.isVisible = true
                } else {
                    warningLabel.isVisible = false
                }
            }
        }
        
        // Create the main form layout
        val baseSettingsPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("API Provider:", providerComboBox)
            .addLabeledComponent("API Key:", apiKeyField)
            .addLabeledComponent("Custom Endpoint:", customEndpointField)
            .addLabeledComponent("Daily Token Limit:", dailyLimitField)
            .addComponent(tokenUsageLabel)
            .addComponent(warningLabel)
            .panel
            
        // Create a link to API provider documentation
        val docLinkLabel = JLabel("<html><a href=''>Get API Keys Documentation</a></html>")
        docLinkLabel.cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        docLinkLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                val url = when (providerComboBox.selectedItem as ApiProvider) {
                    ApiProvider.OPENAI -> "https://platform.openai.com/account/api-keys"
                    ApiProvider.ANTHROPIC -> "https://console.anthropic.com/account/keys"
                    ApiProvider.AZURE_OPENAI -> "https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/create-resource"
                    ApiProvider.CUSTOM -> ""
                }
                if (url.isNotEmpty()) {
                    java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                }
            }
        })
        
        // Assemble the main panel with some spacing and the doc link
        mainPanel = JPanel(BorderLayout())
        mainPanel.add(baseSettingsPanel, BorderLayout.CENTER)
        mainPanel.add(docLinkLabel, BorderLayout.SOUTH)
        mainPanel.border = JBUI.Borders.empty(10)
    }
    
    private fun updateTokenUsageLabel() {
        val percent = if (settings.dailyTokenLimit > 0) {
            (settings.tokenUsage.toDouble() / settings.dailyTokenLimit.toDouble()) * 100
        } else {
            0.0
        }
        
        val usageText = String.format("Token Usage: %,d / %,d (%.1f%%)", 
            settings.tokenUsage, settings.dailyTokenLimit, percent)
            
        tokenUsageLabel.text = usageText
        
        // Set warning color if approaching limit
        if (settings.isApproachingLimit()) {
            tokenUsageLabel.foreground = UIManager.getColor("PasswordField.capsLockIconColor") 
                ?: Color.RED
        } else {
            tokenUsageLabel.foreground = UIManager.getColor("Label.foreground")
        }
    }
    
    fun isModified(settings: ApiSettingsState): Boolean {
        // Debug modifications
        val currentProvider = providerComboBox.selectedItem as ApiProvider
        val providerModified = currentProvider != settings.apiProvider
        
        val apiKeyModified = apiKeyField.password.isNotEmpty() && String(apiKeyField.password) != settings.apiKey
        val endpointModified = customEndpointField.text != settings.customEndpoint
        
        // Track if daily limit is modified
        var limitModified = false
        try {
            val dailyLimit = dailyLimitField.text.toLong()
            limitModified = dailyLimit != settings.dailyTokenLimit
        } catch (e: NumberFormatException) {
            // Invalid number, consider it modified
            limitModified = true
        }
        
        return providerModified || apiKeyModified || endpointModified || limitModified
    }
    
    fun apply(settings: ApiSettingsState) {
        settings.apiProvider = providerComboBox.selectedItem as ApiProvider
        
        val apiKey = String(apiKeyField.password)
        if (apiKey.isNotEmpty()) {
            settings.setApiKey(apiKey)
        }
        
        settings.customEndpoint = customEndpointField.text
        
        try {
            settings.dailyTokenLimit = dailyLimitField.text.toLong()
        } catch (e: NumberFormatException) {
            // Reset to default if invalid
            settings.dailyTokenLimit = 100000
            dailyLimitField.text = settings.dailyTokenLimit.toString()
        }
        
        updateTokenUsageLabel()
    }
    
    fun reset(settings: ApiSettingsState) {
        providerComboBox.selectedItem = settings.apiProvider
        apiKeyField.text = settings.getDecryptedApiKey()
        customEndpointField.text = settings.customEndpoint
        customEndpointField.isVisible = settings.apiProvider == ApiProvider.CUSTOM
        dailyLimitField.text = settings.dailyTokenLimit.toString()
        updateTokenUsageLabel()
    }
} 