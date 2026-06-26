package io.github.agent0876.hybridmc.core.config

data class HybridConfig(
    val java: JavaConfig = JavaConfig(),
    val bedrock: BedrockConfig = BedrockConfig(),
    val world: WorldConfig = WorldConfig(),
)

data class JavaConfig(
    val host: String = "0.0.0.0",
    val port: Int = 25565,
    val maxPlayers: Int = 100,
    val motd: String = "§aHybridMC §7— Java + Bedrock",
    val onlineMode: Boolean = false,
)

data class BedrockConfig(
    val enabled: Boolean = true,
    val host: String = "0.0.0.0",
    val port: Int = 19132,
    val description: String = "HybridMC — Bedrock Edition",
    val maxConnections: Int = 200,
)

data class WorldConfig(
    val name: String = "world",
    val gamemode: String = "survival",
    val difficulty: String = "easy",
)
