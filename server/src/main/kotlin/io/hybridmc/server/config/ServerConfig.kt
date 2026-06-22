package io.hybridmc.server.config

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

private val logger = KotlinLogging.logger {}

/**
 * Server configuration holder. Loads, validates, and automatically writes default
 * server.properties settings at startup.
 */
public class ServerConfig(
    public val serverIp: String = "0.0.0.0",
    public val serverPort: Int = 25565,
    public val bedrockPort: Int = 19132,
    public val onlineMode: Boolean = true,
    public val viewDistance: Int = 10,
    public val gamemode: String = "survival",
    public val motd: String = "A HybridMC Minecraft Server",
) {
    public companion object {
        private const val DEFAULT_TEMPLATE: String = """# HybridMC Server Properties
# Target: Java/Bedrock 26.2

server-ip=0.0.0.0
server-port=25565
bedrock-port=19132
online-mode=true
view-distance=10
gamemode=survival
motd=A HybridMC Minecraft Server
"""

        /**
         * Loads configuration from [path]. If the file does not exist, a default template
         * will be created and written to disk automatically.
         */
        public fun load(path: Path): ServerConfig {
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path.parent ?: Path.of("."))
                    Files.writeString(path, DEFAULT_TEMPLATE)
                    logger.info { "Created default configuration file: ${path.toAbsolutePath()}" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to write default configuration to $path" }
                }
                return ServerConfig()
            }

            val properties = Properties()
            try {
                Files.newInputStream(path).use { properties.load(it) }
            } catch (e: Exception) {
                logger.error(e) { "Failed to load configuration from $path, using defaults" }
                return ServerConfig()
            }

            val serverIp = properties.getProperty("server-ip", "0.0.0.0")

            val rawPort = properties.getProperty("server-port")
            val serverPort =
                if (rawPort != null) {
                    val parsed = rawPort.toIntOrNull()
                    if (parsed != null && parsed in 1..65535) {
                        parsed
                    } else {
                        logger.warn { "Invalid server-port '$rawPort', falling back to default 25565" }
                        25565
                    }
                } else {
                    25565
                }

            val rawBedrockPort = properties.getProperty("bedrock-port")
            val bedrockPort =
                if (rawBedrockPort != null) {
                    val parsed = rawBedrockPort.toIntOrNull()
                    if (parsed != null && parsed in 1..65535) {
                        parsed
                    } else {
                        logger.warn { "Invalid bedrock-port '$rawBedrockPort', falling back to default 19132" }
                        19132
                    }
                } else {
                    19132
                }

            val rawOnlineMode = properties.getProperty("online-mode")
            val onlineMode =
                if (rawOnlineMode != null) {
                    if (rawOnlineMode.equals("true", ignoreCase = true)) {
                        true
                    } else if (rawOnlineMode.equals("false", ignoreCase = true)) {
                        false
                    } else {
                        logger.warn { "Invalid online-mode '$rawOnlineMode', falling back to default true" }
                        true
                    }
                } else {
                    true
                }

            val rawViewDistance = properties.getProperty("view-distance")
            val viewDistance =
                if (rawViewDistance != null) {
                    val parsed = rawViewDistance.toIntOrNull()
                    if (parsed != null && parsed in 2..32) {
                        parsed
                    } else {
                        logger.warn { "Invalid view-distance '$rawViewDistance' (must be between 2 and 32), falling back to default 10" }
                        10
                    }
                } else {
                    10
                }

            val rawGamemode = properties.getProperty("gamemode")
            val validGamemodes = setOf("survival", "creative", "adventure", "spectator")
            val gamemode =
                if (rawGamemode != null) {
                    val normalized = rawGamemode.trim().lowercase()
                    if (normalized in validGamemodes) {
                        normalized
                    } else {
                        logger.warn { "Invalid gamemode '$rawGamemode', falling back to default survival" }
                        "survival"
                    }
                } else {
                    "survival"
                }

            val motd = properties.getProperty("motd", "A HybridMC Minecraft Server")

            return ServerConfig(
                serverIp = serverIp,
                serverPort = serverPort,
                bedrockPort = bedrockPort,
                onlineMode = onlineMode,
                viewDistance = viewDistance,
                gamemode = gamemode,
                motd = motd,
            )
        }
    }
}
