package com.github.zerubeus.aladin.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import java.awt.Point
import com.intellij.openapi.wm.WindowManager
import java.awt.Component

class NotificationPanelWindow(private val project: Project) {
    private var popup: JBPopup? = null
    
    fun toggle(e: AnActionEvent) {
        if (popup?.isVisible == true) {
            popup?.cancel()
            return
        }
        
        val panel = createNotificationPanel()
        popup = createPopup(panel)
        
        // Get the source component if available, otherwise use the IDE frame
        val sourceComponent = e.inputEvent?.component 
            ?: WindowManager.getInstance().getIdeFrame(project)?.component
            ?: return
            
        showPopupInTopRight(sourceComponent)
    }
    
    private fun createNotificationPanel(): JBPanel<*> {
        val panel = JBPanel<JBPanel<*>>()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        // Add a scroll pane
        val scrollPane = JBScrollPane(panel)
        scrollPane.preferredSize = Dimension(400, 600)
        scrollPane.border = BorderFactory.createEmptyBorder()
        
        val containerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        containerPanel.add(scrollPane, BorderLayout.CENTER)
        
        return containerPanel
    }
    
    private fun createPopup(content: JBPanel<*>): JBPopup {
        return JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, null)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setTitle("Notifications")
            .createPopup()
    }
    
    private fun showPopupInTopRight(component: Component) {
        val window = WindowManager.getInstance().getIdeFrame(project)?.component ?: return
        val bounds = window.bounds
        val point = Point(bounds.width - 420, 0)
        popup?.showInScreenCoordinates(window, point)
    }
} 