package io.hybridmc.core.buffer

import java.nio.ByteOrder
import java.util.UUID

/**
 * Heap-backed [PacketBuffer] implementation over a growable [ByteArray].
 *
 * **Not thread-safe** — intended for single-threaded protocol I/O.
 */
public class HeapPacketBuffer : PacketBuffer {
    private var data: ByteArray
    private var _readerIndex: Int = 0
    private var _writerIndex: Int = 0

    override val byteOrder: ByteOrder

    /**
     * Creates an empty, writable buffer.
     *
     * @param initialCapacity initial byte array size (default 256).
     * @param byteOrder byte order for multi-byte primitives (default [ByteOrder.BIG_ENDIAN]).
     */
    public constructor(
        initialCapacity: Int = DEFAULT_CAPACITY,
        byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
    ) {
        this.data = ByteArray(initialCapacity)
        this.byteOrder = byteOrder
    }

    /**
     * Creates a buffer pre-filled with [source] for reading.
     *
     * The reader index starts at 0 and the writer index equals `source.size`.
     */
    public constructor(
        source: ByteArray,
        byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
    ) {
        this.data = source.copyOf()
        this._writerIndex = source.size
        this.byteOrder = byteOrder
    }

    // ── Properties ──────────────────────────────────────────────

    override val readerIndex: Int get() = _readerIndex
    override val writerIndex: Int get() = _writerIndex
    override val readableBytes: Int get() = _writerIndex - _readerIndex
    override val isReadable: Boolean get() = _readerIndex < _writerIndex

    // ── Internal helpers ────────────────────────────────────────

    private fun ensureWritable(bytes: Int) {
        val required = _writerIndex + bytes
        if (required > data.size) {
            data = data.copyOf(maxOf(data.size * 2, required))
        }
    }

    private fun checkReadable(bytes: Int) {
        if (_readerIndex + bytes > _writerIndex) {
            throw IndexOutOfBoundsException(
                "Not enough readable bytes: need $bytes, have $readableBytes " +
                    "(readerIndex=$_readerIndex, writerIndex=$_writerIndex)",
            )
        }
    }

    // ── Primitive I/O ───────────────────────────────────────────

    override fun readBoolean(): Boolean = readByte() != 0.toByte()

    override fun writeBoolean(value: Boolean): PacketBuffer = writeByte(if (value) 1.toByte() else 0.toByte())

    override fun readByte(): Byte {
        checkReadable(1)
        return data[_readerIndex++]
    }

    override fun writeByte(value: Byte): PacketBuffer {
        ensureWritable(1)
        data[_writerIndex++] = value
        return this
    }

    override fun readShort(): Short {
        checkReadable(SHORT_BYTES)
        val b0 = data[_readerIndex++].toInt() and BYTE_MASK
        val b1 = data[_readerIndex++].toInt() and BYTE_MASK
        return if (byteOrder == ByteOrder.BIG_ENDIAN) {
            ((b0 shl 8) or b1).toShort()
        } else {
            (b0 or (b1 shl 8)).toShort()
        }
    }

