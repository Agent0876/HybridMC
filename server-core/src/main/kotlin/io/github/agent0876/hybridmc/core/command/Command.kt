package io.github.agent0876.hybridmc.core.command

/**
 * Represents an entity that can execute commands and receive feedback.
 */
interface CommandSender {
    val name: String
    fun sendMessage(message: String)
}

/**
 * Represents a command that can be executed on the server.
 */
interface Command {
    val name: String
    val aliases: List<String> get() = emptyList()
    val description: String get() = ""
    val usage: String get() = "/$name"

    fun execute(sender: CommandSender, args: List<String>)
}
