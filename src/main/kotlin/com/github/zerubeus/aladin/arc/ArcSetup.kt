package com.github.zerubeus.aladin.arc


import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.DSLAgents
import org.eclipse.lmos.arc.agents.events.BasicEventPublisher
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler

/**
 * Initializes the ARC Framework.
 */
fun setupArc(appConfig: AIClientConfig, contextBeans: Set<Any>): DSLAgents {
    val eventPublisher = BasicEventPublisher(LoggingEventHandler())
    val chatCompleterProvider = chatCompleterProvider(appConfig, eventPublisher)
    return DSLAgents.init(chatCompleterProvider)
}

/**
 * Get a ChatAgent by name.
 */
fun DSLAgents.getChatAgent(name: String) = getAgents().find { it.name == name } as ChatAgent

