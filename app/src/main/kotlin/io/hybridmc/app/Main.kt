package io.hybridmc.app

import io.github.oshai.kotlinlogging.KotlinLogging
import io.hybridmc.core.event.EventBus
import io.hybridmc.core.service.SimpleServiceRegistry
import io.hybridmc.core.service.Subsystem
import io.hybridmc.server.Server

private val logger = KotlinLogging.logger {}

public fun main() {
    logger.info { "HybridMC starting… (target: Java/Bedrock 26.2)" }

    val services = SimpleServiceRegistry()

    // Load server configuration
    val configPath =
        java.nio.file.Path
            .of("server.properties")
    val config =
        io.hybridmc.server.config.ServerConfig
            .load(configPath)
    services.register(io.hybridmc.server.config.ServerConfig::class, config)
    logger.info {
        "Loaded server configuration (Java Port: ${config.serverPort}, Bedrock Port: ${config.bedrockPort}, Online Mode: ${config.onlineMode})"
    }

    // Initialize and register RegistryManager
    val registryManager = io.hybridmc.registry.RegistryManager()
    services.register(io.hybridmc.registry.RegistryManager::class, registryManager)
    services.register(io.hybridmc.core.registry.RegistryFreezer::class, registryManager)

    // Register EventBus so that Server tick loop can dispatch tick events and subsystems can subscribe
    val eventBus = EventBus()
    services.register(EventBus::class, eventBus)

    // Composition root: concrete domain subsystems get registered here as milestones land (M0+).
    val subsystems = emptyList<Subsystem>()

    val mainThread = Thread.currentThread()
    val server = Server(services, subsystems)
    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Shutdown hook triggered, stopping server…" }
            server.stop()
            try {
                mainThread.join()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        },
    )

    // This will block the main thread and run the 20 TPS game loop
    server.start()

    logger.info { "HybridMC shutdown complete." }
}
