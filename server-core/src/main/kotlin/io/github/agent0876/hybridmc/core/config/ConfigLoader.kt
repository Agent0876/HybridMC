package io.github.agent0876.hybridmc.core.config

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object ConfigLoader {

    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    private val DEFAULT_YML = """
        # HybridMC configuration
        editions:
          java:
            enabled: true
            host: 0.0.0.0
            port: 25565
            max-players: 100
            motd: "§aHybridMC §7— Java + Bedrock"
            online-mode: false
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
        val ymlPath = dataDir.resolve("hybrid.yml")
        val propsPath = dataDir.resolve("server.properties")

        if (Files.notExists(ymlPath)) {
            Files.writeString(ymlPath, DEFAULT_YML)
            logger.info("Created default hybrid.yml")
        }

        // Priority: defaults < server.properties (legacy) < hybrid.yml
        var editions = HybridConfig.defaultEditions()

        val props = if (Files.exists(propsPath)) {
            Properties().also { it.load(Files.newBufferedReader(propsPath)) }
        } else null
        if (props != null) {
            editions = mergeLegacyProperties(editions, props)
        }

        val ymlMap = readYaml(ymlPath)
        val ymlEditions = parseEditions(ymlMap)
        editions = deepMerge(editions, ymlEditions)

        return HybridConfig(
            editions = editions,
            world = worldConfig(ymlMap),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseEditions(yml: Map<String, Any?>): Map<String, EditionConfig> {
        val raw = (yml["editions"] as? Map<String, Any?>) ?: return emptyMap()
        val result = mutableMapOf<String, EditionConfig>()
        for ((key, value) in raw) {
            val cfg = value as? Map<String, Any?> ?: continue
            result[key] = EditionConfig(
                enabled = (cfg["enabled"] as? Boolean) ?: true,
                host = (cfg["host"] as? String) ?: "0.0.0.0",
                port = (cfg["port"] as? Number)?.toInt() ?: 25565,
                options = cfg.filterKeys { it !in setOf("enabled", "host", "port") },
            )
        }
        return result.toMap()
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

    private fun deepMerge(
        base: Map<String, EditionConfig>,
        override: Map<String, EditionConfig>,
    ): Map<String, EditionConfig> {
        val result = base.toMutableMap()
        for ((key, overCfg) in override) {
            val baseCfg = result[key]
            result[key] = if (baseCfg != null) {
                baseCfg.copy(
                    enabled = overCfg.enabled,
                    host = overCfg.host,
                    port = overCfg.port,
                    options = baseCfg.options + overCfg.options,
                )
            } else {
                overCfg
            }
        }
        return result
    }

    private fun mergeLegacyProperties(
        editions: Map<String, EditionConfig>,
        props: Properties,
    ): Map<String, EditionConfig> {
        val result = editions.toMutableMap()
        val java = result.getOrPut("java") { EditionConfig() }
        result["java"] = java.copy(
            enabled = java.enabled,
            host = props.getProperty("server-ip", java.host),
            port = props.getProperty("server-port", java.port.toString()).toIntOrNull() ?: java.port,
            options = java.options + mapOf(
                "max-players" to (props.getProperty("max-players", java.options["max-players"]?.toString() ?: "100")),
                "motd" to (props.getProperty("motd", java.options["motd"]?.toString() ?: "§aHybridMC §7— Java + Bedrock")),
                "online-mode" to (props.getProperty("online-mode", java.options["online-mode"]?.toString() ?: "false")),
            ),
        )
        return result
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
