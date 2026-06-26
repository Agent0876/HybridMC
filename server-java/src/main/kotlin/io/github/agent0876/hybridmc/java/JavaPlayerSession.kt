package io.github.agent0876.hybridmc.java

import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.HybridPlayer
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import java.net.InetSocketAddress
import java.util.UUID

/**
 * Represents a single Java Edition player session.
 *
 * Wraps the Netty [ChannelHandlerContext] and implements [HybridPlayer] so this
 * session is compatible with the edition-agnostic [PlayerRegistry] and [GameWorld].
 *
 * This skeleton stores basic player info received during the login handshake.
 * Full packet encoding (game data) can be added per-packet-type later.
 */
class JavaPlayerSession(
    private val ctx: ChannelHandlerContext,
    override val uuid: UUID,
    override val username: String,
    private val registry: PlayerRegistry,
) : HybridPlayer {

    override val edition: Edition = Edition.JAVA

    /** Latency is not yet measured at the TCP level; updated via KeepAlive packets. */
    override var ping: Int = 0
        internal set

    val remoteAddress: InetSocketAddress
        get() = ctx.channel().remoteAddress() as InetSocketAddress

    /**
     * Sends a raw text message to this Java player.
     *
     * In the skeleton phase this is a no-op — a full implementation would encode
     * a Chat Message packet (0x1C in 1.20+) and write it to [ctx].
     */
    override fun sendMessage(text: String) {
        // TODO: encode Chat packet and write to ctx
        // Packet ID 0x1C (System Chat Message), format: String(text) + Boolean(overlay)
    }

    /**
     * Disconnects this player by sending a Disconnect (Login) or Disconnect (Play) packet,
     * then closing the channel.
     *
     * In the skeleton phase we close the channel directly.
     */
    override fun disconnect(reason: String) {
        // TODO: send Disconnect packet with JSON reason before closing
        ctx.channel().close().addListener {
            registry.leave(this)
        }
    }

    /**
     * Sends a raw [ByteBuf] as a Minecraft packet frame.
     * The caller is responsible for correct VarInt length-prefix encoding.
     * [buf] ownership is transferred to Netty.
     */
    fun writeAndFlush(buf: ByteBuf) {
        ctx.writeAndFlush(buf)
    }
}
