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

    private enum class State { HANDSHAKING, STATUS, LOGIN, PLAY }

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
                State.PLAY        -> handlePlay(ctx, packetId, packet)
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

    // -- STATUS --

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

    // -- LOGIN --

    private fun handleLogin(ctx: ChannelHandlerContext, packetId: Int, data: Buffer) {
        when (packetId) {
            0x00 -> handleLoginStart(ctx, data)
            else -> logger.warn("Unknown LOGIN packet 0x{}", packetId.toString(16))
        }
    }

    private var nextEntityId = 1

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

        val entityId = nextEntityId++
        val sess = JavaPlayerSession(ctx, uuid, name, entityId, registry)
        session = sess
        registry.join(sess)

        sendLoginPlay(ctx, sess)
        state = State.PLAY
        logger.info("{} entered PLAY state (entityId={})", name, entityId)
    }

    private fun sendLoginPlay(ctx: ChannelHandlerContext, sess: JavaPlayerSession) {
        writePacket(ctx, 0x28) { buf ->
            buf.writeInt(sess.entityId)
            buf.writeBoolean(false)
            buf.writeUnsignedByte(1)
            buf.writeByte(-1)
            buf.writeUnsignedVarInt(1)
            buf.writeMcString("minecraft:overworld")
            writeRegistryCodec(buf)
            writeOverworldDimension(buf)
            buf.writeMcString("minecraft:overworld")
            buf.writeLong(0L)
            buf.writeUnsignedVarInt(maxPlayers)
            buf.writeUnsignedVarInt(10)
            buf.writeUnsignedVarInt(10)
            buf.writeBoolean(false)
            buf.writeBoolean(true)
            buf.writeBoolean(false)
            buf.writeBoolean(false)
        }
    }

    private fun writeRegistryCodec(buf: Buffer) {
        val codec = NbtWriter()
        codec.compound {
            codec.tagCompound("minecraft:dimension_type") {
                codec.writeString("minecraft:dimension_type")
                codec.tagList("value", 10.toByte()) {
                    dimensionEntry(codec, "minecraft:overworld", overworldElement = true)
                    dimensionEntry(codec, "minecraft:the_nether", overworldElement = false)
                    dimensionEntry(codec, "minecraft:the_end", overworldElement = false)
                }
            }
            codec.tagCompound("minecraft:worldgen/biome") {
                codec.writeString("minecraft:worldgen/biome")
                codec.tagList("value", 10.toByte()) {
                    biomeEntry(codec, "minecraft:plains", 7907327, 329011, 12638463, 4159204)
                    biomeEntry(codec, "minecraft:desert", 7254527, 329011, 12638463, 16759876)
                    biomeEntry(codec, "minecraft:forest", 7907327, 329011, 12638463, 4159204)
                    biomeEntry(codec, "minecraft:ocean", 7907327, 329011, 12638463, 4159204)
                }
            }
            codec.tagCompound("minecraft:chat_type") {
                codec.writeString("minecraft:chat_type")
                codec.tagList("value", 10.toByte()) {
                    chatTypeEntry(codec, "minecraft:chat", 0)
                    chatTypeEntry(codec, "minecraft:say_command", 1)
                    chatTypeEntry(codec, "minecraft:msg_command_incoming", 2)
                    chatTypeEntry(codec, "minecraft:msg_command_outgoing", 3)
                    chatTypeEntry(codec, "minecraft:team_msg_command_incoming", 4)
                    chatTypeEntry(codec, "minecraft:team_msg_command_outgoing", 5)
                    chatTypeEntry(codec, "minecraft:emote_command", 6)
                }
            }
        }
        val nbtBytes = ByteArray(codec.buffer().readableBytes())
        codec.buffer().copyInto(codec.buffer().readerOffset(), nbtBytes, 0, nbtBytes.size)
        buf.writeBytes(nbtBytes)
        codec.buffer().close()
    }

    private fun dimensionEntry(codec: NbtWriter, name: String, overworldElement: Boolean) {
        codec.compound {
            codec.writeString("minecraft:dimension_type")
            codec.tagString("name", name)
            codec.tagInt("id", if (overworldElement) 0 else 1)
            codec.tagCompound("element") {
                codec.tagInt("piglin_safe", 0)
                codec.tagInt("natural", if (overworldElement) 1 else 0)
                codec.tagFloat("ambient_light", if (overworldElement) 0.0f else 0.1f)
                codec.tagLong("fixed_time", -1L)
                codec.tagString("infiniburn", if (overworldElement) "#minecraft:infiniburn_overworld" else "#minecraft:infiniburn_nether")
                codec.tagInt("respawn_anchor_works", if (overworldElement) 0 else 1)
                codec.tagInt("has_skylight", if (overworldElement) 1 else 0)
                codec.tagInt("bed_works", if (overworldElement) 1 else 0)
                codec.tagString("effects", if (overworldElement) "minecraft:overworld" else "minecraft:the_nether")
                codec.tagInt("has_raids", if (overworldElement) 1 else 0)
                codec.tagInt("min_y", if (overworldElement) -64 else 0)
                codec.tagInt("height", 384)
                codec.tagInt("logical_height", if (overworldElement) 384 else 128)
                codec.tagFloat("coordinate_scale", if (overworldElement) 1.0f else 8.0f)
                codec.tagInt("ultrawarm", if (overworldElement) 0 else 1)
                codec.tagInt("has_ceiling", if (overworldElement) 0 else 1)
            }
        }
    }

    private fun biomeEntry(codec: NbtWriter, name: String, skyColor: Int, waterFogColor: Int, fogColor: Int, waterColor: Int) {
        codec.compound {
            codec.writeString("minecraft:worldgen/biome")
            codec.tagString("name", name)
            codec.tagInt("id", 0)
            codec.tagCompound("element") {
                codec.tagString("precipitation", "rain")
                codec.tagFloat("temperature", 0.8f)
                codec.tagFloat("downfall", 0.4f)
                codec.tagCompound("effects") {
                    codec.tagInt("sky_color", skyColor)
                    codec.tagInt("water_fog_color", waterFogColor)
                    codec.tagInt("fog_color", fogColor)
                    codec.tagInt("water_color", waterColor)
                }
            }
        }
    }

    private fun chatTypeEntry(codec: NbtWriter, name: String, id: Int) {
        codec.compound {
            codec.writeString("minecraft:chat_type")
            codec.tagString("name", name)
            codec.tagInt("id", id)
            codec.tagCompound("element") {
                codec.tagCompound("chat") {
                    codec.tagString("translation_key", "chat.type.text")
                    codec.tagList("parameters", 8.toByte()) {
                        codec.writeString("sender")
                        codec.writeString("content")
                    }
                }
            }
        }
    }

    private fun writeOverworldDimension(buf: Buffer) {
        val dim = NbtWriter()
        dim.compound {
            dim.tagInt("piglin_safe", 0)
            dim.tagInt("natural", 1)
            dim.tagFloat("ambient_light", 0.0f)
            dim.tagLong("fixed_time", -1L)
            dim.tagString("infiniburn", "#minecraft:infiniburn_overworld")
            dim.tagInt("respawn_anchor_works", 0)
            dim.tagInt("has_skylight", 1)
            dim.tagInt("bed_works", 1)
            dim.tagString("effects", "minecraft:overworld")
            dim.tagInt("has_raids", 1)
            dim.tagInt("min_y", -64)
            dim.tagInt("height", 384)
            dim.tagInt("logical_height", 384)
            dim.tagFloat("coordinate_scale", 1.0f)
            dim.tagInt("ultrawarm", 0)
            dim.tagInt("has_ceiling", 0)
        }
        val nbtBytes = ByteArray(dim.buffer().readableBytes())
        dim.buffer().copyInto(dim.buffer().readerOffset(), nbtBytes, 0, nbtBytes.size)
        buf.writeBytes(nbtBytes)
        dim.buffer().close()
    }

    // -- PLAY --

    private fun handlePlay(ctx: ChannelHandlerContext, packetId: Int, data: Buffer) {
        when (packetId) {
            0x15 -> handleKeepAlive(data)
            0x05 -> handleChatMessage(ctx, data)
            0x04 -> handleChatCommand(ctx, data)
            0x08 -> handleClientInformation(data)
            else -> logger.debug("[{}] Unhandled PLAY packet 0x{}", session?.username ?: "?", packetId.toString(16))
        }
    }

    private fun handleKeepAlive(data: Buffer) {
        val id = data.readLong()
        session?.handleKeepAliveResponse(id)
    }

    private fun handleChatMessage(ctx: ChannelHandlerContext, data: Buffer) {
        val message = data.readMcString()
        val username = session?.username ?: "?"
        logger.info("[{}] {}", username, message)
        val response = """{"text":"<${username}> ${escapeJson(message)}","color":"white"}"""
        broadcastChat(response)
    }

    private fun handleChatCommand(ctx: ChannelHandlerContext, data: Buffer) {
        val command = data.readMcString()
        val username = session?.username ?: "?"
        logger.info("[{}] /{}", username, command)
        val response = """{"text":"<${username}> /${escapeJson(command)}","color":"gray"}"""
        broadcastChat(response)
    }

    private fun broadcastChat(json: String) {
        registry.all().forEach { player ->
            if (player is JavaPlayerSession && player != session) {
                player.sendRaw(json)
            } else {
                player.sendMessage(json)
            }
        }
    }

    private fun handleClientInformation(data: Buffer) {
        data.readMcString()
        data.readUnsignedByte()
        data.readUnsignedVarInt()
        data.readBoolean()
        data.readUnsignedByte()
        data.readUnsignedVarInt()
        data.readBoolean()
        data.readBoolean()
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // -- Packet helpers --

    private fun writePacket(ctx: ChannelHandlerContext, packetId: Int, body: (Buffer) -> Unit) {
        val alloc = ctx.bufferAllocator()
        val bodyBuf = alloc.allocate(64)
        try {
            bodyBuf.writeUnsignedVarInt(packetId)
            body(bodyBuf)

            val out = alloc.allocate(bodyBuf.readableBytes() + 5)
            out.writeUnsignedVarInt(bodyBuf.readableBytes())
            out.writeBytes(bodyBuf)
            ctx.writeAndFlush(out)
        } finally {
            bodyBuf.close()
        }
    }

    fun writePacketTo(player: JavaPlayerSession, packetId: Int, body: (Buffer) -> Unit) {
        writePacket(player.ctx, packetId, body)
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
