package io.hybridmc.protocol.java

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.CorruptedFrameException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JavaServerTest {
    @Test
    fun `test decoder parses valid frames`() {
        val channel = EmbeddedChannel(VarIntFrameDecoder())
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)

        val buf = Unpooled.buffer()
        // Write packet: length 5 (VarInt) + 5 bytes body
        buf.writeByte(5)
        buf.writeBytes(data)

        assertTrue(channel.writeInbound(buf))

        val decoded = channel.readInbound<ByteBuf>()
        assertNotNull(decoded)
        assertEquals(5, decoded.readableBytes())

        val resultBytes = ByteArray(5)
        decoded.readBytes(resultBytes)
        decoded.release()

        assertTrue(data.contentEquals(resultBytes))
    }

    @Test
    fun `test decoder handles fragmented packets`() {
        val channel = EmbeddedChannel(VarIntFrameDecoder())
        val data = byteArrayOf(0x0A, 0x0B, 0x0C)

        val firstHalf = Unpooled.buffer()
        firstHalf.writeByte(3) // length
        firstHalf.writeByte(0x0A) // first body byte

        // Write first half of packet
        assertFalse(channel.writeInbound(firstHalf)) // should return false as no complete message is decoded

        val secondHalf = Unpooled.buffer()
        secondHalf.writeByte(0x0B)
        secondHalf.writeByte(0x0C)

        // Write remaining body
        assertTrue(channel.writeInbound(secondHalf))

        val decoded = channel.readInbound<ByteBuf>()
        assertNotNull(decoded)
        assertEquals(3, decoded.readableBytes())

        val resultBytes = ByteArray(3)
        decoded.readBytes(resultBytes)
        decoded.release()

        assertTrue(data.contentEquals(resultBytes))
    }

    @Test
    fun `test decoder throws on invalid length`() {
        val channel = EmbeddedChannel(VarIntFrameDecoder())

        val invalidBuf = Unpooled.buffer()
        // Write malformed VarInt length (e.g. invalid sequence that exceeds 5 bytes)
        for (i in 0..5) {
            invalidBuf.writeByte(0x80 or 1)
        }

        assertThrows(Exception::class.java) {
            channel.writeInbound(invalidBuf)
        }
    }

    @Test
    fun `test encoder prepends correct length`() {
        val channel = EmbeddedChannel(VarIntFrameEncoder())
        val data = byteArrayOf(0x05, 0x06, 0x07)

        val msg = Unpooled.wrappedBuffer(data)
        assertTrue(channel.writeOutbound(msg))

        val encoded = channel.readOutbound<ByteBuf>()
        assertNotNull(encoded)

        // VarInt for length 3 is 1 byte
        assertEquals(4, encoded.readableBytes())
        assertEquals(3, encoded.readByte().toInt()) // length prefix

        val resultBytes = ByteArray(3)
        encoded.readBytes(resultBytes)
        encoded.release()

        assertTrue(data.contentEquals(resultBytes))
    }
}
