package io.github.agent0876.hybridmc.java

import io.netty5.buffer.Buffer
import java.nio.charset.StandardCharsets

class NbtWriter {

    private val buf = io.netty5.buffer.BufferAllocator.onHeapUnpooled().allocate(4096)

    fun buffer(): Buffer = buf

    fun writeByte(value: Byte) { buf.writeByte(value) }

    fun writeShort(value: Int) { buf.writeShort(value.toShort()) }

    fun writeInt(value: Int) { buf.writeInt(value) }

    fun writeLong(value: Long) { buf.writeLong(value) }

    fun writeFloat(value: Float) { buf.writeFloat(value) }

    fun writeDouble(value: Double) { buf.writeDouble(value) }

    fun writeString(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        writeShort(bytes.size)
        buf.writeBytes(bytes)
    }

    fun writeByteArray(value: ByteArray) {
        writeInt(value.size)
        buf.writeBytes(value)
    }

    fun writeIntArray(value: IntArray) {
        writeInt(value.size)
        value.forEach { writeInt(it) }
    }

    fun writeLongArray(value: LongArray) {
        writeInt(value.size)
        value.forEach { writeLong(it) }
    }

    fun tagEnd() { buf.writeByte(0) }

    fun tagByte(name: String, value: Byte) {
        buf.writeByte(1); writeString(name); writeByte(value)
    }

    fun tagShort(name: String, value: Int) {
        buf.writeByte(2); writeString(name); writeShort(value)
    }

    fun tagInt(name: String, value: Int) {
        buf.writeByte(3); writeString(name); writeInt(value)
    }

    fun tagLong(name: String, value: Long) {
        buf.writeByte(4); writeString(name); writeLong(value)
    }

    fun tagFloat(name: String, value: Float) {
        buf.writeByte(5); writeString(name); writeFloat(value)
    }

    fun tagDouble(name: String, value: Double) {
        buf.writeByte(6); writeString(name); writeDouble(value)
    }

    fun tagByteArray(name: String, value: ByteArray) {
        buf.writeByte(7); writeString(name); writeByteArray(value)
    }

    fun tagString(name: String, value: String) {
        buf.writeByte(8); writeString(name); writeString(value)
    }

    fun tagList(name: String, elementType: Byte, block: NbtWriter.() -> Unit) {
        buf.writeByte(9); writeString(name); buf.writeByte(elementType); writeInt(0)
        val startSize = buf.readableBytes()
        block()
        val count = buf.readableBytes() - startSize
        val writePos = buf.writerOffset()
        buf.writerOffset(startSize - 4)
        writeInt(count)
        buf.writerOffset(writePos)
    }

    fun tagCompound(name: String, block: NbtWriter.() -> Unit) {
        buf.writeByte(10); writeString(name); compound(block)
    }

    fun tagIntArray(name: String, value: IntArray) {
        buf.writeByte(11); writeString(name); writeIntArray(value)
    }

    fun tagLongArray(name: String, value: LongArray) {
        buf.writeByte(12); writeString(name); writeLongArray(value)
    }

    fun compound(block: NbtWriter.() -> Unit) { block(); tagEnd() }

    fun tagListCount(name: String, elementType: Byte, count: Int, block: NbtWriter.() -> Unit) {
        buf.writeByte(9); writeString(name); buf.writeByte(elementType); writeInt(count)
        block()
    }
}
