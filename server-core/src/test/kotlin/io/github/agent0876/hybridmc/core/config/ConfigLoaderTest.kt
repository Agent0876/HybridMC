package io.github.agent0876.hybridmc.core.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigLoaderTest {

    private fun tempDir(): java.nio.file.Path = Files.createTempDirectory("hybridmc-test-")

    @Test
    fun `creates default hybrid yml when missing`() {
        val dir = tempDir()
        val config = ConfigLoader.load(dir)
        assertTrue(Files.exists(dir.resolve("hybrid.yml")))
        assertNotNull(config.editions["java"])
        assertNotNull(config.editions["bedrock"])
        assertEquals("world", config.world.name)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `reads edition settings from yml`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("hybrid.yml"), """
            editions:
              java:
                enabled: true
                host: 127.0.0.1
                port: 25566
                max-players: 50
                motd: Custom MOTD
              bedrock:
                enabled: false
                host: 10.0.0.1
                port: 19133
                description: Test
                max-connections: 100
        """.trimIndent())

        val config = ConfigLoader.load(dir)
        val java = config.editions["java"]!!
        assertEquals("127.0.0.1", java.host)
        assertEquals(25566, java.port)
        assertEquals(50, (java.options["max-players"] as? Number)?.toInt())
        assertEquals("Custom MOTD", java.options["motd"])

        val bk = config.editions["bedrock"]!!
        assertEquals(false, bk.enabled)
        assertEquals("10.0.0.1", bk.host)
        assertEquals(19133, bk.port)
        assertEquals(100, (bk.options["max-connections"] as? Number)?.toInt())
        assertEquals("Test", bk.options["description"])
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `reads world config from yml`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("hybrid.yml"), """
            world:
              name: nether
              gamemode: creative
              difficulty: hard
        """.trimIndent())

        val config = ConfigLoader.load(dir)
        assertEquals("nether", config.world.name)
        assertEquals("creative", config.world.gamemode)
        assertEquals("hard", config.world.difficulty)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `legacy server-properties merges into java`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), """
            server-port=25565
            server-ip=0.0.0.0
            max-players=99
            motd=Legacy
            online-mode=true
        """.trimIndent())
        Files.writeString(dir.resolve("hybrid.yml"), "")

        val config = ConfigLoader.load(dir)
        val java = config.editions["java"]
        assertNotNull(java)
        assertEquals(25565, java.port)
        assertEquals("0.0.0.0", java.host)
        assertEquals("99", java.options["max-players"].toString())
        assertEquals("Legacy", java.options["motd"])
        assertEquals("true", java.options["online-mode"])
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `yml takes precedence over legacy properties`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), """
            server-port=9999
            motd=FromProperties
        """.trimIndent())
        Files.writeString(dir.resolve("hybrid.yml"), """
            editions:
              java:
                port: 25565
                motd: "FromYml"
        """.trimIndent())

        val config = ConfigLoader.load(dir)
        val java = config.editions["java"]!!
        assertEquals(25565, java.port)
        assertEquals("FromYml", java.options["motd"])
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `uses defaults when yml is invalid`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("hybrid.yml"), "{{invalid_yaml}")

        val config = ConfigLoader.load(dir)
        assertEquals(25565, config.editions["java"]?.port)
        assertEquals(true, config.editions["bedrock"]?.enabled)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `custom edition keys are preserved`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("hybrid.yml"), """
            editions:
              custom-edition:
                enabled: true
                host: 1.2.3.4
                port: 12345
        """.trimIndent())

        val config = ConfigLoader.load(dir)
        val custom = config.editions["custom-edition"]
        assertNotNull(custom)
        assertEquals("1.2.3.4", custom.host)
        assertEquals(12345, custom.port)
        dir.toFile().deleteRecursively()
    }
}
