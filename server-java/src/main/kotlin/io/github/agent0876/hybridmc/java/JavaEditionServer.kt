package io.github.agent0876.hybridmc.java

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import io.github.agent0876.hybridmc.core.ServerLifecycle
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Java Edition server — listens on TCP port 25565 and accepts Minecraft Java clients.
 *
 * The pipeline installed per connection:
 * ```
 * [LengthFieldBasedFrameDecoder] → [JavaPacketHandler]
 * ```
 *
 * Status (0x00) and Login (0x02) handling lives in [JavaPacketHandler].
 */
class JavaEditionServer(
    private val registry: PlayerRegistry,
    private val world: GameWorld,
    private val host: String = "0.0.0.0",
    private val port: Int = 25565,
    private val maxPlayers: Int = 100,
    private val motd: String = "§aHybridMC §7— Java + Bedrock",
) : ServerLifecycle {

    private val logger = LoggerFactory.getLogger(JavaEditionServer::class.java)

    private val bossGroup = MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory())
    private val workerGroup = MultiThreadIoEventLoopGroup(0, NioIoHandler.newFactory())

    override suspend fun start(): Unit = suspendCancellableCoroutine { cont ->
        val bootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline()
                        // Minecraft uses variable-length packets prefixed with a VarInt length.
                        // JavaPacketHandler decodes them in a stateful way per connection.
                        .addLast("packet-handler", JavaPacketHandler(registry, world, motd, maxPlayers))
                }
            })

        val future = bootstrap.bind(host, port)
        future.addListener { f ->
            if (f.isSuccess) {
                logger.info("Java Edition server listening on {}:{}", host, port)
                cont.resume(Unit)
            } else {
                logger.error("Java Edition server failed to bind on {}:{}", host, port, f.cause())
                cont.resumeWithException(f.cause())
            }
        }

        cont.invokeOnCancellation { stop() }
    }

    override fun stop() {
        logger.info("Shutting down Java Edition server…")
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}
