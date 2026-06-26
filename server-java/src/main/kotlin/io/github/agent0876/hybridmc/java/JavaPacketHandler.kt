package io.github.agent0876.hybridmc.java

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import io.netty5.buffer.Buffer
import io.netty5.channel.ChannelHandlerContext
import io.netty5.channel.SimpleChannelInboundHandler
import io.netty5.util.concurrent.FutureListener
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.UUID

class JavaPacketHandler(
    private val registry: PlayerRegistry,
    private val world: GameWorld,
    private val motd: String,
    private val maxPlayers: Int,
) : SimpleChannelInboundHandler<Buffer>() {

    private val logger = LoggerFactory.getLogger(JavaPacketHandler::class.java)

    private enum class State { HANDSHAKING, STATUS, LOGIN }

    private var state = State.HANDSHAKING
    private var session: JavaPlayerSession? = null

    override fun messageReceived(ctx: ChannelHandlerContext, msg: Buffer) {
        handleRawBytes(ctx, msg)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        session?.let { registry.leave(it) }
        super.channelInactive(ctx)
    }

    override fun channelExceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn("Java connection error from {}: {}", ctx.channel().remoteAddress(), cause.message)
        ctx.close()
    }

    private fun handleRawBytes(ctx: ChannelHandlerContext, buf: Buffer) {
        while (buf.readableBytes() > 0) {
            val savedOffset = buf.readerOffset()

            val length = buf.readVarIntOrNull() ?: run {
                buf.readerOffset(savedOffset)
                return
            }
            if (buf.readableBytes() < length) {
                buf.readerOffset(savedOffset)
                return
            }

            val packet = buf.readSplit(length)
            val packetId = packet.readUnsignedVarInt()

            when (state) {
                State.HANDSHAKING -> handleHandshake(ctx, packetId, packet)
                State.STATUS      -> handleStatus(ctx, packetId, packet)
                State.LOGIN       -> handleLogin(ctx, packetId, packet)
            }
            packet.close()
        }
    }

    private fun handleHandshake(ctx: ChannelHandlerContext, packetId: Int, data: Buffer) {
        if (packetId != 0x00) {
            logger.warn("Unexpected packet 0x{} during HANDSHAKING from {}", packetId.toString(16), ctx.channel().remoteAddress())
            ctx.close()
            return
        }
        data.readUnsignedVarInt()
        data.readMcString()
        data.readShort()
        val nextState = data.readUnsignedVarInt()

        state = when (nextState) {
            1    -> State.STATUS
            2    -> State.LOGIN
            else -> {
                logger.warn("Unknown nextState {} from {}", nextState, ctx.channel().remoteAddress())
                ctx.close()
                return
            }
        }
        logger.debug("Handshake from {} -> state={}", ctx.channel().remoteAddress(), state)
    }

    private fun handleStatus(ctx: ChannelHandlerContext, packetId: Int, data: Buffer) {
        when (packetId) {
            0x00 -> sendStatusResponse(ctx)
            0x01 -> sendPong(ctx, data)
            else -> logger.warn("Unknown STATUS packet 0x{}", packetId.toString(16))
        }
    }

    private fun sendStatusResponse(ctx: ChannelHandlerContext) {
        val online = registry.onlineCount
        val json = """
            {
              "version": { "name": "HybridMC 26.2", "protocol": 776 },
              "players": { "max": $maxPlayers, "online": $online, "sample": [] },
              "description": { "text": "$motd" },
              "enforcesSecureChat": false
            }
        """.trimIndent()

        writePacket(ctx, 0x00) { buf ->
            buf.writeMcString(json)
        }
        logger.debug("Sent Status Response to {}", ctx.channel().remoteAddress())
    }

    private fun sendPong(ctx: ChannelHandlerContext, data: Buffer) {
        val payload = data.readLong()
        writePacket(ctx, 0x01) { buf -> buf.writeLong(payload) }
        ctx.writeAndFlush(ctx.bufferAllocator().allocate(0)).addListener(FutureListener { ctx.close() })
    }

    private fun handleLogin(ctx: ChannelHandlerContext, packetId: Int, data: Buffer) {
        when (packetId) {
            0x00 -> handleLoginStart(ctx, data)
            else -> logger.warn("Unknown LOGIN packet 0x{}", packetId.toString(16))
        }
    }

    private fun handleLoginStart(ctx: ChannelHandlerContext, data: Buffer) {
        val name = data.readMcString()
        val uuid = if (data.readableBytes() >= 16) {
            val high = data.readLong()
            val low  = data.readLong()
            UUID(high, low)
        } else {
            UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(StandardCharsets.UTF_8))
        }

        logger.info("Login Start: name={} uuid={} from {}", name, uuid, ctx.channel().remoteAddress())

        writePacket(ctx, 0x02) { buf ->
            buf.writeLong(uuid.mostSignificantBits)
            buf.writeLong(uuid.leastSignificantBits)
            buf.writeMcString(name)
            buf.writeUnsignedVarInt(0)
        }

        val sess = JavaPlayerSession(ctx, uuid, name, registry)
        session = sess
        registry.join(sess)
    }

    private fun writePacket(ctx: ChannelHandlerContext, packetId: Int, body: (Buffer) -> Unit) {
        val alloc = ctx.bufferAllocator()
        val bodyBuf = alloc.allocate(64)
        try {
            bodyBuf.writeUnsignedVarInt(packetId)
            body(bodyBuf)

            val out = alloc.allocate(bodyBuf.readableBytes() + 5)
            out.writeUnsignedVarInt(bodyBuf.readableBytes())
            out.writeBytes(bodyBuf.copy())
            ctx.writeAndFlush(out)
        } finally {
            bodyBuf.close()
        }
    }
}

// -- Buffer extension helpers --

private fun Buffer.readUnsignedVarInt(): Int {
    var result = 0
    var shift = 0
    while (true) {
        val b = readUnsignedByte()
        result = result or ((b and 0x7F) shl shift)
        if (b and 0x80 == 0) return result
        shift += 7
        require(shift < 32) { "VarInt too long" }
    }
}

private fun Buffer.readVarIntOrNull(): Int? {
    if (readableBytes() == 0) return null
    val savedOffset = readerOffset()
    var result = 0
    var shift = 0
    while (true) {
        if (readableBytes() == 0) { readerOffset(savedOffset); return null }
        val b = readUnsignedByte()
        result = result or ((b and 0x7F) shl shift)
        if (b and 0x80 == 0) return result
        shift += 7
        if (shift >= 32) { readerOffset(savedOffset); return null }
    }
}

private fun Buffer.writeUnsignedVarInt(value: Int) {
    var v = value
    while (true) {
        if (v and 0x7F.inv() == 0) { writeUnsignedByte(v); return }
        writeUnsignedByte((v and 0x7F) or 0x80)
        v = v ushr 7
    }
}

private fun Buffer.readMcString(): String {
    val length = readUnsignedVarInt()
    val bytes = ByteArray(length)
    readBytes(bytes, 0, length)
    return String(bytes, StandardCharsets.UTF_8)
}

private fun Buffer.writeMcString(value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    writeUnsignedVarInt(bytes.size)
    writeBytes(bytes)
}
