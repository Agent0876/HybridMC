package io.github.agent0876.hybridmc.java

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.world.GameWorld
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Handles the Minecraft Java Edition protocol handshake and login flow.
 *
 * ## State machine
 * ```
 * HANDSHAKING ──(0x00 Handshake, nextState=1)──► STATUS
 *             ──(0x00 Handshake, nextState=2)──► LOGIN
 * STATUS      ──(0x00 Status Request)───────────► [send 0x00 Status Response, wait ping]
 *             ──(0x01 Ping Request)────────────► [echo pong, close]
 * LOGIN       ──(0x00 Login Start)──────────────► [send 0x02 Login Success, join]
 * ```
 *
 * Packet encoding follows the vanilla 1.20.x protocol specification.
 * VarInt reads/writes are handled by [readVarInt]/[writeVarInt] helpers below.
 *
 * @see <a href="https://wiki.vg/Protocol">wiki.vg/Protocol</a>
 */
@Suppress("MagicNumber")
class JavaPacketHandler(
    private val registry: PlayerRegistry,
    private val world: GameWorld,
    private val motd: String,
    private val maxPlayers: Int,
) : ChannelInboundHandlerAdapter() {

    private val logger = LoggerFactory.getLogger(JavaPacketHandler::class.java)

    private enum class State { HANDSHAKING, STATUS, LOGIN }

    private var state = State.HANDSHAKING
    private var session: JavaPlayerSession? = null

    // ──────────────────────────────────────────────────────────────────────────
    // Netty overrides
    // ──────────────────────────────────────────────────────────────────────────

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is ByteBuf) return
        try {
            handleRawBytes(ctx, msg)
        } finally {
            msg.release()
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        session?.let { registry.leave(it) }
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn("Java connection error from {}: {}", ctx.channel().remoteAddress(), cause.message)
        ctx.close()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // State machine
    // ──────────────────────────────────────────────────────────────────────────

    private fun handleRawBytes(ctx: ChannelHandlerContext, buf: ByteBuf) {
        // Minecraft Java packets: [ VarInt length ] [ VarInt packetId ] [ payload … ]
        // TCP may fragment, so we buffer until we have a full packet.
        while (buf.readableBytes() > 0) {
            buf.markReaderIndex()

            // Read packet length
            val length = buf.readVarIntOrNull() ?: run {
                buf.resetReaderIndex()
                return
            }
            if (buf.readableBytes() < length) {
                buf.resetReaderIndex()
                return
            }

            val packet = buf.readSlice(length)
            val packetId = packet.readVarInt()

            when (state) {
                State.HANDSHAKING -> handleHandshake(ctx, packetId, packet)
                State.STATUS      -> handleStatus(ctx, packetId, packet)
                State.LOGIN       -> handleLogin(ctx, packetId, packet)
            }
        }
    }

    // ── HANDSHAKING ───────────────────────────────────────────────────────────

    private fun handleHandshake(ctx: ChannelHandlerContext, packetId: Int, data: ByteBuf) {
        if (packetId != 0x00) {
            logger.warn("Unexpected packet 0x{} during HANDSHAKING from {}", packetId.toString(16), ctx.channel().remoteAddress())
            ctx.close()
            return
        }
        // 0x00 Handshake: protocolVersion(VarInt) serverAddress(String) serverPort(UShort) nextState(VarInt)
        data.readVarInt()           // protocolVersion (ignored at skeleton level)
        data.readString()           // serverAddress
        data.readShort()            // serverPort
        val nextState = data.readVarInt()

        state = when (nextState) {
            1    -> State.STATUS
            2    -> State.LOGIN
            else -> {
                logger.warn("Unknown nextState {} from {}", nextState, ctx.channel().remoteAddress())
                ctx.close()
                return
            }
        }
        logger.debug("Handshake from {} → state={}", ctx.channel().remoteAddress(), state)
    }

    // ── STATUS ────────────────────────────────────────────────────────────────

    private fun handleStatus(ctx: ChannelHandlerContext, packetId: Int, data: ByteBuf) {
        when (packetId) {
            0x00 -> sendStatusResponse(ctx)
            0x01 -> sendPong(ctx, data)
            else -> logger.warn("Unknown STATUS packet 0x{}", packetId.toString(16))
        }
    }

    private fun sendStatusResponse(ctx: ChannelHandlerContext) {
        // JSON formatted per https://wiki.vg/Server_List_Ping#Response
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
            buf.writeString(json)
        }
        logger.debug("Sent Status Response to {}", ctx.channel().remoteAddress())
    }

    private fun sendPong(ctx: ChannelHandlerContext, data: ByteBuf) {
        val payload = data.readLong()
        writePacket(ctx, 0x01) { buf -> buf.writeLong(payload) }
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    private fun handleLogin(ctx: ChannelHandlerContext, packetId: Int, data: ByteBuf) {
        when (packetId) {
            0x00 -> handleLoginStart(ctx, data)
            else -> logger.warn("Unknown LOGIN packet 0x{}", packetId.toString(16))
        }
    }

    private fun handleLoginStart(ctx: ChannelHandlerContext, data: ByteBuf) {
        // 0x00 Login Start: name(String) uuid(UUID — optional in older versions)
        val name = data.readString()
        val uuid = if (data.readableBytes() >= 16) {
            val high = data.readLong()
            val low  = data.readLong()
            UUID(high, low)
        } else {
            // Offline-mode UUID based on username
            UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(StandardCharsets.UTF_8))
        }

        logger.info("Login Start: name={} uuid={} from {}", name, uuid, ctx.channel().remoteAddress())

        // ── Send 0x02 Login Success ────────────────────────────────────────────
        writePacket(ctx, 0x02) { buf ->
            buf.writeLong(uuid.mostSignificantBits)
            buf.writeLong(uuid.leastSignificantBits)
            buf.writeString(name)
            buf.writeVarInt(0) // number of properties
        }

        val sess = JavaPlayerSession(ctx, uuid, name, registry)
        session = sess
        registry.join(sess)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Packet helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Writes a full Minecraft packet: [ length VarInt ][ packetId VarInt ][ body ].
     */
    private fun writePacket(ctx: ChannelHandlerContext, packetId: Int, body: (ByteBuf) -> Unit) {
        val bodyBuf = ctx.alloc().buffer()
        try {
            bodyBuf.writeVarInt(packetId)
            body(bodyBuf)

            val out = ctx.alloc().buffer()
            out.writeVarInt(bodyBuf.readableBytes())
            out.writeBytes(bodyBuf)
            ctx.writeAndFlush(out)
        } finally {
            bodyBuf.release()
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// ByteBuf extension helpers — VarInt and String encoding per wiki.vg
// ────────────────────────────────────────────────────────────────────────────

internal fun ByteBuf.readVarInt(): Int {
    var result = 0
    var shift = 0
    while (true) {
        val b = readByte().toInt()
        result = result or ((b and 0x7F) shl shift)
        if (b and 0x80 == 0) return result
        shift += 7
        require(shift < 32) { "VarInt too long" }
    }
}

internal fun ByteBuf.readVarIntOrNull(): Int? {
    if (!isReadable) return null
    var result = 0
    var shift = 0
    while (true) {
        if (!isReadable) return null
        val b = readByte().toInt()
        result = result or ((b and 0x7F) shl shift)
        if (b and 0x80 == 0) return result
        shift += 7
        if (shift >= 32) return null
    }
}

internal fun ByteBuf.writeVarInt(value: Int) {
    var v = value
    while (true) {
        if (v and 0x7F.inv() == 0) { writeByte(v); return }
        writeByte((v and 0x7F) or 0x80)
        v = v ushr 7
    }
}

internal fun ByteBuf.readString(): String {
    val length = readVarInt()
    val bytes = ByteArray(length)
    readBytes(bytes)
    return String(bytes, StandardCharsets.UTF_8)
}

internal fun ByteBuf.writeString(value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    writeVarInt(bytes.size)
    writeBytes(bytes)
}
