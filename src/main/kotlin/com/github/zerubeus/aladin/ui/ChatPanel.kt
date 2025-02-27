package com.github.zerubeus.aladin.ui

import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.ScrollPaneConstants
import javax.swing.border.EmptyBorder
import java.awt.Font
import java.awt.Color
import javax.swing.text.DefaultCaret
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.StyledDocument
import javax.swing.JLabel
import javax.swing.ImageIcon
import javax.swing.SwingUtilities
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.border.CompoundBorder

/**
 * A panel that displays a chat interface for the Aladin AI assistant.
 */
class ChatPanel : JBPanel<ChatPanel>(BorderLayout()) {
    private val chatHistoryPane: JTextPane
    private val inputField: JBTextField
    private val sendButton: JButton
    private val messagesPanel: JPanel
    
    // Message styling
    private val userBgColor = JBColor(Color(240, 240, 240), Color(60, 63, 65))
    private val aiBgColor = JBColor(Color(230, 242, 255), Color(45, 48, 50))
    private val userTextColor = JBColor.foreground()
    private val aiTextColor = JBColor.foreground()
    
    // Icons
    private val sendIcon = IconLoader.getIcon("/icons/send.svg", ChatPanel::class.java)
    
    init {
        // Messages panel with BoxLayout for vertical stacking
        messagesPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = JBColor.background()
            border = EmptyBorder(10, 10, 10, 10)
        }
        
