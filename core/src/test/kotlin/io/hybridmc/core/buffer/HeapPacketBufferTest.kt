package io.hybridmc.core.buffer

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteOrder
import java.util.UUID

class HeapPacketBufferTest {
    // ══════════════════════════════════════════════════════════════
    //  VarInt
    // ══════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "VarInt round-trip: {0}")
    @MethodSource("varIntValues")
    fun `VarInt round-trip preserves value`(value: Int) {
        val buf = HeapPacketBuffer()
        buf.writeVarInt(value)
        assertEquals(value, buf.readVarInt())
    }

    @ParameterizedTest(name = "VarInt {0} → expected byte encoding")
    @MethodSource("varIntEncodings")
    fun `VarInt produces expected byte encoding`(
        value: Int,
        expected: ByteArray,
    ) {
        val buf = HeapPacketBuffer()
        buf.writeVarInt(value)
        assertArrayEquals(expected, buf.toByteArray())
    }

    @Test
    fun `malformed VarInt exceeding 5 bytes throws MalformedVarIntException`() {
        // 5 bytes with continuation bit + 1 terminator = reader sees 6th byte attempt
        val data =
            byteArrayOf(
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x00,
            )
        val buf = HeapPacketBuffer(data)
        assertThrows<MalformedVarIntException> { buf.readVarInt() }
    }

    // ══════════════════════════════════════════════════════════════
    //  VarLong
    // ══════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "VarLong round-trip: {0}")
    @MethodSource("varLongValues")
    fun `VarLong round-trip preserves value`(value: Long) {
        val buf = HeapPacketBuffer()
        buf.writeVarLong(value)
        assertEquals(value, buf.readVarLong())
    }

    @Test
    fun `malformed VarLong exceeding 10 bytes throws MalformedVarIntException`() {
        // 10 continuation bytes + 1 terminator
        val data = ByteArray(11) { 0x80.toByte() }
        data[10] = 0x00
        val buf = HeapPacketBuffer(data)
        assertThrows<MalformedVarIntException> { buf.readVarLong() }
    }

