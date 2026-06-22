package io.hybridmc.core.nbt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class NbtCodecTest {
    private fun createTestCompound(): NbtCompound =
        nbtCompound {
            putByte("byteTest", 127)
            putShort("shortTest", 32767)
            putInt("intTest", 2147483647)
            putLong("longTest", 9223372036854775807L)
            putFloat("floatTest", 0.49823147f)
            putDouble("doubleTest", 0.49312871321823148)
            putByteArray("byteArrayTest", byteArrayOf(1, 2, 3))
            putString("stringTest", "HELLO WORLD THIS IS A TEST STRING \u00C5\u00D6\u00D5\u00D8\u00DE")
            putList("listTest (long)", NbtTagType.LONG) {
                add(NbtLong(11))
                add(NbtLong(12))
                add(NbtLong(13))
                add(NbtLong(14))
                add(NbtLong(15))
            }
            putCompound("nested compound test") {
                putCompound("egg") {
                    putString("name", "Eggbert")
                    putFloat("value", 0.5f)
                }
                putCompound("ham") {
                    putString("name", "Hampus")
                    putFloat("value", 0.75f)
                }
            }
            putIntArray("intArrayTest", intArrayOf(1, 2, 3))
            putLongArray("longArrayTest", longArrayOf(1L, 2L, 3L))
        }

    @Test
    fun `JavaDisk format round-trip`() {
        val rootName = "Level"
        val original = createTestCompound()

        val out = ByteArrayOutputStream()
        val writer = NbtWriter(out, NbtFormat.JavaDisk)
        writer.writeRoot(rootName, original)

        val bytes = out.toByteArray()

        val `in` = ByteArrayInputStream(bytes)
        val reader = NbtReader(`in`, NbtFormat.JavaDisk)
        val (readName, readCompound) = reader.readRoot()

        assertEquals(rootName, readName)
        assertEquals(original, readCompound)
    }

    @Test
    fun `JavaNetwork format round-trip`() {
        val original = createTestCompound()

        val out = ByteArrayOutputStream()
        val writer = NbtWriter(out, NbtFormat.JavaNetwork)
        writer.writeRoot("", original) // Name is ignored

        val bytes = out.toByteArray()

        val `in` = ByteArrayInputStream(bytes)
        val reader = NbtReader(`in`, NbtFormat.JavaNetwork)
        val (readName, readCompound) = reader.readRoot()

        assertNull(readName, "JavaNetwork should not read a root name")
        assertEquals(original, readCompound)
    }

    @Test
    fun `BedrockNetwork format round-trip`() {
        val rootName = "BedrockRoot"
        val original = createTestCompound()

        val out = ByteArrayOutputStream()
        val writer = NbtWriter(out, NbtFormat.BedrockNetwork)
        writer.writeRoot(rootName, original)

        val bytes = out.toByteArray()

        val `in` = ByteArrayInputStream(bytes)
        val reader = NbtReader(`in`, NbtFormat.BedrockNetwork)
        val (readName, readCompound) = reader.readRoot()

        assertEquals(rootName, readName)
        assertEquals(original, readCompound)
    }
}
