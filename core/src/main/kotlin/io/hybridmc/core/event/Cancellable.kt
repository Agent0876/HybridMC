package io.hybridmc.core.event

/**
 * Marks an event that can be cancelled, preventing related actions from taking place.
 */
public interface Cancellable {
    /** Returns true if this event is currently cancelled. */
    public var isCancelled: Boolean
}
// Note: We can use a mutable var property for simplicity, or getter/setter. var is simpler and standard in Kotlin.
