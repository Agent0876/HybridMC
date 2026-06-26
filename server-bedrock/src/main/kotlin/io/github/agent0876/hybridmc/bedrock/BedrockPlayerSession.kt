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
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * A Bedrock Edition player session bound to a single [RakNetConnection].
 *
 * Created by [BedrockPacketHandler.onConnect] when RakNet establishes the connection.
 * Implements [HybridPlayer] so it can be registered in the edition-agnostic [PlayerRegistry].
 *
 * ## Packet framing (modern Bedrock, 1.19.20+)
 *
 * The client first sends a **RequestNetworkSettings** (0xC1) packet.  That packet is sent
 * inside a "batch" envelope — the same 0xFE-prefixed frame used for all game packets:
 *
 * ```
 * [byte 0xFE]                  — batch packet marker
 * [byte compressionAlgorithm]  — 0xFF = none, 0x00 = zlib, 0x01 = snappy
 * [byte* payload]              — one or more length-prefixed inner game packets
 * ```
 *
 * Each inner game packet inside the decompressed payload is:
 * ```
 * [unsigned-varint length]
 * [byte* innerPayload]   — starts with the game packet ID as a varint
 * ```
 *
 * The server responds with a **NetworkSettings** (0x8C) packet (compression: none / threshold 0),
 * then the client proceeds to send the **Login** (0x01) packet, which can be zlib-compressed.
 */
