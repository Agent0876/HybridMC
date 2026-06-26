package io.github.agent0876.hybridmc.core

import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.player.TestPlayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class HybridServerTest {

    @Test
    fun `constructor creates world with given registry`() {
        val registry = PlayerRegistry()
        val server = HybridServer(playerRegistry = registry)
        assertEquals(registry, server.playerRegistry)
        assertNotNull(server.world)
    }

    @Test
    fun `start throws if javaServer is not set`() {
        val server = HybridServer()
        server.bedrockServer = FakeServerLifecycle()
        assertFailsWith<IllegalArgumentException> { server.start() }
    }

    @Test
    fun `start throws if bedrockServer is not set`() {
        val server = HybridServer()
        server.javaServer = FakeServerLifecycle()
        assertFailsWith<IllegalArgumentException> { server.start() }
    }

    @Test
    fun `stop broadcasts shutdown and stops sub-servers`() {
        val registry = PlayerRegistry()
        val player = TestPlayer(username = "Tester")
        registry.join(player)

        val server = HybridServer(playerRegistry = registry)
        val java = FakeServerLifecycle()
        val bedrock = FakeServerLifecycle()
        server.javaServer = java
        server.bedrockServer = bedrock

        server.stop()

        assertEquals("§cServer is shutting down…", player.lastMessage)
        assertTrue(java.stopped)
        assertTrue(bedrock.stopped)
    }

    @Test
    fun `stop is safe when no sub-servers set`() {
        val server = HybridServer()
        server.stop()
    }
}

class FakeServerLifecycle : ServerLifecycle {
    var started = false
    var stopped = false

    override suspend fun start() {
        started = true
    }

    override fun stop() {
        stopped = true
    }
}
