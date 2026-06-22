package io.hybridmc.registry

import io.hybridmc.core.Identifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegistryManagerTest {
    @Test
    fun `test registry manager tracks and freezes registries`() {
        val manager = RegistryManager()
        val defaultId = Identifier.of("minecraft:air")

        val blockRegistry = manager.createRegistry<String>(defaultId)
        assertFalse(manager.frozen)
        assertFalse(blockRegistry.frozen)

        // Register default value to avoid freeze validation error
        blockRegistry.register(defaultId, "Air")

        manager.freezeAll()
        assertTrue(manager.frozen)
        assertTrue(blockRegistry.frozen)

        // Attempting to register after freeze should fail
        assertThrows(IllegalStateException::class.java) {
            blockRegistry.register(Identifier.of("minecraft:stone"), "Stone")
        }
    }
}