        // Scroll pane for messages
        val scrollPane = JBScrollPane(messagesPanel).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = BorderFactory.createEmptyBorder()
            viewport.background = JBColor.background()
        }
        
        // Chat history area (not used directly, but kept for future reference)
        chatHistoryPane = JTextPane().apply {
            isEditable = false
            background = JBColor.background()
        }
        
        // Input panel
        val inputPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                JBUI.Borders.empty(10)
            )
            background = JBColor.background()
        }
        
        // Input field
        inputField = JBTextField("Ask Aladin anything...").apply {
            font = JBUI.Fonts.create(Font.SANS_SERIF, 13f)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBUI.CurrentTheme.TextField.borderColor(), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            )
            
            // Add placeholder text behavior
            val placeholderText = "Ask Aladin anything..."
            text = placeholderText
            foreground = JBColor.GRAY
            
            addFocusListener(object : java.awt.event.FocusListener {
                override fun focusGained(e: java.awt.event.FocusEvent) {
                    if (text == placeholderText) {
                        text = ""
                        foreground = JBColor.foreground()
                    }
                }
                
                override fun focusLost(e: java.awt.event.FocusEvent) {
                    if (text.isEmpty()) {
                        text = placeholderText
                        foreground = JBColor.GRAY
                    }
                }
            })
            
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                        e.consume()
                        sendMessage()
                    } else if (e.keyCode == KeyEvent.VK_ENTER && e.isShiftDown) {
                        // Allow multi-line input with Shift+Enter
                        text += "\n"
                    }
                }
            })
        }
        
        // Send button with icon
        sendButton = JButton().apply {
            icon = sendIcon
            toolTipText = "Send message (Enter)"
            isFocusable = false
            isOpaque = false
            isBorderPainted = false
            isContentAreaFilled = false
            addActionListener { sendMessage() }
        }
        
        // Add components to input panel
        inputPanel.add(inputField, BorderLayout.CENTER)
        inputPanel.add(sendButton, BorderLayout.EAST)
        
        // Add components to main panel
        add(scrollPane, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        
        // Add welcome message
        addMessage("Aladin", "Hello! I'm Aladin, your AI coding assistant. How can I help you today?")
        
        // Set preferred size
        preferredSize = Dimension(400, 600)
    }
    
    /**
     * Creates a message bubble panel with the given text and styling.
     */
    private fun createMessagePanel(sender: String, message: String): JPanel {
        val isUser = sender != "Aladin"
        
        // Main panel for the message
        val messagePanel = JPanel(BorderLayout()).apply {
            background = JBColor.background()
            border = EmptyBorder(5, 0, 5, 0)
        }
        
        // Avatar and name panel
        val avatarPanel = JPanel(FlowLayout(if (isUser) FlowLayout.RIGHT else FlowLayout.LEFT)).apply {
            background = JBColor.background()
        }
        
        // Avatar icon (placeholder - you can replace with actual icons)
        val avatarIcon = if (isUser) {
            // User icon
            JLabel("👤")
        } else {
            // Aladin icon
            JLabel("🧞")
        }
        
        // Name label
        val nameLabel = JLabel(if (isUser) "You" else "Aladin").apply {
            font = JBUI.Fonts.create(Font.SANS_SERIF, 12f, Font.BOLD)
            foreground = JBColor.foreground()
        }
        
        avatarPanel.add(avatarIcon)
        avatarPanel.add(nameLabel)
        
        // Message bubble
        val bubblePanel = JPanel(BorderLayout()).apply {
            background = if (isUser) userBgColor else aiBgColor
            border = CompoundBorder(
                BorderFactory.createLineBorder(if (isUser) userBgColor.darker() else aiBgColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            )
        }
        
        // Message text
        val textArea = JBTextArea().apply {
            text = message
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = JBUI.Fonts.create(Font.SANS_SERIF, 13f)
            background = if (isUser) userBgColor else aiBgColor
            foreground = if (isUser) userTextColor else aiTextColor
            border = BorderFactory.createEmptyBorder()
            
            // Calculate preferred size based on text content
            val fontMetrics = getFontMetrics(font)
            val width = 300 // Maximum width
            val lines = text.split("\n")
            var height = 0
            
            for (line in lines) {
                val lineWidth = fontMetrics.stringWidth(line)
                val lineCount = (lineWidth / width) + 1
                height += fontMetrics.height * lineCount
            }
            
            preferredSize = Dimension(width, height)
        }
        
        bubblePanel.add(textArea, BorderLayout.CENTER)
        
        // Add components to message panel
        messagePanel.add(avatarPanel, BorderLayout.NORTH)
        messagePanel.add(bubblePanel, if (isUser) BorderLayout.EAST else BorderLayout.WEST)
        
        return messagePanel
    }
    
    /**
     * Adds a message to the chat history.
     */
    fun addMessage(sender: String, message: String) {
        val messagePanel = createMessagePanel(sender, message)
        
        // Add message panel to messages panel
        messagesPanel.add(messagePanel)
        messagesPanel.add(Box.createVerticalStrut(10))
        
        // Revalidate and repaint
        messagesPanel.revalidate()
        messagesPanel.repaint()
        
        // Scroll to bottom
        SwingUtilities.invokeLater {
            val scrollPane = SwingUtilities.getAncestorOfClass(JBScrollPane::class.java, messagesPanel) as? JBScrollPane
            scrollPane?.let {
                val verticalBar = it.verticalScrollBar
                verticalBar.value = verticalBar.maximum
            }
        }
    }
    
    /**
     * Sends the message from the input field.
     */
    private fun sendMessage() {
        val message = inputField.text.trim()
        val placeholderText = "Ask Aladin anything..."
        
        if (message.isNotEmpty() && message != placeholderText) {
            addMessage("User", message)
            inputField.text = ""
            
            // TODO: Process the message and generate a response
            // This is where you would integrate with your AI backend
            simulateResponse(message)
        }
    }
    
    /**
     * Simulates a response from the AI assistant.
     * This is a placeholder until the actual AI integration is implemented.
     */
    private fun simulateResponse(userMessage: String) {
        // Simple echo response for now
        val response = "I received your message: \"$userMessage\". This is a placeholder response until the AI integration is implemented."
        
        // Add a slight delay to simulate processing time
        Thread {
            Thread.sleep(500)
            SwingUtilities.invokeLater {
                addMessage("Aladin", response)
            }
        }.start()
    }
} 