package io.github.agent0876.hybridmc.bedrock

import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.HybridPlayer
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.raknetty.core.connection.DisconnectReason
import io.github.agent0876.raknetty.core.connection.RakNetConnection
import io.github.agent0876.raknetty.core.protocol.Reliability
import io.netty5.buffer.Buffer
import io.netty5.buffer.BufferAllocator
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.Inflater

class BedrockPlayerSession(
    private val connection: RakNetConnection,
    private val registry: PlayerRegistry,
) : HybridPlayer {

    private val logger = LoggerFactory.getLogger(BedrockPlayerSession::class.java)

    private val allocator = BufferAllocator.onHeapUnpooled()

    enum class LoginState {
        AWAITING_NETWORK_SETTINGS,
        AWAITING_LOGIN,
        LOGGED_IN,
    }

    var loginState: LoginState = LoginState.AWAITING_NETWORK_SETTINGS
        private set

    // -- HybridPlayer --

    override var uuid: UUID = UUID.randomUUID()
        private set

    override var username: String = "<unknown>"
        private set

    override val edition: Edition = Edition.BEDROCK

    override val ping: Int get() = connection.ping

    override fun sendMessage(message: String) {
        if (loginState != LoginState.LOGGED_IN) return
        val inner = allocator.allocate(256)
        try {
            writeVarInt(inner, 0x09)
            inner.writeUnsignedByte(0)
            inner.writeBoolean(false)
            writeMcString(inner, message)
            writeMcString(inner, "")
            writeMcString(inner, "")
            sendBatch(inner)
        } finally {
            inner.close()
        }
    }

    override fun disconnect(reason: String) {
        val inner = allocator.allocate(128)
        try {
            writeVarInt(inner, 0x05)
            inner.writeBoolean(false)
            writeMcString(inner, reason)
            sendBatch(inner)
        } finally {
            inner.close()
        }
        connection.disconnect(DisconnectReason.SERVER_REQUESTED)
        registry.leave(this)
    }

    // -- Session internal API --

    fun handlePayload(payload: Buffer) {
        if (payload.readableBytes() == 0) return

        val firstByte = payload.readUnsignedByte()
        if (firstByte != 0xFE) {
            logger.warn("[BEDROCK] Unexpected first byte 0x{} from {}", firstByte.toString(16), connection.remoteAddress)
            return
        }

        if (loginState == LoginState.AWAITING_NETWORK_SETTINGS) {
            dispatchBatchPayload(payload)
            return
        }

        if (payload.readableBytes() == 0) return
        val compressionByte = payload.readUnsignedByte()

        val decompressed = when (compressionByte) {
            0xFF -> {
                payload.copy()
            }
            0x00 -> {
                decompressZlib(payload)
            }
            else -> {
                logger.warn("[BEDROCK] Unknown compression algorithm 0x{}", compressionByte.toString(16))
                return
            }
        }

        try {
            dispatchBatchPayload(decompressed)
        } finally {
            decompressed.close()
        }
    }

    fun onDisconnect(reason: DisconnectReason) {
        logger.info("[BEDROCK] {} disconnected: {}", username, reason)
        if (loginState == LoginState.LOGGED_IN) registry.leave(this)
    }

    // -- Inner packet dispatcher --

    private fun dispatchBatchPayload(data: Buffer) {
        while (data.readableBytes() > 0) {
            val innerLen = readUnsignedVarInt(data)
            if (innerLen <= 0 || data.readableBytes() < innerLen) {
                logger.warn("[BEDROCK] Inner packet length mismatch: declared={} available={}", innerLen, data.readableBytes())
                break
            }
            val inner = data.readSplit(innerLen)
            dispatchInnerPacket(inner)
            inner.close()
        }
    }

    private fun dispatchInnerPacket(data: Buffer) {
        if (data.readableBytes() == 0) return
        val packetId = readUnsignedVarInt(data)
        logger.debug("[BEDROCK] Inner packet 0x{} (state={})", packetId.toString(16), loginState)

        when (packetId) {
            0xC1 -> handleRequestNetworkSettings(data)
            0x01 -> handleLogin(data)
            0x09 -> handleTextPacket(data)
            0x4D -> handleCommandRequest(data)
            else -> {
                if (loginState == LoginState.LOGGED_IN) {
                    logger.debug("[{}] Unhandled game packet 0x{}", username, packetId.toString(16))
                } else {
                    logger.debug("[BEDROCK] Ignoring packet 0x{} in state {}", packetId.toString(16), loginState)
                }
            }
        }
    }

    // -- Packet handlers --

    private fun handleRequestNetworkSettings(data: Buffer) {
        if (loginState != LoginState.AWAITING_NETWORK_SETTINGS) return
        if (data.readableBytes() < 4) return

        val clientProtocol = data.readInt()
        logger.info("[BEDROCK] RequestNetworkSettings: clientProtocol={} from {}", clientProtocol, connection.remoteAddress)

        val inner = allocator.allocate(7)
        try {
            writeVarInt(inner, 0x8F)
            inner.writeUnsignedShort(0)
            inner.writeUnsignedShort(0)
            inner.writeBoolean(false)
            inner.writeUnsignedByte(0)
            inner.writeInt(0)
            sendBatchPreCompression(inner)
        } finally {
            inner.close()
        }

        loginState = LoginState.AWAITING_LOGIN
        logger.debug("[BEDROCK] Sent NetworkSettings; transitioning to AWAITING_LOGIN")
    }

    private fun handleLogin(data: Buffer) {
        if (loginState != LoginState.AWAITING_LOGIN) return

        if (data.readableBytes() < 4) return
        val protocol = data.readInt()
        logger.debug("[BEDROCK] Login protocol={} from {}", protocol, connection.remoteAddress)

        if (data.readableBytes() < 4) return
        val chainLen = readUnsignedIntLE(data)
        if (chainLen <= 0 || data.readableBytes() < chainLen) return
        val chainBytes = ByteArray(chainLen)
        data.readBytes(ByteBuffer.wrap(chainBytes))
        val chainJson = String(chainBytes, StandardCharsets.UTF_8)

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

        sendPlayStatus(0)

        registry.join(this)
    }

    private fun handleTextPacket(data: Buffer) {
        if (loginState != LoginState.LOGGED_IN) return
        data.readUnsignedByte()
        data.readBoolean()
        if (data.readableBytes() == 0) return
        val message = readMcString(data)
        logger.info("[BEDROCK] {}: {}", username, message)
        val formatted = "§e<${username}> $message"
        registry.broadcast(formatted)
    }

    private fun handleCommandRequest(data: Buffer) {
        if (loginState != LoginState.LOGGED_IN) return
        val command = readMcString(data)
        logger.info("[BEDROCK] {} executed command: {}", username, command)
        registry.commandManager.execute(this, command)
    }

    private fun sendPlayStatus(status: Int) {
        val inner = allocator.allocate(6)
        try {
            writeVarInt(inner, 0x02)
            inner.writeInt(status)
            sendBatch(inner)
        } finally {
            inner.close()
        }
    }

    // -- Batch framing helpers --

    private fun sendBatch(inner: Buffer) {
        if (loginState == LoginState.AWAITING_NETWORK_SETTINGS) {
            sendBatchPreCompression(inner)
            return
        }

        val payloadBuf = allocator.allocate(inner.readableBytes() + 5)
        writeUnsignedVarInt(payloadBuf, inner.readableBytes())
        val innerBytes = ByteArray(inner.readableBytes())
        inner.copyInto(inner.readerOffset(), innerBytes, 0, innerBytes.size)
        payloadBuf.writeBytes(innerBytes)

        val compressed = compressZlib(payloadBuf)
        payloadBuf.close()

        val out = allocator.allocate(2 + compressed.size)
        out.writeUnsignedByte(0xFE)
        out.writeUnsignedByte(0x00)
        out.writeBytes(compressed)
        val toSend = out.split()
        connection.send(toSend, Reliability.RELIABLE_ORDERED)
        out.close()
    }

    private fun sendBatchPreCompression(inner: Buffer) {
        val out = allocator.allocate(inner.readableBytes() + 6)
        out.writeUnsignedByte(0xFE)
        writeUnsignedVarInt(out, inner.readableBytes())
        val innerBytes = ByteArray(inner.readableBytes())
        inner.copyInto(inner.readerOffset(), innerBytes, 0, innerBytes.size)
        out.writeBytes(innerBytes)
        val toSend = out.split()
        connection.send(toSend, Reliability.RELIABLE_ORDERED)
        out.close()
    }

    // -- Compression helpers --

    private fun decompressZlib(src: Buffer): Buffer {
        val input = ByteArray(src.readableBytes())
        src.readBytes(ByteBuffer.wrap(input))
        val inflater = Inflater()
        inflater.setInput(input)
        val outBuf = java.io.ByteArrayOutputStream(input.size * 2)
        val buf = ByteArray(4096)
        try {
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0 && inflater.needsInput()) break
                outBuf.write(buf, 0, n)
            }
        } finally {
            inflater.end()
        }
        val decompressed = allocator.allocate(outBuf.size())
        decompressed.writeBytes(outBuf.toByteArray())
        return decompressed
    }

    private fun compressZlib(src: Buffer): ByteArray {
        val input = ByteArray(src.readableBytes())
        src.copyInto(src.readerOffset(), input, 0, input.size)
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

    // -- VarInt helpers --

    private fun readUnsignedVarInt(buf: Buffer): Int {
        var result = 0
        var shift = 0
        while (shift < 35) {
            if (buf.readableBytes() == 0) return 0
            val b = buf.readUnsignedByte()
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
        return result
    }

    private fun writeVarInt(buf: Buffer, value: Int) {
        var v = value
        while (true) {
            if (v and 0x7F.inv() == 0) { buf.writeUnsignedByte(v); return }
            buf.writeUnsignedByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun writeUnsignedVarInt(buf: Buffer, value: Int) = writeVarInt(buf, value)

    // -- Little-endian helpers --

    private fun readUnsignedIntLE(buf: Buffer): Int {
        val b0 = buf.readUnsignedByte()
        val b1 = buf.readUnsignedByte()
        val b2 = buf.readUnsignedByte()
        val b3 = buf.readUnsignedByte()
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }

    // -- Utility --

    private fun extractExtraData(json: String, key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun readMcString(buf: Buffer): String {
        val len = buf.readUnsignedShort()
        val bytes = ByteArray(len)
        buf.readBytes(java.nio.ByteBuffer.wrap(bytes))
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun writeMcString(buf: Buffer, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        buf.writeUnsignedShort(bytes.size)
        buf.writeBytes(bytes)
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
