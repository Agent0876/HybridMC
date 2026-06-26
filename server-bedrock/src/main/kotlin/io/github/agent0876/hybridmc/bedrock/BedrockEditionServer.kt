package io.github.agent0876.hybridmc.bedrock

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import io.github.agent0876.hybridmc.core.ServerLifecycle
import io.github.agent0876.raknetty.transport.RakNetServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * Bedrock Edition server — listens on UDP port 19132 using RakNetty.
 */
class BedrockEditionServer(
    private val registry: PlayerRegistry,
    private val world: GameWorld,
    private val host: String = "0.0.0.0",
    private val port: Int = 19132,
    private val description: String = "HybridMC — Bedrock Edition",
) : ServerLifecycle {

    private val logger = LoggerFactory.getLogger(BedrockEditionServer::class.java)

    private val group = MultiThreadIoEventLoopGroup(0, NioIoHandler.newFactory())
    private val serverGuid = Random.nextLong()

    private var channelFuture: ChannelFuture? = null

    override suspend fun start(): Unit = suspendCancellableCoroutine { cont ->
        try {
            val bootstrap = RakNetServerBootstrap()
                .group(group)
                .serverGuid(serverGuid)
                .serverInfo {
                    MotdBuilder.build(
                        description = description,
                        serverGuid = serverGuid,
                        worldName = world.name,
                        gameMode = "Survival",
                        registry = registry
                    )
                }
                .maxConnections(200)
                .handler(BedrockPacketHandler(registry, world))

            val future = bootstrap.bind(InetSocketAddress(host, port))
            channelFuture = future

            future.addListener { f ->
                if (f.isSuccess) {
                    logger.info("Bedrock Edition server (RakNet) listening on {}:{}", host, port)
                    cont.resume(Unit)
                } else {
                    logger.error("Bedrock Edition server failed to bind on {}:{}", host, port, f.cause())
                    cont.resumeWithException(f.cause())
                }
            }

            cont.invokeOnCancellation { stop() }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }

    override fun stop() {
        logger.info("Shutting down Bedrock Edition server…")
        group.shutdownGracefully()
    }
}
