package io.github.agent0876.hybridmc.app

import io.github.agent0876.hybridmc.bedrock.BedrockEditionServer
import io.github.agent0876.hybridmc.core.HybridServer
import io.github.agent0876.hybridmc.core.config.ConfigLoader
import io.github.agent0876.hybridmc.java.JavaEditionServer

fun main() {
    val config = ConfigLoader.load()
    val server = HybridServer()

    config.editions.forEach { (key, cfg) ->
        if (!cfg.enabled) return@forEach
        val srv = when (key) {
            "java" -> JavaEditionServer(
                registry = server.playerRegistry,
                world = server.world,
                host = cfg.host,
                port = cfg.port,
                maxPlayers = (cfg.options["max-players"] as? Number)?.toInt() ?: 20,
                motd = (cfg.options["motd"] as? String) ?: "§aHybridMC §7— Java + Bedrock",
            )
            "bedrock" -> BedrockEditionServer(
                registry = server.playerRegistry,
                world = server.world,
                host = cfg.host,
                port = cfg.port,
                portv6 = (cfg.options["server-portv6"] as? Number)?.toInt() ?: 19133,
                maxConnections = (cfg.options["max-connections"] as? Number)?.toInt() ?: 200,
                maxPlayers = (cfg.options["max-players"] as? Number)?.toInt() ?: 20,
                description = (cfg.options["server-name"] as? String) ?: "HybridMC",
                gameMode = config.world.gamemode.replaceFirstChar { it.uppercase() },
            )
            else -> {
                println("Unknown edition '$key' in config, skipping")
                return@forEach
            }
        }
        server.install(srv)
    }

    server.start()
}
