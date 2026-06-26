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

    private const val PROTOCOL_VERSION = 1001     // MCPE protocol for 1.20.x
    private const val GAME_VERSION     = "26.32"
    private const val MAX_PLAYERS      = 200
    private const val IPV4_PORT        = 19132
    private const val IPV6_PORT        = 19133

    fun build(
        description: String,
        serverGuid: Long,
        worldName: String,
        gameMode: String,
        registry: PlayerRegistry,
    ): String = buildString {
        append("MCPE")
        append(';').append(description)
        append(';').append(PROTOCOL_VERSION)
        append(';').append(GAME_VERSION)
        append(';').append(registry.onlineCount)
        append(';').append(MAX_PLAYERS)
        append(';').append(serverGuid)
        append(';').append(worldName)
        append(';').append(gameMode)
        append(";1")                     // edition: 1 = MCPE
        append(';').append(IPV4_PORT)
        append(';').append(IPV6_PORT)
        append(';')
    }
}
