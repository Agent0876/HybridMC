package io.github.agent0876.hybridmc.bedrock

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import io.github.agent0876.hybridmc.core.ServerLifecycle
import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.raknetty.transport.RakNetServerBootstrap
import io.netty5.channel.Channel
import io.netty5.channel.MultithreadEventLoopGroup
import io.netty5.channel.nio.NioHandler
import io.netty5.util.concurrent.Future
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

class BedrockEditionServer(
    private val registry: PlayerRegistry,
    private val world: GameWorld,
    private val host: String = "0.0.0.0",
    private val port: Int = 19132,
    private val portv6: Int = 19133,
    private val maxConnections: Int = 200,
    private val maxPlayers: Int = 20,
    private val description: String = "HybridMC — Bedrock Edition",
    private val gameMode: String = "Survival",
) : ServerLifecycle {

    override val edition: Edition = Edition.BEDROCK
    private val logger = LoggerFactory.getLogger(BedrockEditionServer::class.java)

    private val group = MultithreadEventLoopGroup(0, NioHandler.newFactory())
    private val serverGuid = Random.nextLong()

    private var bindFuture: Future<Channel>? = null

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
                        gameMode = gameMode,
                        registry = registry,
                        maxPlayers = maxPlayers,
                        port = port,
                        portv6 = portv6,
                    )
                }
                .maxConnections(maxConnections)
                .handler(BedrockPacketHandler(registry, world))

            val future = bootstrap.bind(InetSocketAddress(host, port))
            bindFuture = future

            future.addListener(io.netty5.util.concurrent.FutureListener { f ->
                if (f.isSuccess) {
                    logger.info("Bedrock Edition server (RakNet) listening on {}:{}", host, port)
                    cont.resume(Unit)
                } else {
                    logger.error("Bedrock Edition server failed to bind on {}:{}", host, port, f.cause())
                    cont.resumeWithException(f.cause())
                }
            })

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
