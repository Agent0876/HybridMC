package io.github.agent0876.hybridmc.app

import io.github.agent0876.hybridmc.core.HybridServer
import io.github.agent0876.hybridmc.java.JavaEditionServer
import io.github.agent0876.hybridmc.bedrock.BedrockEditionServer

fun main() {
    val server = HybridServer()

    // Inject Java & Bedrock sub-servers
    server.javaServer = JavaEditionServer(
        registry = server.playerRegistry,
        world = server.world,
        port = 25565
    )

    server.bedrockServer = BedrockEditionServer(
        registry = server.playerRegistry,
        world = server.world,
        port = 19132
    )

    // Start the hybrid server (this will block until stopped)
    server.start()
}
