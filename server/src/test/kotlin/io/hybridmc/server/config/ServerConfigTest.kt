package io.hybridmc.server.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ServerConfigTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `test default config creation when file does not exist`() {
        val configPath = tempDir.resolve("server.properties")
        assertFalse(Files.exists(configPath))

        val config = ServerConfig.load(configPath)

        assertTrue(Files.exists(configPath))
        assertEquals("0.0.0.0", config.serverIp)
        assertEquals(25565, config.serverPort)
        assertEquals(19132, config.bedrockPort)
        assertTrue(config.onlineMode)
        assertEquals(10, config.viewDistance)
        assertEquals("survival", config.gamemode)
        assertEquals("A HybridMC Minecraft Server", config.motd)
    }

    @Test
    fun `test loading valid custom config`() {
        val configPath = tempDir.resolve("server.properties")
        val customProperties =
            """
            server-ip=127.0.0.1
            server-port=25570
            bedrock-port=19135
            online-mode=false
            view-distance=16
            gamemode=creative
            motd=My Custom HybridMC Server
            """.trimIndent()
        Files.writeString(configPath, customProperties)

        val config = ServerConfig.load(configPath)

        assertEquals("127.0.0.1", config.serverIp)
        assertEquals(25570, config.serverPort)
        assertEquals(19135, config.bedrockPort)
        assertFalse(config.onlineMode)
        assertEquals(16, config.viewDistance)
        assertEquals("creative", config.gamemode)
        assertEquals("My Custom HybridMC Server", config.motd)
    }

    @Test
    fun `test validation and fallback to defaults on invalid inputs`() {
        val configPath = tempDir.resolve("server.properties")
        val invalidProperties =
            """
            server-ip=10.0.0.1
            server-port=-1
            bedrock-port=999999
            online-mode=not-a-boolean
            view-distance=50
            gamemode=invalid_mode
            motd=Invalid Properties Test
            """.trimIndent()
        Files.writeString(configPath, invalidProperties)

        val config = ServerConfig.load(configPath)

        assertEquals("10.0.0.1", config.serverIp)
        assertEquals(25565, config.serverPort) // fallback to 25565
        assertEquals(19132, config.bedrockPort) // fallback to 19132
        assertTrue(config.onlineMode) // fallback to true
        assertEquals(10, config.viewDistance) // fallback to 10
        assertEquals("survival", config.gamemode) // fallback to survival
        assertEquals("Invalid Properties Test", config.motd)
    }
}
