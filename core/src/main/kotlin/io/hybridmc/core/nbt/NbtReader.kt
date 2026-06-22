package io.hybridmc.core.nbt

import io.hybridmc.core.buffer.MalformedVarIntException
import java.io.DataInput
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reads NBT tags from an [InputStream] using the specified [NbtFormat].
 */
public class NbtReader(
    private val input: InputStream,
    private val format: NbtFormat,
) {
    private val dataInput: DataInput? =
        if (format == NbtFormat.JavaNetwork || format == NbtFormat.JavaDisk) {
            DataInputStream(input)
        } else {
            null
        }

    private val buffer8 = ByteArray(8)
    private val bb8 = ByteBuffer.wrap(buffer8).order(ByteOrder.LITTLE_ENDIAN)

    /**
     * Reads a full compound NBT tree from the stream.
     *
     * @return A pair containing the root name (can be empty, or null for JavaNetwork) and the root compound tag.
     */
    public fun readRoot(): Pair<String?, NbtCompound> {
        val typeId = readByte()
        if (typeId != NbtTagType.COMPOUND.id.toByte()) {
            throw IllegalArgumentException("Expected root tag to be COMPOUND (10), got $typeId")
        }

        val rootName =
            if (format == NbtFormat.JavaNetwork) {
                null // Network format has no root name
            } else {
                readString() // JavaDisk and Bedrock both have root names
            }

        val compound = readCompound()
        return rootName to compound
    }

    /**
     * Reads a single unnamed NBT tag of the specified type.
     */
    public fun readTag(type: NbtTagType): NbtTag =
        when (type) {
            NbtTagType.END -> NbtEnd
            NbtTagType.BYTE -> NbtByte(readByte())
            NbtTagType.SHORT -> NbtShort(readShort())
            NbtTagType.INT -> NbtInt(readInt())
            NbtTagType.LONG -> NbtLong(readLong())
            NbtTagType.FLOAT -> NbtFloat(readFloat())
            NbtTagType.DOUBLE -> NbtDouble(readDouble())
            NbtTagType.BYTE_ARRAY -> NbtByteArray(readByteArray())
            NbtTagType.STRING -> NbtString(readString())
            NbtTagType.LIST -> readList()
            NbtTagType.COMPOUND -> readCompound()
            NbtTagType.INT_ARRAY -> NbtIntArray(readIntArray())
            NbtTagType.LONG_ARRAY -> NbtLongArray(readLongArray())
        }

    private fun readCompound(): NbtCompound {
        val builder = NbtCompoundBuilder()
        while (true) {
            val typeId = readByte().toInt()
            if (typeId == 0) break // END tag
            val type = NbtTagType.fromId(typeId)
            val name = readString()
            val tag = readTag(type)
            builder.put(name, tag)
        }
        return builder.build()
    }

    @Suppress("UNCHECKED_CAST")
    private fun readList(): NbtTag {
        val typeId = readByte().toInt()
        val type = NbtTagType.fromId(typeId)
        val size = if (format == NbtFormat.BedrockNetwork) readVarInt() else readInt()

        require(size >= 0) { "Negative list size: $size" }

        val builder = NbtListBuilder<NbtTag>(type)
        for (i in 0 until size) {
            builder.add(readTag(type))
        }
        return builder.build()
    }

    // ── Primitive reading depending on format ───────────────────

    private fun readFully(
        bytes: ByteArray,
        len: Int,
    ) {
        var read = 0
        while (read < len) {
            val n = input.read(bytes, read, len - read)
            if (n < 0) throw EOFException()
            read += n
        }
    }

    private fun readByte(): Byte {
        val b = input.read()
        if (b < 0) throw EOFException()
        return b.toByte()
    }

    private fun readShort(): Short =
        if (dataInput != null) {
            dataInput.readShort()
        } else {
            readFully(buffer8, 2)
            bb8.getShort(0)
        }

    private fun readInt(): Int =
        if (dataInput != null) {
            dataInput.readInt()
        } else {
            readFully(buffer8, 4)
            bb8.getInt(0)
        }

    private fun readLong(): Long =
        if (dataInput != null) {
            dataInput.readLong()
        } else {
            readFully(buffer8, 8)
            bb8.getLong(0)
        }

    private fun readFloat(): Float =
        if (dataInput != null) {
            dataInput.readFloat()
        } else {
            readFully(buffer8, 4)
            bb8.getFloat(0)
        }

    private fun readDouble(): Double =
        if (dataInput != null) {
            dataInput.readDouble()
        } else {
            readFully(buffer8, 8)
            bb8.getDouble(0)
        }

    private fun readString(): String =
        if (dataInput != null) {
            dataInput.readUTF()
        } else {
            val len = readVarInt()
            val bytes = ByteArray(len)
            readFully(bytes, len)
            String(bytes, Charsets.UTF_8)
        }

    private fun readByteArray(): ByteArray {
        val len = if (format == NbtFormat.BedrockNetwork) readVarInt() else readInt()
        val bytes = ByteArray(len)
        readFully(bytes, len)
        return bytes
    }

    private fun readIntArray(): IntArray {
        val len = if (format == NbtFormat.BedrockNetwork) readVarInt() else readInt()
        val array = IntArray(len)
        for (i in 0 until len) {
            array[i] = readInt()
        }
        return array
    }

    private fun readLongArray(): LongArray {
        val len = if (format == NbtFormat.BedrockNetwork) readVarInt() else readInt()
        val array = LongArray(len)
        for (i in 0 until len) {
            array[i] = readLong()
        }
        return array
    }

    private fun readVarInt(): Int {
        var result = 0
        for (i in 0 until 5) {
            val b = readByte().toInt()
            result = result or ((b and 0x7F) shl (i * 7))
            if ((b and 0x80) == 0) {
                return result
            }
        }
        throw MalformedVarIntException("VarInt too large")
    }
}
