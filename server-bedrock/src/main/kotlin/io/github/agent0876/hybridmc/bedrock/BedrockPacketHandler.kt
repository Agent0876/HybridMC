package io.github.agent0876.hybridmc.bedrock

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import io.github.agent0876.raknetty.core.connection.DisconnectReason
import io.github.agent0876.raknetty.core.connection.RakNetConnection
import io.github.agent0876.raknetty.transport.SimpleRakNetHandler
import io.netty5.buffer.Buffer
import io.netty5.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class BedrockPacketHandler(
    private val registry: PlayerRegistry,
    private val world: GameWorld,
) : SimpleRakNetHandler() {

    private val logger = LoggerFactory.getLogger(BedrockPacketHandler::class.java)

    private val sessions = ConcurrentHashMap<String, BedrockPlayerSession>()

    override fun onConnect(ctx: ChannelHandlerContext, connection: RakNetConnection) {
        val key = connection.remoteAddress.toString()
        val session = BedrockPlayerSession(connection, registry)
        sessions[key] = session
        logger.info("[BEDROCK] Connection established from {} (guid={})", connection.remoteAddress, connection.guid)
    }

    override fun onMessage(ctx: ChannelHandlerContext, connection: RakNetConnection, payload: Buffer) {
        val key = connection.remoteAddress.toString()
        val session = sessions.get(key) ?: run {
            logger.warn("[BEDROCK] Message from unknown connection {}", connection.remoteAddress)
            return
        }

        val payloadCopy = payload.copy()
        try {
            session.handlePayload(payloadCopy)
        } finally {
            payloadCopy.close()
        }
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

    override fun channelExceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn("[BEDROCK] Exception on pipeline: {}", cause.message, cause)
    }
}
