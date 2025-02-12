package com.github.zerubeus.aladin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.github.zerubeus.aladin.ui.NotificationPanelWindow
import javax.swing.Icon

class NotificationPanelAction : AnAction(), DumbAware {
    private var notificationPanel: NotificationPanelWindow? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (notificationPanel == null) {
            notificationPanel = NotificationPanelWindow(project)
        }
        
        notificationPanel?.toggle(e)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
} 