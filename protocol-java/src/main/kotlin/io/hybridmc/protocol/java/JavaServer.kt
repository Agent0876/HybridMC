package io.hybridmc.protocol.java

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.CorruptedFrameException
import io.netty.handler.codec.MessageToByteEncoder
import java.net.InetSocketAddress

private val logger = KotlinLogging.logger {}

/**
 * Netty-based Minecraft Java TCP server. Sets up the port listener and
 * manages framing encoders and decoders.
 */
public class JavaServer(
    private val host: String = "0.0.0.0",
    private val port: Int = 25565,
) {
    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var channelFuture: ChannelFuture? = null

    /** Starts the server listening on the configured host and port. */
    @Suppress("DEPRECATION")
    public fun start() {
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()

        val bootstrap =
            ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(
                    object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            ch
                                .pipeline()
                                .addLast("splitter", VarIntFrameDecoder())
                                .addLast("prepender", VarIntFrameEncoder())
                        }
                    },
                ).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

        channelFuture = bootstrap.bind(InetSocketAddress(host, port)).sync()
        logger.info { "Java Server listening on $host:$port" }
    }

    /** Stops the server, releasing all bound resources. */
    public fun stop() {
        try {
            channelFuture?.channel()?.close()?.sync()
        } finally {
            bossGroup?.shutdownGracefully()
            workerGroup?.shutdownGracefully()
            logger.info { "Java Server stopped" }
        }
    }
}

/**
 * Decodes length-prefixed VarInt packets from Minecraft client connections.
 */
public class VarIntFrameDecoder : ByteToMessageDecoder() {
    override fun decode(
        ctx: ChannelHandlerContext,
        `in`: ByteBuf,
        out: MutableList<Any>,
    ) {
        `in`.markReaderIndex()
        val length = readVarInt(`in`)
        if (length == -1) {
            return
        }
        if (length < 0) {
            throw CorruptedFrameException("Negative length: $length")
        }
        if (`in`.readableBytes() < length) {
            `in`.resetReaderIndex()
            return
        }
        val frame = `in`.readRetainedSlice(length)
        out.add(frame)
    }

    private fun readVarInt(buf: ByteBuf): Int {
        buf.markReaderIndex()
        var result = 0
        var numRead = 0
        var read: Byte
        do {
            if (!buf.isReadable) {
                buf.resetReaderIndex()
                return -1
            }
            read = buf.readByte()
            val value = (read.toInt() and 0x7F)
            result = result or (value shl (7 * numRead))
            numRead++
            if (numRead > 5) {
                throw CorruptedFrameException("VarInt too big")
            }
        } while ((read.toInt() and 0x80) != 0)
        return result
    }
}

/**
 * Prepends a length-prefixed VarInt to outbound packets.
 */
public class VarIntFrameEncoder : MessageToByteEncoder<ByteBuf>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: ByteBuf,
        out: ByteBuf,
    ) {
        val bodyLen = msg.readableBytes()
        writeVarInt(out, bodyLen)
        out.writeBytes(msg)
    }

    private fun writeVarInt(
        buf: ByteBuf,
        value: Int,
    ) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                buf.writeByte(v)
                return
            }
            buf.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }
}
