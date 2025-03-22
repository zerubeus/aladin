package com.github.zerubeus.aladin.arc

import org.eclipse.lmos.arc.agents.DSLAgents

/**
 * Create Agents.
 */
fun DSLAgents.createAgents() = define {
    agent {
        name = "Aladin"
        prompt {
            """
                Welcome to Aladin! I am a simple chatbot that can help you with your queries.
                """
        }
    }

    agent {
        name = "AnotherAgent"
        prompt {
            """
                Welcome to AnotherAgent! I am a simple chatbot that can help you with your queries.
                """
        }
    }
}
