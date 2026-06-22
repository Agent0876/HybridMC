package io.hybridmc.core.service

import kotlin.reflect.KClass

/**
 * A minimal service locator the composition root populates and subsystems read from.
 * Keeps the server decoupled from concrete domain modules.
 */
public interface ServiceRegistry {
    public fun <T : Any> register(
        type: KClass<T>,
        service: T,
    )

    public fun <T : Any> get(type: KClass<T>): T

    public fun <T : Any> find(type: KClass<T>): T?
}

/** Reified convenience accessor: `registry.get<World>()`. */
public inline fun <reified T : Any> ServiceRegistry.get(): T = get(T::class)
