package io.github.agent0876.hybridmc.core.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigLoaderTest {

    private fun tempDir(): java.nio.file.Path = Files.createTempDirectory("hybridmc-test-")

    @Test
    fun `creates default files when missing`() {
        val dir = tempDir()
        val config = ConfigLoader.load(dir)
        assertTrue(Files.exists(dir.resolve("server.properties")))
        assertTrue(Files.exists(dir.resolve("hybrid.yml")))
        assertEquals(25565, config.java.port)
        assertEquals("world", config.world.name)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `reads server-properties correctly`() {
        val dir = tempDir()
        val props = """
            server-port=19132
            server-ip=127.0.0.1
            max-players=50
            motd=Test Server
            online-mode=true
        """.trimIndent()
        Files.writeString(dir.resolve("server.properties"), props)
        Files.writeString(dir.resolve("hybrid.yml"), "world:\n  name: test\n")

        val config = ConfigLoader.load(dir)
        assertEquals(19132, config.java.port)
        assertEquals("127.0.0.1", config.java.host)
        assertEquals(50, config.java.maxPlayers)
        assertEquals("Test Server", config.java.motd)
        assertTrue(config.java.onlineMode)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `reads bedrock config from yml`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), "")
        Files.writeString(
            dir.resolve("hybrid.yml"),
            """
            bedrock:
              enabled: false
              host: 10.0.0.1
              port: 19133
              description: Custom Bedrock
              max-connections: 100
            """.trimIndent()
        )

        val config = ConfigLoader.load(dir)
        assertFalse(config.bedrock.enabled)
        assertEquals("10.0.0.1", config.bedrock.host)
        assertEquals(19133, config.bedrock.port)
        assertEquals("Custom Bedrock", config.bedrock.description)
        assertEquals(100, config.bedrock.maxConnections)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `reads world config from yml`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), "")
        Files.writeString(
            dir.resolve("hybrid.yml"),
            """
            world:
              name: nether
              gamemode: creative
              difficulty: hard
            """.trimIndent()
        )

        val config = ConfigLoader.load(dir)
        assertEquals("nether", config.world.name)
        assertEquals("creative", config.world.gamemode)
        assertEquals("hard", config.world.difficulty)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `uses defaults when files are empty`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), "")
        Files.writeString(dir.resolve("hybrid.yml"), "")

        val config = ConfigLoader.load(dir)
        assertEquals(25565, config.java.port)
        assertTrue(config.bedrock.enabled)
        assertEquals("world", config.world.name)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `uses defaults when yml is invalid`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), "")
        Files.writeString(dir.resolve("hybrid.yml"), "{{invalid_yaml}")

        val config = ConfigLoader.load(dir)
        assertEquals(25565, config.java.port)
        assertTrue(config.bedrock.enabled)
        dir.toFile().deleteRecursively()
    }
}
