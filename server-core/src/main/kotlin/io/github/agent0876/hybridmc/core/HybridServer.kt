package io.github.agent0876.hybridmc.core

import io.github.agent0876.hybridmc.core.command.CommandManager
import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Top-level server that manages [playerRegistry] and [world], and coordinates
 * one or more edition-specific sub-servers (Java, Bedrock, etc.).
 *
 * Usage:
 * ```kotlin
 * val server = HybridServer()
 * server.install(JavaEditionServer(...))
 * server.install(BedrockEditionServer(...))
 * server.start()
 * ```
 */
class HybridServer(
    val playerRegistry: PlayerRegistry = PlayerRegistry(),
) {
    init {
        playerRegistry.commandManager = CommandManager(playerRegistry) { stop() }
    }

    private val logger = LoggerFactory.getLogger(HybridServer::class.java)

    val world: GameWorld = GameWorld(registry = playerRegistry)

    private val servers = mutableListOf<ServerLifecycle>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Registers an edition sub-server. Must be called before [start]. */
    fun install(server: ServerLifecycle) {
        servers.add(server)
    }

    /** All registered edition servers. */
    fun installed(): List<ServerLifecycle> = servers.toList()

    /** Starts all edition servers concurrently, then blocks until [stop] is called. */
    fun start() {
        val editions = servers.map { it.edition.displayName }
        logger.info("==============================================")
        logger.info("  HybridMC — {}", editions.joinToString(" + "))
        logger.info("==============================================")

        servers.forEach { server ->
            scope.launch { server.start() }
        }

        Runtime.getRuntime().addShutdownHook(Thread({
            logger.info("Shutdown signal received — stopping HybridMC…")
            stop()
        }, "hybridmc-shutdown"))

        logger.info("HybridMC is running. Press Ctrl+C to stop.")

        runBlocking { scope.coroutineContext[kotlinx.coroutines.Job]!!.join() }
    }

    /** Gracefully stops all servers and cancels coroutines. */
    fun stop() {
        world.broadcastMessage("§cServer is shutting down…")
        servers.forEach { it.stop() }
        scope.cancel()
        logger.info("HybridMC stopped.")
    }
}

interface ServerLifecycle {
    val edition: Edition
    suspend fun start()
    fun stop()
}
