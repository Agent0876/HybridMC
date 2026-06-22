package io.hybridmc.core.buffer

import java.nio.ByteOrder
import java.util.UUID

/**
 * Edition-agnostic byte I/O abstraction for Minecraft protocol, NBT, and chunk serialization.
 *
 * Multi-byte primitives respect the [byteOrder] configured at creation time.
 * Variable-length encodings ([readVarInt], [readVarLong]) are byte-order independent.
 */
public interface PacketBuffer {
    /** The byte order used for multi-byte primitive operations. */
    public val byteOrder: ByteOrder

    /** Current read position (0-based byte offset). */
    public val readerIndex: Int

    /** Current write position (0-based byte offset). */
    public val writerIndex: Int

    /** Number of readable bytes between [readerIndex] and [writerIndex]. */
    public val readableBytes: Int

    /** `true` when [readableBytes] > 0. */
    public val isReadable: Boolean

    // ── Primitive I/O ───────────────────────────────────────────

    /** Reads a single byte and returns `true` if it is non-zero. */
    public fun readBoolean(): Boolean

    /** Writes `0x01` for `true`, `0x00` for `false`. */
    public fun writeBoolean(value: Boolean): PacketBuffer

    /** Reads a signed 8-bit integer. */
    public fun readByte(): Byte

    /** Writes a signed 8-bit integer. */
    public fun writeByte(value: Byte): PacketBuffer

    /** Reads a 16-bit integer in the buffer's [byteOrder]. */
    public fun readShort(): Short

    /** Writes a 16-bit integer in the buffer's [byteOrder]. */
    public fun writeShort(value: Short): PacketBuffer

    /** Reads a 32-bit integer in the buffer's [byteOrder]. */
    public fun readInt(): Int

    /** Writes a 32-bit integer in the buffer's [byteOrder]. */
    public fun writeInt(value: Int): PacketBuffer

    /** Reads a 64-bit integer in the buffer's [byteOrder]. */
    public fun readLong(): Long

    /** Writes a 64-bit integer in the buffer's [byteOrder]. */
    public fun writeLong(value: Long): PacketBuffer

    /** Reads a 32-bit IEEE 754 float in the buffer's [byteOrder]. */
    public fun readFloat(): Float

    /** Writes a 32-bit IEEE 754 float in the buffer's [byteOrder]. */
    public fun writeFloat(value: Float): PacketBuffer

    /** Reads a 64-bit IEEE 754 double in the buffer's [byteOrder]. */
    public fun readDouble(): Double

    /** Writes a 64-bit IEEE 754 double in the buffer's [byteOrder]. */
    public fun writeDouble(value: Double): PacketBuffer

    // ── Variable-length integers ────────────────────────────────

    /**
     * Reads a VarInt (7-bit groups, continuation bit, up to 5 bytes).
     *
     * @throws MalformedVarIntException if the encoding exceeds 5 bytes.
     */
    public fun readVarInt(): Int

    /** Writes [value] as a VarInt. */
    public fun writeVarInt(value: Int): PacketBuffer

    /**
     * Reads a VarLong (7-bit groups, continuation bit, up to 10 bytes).
     *
     * @throws MalformedVarIntException if the encoding exceeds 10 bytes.
     */
    public fun readVarLong(): Long

    /** Writes [value] as a VarLong. */
    public fun writeVarLong(value: Long): PacketBuffer

    // ── Compound types ──────────────────────────────────────────

    /**
     * Reads a VarInt-prefixed UTF-8 string.
     *
     * @param maxBytes upper bound on the UTF-8 byte count.
     * @throws IllegalArgumentException if the byte count exceeds [maxBytes].
     */
    public fun readString(maxBytes: Int): String

    /** Writes [value] as a VarInt-prefixed UTF-8 string. */
    public fun writeString(value: String): PacketBuffer

    /** Reads a UUID as two ordered longs (most-significant first). */
    public fun readUuid(): UUID

    /** Writes a UUID as two ordered longs (most-significant first). */
    public fun writeUuid(value: UUID): PacketBuffer

    /**
     * Reads a VarInt-prefixed byte array.
     *
     * @param maxLength upper bound on the array length.
     * @throws IllegalArgumentException if the length exceeds [maxLength].
     */
    public fun readByteArray(maxLength: Int): ByteArray

    /** Writes [value] as a VarInt-prefixed byte array. */
    public fun writeByteArray(value: ByteArray): PacketBuffer

    /** Reads exactly [length] raw bytes (no length prefix). */
    public fun readBytes(length: Int): ByteArray

    /** Writes raw bytes (no length prefix). */
    public fun writeBytes(value: ByteArray): PacketBuffer

    // ── Block position (packed long) ────────────────────────────

    /**
     * Reads a block position encoded as a single 64-bit integer.
     *
     * Bit packing/unpacking into x/y/z is handled at a higher layer (e.g. `BlockPos`).
     */
    public fun readPosition(): Long

    /** Writes a packed block position as a single 64-bit integer. */
    public fun writePosition(value: Long): PacketBuffer

    // ── Index / utility ─────────────────────────────────────────

    /** Sets the reader index to [index]. */
    public fun setReaderIndex(index: Int): PacketBuffer

    /** Sets the writer index to [index]. */
    public fun setWriterIndex(index: Int): PacketBuffer

    /**
     * Returns a copy of the bytes between [readerIndex] (inclusive)
     * and [writerIndex] (exclusive).
     */
    public fun toByteArray(): ByteArray
}