    // ══════════════════════════════════════════════════════════════
    //  Boolean
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `boolean round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeBoolean(true)
        buf.writeBoolean(false)
        assertTrue(buf.readBoolean())
        assertFalse(buf.readBoolean())
    }

    // ══════════════════════════════════════════════════════════════
    //  Byte
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `byte round-trip for boundary values`() {
        val buf = HeapPacketBuffer()
        val values = listOf(0.toByte(), Byte.MIN_VALUE, Byte.MAX_VALUE)
        values.forEach { buf.writeByte(it) }
        values.forEach { assertEquals(it, buf.readByte()) }
    }

    // ══════════════════════════════════════════════════════════════
    //  Short — byte order verification
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `short round-trip big-endian with byte verification`() {
        val buf = HeapPacketBuffer(byteOrder = ByteOrder.BIG_ENDIAN)
        buf.writeShort(0x0102.toShort())
        assertArrayEquals(byteArrayOf(0x01, 0x02), buf.toByteArray())
        assertEquals(0x0102.toShort(), buf.readShort())
    }

    @Test
    fun `short round-trip little-endian with byte verification`() {
        val buf = HeapPacketBuffer(byteOrder = ByteOrder.LITTLE_ENDIAN)
        buf.writeShort(0x0102.toShort())
        assertArrayEquals(byteArrayOf(0x02, 0x01), buf.toByteArray())
        assertEquals(0x0102.toShort(), buf.readShort())
    }

    @Test
    fun `short boundary values round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeShort(Short.MIN_VALUE)
        buf.writeShort(Short.MAX_VALUE)
        buf.writeShort(0)
        assertEquals(Short.MIN_VALUE, buf.readShort())
        assertEquals(Short.MAX_VALUE, buf.readShort())
        assertEquals(0.toShort(), buf.readShort())
    }

    // ══════════════════════════════════════════════════════════════
    //  Int — byte order verification
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `int round-trip big-endian with byte verification`() {
        val buf = HeapPacketBuffer(byteOrder = ByteOrder.BIG_ENDIAN)
        buf.writeInt(0x01020304)
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), buf.toByteArray())
        assertEquals(0x01020304, buf.readInt())
    }

    @Test
    fun `int round-trip little-endian with byte verification`() {
        val buf = HeapPacketBuffer(byteOrder = ByteOrder.LITTLE_ENDIAN)
        buf.writeInt(0x01020304)
        assertArrayEquals(byteArrayOf(0x04, 0x03, 0x02, 0x01), buf.toByteArray())
        assertEquals(0x01020304, buf.readInt())
    }

    @Test
    fun `int boundary values round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeInt(Int.MIN_VALUE)
        buf.writeInt(Int.MAX_VALUE)
        buf.writeInt(0)
        buf.writeInt(-1)
        assertEquals(Int.MIN_VALUE, buf.readInt())
        assertEquals(Int.MAX_VALUE, buf.readInt())
        assertEquals(0, buf.readInt())
        assertEquals(-1, buf.readInt())
    }

    // ══════════════════════════════════════════════════════════════
    //  Long — byte order verification
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `long round-trip big-endian with byte verification`() {
        val buf = HeapPacketBuffer(byteOrder = ByteOrder.BIG_ENDIAN)
        buf.writeLong(0x0102030405060708L)
        assertArrayEquals(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),
            buf.toByteArray(),
        )
        assertEquals(0x0102030405060708L, buf.readLong())
    }

    @Test
    fun `long round-trip little-endian with byte verification`() {
        val buf = HeapPacketBuffer(byteOrder = ByteOrder.LITTLE_ENDIAN)
        buf.writeLong(0x0102030405060708L)
        assertArrayEquals(
            byteArrayOf(0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01),
            buf.toByteArray(),
        )
        assertEquals(0x0102030405060708L, buf.readLong())
    }

    @Test
    fun `long boundary values round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeLong(Long.MIN_VALUE)
        buf.writeLong(Long.MAX_VALUE)
        buf.writeLong(0L)
        buf.writeLong(-1L)
        assertEquals(Long.MIN_VALUE, buf.readLong())
        assertEquals(Long.MAX_VALUE, buf.readLong())
        assertEquals(0L, buf.readLong())
        assertEquals(-1L, buf.readLong())
    }

    // ══════════════════════════════════════════════════════════════
    //  Float / Double
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `float round-trip including special values`() {
        val buf = HeapPacketBuffer()
        val values = listOf(0.0f, -1.0f, 3.14f, Float.MAX_VALUE, Float.MIN_VALUE)
        values.forEach { buf.writeFloat(it) }
        values.forEach { assertEquals(it, buf.readFloat()) }
    }

    @Test
    fun `float NaN round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeFloat(Float.NaN)
        assertTrue(buf.readFloat().isNaN())
    }

    @Test
    fun `double round-trip including special values`() {
        val buf = HeapPacketBuffer()
        val values = listOf(0.0, -1.0, 3.14159265358979, Double.MAX_VALUE, Double.MIN_VALUE)
        values.forEach { buf.writeDouble(it) }
        values.forEach { assertEquals(it, buf.readDouble()) }
    }

    @Test
    fun `double NaN round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeDouble(Double.NaN)
        assertTrue(buf.readDouble().isNaN())
    }

    // ══════════════════════════════════════════════════════════════
    //  String
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `string round-trip with ASCII`() {
        val buf = HeapPacketBuffer()
        buf.writeString("Hello, world!")
        assertEquals("Hello, world!", buf.readString(256))
    }

    @Test
    fun `string round-trip with Unicode`() {
        val buf = HeapPacketBuffer()
        buf.writeString("Hello, 世界! 🎮")
        assertEquals("Hello, 世界! 🎮", buf.readString(256))
    }

    @Test
    fun `empty string round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeString("")
        assertEquals("", buf.readString(256))
    }

    @Test
    fun `readString throws when byte count exceeds maxBytes`() {
        val buf = HeapPacketBuffer()
        buf.writeString("Hello") // 5 UTF-8 bytes
        assertThrows<IllegalArgumentException> { buf.readString(3) }
    }

    // ══════════════════════════════════════════════════════════════
    //  UUID
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `UUID round-trip`() {
        val buf = HeapPacketBuffer()
        val uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        buf.writeUuid(uuid)
        assertEquals(uuid, buf.readUuid())
    }

    @Test
    fun `UUID round-trip with random value`() {
        val buf = HeapPacketBuffer()
        val uuid = UUID.randomUUID()
        buf.writeUuid(uuid)
        assertEquals(uuid, buf.readUuid())
    }

    // ══════════════════════════════════════════════════════════════
    //  ByteArray (VarInt-prefixed)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `byteArray round-trip`() {
        val buf = HeapPacketBuffer()
        val original = byteArrayOf(1, 2, 3, 4, 5)
        buf.writeByteArray(original)
        assertArrayEquals(original, buf.readByteArray(10))
    }

    @Test
    fun `empty byteArray round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeByteArray(byteArrayOf())
        assertArrayEquals(byteArrayOf(), buf.readByteArray(10))
    }

    @Test
    fun `readByteArray throws when length exceeds maxLength`() {
        val buf = HeapPacketBuffer()
        buf.writeByteArray(byteArrayOf(1, 2, 3, 4, 5))
        assertThrows<IllegalArgumentException> { buf.readByteArray(3) }
    }

    // ══════════════════════════════════════════════════════════════
    //  Raw bytes (no prefix)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `raw bytes round-trip`() {
        val buf = HeapPacketBuffer()
        val original = byteArrayOf(10, 20, 30)
        buf.writeBytes(original)
        assertArrayEquals(original, buf.readBytes(3))
    }

    @Test
    fun `empty raw bytes round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeBytes(byteArrayOf())
        assertArrayEquals(byteArrayOf(), buf.readBytes(0))
    }

    // ══════════════════════════════════════════════════════════════
    //  Position (packed long)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `position round-trip`() {
        val buf = HeapPacketBuffer()
        val packed = 0x7FFF_FFFF_FFFF_FFFFL
        buf.writePosition(packed)
        assertEquals(packed, buf.readPosition())
    }

    // ══════════════════════════════════════════════════════════════
    //  Buffer state & edge cases
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `readableBytes tracks writes and reads`() {
        val buf = HeapPacketBuffer()
        assertEquals(0, buf.readableBytes)
        assertFalse(buf.isReadable)

        buf.writeInt(42)
        assertEquals(4, buf.readableBytes)
        assertTrue(buf.isReadable)

        buf.readByte()
        assertEquals(3, buf.readableBytes)
    }

    @Test
    fun `buffer auto-grows when capacity exceeded`() {
        val buf = HeapPacketBuffer(initialCapacity = 4)
        buf.writeInt(1)
        buf.writeInt(2) // triggers growth
        assertEquals(8, buf.readableBytes)
        assertEquals(1, buf.readInt())
        assertEquals(2, buf.readInt())
    }

    @Test
    fun `read past writerIndex throws IndexOutOfBoundsException`() {
        val buf = HeapPacketBuffer()
        buf.writeByte(1.toByte())
        buf.readByte()
        assertThrows<IndexOutOfBoundsException> { buf.readByte() }
    }

    @Test
    fun `toByteArray returns only readable bytes`() {
        val buf = HeapPacketBuffer()
        buf.writeByte(1.toByte())
        buf.writeByte(2.toByte())
        buf.writeByte(3.toByte())
        buf.readByte() // advance reader past first byte
        assertArrayEquals(byteArrayOf(2, 3), buf.toByteArray())
    }

    @Test
    fun `setReaderIndex repositions reader`() {
        val buf = HeapPacketBuffer()
        buf.writeShort(0x1234.toShort())
        buf.readByte()
        assertEquals(1, buf.readerIndex)
        buf.setReaderIndex(0)
        assertEquals(0, buf.readerIndex)
        assertEquals(0x1234.toShort(), buf.readShort())
    }

    @Test
    fun `constructor with source array allows immediate reading`() {
        val source = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val buf = HeapPacketBuffer(source, ByteOrder.BIG_ENDIAN)
        assertEquals(0x01020304, buf.readInt())
    }

    @Test
    fun `fluent API chains write calls`() {
        val buf = HeapPacketBuffer()
        buf
            .writeBoolean(true)
            .writeVarInt(42)
            .writeString("test")
        assertTrue(buf.readBoolean())
        assertEquals(42, buf.readVarInt())
        assertEquals("test", buf.readString(256))
    }

    // ══════════════════════════════════════════════════════════════
    //  Test data providers
    // ══════════════════════════════════════════════════════════════

    companion object {
        @JvmStatic
        fun varIntValues(): List<Int> =
            listOf(
                0,
                1,
                2,
                127,
                128,
                255,
                25565,
                32767,
                2097151,
                -1,
                Int.MIN_VALUE,
                Int.MAX_VALUE,
            )

        @JvmStatic
        fun varLongValues(): List<Long> =
            listOf(
                0L,
                1L,
                127L,
                128L,
                255L,
                2147483647L,
                -1L,
                Long.MAX_VALUE,
                Long.MIN_VALUE,
            )

        @JvmStatic
        fun varIntEncodings(): List<Arguments> =
            listOf(
                Arguments.of(0, byteArrayOf(0x00)),
                Arguments.of(1, byteArrayOf(0x01)),
                Arguments.of(127, byteArrayOf(0x7F)),
                Arguments.of(128, byteArrayOf(0x80.toByte(), 0x01)),
                Arguments.of(255, byteArrayOf(0xFF.toByte(), 0x01)),
                Arguments.of(
                    -1,
                    byteArrayOf(
                        0xFF.toByte(),
                        0xFF.toByte(),
                        0xFF.toByte(),
                        0xFF.toByte(),
                        0x0F,
                    ),
                ),
                Arguments.of(
                    Int.MAX_VALUE,
                    byteArrayOf(
                        0xFF.toByte(),
                        0xFF.toByte(),
                        0xFF.toByte(),
                        0xFF.toByte(),
                        0x07,
                    ),
                ),
                Arguments.of(
                    Int.MIN_VALUE,
                    byteArrayOf(
                        0x80.toByte(),
                        0x80.toByte(),
                        0x80.toByte(),
                        0x80.toByte(),
                        0x08,
                    ),
                ),
            )
    }
}
