package com.github.zerubeus.aladin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.NlsContexts.ConfigurableName
import javax.swing.JComponent

/**
 * Configurable component for API settings.
 * Provides the entry point for the settings UI in the IntelliJ preferences.
 */
class ApiSettingsConfigurable : Configurable {
    private var settingsPanel: ApiSettingsPanel? = null
    
    @ConfigurableName
    override fun getDisplayName(): String = "Aladin AI Assistant"
    
    override fun createComponent(): JComponent {
        val settings = service<ApiSettingsState>()
        settingsPanel = ApiSettingsPanel(settings)
        return settingsPanel!!.mainPanel
    }
    
    override fun isModified(): Boolean {
        val settings = service<ApiSettingsState>()
        return settingsPanel?.isModified(settings) ?: false
    }
    
    override fun apply() {
        val settings = service<ApiSettingsState>()
        settingsPanel?.apply(settings)
    }
    
    override fun reset() {
        val settings = service<ApiSettingsState>()
        settingsPanel?.reset(settings)
    }
    
    override fun disposeUIResources() {
        settingsPanel = null
    }
} 