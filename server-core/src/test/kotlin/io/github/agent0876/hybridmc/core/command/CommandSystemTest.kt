package io.github.agent0876.hybridmc.core.command

import io.github.agent0876.hybridmc.core.player.Edition
import io.github.agent0876.hybridmc.core.player.HybridPlayer
import io.github.agent0876.hybridmc.core.player.PlayerRegistry
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandSystemTest {

    private class MockPlayer(
        override val uuid: UUID = UUID.randomUUID(),
        override val username: String = "MockedPlayer",
        override val edition: Edition = Edition.JAVA,
        override val ping: Int = 5,
    ) : HybridPlayer {
        override val entityId: Int = 9999
        override var x: Double = 0.0
        override var y: Double = 100.0
        override var z: Double = 0.0
        override var yaw: Float = 0.0f
        override var pitch: Float = 0.0f

        val messages = mutableListOf<String>()
        var disconnected = false

        override fun sendMessage(message: String) {
            messages.add(message)
        }

        override fun disconnect(reason: String) {
            disconnected = true
        }

        override fun spawnPlayer(target: HybridPlayer) {}
        override fun removePlayer(target: HybridPlayer) {}
        override fun movePlayer(target: HybridPlayer) {}
    }

    private class MockConsoleSender : CommandSender {
        override val name = "CONSOLE"
        val messages = mutableListOf<String>()

        override fun sendMessage(message: String) {
            messages.add(message)
        }
    }

    @Test
    fun `unknown command sends error message`() {
        val registry = PlayerRegistry()
        val manager = CommandManager(registry) {}
        val sender = MockConsoleSender()

        val executed = manager.execute(sender, "/nonexistent")
        assertTrue(!executed)
        assertEquals(1, sender.messages.size)
        assertTrue(sender.messages[0].contains("Unknown command"))
    }

    @Test
    fun `help command lists all commands`() {
        val registry = PlayerRegistry()
        val manager = CommandManager(registry) {}
        val sender = MockConsoleSender()

        val executed = manager.execute(sender, "/help")
        assertTrue(executed)
        assertTrue(sender.messages.size > 1)
        assertTrue(sender.messages[0].contains("Help"))
        assertTrue(sender.messages.any { it.contains("/list") })
        assertTrue(sender.messages.any { it.contains("/say") })
    }

    @Test
    fun `list command lists online players`() {
        val registry = PlayerRegistry()
        val manager = CommandManager(registry) {}
        val sender = MockConsoleSender()

        // 1. empty list
        var executed = manager.execute(sender, "/list")
        assertTrue(executed)
        assertTrue(sender.messages.any { it.contains("No players online") })

        // 2. with one player
        val player = MockPlayer(username = "Player1", edition = Edition.BEDROCK, ping = 42)
        registry.join(player)
        sender.messages.clear()

        executed = manager.execute(sender, "/list")
        assertTrue(executed)
        assertTrue(sender.messages.any { it.contains("Players online (1)") })
        assertTrue(sender.messages.any { it.contains("Player1") && it.contains("Bedrock Edition") && it.contains("42ms") })
    }

    @Test
    fun `say command broadcasts server message`() {
        val registry = PlayerRegistry()
        val manager = CommandManager(registry) {}
        val player = MockPlayer()
        registry.join(player)

        val sender = MockConsoleSender()
        val executed = manager.execute(sender, "/say Hello world!")
        assertTrue(executed)

        // The player should receive the broadcast message
        assertTrue(player.messages.any { it.contains("[Server] Hello world!") })
    }

    @Test
    fun `stop command triggers callback`() {
        val registry = PlayerRegistry()
        var stopped = false
        val manager = CommandManager(registry) { stopped = true }
        val sender = MockConsoleSender()

        val executed = manager.execute(sender, "/stop")
        assertTrue(executed)
        assertTrue(stopped)
        assertTrue(sender.messages.any { it.contains("Stopping the server") })
    }
}
