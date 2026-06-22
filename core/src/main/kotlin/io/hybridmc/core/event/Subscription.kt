package io.hybridmc.core.event

/**
 * Represents a handler registration subscription that can be cancelled/unsubscribed.
 */
public fun interface Subscription {
    /**
     * Unregisters this handler from receiving further events.
     */
    public fun unsubscribe()
}
