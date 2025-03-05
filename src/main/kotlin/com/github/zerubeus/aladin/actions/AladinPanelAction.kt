package com.github.zerubeus.aladin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.github.zerubeus.aladin.ui.AladinPanelWindow

class AladinPanelAction : AnAction(), DumbAware {
    private var aladinPanel: AladinPanelWindow? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (aladinPanel == null) {
            aladinPanel = AladinPanelWindow(project)
        }
        
        aladinPanel?.toggle(e)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
} 