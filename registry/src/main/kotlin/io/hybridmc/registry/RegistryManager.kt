package io.hybridmc.registry

import io.github.oshai.kotlinlogging.KotlinLogging
import io.hybridmc.core.Identifier
import io.hybridmc.core.registry.RegistryFreezer

private val logger = KotlinLogging.logger {}

/**
 * Manages the lifecycle of all game registries, coordinating freeze stages.
 */
public class RegistryManager : RegistryFreezer {
    private val registries: MutableList<Registry<*>> = mutableListOf()
    private var isFrozen: Boolean = false

    /**
     * Creates a new [Registry] under this manager. The registry will be tracked and frozen
     * when [freezeAll] is called.
     *
     * @throws IllegalStateException if the manager is already frozen.
     */
    public fun <T : Any> createRegistry(defaultId: Identifier): Registry<T> {
        check(!isFrozen) { "Cannot create registry: RegistryManager is frozen." }
        val registry = Registry<T>(defaultId)
        registries.add(registry)
        return registry
    }

    /**
     * Freezes all tracked registries, preventing any further registrations.
     */
    public override fun freezeAll() {
        if (isFrozen) return
        logger.info { "Freezing all registries..." }
        registries.forEach { registry ->
            if (!registry.frozen) {
                registry.freeze()
            }
        }
        isFrozen = true
        logger.info { "All registries frozen successfully." }
    }

    /** Returns true if this manager is frozen. */
    public val frozen: Boolean get() = isFrozen
}
