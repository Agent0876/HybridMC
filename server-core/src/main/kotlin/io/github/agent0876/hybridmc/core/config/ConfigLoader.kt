package io.github.agent0876.hybridmc.core.config

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object ConfigLoader {

    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    private val DEFAULT_PROPERTIES = """
        #Minecraft server properties
        server-port=25565
        server-ip=0.0.0.0
        max-players=100
        motd=A Minecraft Server
        online-mode=false
    """.trimIndent()

    private val DEFAULT_YML = """
        # HybridMC configuration
        bedrock:
          enabled: true
          host: 0.0.0.0
          port: 19132
          description: "HybridMC -- Bedrock Edition"
          max-connections: 200
        
        world:
          name: world
          gamemode: survival
          difficulty: easy
    """.trimIndent()

    fun load(dataDir: Path = Path.of(".")): HybridConfig {
        val propsPath = dataDir.resolve("server.properties")
        val ymlPath = dataDir.resolve("hybrid.yml")

        if (Files.notExists(propsPath)) {
            Files.writeString(propsPath, DEFAULT_PROPERTIES)
            logger.info("Created default server.properties")
        }
        if (Files.notExists(ymlPath)) {
            Files.writeString(ymlPath, DEFAULT_YML)
            logger.info("Created default hybrid.yml")
        }

        val props = Properties().also { it.load(Files.newBufferedReader(propsPath)) }
        val ymlMap = readYaml(ymlPath)

        return HybridConfig(
            java = javaConfig(props),
            bedrock = bedrockConfig(ymlMap),
            world = worldConfig(ymlMap),
        )
    }

    private fun javaConfig(props: Properties): JavaConfig {
        return JavaConfig(
            host = props.getProperty("server-ip", "0.0.0.0"),
            port = props.getProperty("server-port", "25565").toIntOrNull() ?: 25565,
            maxPlayers = props.getProperty("max-players", "100").toIntOrNull() ?: 100,
            motd = props.getProperty("motd", "§aHybridMC §7— Java + Bedrock"),
            onlineMode = props.getProperty("online-mode", "false").toBoolean(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun bedrockConfig(yml: Map<String, Any?>): BedrockConfig {
        val bk = (yml["bedrock"] as? Map<String, Any?>) ?: return BedrockConfig()
        return BedrockConfig(
            enabled = (bk["enabled"] as? Boolean) ?: true,
            host = (bk["host"] as? String) ?: "0.0.0.0",
            port = (bk["port"] as? Number)?.toInt() ?: 19132,
            description = (bk["description"] as? String) ?: "HybridMC — Bedrock Edition",
            maxConnections = (bk["max-connections"] as? Number)?.toInt() ?: 200,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun worldConfig(yml: Map<String, Any?>): WorldConfig {
        val w = (yml["world"] as? Map<String, Any?>) ?: return WorldConfig()
        return WorldConfig(
            name = (w["name"] as? String) ?: "world",
            gamemode = (w["gamemode"] as? String) ?: "survival",
            difficulty = (w["difficulty"] as? String) ?: "easy",
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun readYaml(path: Path): Map<String, Any?> {
        return try {
            Files.newBufferedReader(path).use { reader ->
                (Yaml().load<Any>(reader) as? Map<String, Any?>) ?: emptyMap()
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse hybrid.yml, using defaults", e)
            emptyMap()
        }
    }
}
