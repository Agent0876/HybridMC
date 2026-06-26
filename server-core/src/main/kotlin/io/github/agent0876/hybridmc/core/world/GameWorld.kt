package io.github.agent0876.hybridmc.core.world

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import org.slf4j.LoggerFactory

/**
 * Stub game world shared by both editions.
 *
 * This is a skeleton implementation — expand with chunk storage, entity tracking,
 * and game logic as the project grows.
 */
class GameWorld(
    val name: String = "world",
    val registry: PlayerRegistry,
) {

    private val logger = LoggerFactory.getLogger(GameWorld::class.java)

    init {
        logger.info("GameWorld '{}' initialised", name)
    }

    /**
     * Broadcasts [message] to every player in this world.
     * Delegates to [PlayerRegistry.broadcast] so both editions receive it.
     */
    fun broadcastMessage(message: String) {
        logger.debug("broadcast → {}", message)
        registry.broadcast(message)
    }
}
