package com.github.zerubeus.aladin.ui

import com.github.zerubeus.aladin.services.LlmProviderFactory
import com.github.zerubeus.aladin.settings.ApiProvider
import com.github.zerubeus.aladin.settings.ApiSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * A panel that displays a chat interface for the Aladin AI assistant.
 */
class ChatPanel : JBPanel<ChatPanel>(BorderLayout()) {
    private val chatHistoryPane: JTextPane
    private val inputField: JBTextField
    private val sendButton: JButton
    private val aiProviderSelector: ComboBox<String>

    // Messages panel with BoxLayout for vertical stacking
    private val messagesPanel: JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = JBColor.background()
        border = EmptyBorder(10, 10, 10, 10)
    }

    // Message styling
    private val userBgColor = JBColor(
        Color(220, 230, 245),
        JBColor.background().brighter()
    )
    private val aiBgColor = JBColor.background()
    private val userTextColor = JBColor.foreground()
    private val aiTextColor = JBColor.foreground()


    // Icons
    private val sendIcon = IconLoader.getIcon("/icons/send.svg", ChatPanel::class.java)

    init {
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

        // AI Provider selector panel (OpenAI, OLLAMA, etc.)
        val aiProviderPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = JBColor.background()
        }

        // AI Provider selector
        aiProviderSelector = ComboBox<String>().apply {
            // Add available providers
            addItem(ApiProvider.OPENAI.displayName)
            addItem(ApiProvider.OLLAMA.displayName)

            // Set current provider
            val settings = service<ApiSettingsState>()
            selectedItem = settings.apiProvider.displayName

            // Add change listener
            addActionListener {
                val selectedAIProviderName = selectedItem?.toString() ?: ApiProvider.OLLAMA.displayName
                val selectedProvider = ApiProvider.fromDisplayName(selectedAIProviderName)
                settings.apiProvider = selectedProvider
                // Notify user of provider change
                addMessage("System", "Switched to ${selectedProvider.displayName} provider")
            }
        }

        // Add provider label and selector to provider panel
        aiProviderPanel.add(JLabel("Provider:"))
        aiProviderPanel.add(aiProviderSelector)

        // Add provider panel to the top
        add(aiProviderPanel, BorderLayout.NORTH)

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
            font = JBUI.Fonts.create(Font.SANS_SERIF, 13)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1, true),
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
     * Creates a styled text area for messages.
     */
    private fun createMessageTextArea(message: String, bgColor: Color, textColor: Color): JBTextArea {
        return JBTextArea().apply {
            text = message
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = JBUI.Fonts.create(Font.SANS_SERIF, 13)
            background = bgColor
            foreground = textColor
            border = BorderFactory.createEmptyBorder()
        }
    }

    /**
     * Creates a message bubble panel with the given text and styling.
     */
    private fun createMessagePanel(sender: String, message: String): JPanel {
        val isUser = sender != "Aladin"

        val messagePanel = JPanel(BorderLayout()).apply {
            background = JBColor.background()
            border = EmptyBorder(5, 0, 5, 0)
        }

        if (isUser) {
            val userBubblePanel = JPanel(BorderLayout()).apply {
                background = userBgColor
                border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
            }

            val contentPanel = JPanel(GridBagLayout()).apply {
                background = userBgColor
                border = null
            }

            // Changed layout to FlowLayout(FlowLayout.LEFT)
            val avatarPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                background = userBgColor
            }

            val iconLabel = JLabel("👤 You")

            avatarPanel.add(iconLabel)

            val constraints = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.NORTHWEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = JBUI.insets(0, 0, 0, 0)
            }

            contentPanel.add(createMessageTextArea(message, userBgColor, userTextColor), constraints)

            userBubblePanel.add(avatarPanel, BorderLayout.NORTH)
            userBubblePanel.add(contentPanel, BorderLayout.CENTER)
            messagePanel.add(userBubblePanel, BorderLayout.CENTER)
        } else {
            val aiPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                background = JBColor.background()
            }

            val aiAvatarIcon = JLabel("🧞")
            val nameLabel = JLabel("Aladin").apply {
                font = JBUI.Fonts.create(Font.SANS_SERIF, 12)
                foreground = JBColor.foreground()
            }

            aiPanel.add(aiAvatarIcon)
            aiPanel.add(nameLabel)

            val aiBubblePanel = JPanel(BorderLayout()).apply {
                background = aiBgColor
                border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
            }

            aiBubblePanel.add(createMessageTextArea(message, aiBgColor, aiTextColor), BorderLayout.CENTER)

            messagePanel.add(aiPanel, BorderLayout.NORTH)
            messagePanel.add(aiBubblePanel, BorderLayout.CENTER)
        }

        return messagePanel
    }

    /**
     * Adds a message to the chat history.
     */
    private fun addMessage(sender: String, message: String) {
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

            // Use the LLM provider service to get a response
            getAiResponse(message)
        }
    }

    /**
     * Gets a response from the AI service.
     *
     * @param userMessage The message from the user
     */
    private fun getAiResponse(userMessage: String) {
        // Add a "thinking" message
        addMessage("Aladin", "Thinking...")

        // Call the LLM provider service in a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val llmProvider = service<LlmProviderFactory>().getProvider()
                val response = llmProvider.sendMessage(userMessage)

                // Update the UI on the EDT
                ApplicationManager.getApplication().invokeLater {
                    // Remove the "thinking" message
                    removeLastMessage()
                    // Add the actual response
                    addMessage("Aladin", response)
                }
            } catch (e: Exception) {
                // Handle any errors
                ApplicationManager.getApplication().invokeLater {
                    // Remove the "thinking" message
                    removeLastMessage()
                    // Add an error message
                    addMessage("Aladin", "Sorry, I encountered an error: ${e.message}")
                }
            }
        }
    }

    /**
     * Removes the last message from the chat.
     * Used to remove the "thinking" message before adding the actual response.
     */
    private fun removeLastMessage() {
        if (messagesPanel.componentCount >= 2) {
            // Remove the last message (component) and its spacing (vertical strut)
            messagesPanel.remove(messagesPanel.componentCount - 1) // Remove vertical strut
            messagesPanel.remove(messagesPanel.componentCount - 1) // Remove message panel

            // Revalidate and repaint
            messagesPanel.revalidate()
            messagesPanel.repaint()
        }
    }
}