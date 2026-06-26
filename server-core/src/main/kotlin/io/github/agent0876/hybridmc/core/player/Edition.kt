package io.github.agent0876.hybridmc.core.player

/** Identifies which Minecraft edition a player connected from. */
enum class Edition(
    val displayName: String,
    val defaultPort: Int,
) {
    JAVA("Java Edition", 25565),
    BEDROCK("Bedrock Edition", 19132),
}
