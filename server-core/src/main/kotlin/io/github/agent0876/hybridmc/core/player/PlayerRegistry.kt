package io.github.agent0876.hybridmc.core.player

import io.github.agent0876.hybridmc.core.command.CommandManager
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry of all currently connected [HybridPlayer]s.
 *
 * Both [server-java] and [server-bedrock] write to the same instance so that
 * cross-edition features (broadcasting, player count, etc.) work transparently.
 */
class PlayerRegistry {

    lateinit var commandManager: CommandManager

    private val logger = LoggerFactory.getLogger(PlayerRegistry::class.java)

    private val players = ConcurrentHashMap<UUID, HybridPlayer>()

    /** Number of players currently online (both editions). */
    val onlineCount: Int get() = players.size

    /** Snapshot of all connected players. Safe to iterate outside the EventLoop. */
    fun all(): Collection<HybridPlayer> = players.values.toList()

    /** Returns the player with [uuid], or `null` if not connected. */
    fun find(uuid: UUID): HybridPlayer? = players[uuid]

    /**
     * Registers [player] as connected.
     * Called by the edition-specific session after the login handshake succeeds.
     */
    fun join(player: HybridPlayer) {
        players[player.uuid] = player
        logger.info("[{}] {} joined (ping={}ms)", player.edition, player.username, player.ping)

        // Spawn this player to others, and others to this player
        players.values.forEach { existing ->
            if (existing != player) {
                existing.spawnPlayer(player)
                player.spawnPlayer(existing)
            }
        }

        broadcast("§e${player.username} joined the game.")
    }

    /**
     * Removes [player] from the registry.
     * Called by the edition-specific session on disconnect.
     */
    fun leave(player: HybridPlayer) {
        if (players.remove(player.uuid) != null) {
            logger.info("[{}] {} left", player.edition, player.username)

            // Remove this player from others
            players.values.forEach { remaining ->
                remaining.removePlayer(player)
            }

            broadcast("§e${player.username} left the game.")
        }
    }

    /**
     * Broadcasts [sender]'s movement to all other players.
     */
    fun broadcastMovement(sender: HybridPlayer) {
        players.values.forEach { receiver ->
            if (receiver != sender) {
                receiver.movePlayer(sender)
            }
        }
    }

    /**
     * Sends [message] to every connected player.
     * Runs each [sendMessage] call without blocking the caller.
     */
    fun broadcast(message: String) {
        players.values.forEach { it.sendMessage(message) }
    }
}
