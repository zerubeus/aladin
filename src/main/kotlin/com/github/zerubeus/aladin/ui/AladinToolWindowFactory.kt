package com.github.zerubeus.aladin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout

class AladinToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val aladinPanel = createAladinPanel()
        val content = ContentFactory.getInstance().createContent(aladinPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createAladinPanel(): JBPanel<*> {
        val panel = JBPanel<JBPanel<*>>()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        // Add a scroll pane
        val scrollPane = JBScrollPane(panel)
        scrollPane.border = BorderFactory.createEmptyBorder()
        
        val containerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        containerPanel.add(scrollPane, BorderLayout.CENTER)
        
        return containerPanel
    }
} 