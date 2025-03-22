package com.github.zerubeus.aladin.arc

import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.UserMessage


suspend fun demo() {
    // Setup the ARC Framework
    val agents = setupArc(
        AIClientConfig(
            id = "gpt-3.5",
            client = "openai",
            modelName = "gpt-3.5-turbo",
            apiKey = "your-api"
        ), emptySet()
    )

    // Create agents
    agents.createAgents()

    // Execute an Agent
    val conversation = Conversation(transcript = listOf(UserMessage("Hello")))
    val result = agents.getChatAgent("Aladin").execute(conversation)

    println(result)
}
