package io.hybridmc.registry

import io.hybridmc.core.Identifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegistryTest {
    private val defaultId = Identifier.of("minecraft:air")
    private val stoneId = Identifier.of("minecraft:stone")
    private val dirtId = Identifier.of("minecraft:dirt")
    private val bedrockId = Identifier.of("minecraft:bedrock")

    @Test
    fun `test registration and retrieval`() {
        val registry = Registry<String>(defaultId)

        // Register default value first
        registry.register(defaultId, "Air")
        registry.register(stoneId, "Stone")
        registry.register(dirtId, "Dirt")

        assertEquals("Air", registry.defaultValue)
        assertEquals("Air", registry[defaultId])
        assertEquals("Stone", registry[stoneId])
        assertEquals("Dirt", registry[dirtId])

        // Query by rawId
        assertEquals("Air", registry[0])
        assertEquals("Stone", registry[1])
        assertEquals("Dirt", registry[2])

        // Query identifier and raw ID from value
        assertEquals(defaultId, registry.getId("Air"))
        assertEquals(stoneId, registry.getId("Stone"))
        assertEquals(dirtId, registry.getId("Dirt"))

        assertEquals(0, registry.getRawId("Air"))
        assertEquals(1, registry.getRawId("Stone"))
        assertEquals(2, registry.getRawId("Dirt"))

        // Size
        assertEquals(3, registry.size)
    }

    @Test
    fun `test contains checks`() {
        val registry = Registry<String>(defaultId)
        registry.register(defaultId, "Air")
        registry.register(stoneId, "Stone")

        assertTrue(registry.contains(defaultId))
        assertTrue(registry.contains(stoneId))
        assertFalse(registry.contains(dirtId))

        assertTrue(registry.contains("Air"))
        assertTrue(registry.contains("Stone"))
        assertFalse(registry.contains("Dirt"))

        assertTrue(registry.containsRawId(0))
        assertTrue(registry.containsRawId(1))
        assertFalse(registry.containsRawId(2))
    }

    @Test
    fun `test fallback and default values for unregistered query`() {
        val registry = Registry<String>(defaultId)
        registry.register(defaultId, "Air")
        registry.register(stoneId, "Stone")

        // Unregistered Identifier returns defaultValue
        assertEquals("Air", registry[dirtId])
        // Unregistered raw ID returns defaultValue
        assertEquals("Air", registry[999])
        // Unregistered value returns defaultId
        assertEquals(defaultId, registry.getId("Dirt"))
        // Unregistered value returns default raw ID
        assertEquals(0, registry.getRawId("Dirt"))
    }

    @Test
    fun `test access defaultValue before registering throws`() {
        val registry = Registry<String>(defaultId)
        assertThrows(IllegalStateException::class.java) {
            registry.defaultValue
        }
    }

    @Test
    fun `test registration constraints`() {
        val registry = Registry<String>(defaultId)
        registry.register(defaultId, "Air")

        // Duplicate identifier
        assertThrows(IllegalArgumentException::class.java) {
            registry.register(defaultId, "Another Air")
        }

        // Duplicate value
        assertThrows(IllegalArgumentException::class.java) {
            registry.register(stoneId, "Air")
        }
    }

    @Test
    fun `test freeze mechanics`() {
        val registry = Registry<String>(defaultId)

        // Cannot freeze if default value not registered
        assertThrows(IllegalStateException::class.java) {
            registry.freeze()
        }

        registry.register(defaultId, "Air")
        assertFalse(registry.frozen)

        registry.freeze()
        assertTrue(registry.frozen)

        // Cannot freeze again
        assertThrows(IllegalStateException::class.java) {
            registry.freeze()
        }

        // Cannot register new value after freeze
        assertThrows(IllegalStateException::class.java) {
            registry.register(stoneId, "Stone")
        }
    }

    @Test
    fun `test collections and iteration`() {
        val registry = Registry<String>(defaultId)
        registry.register(defaultId, "Air")
        registry.register(stoneId, "Stone")
        registry.register(dirtId, "Dirt")

        // Keys collection
        assertEquals(setOf(defaultId, stoneId, dirtId), registry.keys)

        // Values collection
        assertEquals(listOf("Air", "Stone", "Dirt"), registry.values.toList())

        // Entries
        val expectedEntries =
            mapOf(
                defaultId to "Air",
                stoneId to "Stone",
                dirtId to "Dirt",
            ).entries
        assertEquals(expectedEntries, registry.entries)

        // Iteration
        val list = mutableListOf<String>()
        for (item in registry) {
            list.add(item)
        }
        assertEquals(listOf("Air", "Stone", "Dirt"), list)
    }
}