    override fun writeShort(value: Short): PacketBuffer {
        ensureWritable(SHORT_BYTES)
        val v = value.toInt()
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            data[_writerIndex++] = (v shr 8).toByte()
            data[_writerIndex++] = v.toByte()
        } else {
            data[_writerIndex++] = v.toByte()
            data[_writerIndex++] = (v shr 8).toByte()
        }
        return this
    }

    override fun readInt(): Int {
        checkReadable(INT_BYTES)
        val b0 = data[_readerIndex++].toInt() and BYTE_MASK
        val b1 = data[_readerIndex++].toInt() and BYTE_MASK
        val b2 = data[_readerIndex++].toInt() and BYTE_MASK
        val b3 = data[_readerIndex++].toInt() and BYTE_MASK
        return if (byteOrder == ByteOrder.BIG_ENDIAN) {
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        } else {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }
    }

    override fun writeInt(value: Int): PacketBuffer {
        ensureWritable(INT_BYTES)
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            data[_writerIndex++] = (value shr 24).toByte()
            data[_writerIndex++] = (value shr 16).toByte()
            data[_writerIndex++] = (value shr 8).toByte()
            data[_writerIndex++] = value.toByte()
        } else {
            data[_writerIndex++] = value.toByte()
            data[_writerIndex++] = (value shr 8).toByte()
            data[_writerIndex++] = (value shr 16).toByte()
            data[_writerIndex++] = (value shr 24).toByte()
        }
        return this
    }

    @Suppress("LongMethod")
    override fun readLong(): Long {
        checkReadable(LONG_BYTES)
        val b0 = data[_readerIndex++].toLong() and BYTE_MASK_LONG
        val b1 = data[_readerIndex++].toLong() and BYTE_MASK_LONG
        val b2 = data[_readerIndex++].toLong() and BYTE_MASK_LONG
        val b3 = data[_readerIndex++].toLong() and BYTE_MASK_LONG
        val b4 = data[_readerIndex++].toLong() and BYTE_MASK_LONG
        val b5 = data[_readerIndex++].toLong() and BYTE_MASK_LONG
        val b6 = data[_readerIndex++].toLong() and BYTE_MASK_LONG
        val b7 = data[_readerIndex++].toLong() and BYTE_MASK_LONG
        return if (byteOrder == ByteOrder.BIG_ENDIAN) {
            (b0 shl 56) or (b1 shl 48) or (b2 shl 40) or (b3 shl 32) or
                (b4 shl 24) or (b5 shl 16) or (b6 shl 8) or b7
        } else {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or
                (b4 shl 32) or (b5 shl 40) or (b6 shl 48) or (b7 shl 56)
        }
    }

    @Suppress("LongMethod")
    override fun writeLong(value: Long): PacketBuffer {
        ensureWritable(LONG_BYTES)
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            data[_writerIndex++] = (value shr 56).toByte()
            data[_writerIndex++] = (value shr 48).toByte()
            data[_writerIndex++] = (value shr 40).toByte()
            data[_writerIndex++] = (value shr 32).toByte()
            data[_writerIndex++] = (value shr 24).toByte()
            data[_writerIndex++] = (value shr 16).toByte()
            data[_writerIndex++] = (value shr 8).toByte()
            data[_writerIndex++] = value.toByte()
        } else {
            data[_writerIndex++] = value.toByte()
            data[_writerIndex++] = (value shr 8).toByte()
            data[_writerIndex++] = (value shr 16).toByte()
            data[_writerIndex++] = (value shr 24).toByte()
            data[_writerIndex++] = (value shr 32).toByte()
            data[_writerIndex++] = (value shr 40).toByte()
            data[_writerIndex++] = (value shr 48).toByte()
            data[_writerIndex++] = (value shr 56).toByte()
        }
        return this
    }

    override fun readFloat(): Float = Float.fromBits(readInt())

    override fun writeFloat(value: Float): PacketBuffer = writeInt(value.toRawBits())

    override fun readDouble(): Double = Double.fromBits(readLong())

    override fun writeDouble(value: Double): PacketBuffer = writeLong(value.toRawBits())

    // ── Variable-length integers ────────────────────────────────

    override fun readVarInt(): Int {
        var result = 0
        for (i in 0 until MAX_VARINT_BYTES) {
            val b = readByte().toInt() and BYTE_MASK
            result = result or ((b and SEGMENT_MASK) shl (i * SEGMENT_BITS))
            if (b and CONTINUE_MASK == 0) {
                return result
            }
        }
        throw MalformedVarIntException(
            "VarInt exceeds maximum length of $MAX_VARINT_BYTES bytes",
        )
    }

    override fun writeVarInt(value: Int): PacketBuffer {
        var v = value
        while (true) {
            if (v and SEGMENT_MASK.inv() == 0) {
                writeByte(v.toByte())
                return this
            }
            writeByte(((v and SEGMENT_MASK) or CONTINUE_MASK).toByte())
            v = v ushr SEGMENT_BITS
        }
    }

    override fun readVarLong(): Long {
        var result = 0L
        for (i in 0 until MAX_VARLONG_BYTES) {
            val b = readByte().toLong() and BYTE_MASK_LONG
            result = result or ((b and SEGMENT_MASK_LONG) shl (i * SEGMENT_BITS))
            if (b and CONTINUE_MASK_LONG == 0L) {
                return result
            }
        }
        throw MalformedVarIntException(
            "VarLong exceeds maximum length of $MAX_VARLONG_BYTES bytes",
        )
    }

    override fun writeVarLong(value: Long): PacketBuffer {
        var v = value
        while (true) {
            if (v and SEGMENT_MASK_LONG.inv() == 0L) {
                writeByte(v.toByte())
                return this
            }
            writeByte(((v and SEGMENT_MASK_LONG) or CONTINUE_MASK_LONG).toByte())
            v = v ushr SEGMENT_BITS
        }
    }

    // ── Compound types ──────────────────────────────────────────

    override fun readString(maxBytes: Int): String {
        val byteCount = readVarInt()
        require(byteCount >= 0) { "String byte length is negative: $byteCount" }
        require(byteCount <= maxBytes) {
            "String byte length $byteCount exceeds maximum $maxBytes"
        }
        return String(readBytes(byteCount), Charsets.UTF_8)
    }

    override fun writeString(value: String): PacketBuffer {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeVarInt(bytes.size)
        writeBytes(bytes)
        return this
    }

    override fun readUuid(): UUID {
        val most = readLong()
        val least = readLong()
        return UUID(most, least)
    }

    override fun writeUuid(value: UUID): PacketBuffer {
        writeLong(value.mostSignificantBits)
        writeLong(value.leastSignificantBits)
        return this
    }

    override fun readByteArray(maxLength: Int): ByteArray {
        val length = readVarInt()
        require(length >= 0) { "Byte array length is negative: $length" }
        require(length <= maxLength) {
            "Byte array length $length exceeds maximum $maxLength"
        }
        return readBytes(length)
    }

    override fun writeByteArray(value: ByteArray): PacketBuffer {
        writeVarInt(value.size)
        writeBytes(value)
        return this
    }

    override fun readBytes(length: Int): ByteArray {
        require(length >= 0) { "Length must be non-negative: $length" }
        checkReadable(length)
        val result = data.copyOfRange(_readerIndex, _readerIndex + length)
        _readerIndex += length
        return result
    }

    override fun writeBytes(value: ByteArray): PacketBuffer {
        ensureWritable(value.size)
        value.copyInto(data, _writerIndex)
        _writerIndex += value.size
        return this
    }

    // ── Block position (packed long) ────────────────────────────

    override fun readPosition(): Long = readLong()

    override fun writePosition(value: Long): PacketBuffer = writeLong(value)

    // ── Index / utility ─────────────────────────────────────────

    override fun setReaderIndex(index: Int): PacketBuffer {
        require(index in 0.._writerIndex) {
            "readerIndex $index out of bounds (writerIndex=$_writerIndex)"
        }
        _readerIndex = index
        return this
    }

    override fun setWriterIndex(index: Int): PacketBuffer {
        require(index >= _readerIndex) {
            "writerIndex $index must be >= readerIndex $_readerIndex"
        }
        if (index > data.size) {
            data = data.copyOf(index)
        }
        _writerIndex = index
        return this
    }

    override fun toByteArray(): ByteArray = data.copyOfRange(_readerIndex, _writerIndex)

    override fun toString(): String =
        "HeapPacketBuffer(readerIndex=$_readerIndex, writerIndex=$_writerIndex, " +
            "capacity=${data.size}, order=$byteOrder)"

    private companion object {
        private const val DEFAULT_CAPACITY = 256
        private const val SHORT_BYTES = 2
        private const val INT_BYTES = 4
        private const val LONG_BYTES = 8

        private const val BYTE_MASK = 0xFF
        private const val BYTE_MASK_LONG = 0xFFL

        private const val SEGMENT_BITS = 7
        private const val SEGMENT_MASK = 0x7F
        private const val CONTINUE_MASK = 0x80
        private const val SEGMENT_MASK_LONG = 0x7FL
        private const val CONTINUE_MASK_LONG = 0x80L

        private const val MAX_VARINT_BYTES = 5
        private const val MAX_VARLONG_BYTES = 10
    }
}
