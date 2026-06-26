package io.github.agent0876.hybridmc.bedrock

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import io.github.agent0876.raknetty.core.connection.DisconnectReason
import io.github.agent0876.raknetty.core.connection.RakNetConnection
import io.github.agent0876.raknetty.transport.SimpleRakNetHandler
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Application-layer Netty handler for the Bedrock Edition server.
 *
 * Extends [SimpleRakNetHandler] which dispatches:
 * - [onConnect]    → called once per new RakNet connection (post-handshake)
 * - [onMessage]    → called for each reassembled application-layer payload
 * - [onDisconnect] → called when the connection closes
 *
 * A [BedrockPlayerSession] is created per connection and keyed by the connection's
 * remote address string for O(1) lookup.
 *
 * The raw RakNet payload is forwarded as-is to [BedrockPlayerSession.handlePayload];
 * all batch decompression and inner-packet parsing happens inside the session.
 */
class BedrockPacketHandler(
    private val registry: PlayerRegistry,
    private val world: GameWorld,
) : SimpleRakNetHandler() {

    private val logger = LoggerFactory.getLogger(BedrockPacketHandler::class.java)

    /** Active sessions keyed by connection address string. */
    private val sessions = ConcurrentHashMap<String, BedrockPlayerSession>()

    // ── SimpleRakNetHandler callbacks ─────────────────────────────────────────

    override fun onConnect(ctx: ChannelHandlerContext, connection: RakNetConnection) {
        val key = connection.remoteAddress.toString()
        val session = BedrockPlayerSession(connection, registry)
        sessions[key] = session
        logger.info("[BEDROCK] Connection established from {} (guid={})", connection.remoteAddress, connection.guid)
    }

    /**
     * Dispatches an incoming RakNet payload to the correct [BedrockPlayerSession].
     *
     * [payload] is owned by this call — do NOT release it manually.
     */
    override fun onMessage(ctx: ChannelHandlerContext, connection: RakNetConnection, payload: ByteBuf) {
        val key = connection.remoteAddress.toString()
        val session = sessions[key] ?: run {
            logger.warn("[BEDROCK] Message from unknown connection {}", connection.remoteAddress)
            return
        }

        session.handlePayload(payload)
    }

    override fun onDisconnect(
        ctx: ChannelHandlerContext,
        connection: RakNetConnection,
        reason: DisconnectReason,
    ) {
        val key = connection.remoteAddress.toString()
        val session = sessions.remove(key)
        session?.onDisconnect(reason)
            ?: logger.debug("[BEDROCK] Disconnect from unknown connection {}: {}", connection.remoteAddress, reason)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn("[BEDROCK] Exception on pipeline: {}", cause.message, cause)
    }
}
