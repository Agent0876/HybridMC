package io.github.agent0876.hybridmc.core.player

import java.util.UUID

/**
 * Common abstraction for a connected player regardless of edition.
 *
 * Both [Edition.JAVA] and [Edition.BEDROCK] players implement this interface so that
 * game logic in [server-core] and [GameWorld] can treat all players uniformly.
 */
interface HybridPlayer {

    /** Unique player identifier (randomly generated per session if not authenticated). */
    val uuid: UUID

    /** In-game display name / username. */
    val username: String

    /** Which Minecraft edition this player connected from. */
    val edition: Edition

    /** Current round-trip time in milliseconds. */
    val ping: Int

    /**
     * Sends a plain-text chat message to this player.
     * Implementations must handle edition-specific formatting internally.
     */
    fun sendMessage(text: String)

    /**
     * Disconnects this player with a human-readable [reason].
     * The implementation sends the appropriate disconnect packet before closing the session.
     */
    fun disconnect(reason: String = "Disconnected")
}
