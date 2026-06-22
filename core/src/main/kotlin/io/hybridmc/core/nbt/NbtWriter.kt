package io.hybridmc.core.nbt

import java.io.DataOutput
import java.io.DataOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes NBT tags to an [OutputStream] using the specified [NbtFormat].
 */
public class NbtWriter(
    private val output: OutputStream,
    private val format: NbtFormat,
) {
    private val dataOutput: DataOutput? =
        if (format == NbtFormat.JavaNetwork || format == NbtFormat.JavaDisk) {
            DataOutputStream(output)
        } else {
            null
        }

    private val buffer8 = ByteArray(8)
    private val bb8 = ByteBuffer.wrap(buffer8).order(ByteOrder.LITTLE_ENDIAN)

    /**
     * Writes a full compound NBT tree to the stream.
     *
     * @param rootName The root name (omitted entirely for JavaNetwork).
     * @param root The root compound tag to write.
     */
    public fun writeRoot(
        rootName: String,
        root: NbtCompound,
    ) {
        writeByte(NbtTagType.COMPOUND.id.toByte())
        if (format != NbtFormat.JavaNetwork) {
            writeString(rootName)
        }
        writeCompound(root)
    }

    /**
     * Writes a single unnamed tag.
     */
    public fun writeTag(tag: NbtTag) {
        when (tag) {
            is NbtEnd -> {}

            // END tag has no payload
            is NbtByte -> {
                writeByte(tag.value)
            }

            is NbtShort -> {
                writeShort(tag.value)
            }

            is NbtInt -> {
                writeInt(tag.value)
            }

            is NbtLong -> {
                writeLong(tag.value)
            }

            is NbtFloat -> {
                writeFloat(tag.value)
            }

            is NbtDouble -> {
                writeDouble(tag.value)
            }

            is NbtByteArray -> {
                writeByteArray(tag.value)
            }

            is NbtString -> {
                writeString(tag.value)
            }

            is NbtList<*> -> {
                writeList(tag)
            }

            is NbtCompound -> {
                writeCompound(tag)
            }

            is NbtIntArray -> {
                writeIntArray(tag.value)
            }

            is NbtLongArray -> {
                writeLongArray(tag.value)
            }
        }
    }

    private fun writeCompound(compound: NbtCompound) {
        for ((name, child) in compound.value) {
            writeByte(child.type.id.toByte())
            writeString(name)
            writeTag(child)
        }
        writeByte(0) // END tag
    }

    private fun writeList(list: NbtList<*>) {
        writeByte(list.elementType.id.toByte())
        val size = list.value.size
        if (format == NbtFormat.BedrockNetwork) {
            writeVarInt(size)
        } else {
            writeInt(size)
        }
        for (child in list.value) {
            writeTag(child)
        }
    }

    // ── Primitive writing depending on format ───────────────────

    private fun writeByte(value: Byte) {
        output.write(value.toInt())
    }

    private fun writeShort(value: Short) {
        if (dataOutput != null) {
            dataOutput.writeShort(value.toInt())
        } else {
            bb8.putShort(0, value)
            output.write(buffer8, 0, 2)
        }
    }

    private fun writeInt(value: Int) {
        if (dataOutput != null) {
            dataOutput.writeInt(value)
        } else {
            bb8.putInt(0, value)
            output.write(buffer8, 0, 4)
        }
    }

    private fun writeLong(value: Long) {
        if (dataOutput != null) {
            dataOutput.writeLong(value)
        } else {
            bb8.putLong(0, value)
            output.write(buffer8, 0, 8)
        }
    }

    private fun writeFloat(value: Float) {
        if (dataOutput != null) {
            dataOutput.writeFloat(value)
        } else {
            bb8.putFloat(0, value)
            output.write(buffer8, 0, 4)
        }
    }

    private fun writeDouble(value: Double) {
        if (dataOutput != null) {
            dataOutput.writeDouble(value)
        } else {
            bb8.putDouble(0, value)
            output.write(buffer8, 0, 8)
        }
    }

    private fun writeString(value: String) {
        if (dataOutput != null) {
            dataOutput.writeUTF(value)
        } else {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeVarInt(bytes.size)
            output.write(bytes)
        }
    }

    private fun writeByteArray(value: ByteArray) {
        if (format == NbtFormat.BedrockNetwork) {
            writeVarInt(value.size)
        } else {
            writeInt(value.size)
        }
        output.write(value)
    }

    private fun writeIntArray(value: IntArray) {
        if (format == NbtFormat.BedrockNetwork) {
            writeVarInt(value.size)
        } else {
            writeInt(value.size)
        }
        for (v in value) {
            writeInt(v)
        }
    }

    private fun writeLongArray(value: LongArray) {
        if (format == NbtFormat.BedrockNetwork) {
            writeVarInt(value.size)
        } else {
            writeInt(value.size)
        }
        for (v in value) {
            writeLong(v)
        }
    }

    private fun writeVarInt(value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                writeByte(v.toByte())
                return
            }
            writeByte(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
    }
}
