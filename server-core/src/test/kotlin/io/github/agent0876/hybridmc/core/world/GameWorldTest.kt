package io.github.agent0876.hybridmc.core.world

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.player.TestPlayer
import kotlin.test.Test
import kotlin.test.assertEquals

class GameWorldTest {

    @Test
    fun `constructor sets name and registry`() {
        val registry = PlayerRegistry()
        val world = GameWorld(name = "nether", registry = registry)
        assertEquals("nether", world.name)
    }

    @Test
    fun `constructor uses default name`() {
        val world = GameWorld(registry = PlayerRegistry())
        assertEquals("world", world.name)
    }

    @Test
    fun `broadcastMessage delegates to registry`() {
        val registry = PlayerRegistry()
        val player = TestPlayer(username = "Listener")
        registry.join(player)

        val world = GameWorld(registry = registry)
        world.broadcastMessage("hi")

        assertEquals("hi", player.lastMessage)
    }
}
