package io.hybridmc.core.service

/**
 * A pluggable engine module (world, entity, game, …). The composition root registers
 * subsystems; the server drives their lifecycle through this SPI without ever depending
 * on the concrete modules that implement them.
 */
public interface Subsystem {
    public val id: String

    public fun start(services: ServiceRegistry)

    public fun stop()
}
