package io.github.agent0876.hybridmc.app

import io.github.agent0876.hybridmc.bedrock.BedrockEditionServer
import io.github.agent0876.hybridmc.core.HybridServer
import io.github.agent0876.hybridmc.core.config.ConfigLoader
import io.github.agent0876.hybridmc.java.JavaEditionServer

fun main() {
    val config = ConfigLoader.load()

    val server = HybridServer()

    server.javaServer = JavaEditionServer(
        registry = server.playerRegistry,
        world = server.world,
        host = config.java.host,
        port = config.java.port,
        maxPlayers = config.java.maxPlayers,
        motd = config.java.motd,
    )

    if (config.bedrock.enabled) {
        server.bedrockServer = BedrockEditionServer(
            registry = server.playerRegistry,
            world = server.world,
            host = config.bedrock.host,
            port = config.bedrock.port,
            maxConnections = config.bedrock.maxConnections,
            description = config.bedrock.description,
            gameMode = config.world.gamemode.replaceFirstChar { it.uppercase() },
        )
    }

    server.start()
}
