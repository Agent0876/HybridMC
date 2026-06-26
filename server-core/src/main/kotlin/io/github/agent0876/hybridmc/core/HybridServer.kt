package io.github.agent0876.hybridmc.core

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
 * the Java Edition and Bedrock Edition sub-servers.
 *
 * Usage:
 * ```kotlin
 * val server = HybridServer()
 * server.start()
 * ```
 *
 * The caller registers a shutdown hook or calls [stop] explicitly to cleanly
 * shut down both protocol stacks.
 */
class HybridServer(
    val playerRegistry: PlayerRegistry = PlayerRegistry(),
) {
    private val logger = LoggerFactory.getLogger(HybridServer::class.java)

    val world: GameWorld = GameWorld(registry = playerRegistry)

    /**
     * Supplier that provides the Java sub-server.
     * Set by the application entry-point after injecting shared context.
     */
    var javaServer: ServerLifecycle? = null

    /**
     * Supplier that provides the Bedrock sub-server.
     * Set by the application entry-point after injecting shared context.
     */
    var bedrockServer: ServerLifecycle? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Starts both edition servers concurrently, then blocks until [stop] is called. */
    fun start() {
        logger.info("==============================================")
        logger.info("  HybridMC — Java + Bedrock Hybrid Server")
        logger.info("==============================================")

        val java    = requireNotNull(javaServer)    { "javaServer must be set before start()" }
        val bedrock = requireNotNull(bedrockServer) { "bedrockServer must be set before start()" }

        scope.launch { java.start() }
        scope.launch { bedrock.start() }

        // Register JVM shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(Thread({
            logger.info("Shutdown signal received — stopping HybridMC…")
            stop()
        }, "hybridmc-shutdown"))

        logger.info("HybridMC is running. Press Ctrl+C to stop.")

        // Block the main thread until scope is cancelled
        runBlocking { scope.coroutineContext[kotlinx.coroutines.Job]!!.join() }
    }

    /** Gracefully stops both servers and cancels all coroutines. */
    fun stop() {
        world.broadcastMessage("§cServer is shutting down…")
        javaServer?.stop()
        bedrockServer?.stop()
        scope.cancel()
        logger.info("HybridMC stopped.")
    }
}

/** Minimal lifecycle contract implemented by [JavaEditionServer] and [BedrockEditionServer]. */
interface ServerLifecycle {
    suspend fun start()
    fun stop()
}
