package io.github.agent0876.hybridmc.java

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import io.github.agent0876.hybridmc.core.ServerLifecycle
import io.netty5.bootstrap.ServerBootstrap
import io.netty5.channel.ChannelInitializer
import io.netty5.channel.ChannelOption
import io.netty5.channel.MultithreadEventLoopGroup
import io.netty5.channel.nio.NioHandler
import io.netty5.channel.socket.SocketChannel
import io.netty5.channel.socket.nio.NioServerSocketChannel
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class JavaEditionServer(
    private val registry: PlayerRegistry,
    private val world: GameWorld,
    private val host: String = "0.0.0.0",
    private val port: Int = 25565,
    private val maxPlayers: Int = 100,
    private val motd: String = "§aHybridMC §7— Java + Bedrock",
) : ServerLifecycle {

    private val logger = LoggerFactory.getLogger(JavaEditionServer::class.java)

    private val bossGroup = MultithreadEventLoopGroup(1, NioHandler.newFactory())
    private val workerGroup = MultithreadEventLoopGroup(0, NioHandler.newFactory())

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
                        .addLast("packet-handler", JavaPacketHandler(registry, world, motd, maxPlayers))
                }
            })

        val future = bootstrap.bind(host, port)
        future.addListener(io.netty5.util.concurrent.FutureListener { f ->
            if (f.isSuccess) {
                logger.info("Java Edition server listening on {}:{}", host, port)
                cont.resume(Unit)
            } else {
                logger.error("Java Edition server failed to bind on {}:{}", host, port, f.cause())
                cont.resumeWithException(f.cause())
            }
        })

        cont.invokeOnCancellation { stop() }
    }

    override fun stop() {
        logger.info("Shutting down Java Edition server…")
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}
