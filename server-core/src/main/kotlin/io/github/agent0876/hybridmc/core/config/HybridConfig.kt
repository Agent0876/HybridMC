package io.github.agent0876.hybridmc.core.config

data class HybridConfig(
    val editions: Map<String, EditionConfig> = defaultEditions(),
    val world: WorldConfig = WorldConfig(),
) {
    companion object {
        fun defaultEditions() = mapOf(
            "java" to EditionConfig(
                enabled = true,
                host = "0.0.0.0",
                port = 25565,
                options = mapOf(
                    "motd" to "§aHybridMC §7— Java + Bedrock",
                    "white-list" to "false",
                    "enforce-secure-profile" to "false",
                    "spawn-protection" to "16",
                    "hardcore" to "false",
                    "allow-flight" to "false",
                    "network-compression-threshold" to "256",
                    "simulation-distance" to "10",
                ),
            ),
            "bedrock" to EditionConfig(
                enabled = true,
                host = "0.0.0.0",
                port = 19132,
                options = mapOf(
                    "server-name" to "HybridMC",
                    "server-portv6" to "19133",
                    "allow-list" to "false",
                    "allow-cheats" to "false",
                    "tick-distance" to "4",
                    "max-connections" to "200",
                    "compression-threshold" to "1",
                    "compression-algorithm" to "zlib",
                    "default-player-permission-level" to "member",
                    "texturepack-required" to "false",
                ),
            ),
        )
    }
}

data class EditionConfig(
    val enabled: Boolean = true,
    val host: String = "0.0.0.0",
    val port: Int = 25565,
    val options: Map<String, Any?> = emptyMap(),
)

data class WorldConfig(
    val name: String = "world",
    val seed: String = "",
    val gamemode: String = "survival",
    val forceGamemode: Boolean = false,
    val difficulty: String = "easy",
)
