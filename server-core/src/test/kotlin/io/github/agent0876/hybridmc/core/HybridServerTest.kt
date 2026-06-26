package io.github.agent0876.hybridmc.core

import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import io.github.agent0876.hybridmc.core.player.TestPlayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HybridServerTest {

    @Test
    fun `constructor creates world with given registry`() {
        val registry = PlayerRegistry()
        val server = HybridServer(playerRegistry = registry)
        assertEquals(registry, server.playerRegistry)
        assertNotNull(server.world)
    }

    @Test
    fun `install adds server`() {
        val server = HybridServer()
        val fake = FakeServerLifecycle()
        server.install(fake)
        assertEquals(1, server.installed().size)
        assertEquals(fake, server.installed().single())
    }

    @Test
    fun `stop broadcasts shutdown and stops installed servers`() {
        val registry = PlayerRegistry()
        val player = TestPlayer(username = "Tester")
        registry.join(player)

        val server = HybridServer(playerRegistry = registry)
        val a = FakeServerLifecycle()
        val b = FakeServerLifecycle()
        server.install(a)
        server.install(b)

        server.stop()

        assertEquals("§cServer is shutting down…", player.lastMessage)
        assertTrue(a.stopped)
        assertTrue(b.stopped)
    }

    @Test
    fun `stop is safe with no servers installed`() {
        val server = HybridServer()
        server.stop()
    }

    @Test
    fun `installed returns snapshot`() {
        val server = HybridServer()
        val a = FakeServerLifecycle()
        server.install(a)
        assertEquals(1, server.installed().size)
    }
}

class FakeServerLifecycle : ServerLifecycle {
    override val edition: Edition = Edition.JAVA
    var started = false
    var stopped = false

    override suspend fun start() {
        started = true
    }

    override fun stop() {
        stopped = true
    }
}
