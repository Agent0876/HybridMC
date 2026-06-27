package io.github.agent0876.hybridmc.java

import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.HybridPlayer
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.netty5.buffer.Buffer
import io.netty5.channel.ChannelHandlerContext
import java.net.InetSocketAddress
import java.util.UUID

class JavaPlayerSession(
    val ctx: ChannelHandlerContext,
    override val uuid: UUID,
    override val username: String,
    override val entityId: Int,
    private val registry: PlayerRegistry,
) : HybridPlayer {

    override val edition: Edition = Edition.JAVA

    override var x: Double = 0.0
    override var y: Double = 100.0
    override var z: Double = 0.0
    override var yaw: Float = 0.0f
    override var pitch: Float = 0.0f

    override var ping: Int = 0
        internal set

    val remoteAddress: InetSocketAddress
        get() = ctx.channel().remoteAddress() as InetSocketAddress

    override fun sendMessage(message: String) {
        val json = """{"text":"${escapeJson(message)}"}"""
        sendRaw(json)
    }

    fun sendRaw(json: String) {
        writePacket(0x6C) { buf ->
            buf.writeMcString(json)
            buf.writeBoolean(false)
        }
    }

    override fun disconnect(reason: String) {
        val json = """{"text":"${escapeJson(reason)}","color":"red"}"""
        writePacket(0x1D) { buf -> buf.writeMcString(json) }
        ctx.channel().close().addListener {
            registry.leave(this@JavaPlayerSession)
        }
    }

    fun writeAndFlush(buf: Buffer) {
        ctx.writeAndFlush(buf)
    }

    private var keepAliveId = 0L
    private var pendingKeepAliveTime = 0L

    fun sendKeepAlive() {
        val id = ++keepAliveId
        pendingKeepAliveTime = System.currentTimeMillis()
        writePacket(0x26) { buf -> buf.writeLong(id) }
    }

    fun handleKeepAliveResponse(id: Long) {
        if (id == keepAliveId) {
            ping = (System.currentTimeMillis() - pendingKeepAliveTime).toInt().coerceAtLeast(0)
        }
    }

    private fun writePacket(packetId: Int, body: (Buffer) -> Unit) {
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

    fun sendPositionAndLook() {
        writePacket(0x40) { buf ->
            buf.writeDouble(x)
            buf.writeDouble(y)
            buf.writeDouble(z)
            buf.writeFloat(yaw)
            buf.writeFloat(pitch)
            buf.writeByte(0)
            buf.writeUnsignedVarInt(1)
        }
    }

    override fun spawnPlayer(target: HybridPlayer) {
        writePacket(0x3C) { buf ->
            buf.writeByte(0x09)
            buf.writeUnsignedVarInt(1)
            buf.writeLong(target.uuid.mostSignificantBits)
            buf.writeLong(target.uuid.leastSignificantBits)
            buf.writeMcString(target.username)
            buf.writeUnsignedVarInt(0)
            buf.writeBoolean(true)
        }
        writePacket(0x01) { buf ->
            buf.writeUnsignedVarInt(target.entityId)
            buf.writeLong(target.uuid.mostSignificantBits)
            buf.writeLong(target.uuid.leastSignificantBits)
            buf.writeUnsignedVarInt(124)
            buf.writeDouble(target.x)
            buf.writeDouble(target.y)
            buf.writeDouble(target.z)
            buf.writeByte((target.pitch * 256f / 360f).toInt().toByte())
            buf.writeByte((target.yaw * 256f / 360f).toInt().toByte())
            buf.writeByte((target.yaw * 256f / 360f).toInt().toByte())
            buf.writeUnsignedVarInt(0)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeShort(0)
        }
    }

    override fun removePlayer(target: HybridPlayer) {
        writePacket(0x42) { buf ->
            buf.writeUnsignedVarInt(1)
            buf.writeUnsignedVarInt(target.entityId)
        }
        writePacket(0x3C) { buf ->
            buf.writeByte(0x08)
            buf.writeUnsignedVarInt(1)
            buf.writeLong(target.uuid.mostSignificantBits)
            buf.writeLong(target.uuid.leastSignificantBits)
            buf.writeBoolean(false)
        }
    }

    override fun movePlayer(target: HybridPlayer) {
        writePacket(0x71) { buf ->
            buf.writeUnsignedVarInt(target.entityId)
            buf.writeDouble(target.x)
            buf.writeDouble(target.y)
            buf.writeDouble(target.z)
            buf.writeByte((target.yaw * 256f / 360f).toInt().toByte())
            buf.writeByte((target.pitch * 256f / 360f).toInt().toByte())
            buf.writeBoolean(true)
        }
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
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

private fun Buffer.writeMcString(value: String) {
    val bytes = value.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
    writeUnsignedVarInt(bytes.size)
    writeBytes(bytes)
}
