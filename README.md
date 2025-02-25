# Aladin - AI Assistant for IntelliJ

![Build](https://github.com/zerubeus/aladin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Template ToDo list

- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [ ] Adjust the [pluginGroup](./gradle.properties) and [pluginName](./gradle.properties), as well as the [id](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [ ] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [x] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->

Aladin is an advanced AI assistant for IntelliJ-based IDEs that enhances your coding experience with intelligent, context-aware capabilities. Powered by modern AI technology, Aladin acts as your coding companion, helping you write, understand, and improve code faster and more efficiently.

## Key Features

### Agentic Capabilities

- **Reading & Writing Code**: Automatically inspects and modifies files in your editor through IntelliJ PSI or Document APIs
- **Searching the Codebase**: Scans project files using IntelliJ's indexing or custom text-based search to locate references and snippets
- **Calling MCP Servers**: Sends project context and user queries to external AI or custom endpoints for suggestions and completions
- **Running Terminal Commands**: Executes shell or CLI commands directly from the IDE, displaying output in a custom console view
- **Automatic Web Search**: Performs search queries against external search APIs to supplement AI prompts with up-to-date documentation
- **Iterating on Lints**: Integrates AI-based code inspections and QuickFix suggestions, allowing automated fixes or improvements

### Context-Aware Chat

- Chat interface that leverages the agent to provide real-time Q&A based on the current file and recent conversation history
- References file names, line numbers, and code snippets in responses

### Tab Autocomplete

- AI-driven code completion that surfaces multi-line suggestions as you type
- Predictive text based on agent-processed context around the caret

### Linting & QuickFix Iteration

- Agent-driven inspections for common errors, security pitfalls, or style issues
- Automatic or one-click AI-generated fixes to maintain clean, consistent code

### Plugin Configuration & User Settings

- Control which agentic features are active, including enabling or disabling web search, code scanning, or completion triggers
- Provide and manage credentials for external AI or custom endpoints
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "aladin"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/zerubeus/aladin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Development Status

Aladin is currently under active development. The following features are being implemented:

- [ ] Core agent infrastructure
- [ ] Chat interface
- [ ] Code completion integration
- [ ] Codebase search capabilities
- [ ] Terminal command execution
- [ ] Web search integration
- [ ] Settings and configuration panel

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
