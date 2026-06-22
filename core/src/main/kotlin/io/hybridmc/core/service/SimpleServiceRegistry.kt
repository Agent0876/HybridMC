package io.hybridmc.core.service

import kotlin.reflect.KClass

/** Default in-memory [ServiceRegistry]. Not thread-safe; populate it during boot. */
public class SimpleServiceRegistry : ServiceRegistry {
    private val services = mutableMapOf<KClass<*>, Any>()

    override fun <T : Any> register(
        type: KClass<T>,
        service: T,
    ) {
        require(services.put(type, service) == null) { "Service already registered for $type" }
    }

    override fun <T : Any> get(type: KClass<T>): T = find(type) ?: error("No service registered for $type")

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> find(type: KClass<T>): T? = services[type] as T?
}
