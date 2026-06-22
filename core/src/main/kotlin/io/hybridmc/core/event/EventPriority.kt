package io.hybridmc.core.event

/**
 * Execution priority of an event handler.
 * Handlers are called in order from [LOWEST] to [MONITOR].
 */
public enum class EventPriority {
    /** Called first. Useful for early changes or cancellations. */
    LOWEST,

    /** Called second. */
    LOW,

    /** Default priority. Called third. */
    NORMAL,

    /** Called fourth. */
    HIGH,

    /** Called fifth. Useful for overriding earlier changes. */
    HIGHEST,

    /** Called last. Read-only context; handlers should not modify the event. */
    MONITOR,
}
