package io.hybridmc.core.registry

/**
 * Interface used by the server to trigger freezing of all registries
 * without direct coupling to the registry implementation.
 */
public interface RegistryFreezer {
    /** Freezes all registries. */
    public fun freezeAll()
}
