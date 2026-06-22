package io.hybridmc.core

import io.hybridmc.core.buffer.HeapPacketBuffer
import io.hybridmc.core.buffer.readIdentifier
import io.hybridmc.core.buffer.writeIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IdentifierTest {
    // ══════════════════════════════════════════════════════════════
    //  of(String) — throws on invalid input
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `of with full namespace-path`() {
        val id = Identifier.of("minecraft:stone")
        assertEquals("minecraft", id.namespace)
        assertEquals("stone", id.path)
        assertEquals("minecraft:stone", id.value)
    }

    @Test
    fun `of with path only defaults to minecraft namespace`() {
        val id = Identifier.of("stone")
        assertEquals("minecraft", id.namespace)
        assertEquals("stone", id.path)
        assertEquals("minecraft:stone", id.value)
    }

    @Test
    fun `of with custom namespace`() {
        val id = Identifier.of("custom:my_block")
        assertEquals("custom", id.namespace)
        assertEquals("my_block", id.path)
    }

    @Test
    fun `of with path containing slashes`() {
        val id = Identifier.of("minecraft:block/stone")
        assertEquals("minecraft", id.namespace)
        assertEquals("block/stone", id.path)
    }

    @ParameterizedTest(name = "of(\"{0}\") throws")
    @ValueSource(
        strings = [
            "", ":", ":path", "ns:",
            "UPPER:case", "ns:pa th", "ns:p@th", "ns:path!",
        ],
    )
    fun `of with invalid input throws IllegalArgumentException`(input: String) {
        assertThrows<IllegalArgumentException> { Identifier.of(input) }
    }

    // ══════════════════════════════════════════════════════════════
    //  of(namespace, path) — two-argument factory
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `of with explicit namespace and path`() {
        val id = Identifier.of("custom", "my_item")
        assertEquals("custom:my_item", id.value)
    }

    @Test
    fun `of with invalid namespace throws`() {
        assertThrows<IllegalArgumentException> { Identifier.of("UPPER", "path") }
    }

    @Test
    fun `of with invalid path throws`() {
        assertThrows<IllegalArgumentException> { Identifier.of("ns", "PA TH") }
    }

    @Test
    fun `of with empty namespace throws`() {
        assertThrows<IllegalArgumentException> { Identifier.of("", "path") }
    }

    @Test
    fun `of with empty path throws`() {
        assertThrows<IllegalArgumentException> { Identifier.of("ns", "") }
    }

    // ══════════════════════════════════════════════════════════════
    //  parse(String) — returns null on invalid input
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `parse valid identifier returns non-null`() {
        val id = Identifier.parse("minecraft:stone")
        assertNotNull(id)
        assertEquals("minecraft:stone", id!!.value)
    }

    @Test
    fun `parse without namespace defaults to minecraft`() {
        val id = Identifier.parse("diamond")
        assertNotNull(id)
        assertEquals("minecraft:diamond", id!!.value)
    }

    @ParameterizedTest(name = "parse(\"{0}\") → null")
    @ValueSource(
        strings = [
            "", ":", ":path", "ns:",
            "UPPER:case", "ns:p@th", "ns:!invalid",
        ],
    )
    fun `parse invalid input returns null`(input: String) {
        assertNull(Identifier.parse(input))
    }

    @Test
    fun `parse string with multiple colons returns null`() {
        // "a:b:c" → namespace="a", path="b:c", colon in path is invalid
        assertNull(Identifier.parse("a:b:c"))
    }

    // ══════════════════════════════════════════════════════════════
    //  Allowed characters — boundary verification
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `namespace allows lowercase, digits, dot, underscore, hyphen`() {
        val id = Identifier.of("a-b_c.d0", "path")
        assertEquals("a-b_c.d0", id.namespace)
    }

    @Test
    fun `path allows lowercase, digits, dot, underscore, hyphen, slash`() {
        val id = Identifier.of("ns", "a-b_c.d0/sub/deep")
        assertEquals("a-b_c.d0/sub/deep", id.path)
    }

    // ══════════════════════════════════════════════════════════════
    //  toString
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `toString always returns full namespace-colon-path`() {
        assertEquals("minecraft:stone", Identifier.of("stone").toString())
        assertEquals("custom:item", Identifier.of("custom:item").toString())
    }

    // ══════════════════════════════════════════════════════════════
    //  Equality
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `identifiers with same value are equal`() {
        assertEquals(Identifier.of("minecraft:stone"), Identifier.of("stone"))
    }

    @Test
    fun `identifiers with different values are not equal`() {
        assertNotEquals(Identifier.of("minecraft:stone"), Identifier.of("minecraft:dirt"))
    }

    @Test
    fun `identifiers in different namespaces are not equal`() {
        assertNotEquals(Identifier.of("minecraft:stone"), Identifier.of("custom:stone"))
    }

    // ══════════════════════════════════════════════════════════════
    //  Predefined constants
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `predefined constants have correct values`() {
        assertEquals("minecraft:air", Identifier.AIR.value)
        assertEquals("minecraft:stone", Identifier.STONE.value)
        assertEquals("minecraft:grass_block", Identifier.GRASS_BLOCK.value)
        assertEquals("minecraft:dirt", Identifier.DIRT.value)
        assertEquals("minecraft:bedrock", Identifier.BEDROCK.value)
    }

    @Test
    fun `predefined constant equals equivalent of-created identifier`() {
        assertEquals(Identifier.STONE, Identifier.of("minecraft:stone"))
        assertEquals(Identifier.AIR, Identifier.of("air"))
    }

    // ══════════════════════════════════════════════════════════════
    //  PacketBuffer round-trip
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `identifier survives PacketBuffer round-trip`() {
        val buf = HeapPacketBuffer()
        val original = Identifier.of("custom:my_block")
        buf.writeIdentifier(original)
        assertEquals(original, buf.readIdentifier())
    }

    @Test
    fun `predefined constant survives PacketBuffer round-trip`() {
        val buf = HeapPacketBuffer()
        buf.writeIdentifier(Identifier.STONE)
        assertEquals(Identifier.STONE, buf.readIdentifier())
    }
}
