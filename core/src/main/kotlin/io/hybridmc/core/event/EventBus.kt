package io.hybridmc.core.event

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

/**
 * A synchronous event dispatcher that supports prioritized, reflection-free event subscriptions.
 */
public class EventBus {
    private val handlersMap: ConcurrentHashMap<KClass<*>, List<RegisteredHandler<*>>> = ConcurrentHashMap()

    /**
     * Registers an event handler for a specific event type.
     *
     * @param eventType The class of the event to listen for.
     * @param priority The priority of this handler.
     * @param ignoreCancelled If true, the handler is not invoked if the event is already cancelled.
     * @param handler The callback function to execute.
     * @return A [Subscription] to unregister the handler.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <E : Any> register(
        eventType: KClass<E>,
        priority: EventPriority = EventPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        handler: (E) -> Unit,
    ): Subscription {
        val registered = RegisteredHandler(priority, ignoreCancelled, handler)

        synchronized(this) {
            val list = handlersMap[eventType] ?: emptyList()
            val newList = list.toMutableList()
            newList.add(registered)
            newList.sortBy { it.priority.ordinal }
            handlersMap[eventType] = newList
        }

        return Subscription {
            synchronized(this) {
                val list = handlersMap[eventType] ?: return@Subscription
                val newList = list.toMutableList()
                newList.remove(registered)
                if (newList.isEmpty()) {
                    handlersMap.remove(eventType)
                } else {
                    handlersMap[eventType] = newList
                }
            }
        }
    }

    /**
     * Dispatches an event to all registered handlers for the exact type.
     * If the event implements [Cancellable], cancellation flags will be respected.
     *
     * @param event The event instance to dispatch.
     * @return The dispatched event instance.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <E : Any> post(event: E): E {
        val handlers = handlersMap[event::class] as? List<RegisteredHandler<E>> ?: return event

        for (registered in handlers) {
            if (event is Cancellable && event.isCancelled && registered.ignoreCancelled) {
                continue
            }
            try {
                registered.handler(event)
            } catch (t: Throwable) {
                logger.error(t) { "Error executing event handler for event type ${event::class.simpleName}" }
            }
        }
        return event
    }

    private class RegisteredHandler<E : Any>(
        val priority: EventPriority,
        val ignoreCancelled: Boolean,
        val handler: (E) -> Unit,
    )
}
