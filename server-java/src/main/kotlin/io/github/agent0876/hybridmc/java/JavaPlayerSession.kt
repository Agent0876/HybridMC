package io.github.agent0876.hybridmc.java

import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.HybridPlayer
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.netty5.buffer.Buffer
import io.netty5.channel.ChannelHandlerContext
import java.net.InetSocketAddress
import java.util.UUID

class JavaPlayerSession(
    private val ctx: ChannelHandlerContext,
    override val uuid: UUID,
    override val username: String,
    private val registry: PlayerRegistry,
) : HybridPlayer {

    override val edition: Edition = Edition.JAVA

    override var ping: Int = 0
        internal set

    val remoteAddress: InetSocketAddress
        get() = ctx.channel().remoteAddress() as InetSocketAddress

    override fun sendMessage(text: String) {
    }

    override fun disconnect(reason: String) {
        ctx.channel().close().addListener {
            registry.leave(this@JavaPlayerSession)
        }
    }

    fun writeAndFlush(buf: Buffer) {
        ctx.writeAndFlush(buf)
    }
}
