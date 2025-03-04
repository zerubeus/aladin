package com.github.zerubeus.aladin.actions

import com.github.zerubeus.aladin.settings.ApiSettingsConfigurable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil

/**
 * Action to quickly open the Aladin AI settings panel from the Tools menu.
 */
class OpenSettingsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ApiSettingsConfigurable::class.java)
    }
} 