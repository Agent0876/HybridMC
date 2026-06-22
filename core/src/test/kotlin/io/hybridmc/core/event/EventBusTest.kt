package io.hybridmc.core.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventBusTest {
    private class TestEvent(
        val message: String,
    )

    private class TestCancellableEvent : Cancellable {
        override var isCancelled: Boolean = false
    }

    private open class ParentEvent

    private class ChildEvent : ParentEvent()

    @Test
    fun `test simple event dispatch`() {
        val eventBus = EventBus()
        var received: String? = null

        eventBus.register(TestEvent::class) { event ->
            received = event.message
        }

        eventBus.post(TestEvent("Hello World"))
        assertEquals("Hello World", received)
    }

    @Test
    fun `test event priority execution order`() {
        val eventBus = EventBus()
        val order = mutableListOf<String>()

        eventBus.register(TestEvent::class, EventPriority.NORMAL) { order.add("NORMAL") }
        eventBus.register(TestEvent::class, EventPriority.MONITOR) { order.add("MONITOR") }
        eventBus.register(TestEvent::class, EventPriority.LOWEST) { order.add("LOWEST") }
        eventBus.register(TestEvent::class, EventPriority.HIGH) { order.add("HIGH") }
        eventBus.register(TestEvent::class, EventPriority.LOW) { order.add("LOW") }
        eventBus.register(TestEvent::class, EventPriority.HIGHEST) { order.add("HIGHEST") }

        eventBus.post(TestEvent("test"))

        val expected = listOf("LOWEST", "LOW", "NORMAL", "HIGH", "HIGHEST", "MONITOR")
        assertEquals(expected, order)
    }

    @Test
    fun `test cancellable event propagation and ignoreCancelled`() {
        val eventBus = EventBus()
        val results = mutableListOf<String>()

        // Handler 1: LOWEST, does not ignore cancelled (but can cancel)
        eventBus.register(TestCancellableEvent::class, EventPriority.LOWEST) { event ->
            results.add("LOWEST")
            event.isCancelled = true
        }

        // Handler 2: NORMAL, ignoreCancelled = true (should be skipped)
        eventBus.register(TestCancellableEvent::class, EventPriority.NORMAL, ignoreCancelled = true) {
            results.add("NORMAL")
        }

        // Handler 3: HIGH, ignoreCancelled = false (should be called despite being cancelled)
        eventBus.register(TestCancellableEvent::class, EventPriority.HIGH, ignoreCancelled = false) {
            results.add("HIGH")
        }

        val event = eventBus.post(TestCancellableEvent())

        assertTrue(event.isCancelled)
        assertEquals(listOf("LOWEST", "HIGH"), results)
    }

    @Test
    fun `test unsubscribe removes listener`() {
        val eventBus = EventBus()
        var count = 0

        val subscription =
            eventBus.register(TestEvent::class) {
                count++
            }

        eventBus.post(TestEvent("one"))
        assertEquals(1, count)

        subscription.unsubscribe()

        eventBus.post(TestEvent("two"))
        assertEquals(1, count) // still 1
    }

    @Test
    fun `test exception isolation`() {
        val eventBus = EventBus()
        var postExceptionCalled = false

        eventBus.register(TestEvent::class, EventPriority.LOW) {
            throw RuntimeException("Buggy listener")
        }

        eventBus.register(TestEvent::class, EventPriority.NORMAL) {
            postExceptionCalled = true
        }

        // Posting should not throw, and other handlers must be called
        eventBus.post(TestEvent("isolated"))
        assertTrue(postExceptionCalled)
    }

    @Test
    fun `test exact type matching only`() {
        val eventBus = EventBus()
        var parentCalled = false
        var childCalled = false

        eventBus.register(ParentEvent::class) {
            parentCalled = true
        }

        eventBus.register(ChildEvent::class) {
            childCalled = true
        }

        eventBus.post(ChildEvent())
        assertTrue(childCalled)
        assertFalse(parentCalled, "Parent handler should not receive child events under exact matching")

        childCalled = false
        eventBus.post(ParentEvent())
        assertTrue(parentCalled)
        assertFalse(childCalled, "Child handler should not receive parent events")
    }
}
