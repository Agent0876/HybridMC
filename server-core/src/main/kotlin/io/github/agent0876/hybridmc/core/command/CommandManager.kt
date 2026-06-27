package io.github.agent0876.hybridmc.core.command

import io.github.agent0876.hybridmc.core.player.PlayerRegistry

/**
 * Manages and dispatches commands.
 */
class CommandManager(
    private val registry: PlayerRegistry,
    private val onStop: () -> Unit,
) {
    private val commands = mutableMapOf<String, Command>()

    init {
        register(HelpCommand(this))
        register(ListCommand(registry))
        register(StopCommand(onStop))
        register(SayCommand(registry))
    }

    /**
     * Registers a command and its aliases.
     */
    fun register(command: Command) {
        commands[command.name.lowercase()] = command
        command.aliases.forEach { alias ->
            commands[alias.lowercase()] = command
        }
    }

    /**
     * Executes a command line for a given sender.
     * Returns true if a command was found and executed, false otherwise.
     */
    fun execute(sender: CommandSender, commandLine: String): Boolean {
        val cleanLine = commandLine.trim().removePrefix("/")
        if (cleanLine.isEmpty()) return false

        val parts = cleanLine.split(Regex("\\s+"))
        val label = parts[0].lowercase()
        val args = parts.drop(1)

        val command = commands[label]
        if (command != null) {
            try {
                command.execute(sender, args)
            } catch (e: Exception) {
                sender.sendMessage("§cAn error occurred while executing this command: ${e.message}")
            }
            return true
        }

        sender.sendMessage("§cUnknown command. Type /help for help.")
        return false
    }

    /**
     * Returns a collection of all registered commands (excluding duplicate aliases).
     */
    fun getCommands(): Collection<Command> {
        return commands.values.distinct()
    }
}

// -- Default Commands --

class HelpCommand(private val manager: CommandManager) : Command {
    override val name = "help"
    override val description = "Shows a list of available commands"

    override fun execute(sender: CommandSender, args: List<String>) {
        sender.sendMessage("§a--- HybridMC Help ---")
        manager.getCommands().forEach { cmd ->
            sender.sendMessage("§e/${cmd.name} §7- ${cmd.description}")
        }
    }
}

class ListCommand(private val registry: PlayerRegistry) : Command {
    override val name = "list"
    override val description = "Lists all online players"

    override fun execute(sender: CommandSender, args: List<String>) {
        val players = registry.all()
        sender.sendMessage("§aPlayers online (${players.size}):")
        if (players.isEmpty()) {
            sender.sendMessage("§7No players online.")
        } else {
            players.forEach { p ->
                sender.sendMessage("§7- §f${p.username} §e(${p.edition.displayName}) §a[ping: ${p.ping}ms]")
            }
        }
    }
}

class StopCommand(private val onStop: () -> Unit) : Command {
    override val name = "stop"
    override val description = "Stops the hybrid server"

    override fun execute(sender: CommandSender, args: List<String>) {
        sender.sendMessage("§cStopping the server...")
        onStop()
    }
}

class SayCommand(private val registry: PlayerRegistry) : Command {
    override val name = "say"
    override val description = "Broadcasts a message from the server"
    override val usage = "/say <message>"

    override fun execute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: $usage")
            return
        }
        val message = args.joinToString(" ")
        registry.broadcast("§d[Server] $message")
    }
}
