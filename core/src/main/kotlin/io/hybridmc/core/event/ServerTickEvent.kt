package io.hybridmc.core.event

/**
 * Dispatched on the main event bus on every server tick.
 *
 * @property tickCount The current absolute tick count of the server (starting at 1).
 */
public class ServerTickEvent(
    public val tickCount: Long,
)
