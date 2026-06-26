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
                    "max-players" to "100",
                    "motd" to "§aHybridMC §7— Java + Bedrock",
                    "online-mode" to "false",
                ),
            ),
            "bedrock" to EditionConfig(
                enabled = true,
                host = "0.0.0.0",
                port = 19132,
                options = mapOf(
                    "description" to "HybridMC — Bedrock Edition",
                    "max-connections" to "200",
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
    val gamemode: String = "survival",
    val difficulty: String = "easy",
)
