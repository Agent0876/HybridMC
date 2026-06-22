package io.hybridmc.core.nbt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NbtTagTest {
    @Test
    fun `primitive tags correctly store values`() {
        assertEquals(42.toByte(), NbtByte(42).value)
        assertEquals(NbtTagType.BYTE, NbtByte(42).type)

        assertEquals("hello", NbtString("hello").value)
        assertEquals(NbtTagType.STRING, NbtString("hello").type)
    }

    @Test
    fun `nbtCompound dsl creates compound tags`() {
        val tag =
            nbtCompound {
                putByte("b", 1)
                putString("s", "test")
                putCompound("inner") {
                    putInt("i", 100)
                }
            }

        assertEquals(1.toByte(), tag.getByte("b"))
        assertEquals("test", tag.getString("s"))
        assertEquals(100, tag.getCompound("inner")?.getInt("i"))
        assertNull(tag.getString("not_exist"))
    }

    @Test
    fun `nbtList dsl creates list tags`() {
        val listTag =
            nbtList<NbtString>(NbtTagType.STRING) {
                add(NbtString("one"))
                add(NbtString("two"))
            }

        assertEquals(NbtTagType.STRING, listTag.elementType)
        assertEquals(2, listTag.value.size)
        assertEquals("one", listTag.value[0].value)
        assertEquals("two", listTag.value[1].value)
    }

    @Test
    fun `nbtCompoundList creates list of compounds`() {
        val listTag =
            nbtCompoundList {
                addCompound { putInt("v", 1) }
                addCompound { putInt("v", 2) }
            }

        assertEquals(NbtTagType.COMPOUND, listTag.elementType)
        assertEquals(1, listTag.value[0].getInt("v"))
        assertEquals(2, listTag.value[1].getInt("v"))
    }

    @Test
    fun `adding wrong type to nbtList throws`() {
        assertThrows<IllegalArgumentException> {
            nbtList<NbtTag>(NbtTagType.INT) {
                // Have to bypass generic slightly to test runtime type check if we used raw NbtTag
                // But compiler prevents add(NbtString) if it expects NbtInt.
                // We cast to test the internal require check:
                @Suppress("UNCHECKED_CAST")
                val builder = this as NbtListBuilder<NbtTag>
                builder.add(NbtString("wrong"))
            }
        }
    }

    @Test
    fun `compound equality`() {
        val a =
            nbtCompound {
                putInt("x", 1)
                putInt("y", 2)
            }
        val b =
            nbtCompound {
                putInt("y", 2)
                putInt("x", 1)
            } // Map equality handles order
        val c =
            nbtCompound {
                putInt("x", 1)
                putInt("y", 3)
            }

        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `list equality`() {
        val a = nbtCompoundList { addCompound { putInt("v", 1) } }
        val b = nbtCompoundList { addCompound { putInt("v", 1) } }
        val c = nbtCompoundList { addCompound { putInt("v", 2) } }

        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `end tag exists`() {
        assertEquals(NbtTagType.END, NbtEnd.type)
        assertTrue(NbtEnd === NbtEnd) // Singleton
    }
}
