package com.github.zerubeus.aladin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class AladinToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatPanel = ChatPanel()
        val content = ContentFactory.getInstance().createContent(chatPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
} 