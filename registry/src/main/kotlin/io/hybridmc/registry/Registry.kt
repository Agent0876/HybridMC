package io.hybridmc.registry

import io.hybridmc.core.Identifier
import java.util.Collections

/**
 * A generic registry that maintains mappings between [Identifier], `T` (the registered value), and `int` (the raw network ID).
 *
 * @param defaultId The identifier that should be returned as a fallback if an element is not found.
 */
public class Registry<T : Any>(
    public val defaultId: Identifier,
) : Iterable<T> {
    private val byId = mutableMapOf<Identifier, T>()
    private val byValue = mutableMapOf<T, Identifier>()
    private val byRawId = mutableMapOf<Int, T>()
    private val valueToRawId = mutableMapOf<T, Int>()

    private var nextRawId = 0
    private var isFrozen = false

    /**
     * The fallback value returned when an element is not found.
     * This is late-initialized to the value registered with [defaultId].
     */
    public val defaultValue: T
        get() = byId[defaultId] ?: throw IllegalStateException("Default value ($defaultId) has not been registered yet.")

    /**
     * Registers a new [value] under the given [id]. The raw ID is automatically assigned sequentially starting from 0.
     *
     * @throws IllegalStateException if the registry is frozen.
     * @throws IllegalArgumentException if the [id] or [value] is already registered.
     */
    public fun register(
        id: Identifier,
        value: T,
    ): T {
        check(!isFrozen) { "Cannot register new elements: Registry is frozen." }
        require(!byId.containsKey(id)) { "Identifier $id is already registered." }
        require(!byValue.containsKey(value)) { "Value $value is already registered." }

        val rawId = nextRawId++

        byId[id] = value
        byValue[value] = id
        byRawId[rawId] = value
        valueToRawId[value] = rawId

        return value
    }

    /**
     * Prevents any further registrations to this registry.
     * Ensures that the [defaultId] was actually registered before freezing.
     */
    public fun freeze() {
        check(!isFrozen) { "Registry is already frozen." }
        check(byId.containsKey(defaultId)) {
            "Cannot freeze registry: default value ($defaultId) was never registered."
        }
        isFrozen = true
    }

    /** Returns true if this registry is frozen and can no longer be modified. */
    public val frozen: Boolean get() = isFrozen

    /** Returns the value associated with the given [id], or [defaultValue] if not found. */
    public operator fun get(id: Identifier): T = byId[id] ?: defaultValue

    /** Returns the value associated with the given [rawId], or [defaultValue] if not found. */
    public operator fun get(rawId: Int): T = byRawId[rawId] ?: defaultValue

    /** Returns the [Identifier] associated with the given [value], or [defaultId] if not found. */
    public fun getId(value: T): Identifier = byValue[value] ?: defaultId

    /** Returns the raw network ID associated with the given [value], or the raw ID of the [defaultValue] if not found. */
    public fun getRawId(value: T): Int = valueToRawId[value] ?: valueToRawId.getValue(defaultValue)

    /** Returns true if the given [id] is registered. */
    public fun contains(id: Identifier): Boolean = byId.containsKey(id)

    /** Returns true if the given [value] is registered. */
    public fun contains(value: T): Boolean = byValue.containsKey(value)

    /** Returns true if the given [rawId] is registered. */
    public fun containsRawId(rawId: Int): Boolean = byRawId.containsKey(rawId)

    /** Returns an unmodifiable set of all registered identifiers. */
    public val keys: Set<Identifier> get() = Collections.unmodifiableSet(byId.keys)

    /** Returns an unmodifiable collection of all registered values. */
    public val values: Collection<T> get() = Collections.unmodifiableCollection(byId.values)

    /** Returns an unmodifiable set of all registered entries. */
    public val entries: Set<Map.Entry<Identifier, T>> get() = Collections.unmodifiableSet(byId.entries)

    /** Returns the number of registered elements. */
    public val size: Int get() = byId.size

    override fun iterator(): Iterator<T> = values.iterator()
}
