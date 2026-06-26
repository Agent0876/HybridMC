package io.github.agent0876.hybridmc.bedrock

import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.HybridPlayer
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.raknetty.core.connection.DisconnectReason
import io.github.agent0876.raknetty.core.connection.RakNetConnection
import io.github.agent0876.raknetty.core.protocol.Reliability
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * A Bedrock Edition player session bound to a single [RakNetConnection].
 *
 * Created by [BedrockPacketHandler.onConnect] when RakNet establishes the connection.
 * Implements [HybridPlayer] so it can be registered in the edition-agnostic [PlayerRegistry].
 *
 * The session starts in [LoginState.AWAITING_LOGIN]; after a valid Login packet is parsed
 * it transitions to [LoginState.LOGGED_IN] and joins the registry.
 */
class BedrockPlayerSession(
    private val connection: RakNetConnection,
    private val registry: PlayerRegistry,
) : HybridPlayer {

    private val logger = LoggerFactory.getLogger(BedrockPlayerSession::class.java)

    enum class LoginState { AWAITING_LOGIN, LOGGED_IN }

    var loginState: LoginState = LoginState.AWAITING_LOGIN
        private set

    // ── HybridPlayer ──────────────────────────────────────────────────────────

    override var uuid: UUID = UUID.randomUUID()
        private set

    override var username: String = "<unknown>"
        private set

    override val edition: Edition = Edition.BEDROCK

    /** Delegated to the underlying RakNet connection RTT estimate. */
    override val ping: Int get() = connection.ping

    /**
     * Sends a plain-text chat message to this player.
     *
     * MCPE Text packet (game packet ID 0x09):
     * ```
     * [byte 0x09] [byte type=0] [bool needsTranslation=false] [string message] [string xuid=""] [string platformChatId=""]
     * ```
     * Wrapped in a 0xFE game-wrapper header expected by MCPE.
     */
    override fun sendMessage(text: String) {
        if (loginState != LoginState.LOGGED_IN) return
        val buf = Unpooled.buffer()
        try {
            buf.writeByte(0xFE)         // MCPE game packet wrapper
            buf.writeByte(0x09)         // TextPacket
            buf.writeByte(0)            // type: RAW
            buf.writeBoolean(false)     // needsTranslation
            writeMcString(buf, text)
            writeMcString(buf, "")      // xuid
            writeMcString(buf, "")      // platformChatId
            connection.send(buf.retain(), Reliability.RELIABLE_ORDERED)
        } finally {
            buf.release()
        }
    }

    /**
     * Sends a Disconnect packet (0x05) then closes the RakNet connection.
     */
    override fun disconnect(reason: String) {
        val buf = Unpooled.buffer()
        try {
            buf.writeByte(0xFE)         // wrapper
            buf.writeByte(0x05)         // DisconnectPacket
            buf.writeBoolean(false)     // hideDisconnectReason = false
            writeMcString(buf, reason)
            connection.send(buf.retain(), Reliability.RELIABLE_ORDERED)
        } finally {
            buf.release()
        }
        connection.disconnect(DisconnectReason.SERVER_REQUESTED)
        registry.leave(this)
    }

    // ── Session internal API ──────────────────────────────────────────────────

    /**
     * Processes a raw MCPE game-layer payload (0xFE stripped by caller).
     * Only the Login packet (0x01) is handled in the skeleton phase.
     */
    fun handlePayload(payload: ByteBuf) {
        if (!payload.isReadable) return
        val gamePacketId = payload.readUnsignedByte().toInt()

        when (gamePacketId) {
            0x01 -> handleLogin(payload)     // LoginPacket
            else -> {
                if (loginState == LoginState.LOGGED_IN) {
                    logger.debug("[{}] Unhandled game packet 0x{}", username, gamePacketId.toString(16))
                }
            }
        }
    }

    fun onDisconnect(reason: DisconnectReason) {
        logger.info("[BEDROCK] {} disconnected: {}", username, reason)
        if (loginState == LoginState.LOGGED_IN) registry.leave(this)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parses the MCPE Login packet (0x01).
     *
     * Skeleton implementation — extracts the player name from the JWT chain payload
     * without full signature verification.
     *
     * Full MCPE Login format:
     * ```
     * [int protocol] [varlong chainDataLength] [chainDataJson] [varlong skinDataLength] [skinDataJson]
     * ```
     */
    private fun handleLogin(data: ByteBuf) {
        if (loginState != LoginState.AWAITING_LOGIN) return

        // Protocol version (big-endian int)
        if (data.readableBytes() < 4) return
        val protocol = data.readInt()
        logger.debug("[BEDROCK] Login protocol={} from {}", protocol, connection.remoteAddress)

        // Chain data JSON (little-endian unsigned int length + UTF-8 bytes)
        if (data.readableBytes() < 4) return
        val chainLen = data.readUnsignedIntLE().toInt()
        if (data.readableBytes() < chainLen) return
        val chainBytes = ByteArray(chainLen)
        data.readBytes(chainBytes)
        val chainJson = String(chainBytes, StandardCharsets.UTF_8)

        // Extract username from the last element of the chain array.
        // We use a simple regex to avoid pulling in a JSON library at skeleton stage.
        val extractedName = extractExtraData(chainJson, "displayName")
            ?: extractExtraData(chainJson, "identityPublicKey")?.takeLast(8)
            ?: "Bedrock${connection.remoteAddress.port}"

        val extractedUuid = extractExtraData(chainJson, "identity")
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: UUID.randomUUID()

        uuid     = extractedUuid
        username = extractedName
        loginState = LoginState.LOGGED_IN

        logger.info("[BEDROCK] Login: name={} uuid={}", username, uuid)

        // Send PlayStatus (0x02), status=0 (LOGIN_SUCCESS)
        sendPlayStatus(0)

        registry.join(this)
    }

    private fun sendPlayStatus(status: Int) {
        val buf = Unpooled.buffer(6)
        try {
            buf.writeByte(0xFE)
            buf.writeByte(0x02)         // PlayStatusPacket
            buf.writeInt(status)        // big-endian int
            connection.send(buf.retain(), Reliability.RELIABLE_ORDERED)
        } finally {
            buf.release()
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Naive JSON field extractor — pulls the first occurrence of `"key":"value"` or `"key": "value"`.
     * Good enough for skeleton-phase login parsing without a JSON dependency.
     */
    private fun extractExtraData(json: String, key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return pattern.find(json)?.groupValues?.get(1)
    }

    /** Writes a Minecraft-style string: 2-byte unsigned LE length prefix + UTF-8 bytes. */
    private fun writeMcString(buf: ByteBuf, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        buf.writeShortLE(bytes.size)
        buf.writeBytes(bytes)
    }
}
