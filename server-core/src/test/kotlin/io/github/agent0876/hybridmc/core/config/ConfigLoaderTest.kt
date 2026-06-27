package io.github.agent0876.hybridmc.core.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigLoaderTest {

    private fun tempDir(): java.nio.file.Path = Files.createTempDirectory("hybridmc-test-")

    @Test
    fun `creates default config files when missing`() {
        val dir = tempDir()
        val config = ConfigLoader.load(dir)
        assertTrue(Files.exists(dir.resolve("server.properties")))
        assertTrue(Files.exists(dir.resolve("hybrid.yml")))
        assertNotNull(config.editions["java"])
        assertNotNull(config.editions["bedrock"])
        assertEquals("world", config.world.name)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `reads edition network settings from yml`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("hybrid.yml"), """
            editions:
              java:
                enabled: true
                host: 127.0.0.1
                port: 25566
              bedrock:
                enabled: false
                host: 10.0.0.1
                port: 19133
                server-name: Test
                max-connections: 100
        """.trimIndent())

        val config = ConfigLoader.load(dir)
        val java = config.editions["java"]!!
        assertEquals("127.0.0.1", java.host)
        assertEquals(25566, java.port)

        val bk = config.editions["bedrock"]!!
        assertEquals(false, bk.enabled)
        assertEquals("10.0.0.1", bk.host)
        assertEquals(19133, bk.port)
        assertEquals(100, (bk.options["max-connections"] as? Number)?.toInt())
        assertEquals("Test", bk.options["server-name"])
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `reads world config from server properties`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), """
            level-name=nether
            gamemode=creative
            difficulty=hard
        """.trimIndent())
        Files.writeString(dir.resolve("hybrid.yml"), "")

        val config = ConfigLoader.load(dir)
        assertEquals("nether", config.world.name)
        assertEquals("creative", config.world.gamemode)
        assertEquals("hard", config.world.difficulty)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `reads world config from yml overriding server properties`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), """
            level-name=world
            gamemode=survival
            difficulty=easy
        """.trimIndent())
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
    fun `reads common settings from server properties`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), """
            server-ip=0.0.0.0
            max-players=50
            online-mode=false
            level-name=myworld
            level-seed=12345
            gamemode=creative
            force-gamemode=true
            difficulty=hard
        """.trimIndent())
        Files.writeString(dir.resolve("hybrid.yml"), "")

        val config = ConfigLoader.load(dir)
        val java = config.editions["java"]
        assertNotNull(java)
        assertEquals("0.0.0.0", java.host)
        assertEquals(50, (java.options["max-players"] as? Number)?.toInt())
        assertEquals("false", java.options["online-mode"])
        assertEquals("myworld", config.world.name)
        assertEquals("12345", config.world.seed)
        assertEquals("creative", config.world.gamemode)
        assertEquals(true, config.world.forceGamemode)
        assertEquals("hard", config.world.difficulty)
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `yml overrides server properties`() {
        val dir = tempDir()
        Files.writeString(dir.resolve("server.properties"), """
            server-port=9999
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
