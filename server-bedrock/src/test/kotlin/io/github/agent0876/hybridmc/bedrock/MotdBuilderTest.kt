package io.github.agent0876.hybridmc.bedrock

import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.HybridPlayer
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotdBuilderTest {

    private fun fakePlayer(): HybridPlayer = object : HybridPlayer {
        override val uuid = UUID.randomUUID()
        override val username = "Fake"
        override val edition = Edition.BEDROCK
        override val ping = 0
        override val entityId = 1
        override var x = 0.0
        override var y = 100.0
        override var z = 0.0
        override var yaw = 0.0f
        override var pitch = 0.0f
        override fun sendMessage(message: String) {}
        override fun disconnect(reason: String) {}
        override fun spawnPlayer(target: HybridPlayer) {}
        override fun removePlayer(target: HybridPlayer) {}
        override fun movePlayer(target: HybridPlayer) {}
    }

    @Test
    fun `build returns correct format`() {
        val registry = PlayerRegistry()
        val motd = MotdBuilder.build(
            description = "My Server",
            serverGuid = 12345L,
            worldName = "world",
            gameMode = "Survival",
            registry = registry,
        )
        assertTrue(motd.startsWith("MCPE;"), "should start with MCPE; got: $motd")
        assertTrue(motd.endsWith(";"), "should end with ; got: $motd")
    }

    @Test
    fun `build includes description`() {
        val motd = MotdBuilder.build(
            description = "Test Desc",
            serverGuid = 0L,
            worldName = "world",
            gameMode = "Creative",
            registry = PlayerRegistry(),
        )
        assertTrue(motd.contains(";Test Desc;"))
    }

    @Test
    fun `build includes protocol and version`() {
        val motd = MotdBuilder.build(
            description = "x",
            serverGuid = 0L,
            worldName = "world",
            gameMode = "Survival",
            registry = PlayerRegistry(),
        )
        val parts = motd.split(";")
        assertEquals("1001", parts[2])
        assertEquals("1.26.32", parts[3])
    }

    @Test
    fun `build uses onlineCount from registry`() {
        val registry = PlayerRegistry()
        registry.join(fakePlayer())
        registry.join(fakePlayer())

        val motd = MotdBuilder.build(
            description = "x",
            serverGuid = 0L,
            worldName = "world",
            gameMode = "Survival",
            registry = registry,
        )
        val parts = motd.split(";")
        assertEquals("2", parts[4])
        assertEquals("200", parts[5])
    }

    @Test
    fun `build includes guid`() {
        val motd = MotdBuilder.build(
            description = "x",
            serverGuid = 98765L,
            worldName = "world",
            gameMode = "Survival",
            registry = PlayerRegistry(),
        )
        assertTrue(motd.contains(";98765;"))
    }

    @Test
    fun `build includes world name and game mode`() {
        val motd = MotdBuilder.build(
            description = "x",
            serverGuid = 0L,
            worldName = "custom_world",
            gameMode = "Adventure",
            registry = PlayerRegistry(),
        )
        val parts = motd.split(";")
        assertEquals("custom_world", parts[7])
        assertEquals("Adventure", parts[8])
    }

    @Test
    fun `build includes edition flag, ipv4 port, ipv6 port`() {
        val motd = MotdBuilder.build(
            description = "x",
            serverGuid = 0L,
            worldName = "world",
            gameMode = "Survival",
            registry = PlayerRegistry(),
        )
        val parts = motd.split(";")
        assertEquals("1", parts[9])
        assertEquals("19132", parts[10])
        assertEquals("19133", parts[11])
    }
}