class BedrockPlayerSession(
    private val connection: RakNetConnection,
    private val registry: PlayerRegistry,
) : HybridPlayer {

    private val logger = LoggerFactory.getLogger(BedrockPlayerSession::class.java)

    enum class LoginState {
        /** Waiting for RequestNetworkSettings (0xC1). */
        AWAITING_NETWORK_SETTINGS,
        /** Received NetworkSettings, waiting for Login (0x01). */
        AWAITING_LOGIN,
        /** Fully logged in and registered. */
        LOGGED_IN,
    }

    var loginState: LoginState = LoginState.AWAITING_NETWORK_SETTINGS
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
     * MCPE Text packet (game packet ID 0x09) wrapped in a batch envelope:
     * ```
     * [byte 0xFE] [compressionByte] [varint length] [byte 0x09] [byte type] [bool] [string] [string] [string]
     * ```
     */
    override fun sendMessage(text: String) {
        if (loginState != LoginState.LOGGED_IN) return
        val inner = Unpooled.buffer()
        try {
            writeVarInt(inner, 0x09)          // TextPacket
            inner.writeByte(0)                // type: RAW
            inner.writeBoolean(false)         // needsTranslation
            writeMcString(inner, text)
            writeMcString(inner, "")          // xuid
            writeMcString(inner, "")          // platformChatId
            sendBatch(inner)
        } finally {
            inner.release()
        }
    }

    /**
     * Sends a Disconnect packet (0x05) then closes the RakNet connection.
     */
    override fun disconnect(reason: String) {
        val inner = Unpooled.buffer()
        try {
            writeVarInt(inner, 0x05)          // DisconnectPacket
            inner.writeBoolean(false)         // hideDisconnectReason = false
            writeMcString(inner, reason)
            sendBatch(inner)
        } finally {
            inner.release()
        }
        connection.disconnect(DisconnectReason.SERVER_REQUESTED)
        registry.leave(this)
    }

    // ── Session internal API ──────────────────────────────────────────────────

    /**
     * Entry point called by [BedrockPacketHandler] for every RakNet application message.
     *
     * Expected outer format: `[0xFE] [compressionByte] [payload]`
     */
    fun handlePayload(payload: ByteBuf) {
        if (!payload.isReadable) return

        // 1. Expect 0xFE batch wrapper
        val firstByte = payload.readUnsignedByte().toInt()
        if (firstByte != 0xFE) {
            logger.warn("[BEDROCK] Unexpected first byte 0x{} from {}", firstByte.toString(16), connection.remoteAddress)
            return
        }

        // 2. Read compression algorithm byte
        if (!payload.isReadable) return
        val compressionByte = payload.readUnsignedByte().toInt()

        // 3. Decompress the remaining bytes
        val decompressed = when (compressionByte) {
            0xFF -> {                          // no compression
                payload.slice().retain()
            }
            0x00 -> {                          // zlib
                decompressZlib(payload)
            }
            else -> {
                logger.warn("[BEDROCK] Unknown compression algorithm 0x{}", compressionByte.toString(16))
                return
            }
        }

        try {
            // 4. Dispatch each inner packet
            while (decompressed.isReadable) {
                val innerLen = readUnsignedVarInt(decompressed)
                if (innerLen <= 0 || decompressed.readableBytes() < innerLen) {
                    logger.warn("[BEDROCK] Inner packet length mismatch: declared={} available={}", innerLen, decompressed.readableBytes())
                    break
                }
                val inner = decompressed.readSlice(innerLen)
                dispatchInnerPacket(inner)
            }
        } finally {
            decompressed.release()
        }
    }

    fun onDisconnect(reason: DisconnectReason) {
        logger.info("[BEDROCK] {} disconnected: {}", username, reason)
        if (loginState == LoginState.LOGGED_IN) registry.leave(this)
    }

    // ── Inner packet dispatcher ───────────────────────────────────────────────

    private fun dispatchInnerPacket(data: ByteBuf) {
        if (!data.isReadable) return
        val packetId = readUnsignedVarInt(data)
        logger.debug("[BEDROCK] Inner packet 0x{} (state={})", packetId.toString(16), loginState)

        when (packetId) {
            0xC1 -> handleRequestNetworkSettings(data)   // RequestNetworkSettings
            0x01 -> handleLogin(data)                    // Login
            else -> {
                if (loginState == LoginState.LOGGED_IN) {
                    logger.debug("[{}] Unhandled game packet 0x{}", username, packetId.toString(16))
                } else {
                    logger.debug("[BEDROCK] Ignoring packet 0x{} in state {}", packetId.toString(16), loginState)
                }
            }
        }
    }

    // ── Packet handlers ───────────────────────────────────────────────────────

    /**
     * Handles `RequestNetworkSettings` (0xC1):
     * ```
     * [int32 BE clientProtocol]
     * ```
     * Responds with `NetworkSettings` (0x8C) — compression threshold 0, algorithm 0 (zlib).
     */
    private fun handleRequestNetworkSettings(data: ByteBuf) {
        if (loginState != LoginState.AWAITING_NETWORK_SETTINGS) return
        if (data.readableBytes() < 4) return

        val clientProtocol = data.readInt()
        logger.info("[BEDROCK] RequestNetworkSettings: clientProtocol={} from {}", clientProtocol, connection.remoteAddress)

        // Build NetworkSettings (0x8C) inner payload
        val inner = Unpooled.buffer(7)
        try {
            writeVarInt(inner, 0x8C)           // NetworkSettings packet ID
            inner.writeShortLE(0)              // compressionThreshold (0 = compress everything)
            inner.writeShortLE(0)              // compressionAlgorithm (0 = zlib)
            inner.writeBoolean(false)          // clientThrottle
            inner.writeByte(0)                 // clientThrottleThreshold
            inner.writeFloatLE(0f)             // clientThrottleScalar

            // Send as uncompressed batch (0xFF = no compression) because compression
            // is not yet established at this point in the handshake.
            sendBatchUncompressed(inner)
        } finally {
            inner.release()
        }

        loginState = LoginState.AWAITING_LOGIN
        logger.debug("[BEDROCK] Sent NetworkSettings; transitioning to AWAITING_LOGIN")
    }

    /**
     * Parses the MCPE Login packet (0x01).
     *
     * Skeleton implementation — extracts the player name from the JWT chain payload
     * without full signature verification.
     *
     * Full MCPE Login format:
     * ```
     * [int protocol] [uint32-LE chainDataLength] [chainDataJson] [uint32-LE skinDataLength] [skinDataJson]
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
        val inner = Unpooled.buffer(6)
        try {
            writeVarInt(inner, 0x02)           // PlayStatusPacket
            inner.writeInt(status)             // big-endian int
            sendBatch(inner)
        } finally {
            inner.release()
        }
    }

    // ── Batch framing helpers ─────────────────────────────────────────────────

    /**
     * Wraps [inner] in a batch envelope and sends it over the RakNet connection.
     *
     * After the `NetworkSettings` exchange the spec says compression should be used;
     * we use zlib with the threshold negotiated (0 = always compress).
     * During the early handshake (AWAITING_NETWORK_SETTINGS) use [sendBatchUncompressed].
     */
    private fun sendBatch(inner: ByteBuf) {
        if (loginState == LoginState.AWAITING_NETWORK_SETTINGS) {
            sendBatchUncompressed(inner)
            return
        }
        // Build the length-prefixed inner packet payload
        val payloadBuf = Unpooled.buffer(inner.readableBytes() + 5)
        writeUnsignedVarInt(payloadBuf, inner.readableBytes())
        payloadBuf.writeBytes(inner, inner.readerIndex(), inner.readableBytes())

        // Compress with zlib
        val compressed = compressZlib(payloadBuf)
        payloadBuf.release()

        val out = Unpooled.buffer(2 + compressed.size)
        try {
            out.writeByte(0xFE)      // batch marker
            out.writeByte(0x00)      // zlib
            out.writeBytes(compressed)
            connection.send(out.retain(), Reliability.RELIABLE_ORDERED)
        } finally {
            out.release()
        }
    }

    /** Sends a batch with no compression (0xFF algorithm byte). */
    private fun sendBatchUncompressed(inner: ByteBuf) {
        val out = Unpooled.buffer(inner.readableBytes() + 7)
        try {
            out.writeByte(0xFE)      // batch marker
            out.writeByte(0xFF)      // no compression
            writeUnsignedVarInt(out, inner.readableBytes())
            out.writeBytes(inner, inner.readerIndex(), inner.readableBytes())
            connection.send(out.retain(), Reliability.RELIABLE_ORDERED)
        } finally {
            out.release()
        }
    }

    // ── Compression helpers ───────────────────────────────────────────────────

    private fun decompressZlib(src: ByteBuf): ByteBuf {
        val input = ByteArray(src.readableBytes())
        src.readBytes(input)
        val inflater = Inflater()
        inflater.setInput(input)
        val out = Unpooled.buffer(input.size * 2)
        val buf = ByteArray(4096)
        try {
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0 && inflater.needsInput()) break
                out.writeBytes(buf, 0, n)
            }
        } finally {
            inflater.end()
        }
        return out
    }

    private fun compressZlib(src: ByteBuf): ByteArray {
        val input = ByteArray(src.readableBytes())
        src.readBytes(input)
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, false)
        deflater.setInput(input)
        deflater.finish()
        val buf = ByteArray(4096)
        val baos = java.io.ByteArrayOutputStream()
        try {
            while (!deflater.finished()) {
                val n = deflater.deflate(buf)
                baos.write(buf, 0, n)
            }
        } finally {
            deflater.end()
        }
        return baos.toByteArray()
    }

    // ── VarInt helpers ────────────────────────────────────────────────────────

    private fun readUnsignedVarInt(buf: ByteBuf): Int {
        var result = 0
        var shift = 0
        while (shift < 35) {
            if (!buf.isReadable) return 0
            val b = buf.readUnsignedByte().toInt()
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
        return result
    }

    private fun writeVarInt(buf: ByteBuf, value: Int) {
        var v = value
        while (true) {
            if (v and 0x7F.inv() == 0) { buf.writeByte(v); return }
            buf.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun writeUnsignedVarInt(buf: ByteBuf, value: Int) = writeVarInt(buf, value)

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Naive JSON field extractor — pulls the first occurrence of `"key":"value"` or `"key": "value"`.
     * Good enough for skeleton-phase login parsing without a JSON dependency.
     */
    private fun extractExtraData(json: String, key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return pattern.find(json)?.groupValues?.get(1)
    }

    /** Writes a Minecraft Bedrock-style string: 2-byte unsigned LE length prefix + UTF-8 bytes. */
    private fun writeMcString(buf: ByteBuf, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        buf.writeShortLE(bytes.size)
        buf.writeBytes(bytes)
    }
}
