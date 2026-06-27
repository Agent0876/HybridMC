package io.github.agent0876.hybridmc.core.config

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object ConfigLoader {

    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    private val DEFAULT_PROPERTIES = """
        # Common server settings
        server-ip=
        level-name=world
        level-seed=
        gamemode=survival
        force-gamemode=false
        difficulty=easy
        max-players=20
        online-mode=true
        view-distance=10
        player-idle-timeout=0
    """.trimIndent()

    private val DEFAULT_YML = """
        # HybridMC edition settings
        # Common settings (gamemode, difficulty, max-players, etc.) are in server.properties
        editions:
          java:
            enabled: true
            host: 0.0.0.0
            port: 25565
            # Java Edition specific
            motd: "§aHybridMC §7— Java + Bedrock"
            white-list: false
            enforce-secure-profile: false
            spawn-protection: 16
            hardcore: false
            allow-flight: false
            network-compression-threshold: 256
            simulation-distance: 10
          bedrock:
            enabled: true
            host: 0.0.0.0
            port: 19132
            # Bedrock Edition specific
            server-name: "HybridMC"
            server-portv6: 19133
            allow-list: false
            allow-cheats: false
            tick-distance: 4
            max-connections: 200
            compression-threshold: 1
            compression-algorithm: zlib
            default-player-permission-level: member
            texturepack-required: false
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

        // Priority: defaults < server.properties < hybrid.yml
        var editions = HybridConfig.defaultEditions()

        val props = Files.newBufferedReader(propsPath).use { reader ->
            Properties().also { it.load(reader) }
        }
        editions = mergeServerProperties(editions, props)

        val ymlMap = readYaml(ymlPath)
        val ymlEditions = parseEditions(ymlMap)
        editions = deepMerge(editions, ymlEditions)

        return HybridConfig(
            editions = editions,
            world = worldConfig(props, ymlMap),
        )
    }

    private data class EditionOverride(
        val enabled: Boolean?,
        val host: String?,
        val port: Int?,
        val options: Map<String, Any?>,
    )

    @Suppress("UNCHECKED_CAST")
    private fun parseEditions(yml: Map<String, Any?>): Map<String, EditionOverride> {
        val raw = (yml["editions"] as? Map<String, Any?>) ?: return emptyMap()
        val result = mutableMapOf<String, EditionOverride>()
        for ((key, value) in raw) {
            val cfg = value as? Map<String, Any?> ?: continue
            result[key] = EditionOverride(
                enabled = cfg["enabled"] as? Boolean,
                host = cfg["host"] as? String,
                port = (cfg["port"] as? Number)?.toInt(),
                options = cfg.filterKeys { it !in setOf("enabled", "host", "port") },
            )
        }
        return result.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    private fun worldConfig(props: Properties, yml: Map<String, Any?>): WorldConfig {
        val w = (yml["world"] as? Map<String, Any?>) ?: emptyMap()
        return WorldConfig(
            name = (w["name"] as? String) ?: props.getProperty("level-name", "world"),
            seed = w["seed"]?.toString() ?: props.getProperty("level-seed", ""),
            gamemode = (w["gamemode"] as? String) ?: props.getProperty("gamemode", "survival"),
            forceGamemode = (w["force-gamemode"] as? Boolean)
                ?: props.getProperty("force-gamemode", "false").toBoolean(),
            difficulty = (w["difficulty"] as? String) ?: props.getProperty("difficulty", "easy"),
        )
    }

    private fun deepMerge(
        base: Map<String, EditionConfig>,
        override: Map<String, EditionOverride>,
    ): Map<String, EditionConfig> {
        val result = base.toMutableMap()
        for ((key, overCfg) in override) {
            val baseCfg = result[key]
            result[key] = if (baseCfg != null) {
                baseCfg.copy(
                    enabled = overCfg.enabled ?: baseCfg.enabled,
                    host = overCfg.host ?: baseCfg.host,
                    port = overCfg.port ?: baseCfg.port,
                    options = baseCfg.options + overCfg.options,
                )
            } else {
                EditionConfig(
                    enabled = overCfg.enabled ?: true,
                    host = overCfg.host ?: "0.0.0.0",
                    port = overCfg.port ?: 25565,
                    options = overCfg.options,
                )
            }
        }
        return result
    }

    private fun mergeServerProperties(
        editions: Map<String, EditionConfig>,
        props: Properties,
    ): Map<String, EditionConfig> {
        val result = editions.toMutableMap()
        val java = result.getOrPut("java") { EditionConfig() }
        val commonOptions = buildMap<String, Any?> {
            props.getProperty("max-players")?.toIntOrNull()?.let { put("max-players", it) }
            props.getProperty("online-mode")?.let { put("online-mode", it) }
        }
        result["java"] = java.copy(
            host = props.getProperty("server-ip")?.takeIf { it.isNotBlank() } ?: java.host,
            port = props.getProperty("server-port")?.toIntOrNull() ?: java.port,
            options = java.options + commonOptions,
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
