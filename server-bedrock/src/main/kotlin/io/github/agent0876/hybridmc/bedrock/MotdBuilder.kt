package io.github.agent0876.hybridmc.bedrock

import io.github.agent0876.hybridmc.core.player.PlayerRegistry

/**
 * Builds the MOTD string advertised to Bedrock clients during the UNCONNECTED_PONG handshake.
 *
 * Format (semicolon-separated):
 * ```
 * MCPE;<description>;<protocol>;<version>;<current>;<max>;<serverGuid>;<worldName>;<gameMode>;1;<ipv4Port>;<ipv6Port>;
 * ```
 *
 * Reference: https://wiki.vg/Raknet_Protocol#Unconnected_Ping / MCPE-specific extension
 */
object MotdBuilder {

    private const val PROTOCOL_VERSION = 1001     // MCPE protocol for 1.26.32
    private const val GAME_VERSION     = "1.26.32"

    fun build(
        description: String,
        serverGuid: Long,
        worldName: String,
        gameMode: String,
        registry: PlayerRegistry,
        maxPlayers: Int,
        port: Int,
        portv6: Int,
    ): String = buildString {
        append("MCPE")
        append(';').append(description)
        append(';').append(PROTOCOL_VERSION)
        append(';').append(GAME_VERSION)
        append(';').append(registry.onlineCount)
        append(';').append(maxPlayers)
        append(';').append(serverGuid)
        append(';').append(worldName)
        append(';').append(gameMode)
        append(";1")                     // edition: 1 = MCPE
        append(';').append(port)
        append(';').append(portv6)
        append(';')
    }
}
