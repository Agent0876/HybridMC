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
    val entityId: Int,
    private val registry: PlayerRegistry,
) : HybridPlayer {

    override val edition: Edition = Edition.JAVA

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
